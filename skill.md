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
- DAO / Mapper：封裝 MyBatis 呼叫與 SQL 存取。

主要流程類別：

- `PolicyChangeController`
- `PolicyChangeService`
- `PolicyChangeServiceImpl`
- `PolicyChangeDao`
- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`

## API 回覆格式

所有 API 回覆都應透過 `ResponseUtil` 包成 `ResponseBodyDto<T>`。

傳入的 request body 使用一般 Request DTO，不需要包 `ResponseBodyDto`。

重要範例：

- `ResponseBodyDto<PolicyDetailDto>`
- `ResponseBodyDto<CreateChangeCaseDto>`
- `ResponseBodyDto<AddressChangeDto>`
- `ResponseBodyDto<MainAmountChangeDto>`
- `ResponseBodyDto<List<PolicyChangeCaseDto>>`
- `ResponseBodyDto<UpdateChangeCaseStatusDto>`

## 主要 API

- `GET /api/policies/{policyNo}/{policySeq}`：查詢保單主檔、通訊地址、全部地址資料、附約資料與變更項目代碼。
- `POST /api/change-cases`：只產生變更案號。狀態為 `P`，但尚未寫入受理資料，需等真的有異動資料時才存檔。
- `POST /api/change-cases/address-change`：儲存 `001` 地址變更欄位與變更前後快照。
- `POST /api/change-cases/main-amount-change`：儲存 `002` 主約保額變更。
- `POST /api/change-cases/rider-amount-change`：儲存 `003` 附約保額變更。
- `GET /api/policies/{policyNo}/change-cases`：依保單號碼查詢保全受理資料。
- `PATCH /api/change-cases/{changeCaseNo}/status`：覆核動作。只有這支 API 可以將 `P` 改為 `S` 或 `C`。

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
