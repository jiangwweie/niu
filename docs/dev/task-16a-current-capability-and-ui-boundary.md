# Task 16A：当前项目能力、端职责边界与后续路线确认

> 检查日期：2026-05-13
> 分支：feature/backend-skeleton
> Commit：89f4630

---

## 一、后端能力盘点

### 1.1 总览

| 模块 | Controller | 已有 endpoint 数 | 测试覆盖 | dev-only Header | 核心业务规则 | admin-web 可用 | 适合 staff/mobile |
|------|-----------|-----------------|---------|----------------|------------|---------------|-------------------|
| Health | HealthController | 1 | 无 | 否 | 否 | 否（监控用） | 否 |
| Dict | DictController | 2 | 有（Controller+Service） | 是（但不 requireUser） | 否 | 是（已接入） | 是 |
| Part | PartController | 7 | 有（Controller+Service） | 是 | 部分（启停状态） | 是（已接入） | 是（只读查询） |
| Inventory | InventoryController | 5 | 有（Controller+Service） | 是 | **是**（库存状态机） | 是（已接入） | 是（查询+入库） |
| WorkOrder | WorkOrderController | 10 | 有（Controller+Service） | 是 | **是**（状态机） | 部分（列表+详情） | **是**（创建/提交） |
| Payment | PaymentController | 4 | 有（Controller+Service） | 是 | **是**（金额计算） | 未接入 | **是**（记录支付） |
| Refund | RefundController | 3 | 有（Controller） | 是 | **是**（退款限额） | 未接入 | 是（记录退款） |
| OfficialAfterSales | OfficialAfterSalesController | 5 | 有（Controller+Service） | 是 | **是**（结算状态机） | 未接入 | 否 |
| Reimbursement | 无 Controller | 0 | 无 | — | 空壳 stub | 否 | 未实现 |
| Finance | 无 Controller | 0 | 无 | — | 空壳 stub | 否 | 未实现 |
| Export | 无 Controller | 0 | 无 | — | 空壳 stub | 否 | 未实现 |
| Auth/User/Permission | 无 Controller | 0 | PermissionQueryService 有测试 | — | 数据层已建，未接入 | 否 | 未实现 |
| Mobile/Staff API | 无 Controller | 0 | — | — | — | — | 未实现 |

**后端 API 合计：37 个 endpoint（7 个 Controller），22 张数据库表，4 个 Flyway 迁移脚本，328 个单元/集成测试。**

### 1.2 各模块详细说明

#### HealthController（1 endpoint）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/health` | 健康检查，返回 `{"status":"UP"}` |

- 不需要任何认证
- 无业务逻辑

#### DictController（2 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/dict/types` | 列出所有启用的字典类型 |
| GET | `/api/admin/dict/types/{typeCode}/items` | 按 typeCode 列出字典项 |

- 只读，无写操作
- 不需要用户上下文（不调用 requireCurrentUser）
- **admin-web 已接入**，字典配置页面使用真实 API
- **staff/mobile 可复用**

#### PartController（7 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/parts` | 分页查询配件（支持 partCode/partName/officialPartNo/model/categoryCode/source/enabled） |
| GET | `/api/admin/parts/{partId}` | 配件详情 |
| POST | `/api/admin/parts/official` | 新增官方配件 |
| POST | `/api/admin/parts/third-party` | 新增第三方配件 |
| PUT | `/api/admin/parts/{partId}` | 更新配件 |
| POST | `/api/admin/parts/{partId}/enable` | 启用配件 |
| POST | `/api/admin/parts/{partId}/disable` | 停用配件 |

- 配件编码自动生成（SequenceService）
- 官方配件必须填写 officialPartNo
- **admin-web 已完整接入**（列表、新增、启停）
- staff/mobile 应只读查询（选配件用）

#### InventoryController（5 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/inventory/stocks` | 分页查询库存快照 |
| GET | `/api/admin/inventory/stocks/{partId}` | 单配件库存详情 |
| POST | `/api/admin/inventory/inbound` | 入库（增加 actualQty 和 availableQty） |
| POST | `/api/admin/inventory/adjust` | 库存调整（quantityDelta 正/负） |
| GET | `/api/admin/inventory/flows` | 分页查询库存流水 |

- **核心逻辑**：三量模型 actual = available + reserved，防负库存，每次变更写 inventory_flow
- adjust quantityDelta=0 → `INVENTORY_ADJUST_ZERO`
- adjust 导致负库存 → `INVENTORY_ADJUST_WOULD_NEGATIVE`
- unitCost 可选，直接覆写 part.reference_cost_price
- **admin-web 已完整接入**
- staff/mobile 应可做查询和普通入库，调整应留给管理员

#### WorkOrderController（10 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/work-orders` | 分页查询工单（8 个筛选条件） |
| GET | `/api/admin/work-orders/{workOrderId}` | 工单详情（含费用项、支付汇总、官方售后信息） |
| POST | `/api/admin/work-orders/drafts` | 创建草稿工单 |
| PUT | `/api/admin/work-orders/{workOrderId}/draft` | 更新草稿工单 |
| POST | `/api/admin/work-orders/{workOrderId}/charge-items` | 添加费用项 |
| PUT | `/api/admin/work-orders/{workOrderId}/charge-items/{chargeItemId}` | 更新费用项 |
| DELETE | `/api/admin/work-orders/{workOrderId}/charge-items/{chargeItemId}` | 删除费用项 |
| POST | `/api/admin/work-orders/{workOrderId}/submit` | 提交工单（DRAFT→PENDING_ACCEPT + 预占库存） |
| POST | `/api/admin/work-orders/{workOrderId}/cancel` | 取消工单（释放预占库存） |
| POST | `/api/admin/work-orders/{workOrderId}/settle` | 结算工单（扣减实际库存，校验 receivedAmount ≥ receivableAmount） |

- **核心逻辑**：工单状态机 DRAFT → PENDING_ACCEPT → ... → SETTLED / CANCELLED
- submit/cancel/settle 是跨聚合事务操作（工单+库存+流水+状态日志）
- 费用项类型：PART（影响库存）、LABOR、OTHER
- **admin-web 仅接入列表和详情查看**
- **创建工单、添加费用项、submit/cancel/settle 应属于 staff/mobile 小程序端**

#### PaymentController（4 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/work-orders/{workOrderId}/payments` | 按工单查询支付记录 |
| POST | `/api/admin/work-orders/{workOrderId}/payments` | 记录支付 |
| GET | `/api/admin/work-orders/{workOrderId}/payment-summary` | 支付汇总（总额/退款/净收） |
| GET | `/api/admin/payments` | 全局支付记录分页查询 |

- **核心逻辑**：receivedAmount = SUM(payments) - SUM(refunds)，支付不自动结算
- 支付方式校验（PaymentMethod 枚举）
- **admin-web 未接入**（列表页为 mock 数据）
- **记录支付应在 staff/mobile 端**（现场收费），管理端只做查看

#### RefundController（3 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/work-orders/{workOrderId}/refunds` | 按工单查询退款记录 |
| POST | `/api/admin/work-orders/{workOrderId}/refunds` | 记录退款 |
| GET | `/api/admin/refunds` | 全局退款记录分页查询 |

- **核心逻辑**：退款金额不得超过已付未退余额，退款后更新 receivedAmount
- **admin-web 未接入**
- 退款应在 staff/mobile 端操作，管理端只做查看

#### OfficialAfterSalesController（5 endpoints）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/official-after-sales` | 全局官方售后分页查询 |
| GET | `/api/admin/work-orders/{workOrderId}/official-after-sales` | 按工单查询 |
| POST | `/api/admin/work-orders/{workOrderId}/official-after-sales/order-info` | 录入官方订单号 |
| POST | `/api/admin/work-orders/{workOrderId}/official-after-sales/settle` | 标记已结算（含结算金额+时间） |
| POST | `/api/admin/work-orders/{workOrderId}/official-after-sales/no-settlement-required` | 标记无需结算 |

- **核心逻辑**：PENDING → SETTLED / NOT_REQUIRED 状态机
- 官方结算独立于客户支付，不影响 received_amount 和 inventory
- **admin-web 未接入**（列表页为 mock 数据）
- **这是 admin-web 管理端核心功能**，不属于小程序端

#### Reimbursement 模块（空壳）

- `ReimbursementService` 接口定义了 submit/confirm/reject/cancel，全部抛出 `UnsupportedOperationException("TODO: implement in Phase 6")`
- 有 `ReimbursementEntity`、`ReimbursementStatus` 枚举、`ConfirmReimbursementCommand`
- 无 Controller，无测试
- 数据库 reimbursement 表已建

#### Finance 模块（空壳）

- `FinanceService` 接口定义了 queryDaily/queryMonthly/queryRange，全部抛出 `UnsupportedOperationException("TODO: implement in Phase 7")`
- 无 Controller，无测试

#### Export 模块（空壳）

- 仅有 `package-info.java` 占位
- 无任何实现

#### Auth/User/Permission 模块

- `auth/` 包仅有 `package-info.java` 占位
- `user/` 包有完整的数据层（Entity、Mapper）：
  - `SysUserEntity`、`SysRoleEntity`、`SysUserRoleEntity`、`SysPermissionEntity`、`SysRolePermissionEntity`
- `PermissionQueryService` 可通过 user→role→permission 链路查询权限码，**有测试**
- 但没有任何 Controller 调用此服务
- `CurrentUser.permissions` 始终为空集（DevCurrentUserInterceptor 硬编码）
- 数据库已有 4 个默认角色（SUPER_ADMIN / STORE_ADMIN / FINANCE / TECHNICIAN_FRONT_DESK）和 19 个权限点

#### Mobile/Staff API

- 后端**无任何** `/api/mobile/**` 或 `/api/staff/**` 路径的 Controller
- 所有 Controller 都在 `/api/admin/**` 下
- 计划在 Phase 9 实现

### 1.3 认证机制现状

**DevCurrentUserInterceptor**（仅用于开发环境）：
- 读取 `X-User-Id` 和 `X-Store-Id` 两个 HTTP Header
- 填充 `ThreadLocal<CurrentUser>`（userId + storeId，username 为 null，permissions 为空集）
- 注册在 `/api/admin/**` 路径上
- 代码注释明确标注："This is not authentication and must be replaced"
- admin-web 的 `request.ts` 硬编码 `X-User-Id: '1'`, `X-Store-Id: '1'`

**当前无**：JWT、Session、Login API、权限拦截、路由守卫

---

## 二、admin-web 当前接入状态

### 2.1 页面总览

| 页面 | 路由 | 数据来源 | 读操作 | 写操作 | 状态标签 |
|------|------|---------|--------|--------|---------|
| Dashboard | `/dashboard` | MOCK | — | — | **全 mock** |
| 工单管理 | `/work-order` | REAL（列表+详情） | 查看：**REAL** | 支付/退款/结算/取消：**MOCK** | **mixed** |
| 配件管理 | `/parts` | REAL | 列表+详情：REAL | 新增/启停：**REAL** | **real** |
| 库存管理 | `/inventory` | REAL | 全部 REAL | 入库/调整/流水：**REAL** | **real** |
| 支付记录 | `/payment` | MOCK | — | — | **全 mock** |
| 退款记录 | `/refund` | MOCK | — | — | **全 mock** |
| 官方售后结算 | `/settlement` | MOCK | — | — | **全 mock** |
| 报销台账 | `/reimbursement` | MOCK | — | — | **全 mock** |
| 财务报表 | `/finance` | MOCK | — | — | **全 mock** |
| 字典配置 | `/dictionary` | REAL | 全 REAL | 无写操作 | **real（只读）** |
| 用户与权限 | `/user` | MOCK | — | — | **全 mock** |
| Excel 导出中心 | `/export` | MOCK | — | — | **全 mock** |

**统计：4 页面 real、1 页面 mixed、7 页面全 mock、1 页面 real 但只读**

### 2.2 各页面详细分析

#### Dashboard（全 mock）
- 显示统计卡片（工单数、收入、库存预警、待办事项）
- 数据来自 `mockDashboardData`（setTimeout 模拟）
- 无任何操作按钮
- **误导风险：LOW**（只读页面，不执行操作）

#### 工单管理（mixed — 列表真实，操作按钮全部 mock）

**真实功能：**
- 工单列表查询（8 个筛选条件，分页） — 真实调用 `GET /api/admin/work-orders`
- 工单详情查看 — 真实调用 `GET /api/admin/work-orders/{id}`
- 详情 drawer 展示：基础信息、费用明细表、支付汇总、官方售后信息

**Mock 按钮（HIGH 误导风险）：**

| 按钮 | 位置 | 实际行为 | 误导风险 |
|------|------|---------|---------|
| 新建工单 | 页面顶部 | `ElMessage.info('后续接入新建工单页面')` | **LOW** — 明确提示"后续" |
| 记录支付 | 工单行操作 | dialog title "记录支付 (Mock)"，按钮文本"保存 mock"，handler 只弹 success 提示 | **HIGH** — 表单看起来真实可用 |
| 记录退款 | 工单行操作 | dialog title "记录退款 (Mock)"，按钮文本"保存 mock" | **HIGH** — 同上 |
| 完成结算 | 工单行操作 | MessageBox 说"这是 mock 操作"，确认后弹 success | **MEDIUM** — MessageBox 有提示但不醒目 |
| 取消工单 | 工单行操作 | MessageBox 说"这是 mock 操作"，确认后弹 success | **MEDIUM** — 同上 |

**页面 alert：** "当前页面仅展示操作入口，是否允许结算、取消、退款，最终以后端校验为准。"

#### 配件管理（real）

**已接入：**
- 列表查询 — `GET /api/admin/parts`
- 新增配件（官方/第三方） — `POST /api/admin/parts/official` / `POST /api/admin/parts/third-party`
- 启用/停用 — `POST /api/admin/parts/{partId}/enable` / `POST /api/admin/parts/{partId}/disable`

**未完成：**
- "查看"按钮只弹 `ElMessage.info('查看配件: ' + row.partName)`，未打开详情对话框（`getPartDetail` API 已定义但未在组件中使用）

#### 库存管理（real — 最完整）

**已接入：**
- 库存列表 — `GET /api/admin/inventory/stocks`
- 入库 — `POST /api/admin/inventory/inbound`
- 库存调整 — `POST /api/admin/inventory/adjust`
- 流水查询 — `GET /api/admin/inventory/flows`

#### 支付记录（全 mock）
- 列表数据来自 mock
- "新增支付记录" dialog — 标题含 "(Mock)"，按钮 "保存 mock"
- **误导风险：HIGH** — 表单字段看起来完整真实

#### 退款记录（全 mock）
- 列表数据来自 mock
- "新增退款记录" dialog — 含 "(Mock)"，按钮 "保存 mock"
- **误导风险：HIGH** — 同上

#### 官方售后结算（全 mock）
- 列表数据来自 mock
- "录入结算" dialog — 含 "(Mock)"，按钮 "保存 mock"
- "标记已结算" — MessageBox 提示 mock 操作
- **误导风险：HIGH** — 表单含结算金额、结算时间等关键字段

#### 报销台账（全 mock）
- 列表数据来自 mock
- "新增报销"、"确认"、"驳回"、"取消" — 全部 mock
- **误导风险：MEDIUM** — 按钮标签明确含 "Mock"

#### 财务报表（全 mock）
- 汇总数据和列表数据都来自 mock
- "导出 mock" 按钮 — `ElMessage.info('mock 导出操作...')`
- 快捷日期切换 — `ElMessage.info('mock 切换快捷日期: ...')`
- 页面有 alert："当前页面仅展示 mock 数据，不执行真实财务计算"
- **误导风险：LOW** — 明确标注了 mock

#### 字典配置（real，只读）
- 字典类型列表 — `GET /api/admin/dict/types`
- 字典项列表 — `GET /api/admin/dict/types/{typeCode}/items`
- 无增删改操作
- **状态正常**

#### 用户与权限（全 mock）
- 用户列表、角色列表、权限列表 — 全 mock
- "编辑"、"分配角色"、"启用/停用" — 全 mock
- 页面有 alert："真实登录认证、JWT、权限拦截和后端授权校验后续单独实现"
- **误导风险：LOW** — 明确标注

#### Excel 导出中心（全 mock）
- 所有操作为 mock — "创建 mock 导出任务"、"下载 mock，不生成真实文件"
- **误导风险：LOW** — 按钮标签明确含 "mock"

### 2.3 误导风险汇总

| 风险等级 | 页面 | 问题 |
|---------|------|------|
| **HIGH** | 工单管理 | 记录支付/退款 dialog 看起来完整可用，但不调用后端 |
| **HIGH** | 支付记录 | 新增支付 dialog 看起来完整可用 |
| **HIGH** | 退款记录 | 新增退款 dialog 看起来完整可用 |
| **HIGH** | 官方售后结算 | 录入结算 dialog 看起来完整可用 |
| MEDIUM | 工单管理 | 完成结算/取消工单的 MessageBox 有 mock 提示但不醒目 |
| MEDIUM | 报销台账 | 多个操作按钮含 Mock 提示 |
| LOW | Dashboard / 财务报表 / 用户权限 / 导出中心 | 有明确 mock/alert 标注 |

---

## 三、端职责边界确认

基于 PRD（product.md）、核心业务规则（xiaoniu_03_CORE_BUSINESS_RULES.md）、客户端边界（client-boundary.md）和领域模型（xiaoniu_04_DOMAIN_MODEL.md）确认。

### 3.1 功能归属

| 功能 | admin-web | staff/mobile 小程序 | 两端都可查看 | 后端-only 规则控制 | MVP 暂不做 |
|------|:---------:|:-------------------:|:-----------:|:-----------------:|:---------:|
| 创建工单 | | **主功能** | | | |
| 编辑工单基础信息 | | **主功能** | | | |
| 添加 PART/LABOR/OTHER 费用项 | | **主功能** | | | |
| 临时新增配件并入库 | | **主功能** | | | |
| 提交工单 submit | | **主功能** | | | |
| 取消工单 cancel | | **主功能** | | | |
| 完成结算 settle | | **主功能** | | | |
| 记录客户支付 | | **主功能** | | | |
| 记录客户退款 | | **主功能** | | | |
| 查看工单 | | | **主功能** | | |
| 查看库存 | | | **主功能** | | |
| 普通入库 | | **主功能** | | | |
| 库存调整 | **主功能** | | | | |
| 查看支付记录 | | | **主功能** | | |
| 查看退款记录 | | | **主功能** | | |
| 官方结算录入 | **主功能** | | | | |
| 报销提交 | | **主功能** | | | |
| 报销确认 | **主功能** | | | | |
| 财务报表 | **主功能** | | | | |
| Excel 导出 | **主功能** | | | | |
| 用户/角色/权限管理 | **主功能** | | | | |
| 工单状态机流转 | | | | **后端强制** | |
| 库存三量模型 | | | | **后端强制** | |
| 金额计算 receivedAmount | | | | **后端强制** | |
| 退款限额校验 | | | | **后端强制** | |

### 3.2 边界说明

**Staff/Mobile 小程序端应具备的前端操作：**
- 在维修现场创建工单（录入客户信息、车辆信息）
- 添加费用项（选择配件、输入工时费、其他费用）
- 临时新增配件并入库（现场发现的非库存配件）
- 提交工单（触发库存预占）
- 记录客户支付（微信/支付宝/现金等混合支付）
- 记录客户退款
- 取消工单（释放预占库存）
- 查看自己负责的工单
- 查询可用库存（选配件用）
- 普通入库（扫码/手动）
- 提交报销

**Admin-Web 管理端应具备的前端操作：**
- 查看全部工单列表和详情（监管视角）
- 查看库存和流水
- 库存调整（盘点场景，管理员权限）
- 官方结算录入和标记
- 报销确认/驳回
- 财务报表查看和 Excel 导出
- 用户/角色/权限管理
- 字典配置

**关键架构原则：** admin 和 mobile 共享同一个 Service 层。所有业务规则（状态机、库存、金额）由后端强制执行，无论哪个前端调用。

---

## 四、admin-web 按钮控制建议

### 4.1 工单管理页面（重点）

**当前状态：** 列表和详情查看为真实 API，所有操作按钮为 mock。

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 查询/重置 | REAL | **保留** | 管理端需要查看工单列表 |
| 查看详情 | REAL | **保留** | 管理端需要查看工单详情 |
| 新建工单 | mock → ElMessage | **隐藏** | 创建工单是现场操作，属于 staff/mobile 小程序端 |
| 记录支付 | mock dialog | **隐藏** | 记录支付是现场收费，属于 staff/mobile 小程序端 |
| 记录退款 | mock dialog | **隐藏** | 记录退款是现场操作，属于 staff/mobile 小程序端 |
| 完成结算 | mock confirm | **隐藏** | 结算是现场操作，属于 staff/mobile 小程序端 |
| 取消工单 | mock confirm | **隐藏** | 取消是现场操作，属于 staff/mobile 小程序端 |

**结论：工单管理页面应只保留"查看"功能，其余 4 个操作按钮和 2 个 mock dialog 应隐藏。**

### 4.2 配件管理页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 新增配件 | REAL | **保留** | 管理端维护配件主数据 |
| 启用/停用 | REAL | **保留** | 管理端管理配件生命周期 |
| 查看 | ElMessage（API 已有） | **disabled 显示"后续接入"** | getPartDetail API 存在但组件未使用，后续接入即可 |

### 4.3 库存管理页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 查看库存 | REAL | **保留** | 管理端核心查看功能 |
| 入库 | REAL | **保留** | 管理端可做入库（补录场景） |
| 库存调整 | REAL | **保留** | 管理端盘点调整是管理员权限操作 |
| 查看流水 | REAL | **保留** | 管理端审计需要 |

### 4.4 支付记录页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 列表展示 | MOCK → REAL | **接入 `GET /api/admin/payments`** | 后端已有全局支付查询 API |
| 新增支付记录 | mock dialog | **隐藏** | 支付记录由 staff/mobile 端产生，管理端只查看 |
| 查看详情 | MOCK | **保留**（接入后真实） | 管理端查看支付详情 |

### 4.5 退款记录页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 列表展示 | MOCK → REAL | **接入 `GET /api/admin/refunds`** | 后端已有全局退款查询 API |
| 新增退款记录 | mock dialog | **隐藏** | 退款由 staff/mobile 端操作，管理端只查看 |
| 查看详情 | MOCK | **保留**（接入后真实） | 管理端查看退款详情 |

### 4.6 官方售后结算页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 列表展示 | MOCK → REAL | **接入 `GET /api/admin/official-after-sales`** | 后端已有全局查询 API |
| 录入结算 | mock dialog | **接入真实 API**（后端已有） | **这是管理端核心功能** |
| 标记已结算 | mock | **接入真实 API**（后端已有） | 同上 |
| 查看详情 | MOCK | **保留**（接入后真实） | — |

### 4.7 报销台账页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 列表 | MOCK | **disabled 显示"后续接入"** | 后端 ReimbursementService 尚未实现（Phase 6） |
| 新增报销 | mock | **隐藏** | 新增报销属于 staff/mobile 端 |
| 确认/驳回 | mock | **保留**（disabled） | 管理端功能，但后端未实现 |
| 取消 | mock | **disabled** | 后端未实现 |

### 4.8 财务报表页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 全部功能 | MOCK | **disabled 显示"后续接入"** | 后端 FinanceService 尚未实现（Phase 7） |

### 4.9 Excel 导出中心页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 全部功能 | MOCK | **disabled 显示"后续接入"** | 后端 ExportController 尚未实现 |

### 4.10 用户与权限页面

| 按钮 | 当前状态 | 建议 | 原因 |
|------|---------|------|------|
| 全部功能 | MOCK | **disabled 显示"后续接入"** | 后端 Auth/User API 尚未实现 |
| 页面 alert | 已有"后续实现"提示 | **保留** | — |

### 4.11 Mock dialog 删除/改造清单

| 页面 | Dialog | 建议 |
|------|--------|------|
| 工单管理 | 记录支付 (Mock) | **删除整个 dialog** |
| 工单管理 | 记录退款 (Mock) | **删除整个 dialog** |
| 支付记录 | 新增支付记录 (Mock) | **删除整个 dialog** |
| 退款记录 | 新增退款记录 (Mock) | **删除整个 dialog** |
| 官方售后结算 | 录入/编辑官方结算 (Mock) | **改造为真实 API 调用** |
| 报销台账 | 新增/确认/驳回 (Mock) | **改为 disabled 提示"后端待实现"** |
| 用户与权限 | 编辑/分配角色 (Mock) | **改为 disabled 提示"后端待实现"** |
| Excel 导出 | 新建导出任务 (Mock) | **改为 disabled 提示"后端待实现"** |

---

## 五、后续任务建议

基于当前状态（后端 7 个 Controller 37 个 endpoint，前端 4 页面 real / 7 页面 mock），建议以下 8 个任务按序推进。

### Task 16B：admin-web 按钮/页面状态清理

**范围：**
- 工单管理页面：隐藏"新建工单"、"记录支付"、"记录退款"、"完成结算"、"取消工单"按钮和对应 mock dialog
- 支付记录/退款记录页面：隐藏"新增"按钮和 mock dialog
- 报销/财务/导出/用户权限页面：所有按钮改 disabled + "后续接入"提示
- 配件管理"查看"按钮：disabled + "后续接入"

**原因：** 先清理误导性 mock 操作，避免 Owner 或测试人员误以为功能已实现。

### Task 17：Payment/Refund 列表接入真实 API

**范围：**
- 后端已有 `GET /api/admin/payments` 和 `GET /api/admin/refunds` 全局查询 API
- admin-web payment.ts 改为调用真实 API
- admin-web refund.ts 改为调用真实 API
- 列表字段对齐后端 response
- 工单详情 drawer 中的支付汇总信息可考虑展示关联支付记录

**原因：** 后端已就绪，接入成本低，且能验证后端 Payment/Refund 查询逻辑。

### Task 18：OfficialAfterSales 列表 + 操作接入

**范围：**
- 后端已有 5 个官方售后 API
- admin-web settlement.ts 改为调用 `GET /api/admin/official-after-sales`
- "录入结算" 和 "标记已结算" 对接真实 API
- 工单详情 drawer 中的官方售后信息区域完善

**原因：** 官方结算录入是 admin-web 核心管理功能，后端已完整实现。

### Task 19：Parts 查看详情 + Edit dialog 完善

**范围：**
- 使用已有的 `getPartDetail` API 打开详情/编辑对话框
- 更新配件信息（`PUT /api/admin/parts/{partId}`）
- 完善配件详情展示字段

**原因：** 前后端 API 都已存在，补全 UI 交互即可。

### Task 20：Staff/Mobile API Scope 设计

**范围：**
- 设计 `/api/mobile/**` 路径规划（哪些 endpoint 需要 mobile 版本）
- 明确 mobile 端与 admin 端共享哪些 Service、各自需要哪些 Controller
- 设计 token-based 认证方案（替代 dev-only Header）
- 确定角色与权限映射关系

**输出：** `docs/api/mobile-api-scope.md`

**原因：** 在实现小程序端之前，先做 API scope 设计，避免重复开发或遗漏。

### Task 21：Auth/User/Permission 方案

**范围：**
- 设计登录流程（微信小程序登录 + admin-web 密码/验证码登录）
- 实现 JWT Token 签发和校验
- 替换 DevCurrentUserInterceptor
- 用户/角色/权限 CRUD API
- 路由守卫和按钮级权限控制

**原因：** 当前 dev-only Header 是开发阶段临时方案，正式部署前必须替换。数据层（Entity、Mapper、Service）已就绪。

### Task 22：Reimbursement 后端实现

**范围：**
- 实现 ReimbursementService（submit/confirm/reject/cancel）
- 新建 ReimbursementController
- admin-web 报销台账页面接入真实 API
- 报销提交（staff/mobile）和报销确认（admin-web）的完整流程

**原因：** 按 Roadmap Phase 6 计划，但不影响核心工单/库存/支付流程。

### Task 23：Finance 报表 + Excel 导出

**范围：**
- 实现 FinanceService（queryDaily/queryMonthly/queryRange）
- 实现 ExportController
- admin-web 财务报表和导出中心接入

**原因：** 按 Roadmap Phase 7 计划，是 MVP 收尾阶段功能。

### 优先级排序

```
16B → 17 → 18 → 19 → 20 → 21 → 22 → 23
 清理   接入    接入    补全    设计     实现    实现
 mock  支付/   官方    配件    小程序   认证    报销    财务
 按钮  退款    结算    详情    API     权限    导出
```

- Task 16B 是无风险纯 UI 清理，可立即执行
- Task 17-18 接入已有后端 API，不改业务逻辑
- Task 19 补全配件详情，小工作量
- Task 20-21 为小程序端做准备
- Task 22-23 是 Roadmap 后期功能

---

## 六、风险清单

### HIGH 风险

| # | 风险 | 当前状态 | 影响 | 建议 |
|---|------|---------|------|------|
| H1 | admin-web mock 按钮误导 Owner 以为功能已实现 | 工单页面 4 个 mock 操作按钮、支付/退款/结算页面 mock dialog | Owner 可能基于错误认知做决策 | **立即清理**（Task 16B） |
| H2 | admin-web 越界做员工现场操作 | 新建工单、记录支付、取消工单等在 admin-web 有 mock 入口 | 打破端职责边界，造成维护混乱 | **隐藏**，明确归属 staff/mobile |
| H3 | dev-only Header 被误当正式认证 | 所有 Controller 依赖 `X-User-Id` Header，无 JWT | 开发环境可正常使用，但部署到真实环境 = 零认证 | **在 Phase 9 前替换**（Task 21） |

### MEDIUM 风险

| # | 风险 | 当前状态 | 影响 | 建议 |
|---|------|---------|------|------|
| M1 | Payment/Refund 被误认为真实支付网关 | admin-web mock dialog 中有支付金额、方式等字段 | 用户可能以为对接了微信/支付宝 | 明确标注"手工记录"，不接入支付网关 |
| M2 | 官方结算和客户支付混淆 | 两个独立概念但 UI 上容易混淆 | 财务统计时口径错乱 | 官方结算页面明确标注"这是官方售后结算，与客户支付分开记录" |
| M3 | 工单操作绕过后端状态机 | admin-web mock 操作无后端校验 | 如果 mock 被改为真实调用但跳过状态校验，会破坏数据一致性 | 所有工单操作必须通过后端 action endpoint，禁止直接修改 status 字段 |
| M4 | 报销提交直接入成本 | ReimbursementService 尚未实现，规则待确认 | PENDING 状态报销不应计入成本，只有 CONFIRMED 才计入 | 实现时严格按规则：只有 CONFIRMED 计入 operating cost |

### LOW 风险

| # | 风险 | 当前状态 | 影响 | 建议 |
|---|------|---------|------|------|
| L1 | 库存调整绕过流水 | 已有防护（每次 adjust 写 inventory_flow） | — | 已解决，保持现状 |
| L2 | 财务报表手工改利润 | FinanceService 尚未实现 | — | 实现时确保利润从明细表聚合，禁止手工输入利润值 |
| L3 | 配件"查看"按钮无详情 | `getPartDetail` API 已定义但组件未使用 | 用户点击无反馈 | Task 19 补全 |

### 已排除的风险（当前状态不存在）

- ~~admin-web 真实调用 submit/settle/cancel~~ — 当前全部是 mock
- ~~前端绕过后端修改库存~~ — 库存操作已接入真实后端
- ~~支付金额绕过后端计算~~ — mock 不涉及真实计算

---

## 七、输出摘要

### 报告路径
`docs/dev/task-16a-current-capability-and-ui-boundary.md`

### 是否修改代码
否。仅做检查和文档输出。

### 后端 API 总数/模块数
- **7 个 Controller，37 个 endpoint**
- 模块分布：Health(1) + Dict(2) + Part(7) + Inventory(5) + WorkOrder(10) + Payment(4) + Refund(3) + OfficialAfterSales(5)
- 另有 3 个模块为空壳：Reimbursement、Finance、Export
- 数据库 22 张表，4 个 Flyway 迁移
- 328 个单元/集成测试

### admin-web 页面状态摘要

| 状态 | 页面数 | 页面列表 |
|------|--------|---------|
| **real（完整接入）** | 2 | 配件管理、库存管理 |
| **real（只读）** | 1 | 字典配置 |
| **mixed（读真实 + 写 mock）** | 1 | 工单管理 |
| **全 mock** | 7 | Dashboard、支付记录、退款记录、官方结算、报销台账、财务报表、用户权限、导出中心 |

### 建议隐藏/禁用的按钮列表

| 页面 | 按钮 | 建议操作 |
|------|------|---------|
| 工单管理 | 新建工单 | 隐藏 |
| 工单管理 | 记录支付 | 隐藏（含 dialog） |
| 工单管理 | 记录退款 | 隐藏（含 dialog） |
| 工单管理 | 完成结算 | 隐藏 |
| 工单管理 | 取消工单 | 隐藏 |
| 支付记录 | 新增支付记录 | 隐藏（含 dialog） |
| 退款记录 | 新增退款记录 | 隐藏（含 dialog） |
| 报销台账 | 新增报销 | hidden 或 disabled |
| 报销台账 | 确认/驳回/取消 | disabled + "后端待实现" |
| 财务报表 | 全部操作 | disabled + "后端待实现" |
| 导出中心 | 全部操作 | disabled + "后端待实现" |
| 用户权限 | 全部操作 | disabled + "后端待实现" |
| 配件管理 | 查看 | disabled + "后续接入" |

### 建议迁移到小程序的功能列表

| 功能 | 说明 |
|------|------|
| 创建工单 | 现场录入客户/车辆/维修信息 |
| 编辑工单基础信息 | 修改草稿工单内容 |
| 添加/编辑/删除费用项 | 选择配件、输入工时费、其他费用 |
| 临时新增配件并入库 | 现场发现的非库存配件 |
| 提交工单 submit | 触发库存预占，流转状态 |
| 取消工单 cancel | 释放预占库存 |
| 完成结算 settle | 校验金额后扣减库存 |
| 记录客户支付 | 现场收费（微信/支付宝/现金） |
| 记录客户退款 | 退差价 |
| 普通入库 | 扫码/手动入库 |
| 提交报销 | 员工报销申请 |

### 推荐下一步任务
**Task 16B：admin-web 按钮/页面状态清理** — 无风险 UI 清理，消除 mock 误导。

### 是否有阻塞项
无。所有推荐任务的后端 API 已就绪（Task 17-19）或模块设计已明确（Task 20-23），无外部依赖阻塞。
