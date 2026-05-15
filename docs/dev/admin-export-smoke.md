# M14A Admin Export Backend Smoke

## Scope

M14A implements the minimal backend Excel export capability only.

Implemented:

- `GET /api/admin/exports/finance`
- `GET /api/admin/exports/reimbursements`

Not implemented:

- admin-web export page integration
- mini-program export entry
- async export task center
- long-term file storage
- custom Excel templates
- BI reports
- multi-store aggregation

## Finance Export

Endpoint:

```http
GET /api/admin/exports/finance?reportType=DAILY&date=2026-05-15
X-User-Id: 1
X-Store-Id: 1
```

Supported `reportType` values:

- `DAILY`: uses `date`, defaults to today when omitted.
- `MONTHLY`: uses `month` in `yyyy-MM` format.
- `RANGE`: uses `startDate` and `endDate` in `yyyy-MM-dd` format.

Exported metrics:

- `customerIncome` = payment - refund
- `officialIncome` = official after-sales records with `official_settlement_status = SETTLED`
- `partsCost` = settled work order part line cost
- `reimbursementCost` = confirmed reimbursement amount
- `totalIncome`
- `totalCost`
- `profit`
- `settledWorkOrderCount`
- `confirmedReimbursementCount`

## Reimbursement Export

Endpoint:

```http
GET /api/admin/exports/reimbursements?status=CONFIRMED&applicantId=11&dateFrom=2026-05-01&dateTo=2026-05-31
X-User-Id: 1
X-Store-Id: 1
```

Supported filters:

- `status`
- `applicantId`
- `dateFrom`
- `dateTo`

Exported fields:

- 报销编号
- 报销ID
- 报销人ID
- 用途
- 申请金额
- 状态
- 备注
- 提交时间
- 确认金额
- 确认人
- 确认时间
- 驳回人
- 驳回时间
- 驳回原因
- 是否计入成本

## Boundaries

- `storeId` comes from `CurrentUserContext`; request parameters cannot override it.
- Export is read-only and does not modify work orders, reimbursement records, official settlement records, payment/refund records, or inventory flows.
- Empty result sets still return an `.xlsx` file with headers.
- No database schema change.
- No export task table or async queue.
- No Redis, MQ, or microservice dependency.

## Verification

- `mvn -Dtest=AdminExportControllerTest test`
- `mvn test`
