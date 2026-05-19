# M17A 账号与门店配置 Smoke 说明

## 能力范围

M17A 提供正式交付前最小可用的账号生命周期与单门店配置能力：

- 当前用户修改密码：`POST /api/auth/change-password`
- 当前用户信息：`GET /api/auth/me` 返回 `passwordMustChange`
- 管理员用户列表/详情：`GET /api/admin/users`、`GET /api/admin/users/{id}`
- 管理员创建/编辑用户：`POST /api/admin/users`、`PUT /api/admin/users/{id}`
- 管理员启用/停用用户：`POST /api/admin/users/{id}/enable|disable`
- 管理员重置密码：`POST /api/admin/users/{id}/reset-password`
- 角色/权限只读查看：`GET /api/admin/roles`、`GET /api/admin/permissions`
- 当前门店配置查看/修改：`GET|PUT /api/admin/store/current`

本次不是完整 RBAC 平台，不支持客户自定义权限点，不支持多门店 SaaS。

## 权限点

- `USER_MANAGE`：用户创建、编辑、启停、重置密码。
- `ROLE_MANAGE`：用户列表/详情、角色与权限只读查看。
- `STORE_MANAGE`：当前门店配置查看/修改。

`SUPER_ADMIN` 具备全部能力；`STORE_ADMIN` 通过迁移获得 `STORE_MANAGE`。普通员工和财务不应具备用户重置密码能力。

## passwordMustChange 行为

- 管理员创建用户后，`passwordMustChange=true`。
- 管理员重置用户密码后，`passwordMustChange=true`。
- 当前用户修改密码成功后，`passwordMustChange=false`，并写入 `passwordChangedAt`。
- admin-web 登录后如果 `passwordMustChange=true`，强制跳转 `/change-password`。
- 修改密码后当前 token 继续有效，前端刷新 `/api/auth/me` 同步状态。

## 密码规则

密码使用 Spring Security `PasswordEncoder` bcrypt 存储。新密码至少 8 位，必须包含字母和数字；重置密码不会在日志中输出明文。重置接口只在响应中返回一次临时密码，便于管理员线下告知用户。

## 正式交付建号建议

1. 使用系统初始 `SUPER_ADMIN` 登录。
2. 在 admin-web “门店配置”维护门店名称、联系人、电话和地址。
3. 在“用户与权限”创建客户管理员账号，分配 `STORE_ADMIN` 或预设管理角色。
4. 创建财务、前台/维修员工账号并分配预设角色。
5. 初始密码可手动设置为合规临时密码，也可留空由系统生成。
6. 告知用户首次登录后必须修改密码。

## 已知限制

- 不是完整 RBAC 平台，只能分配预设角色。
- 权限点只读，不支持客户自定义权限点。
- 当前仍为单门店 MVP，接口按当前用户 `storeId` 操作，不做多门店 SaaS 管理。
- 不包含组织架构、审批流、员工提成、自动采购、官方系统同步。

## Smoke 结果

- 后端：`mvn test`，561 tests，全部通过。
- admin-web：`npx vue-tsc --noEmit` 通过。
- admin-web：`npm run build` 通过；仍有 Vite/Rollup 对 `@vueuse/core` PURE 注释和 chunk size 的历史构建警告。
