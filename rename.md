# POS Change API Rename Notes

## Naming Principle

Backend names should describe the layer and purpose clearly:

- Entity: database row model.
- DTO: API or use-case payload.
- Request DTO: incoming request body.
- DAO: persistence access wrapper.
- Mapper: MyBatis SQL interface.
- Response envelope: only `ResponseBodyDto<T>`.

Avoid naming shared payload DTOs as `*Response` when they are not the response envelope.

## Layer Naming

### Controller

Controllers handle HTTP routing and response wrapping only.

Example:

- `PolicyChangeController`

### Service

Service must have an interface and implementation.

Examples:

- `PolicyChangeService`
- `PolicyChangeServiceImpl`

Business rules belong in the service implementation.

### DAO

DAO means persistence access object. It is not a request or response payload.

Examples:

- `PolicyChangeDao`
- `PosChangeDao`

DAO calls mapper methods and keeps SQL access out of the service.

### Mapper

Mapper means MyBatis mapper interface and XML SQL file.

Examples:

- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`

### Entity

Entity classes map table rows.

Examples:

- `MainPolicyMaster`
- `MainPolicyAddress`
- `MainPolicyRide`
- `PolicyChangeField`
- `PolicyChangeFile`
- `CodeDescription`

## DTO Naming

DTO classes are payload models used by the API or service layer.

Shared result DTOs:

- `PolicyDetailDto`
- `CreateChangeCaseDto`
- `AddressChangeDto`
- `MainAmountChangeDto`
- `PolicyChangeCaseDto`
- `UpdateChangeCaseStatusDto`

Request DTOs:

- `CreateChangeCaseRequest`
- `AddressChangeRequest`
- `MainAmountChangeRequest`
- `RiderAmountChangeListRequest`
- `RideAmountChangeRequest`
- `UpdateChangeCaseStatusRequest`
- `PosChangeRequest`

Response envelope:

- `ResponseBodyDto<T>`

## Prior Rename Decisions

These names should not come back as response-only classes:

- `PolicyDetailResponse`
- `CreateChangeCaseResponse`
- `AddressChangeResponse`

Use these shared names instead:

- `PolicyDetailDto`
- `CreateChangeCaseDto`
- `AddressChangeDto`

The earlier `PolicyDetailList` idea was resolved as shared DTO naming. It should not be limited to only API response usage.

## DAO vs DTO

DAO and DTO are different responsibilities:

- DAO: talks to database through mapper methods.
- DTO: carries request or result data between controller, service, and client.
- Entity: represents database table rows.

Example:

- `PosChangeDao` is DAO.
- `PosChangeRequest` is DTO because it is request data.
- `MainPolicyMaster` is entity because it maps a table row.

## Lombok Standard

DTO and entity classes should use Lombok to keep the code compact:

```java
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
```

Keep this standard for new DTOs and entities unless a framework-specific constructor or immutable model is intentionally needed.

## Business Key Naming

`policy_change_field.change_key` records the target row key when multiple rows can belong to one policy:

- Address change `001`: `change_key = address_type`.
- Main amount change `002`: `change_key = MASTER` for master and `change_key = 000` for the main ride row.
- Rider amount change `003`: `change_key = ride_order`.

This prevents applying a saved change to the wrong address or rider amount.

## Change Item Naming

Keep business codes as strings:

- `001`: address change.
- `002`: main policy insured amount change.
- `003`: rider insured amount change.

Do not replace these values with enum names in request or database payloads unless the database and front end are changed together.

## Status Naming

Keep acceptance statuses as uppercase strings:

- `P`: pending / 受理中.
- `S`: completed / 完成.
- `C`: cancelled / 取消.

Create flow only produces `P`.

Review flow is responsible for changing `P` to `S` or `C`.
