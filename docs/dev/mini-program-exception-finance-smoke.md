# 小程序异常与财务入口 Smoke Test 报告

## 环境配置
- **端**: mini-program
- **运行模式**: real

## 1. 异常边界：取消工单
- **入口**：在 `work-order-detail` 页底栏常驻（受 `WORK_ORDER_CANCEL` 和非终态控制）。
- **逻辑**：弹窗输入必须填写的“取消原因”，调用 `POST /api/staff/work-orders/{id}/cancel`。
- **业务验证**：
  - 前端**绝对不会**去干预“库存该释放还是保留”。后端来决定 DRAFT 直接标记取消、已提交则释放预占库存，产生 RELEASE 记录。
  - 对于已结算(SETTLED)的单据，前端 UI 已经配置 `order.status !== 'SETTLED'` 防止操作，若恶意绕过，后端接口同样会进行状态防御拦截。

## 2. 财务辅助：记录退款
- **入口**：在 `work-order-detail` 页底栏常驻（仅受 `REFUND_RECORD` 权限控制，不根据工单状态隐藏）。
- **逻辑**：底部“记录退款”按钮只打开 `t-popup` 底弹出框，确认按钮要求输入退款金额与退款方式后调用 `POST /api/staff/work-orders/{id}/refunds`。
- **业务验证**：
  - 前端不发起微信支付商户回调。
  - 前端**不执行**反结算或回滚库存操作。
  - 记录提交后，仅刷新拉取详情，后端会自动减少 `receivedAmount`（表现为扣除退款后）。
  - 退款表单默认不直接显示在工单详情正文中；校验失败统一使用页面内 `<t-toast id="t-toast" />`。
  - 不存在”覆盖或删除支付记录”的行为。
  - **SETTLED 后允许记录退款**：仅记录财务影响，不反结算、不回滚库存、不改变工单状态。
  - **CANCELLED 后也允许记录退款**：用于定金退款等场景。
  - 退款金额上限、是否可退、状态是否合法，最终由后端判断，前端不做核心判断。

## 3. 财务入口：提交报销
- **入口**：已随之前的 UI 修改放进了 `mine` 页面（我的模块）的快捷操作，受 `REIMBURSEMENT_SUBMIT` 约束。
- **逻辑**：引向了 `reimbursement-placeholder` 页，其内部现已正确接入了 `submitReimbursement` 方法，指向 `POST /api/staff/reimbursements`。
- **业务验证**：
  - 小程序仅作为报销表单登记触点。
  - 前端不干预后续是否被主管同意或计算入成本等逻辑。后端默认将收拢到的申请置为“待确认”状态。

## 4. 全链路控制与底层坚守
- 涉及的三大新增业务流：**不碰** Request Wrapper 登录拦截、**不碰** Config、**不碰** Auth 逻辑。完全通过已存在于 `mini-program/src/api` 下的网络层 API 发送数据，对后端的 JSON 校验结构高度适配。

## 5. 手工验证用例 (Smoke)
1. 选取一个**未结算**但有实收/应收的单据，点击“取消”，验证详情页变更为 `CANCELLED`。
2. 选取一个**未结算**单据，记录支付后，点击“记录退款”，底部 `t-popup` 正常弹出。不填金额或原因点击确认时，Toast 正常提示且不报 selector 错误。
3. 填写退款金额、退款方式和原因后确认退款。退款完成后，工单并不反向结算，而是“已收金额”下降，同时支付记录依旧能够在 admin-web 中可查可对。
4. 在“我的”页面，点击“提交报销”，填入金额和报销用途，点击确认。提示成功后返回。去管理端(admin-web)应当可以看见刚才提交的 PENDING 报销单。

## 6. 类型与编译结果
- 仓库根目录执行 `npx tsc --noEmit` 会命中 npx 的 TypeScript 占位提示，因为根目录没有 `package.json`。
- 在 `mini-program` 目录执行 `npx tsc --noEmit` 报告 38 个错误，集中在 tdesign/miniprogram 类型定义、缺失三方类型和既有 Toast/Dialog import 类型解析问题，本次未新增业务代码类型错误。

## 7. 遗留与排除说明
- M17 范围内无阻塞遗留；扫码、临时配件、车架号历史、微信/手机号登录等属于后续增强。
- `mini-program/src/utils/config.ts` 和 `docs/dev/local-startup-guide.md` 不属于本次提交，不应 stage。
- 401/403 行为不变：request wrapper 拦截 401 跳登录、403 Toast 无权限。
