# M21A 后端收银安全与收银日报 Smoke

> 日期：2026-05-20
> 分支：feature/m18-delivery-readiness

---

## 1. 超收拦截规则

新增错误码：`PAYMENT_EXCEEDS_RECEIVABLE`，当累计已收 + 本次收款 > 应收时，记录支付接口直接拒绝。

**实现位置**：`PaymentServiceImpl.recordPayment()`，在 `selectByIdForUpdate` 获取工悲观锁之后、插入 `payment_record` 之前执行校验。

**并发保护**：校验在 `@Transactional` 事务内，依赖工单悲观锁 `selectByIdForUpdate` 串行化并发支付。两个并发请求不会同时通过超收校验。

---

## 2. 收银日报接口

```
GET /api/admin/finance/cashier-report?date=2026-05-20
```

- 权限：`FINANCE_VIEW`
- `storeId` 从 `CurrentUserContext.requireStoreId()` 获取，不接受请求参数
- 平台账号不进入 `/api/admin/**`

---

## 3. 日期口径

- `payment_record` 使用 `paid_at`
- `refund_record` 使用 `refunded_at`
- 当天范围：`[date 00:00:00, date+1 00:00:00)`（左闭右开）
- 不使用 `created_at`

---

## 4. 返回字段

| 字段 | 类型 | 说明 |
|------|------|------|
| date | LocalDate | 查询日期 |
| storeId | Long | 当前门店 ID |
| storeName | String | 门店名称（预留，当前为 null） |
| totalPaymentAmount | BigDecimal | 当日收款总额 |
| totalRefundAmount | BigDecimal | 当日退款总额 |
| netAmount | BigDecimal | 当日净收款 |
| paymentCount | int | 当日收款笔数 |
| refundCount | int | 当日退款笔数 |
| byMethod | List | 按支付方式拆分（仅含当日有交易的方式） |
| byMethod[].method | String | 支付方式码（WECHAT/ALIPAY/UNIONPAY/CASH/OTHER） |
| byMethod[].paymentAmount | BigDecimal | 该方式收款总额 |
| byMethod[].refundAmount | BigDecimal | 该方式退款总额 |
| byMethod[].netAmount | BigDecimal | 该方式净额 |
| byMethod[].paymentCount | int | 该方式收款笔数 |
| byMethod[].refundCount | int | 该方式退款笔数 |
| currentUnpaidWorkOrderCount | int | 当前门店实时未结清工单数（无日期过滤） |
| currentPartialPaidWorkOrderCount | int | 当前门店实时部分收款工单数（无日期过滤） |

---

## 5. 不做范围

- 不接真实支付网关（微信支付/支付宝）
- 不做自动退款
- 不做挂账/欠款
- 不改工单状态机
- 不改库存规则
- 不改财务报表 customerIncome 核心口径
- 不改小程序
- 不改 admin-web 页面

---

## 6. 后续 M21B 前端计划

- admin-web 工单详情收银区域（收款/退款/结算弹窗）
- admin-web 收银日报页面
- 小程序待收金额展示与超收前端校验

---

## 7. 测试清单

### PaymentOverpayTest（6 场景）

| 场景 | 预期 |
|------|------|
| recordPaymentWithinReceivableSucceeds | 成功，receivedAmount 更新 |
| recordPaymentEqualRemainingReceivableSucceeds | 成功，足额收款 |
| recordPaymentExceedingReceivableFailsWithPaymentExceedsReceivable | PAYMENT_EXCEEDS_RECEIVABLE |
| multiplePartialPaymentsCannotExceedReceivable | 多次累计后超收被拦截 |
| draftWorkOrderPaymentStillRejected | PAYMENT_WORK_ORDER_STATUS_INVALID |
| settledWorkOrderPaymentStillRejected | PAYMENT_WORK_ORDER_STATUS_INVALID |

### CashierReportTest（6 场景）

| 场景 | 预期 |
|------|------|
| cashierReportWithPaymentAndRefundReturnsCorrectTotals | 金额/笔数正确 |
| cashierReportGroupsByPaymentMethod | 按方式分组正确 |
| cashierReportEmptyDayReturnsZeroValues | 全零 |
| cashierReportCountsCurrentUnpaidAndPartialPaidOrders | 实时工单数正确 |
| cashierReportUsesCurrentUserStoreId | storeId 来自上下文 |
| cashierReportWithoutFinanceViewReturnsForbidden | 403 |

### 现有测试修复（3 处）

| 测试 | 修改 |
|------|------|
| `PaymentServiceTest.paymentDoesNotSettleOrConsumeInventory` | receivable 100→200，避免超收 |
| `PaymentServiceTest.paymentSummaryShowsTotalsAndCanSettle` | receivable 300→400，避免超收 |
| `WorkOrderServiceTest.settleAllowsOverPayment` | 改为 `settleWithExactPaymentSucceeds`，payment=100=receivable |
| `WorkOrderServiceTest.settleRecalculatesReceivedAmount` | 移除超收+退款，改为单笔足额支付 |
