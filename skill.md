# POS Change API 技能說明

## 目的

`pos-change-api` 是 POS 保全變更流程的 Spring Boot 後端。負責保單查詢、變更案號產生、保全受理資料暫存，以及覆核完成時將核准的變更回寫到主檔、地址或附約資料。

## 技術棧

- Java 17
- Spring Boot
- MyBatis
- MySQL
- Lombok
- Maven
- Docker

## 架構

後端採三層架構：

- Controller：接收 HTTP 請求，並統一回傳 `ResponseBodyDto<T>`。
- Service 介面與實作：處理商業規則與交易控制。
- DAO 介面與實作：Service 依賴 DAO interface，DAO implementation 封裝資料存取。
- Mapper：MyBatis mapper 介面，搭配 XML SQL，由 MyBatis 產生代理實作，不手動 `implements` DAO。

MyBatis 對應規則：

- Entity 或 DTO 查詢優先使用 `resultType` 直接對應回傳類別。
- 專案已開啟 `map-underscore-to-camel-case`，SQL 欄位例如 `code_description` 會自動對應 Java 欄位 `codeDescription`。
- join、別名欄位或聚合資料若 SQL alias 已對應 DTO 欄位，也使用 `resultType`。
- 只有 `resultType` 無法表達特殊巢狀、複合物件或自訂對應時，才使用 `resultMap`。

主要流程類別：

- `PolicyChangeController`
- `PolicyChangeService`
- `PolicyChangeServiceImpl`
- `PolicyChangeDao`
- `PolicyChangeDaoImpl`
- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`
- `PolicyChangeFieldUtil`

欄位正規化、差異比對與格式檢核放在 `PolicyChangeFieldUtil`，避免 `PolicyChangeServiceImpl` 混入過多細節。

## API 回覆格式

所有 API 回覆都應透過 `ResponseUtil` 包成 `ResponseBodyDto<T>`。

傳入的 request body 不需要包 `ResponseBodyDto`。

DTO 使用規則：

- 除非是 join 其他欄位、畫面聚合資料、計算結果或操作結果，否則不新增 DTO。
- `@RequestBody` 不直接使用 Entity，需建立 request DTO。
- `@RequestBody` 參數需加 `@Valid`，Controller 類別需加 `@Validated`。
- Request DTO 必填欄位需使用 Bean Validation，例如 `@NotBlank`、`@NotNull`、`@NotEmpty`。
- API 回覆 data 若是一對一對應單一 SQL table row，可以直接使用 Entity。
- 每一張 SQL table 都應有一個對應 Entity。
- Entity 欄位應補中文註解，說明欄位業務意義。
- DTO 欄位應補中文註解，說明 request 或 response 的資料意義。

重要範例：

- `ResponseBodyDto<PolicyDetailDto>`
- `ResponseBodyDto<CreateChangeCaseDto>`
- `ResponseBodyDto<AddressChangeDto>`
- `ResponseBodyDto<MainAmountChangeDto>`
- `ResponseBodyDto<List<PolicyChangeCaseDto>>`
- `ResponseBodyDto<UpdateChangeCaseStatusDto>`

## Spring Security 與 CORS

跨域設定統一由 `SecurityConfig` 處理，不在 Controller 使用 `@CrossOrigin`。

- `CorsConfigurationSource` 集中維護前端允許來源。
- `SecurityFilterChain` 啟用 CORS。
- 目前 API 未做登入驗證，`/api/**` 維持 `permitAll`。
- 前後端分離 API 關閉 CSRF。

## 主要 API

| API | 對應畫面 | 用途 |
| --- | --- | --- |
| `GET /api/policies/{policyNo}/{policySeq}` | 新增保全變更頁 | 查詢保單主檔、通訊地址、全部地址資料、主附約資料與變更項目代碼。 |
| `GET /api/postal-codes/{postalCode}` | 新增保全變更頁的地址變更 Dialog | 依郵遞區號前三碼或 3+3 郵遞區號取得中文地址前綴。 |
| `POST /api/change-cases` | 新增保全變更頁的產生案號按鈕 | 只產生變更案號。狀態為 `P`，但尚未寫入受理資料，需等真的有異動資料時才存檔。 |
| `POST /api/change-cases/{changeCaseNo}/address-change` | 新增保全變更頁的 `001` 地址變更 Dialog | Body 使用 `AddressChangeRequest` 儲存地址變更欄位與變更前後快照。 |
| `POST /api/change-cases/{changeCaseNo}/main-amount-change` | 新增保全變更頁的 `002` 主約保額變更 Dialog | Body 使用 `MainAmountChangeRequest` 儲存主約保額變更。 |
| `POST /api/change-cases/{changeCaseNo}/policies/{policyNo}/{policySeq}/rider-amount-change` | 新增保全變更頁的 `003` 附約保額變更 Dialog | Body 使用 `RiderAmountChangeListRequest` 儲存附約保額變更。 |
| `GET /api/policies/{policyNo}/change-cases` | 查詢保全變更頁與覆核頁 | 依保單號碼查詢保全受理資料。 |
| `PATCH /api/change-cases/{changeCaseNo}/status` | 覆核頁 | 覆核動作。只有這支 API 可以將 `P` 改為 `S` 或 `C`。 |

## 商業規則

### CodeDescription 與固定規則

`code_description` 已管理的業務代碼不再另外建立 enum，Service 透過 `CodeDescription` 取得 `code_before`。

由 `CodeDescription` 管理：

- 地址型態。
- 保全變更項目。
- 受理狀態。
- 附約型態。

主要 enum 與 properties：

- `CodeTable`：共用代碼檔查詢 group/field，不存放實際代碼值。
- `CodeDescriptionMeaning`：定義系統要查找的穩定代碼 key，例如 `CHANGE_ITEM/001`，不依賴中文 `code_description`。
- `PolicyChangeFieldName`：保全異動欄位名稱與主檔可回寫欄位白名單。
- `RideChangeField`：主附約保額、保費欄位的組合與解析規則。
- `PolicyRideKey`：主約列附約序號 key。
- `PosChangeProperties`：只讀取環境相關設定，目前為案號日期時區。
- `PostalCodeRule`：郵遞區號檢核規則。

新增固定業務代碼時先補 `code_description`；程式判斷使用穩定代碼 key，不使用中文描述當查詢條件。只有欄位白名單、regex、欄位組合規則這類非 code table 資料才放 enum。

### 案號規則

案號格式：

```text
C + 民國年 3 碼 + 月 2 碼 + 日 2 碼 + 流水號至少 3 碼
```

範例：

```text
C1150629001
```

服務依 `pos.change-case.zone-id` 設定的時區日期產生案號前綴，預設值由 `CHANGE_CASE_ZONE_ID` 環境變數控制，未設定時使用 `Asia/Taipei`。

### 受理狀態

- `P`：受理中。
- `S`：完成。
- `C`：取消。

新增保全變更只會產生 `P`。

只有覆核可以更新狀態：

- `P` 改 `S`：將已儲存的變更回寫到主檔、地址或附約資料。
- `P` 改 `C`：取消，不回寫變更。

### 001 地址變更

- 使用 `main_policy_address`。
- 前端可顯示該保單關聯的所有地址資料，不限定固定三筆。
- `address_type = 01/02` 為地址資料：郵遞區號前三碼必填，後三碼可空白或 `NULL`；地址必填；`email / 電話 / 手機` 欄位鎖住且不列入異動。
- `address_type = 11/12/31` 為聯絡資料：`email / 電話 / 手機` 必填；郵遞區號與地址可空白且不列入異動。
- 聯絡資料若歷史資料同時存在 `full_width_address` 與 `half_width_address`，後端以畫面顯示的有效聯絡值判斷是否異動；畫面未修改時不可建立異動欄位。
- `code_description` 的 `postal-code / zip_code3`：
  - `code_after` 存中文縣市區，例如 `臺北市|中正區`。
  - `code_description` 存英文地址前綴，例如 `Zhongzheng Dist., Taipei City`。
- 郵遞區號變更會帶入中文地址前綴，使用者需重新補完整地址。
- 後端將異動欄位寫入 `policy_change_field`。
- 後端將地址變更前後快照寫入 `policy_change_file`。
- `policy_change_field.change_key` 存放 `address_type`。

### 002 主約保額變更

- 變更主檔保額。
- 必須同時記錄：
  - `main_policy_master.insured_amount`，`change_key = MASTER`
  - `main_policy_ride.000.insured_amount`，`change_key = 000`
- 覆核完成 `S` 時，主檔與對應的主約附約列要一起回寫。

### 003 附約保額變更

- 變更附約保額。
- 不可變更主約列。
- `ride_type = 1` 或 `ride_order = 000` 視為主約列。
- `policy_change_field.change_key` 存放 `ride_order`，避免回寫到錯誤附約。

### 總保費

- 保單主檔的 `premium` 視為總保費，畫面不可直接修改。
- 主附約檔 `main_policy_ride.premium` 若在覆核完成時有異動，後端需將同一保單序號下全部 `main_policy_ride.premium` 加總回寫到 `main_policy_master.premium`。
- 一般主檔欄位回寫不可直接更新 `premium`。

## 資料表

核心商業資料表：

- `main_policy_master`
- `main_policy_address`
- `main_policy_ride`
- `policy_change_acceptance`
- `policy_change_item`
- `policy_change_field`
- `policy_change_file`
- `code_description`

`policy_change_field.change_key` 很重要，當同一張保單與序號下有多筆目標資料時，用它定位實際要修改的資料列。

## 本機啟動

```bash
DB_PASSWORD=12345678 mvn spring-boot:run
```

後端預設 port：

```text
8081
```

資料庫連線可用環境變數控制：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
CHANGE_CASE_ZONE_ID
```

## 驗證

```bash
DB_PASSWORD=12345678 mvn test
docker build -t pos-change-api:latest .
```

`PolicyChangeFieldUtilTest` 應覆蓋異動欄位判斷：

- `zipCode2` 的 `01` 與 `001` 視為相同。
- 地址比對忽略空白，並將全形英數視為半形英數。
- 金額比對忽略小數位 scale，例如 `1000000.00` 與 `1000000`。
- 郵遞區號前三碼必填，後三碼可空白；若填寫需為 3 碼。

`MaskedSqlLogInterceptorTest` 應覆蓋 SQL 參數遮罩：

- email 只保留第一碼與 domain。
- `tel` / `phone` / `mobile` / `cell` 電話與手機保留前三碼與後三碼。
- `policyNo` / `policy_no` 保單號碼保留前三碼與後三碼。
- `fullWidthAddress` / `halfWidthAddress` / `address` / `add` 地址保留前六個字。
- 一般文字需遮中段。
- 數字型欄位可原樣顯示。

## SQL Log 與個資遮罩

不要開啟 MyBatis `StdOutImpl`，因為它會原樣印出 `Parameters`：

```properties
mybatis.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl
# mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
logging.level.com.alin.lin.mapper=info
```

Debug console 需要 SQL 時，統一使用專案的 MyBatis interceptor：

```properties
logging.level.com.alin.lin.interceptor.MaskedSqlLogInterceptor=debug
```

`MaskedSqlLogInterceptor` 只在 debug level 開啟時輸出，內容包含 SQL id、SQL 與遮罩後參數。新增敏感資料型態時，先補遮罩規則與測試。

`logback-spring.xml` 需保留 console 與 rolling file appender：

- 目前 log：`logs/pos-change-api.log`。
- 歷史 log：`logs/pos-change-api.yyyy-MM-dd.i.log.gz`。
- 每天切檔，單檔 100MB，保留 30 天，總容量 3GB。
- 可用 `LOG_PATH` 環境變數改存放位置。

## Docker

Docker image 使用多階段建置：第一階段用 Maven 建 jar，第二階段用 Java 17 JRE 執行。

容器執行時仍需要資料庫環境變數，例如：

```bash
docker run --rm -p 8081:8081 \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/main?serverTimezone=Asia/Taipei&characterEncoding=utf-8' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=12345678 \
  -e CHANGE_CASE_ZONE_ID=Asia/Taipei \
  pos-change-api:latest
```
