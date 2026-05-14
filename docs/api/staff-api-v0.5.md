# Staff API v0.5 - 取消工单接口

本文档在 v0.4 提交工单接口基础上，新增 Staff 端取消工单接口。

## 1. 范围

### 1.1 本次新增

| 模块 | Controller | 接口 | 说明 |
|------|------------|------|------|
| WorkOrder | StaffWorkOrderController | POST /api/staff/work-orders/{workOrderId}/cancel | 取消工单；已提交未结算工单释放预占库存 |

### 1.2 本次未实现

- 工单结算
- 支付 / 退款
- 报销
- 反结算
- 退货回库
- 自动退款
- Excel 导出
- Auth/JWT/微信登录/手机号绑定
- DB schema 修改

## 2. 认证与上下文

当前仍使用 dev-only Header 注入 CurrentUserContext：

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 是 | 当前操作员 ID，写入 operatorId |
| X-Store-Id | 是 | 当前门店 ID，写入 storeId |

Staff 写接口必须从 CurrentUserContext 获取 storeId / operatorId，不信任请求体或 query 中的同名字段。

## 3. 接口

### POST /api/staff/work-orders/{workOrderId}/cancel

取消工单。允许状态以后端现有 WorkOrderService.cancel 为准：DRAFT 可取消；PENDING_ACCEPT / ACCEPTED / PART_ORDERED / PART_ARRIVED 等已提交未结算状态可取消；SETTLED / CANCELLED 不允许取消。

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workOrderId | Long | 是 | 工单 ID |

**请求体（StaffCancelWorkOrderRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | String | 是 | 取消原因，不能为空 |
| remark | String | 否 | 前端备注字段；当前核心 Service 仅使用 reason |

**请求示例：**

```http
POST /api/staff/work-orders/400/cancel
Content-Type: application/json
X-User-Id: 10
X-Store-Id: 1

{
  "reason": "客户暂不维修",
  "remark": "客户要求保留旧件"
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
    "status": "CANCELLED",
    "receivableAmount": 120.50,
    "receivedAmount": 0,
    "remark": "草稿备注",
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

1. DRAFT 工单可以取消。
2. 已提交未结算工单可以取消，具体允许状态以后端 WorkOrderService.cancel 为准。
3. SETTLED 工单不能取消。
4. CANCELLED 工单不能重复取消。
5. DRAFT 取消不释放库存，不生成 RELEASE。
6. 已提交未结算工单取消会释放预占库存。
7. 释放预占库存时生成 RELEASE 库存流水。
8. cancel 不生成 CONSUME 库存流水。
9. cancel 不生成 payment_record。
10. cancel 不生成 refund_record。
11. cancel 不自动退款。
12. cancel 不结算、不反结算。
13. cancel 不做退货回库。
14. 取消动作复用 WorkOrderService.cancel，事务和库存规则由现有 Service 负责。

## 5. 库存释放说明

### 5.1 DRAFT 取消

DRAFT 工单尚未提交，不存在本工单产生的库存预占，因此取消时：

| 字段 | 变化 |
|------|------|
| actualQty | 不变 |
| availableQty | 不变 |
| reservedQty | 不变 |
| inventory_flow | 不新增 RELEASE |

### 5.2 已提交未结算取消

已提交未结算工单在 submit 时已生成 RESERVE 预占。取消时，后端释放对应预占库存：

| 字段 | 变化 |
|------|------|
| actualQty | 不变 |
| availableQty | 增加 |
| reservedQty | 减少 |
| inventory_flow | 新增 RELEASE |

正式扣减实际库存只发生在结算阶段；本接口不会生成 CONSUME。

## 6. 错误响应

错误响应保持统一 ApiResponse 结构：

```json
{
  "code": "WORK_ORDER_CANCEL_NOT_ALLOWED",
  "message": "当前工单状态不允许取消",
  "data": null,
  "traceId": null
}
```

常见错误码：

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| COMMON_BAD_REQUEST | 400 | 缺少 dev header / 参数校验失败 |
| WORK_ORDER_NOT_FOUND | 400 | 工单不存在或不属于当前门店 |
| WORK_ORDER_CANCEL_REASON_REQUIRED | 400 | Service 层取消原因为空 |
| WORK_ORDER_CANCEL_NOT_ALLOWED | 400 | 当前状态不允许取消 |
| INVENTORY_RESERVED_NOT_ENOUGH | 400 | 释放预占库存时预占数量不足 |

## 7. 实现确认

- 不新增 Controller 模块，只扩展 StaffWorkOrderController。
- 不修改数据库 schema。
- 不修改库存 RESERVE / RELEASE / CONSUME 核心规则。
- 不修改支付 / 退款金额计算。
- 不新增结算、支付、退款接口。
- storeId / operatorId 来自 CurrentUserContext。
