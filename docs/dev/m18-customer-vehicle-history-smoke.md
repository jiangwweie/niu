# M18 客户 / 车辆档案 + 维修历史查询 Smoke 说明

## 能力范围

M18 提供客户与车辆档案管理，以及按客户/车辆查看维修历史的能力：

**管理端 (admin-web)**：

- 客户档案列表：`GET /api/admin/customers` — 分页查询，支持姓名/手机号搜索
- 客户详情：`GET /api/admin/customers/{id}` — 含名下车辆列表与最近维修工单
- 新建客户：`POST /api/admin/customers` — 姓名必填，手机号可选
- 编辑客户：`PUT /api/admin/customers/{id}` — 不允许跨店编辑
- 车辆档案列表：`GET /api/admin/vehicles` — 分页查询，支持车架号/车型/客户手机号搜索
- 车辆详情：`GET /api/admin/vehicles/{id}` — 含所属客户与最近维修工单
- 为客户新增车辆：`POST /api/admin/customers/{customerId}/vehicles` — 车架号必填
- 编辑车辆：`PUT /api/admin/vehicles/{id}` — 不允许跨店编辑

**小程序 (mini-program)**：

- 搜索已有客户：`GET /api/staff/customers/search?keyword=` — 用于创建工单时选择
- 搜索已有车辆：`GET /api/staff/vehicles/search?keyword=` — 用于创建工单时选择
- 创建工单增强：`POST /api/staff/work-orders/drafts` — 支持传 `customerId` / `vehicleId`

## 权限点

| 权限码 | 名称 | 说明 |
|--------|------|------|
| CUSTOMER_VIEW | 客户档案查看 | 查看客户列表、详情、车辆列表、详情；小程序搜索客户/车辆 |
| CUSTOMER_MANAGE | 客户档案管理 | 新建/编辑客户；为用户新增车辆；编辑车辆 |

**统一管理**：`CUSTOMER_VIEW` / `CUSTOMER_MANAGE` 同时控制客户和车辆档案，不单独设立 `VEHICLE_VIEW` / `VEHICLE_MANAGE`，以降低权限点复杂度。

### 角色权限分配

| 角色 | CUSTOMER_VIEW | CUSTOMER_MANAGE |
|------|:---:|:---:|
| SUPER_ADMIN | Y | Y |
| STORE_ADMIN | Y | Y |
| TECHNICIAN_FRONT_DESK | Y | - |
| FINANCE | - | - |

## 客户档案实现说明

- 客户表 (`customer`) 已在 V1 建表，字段：`store_id`、`customer_name`、`phone`、`remark`
- 列表查询按 `storeId` 隔离，支持 `keyword`（模糊匹配姓名或手机号）、`phone`（精确匹配）、`customerName`（模糊匹配）
- 客户详情返回名下车辆列表（最多全部）和最近 20 条维修工单
- 新建客户时如提供手机号，校验同一门店内手机号不重复
- 客户姓名必填，手机号可选

## 车辆档案实现说明

- 车辆表 (`vehicle`) 已在 V1 建表，字段：`store_id`、`customer_id`、`model`、`frame_no`、`battery_no`、`remark`
- 车架号 (`frame_no`) 在同一门店内唯一
- 车辆必须归属于某个客户（`customer_id` 必填），通过 `POST /api/admin/customers/{customerId}/vehicles` 创建
- 车辆详情返回所属客户信息和最近 20 条维修工单
- 列表查询支持按车架号、车型、客户手机号搜索

## 维修历史口径

- 客户详情页的维修历史 = 该 `customer_id` 关联的工单，按创建时间倒序，取最近 20 条
- 车辆详情页的维修历史 = 该 `vehicle_id` 关联的工单，按创建时间倒序，取最近 20 条
- 工单展示使用快照字段（`customer_name_snapshot`、`vehicle_model_snapshot` 等），不因后续客户/车辆资料修改而改变历史记录

## 小程序创建工单选择已有客户/车辆流程

1. 创建工单 Step 1 页面新增「搜索已有客户」和「搜索已有车辆」按钮
2. 点击后弹出搜索面板，输入关键词搜索
3. 选择客户后自动填充客户姓名、手机号
4. 选择车辆后自动填充车型、车架号
5. 仍可手工修改已填充的字段
6. 保存草稿时如果已选择客户/车辆，会将 `customerId` / `vehicleId` 传给后端
7. 后端校验：`customerId` / `vehicleId` 必须属于当前门店；如同时传入，车辆必须属于该客户

## storeId 隔离

所有客户/车辆查询、创建、编辑操作均按当前 JWT 用户的 `storeId` 隔离：

- 列表查询：`WHERE store_id = ?`
- 详情查询：先按 `store_id` 过滤，不匹配返回"不存在"
- 创建：使用当前用户的 `storeId`
- 编辑：校验实体 `storeId` 与当前用户一致，不一致返回错误
- 小程序搜索：仅返回当前门店的客户/车辆
- 工单创建：如传 `customerId` / `vehicleId`，校验属于当前门店

## 不支持的内容

- 会员体系 / 客户等级
- 积分系统
- 保险管理
- 多车主关系（一辆车只属于一个客户）
- 客户自定义权限点
- 完整 CRM 能力
- 车牌号字段（后续按需补充）

## 数据库迁移

V10 迁移新增：

- `sys_permission` 记录：`CUSTOMER_VIEW`（1021）、`CUSTOMER_MANAGE`（1022）
- `sys_role_permission`：SUPER_ADMIN + STORE_ADMIN 获得全部两项；TECHNICIAN_FRONT_DESK 仅获得 `CUSTOMER_VIEW`

未修改 `customer` / `vehicle` 表结构，现有字段已满足需求。

## Smoke 验证清单

- [ ] admin-web 客户档案页面可正常访问
- [ ] 客户列表支持姓名、手机号搜索
- [ ] 客户详情展示名下车辆与维修历史
- [ ] 新建客户后可在列表中找到
- [ ] 无 CUSTOMER_VIEW 权限的用户看不到菜单
- [ ] 车辆档案页面可正常访问
- [ ] 车辆列表支持车架号、车型、客户手机号搜索
- [ ] 车辆详情展示所属客户与维修历史
- [ ] 小程序创建工单可搜索并选择已有客户
- [ ] 小程序创建工单可搜索并选择已有车辆
- [ ] 选择客户/车辆后工单正确保存关联关系
- [ ] 手工录入新客户/新车辆流程不受影响
- [ ] 跨门店访问客户/车辆返回错误
- [ ] 工单详情仍使用快照字段展示
