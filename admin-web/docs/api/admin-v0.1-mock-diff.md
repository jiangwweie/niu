# Admin Web v0.1 Mock 字段与后端 API 契约差异表

本文档记录了前端 `admin-web` 在 Mock 阶段 (`src/types` 等) 与后端 Admin API 契约 v0.1 (`docs/api/admin-api-v0.1.md`) 之间的主要差异与对齐点。本文档仅作对接后端真实接口的开发指南和备忘，不代表要求在当前 Mock 阶段把所有的模型都修改为后端结构。

## 1. 全局约定与接口结构响应

### 1.1 统一基础响应结构
- **统一响应**：所有的 mock API 已统一使用 `ApiResponse` 结构：`code` / `message` / `data` / `traceId`。
- **废弃字段**：不再保留 `success` 字段。成功时的 code 已统一对接为 `"SUCCESS"`。

### 1.2 分页结构对齐
- **统一分页**：分页结构已统一使用 `PageResponse` 等效结构：`records` / `pageNo` / `pageSize` / `total`。
- **废弃字段**：不再保留 `list` / `items` / `page` / `totalPages` 等旧结构参数，前端请求已统一改用 `pageNo`，响应取 `records`。

## 2. 身份认证与权限上下文

### 2.1 CurrentUserContext 隐式传参
- `storeId`、`operatorId` 等核心权限与操作标识必须源自后端的 `CurrentUserContext`，绝不能由前端作为参数传入。
- 这个规则在实际对接真实后端时生效，前端需要在请求载荷中移除当前本地模拟的 `storeId` / `operatorId` 字段设定。

## 3. 具体业务模块差异说明

### 3.1 支付记录页面 / 退款记录页面 (Payment / Refund)
- **差异**：前端目前拥有全局独立的“支付记录”与“退款记录”列表页面，目前为全局列表 mock，但 Admin API v0.1 仅定义了相关工单下的查询接口 (`GET /api/admin/work-orders/{workOrderId}/payments` 和 `.../refunds`)。
- **对接建议**：当前全局支付与退款列表**无法直接真实对接**。后续需要后端补充全局级别的分页查询接口，或者前端改为仅在“工单详情”内查看相关的收退款记录。

### 3.2 工单管理页面 (WorkOrder)
- **差异**：前端列表展示和筛选条件当前超出了 `WorkOrderQueryResponse` v0.1 的定义范围。
  - **列表超出的展示字段**：手机号、车架号、是否官方售后、官方订单号。
  - **超出的筛选条件**：创建日期筛选、`officialOnly` 筛选、车架号与手机号查询。
- **对接建议**：在真实对接时，这些超出的字段需要后端扩展查询的 VO / DTO 进行支持；或者前端采取真实对接时降级展示（如列表不展示这些列与查询条件，部分信息只在详情中展现）。

### 3.3 官方售后结算页面 (Official After Sales)
- **差异**：前端列表额外展示了：客户姓名、手机号、车型、车架号、客户实收金额等工单详情关联字段。但 Admin API v0.1 中的 `OfficialAfterSalesQueryResponse` 暂未包含这些信息。
- **对接建议**：真实对接时，需要后端扩展该查询的响应结构；或前端改为在列表缩减字段后，单条通过详情接口聚合展示。

### 3.4 库存管理页面 (Inventory)
- **差异**：前端存有 `source`（配件来源）与 `lowStockOnly`（仅看低于阈值）等筛选条件。但在 Admin API v0.1 当中，这些条件均为预留或服务层未实现状态。
- **对接建议**：当前页面仅为 mock UI。在真实对接前，需要后端补全 DTO 及 Service 层的查询条件支持。

### 3.5 预留模块 (Reimbursement / Finance / Export)
- **差异确认**：这三个模块（报销台账、财务报表、Excel 导出）在后端属于 `Planned` / `Not Implemented`。
- **对接约束**：前端**不得尝试进行真实对接**。当前页面仅用作保持保留 mock UI，无需真实关联直到后续规划落地。

### 3.6 字典配置页面 (Dict)
- **差异**：当前前端是完整字典配置 mock UI；Admin API v0.1 当前只提供 `GET /api/admin/dict/types/{typeCode}/items`。
- **对接约束**：当前没有字典新增、编辑、启停、分页管理接口；真实字典维护需要后端补 Dict 管理接口。

### 3.7 用户与权限页面 (User / Role / Permission)
- **差异**：当前前端有用户、角色、权限点 mock UI；Admin API v0.1 只规划了 `auth/login`、`auth/logout`、`auth/me`，且当前不实现。
- **对接约束**：当前没有用户管理、角色管理、权限点管理、分配角色、启停用户等接口；真实对接前需要后端单独输出 Auth / User / Role / Permission 契约。

### 3.8 配件管理页面 (Part)
- **差异**：Part API v0.1 已比较接近；`categoryCode` 查询为预留，当前 `PartQueryRequest` 未包含；创建配件接口当前未明确 `defaultBarcode`，但编辑接口包含 `defaultBarcode`。
- **对接约束**：前端不得传 `storeId` / `operatorId`。

## 4. 后续对接策略建议（摘录）

1. **接口层面替换为主**：尽量保留目前的组件化视图，修改 `src/api/*.ts` 的函数内代理方式，采用 Axios 与后端通讯。
2. **API adapter 映射处理**：真实业务开始后，前台的 TypeScript 规范要主动迎合后端的 OpenAPI 设计和 Java DTO 产物，以此文档为蓝本进行调整。
