# POS Change API 命名整理

## 命名原則

後端命名要清楚表達所在層級與用途：

- Entity：資料庫資料列模型。
- DTO：join 其他欄位、畫面聚合資料、計算結果或操作結果。
- Request DTO：所有 `@RequestBody` 的傳入資料，不直接使用 Entity。
- DAO：資料存取封裝。
- Mapper：MyBatis SQL 介面。
- Response envelope：只使用 `ResponseBodyDto<T>`。

共用 payload DTO 不應命名成 `*Response`，除非它真的代表回覆外層格式。

## 分層命名

### Controller

Controller 只處理 HTTP 路由與回覆包裝。

範例：

- `PolicyChangeController`

### Service

Service 必須有介面與實作。

範例：

- `PolicyChangeService`
- `PolicyChangeServiceImpl`

商業規則放在 Service implementation。

### DAO

DAO 是資料存取物件，不是 request 或 response payload。

範例：

- `PolicyChangeDao`
- `PolicyChangeDaoImpl`
- `PosChangeDao`
- `PosChangeDaoImpl`

DAO 建議使用介面與實作分離。Service 依賴 DAO interface，DAO implementation 再呼叫 Mapper 方法，讓 Service 不直接碰 SQL 存取細節。

### Mapper

Mapper 是 MyBatis mapper 介面與 XML SQL 檔。

範例：

- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`

### Entity

Entity 對應資料表資料列。

範例：

- `MainPolicyMaster`
- `MainPolicyAddress`
- `MainPolicyRide`
- `PolicyChangeAcceptance`
- `PolicyChangeItem`
- `PolicyChangeField`
- `PolicyChangeFile`
- `CodeDescription`

## DTO 命名

DTO 是 API 或 Service 層使用的資料模型。

共用結果 DTO：

- `PolicyDetailDto`
- `CreateChangeCaseDto`
- `AddressChangeDto`
- `MainAmountChangeDto`
- `PolicyChangeCaseDto`
- `PostalCodeAreaDto`
- `UpdateChangeCaseStatusDto`

DTO 原則：

- 除非是 join 其他欄位、畫面聚合資料、計算結果或操作結果，否則不新增 DTO。
- `@RequestBody` 不直接使用 Entity，需建立 request DTO。
- API 回覆 data 若是一對一對應單一 SQL table row，可以直接使用 Entity，不再包裝 DTO。
- `ResponseBodyDto<T>` 只負責外層回覆格式，`data` 依上面規則放 Entity 或 DTO。

Request body 一律使用 `*Request` DTO：

- 產生案號：Body 使用 `CreateChangeCaseRequest`。
- 地址變更：案號使用 path variable，Body 使用 `AddressChangeRequest`。
- 主約保額變更：案號使用 path variable，Body 使用 `MainAmountChangeRequest`。
- 附約保額變更：案號、保單號碼、序號使用 path variable，Body 使用 `RiderAmountChangeListRequest`。
- 覆核狀態：案號使用 path variable，Body 使用 `UpdateChangeCaseStatusRequest`。
- POS change CRUD：Body 使用 `PosChangeRequest`。

回覆外層：

- `ResponseBodyDto<T>`

## 先前重新命名決策

以下名稱不應再作為 response-only class 回來：

- `PolicyDetailResponse`
- `CreateChangeCaseResponse`
- `AddressChangeResponse`

請改用共用名稱：

- `PolicyDetailDto`
- `CreateChangeCaseDto`
- `AddressChangeDto`

先前提到的 `PolicyDetailList` 已整理為共用 DTO 命名，不應限定成只有 API 回覆用途。

## DAO 與 DTO 區分

DAO 與 DTO 職責不同：

- DAO：透過 Mapper 與資料庫互動。
- DTO：在 Controller、Service 與 Client 之間傳遞 request 或 result data。
- Entity：代表資料庫資料表資料列。

範例：

- `PolicyChangeDao` 是 DAO interface。
- `PolicyChangeDaoImpl` 是 DAO implementation，注入 `PolicyChangeMapper`。
- `PolicyChangeMapper` 是 MyBatis mapper，由 MyBatis 產生代理實作，不手動 `implements` DAO。
- `PosChangeDao` / `PosChangeDaoImpl` 也遵守相同分工。
- `MainPolicyMaster` 是 Entity，因為它對應資料表資料列。
- `PosChangeRequest` 是 request DTO，對應 POS change CRUD 的 request body。

## Lombok 標準

DTO 與 Entity 應使用 Lombok 讓程式碼簡潔：

```java
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
```

新增 DTO 與 Entity 時維持此標準，除非有框架需求或刻意設計成不可變模型。

## 商業 Key 命名

`policy_change_field.change_key` 用來記錄目標資料列 key。當同一張保單有多筆相關資料時，靠它定位要回寫哪一筆：

- 地址變更 `001`：`change_key = address_type`。
- 主約保額變更 `002`：主檔用 `change_key = MASTER`，主約附約列用 `change_key = 000`。
- 附約保額變更 `003`：`change_key = ride_order`。

這可以避免把儲存的變更套用到錯誤地址或錯誤附約保額。

## API 與畫面註解

Controller 的每一支 API 方法上方都應保留簡短註解，標示該 API 對應的前端畫面或 Dialog，例如：

- 新增保全變更頁。
- 地址變更 Dialog。
- 查詢保全變更頁。
- 覆核頁。

註解重點是畫面對應與使用時機，不需要描述每一行程式做什麼。

## 地址與總保費命名

- `PostalCodeAreaDto.addressPrefix`：中文全型地址前綴。
- `PostalCodeAreaDto.halfWidthAddressPrefix`：英文半形地址前綴，來源為 `code_description.code_description`。
- `main_policy_master.premium`：總保費，不是可直接手動修改的主檔保費。
- `main_policy_ride.premium`：主附約各列保費，總保費由這些資料列加總回寫。

## 變更項目命名

商業代碼維持字串：

- `001`：地址變更。
- `002`：主約保額變更。
- `003`：附約保額變更。

除非資料庫與前端一起調整，否則不要在 request 或資料庫 payload 中改成 enum 名稱。

## 狀態命名

受理狀態維持大寫字串：

- `P`：受理中。
- `S`：完成。
- `C`：取消。

新增流程只產生 `P`。

覆核流程負責將 `P` 改為 `S` 或 `C`。
