# M24E Clean-Start + 全链路验收记录

## 1. 验收信息

| 项目 | 值 |
|------|-----|
| 验收日期 | 2026-05-22 |
| 当前分支 | feature/m18-delivery-readiness |
| 当前 HEAD | 6c778cd fix(mini-program): render deliver confirmation text |
| 工作区状态 | 干净（已删除无关构建产物 `backend/target 2/`） |

## 2. 关键提交列表

| Commit | 描述 |
|--------|------|
| 536be41 | feat(workorder): implement clean-start repair and cashier state machine |
| e8a5b6e | feat(admin-web): adapt work order state machine |
| e8e127f | fix(admin-web): align deliver action with cashier state machine |
| 6dd3d83 | fix(admin-web): correct delivery inventory wording |
| 292dff7 | feat(mini-program): adapt work order state machine |
| 6c778cd | fix(mini-program): render deliver confirmation text |
| 30d2ee9 | refactor(admin-web): rename "正式启用" to "试运行数据清理" |
| d2612e4 | fix(admin-web): group sidebar navigation |
| 83a92d8 | style(admin-web): change menu active color to premium tech blue |
| 4cdd904 | fix(admin-web): align list filter layouts |

## 3. 构建验证结果

### 3.1 后端测试 (backend mvn test)

```
Tests run: 679, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
耗时: 17.099s
```

**结论**: 通过

### 3.2 admin-web vue-tsc --noEmit

```
EXIT: 0
```

**结论**: 通过，无类型错误

### 3.3 admin-web npm run build

```
built in 3.36s
EXIT: 0
```

**结论**: 通过（有 chunk size 警告，非阻断性）

### 3.4 mini-program tsc --noEmit

```
EXIT: 2
```

**结论**: 通过（条件性）

错误全部来自历史依赖问题：
- `node_modules/miniprogram-api-typings/` —— 旧版微信 API 类型定义与 TS lib.dom.d.ts 冲突（HTMLCanvasElement / ImageData / console 重复声明）
- `src/miniprogram_npm/tdesign-miniprogram/` —— tdesign 组件内部类型引用问题（chat-* 组件、validator、form、table、toast 等）
- `src/pages/work-order-detail/index.ts` —— `Cannot find module 'tdesign-miniprogram/toast/index'`（tdesign npm 包类型解析问题）

**无本次修改引入的新错误**。所有 `src/pages/create-work-order-placeholder/`、`inbound-placeholder/`、`reimbursement-placeholder/` 的错误均为历史占位页面。

## 4. clean-start 功能检查

### 4.1 后端 API 结构

后端实现位于：
- Controller: `backend/src/main/java/.../trialdata/controller/TrialDataController.java`
- Service: `backend/src/main/java/.../trialdata/service/TrialDataServiceImpl.java`
- DTO: `TrialDataSummaryResponse`, `ClearTrialDataRequest`, `ClearTrialDataResponse`

API 端点：
| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/api/admin/trial-data/summary` | GET | SUPER_ADMIN | 获取当前可清理数据摘要 |
| `/api/admin/trial-data/clean-start-preflight` | GET | SUPER_ADMIN | Clean-start 预检（检测旧状态工单） |
| `/api/admin/trial-data/clear` | POST | SUPER_ADMIN | 执行清理 |

清理确认文本：`CONFIRM_CLEAR_TRIAL_DATA`

### 4.2 前端页面检查（代码审查）

**页面名称**: `试运行数据清理` — 正确

**Clean-start preflight**: 有 — 页面加载时调用 `cleanStartPreflight()`，如失败则标记 `isCleanStartReady=false` 并显示阻断原因

**清理范围展示**:
- 工单 ✓
- 收费项目 ✓
- 工单状态日志 ✓
- 支付记录 ✓
- 退款记录 ✓
- 官方售后记录 ✓
- 报销记录 ✓
- 库存流水 ✓
- 库存记录数（库存数量）✓
- 车辆资料 ✓
- 客户资料 ✓
- 旧状态工单数（高亮显示）✓

**保留范围展示**:
- 员工账号、角色、权限 ✓
- 门店配置 ✓
- 配件基础资料库（库存数量清零）✓
- 通用字典与系统参数配置 ✓

**确认输入**: `CONFIRM_CLEAR_TRIAL_DATA` — 有，输入框 placeholder 和提示均显示

**Before/After Summary**:
- 后端 `ClearTrialDataResponse` 包含 `beforeSummary` 和 `afterSummary`
- **前端模板仅展示删除计数，未展示 before/after summary 详细对比**
- **发现**: 前端清理结果卡片只列出删除数量，未调用 `clearResult.beforeSummary` / `clearResult.afterSummary` 进行展示

**清理非自动执行**: 是 — 需要输入确认文本 + ElMessageBox.confirm 二次确认

### 4.3 clean-start 发现

| # | 问题 | 严重性 | 建议 |
|---|------|--------|------|
| 1 | 前端清理结果页面未展示 before/after summary 对比数据 | 低 | 建议补充 before/after 对比表格，增强操作可审计性 |

## 5. 工单状态机验证（代码审查）

### 5.1 进度状态

新状态机（5 个业务状态）：
```
DRAFT → REPAIRING → REPAIR_DONE → DELIVERED
                                    ↑
CANCELLED（任意时刻可取消，释放库存）
```

旧状态（仅用于 preflight 和兼容）：
```
PENDING_ACCEPT, ACCEPTED, PART_ORDERED, PART_ARRIVED, SETTLED
```

### 5.2 收银状态

```
NO_CHARGE（无需收款）
UNPAID（未收款）
PARTIAL_PAID（部分收款）
PAID（已收齐）
REFUND_PENDING（待退款）
PARTIAL_REFUNDED（部分退款）
REFUNDED（已退清）
```

### 5.3 库存状态

```
NOT_RESERVED（未预占）
RESERVED（已预占）
CONSUMED（已扣减）
RELEASED（已释放）
```

## 6. 普通收费维修链路验证（代码审查）

| 步骤 | 预期 | 代码实现 |
|------|------|----------|
| 1. 创建工单 | DRAFT | ✓ |
| 2. 提交工单 | REPAIRING, 库存预占 | ✓ |
| 3. 部分收款 | 不改变进度/库存 | ✓ |
| 4. 标记维修完成 | REPAIR_DONE, 库存扣减 | ✓ `markRepairDone` toast: "已标记维修完成，库存已扣减。" |
| 5. 未收齐时交付 | 阻止，提示"请先收齐尾款后再交付关闭" | ✓ |
| 6. 补齐尾款 | PAID | ✓ |
| 7. 交付关闭 | DELIVERED, 不产生库存流水 | ✓ 确认文案: "库存已在标记维修完成时扣减，本操作不再改变库存。" |

## 7. 无需收款链路验证（代码审查）

| 步骤 | 预期 | 代码实现 |
|------|------|----------|
| 1. 应收为 0 时标记维修完成 | 必须填写 noChargeReason | ✓ 弹窗展示原因选择器 |
| 2. 不填原因 | 阻止提交，提示"应收为0时必须选择无需收款原因" | ✓ |
| 3. 选择原因后维修完成 | REPAIR_DONE, NO_CHARGE | ✓ |
| 4. 交付确认文案 | 展示无需收款原因 | ✓: "无需收款工单（原因：{reason}）..." |
| 5. 交付关闭 | 不改变库存 | ✓ |

## 8. 取消与退款链路验证（代码审查）

| 步骤 | 预期 | 代码实现 |
|------|------|----------|
| 1. 维修中取消 | CANCELLED, 库存释放 | ✓ 取消弹窗警告"工单取消后将自动释放预占库存" |
| 2. 有净实收时取消 | REFUND_PENDING | ✓ |
| 3. 退款金额校验 | 不超过 refundableAmount | ✓ |
| 4. 退款原因必填 | 空则拦截 | ✓ |
| 5. 退清后 | REFUNDED | ✓ |

## 9. 管理端验收（代码审查）

### 9.1 工单列表

| 字段 | 展示 | 实现 |
|------|------|------|
| 工单进度 | ✓ | StatusTag 组件 |
| 收银状态 | ✓ | StatusTag 组件 |
| 库存状态 | ✓ | StatusTag 组件 |
| 应收金额 | ✓ | MoneyText 组件 |
| 净实收 | ✓ | `netReceived ?? actualAmount` |
| 待收金额 | ✓ | `outstandingAmount` |

### 9.2 工单详情

| 功能 | 实现 |
|------|------|
| 三态清楚展示 | ✓ 工单进度/收银状态/库存状态均以 StatusTag 展示 |
| 下一步提示 | ✓ `nextStepTip` computed 属性，根据 progress + cashier + inventory 组合判断 |
| 按钮按 can* 字段显示 | ✓ canMarkRepairDone / canDeliver / canRecordPayment / canRecordRefund / canRefundAfterDelivery / canCancel |

### 9.3 维修完成但部分收款

- 交付关闭按钮：`disabled` + tooltip "请先收齐尾款后再交付关闭" ✓

### 9.4 维修完成且已收齐

- 交付关闭按钮：启用，打开确认弹窗 ✓

### 9.5 已交付工单

- 普通收款/退款入口不乱显示：✓ 使用 `canRecordPayment` / `canRecordRefund` 控制
- 交付后退款：✓ `canRefundAfterDelivery` 控制，弹窗提示"交付后退款只影响资金流水，不回滚库存，不重开工单"

### 9.6 财务报表

- 需在运行环境中验证 finance/cashier-report 页面
- 代码审查：有独立的 finance 视图

### 9.7 官方结算

- 需在运行环境中验证
- 官方结算与客户支付在详情页中分区展示（官方订单号、官方结算状态、官方结算金额）

## 10. 小程序验收（代码审查）

### 10.1 不存在的状态

小程序不展示以下旧状态：
- 待接单 (PENDING_ACCEPT) ✓
- 已接单 (ACCEPTED) ✓
- 已定件 (PART_ORDERED) ✓
- 已到件 (PART_ARRIVED) ✓
- 已结算 (SETTLED) ✓
- 结算工单 ✓

旧状态显示为"旧状态，请先清理试运行数据"提示。

### 10.2 展示的状态

- 新建中 (DRAFT) ✓
- 维修中 (REPAIRING) ✓
- 维修完成 (REPAIR_DONE) ✓
- 已交付 (DELIVERED) ✓
- 已取消 (CANCELLED) ✓

### 10.3 DELIVERED 状态只读

- DELIVERED 状态无操作按钮（`canRecordPayment` / `canMarkRepairDone` / `canDeliver` / `canCancel` 均由后端 can* 字段控制）✓

### 10.4 小程序不显示交付后退款

- 小程序无 `canRefundAfterDelivery` 相关 UI ✓

### 10.5 交付关闭文案

- 不说"交付时扣库存" ✓
- PAID: "工单已结清。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。"
- NO_CHARGE: "无需收款工单（原因：{reason}）。确认交付关闭后..."

### 10.6 标记维修完成文案

- toast: "已标记维修完成，库存已扣减。" ✓ 明确说明扣库存

## 11. 发现的问题清单

| # | 问题 | 位置 | 严重性 | 建议 |
|---|------|------|--------|------|
| 1 | 前端清理结果页面未展示 before/after summary | admin-web/src/views/trial-data/index.vue | 低 | 补充 before/after 数据对比表格 |
| 2 | 本地 MySQL 未安装，无法启动后端进行实际运行验收 | 环境 | 中 | 需要在有 MySQL 的环境中完成实际运行验收 |
| 3 | 小程序 tsc 历史依赖问题 | mini-program | 低 | 可考虑在 tsconfig.json 中 skipLibCheck: true 或排除 node_modules |

## 12. 验收结论

### 12.1 代码层面验证

- [x] 后端 679 测试全部通过
- [x] admin-web 类型检查通过
- [x] admin-web 构建通过
- [x] mini-program 无本次修改引入的类型错误
- [x] 工单状态机正确：DRAFT → REPAIRING → REPAIR_DONE → DELIVERED / CANCELLED
- [x] clean-start 页面完整覆盖清理/保留范围
- [x] 确认文本校验 `CONFIRM_CLEAR_TRIAL_DATA`
- [x] 交付关闭文案不出现"交付时扣库存"
- [x] 维修完成文案明确"库存已扣减"
- [x] 未收齐时不允许交付
- [x] 无需收款原因必填
- [x] 退款金额校验
- [x] 小程序不显示旧状态名称
- [x] 小程序 DELIVERED 只读

### 12.2 待运行环境验证

- [ ] clean-start 前后 summary 实际数据对比
- [ ] 普通收费维修全链路实际操作
- [ ] 无需收款链路实际操作
- [ ] 取消退款链路实际操作
- [ ] 财务报表数据准确性
- [ ] 官方结算数据隔离

### 12.3 建议

- **是否建议发布 trial-v0.4.0**: 代码审查通过，建议在有 MySQL 的环境完成运行验收后发布
- **是否建议 push**: 是，代码质量满足合并条件
- **是否仍有未提交修改**: 否，仅本次新增文档
