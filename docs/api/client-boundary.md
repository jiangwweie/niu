# Admin / Mobile API 边界说明

本文档明确"小牛电动官方授权店两轮车维修售后库存管理系统"中管理端（Admin）和微信小程序员工端（Mobile）的 API 边界、Service 共享原则和各自职责定位。

---

## 1. 分层架构总览

```text
┌──────────────────────────────────────────────────────┐
│                   Controller / API 层                  │
│   /api/admin/**        /api/mobile/** (或 /api/staff/**)│
│       ↓                        ↓                       │
│            ApplicationService / Service 层              │
│         （业务能力层，Admin 和 Mobile 共用）              │
│                        ↓                               │
│                  Domain / Entity / Mapper              │
└──────────────────────────────────────────────────────┘
```

### 核心原则

| 原则 | 说明 |
|------|------|
| Service 共用 | `ApplicationService` / `Service` 是业务能力层，Admin 和 Mobile 共用同一套实现，不按端重复编码 |
| Controller 按端拆分 | 每个端有独立的 Controller 层，各自定义路由、DTO、权限校验和返回结构 |
| DTO 按端定义 | Mobile 端使用独立轻量 DTO，不直接复用 Admin 的复杂列表 DTO |
| 业务规则统一 | 底层状态流转、库存变动、结算规则由 Service 层统一控制，任何端都不能绕过 |

---

## 2. API 路由前缀

| 端 | 前缀 | 说明 |
|---|---|---|
| 管理端 | `/api/admin/**` | 网页管理端，面向店长/管理员 |
| 微信端 | `/api/mobile/**` 或 `/api/staff/**` | 微信小程序员工端，面向维修工 |

当前阶段（Task 14B）仅实现 `/api/admin/**`，`/api/mobile/**` 为规划接口，尚未实现。

---

## 3. 管理端定位（/api/admin/**）

管理端承担全功能管理职责，主要面向店长和管理员：

| 职责 | 示例 |
|------|------|
| 查询与浏览 | 工单列表、配件列表、库存列表、支付记录、退款记录、官方售后列表 |
| 审核与确认 | 官方售后结算确认、无需结算标记 |
| 统计与导出 | 财务汇总、工单统计、Excel 导出 |
| 配置管理 | 配件创建/修改/启停、字典管理、门店配置 |
| 异常处理 | 退款、库存调整、工单取消 |
| 代操作 | 必要时代维修工执行 submit / cancel / settle / payment / refund |

管理端可以暴露工单 action API（submit、cancel、settle、payment、refund 等），但前端页面是否展示对应按钮由角色权限和业务场景决定，不是接口层面的限制。

---

## 4. 微信端定位（/api/mobile/** 或 /api/staff/**）

微信小程序面向维修工现场使用，聚焦高频操作：

| 职责 | 示例 |
|------|------|
| 现场录入 | 创建工单草稿、填写费用明细、选择配件 |
| 库存查询 | 查看可用库存、搜索配件 |
| 工单操作 | 提交工单、记录客户付款 |
| 报销 | 提交报销申请 |
| 个人工单 | 查看自己相关的工单列表和详情 |

### 微信端设计原则

- 使用独立轻量 DTO，不复用 Admin 的复杂列表 DTO
- 只暴露维修工需要的字段，不暴露财务汇总、全店统计等管理接口
- 后续单独设计，当前阶段不实现

---

## 5. 已有 Admin API（规划与已实现）

以下接口在 `/api/admin/**` 下规划或已实现，供管理端网页调用：

| 模块 | 接口 | 说明 |
|------|------|------|
| Dict | `GET /api/admin/dicts` | 字典项查询 |
| Part | `GET /api/admin/parts` | 配件分页查询 |
| Part | `POST /api/admin/parts` | 创建配件 |
| Part | `PUT /api/admin/parts/{id}` | 修改配件 |
| Part | `PUT /api/admin/parts/{id}/toggle` | 启停配件 |
| Inventory | `GET /api/admin/inventories` | 库存查询 |
| Inventory | `POST /api/admin/inventories/stock-in` | 入库 |
| Inventory | `POST /api/admin/inventories/adjust` | 库存调整 |
| Inventory | `GET /api/admin/inventory-flows` | 库存流水查询 |
| WorkOrder | `GET /api/admin/work-orders` | 工单列表查询 |
| WorkOrder | `GET /api/admin/work-orders/{id}` | 工单详情 |
| WorkOrder | `POST /api/admin/work-orders` | 创建工单草稿 |
| WorkOrder | `PUT /api/admin/work-orders/{id}` | 更新工单草稿 |
| WorkOrder | `PUT /api/admin/work-orders/{id}/submit` | 提交工单 |
| WorkOrder | `PUT /api/admin/work-orders/{id}/cancel` | 取消工单 |
| WorkOrder | `PUT /api/admin/work-orders/{id}/settle` | 结算工单 |
| WorkOrder | `PUT /api/admin/work-orders/{id}/payment` | 记录付款 |
| WorkOrder | `PUT /api/admin/work-orders/{id}/refund` | 退款 |
| Payment | `GET /api/admin/payments` | 支付记录全局查询 |
| Refund | `GET /api/admin/refunds` | 退款记录全局查询 |
| OfficialAfterSales | `GET /api/admin/official-after-sales` | 官方售后查询 |
| OfficialAfterSales | `PUT /api/admin/official-after-sales/{id}/settle` | 官方结算确认 |
| OfficialAfterSales | `PUT /api/admin/official-after-sales/{id}/skip` | 无需结算标记 |

---

## 6. 不能绕过的后端业务规则

无论 Admin 还是 Mobile，所有 action 最终调用同一个 Service 方法，底层规则由后端统一控制：

| 操作 | Service 层行为 | 说明 |
|------|----------------|------|
| submit | 工单状态 → RESERVE，预占库存 | 只有 submit 才触发库存预占 |
| cancel | 工单状态 → CANCELLED，释放预占库存 | 只有 cancel 才释放预占 |
| settle | 工单状态 → SETTLED，实际扣减库存 | 结算后才真正消耗库存 |
| payment | 累计 received_amount，不自动触发结算 | 多次付款累加，不自动改变工单状态 |
| refund | 累计退款金额，不反结算 | 退款不回滚已结算状态，不回滚库存 |
| official settlement | 记录官方结算金额，不影响 received_amount | 官方结算与客户支付完全分开统计 |

前端（无论管理端还是小程序）都不能绕过后端这些规则直接修改库存或工单状态。

---

## 7. Task 14B 说明

当前 Task 14B 继续实现 Admin Controller，这是管理端接口的实现进展，不等于微信小程序接口已完成。

Mobile 端接口将在后续独立任务中设计和实现，届时会：

- 创建 `MobileWorkOrderController` 等独立 Controller
- 定义轻量级 Mobile DTO，按维修工实际需要裁剪字段
- 在 `/api/mobile/**` 或 `/api/staff/**` 路径下注册

---

## 8. 前端注意事项

- 管理端（Web）应使用 `/api/admin/**` 前缀调用接口
- 微信小程序后续应使用 `/api/mobile/**` 或 `/api/staff/**` 前缀
- 前端不得将 Admin API 直接当作小程序 API 使用
- 微信小程序不应引用 Admin 复杂列表 DTO（如全量支付记录、财务汇总字段等）
- 无论哪一端，操作结果以 `ApiResponse`（`code/message/data/traceId`）为准
