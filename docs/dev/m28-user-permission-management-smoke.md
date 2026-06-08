# M28 用户与权限管理 Smoke

## 目标模式

管理端支持用户列表、新增用户、编辑基础信息、角色分配、启用停用、重置密码、微信绑定状态展示和解绑微信。所有权限边界以后端校验为准，前端只做展示和交互优化。

## 角色能力矩阵

| 角色 | 查看用户 | 创建用户 | 分配角色 | 启停 | 重置密码 | 解绑微信 |
| --- | --- | --- | --- | --- | --- | --- |
| SUPER_ADMIN | 全部门店和平台用户 | 任意合法门店或平台用户 | 所有启用且范围合法的角色 | 是 | 是 | 是 |
| STORE_ADMIN | 本门店用户 | 本门店普通员工 | 本门店普通角色 | 本门店普通员工 | 本门店普通员工 | 本门店普通员工 |
| 普通员工 | 否 | 否 | 否 | 否 | 否 | 否 |

## 后端校验点

- 平台 `SUPER_ADMIN` 可访问 `/api/admin/users/**`、`/api/admin/roles`、`/api/admin/permissions`；其他 `/api/admin/**` 和 `/api/staff/**` 仍被平台账号隔离。
- `SUPER_ADMIN` 查询用户时可不带 `storeId` 查看全部，也可用 `storeId` 筛选门店用户。
- `STORE_ADMIN` 查询和操作均强制当前登录人的 `storeId`，传其他 `storeId` 会被拒绝。
- `STORE_ADMIN` 只能操作本门店普通员工，不能操作 `SUPER_ADMIN` 或同门店其他 `STORE_ADMIN`。
- 普通员工没有 `USER_MANAGE` / `ROLE_MANAGE`，用户管理接口返回 403。

## 角色分配规则

- 角色分配优先使用 `roleIds`，保留 `roleCodes` 兼容旧调用。
- 禁用角色、非法角色、跨门店角色和平台/门店角色混用都会返回业务错误。
- `SUPER_ADMIN` 可分配所有启用角色，但平台账号只能分配平台角色，门店账号只能分配所属门店角色。
- `STORE_ADMIN` 不能分配 `SUPER_ADMIN`、平台级角色、其他门店角色或 `STORE_ADMIN`。

## 密码策略

- 新建用户由后端生成 12 位随机临时密码，加密后保存。
- 新建和重置密码都会设置 `password_must_change=true`。
- 临时密码只在创建或重置响应中返回一次，不写日志、不写文档、不写 git。
- 管理端弹窗展示一次性密码，关闭后不再从后端获取。

## 微信绑定与解绑

- 用户列表展示 `wechatBound` 和 `wechatBoundAt`。
- 解绑微信只清空 `wechat_openid`、`wechat_unionid`、`wechat_bound_at`。
- 解绑不删除用户，不影响历史工单；用户需要重新绑定微信后才能使用微信快捷登录。

## 数据库与权限

- 新增 migration：`V15__m28_store_admin_user_manage_permissions.sql`。
- 不新增数据库字段，不删除历史数据，不清空业务表。
- 不新增 permission code。
- migration 仅给启用且未删除的 `STORE_ADMIN` 角色补齐既有 `USER_MANAGE`、`ROLE_MANAGE` 权限关系。

## 前端页面变更

- 用户列表增加所属门店列、门店筛选、角色明细展示。
- `SUPER_ADMIN` 新增用户时可选择平台账号或门店账号；`STORE_ADMIN` 只显示当前门店。
- 角色下拉改为 `roleId`，按后端返回的可分配角色展示。
- 删除手填初始密码和手填重置密码；创建/重置后弹窗展示一次性密码。
- 停用、重置密码、解绑微信、修改角色均有二次确认；提交按钮带 loading/disabled。

## Smoke 步骤

1. `SUPER_ADMIN` 登录管理端，进入“员工与权限”，查看全部用户和门店列。
2. 使用门店筛选，只显示指定门店用户。
3. 新建门店员工，确认弹窗返回一次性密码，数据库 `password_must_change=true`。
4. 修改用户角色，确认有二次确认且角色更新成功。
5. 重置用户密码，确认返回一次性密码且 `password_must_change=true`。
6. 解绑已绑定微信用户，确认绑定状态变为未绑定。
7. `STORE_ADMIN` 登录，只能看到本门店用户，只能创建本门店普通员工。
8. `STORE_ADMIN` 分配 `SUPER_ADMIN`、`STORE_ADMIN`、其他门店角色均失败。
9. 普通员工访问用户管理页面或接口失败。
10. 尝试禁用自己或最后一个 `SUPER_ADMIN`，后端拒绝。

## 验证命令

```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
cd admin-web && npx vue-tsc --noEmit
cd admin-web && npm run build
git diff --check
git status --short
```

## 部署建议

- 本轮不部署、不 push。
- 部署前先在测试库执行 Flyway migration，确认 `STORE_ADMIN` 权限关系补齐。
- 生产验证时重点检查平台账号是否只能进入用户管理和平台门店管理，不能访问门店业务接口。
