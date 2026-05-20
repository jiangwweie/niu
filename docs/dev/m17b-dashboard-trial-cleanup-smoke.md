# M17B Dashboard 真实统计 + 正式启用数据清理 Smoke 说明

## 能力范围

M17B 提供两项正式交付前能力：

1. **Dashboard 真实统计**：admin-web 首页接入真实业务数据，展示今日工单、待结算、低库存、本月财务概览、最近工单、待处理事项。
2. **正式启用数据清理**：SUPER_ADMIN 可清理试运行期间的业务数据（工单、支付、退款、报销、官方结算、库存流水、客户、车辆），保留基础配置数据，使系统可以正式启用。

## 一、Dashboard 统计

### API

`GET /api/admin/dashboard/summary`

### 权限

- 要求 `FINANCE_VIEW` 权限。
- 原因：Dashboard 包含财务金额统计，应与财务报表保持一致的权限控制。
- `SUPER_ADMIN`、`STORE_ADMIN`、`FINANCE` 角色默认具备 `FINANCE_VIEW`。
- `TECHNICIAN_FRONT_DESK` 角色不具备，无法访问 Dashboard 统计数据。

### 统计字段与口径

| 字段 | 说明 | 来源 |
|------|------|------|
| `todayWorkOrderCount` | 今日创建工单数 | `work_order`，按 `DATE(created_at) = 今日` 统计，`store_id` 隔离 |
| `pendingSettleWorkOrderCount` | 待结算工单数 | `work_order`，status 属于 PENDING_ACCEPT/ACCEPTED/PART_ORDERED/PART_ARRIVED，`store_id` 隔离 |
| `pendingReimbursementCount` | 待确认报销数 | `reimbursement`，status = PENDING，`store_id` 隔离 |
| `lowStockPartCount` | 低库存配件数 | `inventory_stock` JOIN `part`，`availableQty <= 3` 且 part ENABLED，`store_id` 隔离 |
| `monthCustomerIncome` | 本月客户支付净收入 | 复用 `FinanceService.queryMonthly`，支付总额 - 退款总额 |
| `monthOfficialIncome` | 本月官方结算收入 | 复用 `FinanceService.queryMonthly`，仅统计 SETTLED |
| `monthPartsCost` | 本月配件成本 | 复用 `FinanceService.queryMonthly`，已结算工单配件成本 |
| `monthReimbursementCost` | 本月报销成本 | 复用 `FinanceService.queryMonthly`，仅统计 CONFIRMED |
| `monthProfit` | 本月利润 | `customerIncome + officialIncome - partsCost - reimbursementCost` |
| `recentWorkOrders` | 最近 5 条工单 | 工单号、客户名、状态、应收、实收、创建时间，`store_id` 隔离 |
| `pendingActions` | 待处理摘要 | 待结算工单数、待确认报销数、待处理官方结算数 |

### 金额口径

Dashboard 的月度财务字段（`monthCustomerIncome`、`monthOfficialIncome`、`monthPartsCost`、`monthReimbursementCost`、`monthProfit`）**完全复用 `FinanceService.queryMonthly`**，与财务报表口径一致，不使用独立计算逻辑。

### 数据隔离

所有查询按 `store_id` 过滤，当前用户只能看到自己门店的数据。

## 二、正式启用数据清理

### API

- `GET /api/admin/trial-data/summary`：查看可清理数据数量（只读）
- `POST /api/admin/trial-data/clear`：执行清理（高风险操作）

### 权限

- 两个接口均要求 `SUPER_ADMIN` 角色（`hasRole('SUPER_ADMIN')`）。
- `STORE_ADMIN`、`FINANCE`、`TECHNICIAN_FRONT_DESK` 均无法访问。

### 确认机制

`POST /api/admin/trial-data/clear` 要求请求体中 `confirmText` 字段值为 `CONFIRM_CLEAR_TRIAL_DATA`，否则返回错误。

### 清理范围

| 表 | 操作 | 说明 |
|----|------|------|
| `work_order_status_log` | 删除 | 工单状态日志 |
| `work_order_charge_item` | 删除 | 工单收费项目 |
| `payment_record` | 删除 | 支付记录 |
| `refund_record` | 删除 | 退款记录 |
| `official_after_sales` | 删除 | 官方售后记录 |
| `reimbursement` | 删除 | 报销记录 |
| `work_order` | 删除 | 工单主表 |
| `inventory_flow` | 删除 | 库存流水 |
| `inventory_stock` | 归零 | `actual_qty=0, available_qty=0, reserved_qty=0`，记录保留 |
| `vehicle` | 删除 | 试运行车辆资料 |
| `customer` | 删除 | 试运行客户资料 |

### 保留范围

| 表 | 说明 |
|----|------|
| `sys_user` | 用户账号 |
| `sys_role` | 角色定义 |
| `sys_permission` | 权限定义 |
| `sys_user_role` | 用户角色关系 |
| `sys_role_permission` | 角色权限关系 |
| `store` | 门店配置 |
| `part` | 配件基础资料（数量归零但配件信息保留） |
| `part_barcode` | 配件条码 |
| `sys_dict_type` / `sys_dict_item` | 字典配置 |
| `sequence_daily` | 编号序列 |

### 事务安全

`clearTrialData` 方法使用 `@Transactional` 注解，所有删除操作在同一个事务中执行。如果中途失败，数据库会回滚到清理前状态，不会产生半清理。

### 操作前必读

1. **强烈建议先备份数据库**。清理后不可恢复。
2. 此为正式启用前的**一次性操作**。
3. 仅 `SUPER_ADMIN` 可执行。
4. `dev`/`test` 环境可正常测试；`prod` 环境请谨慎操作。

## 三、admin-web 页面

### Dashboard 首页

- 首次加载自动请求 `/api/admin/dashboard/summary`
- 展示统计卡片（今日工单、待结算工单、待确认报销、低库存配件）
- 展示本月财务概览（客户收入、官方收入、配件成本、报销成本、利润）
- 展示待处理事项（待结算、待确认报销、待处理官方结算）
- 展示最近 5 条工单表格
- 保留快捷入口卡片
- 接口失败显示空状态提示
- 无假数据

### 正式启用页面

- 路径：`/trial-data`
- 仅 `SUPER_ADMIN` 可见（侧边栏 `hasRole('SUPER_ADMIN')` 控制）
- 展示可清理数据摘要
- 红色高风险提示（不可恢复、建议备份）
- 保留内容说明
- 输入 `CONFIRM_CLEAR_TRIAL_DATA` 确认
- 二次弹窗确认
- 清理完成后展示清理结果并刷新摘要

## 四、已知限制

- Dashboard 不包含图表/大屏，只展示数值卡片和简单表格。
- 低库存阈值固定为 `availableQty <= 3`，不支持自定义。
- 本月财务统计以自然月为口径，不支持自定义时间范围。
- 正式启用清理不支持选择性清理（只能全部清理）。
- 清理操作不支持回滚（需数据库备份恢复）。
- `work_order_status_log` 的删除按 `store_id` 执行，依赖当前单门店 MVP 的门店隔离字段。
