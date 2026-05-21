# M21B-2 小程序收银体验优化 Smoke

## 目标

优化小程序工单详情页收银体验，新增前端收银边界校验，减少误操作。
后端仍为最终校验边界（PAYMENT_EXCEEDS_RECEIVABLE、REFUND_EXCEEDS_PAID_AMOUNT 等）。

## 检查项

### 1. 待收金额展示

- 工单详情页金额摘要区新增"待收金额"行
- 计算方式：max(应收金额 - 实收金额, 0)
- 已收齐时显示 ¥0

### 2. 收款弹窗待收金额提示

- 弹窗内展示"待收金额"只读行
- 支付金额 placeholder 动态显示"不超过待收¥X"
- 待收为 0 时显示绿色提示"已无待收金额"

### 3. 前端超收校验

- 支付金额 > 待收金额时 Toast 提示"支付金额超过待收金额 ¥X"
- 待收 = 0 时阻止提交，Toast 提示"已无待收金额，无需继续收款"

### 4. 结算按钮状态

- 待收 > 0 时结算按钮变灰（disabled），点击 Toast 提示"请先完成收款"
- 待收 = 0 时按钮恢复可点击

### 5. 按钮顺序

收款（记录支付） → 退款（记录退款） → 结算（结算工单）

### 6. 退款弹窗能力

- 展示可退金额 = receivedAmount
- 退款金额输入 placeholder 动态显示"不超过可退¥X"
- 退款金额 > 可退金额时 Toast 提示
- 退款原因必填（前端校验）
- 退款方式必填（前端校验）
- 后端 REFUND_EXCEEDS_PAID_AMOUNT 仍为最终边界

### 7. 权限与状态边界

| 操作 | 权限 | 工单状态要求 |
|------|------|-------------|
| 记录支付 | PAYMENT_RECORD | PENDING_ACCEPT / ACCEPTED / PART_ORDERED / PART_ARRIVED |
| 记录退款 | REFUND_RECORD | 无状态限制（前端） |
| 结算工单 | WORK_ORDER_SETTLE | PENDING_ACCEPT / ACCEPTED / PART_ORDERED / PART_ARRIVED |
| 取消工单 | WORK_ORDER_CANCEL | 非 CANCELLED / 非 SETTLED |

### 8. 后端仍是最终校验

前端校验为体验优化，后端仍保留：
- PAYMENT_EXCEEDS_RECEIVABLE：禁止超收
- REFUND_EXCEEDS_PAID_AMOUNT：退款不可超过已收
- 工单状态校验
- 权限校验

## 不做内容

- 不做支付网关 / 微信支付 / 支付宝网关
- 不做挂账 / 欠款
- 不改工单状态机
- 不改库存规则
- 不改财务报表口径
- 不改后端 API
- 不改 admin-web

## 修改文件

- `mini-program/src/pages/work-order-detail/index.ts`
- `mini-program/src/pages/work-order-detail/index.wxml`

## tsc 检查结果

全部历史错误（tdesign-miniprogram 类型声明 + miniprogram-api-typings），无本次新增错误。
