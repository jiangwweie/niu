# 小程序工单主闭环 real 模式 Smoke Test 报告

## 环境配置
- **端**: mini-program
- **运行模式**: real

## 1. 本次修复的问题与根因
- **问题现象**：在微信开发者工具（以及体验版）中点击“提交工单”后，控制台抛出 500 错误和 `Failed to fetch dynamically imported module: http://127.0.0.1:xxx/appservice/api/workOrder`，工单提交阻断。
- **根因判断**：
  小程序底层构建器/执行环境不完全支持 ES Module 的运行时 `import()` 语法。在 `create-work-order-placeholder/index.ts` 中，5 个核心业务方法（submit、cancel、payment、refund、settle）使用了 `import('../../api/workOrder').then(...)` 懒加载，导致该段代码被微信容器错误地识别为 HTTP 网络请求。
- **修复方案**：
  将上述 5 处动态 import 语法彻底剥离，改为在文件顶部的静态 `import { ... }`，随后直接调用 `submitWorkOrder(this.data.workOrderId!, ...)`，问题彻底解决。

## 2. 闭环链路支持情况
1. **创建/编辑草稿**：由 `create-work-order-placeholder` 提供。
2. **提交工单**：动态 import 修复后已可用，走 `POST /api/staff/work-orders/{id}/submit`。
3. **记录支付**：已在工单详情页 `work-order-detail` 补齐对应 Action Bar 的 `openPaymentPopup` 事件和 `t-popup` 弹窗 UI。记录支付仅在 `PENDING_ACCEPT`、`ACCEPTED`、`PART_ORDERED`、`PART_ARRIVED` 状态显示；`DRAFT` 工单需要先提交，点击“确认记录”后调用 `POST /api/staff/work-orders/{id}/payments`。支付不会自动结算工单。
4. **结算工单**：已在详情页补齐 `t-dialog` 确认弹窗 UI。结算入口同样仅在 `PENDING_ACCEPT`、`ACCEPTED`、`PART_ORDERED`、`PART_ARRIVED` 状态显示，不在 `DRAFT` 状态显示；点击确认后调用 `POST /api/staff/work-orders/{id}/settle`。
5. **状态刷新**：所有上述接口请求成功（`then` 块）后，均触发 `this.fetchData()` 刷新当前页的渲染详情，其中 `receivedAmount` 和 `status` 等将得到立竿见影的刷新。

## 3. 权限与状态控制边界
- 所有新增入详情页的 UI 交互，依然受严格的 `<t-button wx:if="{{hasPaymentPermission && ...}}">` 形式的权限码控制。
- **记录支付**：仅对 `PENDING_ACCEPT`、`ACCEPTED`、`PART_ORDERED`、`PART_ARRIVED` 状态显示。`DRAFT`、`CANCELLED`、`SETTLED` 状态不显示记录支付入口。
- **结算工单**：仅对 `PENDING_ACCEPT`、`ACCEPTED`、`PART_ORDERED`、`PART_ARRIVED` 状态显示；实收金额是否足够仍由后端判断。
- **记录退款**：仅受 `REFUND_RECORD` 权限控制，不根据工单状态隐藏。SETTLED 后仍可记录退款（仅记录财务影响，不反结算、不回滚库存、不改变工单状态）；CANCELLED 后也可记录退款（用于定金退款等场景）。最终是否可退、退款金额上限由后端判断。
- 不存在任何前端自设条件去判断库存该不该预占/该不该扣减。后端接口如果不满足实收条件，直接从 request wrapper 中打出现有 toast 错误阻挡流转。
- Request wrapper 已增强非 2xx 错误提示：400 等业务错误优先展示后端 `message`，例如“当前工单状态不允许记录支付”；401 清 token 并跳登录、403 Toast 无权限行为不变。auth / config 均原样保留，未做任何更改。
- `work-order-detail/index.json` 已注册当前详情页使用的 `t-button`、`t-input`、`t-popup`、`t-dialog`、`t-toast`、`t-icon` 等 TDesign 组件；页面保留 `<t-toast id="t-toast" />`，Toast selector 统一为 `#t-toast`。

## 4. 手工验证用例 (Smoke)
1. 登录员工测试号。
2. 进入“工单”，创建并提交一份全新的草稿，确保不再爆红/提示模块无法拉取。
3. 进入 `DRAFT` 工单详情，默认不显示“记录支付”和“结算工单”；`DRAFT` 工单需要先提交。
4. 提交工单后进入后端允许支付状态，再进入详情页应显示“记录支付”和“结算工单”。点击“记录支付”，底部 `t-popup` 正常弹出。不填金额点击确认时，Toast 正常提示且不报 selector 错误。
5. 录入 100 元现金并确认。接口成功后弹窗关闭，上方“实收金额”实时刷新为 `¥100`。
6. 点击“结算工单”，`t-dialog` 确认弹窗正常显示。若实收不足，后端返回 400，前端 Toast 展示后端 `message`；若费用达标，系统正常扣减库存并在页面标记“SETTLED”。
7. 结算后不再显示“记录支付”；有 `REFUND_RECORD` 权限时仍显示“记录退款”。后退返回列表时，通过 `onShow` 的生命周期拉取最新列表，同步刷新卡片状态。

## 5. 类型与编译结果
- 仓库根目录执行 `npx tsc --noEmit` 会命中 npx 的 TypeScript 占位提示，因为根目录没有 `package.json`。
- 在 `mini-program` 目录执行 `npx tsc --noEmit` 报告 38 个错误，集中在 tdesign/miniprogram 类型定义、缺失三方类型和既有 Toast/Dialog import 类型解析问题，本次未新增业务代码类型错误。

## 6. 遗留与排除说明
- M17 范围内无阻塞遗留；扫码、临时配件、车架号历史、微信/手机号登录等属于后续增强。
- `mini-program/src/utils/config.ts` 和 `docs/dev/local-startup-guide.md` 不属于本次提交，不应 stage。
- 401/403 行为不变：request wrapper 拦截 401 跳登录、403 Toast 无权限。
