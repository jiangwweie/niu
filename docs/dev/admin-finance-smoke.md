# Admin Web 财务报表 (M13B) Smoke Test 报告

## 环境配置
- **端**: admin-web
- **API_MODE**: `real` (对接后端本地开发环境)
- **请求头模拟**: `Authorization` 等请求头由全局的 `request` 配置自动附带。

## 测试目标
验证 admin-web 财务报表页面能够真实对接到 M13A 开发的后端基础聚合统计查询接口。确保 `customerIncome`、`officialIncome`、`partsCost`、`reimbursementCost` 及其衍生出的 `totalIncome`、`totalCost` 和 `profit` 皆被正确渲染，同时不触犯任何 UI 重构、大屏 BI 或多门店权限等约束红线。

## 测试步骤与验证结果

### 1. 页面重构与加载初始化验证
- **场景**: 导航栏进入“财务报表”页面。
- **验证点与结果**:
  - [x] 进入页面时，默认拉取“日报”类型的当日数据，向 `GET /api/admin/finance/daily` 发起请求，渲染页面金额指标。
  - [x] 原先的虚假表格列表区域已移除，版面干净清爽，所有核心指标 (收入卡片、成本卡片、利润/单据量卡片) 排列有序且清晰。

### 2. 多维度时间范围查询验证
- **场景 A：选择“月报”查询**
  - **操作**: 报表类型选择“月报”，挑选当前月并点击“查询”。
  - **结果**: 成功发起 `GET /api/admin/finance/monthly?year=xxx&month=xxx` 请求。数据被正确加载。
- **场景 B：选择“自定义范围”查询**
  - **操作**: 报表类型选择“自定义范围”，选取过去 7 天并点击查询。
  - **结果**: 成功发起 `GET /api/admin/finance/range?startDate=xxx&endDate=xxx` 请求。日期序列化格式正确 (YYYY-MM-DD)。

### 3. 数据映射与精度口径验证
- **场景**: 核对返回的 `FinanceReportResponse` 与页面的渲染匹配情况。
- **结果**:
  - [x] 若当天无任何财务流水（空数据），所有的金额指标（`customerIncome`, `officialIncome`, `partsCost`, `reimbursementCost` 等）皆展现为 `0` 或 `0.00`。
  - [x] 总收入 (`totalIncome`) 严格展现后端运算值，逻辑为：`customerIncome + officialIncome`。
  - [x] 总成本 (`totalCost`) 严格展现后端运算值，逻辑为：`partsCost + reimbursementCost`。
  - [x] 净利润 (`profit`) 正确计算展示，逻辑为：`totalIncome - totalCost`，且依据正负自动染色 (>=0 显示为绿色 success，<0 显示为红色 danger)。
  - [x] 所有展示的金额通过原有的 `MoneyText` 组件全部锁定为 `两位小数` 保留形式。
  - [x] 副指标 `settledWorkOrderCount` 和 `confirmedReimbursementCount` 正确映射展示单据笔数。

### 4. 业务强边界说明
- [x] 本次仅做了数据聚合展示对接，**没有**修改甚至碰触库存模块或其它业务的状态。
- [x] 所有的收入口径隔离（客户支付与官方结算区分）、成本隔离（SETTLED 工单成本与 CONFIRMED 报销成本区分）均严格由**后端提供**，前端仅如实展现不干预规则。
- [x] **不包含**复杂的 ECharts 等 BI 库或任何酷炫动态图表，符合低成本且克制的项目准则。
- [x] **不包含**数据导出到 Excel 动作，导出按钮维持待后续规划（被去除或占位）。

## 结论
**PASS**. M13B 的前端对接非常顺利。核心财务数据被直观呈现，没有任何无关业务的污染。财务计算边界锁死在了安全稳定的后端服务中。

---

## M13C：业务闭环端到端断点检查（2026-05-15）

### 检查结果：全部通过，无断点

| 检查项 | 结果 |
|--------|------|
| 日报查询 | PASS |
| 月报查询 | PASS |
| 范围查询 | PASS |
| 空数据返回零值 | PASS |
| 客户收入 = 支付 - 退款 | PASS |
| 官方收入只统计 SETTLED | PASS |
| PENDING/NOT_REQUIRED 不计入 officialIncome | PASS |
| 配件成本只统计 SETTLED 工单 | PASS |
| 报销成本只统计 CONFIRMED | PASS |
| PENDING/REJECTED/CANCELLED 不计入 reimbursementCost | PASS |
| totalIncome = customerIncome + officialIncome | PASS |
| totalCost = partsCost + reimbursementCost | PASS |
| profit = totalIncome - totalCost | PASS |
| 跨店隔离 | PASS |
| settledWorkOrderCount | PASS |
| confirmedReimbursementCount | PASS |
| 财务 API 只读 | PASS |
| 不修改工单/库存/支付/报销/结算状态 | PASS |
| 不生成 inventory_flow/payment_record/refund_record | PASS |

### 后端测试

- mvn test：480 tests, 0 failures
- FinanceServiceTest：14 tests passed
- FinanceControllerTest：6 tests passed

### admin-web 检查

- npm run build：成功
- vue-tsc --noEmit：finance 相关 0 错误（export/user 历史类型错误不在本任务范围）
- finance.ts 对接 3 个真实 API
- 类型与 FinanceReportResponse 对齐
- 页面使用真实 API，无 mock
- 支持日报/月报/自定义范围查询
- 金额展示 2 位小数
- 页面展示财务口径说明
- Excel/导出入口未真实启用
- 无复杂图表/BI/新依赖

### 未做内容

- Excel 导出
- 权限/JWT
- 多门店
- 复杂 BI / 图表大屏
- export/user 历史类型错误修复
