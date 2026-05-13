# Admin API 联调说明

本文档为前端与后端 Admin API 首轮联调提供快速接入指南。

## 1. 后端启动

```bash
# 启动 MySQL Docker 容器（首次需要）
docker run -d --name xiaoniu-mysql-dev \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=xiaoniu_aftermarket_dev \
  -p 3306:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci

# 首次启动修复 JDBC auth（仅需执行一次）
docker exec xiaoniu-mysql-dev mysql -uroot -proot -e \
  "ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'root'; FLUSH PRIVILEGES;"

# 启动后端（dev profile）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端默认运行在 `http://localhost:8080`。

详细 MySQL 搭建说明见：`docs/dev/mysql-dev-setup.md`

## 2. Dev Header

所有 `/api/admin/**` 接口需要以下 HTTP Header：

```
X-User-Id: 1
X-Store-Id: 1
```

- `X-User-Id`：当前操作用户 ID（Long）
- `X-Store-Id`：当前门店 ID（Long）

> **重要**：这是 dev-only 临时方案，不是正式认证系统。正式 Auth 中间件实现后会替代。

不发送这两个 Header 时，接口会返回：

```json
{
  "code": "COMMON_BAD_REQUEST",
  "message": "缺少用户上下文",
  "data": null,
  "traceId": null
}
```

## 3. CORS

后端已配置 CORS，允许以下本地开发源访问：

- `http://localhost:5173`（Vite 默认）
- `http://localhost:3000`（Create React App 默认）
- `http://localhost:8081`

如果前端运行在其他端口，需要在 `AdminWebMvcConfig.java` 的 `addCorsMappings` 中添加对应 origin。

## 4. Base URL

```javascript
const BASE_URL = 'http://localhost:8080'
const headers = {
  'Content-Type': 'application/json',
  'X-User-Id': '1',
  'X-Store-Id': '1',
}
```

## 5. 统一响应格式

### 成功响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": { ... },
  "traceId": null
}
```

### 错误响应

```json
{
  "code": "WORK_ORDER_NOT_FOUND",
  "message": "工单不存在",
  "data": null,
  "traceId": null
}
```

### 分页响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "records": [ ... ],
    "pageNo": 1,
    "pageSize": 20,
    "total": 100
  },
  "traceId": null
}
```

前端计算总页数：`Math.ceil(total / pageSize)`

## 6. 首轮联调顺序

推荐按以下顺序逐步接入：

### 6.1 Dict（字典查询）

```
GET /api/admin/dict/types                    获取所有启用的字典类型列表
GET /api/admin/dict/types/{typeCode}/items   获取某类型下的字典项
```

获取字典类型列表（替代硬编码）：

```json
{
  "code": "SUCCESS",
  "data": [
    { "typeCode": "WORK_ORDER_STATUS", "typeName": "工单状态", "enabled": true },
    { "typeCode": "PAYMENT_METHOD", "typeName": "支付方式", "enabled": true }
  ]
}
```

获取字典项列表：

```json
{
  "code": "SUCCESS",
  "data": [
    { "itemCode": "DRAFT", "itemName": "草稿", "sortOrder": 1, "enabled": true }
  ]
}
```

说明：
- `typeCode` 不存在或已停用时返回空数组 `[]`，不报错。
- `/types` 端点无需 `X-User-Id` / `X-Store-Id` Header。
- 前端不接字典类型的新增、编辑、停用。

常用 typeCode：
- `WORK_ORDER_STATUS` — 工单状态
- `PAYMENT_METHOD` — 支付方式
- `PART_SOURCE` — 配件来源
- `CHARGE_ITEM_TYPE` — 费用项目类型

### 6.2 Part（配件管理）

```
GET    /api/admin/parts                          分页查询（支持筛选参数）
POST   /api/admin/parts/third-party              创建第三方配件
POST   /api/admin/parts/official                 创建官方配件
PUT    /api/admin/parts/{partId}                 修改配件
POST   /api/admin/parts/{partId}/enable          启用
POST   /api/admin/parts/{partId}/disable         停用
```

配件列表查询参数：

| 参数 | 类型 | 匹配方式 | 说明 |
| --- | --- | --- | --- |
| partCode | String | 精确 | 配件编码 |
| partName | String | 模糊 | 配件名称 |
| officialPartNo | String | 精确 | 官方配件编号 |
| model | String | 模糊 | 车型 |
| categoryCode | String | 精确 | 配件分类编码 |
| source | String | 精确 | 配件来源 (OFFICIAL/THIRD_PARTY) |
| enabled | Boolean | — | 启用状态 |
| pageNo | Integer | — | 页码，默认 1 |
| pageSize | Integer | — | 每页数量，默认 20 |

### 6.3 Inventory（库存管理）

```
GET    /api/admin/inventory/stocks               分页查询库存
GET    /api/admin/inventory/stocks/{partId}      查询某配件库存
POST   /api/admin/inventory/inbound              入库
POST   /api/admin/inventory/adjust               库存调整
GET    /api/admin/inventory/flows                库存流水查询
```

### 6.4 WorkOrder（工单管理）

```
GET    /api/admin/work-orders                    分页查询
GET    /api/admin/work-orders/{id}               详情
POST   /api/admin/work-orders/drafts             创建草稿
PUT    /api/admin/work-orders/{id}/draft         修改草稿
POST   /api/admin/work-orders/{id}/charge-items  添加费用项
PUT    /api/admin/work-orders/{id}/charge-items/{cid}  修改费用项
DELETE /api/admin/work-orders/{id}/charge-items/{cid}  删除费用项
POST   /api/admin/work-orders/{id}/submit        提交
POST   /api/admin/work-orders/{id}/cancel        取消
POST   /api/admin/work-orders/{id}/settle        结算
```

### 6.5 Payment（支付记录）

```
GET    /api/admin/work-orders/{id}/payments           工单支付记录
POST   /api/admin/work-orders/{id}/payments           记录支付
GET    /api/admin/work-orders/{id}/payment-summary    支付汇总
GET    /api/admin/payments                            全局分页查询
```

### 6.6 Refund（退款记录）

```
GET    /api/admin/work-orders/{id}/refunds       工单退款记录
POST   /api/admin/work-orders/{id}/refunds       记录退款
GET    /api/admin/refunds                        全局分页查询
```

### 6.7 OfficialAfterSales（官方售后）

```
GET    /api/admin/official-after-sales                            分页查询
GET    /api/admin/work-orders/{id}/official-after-sales           查询
POST   /api/admin/work-orders/{id}/official-after-sales/order-info    保存订单号
POST   /api/admin/work-orders/{id}/official-after-sales/settle        标记已结算
POST   /api/admin/work-orders/{id}/official-after-sales/no-settlement-required  标记无需结算
```

## 7. 暂不接入的接口

以下接口当前未实现，前端不得对接：

- **Auth/User/Role/Permission** — 认证和权限管理
- **Reimbursement** — 报销
- **Finance** — 财务汇总
- **Export** — Excel 导出
- **Mobile/Staff** — 小程序端 API

## 8. 典型业务链路

```
1. 创建第三方配件       POST /api/admin/parts/third-party
2. 入库                 POST /api/admin/inventory/inbound
3. 创建 DRAFT 工单      POST /api/admin/work-orders/drafts
4. 添加费用项           POST /api/admin/work-orders/{id}/charge-items
5. 提交工单             POST /api/admin/work-orders/{id}/submit
6. 记录支付             POST /api/admin/work-orders/{id}/payments
7. 结算工单             POST /api/admin/work-orders/{id}/settle
8. 查询支付/退款/官方售后列表
```

## 9. 常见错误码

| 错误码 | 含义 | 触发场景 |
|--------|------|---------|
| `COMMON_BAD_REQUEST` | 请求参数错误 | 缺少 Header、参数校验失败 |
| `PART_NOT_FOUND` | 配件不存在 | 查询不存在的配件 |
| `INVENTORY_AVAILABLE_NOT_ENOUGH` | 可用库存不足 | 入库/调整后可用库存为负 |
| `WORK_ORDER_NOT_FOUND` | 工单不存在 | 查询不存在的工单 |
| `WORK_ORDER_NOT_DRAFT` | 工单状态不是草稿 | 对非草稿工单编辑 |
| `WORK_ORDER_SETTLE_NOT_ALLOWED` | 不允许结算 | 工单状态不允许结算 |
| `WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH` | 实收不足 | 结算时实收 < 应收 |
| `WORK_ORDER_CANCEL_NOT_ALLOWED` | 不允许取消 | 工单状态不允许取消 |
| `REFUND_EXCEEDS_PAID_AMOUNT` | 退款超过可退金额 | 退款金额 > (支付-已退) |
| `OFFICIAL_AFTER_SALES_NOT_FOUND` | 官方售后记录不存在 | 查询不存在的官方售后 |

## 10. 金额与时间格式

- 金额字段：`BigDecimal`，JSON 中为 number（如 `120.00`）
- 时间字段：`LocalDateTime`，JSON 格式为 ISO-8601（如 `2026-05-13T10:30:00`）

## 11. API 契约文档

完整 API 契约见：

- `docs/api/admin-api-v0.1.md` — 基础契约
- `docs/api/admin-api-v0.2-delta.md` — v0.2 查询增强

curl 示例见：`docs/dev/admin-api-examples.http`
