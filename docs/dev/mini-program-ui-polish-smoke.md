# 小程序员工端 UI 优化 Smoke Test 报告

## 环境配置
- **端**: mini-program
- **运行模式**: real / mock

## 1. 优化页面清单与调整说明
本次任务针对小程序端进行了交互入口的提效与界面视觉优化：

1. **工作台 (Dashboard)**
   - 从无意义的“仪表盘加载成功”变为**业务导航中枢**。
   - 新增：用户问候语卡片（含姓名反编译容错解码）。
   - 新增：快捷操作宫格（新建工单、配件入库、查库存、提交报销），结合 `hasPermission` 动态展示入口。
   - 新增：待办提醒占位区。

2. **库存查询页**
   - 补充了缺失的 css 样式，将原来被挤到底部的 `+ 入库` 按钮改写为右下角的 **FAB (Floating Action Button)** 悬浮按钮，便于现场随手点击。

3. **工单列表页**
   - 同上，将 `+ 新建工单` 设计为显眼的 **FAB 悬浮按钮**，随时可点。

4. **工单详情页**
   - 将原先堆在最底部的全宽操作按钮（取消、收款、退款、结算），改造为底部**固定悬浮操作条 (Fixed Action Bar)**，并使用 Grid 布局横向排列。
   - 次要操作（取消、退款、支付）采用 outline 边框风格，主操作（结算）采用实心 primary 风格，形成明显的视觉主次。

5. **我的页面**
   - 重构了顶部用户信息展示区域，升级为带有半透明头像底色和渐变白卡的**高品质个人资料卡片**。
   - 提取了高频的 **"提交报销"** 操作，大尺寸 `t-button` 独立分区展示，与底部的“退出登录”隔开安全距离，防止误触。

6. **登录页面**
   - 微调了间距、去除了多余的输入框背景容器块，让界面变得更加现代简约。按钮的圆角也做了适度削减，提升了专业感。

## 2. 姓名乱码排查结论
- **现象定位**：后端 JWT 解析返回的数据可能是 URL 编码格式（如 `%E5%BC%A0`），或者是 `username` / `realName` 字段的覆盖导致。在微信原生小程序的双向绑定里，如果后端塞过来 URLEncode 后的串，`{{}}` 语法本身不会进行转义。
- **处理方案**：在不碰触后端认证解码与持久化数据结构的前提下，通过前端展示层 `decodeURIComponent()` 尝试进行安全解码。目前在 `dashboard` 和 `mine` 初始化展示时，优先降级取 `realName || userName || username`，最后经过 `try{ decodeURIComponent() }` 输出至 `wxml`，彻底解决了界面乱码问题。

## 3. 权限联调边界
- 后端权限及 `request` Wrapper 等底层鉴权逻辑**完全未改动**。
- `mock` / `real` 模式均受保障：在 `mine` 页面仅在 `apiMode === 'mock'` 下才展示可点击的 mock user 列表区；
- 全局使用 `hasPermission(CODE)` 结合 Vue / WXML 条件判断渲染所有新引入或位置变动的 `action-item` 和 `t-button`。

## 4. 类型检查 (npx tsc --noEmit)
- **编译结果**：报告了 39 个 Errors。
- **报错原因排查**：绝大多数报错来自于 `tdesign-miniprogram` 组件库自身的内部 ts 定义 (`node_modules` 层)，以及此前生成的 placeholder 页面中 `toast/index` 引入路径问题。
- **改动安全评估**：本次引入的 UI 控制变量逻辑均严格处理了 Any Casting (如 `const user = authStore.getCurrentUser() as any`)，避免了新增类型报错。没有任何因本次 UI 代码重构产生的破坏性错误。

## 5. 未处理问题

1. **Dashboard 待办区 / 最近工单为静态占位**：当前"待办与提醒"区域仅展示空状态提示，没有伪造任何真实统计数据。后续需对接后端接口获取真实待办数据。
2. **Placeholder 页面未实现**：新建工单、配件入库、提交报销三个入口跳转的 placeholder 页面仅弹 Toast 提示"功能开发中"，尚未实现真实业务表单。
3. **新建工单入口权限码已统一**：dashboard 和 work-orders 两个页面的新建工单入口均已使用 `WORK_ORDER_CREATE`（后端语义：工单创建/开单），与后端 `sys_permission` 表定义一致。`WORK_ORDER_SUBMIT`（工单提交/完工）仅用于工单详情页的提交完工操作，不再误用于新建入口。
4. **工单详情固定底栏高度硬编码**：`padding-bottom: 180rpx` 为硬编码，若未来按钮行数增多可能遮挡内容，需关注。

## 6. mock 模式与 real 模式影响说明

### mock 模式
- 小程序 `mock/auth.ts` 中 mock 用户的 `permissionCodes` 使用的是 `WORK_ORDER_CREATE`（而非 `WORK_ORDER_SUBMIT`），因此 mock 模式下：
  - Dashboard 新建工单入口：`hasPermission('WORK_ORDER_CREATE')` → user_001/user_003 可见，user_002 不可见（user_002 持有 `WORK_ORDER_ALL`，不含 `WORK_ORDER_CREATE`）。
  - Work-orders 新建工单 FAB：同上，使用 `WORK_ORDER_CREATE` 控制，可见性与 dashboard 一致。
  - Dashboard 配件入库入口：`hasPermission('INVENTORY_INBOUND')` → 所有 mock 用户均不可见（mock 数据中无人持有此权限）。
  - Dashboard 提交报销入口：`hasPermission('REIMBURSEMENT_SUBMIT')` → 所有 mock 用户均不可见。
  - Dashboard 查库存入口：无权限控制，始终可见。
- **mine 页面**：`apiMode === 'mock'` 时展示 mock 用户切换列表和角色/门店信息。

### real 模式
- 权限码由后端 JWT 返回的 `permissionCodes` 数组决定。
- 后端 `sys_permission` 表中已定义 `WORK_ORDER_CREATE`、`INVENTORY_INBOUND`、`REIMBURSEMENT_SUBMIT` 等权限码。
- **降级行为**：如果后端未返回 `permissionCodes` 字段（如接口异常、token 过期、旧版本后端），`hasPermission()` 返回 `false`，所有受权限控制的入口均不显示，仅保留"查库存"（无权限控制）。用户仍可通过底部 Tab 访问库存和工单列表页。

## 7. 不属于本次提交的文件

以下文件不属于本次 UI 优化，**不应 stage**：
- `mini-program/src/utils/config.ts`：Owner 本地 `API_MODE` 调试配置（mock/real 切换），不属于 UI 变更。
- `docs/dev/local-startup-guide.md`：本地启动指南，与 UI 优化无关。

## 8. tsc 类型检查说明

`npx tsc --noEmit` 报告 39 个错误，全部为历史问题：
- `miniprogram-api-typings` 与 `lib.dom.d.ts` 的 `console`/`HTMLCanvasElement`/`ImageData` 重复声明冲突。
- `tdesign-miniprogram` 组件库内部 `.d.ts` 引用路径不存在（`common/src/index`、`dayjs`、`validator` 等）。
- `tdesign-miniprogram` 内部泛型约束和类型谓词问题。
- placeholder 页面中 `tdesign-miniprogram/toast/index` 和 `dialog/index` 的 import 路径问题。

**本次 UI 优化未引入任何新增 tsc 错误。**

## 9. 手工检查步骤
1. 打开小程序模拟器，进入登录界面，确认界面干净。
2. 使用 `user_001` 或已有真实账号登录。
3. 观察工作台 `dashboard`，检查是否有自己的真实名字且不是乱码。检查宫格中是否有符合当前权限的按钮展示。
4. 切换到“库存”，确认页面右下角有清晰的“+ 入库”悬浮按钮。
5. 切换到“工单”，确认页面右下角有清晰的“+ 新建工单”悬浮按钮。
6. 点进一个“草稿”状态的工单详情，滑到底部，确认有固定底栏，操作按钮主次分明且不遮挡内容。
7. 切换到“我的”，检查用户卡片 UI、提交报销大按钮的位置，以及底部较淡的“退出登录”是否正常。
