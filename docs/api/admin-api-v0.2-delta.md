# Admin API Contract v0.2 Delta

本文档记录 v0.1 后为首轮前后端联调补齐的查询契约差异。v0.1 主文档仍为基础契约：

- `docs/api/admin-api-v0.1.md`

本次 delta 不改变核心业务规则，不实现 Controller，不实现财务、报销、导出、用户权限管理或 `lowStockOnly`。

## 1. WorkOrder 查询增强

计划接口仍为：

```text
GET /api/admin/work-orders
```

`WorkOrderQueryRequest` 新增查询字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| customerPhone | String | 按客户手机号快照模糊查询 |
| vehicleFrameNo | String | 按车架号快照模糊查询 |
| officialOnly | Boolean | 为 true 时只查询官方售后工单 |
| startTime | LocalDateTime | 按工单创建时间起始过滤 |
| endTime | LocalDateTime | 按工单创建时间结束过滤 |

`WorkOrderQueryResponse` 新增响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| customerPhoneSnapshot | String | 客户手机号快照 |
| frameNoSnapshot | String | 车架号快照 |
| officialAfterSales | Boolean | 是否存在官方售后记录 |
| officialOrderNo | String | 官方订单号 |

说明：

- `officialOnly` 只在 `true` 时启用过滤；`false/null` 不额外过滤。
- 该增强只用于列表查询，不改变工单状态流转。
- submit/cancel/settle 业务规则不变。

## 2. OfficialAfterSales 查询增强

计划接口仍为：

```text
GET /api/admin/official-after-sales
```

`OfficialAfterSalesQueryResponse` 新增响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| customerNameSnapshot | String | 工单客户姓名快照 |
| customerPhoneSnapshot | String | 工单客户手机号快照 |
| vehicleModelSnapshot | String | 工单车型快照 |
| frameNoSnapshot | String | 工单车架号快照 |
| receivedAmount | BigDecimal | 工单客户侧实收金额 |
| workOrderStatus | String | 工单状态 |

说明：

- 官方结算金额仍不影响 `receivedAmount`。
- 不生成 `payment_record`。
- 不生成 `refund_record`。
- 不生成 `inventory_flow`。
- 官方结算与客户支付仍分开统计。

## 3. Payment 全局分页查询

新增计划接口：

```text
GET /api/admin/payments
```

`PaymentQueryRequest` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| workOrderNo | String | 按工单号模糊查询 |
| customerName | String | 按客户姓名快照模糊查询 |
| paymentMethod | String | 支付方式 |
| startTime | LocalDateTime | 按支付时间起始过滤 |
| endTime | LocalDateTime | 按支付时间结束过滤 |
| pageNo | Integer | 页码，默认 1 |
| pageSize | Integer | 每页数量，默认 20 |

`PaymentQueryResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 支付记录 ID |
| workOrderId | Long | 工单 ID |
| workOrderNo | String | 工单号 |
| customerNameSnapshot | String | 客户姓名快照 |
| paymentNo | String | 支付流水号 |
| amount | BigDecimal | 支付金额 |
| paymentMethod | String | 支付方式 |
| paidAt | LocalDateTime | 支付时间 |
| receiverId | Long | 收款人 ID |
| operatorId | Long | 操作人 ID |
| remark | String | 备注 |

说明：

- 全局查询只读取 `payment_record` 和工单快照字段。
- 不改变支付记录逻辑。
- 不改变 `received_amount` 计算逻辑。
- 不自动触发工单结算。
- 不生成库存流水。

## 4. Refund 全局分页查询

新增计划接口：

```text
GET /api/admin/refunds
```

`RefundQueryRequest` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| workOrderNo | String | 按工单号模糊查询 |
| customerName | String | 按客户姓名快照模糊查询 |
| refundMethod | String | 退款方式 |
| startTime | LocalDateTime | 按退款时间起始过滤 |
| endTime | LocalDateTime | 按退款时间结束过滤 |
| pageNo | Integer | 页码，默认 1 |
| pageSize | Integer | 每页数量，默认 20 |

`RefundQueryResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 退款记录 ID |
| workOrderId | Long | 工单 ID |
| workOrderNo | String | 工单号 |
| customerNameSnapshot | String | 客户姓名快照 |
| refundNo | String | 退款流水号 |
| amount | BigDecimal | 退款金额 |
| refundMethod | String | 退款方式 |
| refundedAt | LocalDateTime | 退款时间 |
| operatorId | Long | 操作人 ID |
| reason | String | 退款原因 |
| remark | String | 备注 |

说明：

- 全局查询只读取 `refund_record` 和工单快照字段。
- 不删除或修改 `payment_record`。
- 不改变 `received_amount` 计算逻辑。
- 不自动反结算。
- 不回滚库存。

## 5. Task 14B Controller 对接提示

- Controller 仍应从 `CurrentUserContext` 获取 `storeId/operatorId`。
- 前端不得传入可信 `storeId/operatorId`。
- Controller 响应仍使用 `ApiResponse`：`code/message/data/traceId`。
- 分页响应仍使用 `PageResponse`：`records/pageNo/pageSize/total`。
- 不新增 `success/items/totalPages`。
- 本 delta 不包含 `lowStockOnly`。
