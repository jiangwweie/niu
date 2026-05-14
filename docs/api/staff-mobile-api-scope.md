# Staff/Mobile API Scope 设计

本文档为“小牛电动官方授权店两轮车维修售后库存管理系统”后续员工小程序端提供 API 边界设计。

当前任务仅做 scope 设计：不写 Controller，不新增接口实现，不修改 Service，不修改数据库，不实现认证。

## 1. 总体定位

staff/mobile 小程序端定位为员工、前台、维修员的现场操作入口，面向高频轻量操作：

- 创建、编辑、提交、取消、结算工单
- 添加配件费、工时费、其他费用
- 查询配件和库存
- 记录线下客户支付和退款
- 临时新增配件并进入库存体系
- 提交个人报销
- 查看自己相关工单

设计原则：

- 轻量、高频、少字段，使用独立 staff DTO。
- 不做复杂管理，不替代 admin-web。
- 不做最终业务判断，业务规则由后端 Service 控制。
- 所有库存、工单状态、支付退款、结算规则仍由后端 Service / Application Service 统一校验。

明确边界：

- 小程序不是客户端小程序，不面向车主。
- 小程序不直接操作数据库。
- 小程序不绕过后端状态机。
- 小程序不能自行判断库存是否可扣、工单是否可结算、退款是否可退。

## 2. API 路径建议

推荐 staff/mobile API 统一前缀：

```text
/api/staff/**
```

不推荐将正式前缀定为 `/api/mobile/**`。

理由：

- 当前小程序面向员工，不是 C 端客户。
- `staff` 表达业务角色和权限边界，比 `mobile` 更准确。
- `mobile` 只是端形态，不应成为后端权限边界。
- 未来如果存在 H5、Pad 或其他现场端，也可以继续复用 `/api/staff/**`。

## 3. 认证边界

当前后端开发阶段使用 dev-only Header：

```text
X-User-Id: 1
X-Store-Id: 1
```

约束：

1. 该 Header 只是开发阶段方案，仅用于本地联调和接口验证。
2. staff/mobile API 设计阶段可以继续沿用 dev header 作为临时联调用途。
3. 正式实现前需要单独完成 Auth / JWT / 微信登录 / 手机号绑定方案。
4. 本任务不实现认证，不定义最终登录流程。
5. 不要把 dev-only Header 当正式认证，也不要把前端传入的 userId / storeId 当可信来源。

后续正式实现时，Controller 应从后端认证上下文读取：

- `operatorId`
- `storeId`
- `roleIds`
- `permissionCodes`

## 4. Staff/Mobile API 模块划分

建议 staff API 拆分为以下模块：

| 模块 | 职责 | 实现状态建议 |
| --- | --- | --- |
| Staff WorkOrder API | 工单列表、详情、草稿创建/更新、提交、取消、结算 | 后续新增 staff Controller，复用 WorkOrderService |
| Staff ChargeItem API | 工单费用项新增、修改、删除 | 可并入 StaffWorkOrderController 或单独 Controller |
| Staff Inventory API | 库存查询、库存流水查询、普通入库 | 后续新增 staff Controller，复用 InventoryService |
| Staff Payment API | 查看和记录工单客户支付 | 后续新增 staff Controller，复用 Payment / WorkOrder 相关 Service |
| Staff Refund API | 查看和记录工单客户退款 | 后续新增 staff Controller，复用 Refund / WorkOrder 相关 Service |
| Staff Reimbursement API | 员工提交报销、查看我的报销 | 当前只设计，ReimbursementService 仍为 stub |
| Staff Dict/Part Read API | 字典项、配件查询、配件详情 | 复用 admin 查询 Service，返回更轻量 |
| Staff Me/Profile API | 当前员工信息、权限摘要 | 先占位，不实现 |

## 5. WorkOrder 小程序 API 设计

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/work-orders` | 查询当前员工可见工单列表 |
| GET | `/api/staff/work-orders/{id}` | 查询工单详情 |
| POST | `/api/staff/work-orders/drafts` | 创建草稿工单 |
| PUT | `/api/staff/work-orders/{id}/draft` | 更新草稿工单基础信息 |
| POST | `/api/staff/work-orders/{id}/charge-items` | 添加费用项 |
| PUT | `/api/staff/work-orders/{id}/charge-items/{chargeItemId}` | 更新费用项 |
| DELETE | `/api/staff/work-orders/{id}/charge-items/{chargeItemId}` | 删除费用项 |
| POST | `/api/staff/work-orders/{id}/submit` | 提交工单，触发库存 RESERVE |
| POST | `/api/staff/work-orders/{id}/cancel` | 取消工单，触发库存 RELEASE |
| POST | `/api/staff/work-orders/{id}/settle` | 完成结算，触发库存 CONSUME |

设计说明：

- 这些 endpoint 应复用现有 WorkOrderService / Application Service。
- staff Controller 只负责参数适配、权限校验和轻量 DTO 转换。
- 不允许 staff API 直接修改 `status`。
- `submit` / `cancel` / `settle` 必须走后端 action 方法。
- DRAFT 阶段新增、修改、删除 charge item 不触发库存流水。
- submit 后才预占库存，库存流水类型为 `RESERVE`。
- cancel 必须释放预占库存，库存流水类型为 `RELEASE`。
- settle 后才正式扣减库存，库存流水类型为 `CONSUME`。

列表查询建议默认只返回当前员工相关或当前门店可见范围内的轻量字段，例如：

- 工单 ID / 工单号
- 状态
- 客户姓名、手机号快照
- 车型、车架号快照
- 应收金额、实收金额
- 创建时间、更新时间
- 是否官方售后

## 6. ChargeItem 规则

小程序费用项类型：

| 类型 | 说明 | partId | 是否影响库存 |
| --- | --- | --- | --- |
| `PART` | 配件费 | 必须关联 | `inventoryAffecting = true` |
| `LABOR` | 工时费 | 不关联 | `false` |
| `OTHER` | 其他费用 | 不关联 | `false` |

规则：

- `PART` 必须关联有效 `partId`。
- `PART` 销售价由现场手动填写，不由前端自行改配件主数据售价。
- `LABOR` / `OTHER` 不关联库存，不生成库存流水。
- 应收金额以后端按费用项汇总为准，前端展示只能作为结果呈现。
- 已提交后修改费用项暂不作为默认能力；如后续需要，应单独设计 `adjust-charge-items` 或补差价/补库存流水方案。

## 7. 临时配件流程

现场可能遇到配件尚未录入主数据但需要立即用于工单的场景。

### 7.1 可选方案 A：复用三步接口

```text
POST /api/staff/parts
POST /api/staff/inventory/inbound
POST /api/staff/work-orders/{id}/charge-items
```

优点：

- 每一步边界清晰。
- 复用 Part、Inventory、WorkOrder 现有 Service。
- 失败点容易定位。

缺点：

- 小程序端流程较长。
- 现场操作需要处理三次请求和中间状态。

### 7.2 可选方案 B：新增 staff 组合接口

```text
POST /api/staff/work-orders/{id}/temporary-part-charge-item
```

组合接口后端内部依次完成：

1. 创建临时配件主数据。
2. 按本次使用数量入库。
3. 生成 `INBOUND` 库存流水。
4. 将该配件作为 `PART` 费用项加入草稿工单。

优点：

- 小程序交互更简单，适合现场高频录入。
- 可由后端事务包住完整流程，减少中间不一致。

缺点：

- 是新增组合能力，需要明确事务边界、幂等策略和错误返回。
- 容易被误用成绕过配件管理的入口，需要权限和字段约束。

### 7.3 推荐

当前阶段推荐先文档预留方案 B，不立即实现组合接口。首批实现可以先走方案 A 的能力组合，待小程序真实交互确认后再评估是否增加组合接口。

无论采用哪种方案，必须满足：

- 临时配件必须进入配件主数据。
- 必须进入库存体系。
- 必须生成 `INBOUND` 库存流水。
- 临时入库数量默认等于本次使用数量。
- 提交工单时仍走 `RESERVE`。
- 结算时仍走 `CONSUME`。
- 不允许只写备注或只作为 `OTHER` 费用绕过库存。

## 8. Inventory 小程序 API 设计

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/inventory/stocks` | 查询库存列表，可按配件名称、编码、分类过滤 |
| GET | `/api/staff/inventory/stocks/{partId}` | 查询单个配件库存 |
| GET | `/api/staff/inventory/flows?partId=xxx` | 查询配件库存流水 |
| POST | `/api/staff/inventory/inbound` | 普通入库 |

边界：

- 小程序可查询库存。
- 小程序可做普通入库，是否开放由权限决定。
- 库存调整 `ADJUST` 应优先留给 admin-web 管理端，不作为 staff/mobile 默认能力。
- 不做复杂盘点。
- 不做门店调拨。
- 不允许小程序直接写库存余量，所有变更必须通过库存流水。

## 9. Payment 小程序 API 设计

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/work-orders/{id}/payments` | 查询当前工单支付记录 |
| GET | `/api/staff/work-orders/{id}/payment-summary` | 查询当前工单支付汇总 |
| POST | `/api/staff/work-orders/{id}/payments` | 记录客户线下支付 |

边界：

- 小程序记录的是线下支付结果。
- 本阶段不接微信支付 / 支付宝真实网关。
- 不新增 `payment_order`。
- 支付不等于结算。
- record payment 不自动将工单变为 `SETTLED`。
- 结算仍需调用 `POST /api/staff/work-orders/{id}/settle`。
- `receivedAmount = paymentTotal - refundTotal`，以后端计算为准。
- 支付支持多次付款和混合付款。

## 10. Refund 小程序 API 设计

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/work-orders/{id}/refunds` | 查询当前工单退款记录 |
| POST | `/api/staff/work-orders/{id}/refunds` | 记录客户退款 |

边界：

- 退款必须生成 `refund_record`。
- 不删除、不覆盖原 `payment_record`。
- 退款不能超过可退金额。
- 已结算后退款只影响财务实收，不自动反结算，不自动回滚库存。
- 复杂退货、反结算、库存回滚后续单独设计。

## 11. Reimbursement 小程序 API 设计

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| POST | `/api/staff/reimbursements` | 员工提交报销 |
| GET | `/api/staff/reimbursements/my` | 查询我的报销 |
| PUT | `/api/staff/reimbursements/{id}/cancel` | 员工取消待确认报销，建议预留 |

`cancel` 判断：建议预留但不作为首批必须实现。只有 `PENDING` 报销允许员工取消，已 `CONFIRMED` 或已驳回的报销不允许取消。

边界：

- 小程序负责员工提交报销。
- admin-web 负责确认 / 驳回。
- 只有 `CONFIRMED` 报销才计入运营成本。
- 当前后端 ReimbursementService 仍是 stub，本任务只设计，不实现。

## 12. Dict / Part 只读复用

建议 endpoint：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/dict/types/{typeCode}/items` | 查询字典项 |
| GET | `/api/staff/parts` | 查询配件列表 |
| GET | `/api/staff/parts/{partId}` | 查询配件详情 |

说明：

- 小程序需要字典项展示支付方式、配件来源、费用类型、工单状态等。
- 小程序需要查询配件并选择到费用项。
- staff API 可以复用 admin 的查询 Service，但 response 应更轻量。
- 小程序只读查询不应暴露配件启用/停用、字典配置等管理能力。

## 13. Staff Me/Profile API 占位

建议后续预留：

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/api/staff/me` | 当前员工信息、门店、角色、权限摘要 |

当前不实现。正式认证完成后再落地。

## 14. 权限与角色

staff/mobile 端最小权限点草案：

| 权限码 | 说明 |
| --- | --- |
| `STAFF_WORK_ORDER_CREATE` | 创建工单草稿 |
| `STAFF_WORK_ORDER_UPDATE_DRAFT` | 更新草稿工单 |
| `STAFF_WORK_ORDER_SUBMIT` | 提交工单 |
| `STAFF_WORK_ORDER_CANCEL` | 取消工单 |
| `STAFF_WORK_ORDER_SETTLE` | 结算工单 |
| `STAFF_CHARGE_ITEM_MANAGE` | 管理草稿费用项 |
| `STAFF_PAYMENT_RECORD` | 记录客户支付 |
| `STAFF_REFUND_RECORD` | 记录客户退款 |
| `STAFF_INVENTORY_VIEW` | 查看库存 |
| `STAFF_INVENTORY_INBOUND` | 普通入库 |
| `STAFF_REIMBURSEMENT_SUBMIT` | 提交报销 |
| `STAFF_PART_READ` | 查询配件 |
| `STAFF_DICT_READ` | 查询字典 |

说明：

- 本任务不实现权限。
- 后续 Auth / User / Permission 任务中再落地。
- 当前 dev 阶段可先复用 `X-User-Id` / `X-Store-Id`。

## 15. 小程序不应承担的功能

staff/mobile 不做：

- 财务报表
- Excel 导出
- 用户 / 角色 / 权限管理
- 官方结算录入
- 报销确认
- 库存调整，除非后续明确管理员权限
- 字典配置
- 配件启用 / 停用
- 复杂盘点
- 真实支付网关
- 自动退款
- 反结算
- 多门店调拨
- 自动采购
- 同步官方系统
- 自动打款
- 员工提成
- 客户端小程序能力

## 16. 与 admin-web 的关系

admin-web 和 staff/mobile 的关系：

- 两端共享后端核心 Service / Application Service。
- 两端可以有不同 Controller、不同 DTO、不同字段裁剪。
- 后端业务规则必须一致。
- 不复制一套业务逻辑。
- 不让前端自行判断库存、结算、退款等核心规则。
- admin-web 负责管理、查看、核对、库存调整、官方结算、报销确认、财务报表、导出、权限配置。
- staff/mobile 负责现场录入和现场操作，不替代管理端。

## 17. 推荐实现顺序

建议后续任务顺序：

1. Staff API skeleton + dev header：建立 `/api/staff/**` Controller 包、统一当前用户上下文读取、基础响应规范。
2. Staff Dict / Part / Inventory read：先提供小程序选择配件、展示字典、查询库存的只读能力。
3. Staff WorkOrder DRAFT create/update：支持创建和编辑草稿工单基础信息。
4. Staff ChargeItem manage：支持草稿阶段费用项新增、修改、删除。
5. Staff Submit / Cancel / Settle action：接入后端状态机和库存 RESERVE / RELEASE / CONSUME。
6. Staff Payment / Refund record：支持线下支付和退款记录。
7. Staff Reimbursement submit：在 ReimbursementService 完成后支持员工提交报销。
8. 小程序 UI 接入：按 staff API 联调现场工作流。

任务分工建议：

- Codex：Staff API skeleton、WorkOrder action、Payment/Refund、Inventory inbound、涉及状态机/库存/事务一致性的任务。
- Claude / Gemini：轻量查询接口、DTO 文档同步、小程序 UI 页面、普通表单校验、README / API examples。
- Owner 审核：认证方案、权限点、临时配件组合接口、已提交工单费用项调整、反结算或退货相关设计。

## 18. 禁止范围

本阶段明确不做：

- 不写 Controller。
- 不改 Service。
- 不改数据库。
- 不实现 Auth。
- 不接微信支付。
- 不接支付宝支付。
- 不新增 `payment_order`。
- 不做 Reimbursement 真实实现。
- 不做 Finance / Export。
- 不引入 Redis / MQ / 微服务。
- 不做复杂审批流。
- 不做多门店正式运营。

## 19. 推荐第一批实现 endpoint

建议第一批实现集中在只读能力和草稿工单闭环，降低风险：

```text
GET  /api/staff/dict/types/{typeCode}/items
GET  /api/staff/parts
GET  /api/staff/parts/{partId}
GET  /api/staff/inventory/stocks
GET  /api/staff/inventory/stocks/{partId}

GET  /api/staff/work-orders
GET  /api/staff/work-orders/{id}
POST /api/staff/work-orders/drafts
PUT  /api/staff/work-orders/{id}/draft
POST /api/staff/work-orders/{id}/charge-items
PUT  /api/staff/work-orders/{id}/charge-items/{chargeItemId}
DELETE /api/staff/work-orders/{id}/charge-items/{chargeItemId}
```

第二批再实现会触发核心规则的 action：

```text
POST /api/staff/work-orders/{id}/submit
POST /api/staff/work-orders/{id}/cancel
POST /api/staff/work-orders/{id}/settle
POST /api/staff/work-orders/{id}/payments
POST /api/staff/work-orders/{id}/refunds
POST /api/staff/inventory/inbound
```

## 20. 需要 Owner 决策的问题

后续实现前建议 Owner 明确：

1. staff 工单列表可见范围：只看自己创建/维修相关，还是看当前门店全部现场工单。
2. 普通员工是否允许 `POST /api/staff/inventory/inbound`，还是仅店长/库管角色允许。
3. 临时配件是否首批实现组合接口 `temporary-part-charge-item`。
4. 已提交后是否允许调整费用项；如果允许，需要单独设计补库存流水和金额差异规则。
5. staff 退款权限是否默认开放，还是仅店长/前台角色开放。
6. 微信登录、手机号绑定、账号体系的最终认证方案。
