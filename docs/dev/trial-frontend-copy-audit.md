# 前端文案与术语排查报告

## 1. 排查范围

### admin-web
- `admin-web/src/views` — 全部 20 个 Vue 页面
- `admin-web/src/layouts/BasicLayout.vue` — 侧边栏菜单、页头、页脚
- `admin-web/src/router/index.ts` — 路由 meta.title
- `admin-web/src/components` — MoneyText、PageContainer、StatusTag
- `admin-web/src/utils` — request.ts 错误提示
- `admin-web/src/api` — 仅检查错误提示文案

### mini-program
- `mini-program/src/pages` — 全部 11 个页面
- `mini-program/src/components` — 无文件
- `mini-program/src/utils` — request.ts 错误提示、config.ts
- `mini-program/src/api` — 仅检查错误提示文案

## 2. 已修复问题

### 2.1 菜单与页面标题统一（admin-web）

| 位置 | 修复前 | 修复后 |
|------|--------|--------|
| router meta / 侧边栏 | 支付记录 | 收款记录 |
| router meta / 侧边栏 | 官方售后结算 | 官方结算 |
| router meta / 侧边栏 | 字典配置 | 基础配置 |
| router meta / 侧边栏 | 用户与权限 | 员工与权限 |
| router meta | Dashboard 首页 | 工作台 |
| router meta | Excel 导出中心 | 数据导出 |
| 侧边栏页脚 | MVP 试运行版 | 试运行版 |
| 页头版本标 | Admin | 管理端 |
| payment/index.vue | 支付记录 → 收款记录 |
| dictionary/index.vue | 字典配置 → 基础配置 |
| user/index.vue | 用户与权限 → 员工与权限 |
| settlement/index.vue | 官方售后结算 → 官方结算 |
| export/index.vue | Excel 导出中心 → 数据导出 |

### 2.2 英文枚举值 / API 字段名泄露（admin-web）

| 文件 | 修复前 | 修复后 |
|------|--------|--------|
| finance/index.vue | 状态为 SETTLED 的官方结算 | 状态为"已结算"的官方结算 |
| finance/index.vue | SETTLED 工单的配件成本 (line_cost_amount) | 已结算工单的配件成本 |
| finance/index.vue | 状态为 CONFIRMED 的报销 | 状态为"已确认"的报销 |
| dashboard/index.vue | PART_ORDERED → 配件已订 | 已定件 |
| dashboard/index.vue | PART_ARRIVED → 配件已到 | 已到件 |

### 2.3 开发态词汇清理（admin-web）

| 文件 | 修复前 | 修复后 |
|------|--------|--------|
| payment/index.vue alert | 列表已接入真实后端，记录支付属于员工小程序端现场操作。 | 收款操作在员工小程序端完成，此处仅查看记录。 |
| refund/index.vue alert | 列表已接入真实后端，记录退款属于员工小程序端现场操作。 | 退款操作在员工小程序端完成，此处仅查看记录。 |

### 2.4 ID 标签优化（admin-web）

| 文件 | 修复前 | 修复后 |
|------|--------|--------|
| reimbursement/index.vue | 报销人ID ×3 处 | 报销人 |
| reimbursement/index.vue | 确认人ID | 确认人 |
| reimbursement/index.vue | 处理人ID | 处理人 |
| payment/index.vue drawer | 操作人ID | 操作人 |
| payment/index.vue drawer | 收款人ID | 收款人 |

### 2.5 英文枚举值 / 开发态词汇清理（mini-program）

| 文件 | 修复前 | 修复后 |
|------|--------|--------|
| work-order-detail/wxml | `{{order.status}}` 直接显示英文枚举 | `{{statusText}}` 显示中文 |
| work-order-detail/wxml | `workOrderNo \|\| 'DRAFT'` | `workOrderNo \|\| '草稿'` |
| work-order-detail/wxml | 操作结果以系统校验为准。 | 具体以系统校验结果为准。 |
| work-order-detail/wxml | 操作提示：本系统仅记录线下收款结果，不发起真实线上支付。 | 注意：此处仅记录线下收款结果。 |
| work-order-detail/wxml | 操作提示：本系统仅记录线下退款结果，不接真实支付网关。 | 注意：此处仅记录线下退款结果。 |
| work-order-detail/ts | 工单已取消，库存释放以后端结果为准。 | 工单已取消。 |
| work-order-detail/ts | 收款记录已保存。收款不会自动结算工单。 | 收款记录已保存。 |
| create-work-order/wxml | `workOrderNo \|\| 'DRAFT'` ×2 处 | `workOrderNo \|\| '草稿'` |
| create-work-order/wxml | 状态标签内联三元只处理 DRAFT/PENDING_ACCEPT | 改用 statusText 变量 |
| create-work-order/wxml | DRAFT 阶段的配件明细会生成 RESERVE 库存流水... | 提交工单后将预占库存，提交不会收款、不会结算。 |
| create-work-order/wxml | 当前仅创建草稿并维护费用明细。提交工单后才会预占库存，本阶段不会生成库存流水。 | 当前为草稿阶段，可维护费用明细。提交工单后将预占库存。 |
| create-work-order/wxml | 是否允许提交最终以后端校验为准。提交工单后才会预占库存；提交不会收款、不会结算。 | 提交后将预占库存，不会自动收款或结算。 |
| create-work-order/wxml | 取消后是否释放库存以后端状态和库存预占记录为准；前端不直接修改库存。 | 取消后将释放预占库存。 |
| create-work-order/wxml | 是否允许退款以及可退金额上限以后端校验为准。前端只负责登记。 | 可退金额上限以系统校验为准。 |
| create-work-order/wxml | 是否允许结算完全以后端校验为准。结算后将正式扣减库存。 | 结算后将正式扣减库存。 |
| create-work-order/wxml | 操作提示：本系统仅记录线下收款结果，不发起真实线上支付。 | 注意：此处仅记录线下收款结果。 |
| create-work-order/wxml | 操作提示：本系统仅记录线下退款结果，不接真实支付网关。 | 注意：此处仅记录线下退款结果。 |
| create-work-order/ts | 费用明细已添加。DRAFT 阶段不会预占库存。 | 费用明细已添加。 |
| create-work-order/ts | 费用明细已删除。DRAFT 阶段不会生成库存流水。 | 费用明细已删除。 |
| create-work-order/ts | 工单已提交，库存预占以后端结果为准。 | 工单已提交。 |
| create-work-order/ts | 工单已取消，库存释放以后端结果为准。 | 工单已取消。 |
| create-work-order/ts | 收款记录已保存。收款不会自动结算工单。 | 收款记录已保存。 |
| inbound/wxml | 入库成功后由后端生成 INBOUND 库存流水... | 入库成功后将更新库存，库存数量以系统实际记录为准。 |
| inbound/wxml | 流水号 | 入库单号 |
| payment-placeholder/wxml | payment-placeholder page | 收款功能 / 收款操作在工单详情页完成 |
| mine/ts | 门店ID ${user.storeId} | 使用 storeName，不再显示 storeId |
| mine/ts | roleCodes.join(' / ') 回退 | 不再显示原始 roleCodes |
| work-orders/ts | statusText 回退到原始 item.status | 回退到"未知" |
| request.ts | 请求失败(${res.statusCode}) | 请求失败，请稍后重试 |
| request.ts | 网络请求失败，请检查后端服务或开发环境配置 | 网络请求失败，请检查网络连接 |

## 3. 未处理问题

### 3.1 记录但不修改

| 问题 | 文件 | 说明 |
|------|------|------|
| NIU logo 文字 | BasicLayout.vue | 品牌标识，非技术术语，保留 |
| mock 模式调试切换 | mine/wxml | 条件为 `apiMode === 'mock'`，当前为 `real` 模式，不会展示 |
| CONFIRM_CLEAR_TRIAL_DATA | trial-data/index.vue | 高风险操作的安全确认机制，故意使用英文，保留 |
| categoryCode 原始枚举 | mini-program parts/wxml | 显示 BRAKE 等分类编码，建议后续接入字典翻译 |
| 确认人ID / 处理人ID | reimbursement 详情抽屉 | 已改为"确认人"/"处理人"，但值仍为数字 ID，建议后续显示姓名 |
| 支付编号 / 支付金额 / 支付方式 / 支付时间 | payment/index.vue 表格列名 | 已改为"收款记录"页面标题，但表格列名仍使用"支付"，与后端 API 字段对应，暂不修改以避免与 API 不一致 |

### 3.2 建议后续处理

1. **payment/index.vue 表格列名**：页面标题已改为"收款记录"，但表格列仍显示"支付编号"、"支付金额"、"支付方式"、"支付时间"。建议统一为"收款编号"、"收款金额"、"收款方式"、"收款时间"，但需与后端 API 字段名保持一致映射关系。
2. **parts/index.vue categoryCode 显示**：配件分类编码直接显示英文枚举值，建议接入字典配置翻译。
3. **reimbursement 确认人/处理人显示 ID**：当前显示数字 ID 而非姓名，建议后续查询用户姓名。
4. **mini-program 库存页面**：库存列表和库存流水页面检查通过，无明显问题。

## 4. 用户可见英文/API 字段排查结果

| 字段 | 是否用户可见 | 处理 |
|------|-------------|------|
| customerIncome | 仅在 TS 数据绑定中使用，标签已为"客户实收收入" | 无需修改 |
| officialIncome | 仅在 TS 数据绑定中使用，标签已为"官方结算收入" | 无需修改 |
| partsCost | 仅在 TS 数据绑定中使用，标签已为"配件成本" | 无需修改 |
| profit | 仅在 TS 数据绑定中使用，标签已为"净利润" | 无需修改 |
| receivableAmount | TS 数据绑定，标签已为"应收金额" | 无需修改 |
| receivedAmount / actualAmount | TS 数据绑定，标签已为"已收金额" | 无需修改 |
| workOrderId | 仅在 TS 代码中使用 | 无需修改 |
| storeId | mine 页面显示"门店ID"，已修复 | 已修复 |
| userId | 仅在 TS 代码中使用 | 无需修改 |
| roleCodes | mine 页面回退显示，已修复 | 已修复 |
| permissionCodes | 仅在 TS 权限检查中使用 | 无需修改 |
| passwordMustChange | user 页面标签"需改密"，已为中文 | 无需修改 |
| wechatBound | user 页面标签"微信绑定"，已为中文 | 无需修改 |

## 5. 枚举值展示排查结果

| 枚举 | 用户可见位置 | 状态 |
|------|-------------|------|
| DRAFT → 草稿 | work-order 列表、dashboard、work-order-detail | 全部已映射中文 |
| PENDING_ACCEPT → 待接单 | 同上 | 全部已映射中文 |
| ACCEPTED → 已接单 | 同上 | 全部已映射中文 |
| PART_ORDERED → 已定件 | dashboard 修复为一致 | 已修复 |
| PART_ARRIVED → 已到件 | dashboard 修复为一致 | 已修复 |
| SETTLED → 已结算 | 全部页面 | 已映射中文 |
| CANCELLED → 已取消 | 全部页面 | 已映射中文 |
| WECHAT/ALIPAY/UNIONPAY/CASH | payment/refund/work-order 表单 | 全部已映射中文 |
| OFFICIAL/THIRD_PARTY | parts/inventory | 全部已映射中文 |
| ENABLED/DISABLED | platform/store | 已映射中文 |
| PENDING/CONFIRMED/REJECTED | reimbursement | 已映射中文 |

## 6. 开发态词汇排查结果

已清理所有用户可见的开发态词汇：
- "后端"/"前端" — 全部移除或改写
- "支付网关" — 已移除
- "mock 数据" — 当前为 real 模式，mock 区域不会展示
- "placeholder" — payment-placeholder 已改为中文
- "API"/"DTO" — 用户页面未发现
- "token"/"JWT" — 用户页面未发现
- "localStorage" — 用户页面未发现

## 7. 错误码展示排查结果

admin-web 和 mini-program 的 API 错误码（如 FORBIDDEN、UNAUTHORIZED 等）均通过后端返回的 `message` 字段展示中文提示，前端未直接暴露英文错误码。

mini-program request.ts 已修复：
- `请求失败(${res.statusCode})` → `请求失败，请稍后重试`
- `网络请求失败，请检查后端服务或开发环境配置` → `网络请求失败，请检查网络连接`

## 8. 金额格式和空状态检查结果

### 金额格式
- admin-web 统一使用 `<MoneyText>` 组件，格式为 `¥0.00`
- mini-program 使用 `¥{{amount}}` 格式，同一页面内保持一致
- 未发现格式不一致问题

### 空状态文案
- admin-web 使用 `<el-empty>` 组件，已有中文描述
- mini-program 使用 `暂无工单`、`暂无费用明细`、`暂无配件` 等中文空状态文案
- 未发现英文或空白空状态

## 9. 验证命令结果

### admin-web vue-tsc
```
npx vue-tsc --noEmit — 通过，无错误
```

### admin-web build
```
npm run build — 成功，3.35s
```

### mini-program tsc
```
npx tsc --noEmit — 仅有历史类型错误（miniprogram_npm/tdesign-miniprogram 类型定义问题）
本次修改未新增任何类型错误
```

## 10. 后续建议

1. **payment 表格列名统一**：页面标题已改为"收款记录"，建议后续将表格列名从"支付"统一为"收款"。
2. **配件分类字典化**：parts 页面的 categoryCode 枚举值建议接入字典配置翻译为中文。
3. **确认人/处理人显示姓名**：reimbursement 详情中的确认人 ID 建议关联用户姓名显示。
4. **mini-program 页面路径名**：`create-work-order-placeholder`、`inbound-placeholder`、`payment-placeholder`、`reimbursement-placeholder` 等路径名含 "placeholder"，虽不影响用户（用户看不到 URL），但建议后续重命名为正式路径。
5. **mock 模式调试面板**：mine 页面的 mock 模式切换面板当前不可见（API_MODE=real），但代码仍保留。建议正式发布前移除 mock 相关代码。
