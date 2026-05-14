# Staff API v0.4 - 提交工单接口

本文档在 v0.3 工单草稿与费用明细写接口基础上，新增 Staff 端提交工单接口。

## 1. 范围

### 1.1 本次新增

| 模块 | Controller | 接口 | 说明 |
|------|------------|------|------|
| WorkOrder | StaffWorkOrderController | POST /api/staff/work-orders/{workOrderId}/submit | 提交 DRAFT 工单并预占库存 |

### 1.2 本次未实现

- 工单取消
- 工单结算
- 支付 / 退款
- 报销
- 财务汇总
- Excel 导出
- Auth/JWT/微信登录/手机号绑定
- DB schema 修改

## 2. 认证与上下文

当前仍使用 dev-only Header 注入 CurrentUserContext：

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 是 | 当前操作员 ID，写入 operatorId |
| X-Store-Id | 是 | 当前门店 ID，写入 storeId |

正式认证实现前，Staff 端写接口必须从 CurrentUserContext 获取 storeId / operatorId，不信任请求体或 query 中的同名字段。

## 3. 接口

### POST /api/staff/work-orders/{workOrderId}/submit

提交工单。仅 DRAFT 工单允许提交，提交成功后由后端现有规则进入 PENDING_ACCEPT。

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workOrderId | Long | 是 | 工单 ID |

**请求体（StaffSubmitWorkOrderRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| remark | String | 否 | 提交备注 |

**请求示例：**

```http
POST /api/staff/work-orders/400/submit
Content-Type: application/json
X-User-Id: 10
X-Store-Id: 1

{
  "remark": "客户确认维修方案，提交工单"
}
```

**响应（ApiResponse<StaffWorkOrderDetail>）：**

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "id": 400,
    "workOrderNo": "WO202605140001",
    "customerNameSnapshot": "张三",
    "customerPhoneSnapshot": "13800138000",
    "vehicleModelSnapshot": "NQi",
    "frameNoSnapshot": "FRAME001",
    "batteryNoSnapshot": "BAT001",
    "repairItem": "更换电池",
    "status": "PENDING_ACCEPT",
    "receivableAmount": 120.50,
    "receivedAmount": 0,
    "remark": "客户确认维修方案，提交工单",
    "createdAt": "2026-05-14T12:00:00",
    "chargeItems": [
      {
        "id": 9001,
        "chargeType": "PART",
        "itemName": "48V20Ah电池",
        "partId": 100,
        "partCodeSnapshot": "P-0001",
        "partNameSnapshot": "48V20Ah电池",
        "partSourceSnapshot": "OFFICIAL",
        "quantity": 1,
        "unit": "个",
        "unitPrice": 120.50,
        "lineAmount": 120.50,
        "remark": null
      }
    ]
  },
  "traceId": null
}
```

**Staff 响应不暴露字段：**

- 工单：storeId / customerId / vehicleId
- 费用明细：costPriceSnapshot / lineCostAmount / inventoryAffecting / tempPart / status / workOrderId

## 4. 业务边界

1. 只允许 DRAFT 状态提交。
2. 提交成功后进入 PENDING_ACCEPT。
3. submit 才触发库存预占。
4. 对 PART 且影响库存的费用明细预占库存。
5. LABOR / OTHER 不影响库存。
6. 库存不足时提交失败，工单仍保持 DRAFT。
7. 成功提交后生成 RESERVE 库存流水。
8. submit 不生成 CONSUME 库存流水。
9. submit 不生成 RELEASE 库存流水。
10. submit 不生成 payment_record。
11. submit 不生成 refund_record。
12. submit 不收款。
13. submit 不结算。
14. 重复提交或非 DRAFT 提交失败。
15. 提交动作复用 WorkOrderService.submit，事务和库存规则由现有 Service 负责。

## 5. 库存预占说明

提交时，后端会汇总工单内需要影响库存的 PART 明细数量，并调用现有库存预占逻辑：

| 字段 | 变化 |
|------|------|
| actualQty | 不变 |
| availableQty | 减少 |
| reservedQty | 增加 |
| inventory_flow | 新增 RESERVE |

结算时才会正式扣减库存；本接口不做结算，也不会生成 CONSUME。

## 6. 错误响应

错误响应保持统一 ApiResponse 结构：

```json
{
  "code": "WORK_ORDER_NOT_DRAFT",
  "message": "工单不是草稿状态",
  "data": null,
  "traceId": null
}
```

常见错误码：

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| COMMON_BAD_REQUEST | 400 | 缺少 dev header / 参数错误 |
| WORK_ORDER_NOT_FOUND | 400 | 工单不存在或不属于当前门店 |
| WORK_ORDER_NOT_DRAFT | 400 | 工单非 DRAFT，不能提交 |
| INVENTORY_STOCK_NOT_FOUND | 400 | 配件库存不存在 |
| INVENTORY_AVAILABLE_NOT_ENOUGH | 400 | 可用库存不足 |

## 7. 实现确认

- 不新增 Controller 模块，只扩展 StaffWorkOrderController。
- 不修改数据库 schema。
- 不修改库存 RESERVE / RELEASE / CONSUME 核心规则。
- 不修改支付 / 退款金额计算。
- 不新增取消、结算、支付、退款接口。
- storeId / operatorId 来自 CurrentUserContext。
