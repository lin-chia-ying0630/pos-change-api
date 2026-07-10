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

主要流程類別：

- `PolicyChangeController`
- `PolicyChangeService`
- `PolicyChangeServiceImpl`
- `PolicyChangeDao`
- `PolicyChangeDaoImpl`
- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`

## API 回覆格式

所有 API 回覆都應透過 `ResponseUtil` 包成 `ResponseBodyDto<T>`。

傳入的 request body 不需要包 `ResponseBodyDto`。

DTO 使用規則：

- 除非是 join 其他欄位、畫面聚合資料、計算結果或操作結果，否則不新增 DTO。
- `@RequestBody` 不直接使用 Entity，需建立 request DTO。
- API 回覆 data 若是一對一對應單一 SQL table row，可以直接使用 Entity。
- 每一張 SQL table 都應有一個對應 Entity。
- Entity 欄位應補中文註解，說明欄位業務意義。

重要範例：

- `ResponseBodyDto<PolicyDetailDto>`
- `ResponseBodyDto<CreateChangeCaseDto>`
- `ResponseBodyDto<AddressChangeDto>`
- `ResponseBodyDto<MainAmountChangeDto>`
- `ResponseBodyDto<List<PolicyChangeCaseDto>>`
- `ResponseBodyDto<UpdateChangeCaseStatusDto>`

## 主要 API

| API | 對應畫面 | 用途 |
| --- | --- | --- |
| `GET /api/policies/{policyNo}/{policySeq}` | 新增保全變更頁 | 查詢保單主檔、通訊地址、全部地址資料、主附約資料與變更項目代碼。 |
| `GET /api/postal-codes/{postalCode}` | 新增保全變更頁的地址變更 Dialog | 依郵遞區號前三碼或 3+3 郵遞區號取得中文全型地址前綴與英文半形地址前綴。 |
| `POST /api/change-cases` | 新增保全變更頁的產生案號按鈕 | 只產生變更案號。狀態為 `P`，但尚未寫入受理資料，需等真的有異動資料時才存檔。 |
| `POST /api/change-cases/{changeCaseNo}/address-change` | 新增保全變更頁的 `001` 地址變更 Dialog | Body 使用 `AddressChangeRequest` 儲存地址變更欄位與變更前後快照。 |
| `POST /api/change-cases/{changeCaseNo}/main-amount-change` | 新增保全變更頁的 `002` 主約保額變更 Dialog | Body 使用 `MainAmountChangeRequest` 儲存主約保額變更。 |
| `POST /api/change-cases/{changeCaseNo}/policies/{policyNo}/{policySeq}/rider-amount-change` | 新增保全變更頁的 `003` 附約保額變更 Dialog | Body 使用 `RiderAmountChangeListRequest` 儲存附約保額變更。 |
| `GET /api/policies/{policyNo}/change-cases` | 查詢保全變更頁與覆核頁 | 依保單號碼查詢保全受理資料。 |
| `PATCH /api/change-cases/{changeCaseNo}/status` | 覆核頁 | 覆核動作。只有這支 API 可以將 `P` 改為 `S` 或 `C`。 |

## 商業規則

### 案號規則

案號格式：

```text
C + 民國年 3 碼 + 月 2 碼 + 日 2 碼 + 流水號至少 3 碼
```

範例：

```text
C1150629001
```

服務使用台北時區日期產生案號前綴。

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
- 郵遞區號前三碼必填，後三碼可空白或 `NULL`；若後三碼有填寫，需滿 3 碼。
- `code_description` 的 `postal-code / zip_code3`：
  - `code_after` 存中文縣市區，例如 `臺北市|中正區`。
  - `code_description` 存英文地址前綴，例如 `Zhongzheng Dist., Taipei City`。
- 郵遞區號變更會帶入中文全型地址前綴與英文半形地址前綴，使用者需重新補完整地址。
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
```

## 驗證

```bash
DB_PASSWORD=12345678 mvn test
docker build -t pos-change-api:latest .
```

## Docker

Docker image 使用多階段建置：第一階段用 Maven 建 jar，第二階段用 Java 17 JRE 執行。

容器執行時仍需要資料庫環境變數，例如：

```bash
docker run --rm -p 8081:8081 \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/main?serverTimezone=Asia/Taipei&characterEncoding=utf-8' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=12345678 \
  pos-change-api:latest
```
