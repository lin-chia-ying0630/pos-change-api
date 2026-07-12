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

## 變更項目

### 001 地址與聯絡資料

- `01/02` 使用郵遞區號與地址。
- 其他型態使用 `email / 電話 / 手機`。
- 地址快照 `policy_change_file.change_key = address_type`。
- 地址欄位 `policy_change_field.change_key = address_type`。
- 未修改或改回原值時 `changedFieldCount = 0`，且不可保留舊草稿。

### 002 主約保額

- 同時記錄主檔保額，`change_key = MASTER`。
- 同時記錄主約列 `ride_order = 000`，`change_key = 000`。
- 覆核時兩筆需一起成功或一起 rollback。

### 003 附約保額

- request 必須包含 `rideOrder`。
- `ride_type = 1` 或 `ride_order = 000` 不可由 003 修改。
- 每筆欄位的 `change_key = rideOrder`。
- request 內不可有重複 `rideOrder`。

## CodeDescription

- 地址型態、變更項目、受理狀態與主附約型態由 `code_description` 管理。
- Java 判斷使用穩定 code key，不使用中文描述。
- `CodeTable` 定義 group/field；`CodeDescriptionMeaning` 定義程式需要的穩定 key。
- 欄位名稱、regex、組合與解析規則才放 enum。

## Security 與設定

- CORS origin 從 `pos.cors.allowed-origins`／`CORS_ALLOWED_ORIGINS` 取得，不可硬寫在 Controller。
- `local` 由 `POS_SECURITY_ENABLED` 決定且預設關閉；`prod` 必須開啟 `pos.security.enabled=true`。
- MAKER 可新增與儲存；REVIEWER 才能覆核。
- DB 與帳號密碼只能由環境變數、Docker secret 或 K8s Secret 提供，程式不得有正式預設密碼。
- K8s 使用 Actuator liveness/readiness endpoint。

## Flyway

- 禁止重新加入單一 `schema.sql` 當正式升版工具。
- 已發布 migration 不可修改；新結構建立下一個 `Vn__description.sql`。
- 示範保單放 `db/local`，不可放正式 migration。
- 新增資料表時同步新增 Entity、DAO SQL、測試及 README 資料流說明。

## 測試

- 一般單元測試不得依賴開發者本機 MySQL。
- Controller/Security slice 至少驗證未登入、角色越權、MAKER/REVIEWER 正常流程與 CORS properties。
- SQL、Flyway、交易與併發流程使用 MySQL Testcontainers。
- 至少覆蓋：原子案號、無異動、重複儲存、改回原值、P/S/C、409 衝突、001/002/003。
- Docker 不可用時整合測試可略過；CI 必須在 Docker 可用環境完整執行。
- 修改 SQL 遮罩規則時同步補 `MaskedSqlLogInterceptorTest`。

## 驗證指令

```bash
mvn test
mvn clean verify
docker build -t pos-change-api:latest .
```

- Docker Maven 層使用 BuildKit cache；不要以 `dependency:go-offline` 預抓未使用的 dependency management BOM。
