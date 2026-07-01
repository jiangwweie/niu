# Admin / Staff / Client API 边界说明

本文档明确“小牛电动官方授权店两轮车维修售后库存管理系统”中管理端（Admin）、微信小程序员工端（Staff）和规划中的客户侧（Client）API 边界、Service 共享原则和各自职责定位。

---

## 1. 分层架构总览

```text
┌──────────────────────────────────────────────────────┐
│                   Controller / API 层                  │
│ /api/admin/**   /api/staff/**   /api/client/**         │
│       ↓              ↓              ↓                  │
│            ApplicationService / Service 层              │
│       （业务能力层，Admin / Staff / Client 按边界复用）   │
│                        ↓                               │
│                  Domain / Entity / Mapper              │
└──────────────────────────────────────────────────────┘
```

### 核心原则

| 原则 | 说明 |
|------|------|
| Service 共用 | `ApplicationService` / `Service` 是业务能力层，Admin、Staff 和 Client 在同一业务规则下按需复用，不按端重复编码 |
| Controller 按端拆分 | 每个端有独立的 Controller 层，各自定义路由、DTO、权限校验和返回结构 |
| DTO 按端定义 | Staff / Client 端使用独立轻量 DTO，不直接复用 Admin 的复杂列表 DTO |
| 业务规则统一 | 底层状态流转、库存变动、交付关闭、支付退款规则由 Service 层统一控制，任何端都不能绕过 |

---

## 2. API 路由前缀

| 端 | 前缀 | 说明 |
|---|---|---|
| 管理端 | `/api/admin/**` | 网页管理端，面向平台超管、店长、门店管理员 |
| 员工端 | `/api/staff/**` | 微信小程序员工端，面向维修前台、库存员、收银员等门店员工 |
| 客户端 | `/api/client/**` | 规划中的客户侧小程序，面向到店预约、排号、进度查询 |

当前系统已实现 `/api/admin/**` 和 `/api/staff/**`。`/api/client/**` 为客户侧预约排号系统规划边界。

---

## 3. 管理端定位（/api/admin/**）

管理端承担全功能管理职责，主要面向平台超管、店长和门店管理员：

| 职责 | 示例 |
|------|------|
| 查询与浏览 | 工单列表、配件列表、库存列表、支付记录、退款记录、官方售后列表 |
| 审核与确认 | 官方售后结算确认、无需结算标记、报销确认 |
| 统计与导出 | 财务汇总、工单统计、Excel 导出 |
| 配置管理 | 配件创建/修改/启停、字典管理、门店配置 |
| 异常处理 | 退款、库存调整、工单取消 |
| 代操作 | 必要时代维修工执行 submit / cancel / mark-repair-done / deliver / payment / refund |

管理端可以暴露工单 action API，但前端页面是否展示对应按钮由角色权限和业务场景决定，不是接口层面的限制。

---

## 4. 员工端定位（/api/staff/**）

微信小程序员工端面向门店现场使用，聚焦高频操作：

| 职责 | 示例 |
|------|------|
| 现场录入 | 创建工单草稿、填写费用明细、选择配件 |
| 库存查询 | 查看可用库存、搜索配件 |
| 工单操作 | 提交工单、标记维修完成、交付关闭、记录客户付款 |
| 报销 | 提交报销申请 |
| 资方业务 | 资方申请、台账、收款登记 |
| 个人工单 | 查看自己相关的工单列表和详情 |

### 员工端设计原则

- 使用独立轻量 DTO，不复用 Admin 的复杂列表 DTO。
- 只暴露门店员工需要的字段，不暴露财务汇总、全店统计等管理接口。
- 所有写操作仍必须调用后端业务 Service，不得直接修改状态或库存。

---

## 5. 客户端定位（/api/client/**）

客户侧小程序用于预约排号和轻量进度查看，不承接员工权限和后台管理职责：

| 职责 | 示例 |
|------|------|
| 客户身份 | 微信 openid 建立客户侧身份，不进入 `sys_user` |
| 预约 | 选择门店、服务类型、日期、时间段、填写车辆和问题描述 |
| 签到排号 | 到店签到、生成排队号、查看当前状态 |
| 预约管理 | 查看我的预约、取消预约、查看服务进度 |
| 工单关联查看 | 预约转工单后，只查看脱敏摘要和进度 |

### 客户端设计原则

- 客户账号使用独立的 `client_user`，不能复用员工 `sys_user`。
- 客户侧只能创建预约单，不能直接创建正式工单并提交。
- 客户侧不能直接触发库存预占、库存扣减、支付、退款、交付关闭、官方结算和财务入账。
- 预约转工单必须由员工端或管理端确认后执行。
- 客户侧接口只返回客户本人数据，必须以 `client_user_id` 和 `store_id` 双重约束查询范围。

---

## 6. 不能绕过的后端业务规则

无论 Admin、Staff 还是 Client，所有 action 最终调用同一个 Service 方法，底层规则由后端统一控制：

| 操作 | Service 层行为 | 说明 |
|------|----------------|------|
| appointment | 创建预约单，不影响库存/支付/财务 | 客户侧只进入预约队列 |
| convert appointment | 预约转工单 DRAFT | 只能由员工/管理端确认后执行 |
| submit | 工单状态 `DRAFT` → `REPAIRING`，预占库存 | 只有 submit 才触发库存预占 |
| cancel | 工单状态 → `CANCELLED`，释放预占库存 | 只有 cancel 才释放预占 |
| mark repair done | 工单状态 `REPAIRING` → `REPAIR_DONE`，正式扣减库存 | 库存 CONSUME 在维修完成时发生 |
| deliver | 工单状态 `REPAIR_DONE` → `DELIVERED` | 交付关闭不重复扣库存 |
| payment | 累计 `received_amount`，不自动触发交付关闭 | 多次付款累加，不自动改变工单状态 |
| refund | 累计退款金额，不反结算 | 退款不回滚已交付状态，不回滚库存 |
| official settlement | 记录官方结算金额，不影响 `received_amount` | 官方结算与客户支付完全分开统计 |

前端（无论管理端、员工端还是客户端）都不能绕过后端这些规则直接修改库存或工单状态。

---

## 7. 客户侧预约排号建议

客户侧建议作为独立 Maven 模块规划，但仍运行在同一单体应用内，避免过早引入微服务：

- Maven 模块名建议：`client-appointment`
- Java 包建议：`com.xiaoniu.aftermarket.client`、`com.xiaoniu.aftermarket.appointment`
- API 前缀：`/api/client/**`
- 表建议：`client_user`、`service_appointment`、`appointment_status_log`、`appointment_capacity_rule`
- 与工单关系：预约单只保存 `converted_work_order_id`，工单仍由 `workorder` 模块负责
- 与库存关系：预约模块不调用库存预占、释放、扣减能力

---

## 8. 前端注意事项

- 管理端（Web）应使用 `/api/admin/**` 前缀调用接口。
- 员工小程序应使用 `/api/staff/**` 前缀。
- 客户小程序应使用 `/api/client/**` 前缀。
- 前端不得将 Admin API 直接当作小程序 API 使用。
- 员工小程序不应引用 Admin 复杂列表 DTO。
- 客户小程序不得引用 Staff DTO 或员工权限模型。
- 无论哪一端，操作结果以 `ApiResponse`（`code/message/data/traceId`）为准。
