# Staff API v0.6 - 记录支付接口

本文档在 v0.5 取消工单接口基础上，新增 Staff 端记录客户线下支付接口。

## 1. 范围

### 1.1 本次新增

| 模块 | Controller | 接口 | 说明 |
|------|------------|------|------|
| WorkOrder Payment | StaffWorkOrderController | POST /api/staff/work-orders/{workOrderId}/payments | 记录当前门店工单的一笔客户支付 |

### 1.2 本次未实现

- 退款
- 工单结算
- 取消工单新逻辑
- 真实微信支付 / 支付宝支付网关
- 自动收款
- 自动对账
- 自动 SETTLED
- 库存 CONSUME / RELEASE / RESERVE
- DB schema 修改

## 2. 认证与上下文

当前仍使用 dev-only Header 注入 CurrentUserContext：

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 是 | 当前操作员 ID，写入 operatorId，并作为 Staff 默认 receiverId |
| X-Store-Id | 是 | 当前门店 ID，写入 storeId |

Staff 写接口必须从 CurrentUserContext 获取 storeId / operatorId，不信任请求体或 query 中的同名字段。

## 3. 接口

### POST /api/staff/work-orders/{workOrderId}/payments

记录一笔客户支付。接口只生成 payment_record 并更新工单 receivedAmount，不自动结算工单。

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workOrderId | Long | 是 | 工单 ID |

**请求体（StaffRecordPaymentRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| amount | BigDecimal | 是 | 支付金额，必须大于 0 |
| paymentMethod | String | 是 | WECHAT / ALIPAY / UNIONPAY / CASH / OTHER |
| paidAt | LocalDateTime | 否 | 支付时间；为空时由后端使用当前时间 |
| receiverId | Long | 否 | Staff v0.6 不作为可信来源；后端默认使用当前操作人 |
| remark | String | 否 | 备注 |

**请求示例：**

```http
POST /api/staff/work-orders/400/payments
Content-Type: application/json
X-User-Id: 10
X-Store-Id: 1

{
  "amount": 120.50,
  "paymentMethod": "WECHAT",
  "paidAt": "2026-05-14T10:30:00",
  "remark": "客户微信支付"
}
```

**响应（ApiResponse<StaffPaymentRecordResponse>）：**

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "id": 7001,
    "workOrderId": 400,
    "paymentNo": "PAY202605140001",
    "amount": 120.50,
    "paymentMethod": "WECHAT",
    "paidAt": "2026-05-14T10:30:00",
    "receiverId": 10,
    "operatorId": 10,
    "remark": "客户微信支付"
  },
  "traceId": null
}
```

**Staff 响应不暴露字段：**

- 不返回 storeId
- 不返回工单成本字段
- 不返回费用明细成本字段
- 不返回库存成本字段

## 4. 业务边界

1. 支付只生成 payment_record。
2. 支付会更新工单 receivedAmount。
3. 支付不会自动 SETTLED。
4. 支付不会生成库存 CONSUME。
5. 支付不会生成库存 RELEASE。
6. 支付不会生成库存 RESERVE。
7. 支付不会生成 refund_record。
8. amount 必须大于 0。
9. paymentMethod 必须是 WECHAT / ALIPAY / UNIONPAY / CASH / OTHER。
10. CANCELLED 工单不允许记录支付，遵循现有 PaymentService 校验。
11. SETTLED 工单是否允许补录支付，遵循现有 PaymentService 校验；当前实现不允许。
12. 跨 store 工单返回 WORK_ORDER_NOT_FOUND。
13. 本接口不接真实支付网关，只记录门店线下已收款事实。

## 5. 支付与库存说明

记录支付是客户支付侧动作，不是库存动作：

| 项目 | 本接口行为 |
|------|------------|
| payment_record | 新增 1 条 |
| work_order.received_amount | 按现有 PaymentAmountService 重算并更新 |
| work_order.status | 不自动变更为 SETTLED |
| inventory_flow.RESERVE | 不新增 |
| inventory_flow.RELEASE | 不新增 |
| inventory_flow.CONSUME | 不新增 |
| refund_record | 不新增 |

正式扣减实际库存只发生在结算阶段；本接口不会扣减库存。

## 6. 错误响应

错误响应保持统一 ApiResponse 结构：

```json
{
  "code": "PAYMENT_METHOD_INVALID",
  "message": "支付方式无效",
  "data": null,
  "traceId": null
}
```

常见错误码：

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| COMMON_BAD_REQUEST | 400 | 缺少 dev header / 参数校验失败 |
| WORK_ORDER_NOT_FOUND | 400 | 工单不存在或不属于当前门店 |
| PAYMENT_AMOUNT_INVALID | 400 | Service 层判断支付金额无效 |
| PAYMENT_METHOD_INVALID | 400 | 支付方式无效 |
| PAYMENT_WORK_ORDER_STATUS_INVALID | 400 | 当前工单状态不允许记录支付 |

## 7. 实现确认

- 不新增 Controller 模块，只扩展 StaffWorkOrderController。
- 复用现有 RecordPaymentApplicationService / PaymentService。
- 不修改数据库 schema。
- 不修改支付核心金额计算规则。
- 不修改库存 RESERVE / RELEASE / CONSUME 核心规则。
- 不新增退款、结算接口。
- storeId / operatorId 来自 CurrentUserContext。
- Staff 默认 receiverId 使用 CurrentUserContext.userId。
