# 小牛电动官方授权店两轮车维修售后库存管理系统 - 微信小程序端

## 1. 技术栈与阶段目标
- **阶段**：Phase M2 (Staff API 只读页面实现)
- **技术栈**：原生微信小程序 + TypeScript + TDesign Miniprogram
- **目标**：完成 Staff 相关的核心查询只读页面（配件查询、库存查询、工单查询、工单详情）。
- **重要边界**：
  - 只实现只读页面，不实现任何真实写操作（如：不实现工单提交/取消/结算，不实现入库提交，不实现支付/退款/报销提交等）。
  - 不展示成本价及与成本相关的任何敏感字段。
  - 不实现真实微信登录，不实现 JWT，不实现真实权限拦截。
  - 不修改后端代码。

## 2. API 层与 Mock / Real 模式说明
- **API 方法位置**：所有请求定义在 `src/api/` 目录下（`dict.ts`, `parts.ts`, `inventory.ts`, `workOrder.ts`）。
- **路径规范**：所有 API 请求路径均对齐后端的 `GET /api/staff/**` 规范。
- **运行模式**：
  - 当前默认配置为 `API_MODE = 'mock'`。所有的页面都将通过 `src/mock/` 目录下的对应数据渲染。
  - `real` 模式仅作预留，M2 阶段不发起到真实后端的网络请求。如果切换至 `real` 模式，请求会自动携带相关的 mock headers 供后续本地联调使用。

## 3. 页面功能说明
本次交付 4 个核心只读页面，采用大按钮、卡片化布局，适合员工现场操作：
1. **配件查询 (`pages/parts`)**：展示配件编码、配件名称、来源标签（官方/第三方）、官方品号、型号与分类。
2. **库存查询 (`pages/inventory`)**：展示配件实际库存、可用库存（高亮强调）、预占库存及最近变更时间。
3. **工单查询 (`pages/work-orders`)**：展示工单号、状态标签、客户姓名、车型、应收实收金额。点击卡片可进入详情页。
4. **工单详情 (`pages/work-order-detail`)**：顶部展示工单状态和客户信息，中部列出车架号电池号等基础信息，底部按类别（配件、工时、其他）列出费用明细，最后提供金额摘要。

**安全保证**：所有类型定义、Mock 数据和前端页面中，均已严格排除且不展示 `referenceCostPrice`（参考成本价）、`costPriceSnapshot`（成本快照）、`lineCostAmount`（行成本金额）等敏感成本字段。也没有强行展示现阶段不需要的官方售后单号等字段。

## 4. TDesign 运行说明
1. 确保已经在目录内执行 `npm install`。
2. 打开微信开发者工具并导入本项目。
3. 务必点击菜单栏 **工具 -> 构建 npm** 确保 TDesign 组件生效。
4. 编译预览页面，您可以体验各页面的 TDesign 搜索框、卡片单元格等设计组件。

## 5. 修改文件与资源清单 (Phase M2)
- `src/types/parts.ts` / `inventory.ts` / `workOrder.ts`：更新模型属性，剔除成本字段。
- `src/mock/parts.ts` / `inventory.ts` / `workOrder.ts`：更新 Mock 响应数据，对齐 Staff API 出参结构。
- `src/api/dict.ts` / `parts.ts` / `inventory.ts` / `workOrder.ts`：对接路由 `/api/staff/**`。
- `src/pages/parts/*`：完成页面开发。
- `src/pages/inventory/*`：完成页面开发。
- `src/pages/work-orders/*`：完成页面开发。
- `src/pages/work-order-detail/*`：完成页面开发。

## 6. Staff 只读接口联调说明
1. **默认模式**：当前项目的 API 请求默认处于 `API_MODE = 'mock'`，所有数据基于本地 mock 文件返回。
2. **切换模式**：如需联调真实后端，请打开 `src/utils/config.ts`，将 `API_MODE` 修改为 `'real'`。
3. **BASE_URL 配置**：
   - 在 `config.ts` 中配置 `BASE_URL`。本地可填 `http://localhost:8080`。
   - **注意**：真机无法访问电脑的 localhost。真机联调必须配置为局域网 IP (例如 `http://192.168.1.100:8080`)，或者使用内网穿透与 HTTPS 域名。
4. **微信开发者工具设置**：
   - 本地调试 HTTP 服务时，请在工具右上角 **详情 -> 本地设置** 中勾选 **“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”**。
5. **dev-only Header 的用途**：
   - 联调时无需真实登录。切换为 `real` 模式后，请求会自动带上在“我的”页面所选 mock 用户的 `X-User-Id` 和 `X-Store-Id` Header 供后端识别。此方案仅用于联调。
6. **当前联调边界**：
- 当前 **仅支持只读接口联调**，及 **配件入库写接口**。
   - **不支持**其它任何写操作（禁止工单创建、提交、工单结算、支付或报销提交）。
   - **不支持**微信真实登录、JWT、以及真实的权限拦截体系。

## 7. 配件入库与写操作 (Phase M3)
- M3 阶段已全面接入后端的 `/api/staff/inventory/inbound` 真实写接口，在 `pages/inbound-placeholder/index` (展示为“配件入库”) 内实现。
- 采用 TDesign Popup 的 `Select` 进行安全选取，防误录非法商品，扫码功能现仅为 Mock 占位提示。
- 其他任何更改库存（如工单扣减、盘点）、支付和账单生成的写操作目前均处于 **锁定/屏蔽状态**。

## 8. 工单草稿与明细 (Phase M4B)
- M4B 阶段已接入后端的工单 `DRAFT` 草稿创建和费用明细 (`chargeItems`) 维护接口。
- 支持通过“添加配件费/工时费/其他费用”维护不同的费用明细，实时计算应收金额。
- 当前阶段 **绝对不** 支持“提交工单”、“结算”、“支付”与“退款”。
- 当前处于 DRAFT 的费用明细 **绝对不** 会触发库存预占或产生库存流水。
