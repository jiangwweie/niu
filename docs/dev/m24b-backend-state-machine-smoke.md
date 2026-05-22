# M24B Backend State Machine Smoke

## Design Source

- Spec: `docs/superpowers/specs/2026-05-22-m24-workorder-cashier-inventory-state-machine.md`
- Implementation mode: clean-start backend refactor. Trial/dev business data must be cleared explicitly before enabling the new state machine.

## New Work Order Progress States

| Code | Text | Meaning |
| --- | --- | --- |
| DRAFT | 新建中 | Draft input, no stock reservation, no payment/refund/delivery. |
| REPAIRING | 维修中 | Submitted work order, stock reserved, payment/refund allowed. |
| REPAIR_DONE | 维修完成 | Repair completed, stock consumed, delivery waits for cashier closure. |
| DELIVERED | 已交付 | Customer has picked up vehicle; work order is closed. |
| CANCELLED | 已取消 | Work order no longer continues. |

## Cashier Status

| Code | Text | Rule |
| --- | --- | --- |
| NO_CHARGE | 无需收款 | `receivableAmount = 0` and `netReceived = 0`. |
| UNPAID | 未收款 | No effective customer payment. |
| PARTIAL_PAID | 部分收款 | `0 < netReceived < receivableAmount`. |
| PAID | 已收齐 | `netReceived >= receivableAmount` and receivable is positive. |
| REFUND_PENDING | 待退款 | Cancelled work order still has net received amount. |
| PARTIAL_REFUNDED | 部分退款 | Refunded but net received remains positive. |
| REFUNDED | 已退清 | Refunded and net received is zero. |

`CashierStatusService` is the backend source of truth. DTOs derive cashier status rather than asking frontends to calculate it.

## Key APIs

- `POST /api/staff/work-orders/{id}/submit`: `DRAFT -> REPAIRING`, creates `RESERVE` inventory flow.
- `POST /api/staff/work-orders/{id}/mark-repair-done`: `REPAIRING -> REPAIR_DONE`, creates `CONSUME` inventory flow.
- `POST /api/staff/work-orders/{id}/deliver`: `REPAIR_DONE -> DELIVERED`, requires `PAID` or `NO_CHARGE`, no inventory flow.
- `POST /api/staff/work-orders/{id}/cancel`: `DRAFT/REPAIRING -> CANCELLED`; repairing cancel creates `RELEASE`.
- `POST /api/staff/work-orders/{id}/payments`: allowed only in `REPAIRING` and `REPAIR_DONE`.
- `POST /api/staff/work-orders/{id}/refunds`: allowed in `REPAIRING`, `REPAIR_DONE`, `CANCELLED`; staff cannot refund delivered orders.
- `POST /api/staff/work-orders/{id}/non-inventory-charges`: allowed only in `REPAIR_DONE`, only `LABOR` and `OTHER`, reason required.
- Admin work-order APIs expose the same progress actions. Delivered-order refund is admin-only.
- Legacy `/settle` returns `WORK_ORDER_LEGACY_SETTLE_DISABLED` and must not trigger `CONSUME`.

## Inventory Actions

- Submit/start repair: `RESERVE`.
- Mark repair done: `CONSUME`.
- Deliver: no inventory action.
- Cancel while repairing: `RELEASE`.
- Delivered refund: no `RELEASE`, no consume reversal, no stock rollback.

`inventoryStatus` is derived in DTOs for M24B and is not stored on `work_order`.

## Clean Start

Clean-start is explicit and is not hidden in Flyway or application startup.

- Summary: `GET /api/admin/trial-data/summary`.
- Preflight: `GET /api/admin/trial-data/clean-start-preflight`.
- Cleanup: `POST /api/admin/trial-data/clear` with `CONFIRM_CLEAR_TRIAL_DATA`.
- Permission: SUPER_ADMIN only.
- Preserved: accounts, roles, permissions, stores, parts base data.
- Cleared/reset: work orders, payments, refunds, official after-sales, reimbursements, customers, vehicles, inventory flows, and inventory stock quantities.
- If legacy work-order statuses remain, preflight returns `WORK_ORDER_LEGACY_STATUS_EXISTS`.

## NO_CHARGE

Zero-receivable work orders derive `NO_CHARGE` rather than `PAID`. Zero-receivable repair completion or delivery must include a reason such as official after-sales, free inspection, owner waiver, warranty, or other. `REPAIR_DONE + NO_CHARGE` can be delivered.

## Delivered Refund Permission

Delivered-order refund is admin-only and requires `REFUND_AFTER_DELIVERY`, granted to `SUPER_ADMIN` and `STORE_ADMIN` in M24B. The operation requires a refund reason, keeps work-order status as `DELIVERED`, and does not change inventory.

## Repair-Done Additional Charges

`REPAIR_DONE` supports additional `LABOR` and `OTHER` charges only. A reason is required. Additional charges recalculate receivable/outstanding/cashier status, so a previously `PAID` repair-done order can become `PARTIAL_PAID` and cannot be delivered until paid again.

Not included in M24B: adding stock-affecting parts after repair done, returning consumed parts, reverse delivery, forced cancel after repair done, and frontend adaptation.

## Smoke Checks

- `DRAFT -> REPAIRING` reserves stock and is not repeatable.
- `REPAIRING -> REPAIR_DONE` consumes stock and is not repeatable.
- `REPAIR_DONE + PARTIAL_PAID` cannot deliver.
- `REPAIR_DONE + PAID/NO_CHARGE` can deliver without inventory action.
- Delivered refund works only through admin with `REFUND_AFTER_DELIVERY`.
- Staff delivered refund is rejected.
- `/settle` is deprecated and does not consume stock.
- Clean-start preflight blocks legacy statuses.

## Test Result

- Full backend `mvn test` passed.
