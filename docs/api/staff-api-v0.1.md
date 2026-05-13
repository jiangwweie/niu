# Staff API v0.1 — 第一批只读接口

本文档记录 `/api/staff/**` 第一批只读接口的契约。

## 1. 范围

### 1.1 API 前缀

```text
/api/staff
```

### 1.2 本次实现模块

| 模块 | Controller | 说明 |
|------|-----------|------|
| Dict | StaffDictController | 字典项只读查询 |
| Part | StaffPartController | 配件只读查询，仅返回启用配件 |
| Inventory | StaffInventoryController | 库存只读查询 |
| WorkOrder | StaffWorkOrderController | 工单只读查询 |

### 1.3 本次未实现

- 工单创建/草稿、提交、取消、结算
- 收费项新增/修改/删除
- 支付记录、退款记录
- 库存入库/调整
- 报销
- Auth/JWT/微信登录/手机号绑定
- 真实支付网关
- DB schema 修改

## 2. 通用协议

### 2.1 统一成功响应

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
  "code": "COMMON_BAD_REQUEST",
  "message": "缺少用户上下文",
  "data": null,
  "traceId": null
}
```

### 2.3 分页响应

```json
{
  "records": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 0
}
```

## 3. Dev Header（开发联调用）

**重要：这不是正式认证机制。** 正式认证方案（JWT / 微信登录）待后续任务实现。

所有 `/api/staff/**` 接口（Dict 除外）需要以下请求头：

| Header | 必填 | 说明 |
|--------|------|------|
| `X-User-Id` | 是 | 当前操作员 ID（开发模拟） |
| `X-Store-Id` | 是 | 当前门店 ID（开发模拟） |

缺少任一 header 时返回：

```json
{
  "code": "COMMON_BAD_REQUEST",
  "message": "缺少用户上下文",
  "data": null,
  "traceId": null
}
```

`X-Store-Id` 决定数据可见范围，不会被请求体中的 `storeId` 覆盖。

## 4. Endpoint 清单

### 4.1 Dict 只读

#### GET /api/staff/dict/types/{typeCode}/items

获取指定字典类型的启用项列表。

**不需要 X-User-Id / X-Store-Id。**

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| typeCode | String | 字典类型编码，如 `PART_CATEGORY` |

**响应字段（`List<DictItemResponse>`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| itemCode | String | 字典项编码 |
| itemName | String | 字典项名称 |
| sortOrder | Integer | 排序序号 |
| enabled | Boolean | 是否启用 |

---

### 4.2 Part 只读

#### GET /api/staff/parts

查询启用配件列表（分页）。只返回 `status = ENABLED` 的配件。

**Query 参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| partCode | 否 | 配件编码（模糊匹配） |
| partName | 否 | 配件名称（模糊匹配） |
| officialPartNo | 否 | 官方品号 |
| model | 否 | 车型 |
| categoryCode | 否 | 分类编码 |
| source | 否 | 来源（OFFICIAL / THIRD_PARTY） |
| pageNo | 否 | 页码，默认 1 |
| pageSize | 否 | 每页条数，默认 20 |

**响应字段（`PageResponse<StaffPartListItem>`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 配件 ID |
| partCode | String | 配件编码 |
| officialPartNo | String | 官方品号 |
| partName | String | 配件名称 |
| model | String | 车型 |
| source | String | 来源 |
| categoryCode | String | 分类编码 |
| status | String | 状态（固定 ENABLED） |

**不暴露字段（与 Admin DTO 差异）：**
- `referenceCostPrice`（参考成本价）
- `defaultBarcode`（默认条码）
- `locationRemark`（存放位置）
- `createSource`（创建来源）
- `storeId`

#### GET /api/staff/parts/{partId}

获取配件详情（不含成本价）。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| partId | Long | 配件 ID |

**响应字段（`StaffPartDetail`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 配件 ID |
| partCode | String | 配件编码 |
| officialPartNo | String | 官方品号 |
| partName | String | 配件名称 |
| model | String | 车型 |
| source | String | 来源 |
| categoryCode | String | 分类编码 |
| status | String | 状态 |
| remark | String | 备注 |

**错误响应：**
- 配件不存在或不属于当前门店：`PART_NOT_FOUND`

---

### 4.3 Inventory 只读

#### GET /api/staff/inventory/stocks

查询当前门店库存列表（分页）。

**Query 参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| partCode | 否 | 配件编码 |
| partName | 否 | 配件名称 |
| pageNo | 否 | 页码 |
| pageSize | 否 | 每页条数 |

**响应字段（`PageResponse<StaffInventoryStockItem>`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| partId | Long | 配件 ID |
| partCode | String | 配件编码 |
| partName | String | 配件名称 |
| partSource | String | 配件来源 |
| actualQty | Integer | 实际库存 |
| availableQty | Integer | 可用库存 |
| reservedQty | Integer | 预占库存 |

**不暴露字段（与 Admin DTO 差异）：**
- `id`（库存记录 ID）
- `storeId`
- `lastFlowId`
- `lastChangedAt`

#### GET /api/staff/inventory/stocks/{partId}

获取指定配件的库存详情。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| partId | Long | 配件 ID |

**响应字段（`StaffInventoryStockDetail`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| partId | Long | 配件 ID |
| partCode | String | 配件编码 |
| partName | String | 配件名称 |
| partSource | String | 配件来源 |
| actualQty | Integer | 实际库存 |
| availableQty | Integer | 可用库存 |
| reservedQty | Integer | 预占库存 |
| lastChangedAt | LocalDateTime | 最后变更时间 |

**错误响应：**
- 库存记录不存在：`PART_STOCK_NOT_FOUND`

---

### 4.4 WorkOrder 只读

#### GET /api/staff/work-orders

查询当前门店工单列表（分页）。第一版按 storeId 隔离，不按创建人隔离。

**Query 参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| workOrderNo | 否 | 工单号 |
| customerName | 否 | 客户姓名 |
| customerPhone | 否 | 客户电话 |
| vehicleFrameNo | 否 | 车架号 |
| status | 否 | 工单状态 |
| pageNo | 否 | 页码 |
| pageSize | 否 | 每页条数 |

**响应字段（`PageResponse<StaffWorkOrderListItem>`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 工单 ID |
| workOrderNo | String | 工单号 |
| customerNameSnapshot | String | 客户姓名 |
| customerPhoneSnapshot | String | 客户电话 |
| vehicleModelSnapshot | String | 车型 |
| frameNoSnapshot | String | 车架号 |
| status | String | 工单状态 |
| receivableAmount | BigDecimal | 应收金额 |
| receivedAmount | BigDecimal | 实收金额 |
| createdAt | LocalDateTime | 创建时间 |

**不暴露字段（与 Admin DTO 差异）：**
- `officialAfterSales`（官方售后标记）
- `officialOrderNo`（官方售后订单号）
- `storeId`
- `customerId`
- `vehicleId`

#### GET /api/staff/work-orders/{workOrderId}

获取工单详情，含只读收费项明细。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| workOrderId | Long | 工单 ID |

**响应字段（`StaffWorkOrderDetail`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 工单 ID |
| workOrderNo | String | 工单号 |
| customerNameSnapshot | String | 客户姓名 |
| customerPhoneSnapshot | String | 客户电话 |
| vehicleModelSnapshot | String | 车型 |
| frameNoSnapshot | String | 车架号 |
| batteryNoSnapshot | String | 电池编号 |
| repairItem | String | 维修项目 |
| status | String | 工单状态 |
| receivableAmount | BigDecimal | 应收金额 |
| receivedAmount | BigDecimal | 实收金额 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| chargeItems | List | 收费项明细列表 |

**chargeItems 字段（`StaffChargeItem`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 收费项 ID |
| chargeType | String | 类型（PART / LABOR / OTHER） |
| itemName | String | 项目名称 |
| partId | Long | 配件 ID（PART 类型才有） |
| partCodeSnapshot | String | 配件编码快照 |
| partNameSnapshot | String | 配件名称快照 |
| partSourceSnapshot | String | 配件来源快照 |
| quantity | Integer | 数量 |
| unit | String | 单位 |
| unitPrice | BigDecimal | 单价 |
| lineAmount | BigDecimal | 行金额 |
| remark | String | 备注 |

**不暴露字段（与 Admin DTO 差异）：**
- `costPriceSnapshot`（成本价快照）
- `lineCostAmount`（行成本金额）
- `inventoryAffecting`（是否影响库存）
- `tempPart`（是否临时配件）
- `status`（收费项状态）
- `workOrderId`

**错误响应：**
- 工单不存在或不属于当前门店：`WORK_ORDER_NOT_FOUND`

## 5. Service 复用说明

Staff Controller 完全复用现有 Service 接口：

| Staff Controller | 复用的 Service |
|------------------|---------------|
| StaffDictController | DictService |
| StaffPartController | PartService |
| StaffInventoryController | InventoryService, PartMapper |
| StaffWorkOrderController | WorkOrderService |

没有为 Staff 端编写任何业务逻辑，所有查询均委托给现有 Service。

## 6. DTO 裁剪说明

| 字段类别 | Admin 暴露 | Staff 暴露 | 说明 |
|----------|-----------|-----------|------|
| 配件成本价 referenceCostPrice | 是 | 否 | 员工无需看到参考成本价 |
| 配件条码 defaultBarcode | 是 | 否 | 非核心字段 |
| 配件存放位置 locationRemark | 是 | 否 | 非核心字段 |
| 配件创建来源 createSource | 是 | 否 | 非核心字段 |
| 收费项成本价 costPriceSnapshot | 是 | 否 | 敏感财务字段 |
| 收费项行成本 lineCostAmount | 是 | 否 | 敏感财务字段 |
| 收费项库存影响标记 inventoryAffecting | 是 | 否 | 内部逻辑字段 |
| 收费项临时配件标记 tempPart | 是 | 否 | 内部逻辑字段 |
| 收费项状态 status | 是 | 否 | Staff 只读不修改 |
| 工单官方售后标记 officialAfterSales | 是 | 否 | 可在后续版本加入 |
| 工单官方售后订单号 officialOrderNo | 是 | 否 | 可在后续版本加入 |
| storeId / customerId / vehicleId | 是 | 否 | 内部关联 ID |

## 7. 当前不支持的操作

- POST /api/staff/work-orders/drafts
- PUT /api/staff/work-orders/{id}/draft
- POST /api/staff/work-orders/{id}/charge-items
- PUT /api/staff/work-orders/{id}/charge-items/{chargeItemId}
- DELETE /api/staff/work-orders/{id}/charge-items/{chargeItemId}
- POST /api/staff/work-orders/{id}/submit
- POST /api/staff/work-orders/{id}/cancel
- POST /api/staff/work-orders/{id}/settle
- POST /api/staff/payment/record
- POST /api/staff/refund/record
- POST /api/staff/inventory/inbound
- POST /api/staff/inventory/adjust
- 任何写操作
- Auth / JWT / 微信登录 / 手机号绑定
- 真实支付网关
