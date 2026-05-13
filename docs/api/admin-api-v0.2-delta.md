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

## 5. Dict 查询增强

### 5.1 新增：字典类型列表

```text
GET /api/admin/dict/types
```

返回所有启用的字典类型列表。无需认证 Header（纯只读公共数据）。

响应：

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "typeCode": "WORK_ORDER_STATUS",
      "typeName": "工单状态",
      "enabled": true
    }
  ]
}
```

说明：
- 只返回 `status = ENABLED` 且未软删除的字典类型。
- 不实现字典类型的新增、编辑、停用。
- 前端可用此接口替代硬编码字典类型列表。

### 5.2 字典项查询行为确认

```text
GET /api/admin/dict/types/{typeCode}/items
```

响应格式：

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "itemCode": "DRAFT",
      "itemName": "草稿",
      "sortOrder": 1,
      "enabled": true
    }
  ]
}
```

行为说明：
- `typeCode` 存在且启用：返回该类型下所有启用的字典项，按 `sort_order` 升序排列。
- `typeCode` 不存在或已停用：返回空数组 `[]`，不返回错误。
- 不返回 `id`、`typeId`、`remark` 等内部字段。
- 不实现字典项的新增、编辑、停用。

## 6. Part 查询增强

`GET /api/admin/parts` 新增可选查询参数：

| 参数 | 类型 | 匹配方式 | 说明 |
| --- | --- | --- | --- |
| officialPartNo | String | 精确匹配 | 按官方配件编号筛选 |
| model | String | 模糊匹配 | 按车型模糊筛选 |
| categoryCode | String | 精确匹配 | 按配件分类编码筛选 |

示例：

```text
GET /api/admin/parts?officialPartNo=BAT-N1S-001&pageNo=1&pageSize=20
GET /api/admin/parts?model=N1S&pageNo=1&pageSize=20
GET /api/admin/parts?categoryCode=BRAKE&pageNo=1&pageSize=20
```

说明：
- 所有新参数均为可选，与现有 `partCode`/`partName`/`source`/`status` 可任意组合。
- `storeId` 隔离保持不变。
- 不改变现有分页逻辑。

## 7. Inventory API 契约

### 7.1 GET /api/admin/inventory/flows 响应字段

`InventoryFlowQueryResponse` 完整字段清单：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 流水 ID |
| storeId | Long | 门店 ID |
| partId | Long | 配件 ID |
| partCode | String | 配件编码（从 Part 表关联获取） |
| partName | String | 配件名称（从 Part 表关联获取） |
| flowType | String | 流水类型：INBOUND / RESERVE / RELEASE / CONSUME / ADJUST |
| quantityDelta | Integer | 数量变化（正值入库/调增，负值出库/调减） |
| actualBefore | Integer | 变化前实际库存 |
| actualAfter | Integer | 变化后实际库存 |
| availableBefore | Integer | 变化前可用库存 |
| availableAfter | Integer | 变化后可用库存 |
| reservedBefore | Integer | 变化前预占库存 |
| reservedAfter | Integer | 变化后预占库存 |
| businessType | String | 业务类型：MANUAL_INBOUND / MANUAL_ADJUST（工单场景后续扩展） |
| businessId | Long | 业务 ID（当前为 null，工单场景后续填充） |
| operatorId | Long | 操作人 ID |
| operatedAt | LocalDateTime | 操作时间 |
| reason | String | 原因 |
| remark | String | 备注 |
| unitCost | BigDecimal | 入库单价（仅 INBOUND 流水有值，其他为 null） |

响应示例：

```json
{
  "code": "SUCCESS",
  "data": {
    "records": [
      {
        "id": 1,
        "storeId": 1,
        "partId": 1,
        "partCode": "P-BRAKE-001",
        "partName": "刹车片",
        "flowType": "INBOUND",
        "quantityDelta": 100,
        "actualBefore": 0,
        "actualAfter": 100,
        "availableBefore": 0,
        "availableAfter": 100,
        "reservedBefore": 0,
        "reservedAfter": 0,
        "businessType": "MANUAL_INBOUND",
        "businessId": null,
        "operatorId": 1,
        "operatedAt": "2026-05-13T10:00:00",
        "reason": "首次入库",
        "remark": null,
        "unitCost": 45.50
      }
    ],
    "pageNo": 1,
    "pageSize": 50,
    "total": 1
  }
}
```

说明：

- `actualBefore/actualAfter` 即"变化前后实际库存"，对应前端 beforeQty / afterQty。
- `availableBefore/availableAfter` 即"变化前后可用库存"。
- `reservedBefore/reservedAfter` 即"变化前后预占库存"。
- `businessType` 当前值为 `MANUAL_INBOUND`（入库）和 `MANUAL_ADJUST`（调整），工单场景后续扩展为 `WORK_ORDER_RESERVE` 等。
- 不返回 `flowNo`、`operatorName` 等字段。当前无流水号生成机制，无用户系统联表查询。

### 7.2 GET /api/admin/inventory/flows 查询参数

| 参数 | 类型 | 必填 | 匹配方式 | 说明 |
| --- | --- | --- | --- | --- |
| partId | Long | 否 | 精确匹配 | 按配件 ID 筛选 |
| partCode | String | 否 | 精确匹配 | 按配件编码筛选（通过 Part 表关联） |
| partName | String | 否 | 模糊匹配 | 按配件名称筛选（通过 Part 表关联） |
| flowType | String | 否 | 精确匹配 | 按流水类型筛选 |
| pageNo | Integer | 否 | -- | 页码，默认 1 |
| pageSize | Integer | 否 | -- | 每页数量，默认 20 |

说明：

- `storeId` 从 `CurrentUserContext` 注入，不接受请求体覆盖，storeId 隔离完整。
- 支持 `partId + pageNo + pageSize` 组合查询。
- 按 `id DESC` 排序（最新流水在前）。

### 7.3 unitCost 语义

`unitCost` 是本次入库的成本单价，业务规则：

1. 可选字段，允许为 null。
2. 持久化到 `inventory_flow.unit_cost`。
3. 入库时若提供 `unitCost`，同步更新 `part.reference_cost_price`（直接覆盖，非加权平均）。
4. 调整（ADJUST）不写入 `unitCost`。
5. 不引入 FIFO / 加权平均库存成本算法。
6. 值不能为负数，否则返回 `INBOUND_UNIT_COST_NEGATIVE`。

### 7.4 adjust quantityDelta 边界行为

| 场景 | 行为 | 错误码 |
| --- | --- | --- |
| quantityDelta = 0 | 拒绝 | `INVENTORY_ADJUST_ZERO`（调整数量不能为0） |
| 调整后 availableQty < 0 | 拒绝 | `INVENTORY_ADJUST_WOULD_NEGATIVE`（调整后可用库存不能为负数） |
| 调整后 actualQty < 0 | 拒绝 | `INVENTORY_ADJUST_ACTUAL_NEGATIVE`（调整后实际库存不能为负数） |
| 无库存记录 | 拒绝 | `PART_STOCK_NOT_FOUND`（库存记录不存在） |
| reason 为空 | 拒绝 | `INVENTORY_ADJUST_REASON_REQUIRED`（调整原因不能为空） |

检查顺序：先检 `availableAfter < 0`，再检 `actualAfter < 0`。两个检查相互独立。

## 8. Task 14B Controller 对接提示

- Controller 仍应从 `CurrentUserContext` 获取 `storeId/operatorId`。
- 前端不得传入可信 `storeId/operatorId`。
- Controller 响应仍使用 `ApiResponse`：`code/message/data/traceId`。
- 分页响应仍使用 `PageResponse`：`records/pageNo/pageSize/total`。
- 不新增 `success/items/totalPages`。
- 本 delta 不包含 `lowStockOnly`。
