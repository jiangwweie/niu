# M27A 搜索能力盘点与模糊搜索范围定义

## 1. 客户反馈背景

客户反馈系统搜索条件不好用，希望尽可能支持模糊搜索。
当前搜索能力范围不清晰，不能直接大改代码。
本阶段仅做审计和方案，不改代码、不提交、不部署、不 push。

审计日期：2026-06-08

---

## 2. 小程序搜索入口清单

### 2.1 工单列表搜索

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/work-orders/index.ts` / `.wxml` |
| 搜索对象 | 工单（work orders） |
| 提示文案 | `搜索工单号 / 客户名 / 手机号 / 车架号` |
| 前端参数 | `keyword`（非空时发送）、`status`、`pageNo`、`pageSize=20` |
| 后端接口 | `GET /api/staff/work-orders` |
| 后端匹配 | LIKE %keyword%，跨 5 字段 + official_after_sales.official_order_no |
| 模糊搜索 | 支持（LIKE %keyword%，含工单号/客户名/手机号/车架号/车型） |
| 手机后4位 | 支持（后端 LIKE '%keyword%'） |
| 车架号后6位 | 支持（后端 LIKE '%keyword%'） |
| 编码/条码 | 不适用（工单搜索） |
| 触发方式 | 提交触发（Enter/搜索按钮），非实时输入触发 |
| 分页交互 | 搜索/切换状态均重置 pageNo=1，keyword 和 status 不互相清除 |
| 分页实现 | 无限滚动加载（loadMore） |

### 2.2 创建工单 — 客户搜索

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/create-work-order-placeholder/index.ts` / `.wxml` |
| 搜索对象 | 客户（customers） |
| 提示文案 | `姓名或手机号` |
| 前端参数 | `keyword` |
| 后端接口 | `GET /api/staff/customers/search` |
| 后端匹配 | LIKE %keyword%，跨 customerName + phone |
| 模糊搜索 | 支持 |
| 手机后4位 | 支持 |
| 车架号后6位 | 不适用 |
| 编码/条码 | 不适用 |
| 触发方式 | 300ms 防抖自动触发 + 手动搜索按钮 |
| 分页 | 无分页，一次返回全部结果 |

### 2.3 创建工单 — 车辆搜索

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/create-work-order-placeholder/index.ts` / `.wxml` |
| 搜索对象 | 车辆（vehicles） |
| 提示文案 | `车架号/车型/客户手机号` |
| 前端参数 | `keyword`、`customerId`（可选，已选客户时传递） |
| 后端接口 | `GET /api/staff/vehicles/search` |
| 后端匹配 | LIKE %keyword%，跨 frameNo + model + batteryNo + 客户phone（两次查询合并） |
| 模糊搜索 | 支持 |
| 手机后4位 | 支持（通过客户手机号子查询） |
| 车架号后6位 | 支持 |
| 编码/条码 | 不适用 |
| 触发方式 | 300ms 防抖自动触发 + 手动搜索按钮 |
| 分页 | 无分页 |
| 特殊交互 | 选客户后，车辆搜索自动限定该客户车辆 |

### 2.4 创建工单 — 配件搜索（弹窗内本地筛选）

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/create-work-order-placeholder/index.ts` / `.wxml` |
| 搜索对象 | 配件（parts） |
| 提示文案 | `搜索配件编码/名称` |
| 前端参数 | 无 API 调用，纯客户端 includes() 筛选 |
| 后端接口 | 无（数据在页面加载时通过 `getParts()` 一次性获取） |
| 匹配字段 | `partName` + `partCode`（toLowerCase + includes） |
| 模糊搜索 | 支持（子串匹配） |
| 手机后4位 | 不适用 |
| 车架号后6位 | 不适用 |
| 编码/条码 | **部分支持** — 搜 partCode 但不搜 officialPartNo 和 defaultBarcode |
| 触发方式 | 每次输入立即触发，无防抖 |
| 分页 | 无 |

### 2.5 入库页 — 配件搜索（弹窗内本地筛选）

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/inbound-placeholder/index.ts` / `.wxml` |
| 搜索对象 | 配件（parts） |
| 提示文案 | `搜索配件编码/名称` |
| 前端参数 | 无 API 调用，纯客户端 includes() 筛选 |
| 匹配字段 | `partName` + `partCode` |
| 模糊搜索 | 支持（子串匹配） |
| 编码/条码 | **部分支持** — 搜 partCode 但不搜 officialPartNo 和 defaultBarcode |
| 触发方式 | 每次输入立即触发，无防抖 |
| 另有扫码 | `wx.scanCode()` + `lookupPartByCode()` 精确匹配条码 |

### 2.6 库存查询搜索

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/inventory/index.ts` / `.wxml` |
| 搜索对象 | 库存（inventory stocks） |
| 提示文案 | `搜索配件编码 / 名称` |
| 前端参数 | `keyword`（始终发送，含空字符串） |
| 后端接口 | `GET /api/staff/inventory/stocks` |
| 后端匹配 | 两阶段：先查 part 表（partCode 精确 + partName LIKE），再查 inventory_stock |
| 模糊搜索 | partName 支持，partCode 为精确匹配 |
| 编码/条码 | 不搜 officialPartNo，不搜条码 |
| 触发方式 | 300ms 防抖自动触发 |
| 分页 | **无前端分页**，一次性返回全部 |

### 2.7 配件列表搜索

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/pages/parts/index.ts` / `.wxml` |
| 搜索对象 | 配件（parts） |
| 提示文案 | `搜索配件编码 / 名称` |
| 前端参数 | `keyword`（始终发送） |
| 后端接口 | `GET /api/staff/parts` |
| 后端匹配 | partCode 精确 + partName LIKE %keyword%（各自独立过滤，AND 关系） |
| 模糊搜索 | partName 支持，partCode 为精确匹配 |
| 编码/条码 | 不搜 officialPartNo 和条码（officialPartNo 是独立筛选字段，需精确匹配） |
| 触发方式 | 300ms 防抖自动触发 |
| 分页 | **无前端分页** |

### 2.8 条码/编码精确查找

| 项目 | 详情 |
|------|------|
| 文件 | `mini-program/src/api/parts.ts` + 创建工单页/入库页 |
| 搜索对象 | 配件（by barcode/code） |
| 前端参数 | `code`（扫码结果） |
| 后端接口 | `GET /api/staff/parts/lookup` |
| 后端匹配 | **精确匹配**，依次查：part_barcode.barcode → part.part_code → part.official_part_no → part.default_barcode |
| 模糊搜索 | 不支持（精确匹配） |

### 2.9 不具备搜索的页面

| 页面 | 说明 |
|------|------|
| 报销页 | 仅提交表单（用途+金额+备注），无列表、无搜索 |
| 收款页 | 无独立列表，在工单详情内记录 |

---

## 3. 管理端搜索入口清单

### 3.1 工单管理

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/work-order/index.vue` |
| 搜索字段 | 工单号(`workOrderNo`，精确)、客户姓名(`customerName`，LIKE)、手机号(`customerPhone`，LIKE)、车架号(`vehicleFrameNo`，LIKE) |
| 筛选字段 | 状态(`status`，精确)、是否官方(`officialOnly`)、日期范围(`startTime`/`endTime`) |
| 后端接口 | `GET /api/admin/work-orders` |
| 后端匹配 | keyword 模式：LIKE %keyword% 跨 5 字段 + official_order_no；独立字段：混合精确/模糊 |
| 分页交互 | 正确：搜索重置 pageNo=1，换页保留搜索条件 |
| **BUG** | `cashierStatus` 筛选器在 UI 中有 7 个选项，但 API 映射函数从未传递该参数，筛选完全无效 |

### 3.2 客户管理

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/customer/index.vue` |
| 搜索字段 | 客户姓名(`customerName`)、手机号(`phone`) |
| 提示文案 | 均为 `请输入`（过于模糊） |
| 后端接口 | `GET /api/admin/customers` |
| 后端匹配 | customerName LIKE，phone 精确 |
| 分页交互 | **BUG** — `@current-change` 绑定的是 `handleSearch` 而非 `fetchData`，导致翻页时 pageNo 被重置为 1，用户永远无法翻到第 2 页 |

### 3.3 车辆档案

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/vehicle/index.vue` |
| 搜索字段 | 车架号(`vin`)、车型(`model`)、客户手机号(`customerPhone`) |
| 提示文案 | 均为 `请输入` |
| 后端接口 | `GET /api/admin/vehicles` |
| 后端匹配 | LIKE %keyword%，跨 frameNo + model + batteryNo + 客户phone |
| 分页交互 | **BUG** — 同客户管理，翻页永远停在第 1 页 |

### 3.4 配件管理

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/parts/index.vue` |
| 搜索字段 | 配件编码(`partCode`，精确)、配件名称(`partName`，LIKE)、官方品号(`officialPartNo`，精确)、型号(`model`，LIKE)、分类(`categoryCode`，精确)、来源(`source`，精确)、状态(`enabled`，精确) |
| 后端接口 | `GET /api/admin/parts` |
| 后端匹配 | 混合：partCode/officialPartNo/categoryCode/source 精确；partName/model 模糊 |
| 注意 | 条码(`barcode`)在表格中显示但**无搜索入口** |
| 分页交互 | 正确 |

### 3.5 库存管理

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/inventory/index.vue` |
| 搜索字段 | 配件编码(`partCode`，精确)、配件名称(`partName`，LIKE)、来源(`source`，精确)、视图类型(`view`，精确) |
| 后端接口 | `GET /api/admin/inventory/stocks` |
| 后端匹配 | 两阶段：先查 part，再查 inventory_stock |
| 注意 | 入库弹窗有远程搜索配件功能，同时发 3 个请求（按 partName/partCode/officialCode），合并结果 — 这是管理端唯一的"万能搜索" |
| 分页交互 | 正确 |

### 3.6 收款记录

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/payment/index.vue` |
| 搜索字段 | 工单号(`workOrderNo`，LIKE)、客户姓名(`customerName`，LIKE)、支付方式(`paymentMethod`，精确)、日期范围 |
| 后端接口 | `GET /api/admin/payments` |
| 注意 | 无手机号搜索、无车架号搜索 |
| 分页交互 | 正确 |

### 3.7 退款记录

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/refund/index.vue` |
| 搜索字段 | 工单号(`workOrderNo`)、客户姓名(`customerName`)、退款方式(`refundMethod`)、日期范围 |
| 后端接口 | `GET /api/admin/refunds` |
| 分页交互 | 正确 |

### 3.8 报销台账

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/reimbursement/index.vue` |
| 搜索字段 | 报销编号(`reimbursementNo`，LIKE)、报销人ID(`applicantId`，精确数字)、状态(`status`，精确)、日期范围(`dateFrom`/`dateTo`) |
| 后端接口 | `GET /api/admin/reimbursements` |
| 注意 | 日期参数命名不一致：用 `dateFrom`/`dateTo` 而非其他页面的 `startTime`/`endTime` |
| 分页交互 | 正确 |

### 3.9 官方结算

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/settlement/index.vue` |
| 搜索字段 | 工单号(`workOrderNo`，LIKE)、官方订单号(`officialOrderNo`，LIKE)、结算状态(`settlementStatus`，精确)、日期范围 |
| 后端接口 | `GET /api/admin/official-after-sales` |
| 分页交互 | 正确 |

### 3.10 员工与权限

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/user/index.vue` |
| 搜索字段 | 员工账号(`username`，LIKE)、姓名(`realName`，LIKE)、角色(`roleCode`，精确)、状态(`enabled`，精确) |
| 后端接口 | `GET /api/admin/users` |
| 注意 | 手机号在表格中显示但无搜索入口 |
| 分页交互 | **BUG** — 切换 pageSize 时不重置 pageNo，可能导致页面越界 |

### 3.11 财务报表 / 收银日报

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/finance/index.vue` / `cashier-report.vue` |
| 搜索方式 | 日期选择（日报/月报/自定义区间），无文本搜索、无分页 |

### 3.12 基础配置（字典）

| 项目 | 详情 |
|------|------|
| 文件 | `admin-web/src/views/dictionary/index.vue` |
| 搜索方式 | 纯客户端 includes() 筛选，无 API 调用 |

---

## 4. 当前搜索能力矩阵

| 搜索能力 | 工单列表 | 客户搜索 | 车辆搜索 | 配件搜索(本地) | 库存查询 | 配件列表 | 管理端工单 | 管理端客户 | 管理端车辆 | 管理端配件 | 管理端库存 | 收款 | 报销 | 官方结算 |
|----------|---------|---------|---------|-------------|---------|---------|----------|----------|----------|----------|----------|------|------|---------|
| 模糊搜索 | Y | Y | Y | Y(本地) | 部分 | 部分 | Y | 部分 | Y | 部分 | 部分 | Y | Y | Y |
| 手机后4位 | Y | Y | Y | - | - | - | Y | ? | Y | - | - | - | - | - |
| 车架后6位 | Y | - | Y | - | - | - | Y | - | Y | - | - | - | - | - |
| 配件编码 | - | - | - | Y(本地) | 精确 | 精确 | - | - | - | 精确 | 精确 | - | - | - |
| 官方品号 | - | - | - | **缺失** | **缺失** | **缺失** | - | - | - | 精确 | **缺失** | - | - | Y(订单号) |
| 条码 | - | - | - | **缺失** | **缺失** | **缺失** | - | - | - | **缺失** | **缺失** | - | - | - |

说明：Y=支持，部分=名称支持模糊但编码精确，精确=后端精确匹配，缺失=字段存在但搜索未覆盖，-=不适用

---

## 5. 当前不支持模糊搜索的地方

| 编号 | 位置 | 问题 | 影响 |
|------|------|------|------|
| F1 | 小程序配件本地筛选 | 不搜 `officialPartNo` | 用户输入官方品号无法找到配件 |
| F2 | 小程序配件本地筛选 | 不搜 `defaultBarcode` | 用户输入条码无法找到配件 |
| F3 | 小程序/管理端配件列表 | `partCode` 使用精确匹配(eq) | 输入部分编码无法找到配件 |
| F4 | 小程序/管理端配件列表 | `officialPartNo` 使用精确匹配(eq) | 输入部分官方品号无法找到配件 |
| F5 | 管理端库存列表 | `partCode` 使用精确匹配(eq) | 输入部分编码无法查找库存 |
| F6 | 管理端库存列表 | 无 officialPartNo 搜索入口 | 无法按官方品号查库存 |
| F7 | 管理端配件管理 | `barcode` 在表格中显示但无搜索框 | 无法按条码搜索配件 |
| F8 | 小程序配件本地筛选 | 无防抖，每次按键触发全量过滤 | 配件量大时性能风险 |

---

## 6. 当前字段不一致的地方

| 编号 | 问题 | 位置 |
|------|------|------|
| I1 | 管理端客户搜索提示文案仅为 `请输入`，未说明可搜姓名/手机号 | `admin-web/src/views/customer/index.vue` |
| I2 | 管理端车辆搜索提示文案仅为 `请输入`，未说明可搜车架号/车型/手机号 | `admin-web/src/views/vehicle/index.vue` |
| I3 | 报销页日期参数用 `dateFrom`/`dateTo`，其他页面用 `startTime`/`endTime` | `admin-web/src/api/reimbursement.ts` |
| I4 | 管理端入库弹窗搜索配件发 3 个独立请求（按名称/编码/官方品号），而列表页只有 2 个字段 | `admin-web/src/views/inventory/index.vue` |
| I5 | 小程序工单列表搜索是提交触发，库存/配件列表是实时输入触发，行为不一致 | 各页面 TS |

---

## 7. 搜索条件与分页/状态筛选互相影响的问题

| 编号 | 页面 | 问题 | 严重程度 |
|------|------|------|---------|
| P1 | 管理端客户管理 | `@current-change` 绑定 `handleSearch` 而非 `fetchData`，翻页永远回到第1页 | **高** |
| P2 | 管理端车辆档案 | 同上，翻页永远回到第1页 | **高** |
| P3 | 管理端工单管理 | `cashierStatus` 筛选器 UI 存在但 API 不传递，筛选完全无效 | **中** |
| P4 | 管理端员工管理 | 切换 pageSize 不重置 pageNo，可能导致空页 | **低** |
| P5 | 小程序库存/配件列表 | 无分页，数据量大时可能一次返回过多数据 | **低**（当前数据量小） |

---

## 8. 建议的统一搜索规则

### 8.1 前端统一规则

1. **所有文本搜索统一使用 `keyword` 参数**，前端不拆分为多个独立字段发送（除非业务必须精确筛选的场景如来源/状态）
2. **搜索触发统一**：列表页统一为提交触发（Enter/搜索按钮），弹窗内搜索统一为 300ms 防抖实时触发
3. **提示文案统一**：每个搜索框必须明确告知用户可搜索的字段，如 `搜索配件编码/名称/官方品号`
4. **分页统一**：所有列表页使用 `el-pagination`，搜索/筛选重置 pageNo=1，翻页保留所有条件

### 8.2 后端统一规则

1. **keyword 字段统一使用 LIKE %keyword%**（包含匹配），当前已是此模式
2. **配件相关搜索统一覆盖**：partCode、partName、officialPartNo、defaultBarcode（当前缺失后两个）
3. **LIKE 通配符转义**：对 keyword 中的 `%` 和 `_` 前加 `\` 转义，防止恶意注入
4. **输入 trim 统一**：所有 Controller 层对 keyword 统一 `.trim()`
5. **不加全文索引**：当前数据量下 LIKE %keyword% 性能可接受，暂不引入 FULLTEXT

---

## 9. 建议修复批次

### M27B-1：创建工单搜索增强（小程序，高优先级）

**范围**：
- 创建工单页配件本地筛选增加 `officialPartNo` 和 `defaultBarcode` 字段匹配
- 添加 300ms 防抖

**涉及文件**：
- `mini-program/src/pages/create-work-order-placeholder/index.ts`

**是否需要 backend**：否（纯前端本地过滤修改）

**是否涉及数据库**：否

**风险**：低，仅修改前端 includes 条件

**验证方式**：在创建工单页添加配件弹窗中输入官方品号，验证能搜索到

### M27B-2：工单列表搜索/筛选增强

**范围**：
- 修复管理端客户管理分页 BUG（P1）
- 修复管理端车辆档案分页 BUG（P2）
- 修复管理端 cashierStatus 筛选无效 BUG（P3）
- 管理端客户/车辆搜索框提示文案补充（I1、I2）

**涉及文件**：
- `admin-web/src/views/customer/index.vue`
- `admin-web/src/views/vehicle/index.vue`
- `admin-web/src/views/work-order/index.vue`
- `admin-web/src/api/workOrder.ts`

**是否需要 backend**：cashierStatus 需要确认后端是否支持该筛选参数

**是否涉及数据库**：否

**风险**：低，仅修改前端事件绑定和 API 参数映射

**验证方式**：
- 客户管理翻到第 2 页，验证不跳回第 1 页
- 车辆档案翻到第 2 页，验证不跳回第 1 页
- 工单管理选择 cashierStatus 筛选，验证结果有变化

### M27B-3：配件/库存搜索增强

**范围**：
- 小程序入库页配件本地筛选增加 officialPartNo、defaultBarcode（F1、F2）
- 小程序配件/入库页本地筛选添加防抖（F8）
- 后端配件列表 keyword 统一搜索（覆盖 partCode、partName、officialPartNo、defaultBarcode）（F3、F4）
- 后端库存列表 keyword 统一搜索（F5、F6）
- 管理端配件管理增加条码搜索入口（F7）

**涉及文件**：
- `mini-program/src/pages/inbound-placeholder/index.ts`
- `backend/.../part/service/impl/PartServiceImpl.java`
- `backend/.../inventory/service/impl/InventoryServiceImpl.java`
- `admin-web/src/views/parts/index.vue`

**是否需要 backend**：是（配件列表和库存列表的 keyword 搜索需要后端扩展）

**是否涉及数据库**：否（修改 LIKE 条件，不改表结构）

**风险**：中，后端修改影响面较大，需回归测试配件/库存相关功能

**验证方式**：
- 小程序配件列表输入部分官方品号，验证能搜到
- 管理端配件管理按条码搜索，验证能搜到
- 管理端库存管理输入配件名称，验证能搜到

### M27B-4：管理端搜索统一增强

**范围**：
- 统一所有搜索框提示文案（I1、I2）
- 统一日期参数命名（I3：报销页 dateFrom/dateTo 改为 startTime/endTime，或反过来统一）
- 修复员工管理分页 pageSize 切换问题（P4）
- LIKE 通配符转义（后端安全加固）
- 输入 trim 统一

**涉及文件**：
- 多个管理端 vue 文件
- 多个后端 Service 文件
- `admin-web/src/api/reimbursement.ts`

**是否需要 backend**：是（LIKE 转义和 trim 统一需要后端修改）

**是否涉及数据库**：否

**风险**：低，均为非功能性改进

**验证方式**：各页面搜索输入含 `%` 或 `_` 的内容，验证不返回异常结果

---

## 10. 是否需要 backend 支持

| 批次 | 需要 backend | 说明 |
|------|-------------|------|
| M27B-1 | 否 | 纯前端本地过滤修改 |
| M27B-2 | 部分 | cashierStatus 需确认后端支持，其余纯前端 |
| M27B-3 | **是** | 配件列表和库存列表 keyword 搜索扩展 |
| M27B-4 | **是** | LIKE 转义、trim 统一 |

---

## 11. 每个建议是否涉及数据库

| 批次 | 涉及数据库 | 说明 |
|------|-----------|------|
| M27B-1 | 否 | |
| M27B-2 | 否 | |
| M27B-3 | 否 | 仅修改查询条件，不改表结构 |
| M27B-4 | 否 | |

所有建议均不涉及数据库表结构变更。

---

## 12. 风险和验证方式

### 12.1 安全边界确认

- [x] 未改代码
- [x] 未改数据库
- [x] 未连接生产数据库写数据
- [x] 未删除历史数据
- [x] 未改库存/工单状态机/支付/退款/结算/财务核心规则
- [x] 未 push
- [x] 未输出 secret

### 12.2 后端发现的安全隐患

| 风险 | 说明 | 建议 |
|------|------|------|
| LIKE 通配符注入 | 用户输入 `%` 或 `_` 会被当作 SQL 通配符，`%` 可返回全表数据 | 后端统一转义 `%` → `\%`，`_` → `\_` |
| 输入 trim 不一致 | 部分接口 trim，部分不 trim，空格可能导致搜索不到 | 统一在 Controller 层 trim |
| 全表扫描 | LIKE '%keyword%' 无法使用 B-tree 索引，store 维度下数据量小时可接受 | 当前无需优化，数据量增长后考虑 FULLTEXT |

### 12.3 建议验证场景

| 场景 | 预期 |
|------|------|
| 小程序工单列表输入手机号后4位 | 能搜到相关工单 |
| 小程序工单列表输入车架号后6位 | 能搜到相关工单 |
| 小程序创建工单客户搜索输入手机号后4位 | 能搜到对应客户 |
| 小程序创建工单车辆搜索输入车架号后6位 | 能搜到对应车辆 |
| 小程序创建工单配件搜索输入官方品号 | 能搜到对应配件 |
| 管理端配件管理按条码搜索 | 能搜到对应配件 |
| 管理端工单管理翻到第2页 | 结果正确，不跳回第1页 |
| 管理端客户管理翻到第2页 | 结果正确，不跳回第1页 |
| 管理端车辆档案翻到第2页 | 结果正确，不跳回第1页 |

---

## 最终输出

1. **是否改代码**：否
2. **搜索入口数量**：小程序 8 个 + 管理端 12 个 = 共 20 个搜索入口
3. **小程序高优先级问题**：配件本地筛选不搜 officialPartNo/defaultBarcode（M27B-1）
4. **管理端高优先级问题**：客户/车辆管理分页 BUG（永远停在第1页）（M27B-2）
5. **是否需要 backend**：M27B-3 和 M27B-4 需要后端支持
6. **建议优先修复顺序**：M27B-2（BUG 修复）→ M27B-1（用户痛点）→ M27B-3（搜索增强）→ M27B-4（统一规范）
7. **文档路径**：`docs/dev/m27a-search-capability-audit.md`
8. **git status**：见下方
9. **确认未 push**：是
