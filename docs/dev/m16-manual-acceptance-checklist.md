# M16 手工试运行验收清单

> 最后更新：2026-05-16

## 一、验收目标

当前系统已进入 MVP 可人工试运行版。本清单用于 Owner 在浏览器（admin-web）和微信开发者工具（mini-program）中手工点验，确认主链路、权限控制、退款策略等核心功能符合预期。

M16B 真实 smoke 已通过，主链路已跑通：
入库 → 创建工单 → 添加费用 → 提交工单 → 支付 → 结算 → 报销 → 官方结算 → 财务报表 → Excel 导出

退款授权 smoke 已通过：
- trial_staff 无 REFUND_RECORD，退款返回 403
- trial_store_admin 有 REFUND_RECORD，退款成功
- receivedAmount = paymentTotal - refundTotal
- 管理端退款记录可查
- 财务报表 customerIncome 正确体现退款后净收入

当前退款策略（方案 B）：普通员工默认无退款权限，退款由门店管理员 / 财务 / 特定授权账号处理。

## 二、试运行账号

| 账号 | 密码 | 角色 | 用途 | 入口 |
|---|---|---|---|---|
| system_admin | Trial@2026! | SUPER_ADMIN | 系统超管 / 软件商管理员 | admin-web |
| trial_store_admin | Trial@2026! | STORE_ADMIN | 门店管理员，可验证退款 | admin-web / mini-program |
| trial_finance | Trial@2026! | FINANCE | 财务报表 / 导出验证 | admin-web |
| trial_staff | Trial@2026! | TECHNICIAN_FRONT_DESK | 员工 / 前台，无退款权限 | mini-program |

注意事项：
- 密码仅用于试运行，首次正式使用后应修改密码
- system_admin 是软件商 / 系统拥有者超管，不是普通门店员工
- 当前 system_admin 绑定 store_id=1 是 MVP 单门店技术兼容
- SQL 文件：`docs/dev/m16-trial-initial-users.sql`（手动执行，不在 Flyway 中）

## 三、admin-web 验收清单

| # | 验收项 | 操作说明 | 预期结果 | 实际结果 | 通过 |
|---|---|---|---|---|---|
| 1 | 登录页可访问 | 浏览器打开 http://localhost:3000 | 自动跳转 /login，显示登录表单 | | |
| 2 | system_admin 登录 | 输入 system_admin / Trial@2026! | 登录成功，跳转首页 | | |
| 3 | trial_store_admin 登录 | 退出后用 trial_store_admin 登录 | 登录成功，跳转首页 | | |
| 4 | trial_finance 登录 | 退出后用 trial_finance 登录 | 登录成功，跳转首页 | | |
| 5 | 顶部用户信息 | 登录后查看顶部栏 | 显示真实姓名、角色、门店名 | | |
| 6 | 首页无 mock 假数据 | 查看 Dashboard 页面 | 无硬编码假数据，为轻量入口 | | |
| 7 | 用户与权限页 | 点击用户与权限菜单 | 不展示 mock 假表，页面结构正常 | | |
| 8 | Excel 导出中心 | 点击导出中心菜单 | 不展示 mock 假任务 | | |
| 9 | 菜单按权限显隐 | 用 trial_finance 登录 | 仅显示 FINANCE 角色可见的菜单 | | |
| 10 | 工单列表/详情 | 点击工单管理 | 列表可加载，详情可查看 | | |
| 11 | 配件管理 | 点击配件管理 | 列表可加载 | | |
| 12 | 库存管理 | 点击库存管理 | 列表可加载 | | |
| 13 | 官方结算 | 点击官方售后结算 | 可录入 / 标记结算 | | |
| 14 | 报销台账 | 点击报销台账 | 可确认 / 驳回报销 | | |
| 15 | 财务报表 | 点击财务报表 | 日报 / 月报 / 区间报可查看 | | |
| 16 | Excel 导出 | 在财务或报销页点击导出 | 可下载 xlsx 文件 | | |
| 17 | 无权限操作 403 | 用 trial_finance 尝试配件写操作 | 返回 403 提示，不跳登录页 | | |
| 18 | 退出登录 | 点击退出 | token 清除，返回 /login | | |

## 四、小程序验收清单

| # | 验收项 | 操作说明 | 预期结果 | 实际结果 | 通过 |
|---|---|---|---|---|---|
| 1 | 登录页可访问 | 微信开发者工具打开 mini-program | 显示登录表单 | | |
| 2 | trial_staff 登录 | 输入 trial_staff / Trial@2026! | 登录成功，跳转工作台 | | |
| 3 | 工作台显示正常 | 查看工作台页面 | 显示正常，无异常 | | |
| 4 | 无 Mock 字样 | 检查各页面 | 无 "Mock" / "mock" / "假数据" 字样 | | |
| 5 | 新建工单入口明显 | 查看工作台或工单列表 | 新建按钮位置合理，易于发现 | | |
| 6 | 入库入口明显 | 查看库存页 | 入库按钮位置合理，易于发现 | | |
| 7 | 库存查询可用 | 进入库存 tab | 可查看库存列表 | | |
| 8 | 配件入库可用 | 点击入库 | 可提交入库记录 | | |
| 9 | 创建 DRAFT 工单可用 | 点击新建工单 | 可创建草稿工单 | | |
| 10 | 添加费用可用 | 在工单中添加费用 | PART / LABOR / OTHER 三种类型可用 | | |
| 11 | 提交工单可用 | 点击提交 | 工单状态变为 SUBMITTED | | |
| 12 | 记录支付可用 | 点击记录支付 | 可录入支付记录 | | |
| 13 | 结算工单可用 | 点击结算 | 工单状态变为 SETTLED | | |
| 14 | 提交报销可用 | 点击提交报销 | 可提交报销记录 | | |
| 15 | trial_staff 无退款权限 | trial_staff 尝试退款 | 无退款按钮，或退款返回 403 | | |
| 16 | trial_store_admin 可退款 | 用 trial_store_admin 登录小程序 | 可执行退款操作 | | |
| 17 | 401 清 token 跳登录 | token 过期后操作 | 清 token，跳转登录页 | | |
| 18 | 403 toast 提示 | 无权限操作 | toast 提示无权限，不清 token | | |
| 19 | 退出登录可用 | 点击退出 | 清 token，返回登录页 | | |

## 五、完整业务闭环验收

按顺序执行以下步骤，验证主链路端到端可用：

| # | 步骤 | 操作位置 | 预期结果 | 实际结果 | 通过 |
|---|---|---|---|---|---|
| 1 | 小程序登录 trial_staff | mini-program | 登录成功 | | |
| 2 | 查询库存 | mini-program 库存 tab | 显示库存列表 | | |
| 3 | 入库一个配件 | mini-program 入库 | 入库成功，库存增加 | | |
| 4 | 创建 DRAFT 工单 | mini-program 新建工单 | 工单创建成功 | | |
| 5 | 添加配件费用、人工费、其他费用 | mini-program 工单详情 | 三种费用均可添加 | | |
| 6 | 提交工单 | mini-program | 工单状态变为 SUBMITTED | | |
| 7 | 查看库存 reserved 增加 | mini-program 库存 | 预占库存增加 | | |
| 8 | 记录支付 | mini-program | 支付记录创建成功 | | |
| 9 | 结算工单 | mini-program | 工单状态变为 SETTLED | | |
| 10 | 查看库存 actual 扣减、reserved 归零 | mini-program 库存 | 实际库存扣减，预占归零 | | |
| 11 | 提交报销 | mini-program | 报销提交成功 | | |
| 12 | 管理端登录 trial_store_admin 或 system_admin | admin-web | 登录成功 | | |
| 13 | 查看工单 | admin-web 工单管理 | 可查看刚才的工单 | | |
| 14 | 查看库存 | admin-web 库存管理 | 库存数据与小程序一致 | | |
| 15 | 录入官方结算 | admin-web 官方售后结算 | 结算录入成功 | | |
| 16 | 确认报销 | admin-web 报销台账 | 报销状态变为已确认 | | |
| 17 | 查看财务报表 | admin-web 财务报表 | 数据正确反映业务 | | |
| 18 | 导出 Excel | admin-web 导出按钮 | xlsx 文件可下载 | | |

## 六、退款权限验收

当前策略：普通员工默认无退款权限，退款由门店管理员 / 财务 / 特定授权账号处理。

| # | 步骤 | 操作位置 | 预期结果 | 实际结果 | 通过 |
|---|---|---|---|---|---|
| 1 | trial_store_admin 登录小程序 | mini-program | 登录成功 | | |
| 2 | 创建或选择一个工单 | mini-program | 工单可用 | | |
| 3 | 记录支付 50 | mini-program | 支付记录创建 | | |
| 4 | 记录退款 7 | mini-program | 退款记录创建 | | |
| 5 | 确认 receivedAmount = 43 | mini-program / admin-web | receivedAmount = 50 - 7 = 43 | | |
| 6 | 管理端查看退款记录 | admin-web 退款记录 | 可查到退款 7 的记录 | | |
| 7 | 财务报表 customerIncome 增加 43 | admin-web 财务报表 | customerIncome 正确反映净收入 | | |
| 8 | trial_staff 尝试退款 | mini-program | 返回 403 或退款按钮隐藏 | | |

## 七、已知非阻塞问题

| # | 问题 | 影响 | 优先级 |
|---|---|---|---|
| 1 | Dashboard 真实统计接口未做，当前首页为轻量入口 | 不影响业务操作 | P3 |
| 2 | 用户与权限管理后台未开放 | 无法通过 UI 管理账号，需 SQL | P2 |
| 3 | Excel 导出中心未开放，但财务/报销等页面已有真实导出按钮 | 导出功能可用，入口分散 | P3 |
| 4 | 小程序工作台统计不是完整经营看板 | 信息量有限 | P3 |
| 5 | WORK_ORDER_SUBMIT 临时控制新建工单入口，后续建议补 WORK_ORDER_CREATE / DRAFT 权限语义 | 权限粒度不够细 | P2 |
| 6 | admin-web / mini-program 仍有部分历史类型或 mock 文件 | 不影响 real 模式试运行 | P3 |
| 7 | 暂无完整多门店 / SaaS 平台账号体系 | 当前单门店 MVP 可用 | P2 |
| 8 | system_admin 当前绑定 store_id=1 是 MVP 技术兼容 | 多门店时需解耦 | P2 |

## 八、验收记录区

| 验收日期 | 验收人 | 模块 | 结果 | 问题描述 | 优先级 | 备注 |
|---|---|---|---|---|---|---|
| | | | 通过 / 不通过 / 待修 | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |