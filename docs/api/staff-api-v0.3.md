# Staff API v0.3 — 工单草稿与费用明细写接口

本文档在 v0.2 配件入库接口基础上，新增 Staff 端工单草稿与费用明细写接口。

## 1. 范围

### 1.1 本次新增

| 模块 | Controller | 接口 | 说明 |
|------|-----------|------|------|
| WorkOrder | StaffWorkOrderController | POST /api/staff/work-orders/drafts | 创建工单草稿 |
| WorkOrder | StaffWorkOrderController | PUT /api/staff/work-orders/{workOrderId}/draft | 修改草稿基础信息 |
| WorkOrder | StaffWorkOrderController | POST /api/staff/work-orders/{workOrderId}/charge-items | 添加费用明细 |
| WorkOrder | StaffWorkOrderController | PUT /api/staff/work-orders/{workOrderId}/charge-items/{chargeItemId} | 修改费用明细 |
| WorkOrder | StaffWorkOrderController | DELETE /api/staff/work-orders/{workOrderId}/charge-items/{chargeItemId} | 删除费用明细 |

### 1.2 本次未实现

- 工单提交
- 工单取消
- 工单结算
- 支付记录
- 退款记录
- 库存预占 / 释放 / 扣减
- 报销
- Auth/JWT/微信登录/手机号绑定
- 真实支付网关
- DB schema 修改

## 2. 业务边界

1. 只允许 DRAFT 阶段编辑草稿和费用明细
2. 创建草稿不预占库存
3. 添加 PART 明细不预占库存
4. 修改/删除费用明细不生成库存流水
5. receivableAmount 由后端按费用明细重算
6. receivedAmount 在草稿阶段通常为 0
7. PART 类型费用明细必须关联 partId
8. LABOR / OTHER 类型不应要求 partId
9. Staff 请求不传 costPriceSnapshot / lineCostAmount
10. storeId / operatorId 来自 CurrentUserContext（X-Store-Id / X-User-Id header），不信任请求体
11. Staff 响应 DTO 不暴露成本敏感字段（costPriceSnapshot / lineCostAmount / inventoryAffecting / tempPart / storeId / customerId / vehicleId）

## 3. Endpoints

### 3.1 POST /api/staff/work-orders/drafts

创建工单草稿。

**请求头（必填）：**

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 是 | 当前操作员 ID |
| X-Store-Id | 是 | 当前门店 ID |

**请求体（StaffCreateDraftWorkOrderRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerNameSnapshot | String | 是 | 客户姓名 |
| customerPhoneSnapshot | String | 否 | 客户电话 |
| vehicleModelSnapshot | String | 否 | 车型 |
| frameNoSnapshot | String | 否 | 车架号 |
| batteryNoSnapshot | String | 否 | 电池编号 |
| repairItem | String | 是 | 维修项目 |
| remark | String | 否 | 备注 |

**请求示例：**

```json
{
  "customerNameSnapshot": "张三",
  "customerPhoneSnapshot": "13800138000",
  "vehicleModelSnapshot": "NQi",
  "frameNoSnapshot": "FRAME001",
  "batteryNoSnapshot": "BAT001",
  "repairItem": "更换轮胎",
  "remark": "紧急维修"
}
```

**响应（ApiResponse<StaffWorkOrderDetail>）：**

复用现有 StaffWorkOrderDetail 结构，包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 工单 ID |
| workOrderNo | String | 工单编号 |
| customerNameSnapshot | String | 客户姓名 |
| customerPhoneSnapshot | String | 客户电话 |
| vehicleModelSnapshot | String | 车型 |
| frameNoSnapshot | String | 车架号 |
| batteryNoSnapshot | String | 电池编号 |
| repairItem | String | 维修项目 |
| status | String | 工单状态（DRAFT） |
| receivableAmount | BigDecimal | 应收金额（0） |
| receivedAmount | BigDecimal | 实收金额（0） |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| chargeItems | List | 费用明细列表（空） |

**不暴露字段：** storeId / customerId / vehicleId / chargeItems 中的 costPriceSnapshot / lineCostAmount / inventoryAffecting / tempPart

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| COMMON_BAD_REQUEST | 400 | 缺少 header / 参数校验失败（customerNameSnapshot 为空、repairItem 为空） |

### 3.2 PUT /api/staff/work-orders/{workOrderId}/draft

修改草稿基础信息。仅 DRAFT 状态可修改。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| workOrderId | Long | 工单 ID |

**请求体（StaffUpdateDraftWorkOrderRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerNameSnapshot | String | 否 | 客户姓名 |
| customerPhoneSnapshot | String | 否 | 客户电话 |
| vehicleModelSnapshot | String | 否 | 车型 |
| frameNoSnapshot | String | 否 | 车架号 |
| batteryNoSnapshot | String | 否 | 电池编号 |
| repairItem | String | 否 | 维修项目 |
| remark | String | 否 | 备注 |

**请求示例：**

```json
{
  "customerNameSnapshot": "李四",
  "repairItem": "更换电机",
  "remark": "更新备注"
}
```

**响应（ApiResponse<StaffWorkOrderDetail>）：** 同 3.1

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| WORK_ORDER_NOT_DRAFT | 400 | 工单非 DRAFT 状态 |
| WORK_ORDER_NOT_FOUND | 400 | 工单不存在 |

### 3.3 POST /api/staff/work-orders/{workOrderId}/charge-items

添加费用明细。仅 DRAFT 状态可添加。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| workOrderId | Long | 工单 ID |

**请求体（StaffAddChargeItemRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chargeType | String | 是 | 费用类型：PART / LABOR / OTHER |
| itemName | String | 是 | 项目名称 |
| partId | Long | PART 必填 | 配件 ID（仅 PART 类型必填） |
| quantity | Integer | 是 | 数量，必须 > 0 |
| unit | String | 否 | 单位 |
| unitPrice | BigDecimal | 是 | 单价，不能为负数 |
| remark | String | 否 | 备注 |

**请求示例：**

```json
{
  "chargeType": "PART",
  "itemName": "48V20Ah电池",
  "partId": 100,
  "quantity": 2,
  "unitPrice": 120.50
}
```

**响应（ApiResponse<StaffChargeItemIdResponse>）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| chargeItemId | Long | 新增费用明细 ID |

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| COMMON_BAD_REQUEST | 400 | 参数校验失败（quantity≤0、缺少必填字段） |
| WORK_ORDER_NOT_DRAFT | 400 | 工单非 DRAFT 状态 |
| PART_REQUIRED_FOR_PART_CHARGE | 400 | PART 类型缺少 partId |
| CHARGE_PRICE_INVALID | 400 | 单价不能为负数 |

### 3.4 PUT /api/staff/work-orders/{workOrderId}/charge-items/{chargeItemId}

修改费用明细。仅 DRAFT 状态可修改。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| workOrderId | Long | 工单 ID |
| chargeItemId | Long | 费用明细 ID |

**请求体（StaffUpdateChargeItemRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| itemName | String | 否 | 项目名称 |
| quantity | Integer | 是 | 数量，必须 > 0 |
| unit | String | 否 | 单位 |
| unitPrice | BigDecimal | 否 | 单价 |
| remark | String | 否 | 备注 |

**请求示例：**

```json
{
  "itemName": "48V20Ah电池",
  "quantity": 3,
  "unitPrice": 130.00
}
```

**响应（ApiResponse<Void>）：** 无 data

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| COMMON_BAD_REQUEST | 400 | 参数校验失败 |
| WORK_ORDER_NOT_DRAFT | 400 | 工单非 DRAFT 状态 |
| CHARGE_ITEM_NOT_FOUND | 400 | 费用明细不存在 |

### 3.5 DELETE /api/staff/work-orders/{workOrderId}/charge-items/{chargeItemId}

删除费用明细。仅 DRAFT 状态可删除。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| workOrderId | Long | 工单 ID |
| chargeItemId | Long | 费用明细 ID |

**响应（ApiResponse<Void>）：** 无 data

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| WORK_ORDER_NOT_DRAFT | 400 | 工单非 DRAFT 状态 |
| CHARGE_ITEM_NOT_FOUND | 400 | 费用明细不存在 |

注：删除费用明细不生成库存流水（DRAFT 阶段未预占库存）。

## 4. Service 复用说明

| Staff Controller 方法 | 复用的 Service | 说明 |
|----------------------|---------------|------|
| createDraft | WorkOrderService.createDraft() | 完全复用 |
| updateDraft | WorkOrderService.updateDraft() | 完全复用 |
| addChargeItem | WorkOrderService.addChargeItem() | 完全复用 |
| updateChargeItem | WorkOrderService.updateChargeItem() | 完全复用 |
| deleteChargeItem | WorkOrderService.removeChargeItem() | 完全复用 |

Controller 职责：
1. 从 CurrentUserContext 获取 storeId / operatorId
2. 将 Staff Request 映射为 Service Command
3. 调用 WorkOrderService 对应方法
4. 读取更新后的工单详情，构建 Staff 响应
5. 响应裁剪，不暴露成本敏感字段

## 5. 不影响范围

- 不影响工单提交/取消/结算
- 不影响支付/退款
- 不影响库存预占/释放/扣减
- 不影响报销
- 不修改数据库 schema
- 不修改库存核心规则
- 不新增 Service 核心逻辑
- DRAFT 编辑阶段不生成库存流水
