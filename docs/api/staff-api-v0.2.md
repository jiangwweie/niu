# Staff API v0.2 — 配件入库写接口

本文档在 v0.1 只读接口基础上，新增 Staff 端配件入库写接口。

## 1. 范围

### 1.1 本次新增

| 模块 | Controller | 接口 | 说明 |
|------|-----------|------|------|
| Inventory | StaffInventoryController | POST /api/staff/inventory/inbound | 配件入库 |

### 1.2 本次未实现

- 工单创建/草稿、提交、取消、结算
- 收费项新增/修改/删除
- 支付记录、退款记录
- 库存调整
- 报销
- Auth/JWT/微信登录/手机号绑定
- 真实支付网关
- DB schema 修改

## 2. 业务边界

1. 入库后生成 INBOUND 库存流水
2. actualQty 增加
3. availableQty 增加
4. reservedQty 不变
5. quantity 必须 > 0
6. unitCost 不能为负数
7. partId 必须存在且属于当前门店
8. 已停用配件不允许入库（复用 InventoryService 校验）
9. storeId / operatorId 来自 CurrentUserContext（X-Store-Id / X-User-Id header），不信任请求体
10. Staff 查询 DTO 不暴露 referenceCostPrice / costPriceSnapshot / lineCostAmount 等成本字段

## 3. Endpoint

### POST /api/staff/inventory/inbound

配件入库。

**请求头（必填）：**

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 是 | 当前操作员 ID |
| X-Store-Id | 是 | 当前门店 ID |

**请求体（StaffInventoryInboundRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| partId | Long | 是 | 配件 ID |
| quantity | Integer | 是 | 入库数量，必须 > 0 |
| unitCost | BigDecimal | 否 | 入库单价，不能为负数 |
| barcode | String | 否 | 条码 |
| locationRemark | String | 否 | 存放位置 |
| reason | String | 否 | 入库原因 |
| remark | String | 否 | 备注 |

**请求示例：**

```json
{
  "partId": 100,
  "quantity": 10,
  "unitCost": 120.50,
  "barcode": "BC001",
  "locationRemark": "A区货架",
  "reason": "补货",
  "remark": "常规补货"
}
```

**响应（ApiResponse<StaffInventoryInboundResponse>）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| partId | Long | 配件 ID |
| partCode | String | 配件编码 |
| partName | String | 配件名称 |
| actualQty | Integer | 入库后实际库存 |
| availableQty | Integer | 入库后可用库存 |
| reservedQty | Integer | 预占库存（不变） |
| flowId | Long | 入库流水 ID |
| operatedAt | LocalDateTime | 操作时间 |

**响应示例：**

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "partId": 100,
    "partCode": "TEST-001",
    "partName": "48V20Ah电池",
    "actualQty": 110,
    "availableQty": 90,
    "reservedQty": 20,
    "flowId": 5001,
    "operatedAt": "2026-05-14T10:30:00"
  },
  "traceId": null
}
```

**不暴露字段：**
- unitCost（写入时传入，但响应不返回）
- referenceCostPrice / costPriceSnapshot / lineCostAmount（成本敏感字段）
- storeId / operatorId（内部字段）

**错误响应：**

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| COMMON_BAD_REQUEST | 400 | 缺少 header / 参数校验失败（包括 quantity≤0、unitCost<0） |
| PART_NOT_FOUND | 400 | 配件不存在 |
| PART_DISABLED | 400 | 配件已停用 |
| INVENTORY_QTY_MUST_POSITIVE | 400 | 入库数量必须大于 0（Service 层校验，正常情况下 Bean Validation 先拦截） |
| INBOUND_UNIT_COST_NEGATIVE | 400 | 入库单价不能为负数（Service 层校验，正常情况下 Bean Validation 先拦截） |

注：quantity≤0 和 unitCost<0 在 Controller 层由 Bean Validation（@Positive / @DecimalMin）拦截，返回 COMMON_BAD_REQUEST。Service 层也有对应校验作为兜底。

## 4. Service 复用说明

| Staff Controller 方法 | 复用的 Service | 说明 |
|----------------------|---------------|------|
| inbound | InventoryService.inbound() | 完全复用，不新增 Service 逻辑 |

Controller 职责：
1. 从 CurrentUserContext 获取 storeId / operatorId
2. 将 StaffInventoryInboundRequest 映射为 InventoryInboundCommand
3. 调用 InventoryService.inbound()
4. 读取更新后的库存和流水，构建 StaffInventoryInboundResponse
5. 响应裁剪，不暴露成本敏感字段

## 5. 不影响范围

- 不影响工单创建/提交/取消/结算
- 不影响支付/退款
- 不影响库存调整
- 不影响报销
- 不修改数据库 schema
- 不修改库存核心规则
- 不新增 Service 核心逻辑
