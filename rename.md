# POS Change API 命名整理

## 命名原則

後端命名要清楚表達所在層級與用途：

- Entity：資料庫資料列模型。
- DTO：API 或 use case 的資料承載物件。
- Request DTO：傳入 request body。
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
- `PosChangeDao`

DAO 呼叫 Mapper 方法，讓 Service 不直接碰 SQL 存取細節。

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
- `UpdateChangeCaseStatusDto`

Request DTO：

- `CreateChangeCaseRequest`
- `AddressChangeRequest`
- `MainAmountChangeRequest`
- `RiderAmountChangeListRequest`
- `RideAmountChangeRequest`
- `UpdateChangeCaseStatusRequest`
- `PosChangeRequest`

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

- `PosChangeDao` 是 DAO。
- `PosChangeRequest` 是 DTO，因為它是 request data。
- `MainPolicyMaster` 是 Entity，因為它對應資料表資料列。

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
