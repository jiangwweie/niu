# 06_ARCHITECTURE_AND_API_STYLE.md

# 小牛电动官方授权店两轮车维修售后库存管理系统 - 架构与 API 风格文档

版本：v0.2  
日期：2026-05-10  
阶段：项目准备阶段 Step 6 / 8  
修订依据：工单费用项目明细模型升级 2026-05-10  
前置文档：

- `01_MVP_SCOPE.md`
- `02_BUSINESS_FLOWS.md`
- `03_CORE_BUSINESS_RULES.md`
- `04_DOMAIN_MODEL.md` v0.3
- `05_DATABASE_DESIGN.md` v0.3

用途：用于明确 MVP 阶段系统技术架构、模块边界、API 风格、状态动作接口、事务边界、权限校验、前后端职责和 Codex / Claude 实现约束。

---

## 1. 文档目的

本文档用于回答：

1. 系统采用什么技术架构？
2. 后端如何分模块？
3. 管理端和小程序分别承担什么职责？
4. API 如何设计，避免普通 CRUD 破坏业务规则？
5. 哪些接口必须是业务动作接口？
6. 哪些场景必须使用事务？
7. 权限、认证、错误码、分页、导出如何统一？
8. Codex 实现时必须遵守哪些架构护栏？
9. Claude 实现页面和测试时不能越过哪些边界？

本文档不是详细接口清单，也不是 OpenAPI 完整定义。  
详细接口文档可在 Codex 生成后端骨架或每个模块任务卡时逐步补充。

---

## 2. 总体架构结论

MVP 阶段采用：

```text
微信原生小程序员工端
        ↓
Spring Boot REST API
        ↓
MySQL 8

Vue3 + Element Plus 网页管理端
        ↓
Spring Boot REST API
        ↓
MySQL 8
```

后端架构：

```text
Java Spring Boot 3
模块化单体 Modular Monolith
MySQL 8
MyBatis / MyBatis-Plus
JWT / Sa-Token / Spring Security 之一
EasyExcel
```

部署架构：

```text
单机部署优先
Nginx + Spring Boot + MySQL
Docker Compose 可选
```

当前不采用：

```text
微服务
分布式事务
消息队列 MQ
Redis
Elasticsearch
复杂工作流引擎
低代码平台
复杂多租户系统
复杂 BI 系统
```

---

## 3. 架构原则

### 3.1 模块化单体

系统采用模块化单体，而不是微服务。

原因：

```text
1. MVP 单门店，业务复杂但规模不大。
2. Java Spring Boot + MySQL 足以支撑。
3. 一个人主导开发，AI 辅助，不适合过早拆微服务。
4. 库存、工单、支付、财务之间需要强事务一致性。
5. 微服务会引入部署、网络、事务和调试复杂度。
```

模块化单体要求：

```text
代码按领域模块分包
模块边界清晰
核心业务动作通过应用服务协调
不把所有逻辑堆在 Controller
不把所有模块写成简单 CRUD
```

---

### 3.2 后端是业务规则权威

后端必须负责最终业务判断。

小程序和管理端不得作为以下规则的最终判断来源：

```text
库存是否足够
是否允许提交工单
是否允许取消工单
是否允许结算工单
是否允许退款
是否允许库存调整
是否允许报销确认
官方结算是否计入官方收入
财务统计口径
权限判断
```

前端可以做提示和基础校验，但后端必须再次校验。

---

### 3.3 核心状态变化必须走业务动作接口

禁止通过普通 update 接口修改核心状态。

错误方式：

```text
PUT /api/work-orders/{id}
body: { "status": "SETTLED" }
```

正确方式：

```text
POST /api/work-orders/{id}/submit
POST /api/work-orders/{id}/cancel
POST /api/work-orders/{id}/settle
```

原因：

```text
提交工单会预占库存
取消工单会释放库存
结算工单会校验实收并扣减库存
这些都不是普通字段更新
```

---

### 3.4 明细和流水优先

系统关键数据必须通过明细或流水追溯。

包括：

```text
库存变化 → inventory_flow
支付 → payment_record
退款 → refund_record
工单状态变化 → work_order_status_log
官方结算 → official_after_sales
报销 → reimbursement
```

不得用单个总字段替代明细。

---

### 3.5 财务报表从明细汇总

MVP 不建可手动修改利润的财务主表。

财务统计来源：

```text
work_order
work_order_charge_item
payment_record
refund_record
official_after_sales
reimbursement
```

财务报表只读汇总，不直接写利润。

---

## 4. 后端模块划分

推荐包结构：

```text
com.xiaoniu.aftermarket
├── AftermarketApplication.java
├── common
├── auth
├── user
├── dict
├── customer
├── part
├── inventory
├── workorder
├── payment
├── official
├── reimbursement
├── finance
└── export
```

---

## 5. 分层结构建议

每个业务模块建议采用：

```text
module
├── controller
├── application
├── service
├── domain
├── mapper
├── entity
├── dto
└── enums
```

说明：

```text
controller      接收 HTTP 请求，做参数校验和权限入口
application     应用服务，协调跨模块事务和业务动作
service         模块内业务服务
domain          领域模型或领域规则，可简化
mapper          MyBatis / MyBatis-Plus Mapper
entity          数据库实体
dto             Request / Response / Command / Query DTO
enums           后端核心枚举
```

MVP 可以简化，但必须保留以下原则：

```text
Controller 不直接操作 Mapper
Controller 不直接拼业务事务
核心动作放 application/service
跨模块动作放 application
库存变化统一走 inventory 服务
```

---

## 6. 模块职责边界

### 6.1 common 模块

职责：

```text
统一返回结构
统一异常
错误码
分页对象
审计字段填充
当前登录用户上下文
通用工具
幂等辅助
```

不得承载具体业务逻辑。

---

### 6.2 auth 模块

职责：

```text
手机号登录
微信登录
微信绑定手机号
Token 生成与校验
登录态维护
当前用户上下文
```

不负责：

```text
角色权限配置页面
具体业务权限判断规则
```

---

### 6.3 user 模块

职责：

```text
用户管理
角色管理
权限点管理
用户角色关联
角色权限关联
用户启停
```

边界：

```text
权限点由后端稳定编码
角色只是权限点集合
业务代码判断权限点，不判断角色名称
```

---

### 6.4 dict 模块

职责：

```text
字典类型
字典项
展示名称
排序
启停
```

边界：

```text
字典不决定核心状态流转
工单状态流转必须由后端枚举和业务动作控制
```

---

### 6.5 customer 模块

职责：

```text
客户基础信息
车辆基础信息
车架号维修历史查询
```

边界：

```text
MVP 不做复杂 CRM
工单中必须保存客户和车辆快照
```

---

### 6.6 part 模块

职责：

```text
配件主数据
官方配件
第三方配件编码
配件条码
配件启停
```

边界：

```text
part 模块不直接修改库存数量
库存变化必须走 inventory 模块
```

---

### 6.7 inventory 模块

职责：

```text
库存快照
库存流水
正常入库
临时入库
库存预占
库存释放
库存消耗
库存调整
```

核心要求：

```text
所有库存变化必须生成 inventory_flow
其他模块不得直接 update inventory_stock
```

对外提供应用服务能力：

```text
inbound(...)
reserveForWorkOrder(...)
releaseForWorkOrder(...)
consumeForWorkOrder(...)
adjust(...)
adjustReservationForWorkOrderChargeItems(...)
```

---

### 6.8 workorder 模块

职责：

```text
创建工单
编辑 DRAFT 工单
提交工单
取消工单
结算工单
调整已提交工单费用项目（adjust-charge-items）
工单状态日志
维修历史
```

边界：

```text
工单模块不直接 update 库存表
工单提交/取消/结算通过应用服务协调 inventory/payment
```

---

### 6.9 payment 模块

职责：

```text
记录客户支付
记录客户退款
计算支付总额
计算退款总额
计算实收金额
校验可退款金额
```

边界：

```text
payment 模块不负责结算工单状态
payment_record 只表示客户支付
official_after_sales 不得写入 payment_record
```

---

### 6.10 official 模块

职责：

```text
官方售后标记
官方售后订单号
官方结算金额
官方结算状态
官方结算时间
```

边界：

```text
官方结算不影响客户支付实收
官方结算不写入 payment_record
官方结算收入在 finance 中单独统计
```

---

### 6.11 reimbursement 模块

职责：

```text
提交报销
确认报销
驳回/取消报销
报销列表
```

边界：

```text
只有 CONFIRMED 报销进入财务成本
MVP 不做多级审批
MVP 不做自动打款
```

---

### 6.12 finance 模块

职责：

```text
日报
月报
时间范围查询
客户支付净收入
官方结算收入
配件收入
人工费收入
其他收入
配件成本
报销成本
利润
```

边界：

```text
finance 只做汇总查询
不直接修改业务明细
不维护可手工修改利润表
```

---

### 6.13 export 模块

职责：

```text
工单列表导出
库存报表导出
库存流水导出
财务报表导出
利润报表导出
报销台账导出
```

边界：

```text
MVP 同步导出即可
不建 ExportTask
不做复杂自定义模板
导出必须受权限控制
导出结果与查询条件一致
```

---

## 7. API 总体风格

### 7.1 REST 风格

基础风格：

```text
GET     查询
POST    创建或业务动作
PUT     全量更新，谨慎使用
PATCH   局部更新，谨慎使用
DELETE  MVP 尽量不用
```

MVP 不推荐大量使用 DELETE。

原因：

```text
核心业务数据不开放普通删除
主数据优先停用
流水和明细保留追溯
```

---

### 7.2 URL 命名

统一使用：

```text
/api/模块资源
```

示例：

```text
/api/work-orders
/api/inventory/stocks
/api/payments
/api/refunds
/api/reimbursements
```

资源名使用复数：

```text
work-orders
parts
payments
refunds
```

业务动作使用动词子路径：

```text
/api/work-orders/{id}/submit
/api/work-orders/{id}/settle
/api/reimbursements/{id}/confirm
```

---

### 7.3 管理端和小程序 API

MVP 可先共用同一套 API，通过权限控制区分能力。

如后续需要区分，可采用：

```text
/api/admin/...
/api/miniapp/...
```

当前建议：

```text
先不人为拆两套 API
通过权限、DTO 和前端页面控制入口
```

注意：

```text
即使小程序不展示某按钮，后端也必须校验权限
```

---

## 8. 统一响应格式

建议统一返回：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "traceId": "optional"
}
```

失败示例：

```json
{
  "code": "WORK_ORDER_NOT_SETTLEABLE",
  "message": "实收金额不足，不能结算工单",
  "data": null,
  "traceId": "optional"
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| code | 业务状态码 |
| message | 用户可读错误信息 |
| data | 返回数据 |
| traceId | 问题追踪 ID，可选 |

---

## 9. 分页格式

查询列表统一分页参数：

```text
pageNo
pageSize
```

示例：

```text
GET /api/work-orders?pageNo=1&pageSize=20
```

分页返回：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "records": [],
    "pageNo": 1,
    "pageSize": 20,
    "total": 100
  }
}
```

默认规则：

```text
pageNo 从 1 开始
pageSize 默认 20
pageSize 最大 100，导出接口除外
```

---

## 10. 错误码风格

错误码建议按模块前缀：

```text
COMMON_*
AUTH_*
USER_*
PERMISSION_*
PART_*
INVENTORY_*
WORK_ORDER_*
PAYMENT_*
REFUND_*
OFFICIAL_*
REIMBURSEMENT_*
FINANCE_*
EXPORT_*
```

示例：

```text
INVENTORY_NOT_ENOUGH
WORK_ORDER_STATUS_INVALID
WORK_ORDER_ALREADY_SETTLED
PAYMENT_AMOUNT_INVALID
REFUND_AMOUNT_EXCEEDS_PAID
OFFICIAL_SETTLEMENT_INVALID
REIMBURSEMENT_ALREADY_CONFIRMED
PERMISSION_DENIED
```

错误处理原则：

```text
1. 不直接抛数据库错误给前端。
2. 不把 NullPointerException 之类技术异常暴露给用户。
3. 核心业务失败必须返回明确业务错误码。
4. 错误信息要适合小程序和管理端展示。
```

---

## 11. 认证与登录

### 11.1 登录方式

MVP 支持：

```text
手机号登录
微信登录
微信绑定手机号
```

### 11.2 Token 方案

可选：

```text
JWT
Sa-Token
Spring Security + JWT
```

建议：

```text
如果追求简单：Sa-Token
如果追求标准 Spring 体系：Spring Security + JWT
```

Codex 后续可根据项目偏好选择，但必须保证：

```text
1. 当前用户 ID 可获取。
2. 当前用户 store_id 可获取。
3. 当前用户权限点可获取。
4. 操作人字段可自动填充或方便填充。
```

### 11.3 当前用户上下文

后端应统一提供：

```text
CurrentUser
  - userId
  - storeId
  - realName
  - permissionCodes
```

业务服务不得依赖前端传入当前用户 ID。

---

## 12. 权限控制

### 12.1 权限模型

采用：

```text
用户 - 角色 - 权限点
```

业务操作校验权限点，不校验角色名称。

### 12.2 权限校验方式

可以使用：

```text
注解
拦截器
AOP
Service 内显式校验
```

建议核心接口使用注解或统一拦截方式，例如：

```text
@RequirePermission("WORK_ORDER_SETTLE")
```

### 12.3 必须校验权限的动作

```text
用户管理
角色权限管理
字典管理
库存调整
工单提交
工单取消
工单结算
支付记录
退款记录
官方结算录入
报销确认
财务报表查看
Excel 导出
```

### 12.4 数据权限

MVP 单门店，但仍要通过 `store_id` 限制数据。

原则：

```text
用户只能访问自己 store_id 下的数据。
超级管理员在 MVP 也默认只访问默认门店。
```

后续多门店再扩展跨门店数据权限。

---

## 13. 审计字段填充

### 13.1 通用审计

新增记录：

```text
created_by = 当前用户 ID
created_at = 当前时间
updated_by = 当前用户 ID
updated_at = 当前时间
```

更新记录：

```text
updated_by = 当前用户 ID
updated_at = 当前时间
```

### 13.2 业务动作审计

关键动作必须有业务字段：

```text
submitted_by / submitted_at
settled_by / settled_at
cancelled_by / cancelled_at
operator_id / operated_at
confirmed_by / confirmed_at
official_settlement_operator_id / official_settlement_time
```

说明：

```text
通用审计字段不替代业务动作字段。
```

---

## 14. API 资源分组

## 14.1 Auth API

```text
POST /api/auth/login/phone
POST /api/auth/login/wechat
POST /api/auth/wechat/bind-phone
POST /api/auth/logout
GET  /api/auth/me
```

说明：

```text
微信登录后如果没有手机号绑定，需要进入绑定手机号流程。
```

---

## 14.2 User / Role / Permission API

```text
GET  /api/users
POST /api/users
GET  /api/users/{id}
PUT  /api/users/{id}
POST /api/users/{id}/enable
POST /api/users/{id}/disable

GET  /api/roles
POST /api/roles
PUT  /api/roles/{id}
POST /api/roles/{id}/permissions

GET  /api/permissions
```

说明：

```text
用户创建时 phone / username / wechat_openid 至少具备一个登录标识。
角色名称不作为权限判断依据。
```

---

## 14.3 Dict API

```text
GET  /api/dict-types
POST /api/dict-types
PUT  /api/dict-types/{id}

GET  /api/dict-items
POST /api/dict-items
PUT  /api/dict-items/{id}
POST /api/dict-items/{id}/enable
POST /api/dict-items/{id}/disable
```

说明：

```text
工单状态字典只用于展示，不控制状态流转。
```

---

## 14.4 Customer / Vehicle API

```text
GET  /api/customers
POST /api/customers
GET  /api/customers/{id}
PUT  /api/customers/{id}

GET  /api/vehicles
POST /api/vehicles
GET  /api/vehicles/{id}
PUT  /api/vehicles/{id}
GET  /api/vehicles/by-frame-no/{frameNo}/work-orders
```

说明：

```text
工单中保存客户和车辆快照。
车架号用于维修历史追溯。
```

---

## 14.5 Part API

```text
GET  /api/parts
POST /api/parts
GET  /api/parts/{id}
PUT  /api/parts/{id}
POST /api/parts/{id}/enable
POST /api/parts/{id}/disable
GET  /api/parts/by-barcode/{barcode}
```

说明：

```text
part 只管配件主数据。
库存变化不通过 part 接口完成。
```

---

## 14.6 Inventory API

查询类：

```text
GET /api/inventory/stocks
GET /api/inventory/stocks/{id}
GET /api/inventory/flows
```

业务动作类：

```text
POST /api/inventory/inbound
POST /api/inventory/adjust
```

不建议暴露：

```text
PUT /api/inventory/stocks/{id}
```

原因：

```text
库存不能被直接改字段。
必须通过入库、调整、工单提交/取消/结算等业务动作改变。
```

---

## 14.7 WorkOrder API

查询类：

```text
GET /api/work-orders
GET /api/work-orders/{id}
GET /api/work-orders/{id}/charge-items
GET /api/work-orders/{id}/status-logs
```

创建与 DRAFT 编辑：

```text
POST /api/work-orders
PUT  /api/work-orders/{id}
POST /api/work-orders/{id}/charge-items
PUT  /api/work-orders/{id}/charge-items/{itemId}
POST /api/work-orders/{id}/charge-items/{itemId}/remove
```

说明：

```text
以上编辑接口仅允许 DRAFT 草稿阶段使用。
DRAFT 阶段添加、修改、删除 charge items 不影响库存。
```

核心动作：

```text
POST /api/work-orders/{id}/submit
POST /api/work-orders/{id}/cancel
POST /api/work-orders/{id}/settle
POST /api/work-orders/{id}/adjust-charge-items
```

说明：

```text
submit：DRAFT → PENDING_ACCEPT，只对 charge_type = PART 且 inventory_affecting = 1 的明细预占库存
cancel：未结算工单取消，只对 PART 类型明细释放库存
settle：校验实收金额，只对 PART 类型明细扣减库存，状态变 SETTLED
adjust-charge-items：仅用于已提交未结算工单调整费用项目，PART 类型同步预占差异，LABOR / OTHER 只更新明细
```

禁止：

```text
通过 PUT /api/work-orders/{id} 修改状态为 SETTLED 或 CANCELLED
通过普通 charge-item update 修改已提交工单费用明细
```

---

## 14.8 Payment API

```text
GET  /api/payments
GET  /api/work-orders/{id}/payments
POST /api/payments
```

说明：

```text
支付记录只表示客户支付。
支付不自动结算工单。
```

---

## 14.9 Refund API

```text
GET  /api/refunds
GET  /api/work-orders/{id}/refunds
POST /api/refunds
```

说明：

```text
退款必须校验可退款金额。
退款不删除原支付记录。
已结算后退款不自动回滚库存或工单状态。
```

---

## 14.10 Official API

```text
GET  /api/official-after-sales
GET  /api/work-orders/{id}/official-after-sales
POST /api/work-orders/{id}/official-after-sales
PUT  /api/work-orders/{id}/official-after-sales
POST /api/work-orders/{id}/official-after-sales/settle
```

说明：

```text
官方结算金额不写入 payment_record。
official-after-sales/settle 表示标记官方结算，不代表客户支付结算。
```

---

## 14.11 Reimbursement API

```text
GET  /api/reimbursements
POST /api/reimbursements
GET  /api/reimbursements/{id}
PUT  /api/reimbursements/{id}
POST /api/reimbursements/{id}/confirm
POST /api/reimbursements/{id}/reject
POST /api/reimbursements/{id}/cancel
```

说明：

```text
PENDING 可修改或取消。
CONFIRMED 计入成本。
REJECTED_OR_CANCELLED 不计入成本。
```

---

## 14.12 Finance API

```text
GET /api/finance/daily
GET /api/finance/monthly
GET /api/finance/range
```

建议查询参数：

```text
startDate
endDate
```

返回结果应分开展示：

```text
客户支付净收入
官方结算收入
配件销售收入
人工费收入
其他收入
配件成本
报销成本
利润
```

时间口径：

```text
客户支付净收入：paid_at / refunded_at
工单结算收入与配件成本：settled_at
官方结算收入：official_settlement_time
报销成本：confirmed_at
```

---

## 14.13 Export API

```text
GET /api/export/work-orders
GET /api/export/inventory-stocks
GET /api/export/inventory-flows
GET /api/export/finance
GET /api/export/profit
GET /api/export/reimbursements
```

说明：

```text
MVP 同步导出即可。
导出结果必须与查询条件一致。
导出必须受权限控制。
```

---

## 15. 请求 DTO 风格

### 15.1 命名规则

```text
CreateWorkOrderRequest
UpdateDraftWorkOrderRequest
SubmitWorkOrderRequest
CancelWorkOrderRequest
SettleWorkOrderRequest
AdjustWorkOrderChargeItemsRequest
CreatePaymentRequest
CreateRefundRequest
ConfirmReimbursementRequest
```

查询请求：

```text
WorkOrderQuery
InventoryStockQuery
PaymentQuery
FinanceRangeQuery
```

响应对象：

```text
WorkOrderDetailResponse
WorkOrderListResponse
InventoryStockResponse
FinanceSummaryResponse
```

---

### 15.2 Command 和 Query 分离

业务动作使用 Command/Request：

```text
SubmitWorkOrderRequest
SettleWorkOrderRequest
```

查询使用 Query：

```text
WorkOrderQuery
FinanceRangeQuery
```

不要用一个大 DTO 同时承担创建、编辑、提交、结算。

---

## 16. 核心业务动作 API 详细约束

## 16.1 提交工单

接口：

```text
POST /api/work-orders/{id}/submit
```

前置条件：

```text
工单状态 = DRAFT
工单至少有必要维修信息
至少有一个 charge item
所有 charge_type = PART 且 inventory_affecting = 1 的明细可用库存充足
```

事务内动作：

```text
1. 校验工单状态。
2. 遍历 work_order_charge_item。
3. 只处理 charge_type = PART 且 inventory_affecting = 1 的明细。
4. 校验库存。
5. inventory_stock.available_qty 减少。
6. inventory_stock.reserved_qty 增加。
7. 写 inventory_flow，flow_type = RESERVE。
8. work_order.status = PENDING_ACCEPT。
9. 写 work_order_status_log。
10. 写 submitted_by / submitted_at。
```

失败时：

```text
整体回滚
不能出现工单已提交但库存未预占
不能出现库存已预占但工单仍是 DRAFT
```

---

## 16.2 取消工单

接口：

```text
POST /api/work-orders/{id}/cancel
```

前置条件：

```text
工单未结算
工单未取消
工单已提交或处于可取消状态
```

事务内动作：

```text
1. 校验工单状态。
2. 如果已预占库存，只释放 charge_type = PART 且 inventory_affecting = 1 的明细对应预占。
3. inventory_stock.reserved_qty 减少。
4. inventory_stock.available_qty 增加。
5. 写 inventory_flow，flow_type = RELEASE。
6. work_order.status = CANCELLED。
7. 写 work_order_status_log。
8. 写 cancelled_by / cancelled_at / cancel_reason。
```

说明：

```text
如果已有付款，需要另行记录退款。
取消接口不自动生成退款，除非后续明确设计。
```

---

## 16.3 结算工单

接口：

```text
POST /api/work-orders/{id}/settle
```

前置条件：

```text
工单未取消
工单未结算
工单已提交
实收金额 >= 应收金额
存在预占库存
```

事务内动作：

```text
1. 计算应收金额 = Σ work_order_charge_item.line_amount。
2. 计算实收金额。
3. 校验实收金额 >= 应收金额。
4. 只对 charge_type = PART 且 inventory_affecting = 1 的明细扣减库存。
5. inventory_stock.reserved_qty 减少。
6. inventory_stock.actual_qty 减少。
7. 写 inventory_flow，flow_type = CONSUME。
8. work_order.status = SETTLED。
9. 写 work_order_status_log。
10. 写 settled_by / settled_at。
```

失败时：

```text
整体回滚
不能出现工单已结算但库存未扣
不能出现库存已扣但工单未结算
```

---

## 16.4 调整已提交工单费用项目

接口：

```text
POST /api/work-orders/{id}/adjust-charge-items
```

适用范围：

```text
已提交
未结算
未取消
```

不适用：

```text
DRAFT 工单：直接走普通编辑
SETTLED 工单：MVP 不允许调整
CANCELLED 工单：不允许调整
```

事务内动作：

```text
1. 校验工单状态。
2. 读取旧工单费用明细。
3. 对比新明细，计算差异。
4. 对 charge_type = PART 且 inventory_affecting = 1 的明细：
   a. 删除或减少：释放对应预占库存，写 RELEASE 流水。
   b. 新增或增加：校验可用库存，预占差量库存，写 RESERVE 流水。
5. 对 charge_type = LABOR / OTHER 的明细：
   a. 只更新明细内容和金额。
   b. 不写库存流水。
6. 更新 work_order_charge_item。
7. 重新计算 receivable_amount = Σ work_order_charge_item.line_amount。
8. 记录调整人、调整时间、调整原因。
```

失败时：

```text
整体回滚
不能出现工单费用明细已变但库存预占未变
不能出现库存预占已变但工单费用明细未变
```

---

## 16.5 记录支付

接口：

```text
POST /api/payments
```

前置条件：

```text
工单存在
工单未取消，或允许为历史工单补录支付需管理员权限
金额 > 0
支付方式有效
```

动作：

```text
写 payment_record
不自动修改工单状态
```

---

## 16.6 记录退款

接口：

```text
POST /api/refunds
```

前置条件：

```text
工单存在
退款金额 > 0
退款金额 <= 支付总额 - 已退款总额
退款方式有效
必须填写退款原因
```

动作：

```text
写 refund_record
不删除 payment_record
不自动回滚库存
不自动回退工单状态
```

---

## 16.7 官方结算

接口：

```text
POST /api/work-orders/{id}/official-after-sales/settle
```

前置条件：

```text
工单存在
有 OFFICIAL_SETTLEMENT_MANAGE 权限
官方结算金额 >= 0
```

动作：

```text
更新 official_after_sales
official_settlement_status = SETTLED
official_settlement_amount = 输入金额
official_settlement_time = 当前时间或输入时间
official_settlement_operator_id = 当前用户
```

不影响：

```text
payment_record
refund_record
work_order 实收金额
inventory_stock
```

---

## 16.8 报销确认

接口：

```text
POST /api/reimbursements/{id}/confirm
```

前置条件：

```text
报销状态 = PENDING
有 REIMBURSEMENT_CONFIRM 权限
确认金额 > 0
```

动作：

```text
status = CONFIRMED
confirmed_amount = 输入金额
confirmed_by = 当前用户
confirmed_at = 当前时间
```

说明：

```text
CONFIRMED 报销进入财务成本。
```

---

## 17. 事务边界

### 17.1 必须使用事务的应用服务

```text
InventoryInboundService
InventoryAdjustService
SubmitWorkOrderService
CancelWorkOrderService
SettleWorkOrderService
AdjustWorkOrderChargeItemsService
RecordRefundService
ConfirmReimbursementService
OfficialSettlementService
```

### 17.2 事务规则

事务必须保证：

```text
库存快照和库存流水一致
工单状态和状态日志一致
退款金额校验和退款记录一致
官方结算状态和金额一致
报销状态和确认金额一致
```

### 17.3 不允许拆开的操作

以下操作不允许拆成多个无事务步骤：

```text
提交工单 + PART 类型明细库存预占
取消工单 + PART 类型明细库存释放
结算工单 + PART 类型明细库存扣减
已提交工单调整费用项目 + PART 类型明细库存预占差异调整
入库 + 库存流水
库存调整 + 库存流水
```

---

## 18. 幂等与重复提交

### 18.1 必须防重复的动作

```text
提交工单
取消工单
结算工单
调整已提交工单费用项目
入库
库存调整
退款
报销确认
官方结算
```

### 18.2 MVP 防重复策略

MVP 至少使用：

```text
状态前置校验
数据库事务
唯一编号
必要的行锁或乐观锁
```

### 18.3 建议增加版本字段

对高并发风险较高的表可考虑：

```text
version INT DEFAULT 0
```

候选表：

```text
inventory_stock
work_order
```

MVP 可以先用数据库事务和行锁控制，是否加 version 由 Codex 结合实现复杂度决定。

---

## 19. 并发与库存锁定策略

### 19.1 库存更新必须防并发

库存预占、释放、扣减、调整必须避免并发覆盖。

建议 Codex 使用：

```text
SELECT ... FOR UPDATE
```

锁定对应 `inventory_stock` 行，或者使用带条件的原子更新。

例如预占时必须确保：

```text
available_qty >= reserve_qty
```

### 19.2 禁止负库存

禁止出现：

```text
actual_qty < 0
available_qty < 0
reserved_qty < 0
```

### 19.3 库存流水顺序

在同一事务中：

```text
先锁库存
计算 before 值
更新库存快照
写库存流水 after 值
提交事务
```

---

## 20. 编号生成架构

业务编号包括：

```text
工单编号
支付编号
退款编号
报销编号
第三方配件编码
```

MVP 使用：

```text
sequence_daily
```

架构要求：

```text
1. 编号由后端生成。
2. 前端不得生成最终编号。
3. 编号生成必须持久化。
4. 禁止使用应用内存计数。
5. 禁止使用 MAX + 1。
6. 不引入 Redis。
```

建议服务：

```text
SequenceService.next(seqType)
```

示例：

```text
WO202605100001
PAY202605100001
REF202605100001
REIM202605100001
TP202605100001
```

---

## 21. 财务报表 API 口径

### 21.1 时间口径必须分开

财务报表中以下时间口径不强制相等：

```text
客户支付净收入：paid_at / refunded_at
工单结算收入：settled_at
官方结算收入：official_settlement_time
报销成本：confirmed_at
```

### 21.2 API 返回建议

Finance 返回应明确字段：

```text
paymentNetIncome
officialSettlementIncome
partSalesIncome       // charge_type = PART 的 line_amount 汇总
laborFeeIncome        // charge_type = LABOR 的 line_amount 汇总
otherFeeIncome        // charge_type = OTHER 的 line_amount 汇总
partCost              // charge_type = PART 的 line_cost_amount 汇总
reimbursementCost
profit
```

还应考虑展示：

```text
receivableIncome
receivedIncome
```

避免老板把“应收收入”和“实收收入”混淆。

### 21.3 已结算后退款

规则：

```text
退款影响 paymentNetIncome
不自动回退 work_order.status
不自动回滚 inventory_stock
```

---

## 22. Excel 导出架构

### 22.1 MVP 同步导出

第一版采用同步导出。

不建：

```text
export_task
export_record
```

### 22.2 导出权限

每个导出接口必须校验：

```text
EXCEL_EXPORT
```

部分财务导出还需要：

```text
FINANCE_VIEW
```

### 22.3 查询条件一致

导出接口应复用列表/报表查询条件。

禁止：

```text
页面查询 A 条件，导出 B 条件
```

---

## 23. 小程序架构边界

### 23.1 技术选型

MVP 推荐：

```text
微信原生小程序
Vant Weapp 或 WeUI
```

### 23.2 小程序职责

小程序负责：

```text
登录
工作台
创建 DRAFT 工单
编辑 DRAFT 工单
提交工单
查询库存
扫码入库
记录支付
记录退款
提交报销
查看工单状态
```

### 23.3 小程序不得负责

小程序不得负责：

```text
最终库存计算
最终结算判断
最终权限判断
财务利润计算
官方结算统计
直接修改工单状态
直接修改库存数量
```

### 23.4 小程序 API 调用要求

小程序必须调用后端业务动作接口：

```text
submit
cancel
settle
adjust-charge-items
payments
refunds
```

不得通过普通 update 绕过状态动作。

---

## 24. 管理端架构边界

### 24.1 技术选型

```text
Vue3
Element Plus
```

### 24.2 管理端职责

管理端负责：

```text
用户权限
字典配置
配件管理
库存管理
工单管理
支付退款查看
官方结算录入
报销确认
财务报表
Excel 导出
```

### 24.3 管理端不得绕过后端

即使是管理端，也不能：

```text
直接修改库存数量
直接修改工单状态为已结算
直接把官方结算写入客户支付
直接把待确认报销计入成本
```

---

## 25. 日志与审计

### 25.1 应用日志

后端应记录：

```text
登录失败
权限拒绝
核心业务动作失败
库存不足
重复提交
退款金额超限
系统异常
```

### 25.2 业务日志

业务日志以业务表为主：

```text
inventory_flow
work_order_status_log
payment_record
refund_record
reimbursement
official_after_sales
```

应用日志不能替代业务流水。

---

## 26. 安全要求

MVP 基础要求：

```text
1. 所有业务接口必须登录后访问。
2. 所有关键操作必须校验权限。
3. 后端不得信任前端传入的 user_id / store_id。
4. 防止越权访问其他门店数据。
5. 错误信息不能泄露数据库结构。
6. 导出接口必须校验权限。
7. 密码如果启用，必须哈希存储。
```

---

## 27. 文件与图片

MVP 不做图片上传。

因此暂不设计：

```text
文件服务
对象存储
附件表
图片凭证
```

后续如果加入凭证上传，再单独设计：

```text
attachment
voucher
file_storage
```

---

## 28. 测试架构建议

### 28.1 Codex 必须覆盖的测试

```text
库存入库
提交工单预占库存（仅 PART 类型明细）
取消工单释放库存（仅 PART 类型明细）
结算工单扣减库存（仅 PART 类型明细）
已提交工单调整费用项目同步库存差异（仅 PART 类型）
添加 LABOR 明细不影响库存
添加 OTHER 明细不影响库存
多次付款
混合付款
退款校验
实收不足不能结算
官方结算不进入客户支付
报销确认后计入成本
charge_type 财务分类正确
```

### 28.2 Claude 可补充的测试

```text
Controller 参数校验
列表查询
分页
导出接口
前端页面表单校验
简单接口集成测试
文档同步
```

### 28.3 Owner 手工验收主链路

```text
配件入库 10 个
创建 DRAFT 工单使用 2 个
提交工单
可用库存变 8，预占库存变 2，实际库存仍 10
客户付款达到应收
结算工单
实际库存变 8，预占库存变 0，可用库存仍 8
查看库存流水
查看财务报表
导出 Excel
```

---

## 29. Codex 实现护栏

Codex 后续实现必须遵守：

```text
1. 不引入微服务。
2. 不引入 Redis / MQ。
3. 不创建 MVP 暂不需要的表。
4. 不把库存写成 part.stock。
5. 不绕过 inventory_flow。
6. 不把支付和退款塞进 work_order 字段。
7. 不把官方结算写入 payment_record。
8. 不用 PUT 普通更新接口改工单核心状态。
9. 不让前端决定结算是否成功。
10. 不让小程序直接修改库存数量。
11. 不使用 MAX + 1 或内存计数生成业务编号。
12. 不使用 double / float 表示金额。
13. 不得把 LABOR / OTHER 明细纳入库存预占/扣减。
14. 不得再使用 labor_fee / other_fee 作为工单应收金额的主要来源。
```

---

## 30. Claude 实现护栏

Claude 可以处理：

```text
页面
表单
列表
字段展示
前端校验
简单接口对接
测试
文档
```

Claude 不得处理：

```text
库存预占逻辑
库存扣减逻辑
工单状态机
支付退款结算判断
官方结算统计
财务利润口径
数据库核心结构
事务边界
权限核心逻辑
```

如 Claude 发现问题，应输出：

```text
发现的问题
影响范围
建议交给 Codex 的原因
建议测试场景
```

不得直接重构核心逻辑。

```text
不得在页面中绕过 charge_type，不能把所有维修项目默认当 PART。
```

---

## 31. 后续接口文档生成方式

本文档确认后，不建议一次性生成全部详细 API 文档。

推荐方式：

```text
按实施路线逐模块生成 API 详情：
1. 登录与权限
2. 配件与库存
3. 工单
4. 支付退款
5. 官方售后
6. 报销
7. 财务与导出
```

每个模块任务卡中要求 Codex 输出：

```text
接口路径
请求 DTO
响应 DTO
权限点
业务规则
失败场景
测试用例
```

---

## 32. 当前文档状态

本文档状态：

```text
Reviewed v0.2
已将 work_order item API 更新为 charge-items
已明确 charge_type = PART / LABOR / OTHER 对库存和财务的影响
已更新 Codex / Claude 护栏
可进入 Step 7：07_IMPLEMENTATION_ROADMAP.md
```

复核重点：

```text
1. 架构是否保持简单不过度？
2. 模块边界是否清晰？
3. API 是否避免了普通 CRUD 绕过业务规则？
4. 工单提交、取消、结算、调整费用项目是否都有业务动作接口？
5. 事务边界是否覆盖库存、工单、支付、退款、报销、官方结算？
6. 小程序和管理端职责是否清楚？
7. Codex / Claude 护栏是否足够明确？
8. charge_type = PART / LABOR / OTHER 对应的 API 逻辑是否正确？
```

本文档确认后，下一步进入：

```text
07_IMPLEMENTATION_ROADMAP.md
```
