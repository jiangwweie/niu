# M26C-1 客户体验问题排查与修复报告：小程序工单搜索与状态筛选失效热修复

## 1. 客户反馈问题
小程序体验版上线后，客户在使用中反馈了以下体验风险问题（均为 P0 级别核心链路阻断）：
1. **工单列表“全部/状态筛选”失效**：工单列表页选择“全部”或状态筛选条件时，看不到其他状态的工单，筛选完全失效。
2. **客户搜索失效**：在创建工单页面，输入关键字搜索已有客户没有反应，无法正常输入或匹配数据。
3. **车辆搜索失效**：在创建工单页面，搜索已有车辆也没有反应，无法匹配。

## 2. 问题原因分析

### 2.1 工单列表“全部/状态筛选”失效原因
在工单列表页 `work-orders/index.ts` 中，调用 `getWorkOrders` 接口时传递了 `status: this.data.activeStatus || undefined`。
当用户切换到“全部”标签时，`activeStatus` 的值为 `''`，因此计算得出传递给后端的参数值为 `undefined`。
微信小程序原生的 `wx.request` 在处理 `GET` 请求时，会将值为 `undefined` 的键值对直接转换为字符串并拼接到 URL 后面，导致发出的请求为 `?status=undefined`。
后端的 `WorkOrderServiceImpl` 中 `StringUtils.hasText("undefined")` 判断为 `true`，继而在数据库中执行了 `WHERE status = 'undefined'` 的查询，查不到任何有效数据并返回了空列表，造成用户认为“全部及筛选”失效的错觉。

### 2.2 创建工单客户搜索失效原因
第一层阻断在于交互逻辑冲突：在 `api/customer.ts` 的 `searchCustomers` 请求配置中开启了 `showLoading: true`。小程序页面上的 `<t-search>` 搜索框通过 `debounce` (300ms) 监听输入。当客户敲下第一个字符，300ms 后系统调用查询接口并唤起 `wx.showLoading({ mask: true })`，该全局遮罩会瞬间抢夺屏幕焦点并强制收起输入法软键盘，造成客户无法继续完整输入名字，形成“无法搜索/没有反应”的严重体验问题。
第二层阻断在于数据结构映射错位：排除了软键盘阻断后，虽然能拿到客户列表数据，但因为后端 `StaffCustomerController` 提供的字段名为 `name`，而小程序 TypeScript 定义的 `CustomerSearchResult` 接口及 WXML 模板中错误绑定了 `customerName`（`item.customerName` 为 `undefined`），导致搜索列表呈现出的是大面积的空白名字条目。

### 2.3 创建工单车辆搜索失效原因
与客户搜索的交互逻辑冲突一致：`searchVehicles` 的接口请求中同样启用了 `showLoading: true`，防抖自动查询时弹出全局 loading 抢占焦点，导致收起键盘从而中断了用户的车辆匹配输入。

## 3. 修复方式

- **修改 `mini-program/src/pages/work-orders/index.ts`**：废弃了 `status: this.data.activeStatus || undefined` 的传递方式，改为动态条件判断（只有当 `keyword` 或 `activeStatus` 有真值时，才把对应属性附加进 `params` 对象中，避免因 `undefined` 引发微信框架层的误处理）。
- **修改 `mini-program/src/api/customer.ts`**：
  - 去除 `searchCustomers` 和 `searchVehicles` 配置中的 `showLoading: true`，实现静默拉取搜索结果，确保用户输入连贯。
  - 将 `CustomerSearchResult` 中的属性 `customerName` 修正为与后端匹配的 `name`。
- **修改 `mini-program/src/pages/create-work-order-placeholder/index.ts` 和 `index.wxml`**：
  - 更新 WXML 以使用 `{{item.name}}` 进行展示。
  - 更新 TS `selectCustomer` 方法使其读取 `customer.name`。

## 4. 边界约束检查

- **未修改 backend**：仅排查了后端的 Spring Boot Controller 和 MyBatis 参数结构，没有做任何代码更改。
- **未修改数据库**：未产生任何 DDL 或 DML。
- **未删除历史数据**：严格保留原有体验版所有客户和系统基础数据。
- **未修改核心业务规则**：没有触碰库存、工单状态机、支付、退款、结算、财务等底层核心链路逻辑。

## 5. 小程序体验版验证步骤
1. **验收工单列表**：
   - 登录系统进入底部“工单” Tab。
   - 分别点击顶部导航“全部”、“新建中”、“维修中”等标签页，观察工单列表是否能展示对应的数据（而不是一直处于空状态或错误匹配）。
2. **验收新建工单客户搜索**：
   - 点击“新建工单”。
   - 点击“搜索已有客户”，在弹出的搜索框中输入“测试”等文字，验证软键盘是否还会意外关闭，客户列表项是否能正常展示客户的真实名字并能顺畅回填。
3. **验收新建工单车辆搜索**：
   - 点击“新建工单”。
   - 点击“搜索车辆”，验证连续输入关键字查询车架号过程顺滑无阻。

## 6. 建议体验版版本号
**v0.2.1-M26C.1 (Hotfix)**
