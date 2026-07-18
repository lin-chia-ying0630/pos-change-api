# POS Change API 開發規範

## 目的

本檔提供後續修改 `pos-change-api` 時必須維持的架構與交易規則。重新設計流程、資料表或分層後，需同步更新本檔與 `readme.md`。

## 技術棧

- Java 17、Spring Boot、Spring Security。
- MyBatis、MySQL、Flyway。
- Bean Validation、Lombok。
- JUnit 5、Mockito、Testcontainers。
- Maven、Docker、GitHub Actions。

## 三層架構

### Controller

- 只處理路由、path/body 參數、`@Valid`、`@Validated` 與回覆包裝。
- 所有回覆包含 Spring Security 的 `401/403`，都必須符合 `ResponseBodyDto<T>`。
- 每支 API 保留一行中文註解，說明對應畫面與使用時機。

### Service

- 每個 use case 都有 interface 與 implementation。
- `PolicyChangeServiceImpl` 只做 facade 委派，不重複商業邏輯。
- 目前 use case：Query、Draft、Address Save、Amount Save、Review、Apply。
- 跨多張表的儲存或覆核必須使用 `@Transactional`。
- 正規化與純欄位差異比對放 `PolicyChangeFieldUtil`。

### DAO

- `PolicyChangeDao` 是 MyBatis mapper interface，由 MyBatis 直接建立代理。
- SQL 放在 `PolicyChangeDao.xml`，namespace 必須是 `com.alin.lin.dao.PolicyChangeDao`。
- 不建立只逐項轉呼叫的 `PolicyChangeDaoImpl` 或另一份 Mapper interface。
- 寫入方法回傳 affected row count；狀態轉換與重要更新必須檢查筆數。
- 單表 row 使用 Entity；join、聚合、畫面組合或操作結果才使用 DTO。

## DTO 與 Entity

- 每張 SQL table 都要有一個中文註解完整的 Entity。
- `@RequestBody` 一律使用 `*Request` DTO，不直接使用 Entity。
- Request 必填欄位使用 Bean Validation。
- DTO 與 Entity 預設使用：

```java
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
```

- API 外層只使用 `ResponseBodyDto<T>`；request 不包回覆外層。

## 案號

- 格式：`C + 民國年 + 月日 + 至少三碼流水號`。
- 必須由 `policy_change_case_sequence` 使用單一原子 `INSERT ... ON DUPLICATE KEY UPDATE` 與 connection-local `LAST_INSERT_ID()` 取號。
- 禁止使用 JVM 記憶體、自增欄位快取或只查 `MAX(change_case_no)`。
- 取號不建立受理檔，只有真的異動才建立 `P` 草稿。
- 取號前必須依 `policy_no + policy_seq + change_item` 查最近一筆已受理案件；最近狀態為 `P` 時回覆 HTTP 409 與「此保單正在受理中，無法申請」。eligibility API 與建案 Service 必須執行相同規則。
- 取號必須新增一筆 `policy_change_case_reservation`，並以 `policy_change_case_reservation_item` 保存一至多個勾選項目；儲存前驗證保單、擁有者、期限與項目是否在預約清單。
- 同一案號只能有一筆 `policy_change_acceptance`，但可以依序新增多筆 `policy_change_item`；第二項儲存不得再次建立受理檔或拒絕既有案號。
- 流水號不可限制在 `999`；`String.format("%03d", serial)` 只定義最小寬度。

## 草稿

- `policy_change_field` 的有效草稿鍵為案號、項目、欄位、`change_key`。
- `policy_change_file` 的有效草稿鍵為案號、項目、檔案、`change_key`。
- 同一目標重複儲存要替換最新草稿，不能累積多筆有效資料。
- 使用者改回主檔原值時要刪除該目標草稿。
- 項目已無欄位或檔案時刪除 `policy_change_item`；案件已無項目時刪除 `P` 受理資料。
- 若未來需要編輯歷程，另建立 revision/history，不可把歷程當成目前有效草稿。

## 覆核

- 完成流程固定為 `P -> A -> 套用 -> S`，取消為 `P -> C`。
- `P -> A/C` 必須使用條件更新並檢查 affected row count，避免重複覆核。
- 套用前以 `SELECT ... FOR UPDATE` 鎖定資料列，再確認目前值仍等於草稿 `content_before`。
- 主附約一律先鎖主檔、再依 `ride_order` 鎖附約；地址依 `change_key` 排序鎖定，禁止反向鎖順序。
- 每個主檔、地址或附約 UPDATE 都必須檢查 affected row count 為 1。
- 主檔已被其他案件修改時拋出 `ChangeCaseConflictException` 並回覆 HTTP 409。
- 覆核清單之外必須提供案件明細 API，包含 `changeFields` 與 `changeFiles`。
- 完成、主檔套用、總保費重算與狀態更新必須在同一交易內。
- 受理檔必須保存 `created_by`、`reviewed_by`、`reviewed_at`；啟用 Security 時禁止建檔人覆核自己的案件。

## 變更項目

### 001 地址與聯絡資料

- `01/02` 使用郵遞區號與地址。
- 其他型態使用 `email / 電話 / 手機`。
- 地址快照 `policy_change_file.change_key = address_type`。
- 地址欄位 `policy_change_field.change_key = address_type`。
- 未修改或改回原值時 `changedFieldCount = 0`，且不可保留舊草稿。

### 002 主約保額

- 案件清單聚合變更項目時必須去重。
- `main_policy_master` 不保存主約險種、年期與保額。
- 002 只記錄並更新 `main_policy_ride` 的主約列，`ride_order = 000`、`change_key = 000`。
- 002 同時保存 `main_policy_ride` 完整資料列快照；欄位紀錄負責套用，檔案快照負責查詢呈現。
- 查詢明細需將 snake_case 或複合欄位名稱轉為對應 JSON key，再由 `CHT-code` 補入 `changeFields.chineseName`。
- `changedFieldCount` 回傳業務異動數 `1`。

### 003 附約保額

- request 必須包含 `rideOrder`。
- `ride_type = 1` 或 `ride_order = 000` 不可由 003 修改。
- 每筆欄位的 `change_key = rideOrder`。
- request 內不可有重複 `rideOrder`。

## CodeDescription

- 地址型態、變更項目、受理狀態與主附約型態由 `code_description` 管理。
- Java 判斷使用穩定 code key，不使用中文描述。
- 覆核快照欄位名稱使用 `codeGroup=CHT-code`、`codeField=JSON key`、`codeBefore=中文名稱`；案件明細 API 將 JSON 拆成 `snapshotFields`，逐欄回傳中文名稱與異動前後值。
- `CodeTable` 定義 group/field；`CodeDescriptionMeaning` 定義程式需要的穩定 key。
- 欄位名稱、regex、組合與解析規則才放 enum。

## Security 與設定

- CORS origin 從 `pos.cors.allowed-origins`／`CORS_ALLOWED_ORIGINS` 取得，不可硬寫在 Controller。
- Security 採 fail-closed，所有 profile 預設開啟；只有 `local`／`test` 可明確關閉，其他 profile 關閉時必須拒絕啟動。
- `prod` 必須設定 `pos.security.require-https=true`，並由可信任反向代理傳入 HTTPS forwarding header。
- MAKER 可新增與儲存；REVIEWER 才能覆核。
- MAKER 只能修改與查看自己建立的案件，REVIEWER 可查看覆核清單；Service 層也必須重做案件擁有者檢查。
- 經辦與覆核帳號不可相同，密碼至少 12 個字元。
- DB 與帳號密碼只能由環境變數、Docker secret 或 K8s Secret 提供，程式不得有正式預設密碼。
- K8s 使用 Actuator liveness/readiness endpoint。
- Docker runtime 必須非 root；應使用唯讀 root filesystem、`no-new-privileges` 與最小 Linux capabilities。
- Docker 建置與執行映像必須固定 digest；升級時同步掃描弱點並更新 digest。

## Flyway

- 禁止重新加入單一 `schema.sql` 當正式升版工具。
- 已發布 migration 不可修改；新結構建立下一個 `Vn__description.sql`。
- 示範保單放 `db/local`，不可放正式 migration。
- 新增資料表時同步新增 Entity、DAO SQL、測試及 README 資料流說明。

## 測試

- 一般單元測試不得依賴開發者本機 MySQL。
- IntelliJ 本機 Debug 使用 `.run/POS Change API Local.run.xml`，必須啟用 `local` profile，避免 `db/local` migration 在同一資料庫被 Flyway 判定為 missing。
- Controller/Security slice 至少驗證未登入、角色越權、MAKER/REVIEWER 正常流程與 CORS properties。
- SQL、Flyway、交易與併發流程使用 MySQL Testcontainers。
- 至少覆蓋：原子案號、無異動、重複儲存、改回原值、P/S/C、409 衝突、001/002/003。
- Docker 不可用時整合測試可略過；CI 必須在 Docker 可用環境完整執行。
- 修改 SQL 遮罩規則時同步補 `MaskedSqlLogInterceptorTest`。
- CI 必須執行 CodeQL、OSV、Docker build；Maven、Docker 與 GitHub Actions 由 Dependabot 定期更新。

## 驗證指令

```bash
mvn test
mvn clean verify
docker build -t pos-change-api:latest .
```

- Docker Maven 層使用 BuildKit cache；不要以 `dependency:go-offline` 預抓未使用的 dependency management BOM。
# Deployment rules

- Production uses JDBC-backed `users` and `authorities`; local/test may use in-memory users.
- Never commit passwords, password hashes, `.env` files, or database backups.
- Back up MySQL before every Flyway migration and rehearse V6/V7 against a sanitized copy.
- Keep database constraints and transactional status transitions as the source of truth for concurrent cases.
