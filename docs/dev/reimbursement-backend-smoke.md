# M12A Reimbursement Backend Smoke

## Scope

M12A implements the minimal backend reimbursement ledger only.

Implemented:

- `POST /api/staff/reimbursements`
- `GET /api/admin/reimbursements`
- `GET /api/admin/reimbursements/{id}`
- `POST /api/admin/reimbursements/{id}/confirm`
- `POST /api/admin/reimbursements/{id}/reject`

Not implemented in M12A:

- Staff cancel API
- mini-program reimbursement page
- admin-web reimbursement page integration
- finance report integration
- Excel export
- receipt upload
- automatic payment
- multi-step approval

## Business Rules

- Staff submit creates `PENDING`.
- `PENDING` is not operating cost.
- Only `CONFIRMED` is operating cost.
- `REJECTED` and `CANCELLED` are not operating cost.
- `storeId`, `applicantId`, `operatorId`, and `confirmedBy` come from `CurrentUserContext`.
- Reimbursement does not create `inventory_flow`.
- Reimbursement does not create `payment_record` or `refund_record`.
- Reimbursement does not change `work_order.received_amount`.
- Reimbursement does not change official settlement records.

## Smoke Requests

```http
POST /api/staff/reimbursements
Content-Type: application/json
X-User-Id: 7
X-Store-Id: 1

{
  "purpose": "购买清洁用品",
  "amount": 18.50,
  "remark": "门店日常"
}
```

```http
GET /api/admin/reimbursements?status=PENDING&pageNo=1&pageSize=20
X-User-Id: 101
X-Store-Id: 1
```

```http
POST /api/admin/reimbursements/1/confirm
Content-Type: application/json
X-User-Id: 101
X-Store-Id: 1

{
  "confirmedAmount": 18.50,
  "remark": "票据齐全"
}
```

```http
POST /api/admin/reimbursements/1/reject
Content-Type: application/json
X-User-Id: 101
X-Store-Id: 1

{
  "rejectReason": "票据不完整"
}
```

## Verification

- `mvn -Dtest=ReimbursementControllerTest test`
- `mvn test`
