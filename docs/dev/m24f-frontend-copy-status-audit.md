# M24F 前端状态文案与用户可见术语审查

## 1. 审查范围

- `admin-web/src/views`
- `admin-web/src/components`
- `admin-web/src/layouts`
- `admin-web/src/router`
- `admin-web/src/api`
- `admin-web/src/types`
- `admin-web/src/mock`
- `mini-program/src/pages`
- `mini-program/src/components`
- `mini-program/src/api`
- `mini-program/src/types`
- `mini-program/src/mock`
- `docs/dev` 中 M24 相关文档

## 2. 搜索词

已按任务要求搜索工单进度状态、收银状态、库存状态、旧状态、DTO 字段名、金额字段、权限字段、错误码和程序术语。

重点搜索词包括：

- 工单状态：`DRAFT`、`REPAIRING`、`REPAIR_DONE`、`DELIVERED`、`CANCELLED`
- 收银状态：`NO_CHARGE`、`UNPAID`、`PARTIAL_PAID`、`PAID`、`REFUND_PENDING`、`PARTIAL_REFUNDED`、`REFUNDED`
- 库存状态：`NOT_RESERVED`、`RESERVED`、`CONSUMED`、`RELEASED`
- 旧状态：`PENDING_ACCEPT`、`ACCEPTED`、`PART_ORDERED`、`PART_ARRIVED`、`SETTLED`
- DTO / 金额字段：`progressStatusText`、`cashierStatusText`、`inventoryStatusText`、`paymentTotal`、`refundTotal`、`netReceived`、`outstandingAmount`、`refundableAmount`
- 错误码：`WORK_ORDER_LEGACY_SETTLE_DISABLED`、`WORK_ORDER_LEGACY_STATUS_EXISTS`、`PAYMENT_EXCEEDS_RECEIVABLE`、`REFUND_EXCEEDS_PAID_AMOUNT`、`FORBIDDEN`、`UNAUTHORIZED`

## 3. 修复页面清单

### admin-web

- 工单管理：列表三态标签、详情抽屉三态标签、无需收款原因、收银摘要、收款记录、交付关闭确认文案。
- 首页：最近工单状态、待处理工单文案。
- 官方结算：工单状态标签、客户实收与官方结算口径文案。
- 客户档案 / 车辆档案：维修历史中的工单状态标签。
- 财务报表 / 收银日报 / 收款记录 / 退款记录 / 报销台账 / 库存流水：用户可见标签和未知值兜底。
- 基础配置和 mock 数据：工单状态、收款方式文案与 mock 状态值。
- 请求拦截器：后端错误码转中文提示。

### mini-program

- 工单列表：进度、收银、库存状态集中映射。
- 工单详情：三态标签、无需收款原因、交付关闭确认文案。
- 创建工单页：状态展示、收银状态、库存状态兜底映射。
- 请求拦截器：后端错误码转中文提示。

## 4. 状态文案映射表

### 工单进度状态

| 后端值 | 用户展示 |
|---|---|
| `DRAFT` | 新建中 |
| `REPAIRING` | 维修中 |
| `REPAIR_DONE` | 维修完成 |
| `DELIVERED` | 已交付 |
| `CANCELLED` | 已取消 |
| 旧状态或未知旧工单状态 | 旧状态，请先清理试运行数据 |

### 收银状态

| 后端值 | 用户展示 |
|---|---|
| `NO_CHARGE` | 无需收款 |
| `UNPAID` | 未收款 |
| `PARTIAL_PAID` | 部分收款 |
| `PAID` | 已收齐 |
| `REFUND_PENDING` | 待退款 |
| `PARTIAL_REFUNDED` | 部分退款 |
| `REFUNDED` | 已退清 |

### 库存状态

| 后端值 | 用户展示 |
|---|---|
| `NOT_RESERVED` | 未预占 |
| `RESERVED` | 已预占 |
| `CONSUMED` | 已扣减 |
| `RELEASED` | 已释放 |

## 5. admin-web 修复点

- 新增 `admin-web/src/utils/statusText.ts`，集中处理三态中文文案、旧状态提示、无需收款原因、错误码中文提示。
- 状态展示优先使用后端 `*StatusText` 字段，缺失时使用前端中文映射兜底。
- 旧版收款和关闭工单相关用户文案已统一为“记录收款”和“交付关闭”。
- 旧工单状态文案不再作为正常状态展示，统一兜底为“旧状态，请先清理试运行数据”。
- 官方结算模块保留“已结算”作为官方结算状态，不作为工单进度状态。

## 6. mini-program 修复点

- 新增 `mini-program/src/utils/statusText.ts`，集中处理三态中文文案、旧状态提示、无需收款原因、错误码中文提示。
- 工单列表、工单详情、创建工单页统一使用中文状态映射。
- 小程序详情页不再直接展示 `noChargeReason` 代码，改为中文原因。
- 交付关闭文案保持：“库存已在标记维修完成时扣减，本操作不再改变库存。”

## 7. 剩余非阻塞项

- 历史非 M24 文档中仍有旧状态机技术说明，属于历史记录，不作为当前交付验收页面文案。
- 权限管理等管理型页面仍会展示权限代码，这是管理员配置语境下的技术标识，不属于本次工单状态枚举暴露。

## 8. 验证命令结果

| 命令 | 结果 |
|---|---|
| `cd admin-web && npx vue-tsc --noEmit` | 通过，exit code 0 |
| `cd admin-web && npm run build` | 通过，exit code 0。仅有 Vite chunk size 与 Rollup pure annotation 警告，非本次业务错误。 |
| `cd mini-program && npx tsc --noEmit` | 未通过，exit code 2。错误集中在历史 `miniprogram-api-typings` / `tdesign-miniprogram` 类型依赖与缺失模块声明；本次修改文件未出现新增业务类型错误。 |
| `git diff --check` | 通过，exit code 0 |
| `git status --short` | 仅包含 admin-web / mini-program / docs/dev 修改 |
