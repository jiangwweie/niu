# M28R 用户与权限管理安全复核

## 1. M28 复核背景

- 复核日期：2026-06-08
- 当前分支：`feature/m18-delivery-readiness`
- 用户提供的 M28 提交：`2656a35 feat(user): support scoped user creation and role assignment`
- 实际复核 HEAD：`63cdeb0 fix(user): harden scoped user permission checks`
- 本轮状态：未部署、未 push、未修改生产数据库、未删除历史数据。
- 本轮范围：复核 M28 用户与权限管理实现，并新增本复核文档；未修改 backend/admin-web 业务代码。

## 2. Flyway migration 复核结果

复核文件：`backend/src/main/resources/db/migration/V15__m28_store_admin_user_manage_permissions.sql`

结论：migration 安全、幂等、无结构变更。

- 只向 `sys_role_permission` 补齐既有权限关系，不新增权限码，不改表结构。
- 通过 `NOT EXISTS` 防止重复插入同一 `role_id + permission_id` 关系。
- 只匹配启用且未删除的 `STORE_ADMIN` 角色，以及启用且未删除的 `USER_MANAGE`、`ROLE_MANAGE` 权限。
- 未发现 `DELETE`、`TRUNCATE`、`DROP`、`ALTER` 或历史数据清理语句。
- 未直接影响 `SUPER_ADMIN` 角色既有权限。
- 未误给普通员工角色或其他角色授权。

关于 `ROLE_MANAGE`：当前后端实现中，`ROLE_MANAGE` 仅参与用户管理入口、角色列表、权限列表等读取能力；角色定义编辑、角色权限定义写入接口不存在。用户创建、编辑、启停、重置密码、微信解绑仍由 `USER_MANAGE` 加服务层范围校验兜底。因此当前给 `STORE_ADMIN` 补 `ROLE_MANAGE` 不构成阻塞问题。

上线后注意：如果未来新增“编辑角色定义/修改角色权限”的接口，不能仅复用 `ROLE_MANAGE` 作为写权限放行，必须继续增加 `SUPER_ADMIN` 或门店范围校验。

## 3. 角色可分配范围复核

结论：后端对 `roleIds` 和兼容 `roleCodes` 均有可分配范围校验，前端传参不能绕过。

- `SUPER_ADMIN` 可分配合法、启用、未删除角色。
- 平台用户与门店用户角色不可混用。
- `SUPER_ADMIN` 不能把 `SUPER_ADMIN` 分配给门店用户。
- `STORE_ADMIN` 只能分配本门店、启用、未删除的普通门店角色。
- `STORE_ADMIN` 不能分配平台角色、其他门店角色、`SUPER_ADMIN`、`STORE_ADMIN`。
- 禁用角色、删除角色、非法 `roleId` 会由后端返回业务错误。
- 角色解析不是只信前端下拉，服务层会重新按数据库角色状态、门店、角色码校验。

## 4. 门店隔离复核

结论：门店隔离由后端服务层兜底，不依赖前端隐藏控件。

- `SUPER_ADMIN` 可查看全部用户，包括平台账号和各门店账号。
- `STORE_ADMIN` 查询用户列表时被限制在当前登录用户门店。
- `STORE_ADMIN` 传入其他 `storeId` 查询、创建或操作时会被拒绝或强制回当前门店。
- 创建用户时，`STORE_ADMIN` 的目标 `storeId` 来自 `CurrentUser`，不是来自前端可信输入。
- 更新用户时，`STORE_ADMIN` 不能修改目标用户门店。
- 启停、重置密码、微信解绑等操作均先按目标用户 `storeId` 和角色集合校验。
- 平台用户和门店用户的 `accountType/storeId/role` 边界清楚。

## 5. 越权场景复核

结论：已复核的越权场景均有后端拒绝路径。

| 场景 | 复核结论 |
| --- | --- |
| 普通员工调用用户管理接口 | 后端拒绝 |
| `STORE_ADMIN` 查看其他门店用户 | 后端拒绝 |
| `STORE_ADMIN` 创建其他门店用户 | 后端拒绝或强制当前门店 |
| `STORE_ADMIN` 传入其他 `storeId` | 后端不信任该输入 |
| `STORE_ADMIN` 分配 `SUPER_ADMIN` | 后端拒绝 |
| `STORE_ADMIN` 分配 `STORE_ADMIN` | 后端拒绝 |
| `STORE_ADMIN` 操作 `SUPER_ADMIN` | 后端拒绝 |
| `STORE_ADMIN` 操作其他门店 `STORE_ADMIN` | 后端拒绝 |
| `STORE_ADMIN` 操作其他门店普通员工 | 后端拒绝 |
| 用户禁用自己 | 后端拒绝 |
| 用户清空自己角色 | 后端拒绝 |
| 禁用最后一个启用 `SUPER_ADMIN` | 后端拒绝 |
| 降权最后一个启用 `SUPER_ADMIN` | 后端拒绝 |
| 重置其他门店用户密码 | 后端拒绝 |
| 解绑其他门店用户微信 | 后端拒绝 |

## 6. 密码安全复核

结论：临时密码策略符合目标模式，未发现新增泄露路径。

- 创建用户密码由后端随机生成。
- 重置密码由后端随机生成。
- 创建和重置后均设置 `password_must_change=true`。
- 临时密码只在创建/重置响应中返回一次。
- 普通详情、列表接口不返回密码。
- 重置密码请求不依赖前端自定义密码。
- 未发现新增日志输出临时密码。
- 未在复核文档中记录任何真实密码、token、hash、JWT secret 或微信 secret。
- 前端仅在一次性密码弹窗中展示响应值，未发现控制台打印或自动持久化。

## 7. 微信解绑复核

结论：微信解绑逻辑安全，未发现删除用户或影响历史业务数据的行为。

- 解绑只清空 `wechat_openid`、`wechat_unionid`、`wechat_bound_at`。
- 不删除用户。
- 不修改历史工单、支付、退款、库存、结算、财务数据。
- `STORE_ADMIN` 只能解绑本门店普通员工。
- 解绑 `SUPER_ADMIN`、同门店 `STORE_ADMIN` 或其他门店用户会被拒绝。
- 解绑后微信快捷登录会回到未绑定状态，需要按既有绑定流程重新关联账号。

## 8. 前端权限展示复核

结论：前端展示与后端目标模式一致，但安全边界仍以后端为准。

- 用户管理入口只对具备 `USER_MANAGE` 或 `ROLE_MANAGE` 的用户展示。
- 普通员工不可见“员工与权限”入口。
- 平台 `SUPER_ADMIN` 可进入用户管理路由。
- `STORE_ADMIN` 新增用户时不提供可编辑门店选择，显示当前门店口径。
- `STORE_ADMIN` 角色下拉依赖后端可分配角色列表，不展示 `SUPER_ADMIN`、`STORE_ADMIN`。
- 停用、重置密码、解绑微信、角色修改存在二次确认。
- 关键提交按钮存在 loading/disabled 状态，降低重复点击风险。
- 一次性密码弹窗只用于人工复制保存，未发现自动持久化。

## 9. 测试覆盖情况

复核测试文件：

- `backend/src/test/java/com/niushop/controller/AdminUserManagementControllerTest.java`
- `backend/src/test/java/com/niushop/controller/AdminUserSecurityHardeningTest.java`
- `backend/src/test/java/com/niushop/controller/PlatformStoreControllerTest.java`
- `backend/src/test/java/com/niushop/controller/WechatAuthTest.java`

覆盖情况：

- 覆盖 `SUPER_ADMIN` 创建用户、跨门店筛选、分配角色、重置密码、微信解绑。
- 覆盖 `STORE_ADMIN` 创建本门店普通员工成功。
- 覆盖普通员工访问用户管理接口失败。
- 覆盖跨门店查看、更新、启停、重置密码、创建失败。
- 覆盖 `STORE_ADMIN` 分配 `SUPER_ADMIN`、`STORE_ADMIN` 失败。
- 覆盖 `STORE_ADMIN` 操作 `SUPER_ADMIN` 或同门店 `STORE_ADMIN` 失败。
- 覆盖用户禁用自己失败。
- 覆盖最后一个启用 `SUPER_ADMIN` 不能被禁用或降权。
- 覆盖重置密码后强制改密。
- 覆盖微信解绑仅清空绑定字段。
- 覆盖平台账号可访问用户管理接口但不能访问门店业务接口。

历史验收记录显示 M28 修复后已通过：

- `cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test`
- `cd admin-web && npx vue-tsc --noEmit`
- `cd admin-web && npm run build`
- `git diff --check`

本轮 M28R 仅新增复核文档，未重新执行完整后端和前端测试。

## 10. 发现的问题

未发现阻塞部署的安全问题。

非阻塞注意项：

- `ROLE_MANAGE` 当前不是角色定义写权限；如果未来新增角色定义编辑或权限绑定写接口，需要重新收紧 `STORE_ADMIN` 对该能力的使用范围。
- 生产启用前需要确认线上数据库中 `STORE_ADMIN` 角色、权限状态符合 migration 预期，且 Flyway 只在目标环境正常执行一次。
- 前端权限展示不是安全边界，后续新增用户管理动作时必须同步增加服务层校验和测试。

## 11. 是否阻塞部署

结论：不阻塞部署。

条件：

- 仅部署当前已复核 HEAD 对应代码和 migration。
- 部署前确认目标环境备份、Flyway 执行计划、回滚预案。
- 部署后按本文件 smoke checklist 验证。

## 12. 部署前建议

- 部署前在预发或本地类生产数据环境执行 Flyway，并确认 `sys_role_permission` 没有重复关系。
- 用平台 `SUPER_ADMIN` 验证用户管理入口、平台用户创建、门店用户创建、跨门店筛选。
- 用 `STORE_ADMIN` 验证只能创建本门店普通员工，且不能分配 `SUPER_ADMIN`、`STORE_ADMIN` 或其他门店角色。
- 用普通员工验证用户管理入口不可见，直接请求接口返回拒绝。
- 验证创建/重置密码只在成功响应弹窗展示一次，刷新后不可再取回。
- 验证微信解绑后原用户仍存在，历史工单仍可查询，微信快捷登录提示符合未绑定状态。
- 不在部署日志、工单、文档中记录一次性密码。

## 13. 生产 smoke checklist

- [ ] Flyway 已执行且 `V15__m28_store_admin_user_manage_permissions.sql` 成功。
- [ ] `STORE_ADMIN` 角色仅新增既有 `USER_MANAGE`、`ROLE_MANAGE` 权限关系，无重复数据。
- [ ] 平台 `SUPER_ADMIN` 可进入“员工与权限”。
- [ ] 平台 `SUPER_ADMIN` 可查看全部门店用户和平台账号。
- [ ] 平台 `SUPER_ADMIN` 可创建平台账号和指定门店用户。
- [ ] 平台 `SUPER_ADMIN` 不能禁用自己。
- [ ] 平台 `SUPER_ADMIN` 不能禁用或降权最后一个启用超管。
- [ ] `STORE_ADMIN` 可进入“员工与权限”。
- [ ] `STORE_ADMIN` 只能查看本门店用户。
- [ ] `STORE_ADMIN` 只能创建本门店普通员工。
- [ ] `STORE_ADMIN` 不能分配 `SUPER_ADMIN`、`STORE_ADMIN`、平台角色或其他门店角色。
- [ ] `STORE_ADMIN` 不能操作同门店 `STORE_ADMIN`。
- [ ] `STORE_ADMIN` 不能操作其他门店用户。
- [ ] 普通员工不可见用户管理入口，直接请求接口被拒绝。
- [ ] 创建用户成功后只展示一次性临时密码。
- [ ] 重置密码成功后只展示一次性临时密码，并强制下次修改。
- [ ] 微信解绑只清空绑定字段，不删除用户，不影响历史业务数据。
- [ ] 平台账号仍不能访问门店库存、工单、支付、退款、结算、财务等业务接口。
