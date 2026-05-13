# Admin API Contract v0.1

本文档是“小牛电动官方授权店两轮车维修售后库存管理系统”后端 Admin API 契约草案，供后续 Controller 实现和前端真实接口对接使用。

当前任务为 docs-only：不实现 Controller，不实现登录认证，不修改 Service / Entity / Mapper / migration / 测试。

## 1. 范围与状态

### 1.1 API 前缀

Admin API 统一前缀：

```text
/api/admin
```

### 1.2 已实现服务层，可进入 Controller 契约

- Dict：字典项查询
- Part：配件主数据创建、修改、启停、分页查询
- Inventory：入库、调整、库存查询、库存流水查询
- WorkOrder：草稿、费用明细、提交、取消、结算、查询
- Payment：支付记录、支付汇总
- Refund：退款记录
- OfficialAfterSales：官方订单号、官方结算、无需结算、查询

### 1.3 Planned only，不作为当前真实接口

- Reimbursement：报销暂时搁置
- Finance：财务汇总暂时不做
- Export：Excel 真实导出暂时不做

前端 `src/types` 仅为 mock 阶段参考，真实字段以后端 Controller DTO 和本文档为准，不允许由前端 mock 字段反向决定后端 DTO。

## 2. 通用协议

### 2.1 统一成功响应

以后端当前 `ApiResponse<T>` 为准，字段为 `code/message/data/traceId`，当前没有 `success` 字段。

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "traceId": null
}
```

### 2.2 统一错误响应

```json
{
  "code": "WORK_ORDER_NOT_FOUND",
  "message": "工单不存在",
  "data": null,
  "traceId": null
}
```

常见错误码来自 `ErrorCode`，包括：

- `COMMON_BAD_REQUEST`
- `COMMON_NOT_FOUND`
- `PART_NOT_FOUND`
- `PART_DISABLED`
- `INVENTORY_AVAILABLE_NOT_ENOUGH`
- `WORK_ORDER_NOT_FOUND`
- `WORK_ORDER_NOT_DRAFT`
- `PAYMENT_METHOD_INVALID`
- `REFUND_EXCEEDS_PAID_AMOUNT`
- `WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH`
- `OFFICIAL_AFTER_SALES_NOT_FOUND`
- `OFFICIAL_SETTLEMENT_NOT_ALLOWED`

### 2.3 分页请求

当前通用分页对象为 `PageRequest`，字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNo | Integer | 否 | 从 1 开始，默认 1 |
| pageSize | Integer | 否 | 默认 20，最大 100 |

当前后端分页 DTO 暂未定义 `sortBy` / `sortDirection`。v0.1 Controller 不强制提供排序参数，如后续增加必须由后端 DTO 明确。

### 2.4 分页响应

当前通用分页响应为 `PageResponse<T>`，字段：

```json
{
  "records": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 100
}
```

说明：当前没有 `items` 字段，也没有 `totalPages` 字段。前端如需总页数，可按 `ceil(total / pageSize)` 计算；后续如要增加 `totalPages`，需修改后端分页 Response。

### 2.5 金额和时间

- 所有金额字段使用 `BigDecimal`，JSON 中按 number 或 string 序列化策略以后端 Jackson 配置为准。
- 时间字段使用 `LocalDateTime`，建议 Controller JSON 格式统一为 ISO-8601，例如 `2026-05-13T10:30:00`。

## 3. 登录认证方案设计

本节只设计方案，不实现。

### 3.1 MVP 推荐方案

MVP 管理端建议先采用账号密码登录，后续再扩展手机号验证码。原因是当前后端已有用户、角色、权限种子和权限查询能力，但尚未实现短信验证码基础设施。

建议规划接口：

```text
POST /api/admin/auth/login
POST /api/admin/auth/logout
GET  /api/admin/auth/me
```

登录成功返回：

```json
{
  "token": "admin-jwt-or-session-token",
  "userId": 1,
  "storeId": 1,
  "roleIds": [1],
  "permissionCodes": ["PART_MANAGE", "WORK_ORDER_CREATE"]
}
```

后续请求：

```text
Authorization: Bearer <token>
```

后端通过 token 解析：

- `userId`
- `storeId`
- `roleIds`
- `permissionCodes`

### 3.2 CurrentUserContext 约束

- Controller 不允许以前端传入的 `operatorId` 作为最终可信来源。
- Controller 不允许以前端传入的 `storeId` 作为最终可信来源。
- `storeId` / `operatorId` 最终应来自后端 `CurrentUserContext`。
- Service 层现有 Command 中的 `storeId` / `operatorId` 由 Controller 从 `CurrentUserContext` 填充。
- 权限点以后端判断为准，前端只做显示控制。

### 3.3 Dev-only Header

正式认证实现前，本地开发可临时设计：

```text
X-User-Id: 1
X-Store-Id: 1
```

该方案仅用于本地开发和联调，不是正式安全方案，不能用于生产。

## 4. 枚举与字典

### 4.1 WorkOrderStatus

- `DRAFT`
- `PENDING_ACCEPT`
- `ACCEPTED`
- `PART_ORDERED`
- `PART_ARRIVED`
- `SETTLED`
- `CANCELLED`

### 4.2 ChargeType

- `PART`
- `LABOR`
- `OTHER`

字典类型种子为 `CHARGE_ITEM_TYPE`。

### 4.3 InventoryFlowType

- `INBOUND`
- `RESERVE`
- `RELEASE`
- `CONSUME`
- `ADJUST`

### 4.4 PaymentMethod

以后端 enum 和 seed 字典为准：

- `WECHAT`
- `ALIPAY`
- `UNIONPAY`
- `CASH`
- `OTHER`

### 4.5 OfficialSettlementStatus

- `NOT_REQUIRED`
- `PENDING`
- `SETTLED`

### 4.6 ReimbursementStatus

报销当前 planned only：

- `PENDING`
- `CONFIRMED`
- `REJECTED_OR_CANCELLED`

### 4.7 PartSource / CommonStatus

配件来源：

- `OFFICIAL`
- `THIRD_PARTY`

通用状态：

- `ENABLED`
- `DISABLED`

## 5. DictController

基础路径：

```text
/api/admin/dict
```

### 5.1 获取某类启用字典项

```text
GET /api/admin/dict/types/{typeCode}/items
```

响应 `data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| itemCode | String | 字典项编码 |
| itemName | String | 字典项名称 |
| sortOrder | Integer | 排序 |
| enabled | Boolean | 是否启用，Controller 可由 `status == ENABLED` 转换 |

说明：服务层当前返回 `SysDictItemEntity`，Controller 建议转为 VO，避免直接暴露 `typeId/system/deleted` 等持久化字段。

## 6. PartController

基础路径：

```text
/api/admin/parts
```

### 6.1 分页查询配件

```text
GET /api/admin/parts
```

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| partCode | String | 配件编码，模糊查询 |
| partName | String | 配件名称，模糊查询 |
| source | String | `OFFICIAL` / `THIRD_PARTY` |
| enabled | Boolean | 建议 Controller 转为 `status` |
| categoryCode | String | 预留筛选项；当前 `PartQueryRequest` 未包含，Controller v0.1 可暂不实现或后续补 DTO |
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页数量 |

服务层请求字段参考 `PartQueryRequest`：`storeId/partCode/partName/source/status/pageNo/pageSize`。

响应 `data`：`PageResponse<PartQueryResponse>`。

`PartQueryResponse` 字段：

- `id`
- `storeId`
- `partCode`
- `officialPartNo`
- `partName`
- `model`
- `source`
- `categoryCode`
- `referenceCostPrice`
- `defaultBarcode`
- `locationRemark`
- `createSource`
- `status`
- `remark`

### 6.2 查询配件详情

```text
GET /api/admin/parts/{partId}
```

响应建议复用 `PartQueryResponse` 或新增 `PartDetailResponse`。当前服务层 `getById` 返回 `PartEntity`，Controller 应转 VO。

### 6.3 创建官方配件

```text
POST /api/admin/parts/official
```

请求字段参考 `CreatePartCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| partName | String | 是 | 配件名称 |
| officialPartNo | String | 是 | 官方品号，官方配件必填 |
| model | String | 否 | 车型/型号 |
| categoryCode | String | 否 | 分类编码 |
| referenceCostPrice | BigDecimal | 否 | 参考成本价 |
| locationRemark | String | 否 | 库位备注 |
| remark | String | 否 | 备注 |

`storeId/operatorId` 由 `CurrentUserContext` 填充。

### 6.4 创建第三方配件

```text
POST /api/admin/parts/third-party
```

请求字段同 `CreatePartCommand`，第三方配件不要求 `officialPartNo`。

### 6.5 修改配件基础信息

```text
PUT /api/admin/parts/{partId}
```

请求字段参考 `UpdatePartCommand`：

- `partName`
- `officialPartNo`
- `model`
- `categoryCode`
- `referenceCostPrice`
- `defaultBarcode`
- `locationRemark`
- `remark`

### 6.6 启用配件

```text
POST /api/admin/parts/{partId}/enable
```

### 6.7 停用配件

```text
POST /api/admin/parts/{partId}/disable
```

## 7. InventoryController

基础路径：

```text
/api/admin/inventory
```

### 7.1 分页查询库存

```text
GET /api/admin/inventory/stocks
```

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| partCode | String | 配件编码 |
| partName | String | 配件名称 |
| source | String | 预留；当前 `InventoryService.pageQuery` 未包含 |
| lowStockOnly | Boolean | 预留；当前服务层未实现 |
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页数量 |

响应 `data`：`PageResponse<InventoryStockQueryResponse>`。

`InventoryStockQueryResponse` 字段：

- `id`
- `storeId`
- `partId`
- `partCode`
- `partName`
- `partSource`
- `actualQty`
- `availableQty`
- `reservedQty`
- `lastFlowId`
- `lastChangedAt`

### 7.2 查询某配件库存

```text
GET /api/admin/inventory/stocks/{partId}
```

响应建议转为 `InventoryStockQueryResponse`。当前服务层 `getByPartId(storeId, partId)` 返回 `InventoryStockEntity`。

### 7.3 普通入库

```text
POST /api/admin/inventory/inbound
```

请求字段参考 `InventoryInboundCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| partId | Long | 是 | 配件 ID |
| quantity | Integer | 是 | 入库数量，必须大于 0 |
| unitCost | BigDecimal | 否 | 入库单价，不能为负数 |
| barcode | String | 否 | 条码 |
| locationRemark | String | 否 | 库位备注 |
| reason | String | 否 | 原因 |
| remark | String | 否 | 备注 |

业务效果：生成 `INBOUND` 库存流水，增加实际库存和可用库存。

### 7.4 库存调整

```text
POST /api/admin/inventory/adjust
```

请求字段参考 `InventoryAdjustCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| partId | Long | 是 | 配件 ID |
| quantityDelta | Integer | 是 | 调整数量，不能为 0 |
| reason | String | 是 | 调整原因 |
| remark | String | 否 | 备注 |

业务效果：生成 `ADJUST` 库存流水。调整后可用库存和实际库存不能为负数。

### 7.5 查询库存流水

```text
GET /api/admin/inventory/flows
```

查询参数参考 `InventoryFlowQueryRequest`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| partId | Long | 配件 ID |
| partCode | String | 配件编码 |
| partName | String | 配件名称 |
| flowType | String | `INBOUND/ADJUST/RESERVE/RELEASE/CONSUME` |
| startTime | LocalDateTime | 预留；当前 DTO 未包含 |
| endTime | LocalDateTime | 预留；当前 DTO 未包含 |
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页数量 |

响应 `data`：`PageResponse<InventoryFlowQueryResponse>`。

## 8. WorkOrderController

基础路径：

```text
/api/admin/work-orders
```

### 8.1 分页查询工单

```text
GET /api/admin/work-orders
```

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| workOrderNo | String | 工单号 |
| customerName | String | 客户姓名 |
| customerPhone | String | 预留；当前 `WorkOrderQueryRequest` 未包含 |
| vehicleFrameNo | String | 预留；当前 `WorkOrderQueryRequest` 未包含 |
| status | String | 工单状态 |
| officialOnly | Boolean | 预留；当前 `WorkOrderQueryRequest` 未包含 |
| startTime | LocalDateTime | 预留；当前 `WorkOrderQueryRequest` 未包含 |
| endTime | LocalDateTime | 预留；当前 `WorkOrderQueryRequest` 未包含 |
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页数量 |

服务层当前请求字段为 `storeId/status/workOrderNo/customerName/pageNo/pageSize`。

响应 `data`：`PageResponse<WorkOrderQueryResponse>`。

`WorkOrderQueryResponse` 字段：

- `id`
- `workOrderNo`
- `customerNameSnapshot`
- `vehicleModelSnapshot`
- `status`
- `receivableAmount`
- `receivedAmount`
- `createdAt`

### 8.2 查询工单详情

```text
GET /api/admin/work-orders/{workOrderId}
```

响应 `data` 当前基础字段参考 `WorkOrderDetailResponse`：

- `id`
- `storeId`
- `workOrderNo`
- `customerId`
- `vehicleId`
- `customerNameSnapshot`
- `customerPhoneSnapshot`
- `vehicleModelSnapshot`
- `frameNoSnapshot`
- `batteryNoSnapshot`
- `repairItem`
- `status`
- `receivableAmount`
- `receivedAmount`
- `remark`
- `createdAt`
- `chargeItems`

`chargeItems` 字段参考 `WorkOrderChargeItemResponse`：

- `id`
- `workOrderId`
- `chargeType`
- `itemName`
- `partId`
- `partCodeSnapshot`
- `partNameSnapshot`
- `partSourceSnapshot`
- `quantity`
- `unit`
- `unitPrice`
- `lineAmount`
- `costPriceSnapshot`
- `lineCostAmount`
- `inventoryAffecting`
- `tempPart`
- `status`
- `remark`

Controller v0.1 详情建议额外聚合：

- `paymentSummary`：参考 `PaymentSummaryResponse`
- `officialAfterSales`：参考 `OfficialAfterSalesResponse`

该聚合不改变 Service 业务逻辑，只由 Controller 编排查询。

### 8.3 创建工单草稿

```text
POST /api/admin/work-orders/drafts
```

请求字段参考 `CreateDraftWorkOrderCommand`：

- `customerId`
- `vehicleId`
- `customerNameSnapshot`
- `customerPhoneSnapshot`
- `vehicleModelSnapshot`
- `frameNoSnapshot`
- `batteryNoSnapshot`
- `repairItem`
- `remark`
- `chargeItems`

`chargeItems` 字段参考 `WorkOrderChargeItemInput`：

- `chargeType`
- `itemName`
- `partId`
- `quantity`
- `unit`
- `unitPrice`
- `lineAmount`
- `costPriceSnapshot`
- `lineCostAmount`
- `inventoryAffecting`
- `tempPart`
- `remark`

响应 `data`：建议返回新建 `workOrderId`。

### 8.4 修改草稿基础信息

```text
PUT /api/admin/work-orders/{workOrderId}/draft
```

请求字段参考 `UpdateWorkOrderDraftCommand`：

- `customerNameSnapshot`
- `customerPhoneSnapshot`
- `vehicleModelSnapshot`
- `frameNoSnapshot`
- `batteryNoSnapshot`
- `repairItem`
- `remark`

仅允许 `DRAFT` 状态。

### 8.5 添加费用明细

```text
POST /api/admin/work-orders/{workOrderId}/charge-items
```

请求字段参考 `AddWorkOrderChargeItemCommand`：

- `chargeType`
- `itemName`
- `partId`
- `quantity`
- `unit`
- `unitPrice`
- `remark`

仅允许 `DRAFT` 状态编辑。

### 8.6 修改费用明细

```text
PUT /api/admin/work-orders/{workOrderId}/charge-items/{chargeItemId}
```

请求字段参考 `UpdateWorkOrderChargeItemCommand`：

- `itemName`
- `quantity`
- `unit`
- `unitPrice`
- `remark`

### 8.7 删除费用明细

```text
DELETE /api/admin/work-orders/{workOrderId}/charge-items/{chargeItemId}
```

### 8.8 提交工单

```text
POST /api/admin/work-orders/{workOrderId}/submit
```

请求字段参考 `SubmitWorkOrderCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| remark | String | 否 | 提交备注 |

业务边界：

- submit 才预占库存。
- 只允许 `DRAFT`。
- 对影响库存的配件费用明细生成 `RESERVE` 库存流水。
- 不结算。
- 不收款。

### 8.9 取消工单

```text
POST /api/admin/work-orders/{workOrderId}/cancel
```

请求字段参考 `CancelWorkOrderCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | String | 是 | 取消原因 |

业务边界：

- `DRAFT` 取消不释放库存。
- 已提交未结算取消释放预占库存，生成 `RELEASE` 库存流水。
- 不自动退款。

### 8.10 结算工单

```text
POST /api/admin/work-orders/{workOrderId}/settle
```

请求字段参考 `SettleWorkOrderCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| remark | String | 否 | 结算备注 |

业务边界：

- 校验 `receivedAmount >= receivableAmount`。
- 对已预占库存执行正式扣减，生成 `CONSUME` 库存流水。
- 更新工单状态为 `SETTLED`。
- 支付完成不会自动 settle。

## 9. PaymentController

基础路径：

```text
/api/admin/work-orders/{workOrderId}/payments
```

### 9.1 查询工单支付记录

```text
GET /api/admin/work-orders/{workOrderId}/payments
```

响应 `data`：`List<PaymentRecordResponse>`。

字段：

- `id`
- `workOrderId`
- `paymentNo`
- `amount`
- `paymentMethod`
- `paidAt`
- `receiverId`
- `operatorId`
- `remark`

### 9.2 记录支付

```text
POST /api/admin/work-orders/{workOrderId}/payments
```

请求字段参考 `RecordPaymentCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| amount | BigDecimal | 是 | 支付金额，必须大于 0 |
| paymentMethod | String | 是 | 支付方式 |
| paidAt | LocalDateTime | 否 | 支付时间 |
| receiverId | Long | 是 | 收款人 |
| remark | String | 否 | 备注 |

业务边界：

- 支付只生成 `payment_record`。
- 支付会更新工单 `received_amount`。
- 支付不会自动 `SETTLED`。
- 支付不会生成库存 `CONSUME`。

### 9.3 查询支付汇总

```text
GET /api/admin/work-orders/{workOrderId}/payment-summary
```

响应 `data`：`PaymentSummaryResponse`。

字段：

- `workOrderId`
- `receivableAmount`
- `paymentTotal`
- `refundTotal`
- `receivedAmount`
- `canSettle`

## 10. RefundController

基础路径：

```text
/api/admin/work-orders/{workOrderId}/refunds
```

### 10.1 查询工单退款记录

```text
GET /api/admin/work-orders/{workOrderId}/refunds
```

响应 `data`：`List<RefundRecordResponse>`。

字段：

- `id`
- `workOrderId`
- `refundNo`
- `amount`
- `refundMethod`
- `refundedAt`
- `operatorId`
- `reason`
- `remark`

### 10.2 记录退款

```text
POST /api/admin/work-orders/{workOrderId}/refunds
```

请求字段参考 `RecordRefundCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| amount | BigDecimal | 是 | 退款金额，必须大于 0 |
| refundMethod | String | 是 | 退款方式 |
| refundedAt | LocalDateTime | 否 | 退款时间 |
| reason | String | 是 | 退款原因 |
| remark | String | 否 | 备注 |

业务边界：

- 退款只生成 `refund_record`。
- 退款不会删除 `payment_record`。
- 退款会影响工单 `received_amount`。
- 退款不会反结算。
- 退款不会自动回滚库存。

## 11. OfficialAfterSalesController

基础路径：

```text
/api/admin/official-after-sales
```

### 11.1 分页查询官方售后记录

```text
GET /api/admin/official-after-sales
```

查询参数参考 `OfficialAfterSalesQueryRequest`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| officialOrderNo | String | 官方订单号 |
| workOrderNo | String | 工单号 |
| settlementStatus | String | `PENDING/SETTLED/NOT_REQUIRED` |
| startTime | LocalDateTime | 建议 Controller 映射为 `settlementStartTime` |
| endTime | LocalDateTime | 建议 Controller 映射为 `settlementEndTime` |
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页数量 |

响应 `data`：`PageResponse<OfficialAfterSalesQueryResponse>`。

字段：

- `id`
- `storeId`
- `workOrderId`
- `workOrderNo`
- `officialOrderNo`
- `settlementAmount`
- `settlementStatus`
- `settlementTime`

### 11.2 查询某工单官方售后信息

```text
GET /api/admin/work-orders/{workOrderId}/official-after-sales
```

响应 `data`：`OfficialAfterSalesResponse`。

字段：

- `id`
- `storeId`
- `workOrderId`
- `workOrderNo`
- `officialAfterSales`
- `officialOrderNo`
- `settlementAmount`
- `settlementStatus`
- `settlementTime`
- `operatorId`
- `settlementRemark`
- `remark`
- `createdAt`
- `updatedAt`

### 11.3 保存官方订单号

```text
POST /api/admin/work-orders/{workOrderId}/official-after-sales/order-info
```

请求字段参考 `SaveOfficialOrderInfoCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| officialOrderNo | String | 是 | 官方售后订单号 |
| remark | String | 否 | 备注 |

### 11.4 标记官方已结算

```text
POST /api/admin/work-orders/{workOrderId}/official-after-sales/settle
```

请求字段参考 `MarkOfficialSettledCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| settlementAmount | BigDecimal | 是 | 官方结算金额 |
| settlementTime | LocalDateTime | 否 | 官方结算时间 |
| remark | String | 否 | 备注 |

业务边界：

- 官方结算金额不影响工单 `received_amount`。
- 不生成 `payment_record`。
- 不生成 `refund_record`。
- 不生成 `inventory_flow`。
- 官方结算与客户支付分开统计。

### 11.5 标记无需官方结算

```text
POST /api/admin/work-orders/{workOrderId}/official-after-sales/no-settlement-required
```

请求字段参考 `MarkNoSettlementRequiredCommand`：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | String | 否 | 无需结算原因 |
| remark | String | 否 | 备注 |

业务边界同官方结算：只记录官方售后结算状态，不影响客户支付和库存。

## 12. ReimbursementController

Status: Planned / Not Implemented

基础路径建议：

```text
/api/admin/reimbursements
```

接口草案：

```text
POST /api/admin/reimbursements
POST /api/admin/reimbursements/{id}/confirm
POST /api/admin/reimbursements/{id}/reject
GET  /api/admin/reimbursements
```

说明：

- 当前后端报销能力暂时搁置。
- 前端不得对接真实接口。
- 报销确认后才计入运营成本，该规则后续实现时必须保留。

## 13. FinanceController

Status: Planned / Not Implemented

基础路径建议：

```text
/api/admin/finance
```

接口草案：

```text
GET /api/admin/finance/summary
GET /api/admin/finance/daily
GET /api/admin/finance/monthly
```

说明：

- 当前后端未实现财务汇总。
- 前端不得对接真实接口。
- 官方结算与客户支付必须分开统计。

## 14. ExportController

Status: Planned / Not Implemented

基础路径建议：

```text
/api/admin/exports
```

接口草案：

```text
GET  /api/admin/exports/types
POST /api/admin/exports/tasks
GET  /api/admin/exports/tasks
GET  /api/admin/exports/tasks/{taskId}/download
```

说明：

- 当前后端未实现 Excel 真实导出。
- 前端不得对接真实接口。

## 15. Action 类接口业务边界

### 15.1 submit

- 只允许 `DRAFT`。
- 提交时才预占库存。
- 对库存影响明细生成 `RESERVE` 库存流水。
- 不结算。
- 不收款。

### 15.2 cancel

- `DRAFT` 取消不释放库存。
- 已提交未结算取消释放库存，生成 `RELEASE` 库存流水。
- 不自动退款。
- 不删除支付记录。

### 15.3 settle

- 校验 `received_amount >= receivable_amount`。
- 扣减库存，生成 `CONSUME` 库存流水。
- 更新工单为 `SETTLED`。
- 支付完成不会自动 settle。

### 15.4 record payment

- 记录 `payment_record`。
- 更新 `received_amount`。
- 不自动 `SETTLED`。
- 不生成库存 `CONSUME`。

### 15.5 record refund

- 记录 `refund_record`。
- 更新 `received_amount`。
- 不删除 `payment_record`。
- 不自动反结算。
- 不自动回滚库存。

### 15.6 official settle

- 只记录官方结算。
- 不影响客户支付。
- 不影响 `received_amount`。
- 不生成 `payment_record`。
- 不生成 `refund_record`。
- 不影响库存。

## 16. Controller 实现注意事项

- Controller 负责从 `CurrentUserContext` 填充 `storeId/operatorId`。
- Controller 不信任前端传入的 `storeId/operatorId`。
- Controller 应把 Entity 转为 VO，不直接暴露持久化对象。
- 已实现模块字段以当前 Command / Response DTO 为准。
- 预留查询条件不得在 Controller 中假装已实现；如服务层 DTO 未支持，应标记为预留或补任务卡。
- 不得把官方结算金额算入客户支付。
- 不得把支付直接塞进工单单字段替代 `payment_record`。
- 库存变化必须有库存流水。
