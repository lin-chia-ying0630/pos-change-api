# POS Change API 保全變更後端

`pos-change-api` 提供保單查詢、保全變更草稿、案件查詢與覆核回寫 API。前端可以先取得案號，但只有真的修改資料時，後端才建立受理檔與異動明細。

## 功能流程

```mermaid
flowchart LR
    A["查詢保單"] --> B["複選一至多個變更項目"]
    B --> C{"最近相同項目為 P？"}
    C -->|是| K["拒絕申請並回傳受理中訊息"]
    C -->|否| L["資料庫原子取得一個案號並保留所選項目"]
    L --> D{"是否真的異動"}
    D -->|否| E["不建立受理資料"]
    D -->|是| F["建立一筆 P-受理中與多筆變更項目草稿"]
    F --> G["覆核先查看異動前後值"]
    G --> H{"覆核結果"}
    H -->|完成| I["P → A → 套用 → S"]
    H -->|取消| J["P → C，不套用"]
```

### 變更項目

- `001`：地址、email、電話或手機變更。
- `002`：主約保額變更，只讀寫 `main_policy_ride` 的主約列（`ride_order = 000`），並保存主約完整 before/after 快照供查詢與覆核。
- `003`：附約保額變更，以 `rideOrder` 定位正確附約。

### 受理狀態

- `P`：受理中，等待覆核。
- `A`：覆核交易套用中，只供後端原子鎖定使用。
- `S`：完成，異動已回寫。
- `C`：取消，異動不回寫。

覆核完成使用條件更新將 `P` 改成 `A`。只有成功鎖定案件的交易能套用資料，再將 `A` 改成 `S`，可避免兩位覆核人員同時完成同一案件。

受理檔會記錄 `created_by`、`reviewed_by` 與 `reviewed_at`。啟用 Security 時，只有原建檔經辦可修改草稿，且建檔人不得覆核自己的案件。

## 案號規則

格式為：

```text
C + 民國年 3 碼 + 月 2 碼 + 日 2 碼 + 流水號至少 3 碼
```

例如 `C1150712001`。流水號由 `policy_change_case_sequence` 使用 MySQL 原子遞增與 connection-local `LAST_INSERT_ID()` 取得，支援多執行緒、多 Pod 與服務重啟；超過 `999` 後會自然成長為四碼以上。

取號時會寫入一筆 `policy_change_case_reservation`，綁定保單、經辦帳號及有效期限，並在 `policy_change_case_reservation_item` 保存所有勾選項目；預設保留 30 分鐘。同一案號可逐項儲存 `001`、`002`、`003`，第一次有實際異動時只建立一筆 `policy_change_acceptance`，各項目分別寫入 `policy_change_item`。自行拼湊、使用別人的、已過期、保單不符或未在取號時選擇的項目都會被拒絕。

取號前依 `policy_no + policy_seq + change_item` 查詢最近一筆已受理案件。最近狀態為 `P` 時回傳 HTTP 409 與「此保單正在受理中，無法申請」；最近狀態為 `S`、`C` 或沒有歷史案件時才可申請。前端可先呼叫 eligibility API 顯示結果，但 `POST /api/change-cases` 必須再次檢核，不能只信任前端。

## 草稿規則

`policy_change_field` 與 `policy_change_file` 使用商業唯一鍵保存目前有效草稿：

- 欄位草稿：案號、項目、欄位名稱與 `change_key` 唯一。
- 檔案快照：案號、項目、檔案名稱與 `change_key` 唯一。
- 重複儲存同一目標會更新最新值，不會累積多筆有效版本。
- 改回原值時會刪除該目標草稿；案件已無任何異動時，一併移除受理資料。

`change_key` 用來定位資料列：

- 地址：`address_type`。
- 主約列：`000`。
- 附約：`ride_order`。

查詢案件明細時，`changeFields.chineseName` 與快照欄位名稱都由 `CodeDescription` 的 `CHT-code` 提供，確保畫面用詞一致。

## 覆核衝突

覆核套用前會比較正式資料目前值與草稿的 `content_before`：

- 值相同才允許套用。
- 比較與回寫期間使用 `SELECT ... FOR UPDATE` 鎖定目標資料列；主附約固定先鎖主檔、再依序鎖附約。
- 若其他案件已先修改同一地址、主約或附約，回傳 HTTP `409 Conflict`。
- 每次回寫都檢查 affected row 必須為 1，避免資料不存在時仍顯示完成。
- 整個覆核在同一交易內執行；任何一項失敗都會回復案件狀態與主檔更新。

## API

所有成功與錯誤回覆都使用 `ResponseBodyDto<T>`；request body 不包 `ResponseBodyDto`。

| API | 畫面 | 用途 |
| --- | --- | --- |
| `GET /api/auth/me` | 登入頁 | 驗證帳號並取得 MAKER / REVIEWER 角色。 |
| `GET /api/policies/{policyNo}/{policySeq}` | 新增頁 | 查詢主檔、地址、主附約與代碼。 |
| `GET /api/postal-codes/{postalCode}` | 地址 Dialog | 查詢 3 或 3+3 郵遞區號地址前綴。 |
| `GET /api/policies/{policyNo}/{seq}/change-items/{changeItem}/eligibility` | 新增頁 | 查詢同保單與項目最近案件，判斷是否可申請。 |
| `POST /api/change-cases` | 新增頁 | 傳入 `changeItems` 複選清單並原子取得一個案號，不建立受理資料。 |
| `POST /api/change-cases/{caseNo}/address-change` | 001 Dialog | 儲存地址或聯絡資料草稿。 |
| `POST /api/change-cases/{caseNo}/main-amount-change` | 002 Dialog | 儲存主約保額草稿。 |
| `POST /api/change-cases/{caseNo}/policies/{policyNo}/{seq}/rider-amount-change` | 003 Dialog | 儲存附約保額草稿。 |
| `GET /api/policies/{policyNo}/change-cases` | 查詢／覆核頁 | 查詢案件清單。 |
| `GET /api/policies/{policyNo}/{seq}/change-cases/{caseNo}` | 查詢／覆核頁 | 查詢欄位與檔案快照；快照 JSON 依 `CHT-code` 拆成逐欄中文名稱與異動前後值。 |
| `PATCH /api/change-cases/{caseNo}/status` | 覆核頁 | REVIEWER 將案件改成 `S` 或 `C`。 |

## 資安弱點驗證來源

本專案使用多個公開來源交叉檢查已知弱點，避免只依賴單一資料庫：

| 網站／工具 | 用途 | 網址 |
| --- | --- | --- |
| Google OSV.dev／OSV-Scanner | 掃描 Maven、npm 直接與間接依賴，依套件版本比對公開弱點。 | [OSV.dev](https://osv.dev/)／[OSV-Scanner](https://google.github.io/osv-scanner/) |
| npm Audit | 依 `package-lock.json` 對照 npm 官方 Advisory，檢查前端 production 與 development dependencies。 | [npm Audit](https://docs.npmjs.com/cli/commands/npm-audit/) |
| GitHub Advisory Database／Dependabot | 依 repository dependency graph 持續追蹤新公布弱點；GitHub repository 必須啟用 Dependabot Alerts。 | [GitHub Advisory Database](https://github.com/advisories)／[Dependabot Alerts](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-alerts) |
| NIST NVD／CVE | 查核 CVE、CVSS、CWE 與受影響版本；由 OWASP Dependency-Check 進行 SCA 對照。 | [NVD](https://nvd.nist.gov/)／[OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) |

掃描報告預設放在 `logs/security-scan-YYYY-MM-DD.md`，原始 OSV／npm JSON 也放在 `logs/`。該目錄已由 Git 排除，避免報告中的本機路徑或環境資訊進入版本庫。掃描只能識別已公開且能正確對應版本的弱點，不能取代程式碼審查、權限測試、動態掃描與滲透測試。

## 架構

```mermaid
flowchart TD
    A["PolicyChangeController"] --> B["PolicyChangeService facade"]
    B --> C["PolicyQueryService"]
    B --> D["ChangeCaseDraftService"]
    B --> E["AddressChangeSaveService"]
    B --> F["AmountChangeSaveService"]
    B --> G["ChangeCaseReviewService"]
    G --> H["ChangeCaseApplyService"]
    C --> I["PolicyChangeDao"]
    D --> I
    E --> I
    F --> I
    G --> I
    H --> I
    I --> J["PolicyChangeDao.xml / MyBatis"]
    J --> K["MySQL"]
```

後端維持三層：

- Controller：HTTP、Bean Validation 與 `ResponseBodyDto`。
- Service：use case、交易與商業規則；每個 Service 都有 interface。
- DAO：`PolicyChangeDao` 由 MyBatis 直接建立代理，不再保留純轉呼叫的 DAO implementation 與 Mapper interface。

`PolicyChangeServiceImpl` 是薄 facade，不重複實作各 use case。

## 資料庫版本

資料庫結構由 Flyway 管理：

- `db/migration/V1__baseline.sql`：核心資料表、代碼與郵遞區號。
- `db/migration/V2__harden_change_case_workflow.sql`：原子流水號、草稿唯一鍵與更新時間。
- `db/migration/V3__add_cht_json_field_names.sql`：建立快照 JSON key 對應的繁體中文欄位名稱。
- `db/migration/V4__secure_change_case_audit.sql`：案號保留、建檔人、覆核人與覆核時間。
- `db/migration/V5__support_multiple_change_items_per_case.sql`：同一案號的多項預約明細。
- `db/local/R__demo_policy.sql`：只在 `local` profile 建立示範保單。

正式環境不要手動重跑舊 `schema.sql`。資料庫本身需先建立，啟動時由 Flyway 套用尚未執行的版本。

## Security 與 CORS

所有環境預設 `POS_SECURITY_ENABLED=true`，缺少帳密時會拒絕啟動；只有 `local` 或 `test` profile 可以明確設為 `false`。Compose 預設也會開啟登入，並要求提供經辦與覆核帳密。正式環境使用 `prod` profile，除了登入外也強制 HTTPS：

```text
POS_SECURITY_ENABLED=true
POS_SECURITY_REQUIRE_HTTPS=true
POS_MAKER_USERNAME
POS_MAKER_PASSWORD
POS_REVIEWER_USERNAME
POS_REVIEWER_PASSWORD
```

- `MAKER`：查詢、取號、儲存 001／002／003。
- `REVIEWER`：查詢案件明細、完成或取消案件。
- 密碼至少 12 個字元；經辦、覆核帳號不可相同。
- MAKER 的案件清單只顯示自己建立的案件；REVIEWER 可查看待覆核案件。
- Basic Auth 正式流量必須經 TLS；反向代理需正確傳入 `X-Forwarded-Proto=https`。
- 未登入回覆 `401 ResponseBodyDto`，角色不符回覆 `403 ResponseBodyDto`。
- CORS 來源由 `CORS_ALLOWED_ORIGINS` 以逗號分隔設定。

資料庫帳密沒有程式預設值，必須由環境變數或 Secret 提供：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## 本機啟動

IntelliJ 可直接選擇 shared run configuration `POS Change API Local` 後按 Debug；此設定會啟用 `local` profile，載入正式 migration 與 `db/local` 測試資料。本機預設連線 `localhost:3306/main`，可再用下列環境變數覆寫。

```bash
export SPRING_PROFILES_ACTIVE=local
export DB_URL='jdbc:mysql://localhost:3306/main?serverTimezone=Asia/Taipei&characterEncoding=utf-8'
export DB_USERNAME='your-user'
export DB_PASSWORD='your-password'
export POS_MAKER_USERNAME='maker'
export POS_MAKER_PASSWORD='replace-with-at-least-12-characters'
export POS_REVIEWER_USERNAME='reviewer'
export POS_REVIEWER_PASSWORD='replace-with-another-12-characters'
mvn spring-boot:run
```

預設 API：`http://localhost:8081`。

健康檢查：

```text
GET /actuator/health/liveness
GET /actuator/health/readiness
```

## 測試與 CI

```bash
mvn test
mvn clean verify
```

- 一般單元測試不連本機 MySQL。
- `SecurityAuthorizationTest` 驗證 401/403 `ResponseBodyDto`、MAKER/REVIEWER 分權、假案號、負數保額、登入身份與 CORS origin。
- `PolicyChangeSupportServiceImplTest` 與 `ChangeCaseReviewServiceImplTest` 驗證案號擁有者、逾期案號及 maker-checker 稽核欄位。
- `PolicyChangeWorkflowIntegrationTest` 使用 MySQL Testcontainers 驗證 Flyway、原子案號、無異動、重複儲存、過期案件及兩案同時覆核衝突。
- Docker 未啟動時整合測試會略過；GitHub Actions 的 Docker 環境會完整執行。
- `.github/workflows/ci.yml` 在 push 與 pull request 執行測試、打包與 Docker build。
- `.github/workflows/security.yml` 每週及主分支異動執行 CodeQL 與 OSV；Dependabot 每週檢查 Maven、Docker 與 Actions。

## Docker

```bash
docker build -t pos-change-api:latest .
```

Dockerfile 使用 BuildKit cache 保存 Maven 本機倉庫，runtime 採目前掃描無 Critical／High 的 Temurin UBI minimal JRE、不再額外安裝套件，並以 UID `10001` 非 root 執行。建置與執行映像都固定 image digest；升級時須同步掃描弱點並提交新的 digest。Compose 將 API 設為唯讀檔案系統、移除 Linux capabilities，且只綁定本機回圈位址。

前後端與 MySQL 建議由 `pos-web/compose.yaml` 一起啟動，避免 port、network 與資料庫環境設定不一致。

## SQL Log 與個資

MyBatis 原始參數 log 維持關閉：

```properties
mybatis.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl
logging.level.com.alin.lin.dao=info
```

Debug SQL 統一由 `MaskedSqlLogInterceptor` 輸出，保單號碼、地址、email、電話與手機會遮罩。Log 同時輸出 stdout 與 rolling file；容器或 K8s 應以 stdout 收集為主。
# 正式部署安全設定

`main.code_description` 的 `main-screen/screen` 對照表定義四個畫面支線：`CREATE`、`UPDATE`、`DELETE` 僅對應 `MAKER`，`REVIEW` 對應 `REVIEWER`。查詢保單時一併回傳給前端；實際 API 權限仍由 Spring Security 後端控管。

`prod` profile 使用 JDBC 帳號表 `users` / `authorities`，啟動時以環境變數提供的帳號密碼建立或更新 BCrypt 雜湊；資料庫不保存明文密碼。`local` 與 `test` 才使用 In-Memory 帳號。

正式部署前請先執行前端專案的 `./backup-mysql.sh`，再啟動 API。Flyway migration 是 forward-only，若需回復必須使用部署前備份與上一版 image，不可修改已套用的 migration。
