# POS Change API Skill

## Purpose

`pos-change-api` is the Spring Boot backend for the POS policy-change workflow. It handles policy lookup, change-case number generation, pending change recording, and review-time application of approved changes.

## Stack

- Java 17
- Spring Boot
- MyBatis
- MySQL
- Lombok
- Maven
- Docker

## Architecture

The backend follows a three-layer structure:

- Controller: receives HTTP requests and returns `ResponseBodyDto<T>`.
- Service interface and implementation: contains business rules and transaction control.
- DAO / Mapper: wraps MyBatis mapper calls and SQL access.

Primary flow classes:

- `PolicyChangeController`
- `PolicyChangeService`
- `PolicyChangeServiceImpl`
- `PolicyChangeDao`
- `PolicyChangeMapper`
- `PolicyChangeMapper.xml`

## API Response Contract

All API responses should be wrapped by `ResponseBodyDto<T>` through `ResponseUtil`.

Request bodies are normal request DTOs and are not wrapped in `ResponseBodyDto`.

Important examples:

- `ResponseBodyDto<PolicyDetailDto>`
- `ResponseBodyDto<CreateChangeCaseDto>`
- `ResponseBodyDto<AddressChangeDto>`
- `ResponseBodyDto<MainAmountChangeDto>`
- `ResponseBodyDto<List<PolicyChangeCaseDto>>`
- `ResponseBodyDto<UpdateChangeCaseStatusDto>`

## Main Endpoints

- `GET /api/policies/{policyNo}/{policySeq}`: query policy master, communication address, all address rows, ride rows, and change-item codes.
- `POST /api/change-cases`: generate a case number only. The case is pending `P` but is not saved until a real change is saved.
- `POST /api/change-cases/address-change`: save address change fields and change-file snapshot for item `001`.
- `POST /api/change-cases/main-amount-change`: save main policy amount changes for item `002`.
- `POST /api/change-cases/rider-amount-change`: save rider amount changes for item `003`.
- `GET /api/policies/{policyNo}/change-cases`: query acceptance cases by policy number.
- `PATCH /api/change-cases/{changeCaseNo}/status`: review action. Only this endpoint can change `P` to `S` or `C`.

## Business Rules

### Case Number

Case number format:

```text
C + 民國年 3 碼 + 月 2 碼 + 日 2 碼 + 流水號至少 3 碼
```

Example:

```text
C1150629001
```

The service uses Taipei date for the prefix.

### Status

- `P`: pending / 受理中.
- `S`: completed / 完成.
- `C`: cancelled / 取消.

New change cases are `P`.

Only review can update:

- `P` to `S`: apply saved changes to master/address/ride tables.
- `P` to `C`: cancel without applying saved changes.

### 001 Address Change

- Uses `main_policy_address`.
- The front end may display all address-related rows for the policy.
- Backend stores changed fields in `policy_change_field`.
- Backend stores before/after address snapshots in `policy_change_file`.
- `policy_change_field.change_key` stores `address_type`.

### 002 Main Policy Amount Change

- Changes the master insured amount.
- Must record both:
  - `main_policy_master.insured_amount` with `change_key = MASTER`
  - `main_policy_ride.000.insured_amount` with `change_key = 000`
- On approval `S`, both master and the matching main ride row are updated together.

### 003 Rider Amount Change

- Changes rider insured amount.
- Must not change the main contract row.
- Rows with `ride_type = 1` or `ride_order = 000` are treated as main contract rows.
- `policy_change_field.change_key` stores `ride_order` so the correct rider row is updated.

## Database Tables

Core business tables:

- `main_policy_master`
- `main_policy_address`
- `main_policy_ride`
- `policy_change_acceptance`
- `policy_change_item`
- `policy_change_field`
- `policy_change_file`
- `code_description`

`policy_change_field.change_key` is important for locating the target row when more than one row can share the same policy number and sequence.

## Local Run

```bash
DB_PASSWORD=12345678 mvn spring-boot:run
```

Backend default port:

```text
8081
```

Database connection can be controlled by environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## Verification

```bash
DB_PASSWORD=12345678 mvn test
docker build -t pos-change-api:latest .
```

## Docker

The Docker image builds the Maven jar in one stage and runs it with Java 17 JRE in the runtime stage.

Runtime container still needs database environment variables, for example:

```bash
docker run --rm -p 8081:8081 \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/main?serverTimezone=Asia/Taipei&characterEncoding=utf-8' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=12345678 \
  pos-change-api:latest
```
