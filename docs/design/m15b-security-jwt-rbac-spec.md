# M15B Security JWT RBAC 设计文档

## 1. 当前认证与权限现状摘要

当前后端已经有业务上下文门面，但没有正式认证授权链路。

- `CurrentUserContext` 使用 `ThreadLocal<CurrentUser>` 保存当前请求用户，业务 Controller 从中读取 `userId` / `storeId` / `operatorId`，避免信任 body、query 或前端传入的业务身份字段。
- `CurrentUser` 当前包含 `userId`、`storeId`、`username`、`permissions`，但 dev header 注入时 `username = null`、`permissions = Set.of()`。
- `DevCurrentUserInterceptor` 仅在 `dev` / `test` profile 生效，读取 `X-User-Id` 和 `X-Store-Id`，在请求进入时写入 `CurrentUserContext`，请求结束时清理 ThreadLocal。它不是认证机制。
- `AdminWebMvcConfig` 仅在 `dev` / `test` profile 生效，配置 `/api/**` CORS，并把 `DevCurrentUserInterceptor` 注册到 `/api/admin/**` 和 `/api/staff/**`。
- 数据库已有 `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`，可表达用户、角色、权限点和两张关联表。
- V2 seed 已有 `SUPER_ADMIN`、`STORE_ADMIN`、`FINANCE`、`TECHNICIAN_FRONT_DESK` 等角色，以及 `WORK_ORDER_SETTLE`、`REFUND_RECORD`、`FINANCE_VIEW`、`EXCEL_EXPORT`、`OFFICIAL_SETTLEMENT_MANAGE`、`REIMBURSEMENT_CONFIRM` 等权限点。
- `PermissionQueryService` 已能通过 user -> role -> permission 链路查询启用且未删除的权限码，并提供 `hasPermission`，但当前尚未接入认证过滤器或方法级权限控制。
- `admin-web` 当前没有登录页、token 保存、路由守卫，`src/utils/request.ts` 固定写入 `X-User-Id=1`、`X-Store-Id=1`。
- `mini-program` 当前默认 mock 模式，没有真实登录；切换 real 模式时 request wrapper 会携带 mock 用户的 `X-User-Id` / `X-Store-Id`。

主要风险：

- 生产 profile 下没有正式认证入口，也没有认证拦截器。
- header 身份可伪造，不能进入生产认证链路。
- `CurrentUser.permissions` 当前为空，已有 RBAC 数据没有参与接口授权。
- 高风险接口仅依赖业务状态校验，没有统一权限边界。
- 前后端仍处于 dev-only header 混合状态，后续切换必须兼容大量测试。

## 2. 技术选型对比

| 方案 | 实现复杂度 | 当前兼容性 | 长期维护性 | 权限扩展能力 | 测试影响 | 前后端接入难度 | 多门店/多端扩展 | 风险 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 手写 JWT + Interceptor | 低到中 | 短期最贴近现有 `DevCurrentUserInterceptor` | 较弱，认证、授权、异常、方法权限都要自维护 | 可做，但容易分散到拦截器和业务代码 | 旧测试改动较小 | 前端只需 token | 可扩展，但自研边界会越来越重 | 容易形成第二套安全框架，权限语义不标准 |
| Spring Security + JWT | 中 | 需要引入 filter chain，但可保留 `CurrentUserContext` 门面 | 强，生态成熟，认证、授权、异常、测试工具完整 | 强，天然支持 `GrantedAuthority`、URL 和方法级授权 | 初次接入影响较大，但可用 dev/test fallback 降低冲击 | 前端标准 Bearer token | 强，适合 admin/staff 多端隔离和未来扩展 | 首次配置复杂，需要严格 profile 隔离 |
| Sa-Token | 中 | 接入成本可控，但会引入另一套会话/权限模型 | 中，国内生态友好但和 Spring 官方安全体系不完全一致 | 较强，注解和权限模型易用 | 需要学习和适配测试 | 前端接入简单 | 可扩展 | 对 Spring Security 标准能力复用较少，团队长期维护依赖框架特性 |

推荐结论：采用 **Spring Security + JWT**。

原因是本项目已经进入支付、退款、结算、财务、Excel 导出等高风险接口闭合阶段，权限边界需要标准化。Spring Security 可作为认证授权基础设施，JWT 作为前后端凭证，RBAC 权限点映射为 `GrantedAuthority`。业务层继续使用 `CurrentUserContext`，避免业务模块直接散落依赖 Spring Security。

## 3. 目标架构

目标请求链路：

```text
HTTP Request
  ↓
Spring Security Filter Chain
  ↓
JwtAuthenticationFilter
  ↓
解析 Authorization: Bearer token
  ↓
加载用户、角色、权限
  ↓
写入 SecurityContext
  ↓
桥接写入 CurrentUserContext
  ↓
Controller / Service 使用 CurrentUserContext
  ↓
请求结束 clear ThreadLocal
```

职责边界：

- `SecurityContext` 是认证授权基础设施，负责表达“当前请求是否已认证、拥有哪些权限”。
- `CurrentUserContext` 是业务上下文门面，负责给业务代码提供稳定的 `userId`、`storeId`、`username`、`permissions`。
- Controller 和 Service 不应到处直接读取 `SecurityContextHolder`，否则后续调整安全框架或 token 结构会扩散到业务模块。
- `JwtAuthenticationFilter` 或专门的桥接 filter 在认证成功后构造 `CurrentUser`，写入 `CurrentUserContext`。
- 请求结束必须清理 `CurrentUserContext`，防止线程复用导致身份串扰。

## 4. JWT 设计

建议 access token claims：

| Claim | 含义 |
| --- | --- |
| `sub` | 用户主键字符串，等价于 `userId` |
| `userId` | 用户 ID，便于前端和后端调试 |
| `storeId` | 当前门店 ID，MVP 阶段仍是单门店上下文 |
| `username` | 登录账号 |
| `realName` | 用户真实姓名 |
| `roleCodes` | 角色编码列表 |
| `permissionCodes` | 权限点编码列表 |
| `userType` 或 `accountType` | 登录端类型；现表暂无字段，短期可由角色推断 |
| `iat` | 签发时间 |
| `exp` | 过期时间 |

设计建议：

- access token 有效期建议 2 到 8 小时。门店后台可先取 4 小时，降低权限变更不实时的窗口。
- 第一阶段不做 refresh token。登录过期后重新登录即可，避免引入黑名单、续期和多端踢出复杂度。
- JWT secret 从配置读取，例如 `application.yml` / 环境变量，不写死在代码和测试外的公开配置中。
- 第一阶段可以把 `permissionCodes` 放入 token，减少每次请求查库成本，并直接映射为 `GrantedAuthority`。
- 权限变更即时生效不是第一阶段目标。短期通过较短 access token 有效期控制风险。
- 后续如需要增强实时性，可增加 `tokenVersion` / `permissionVersion`，或改为 token 只放 userId、storeId，每次请求加载权限并配合缓存。

## 5. 用户类型与 admin/staff 隔离设计

当前 `sys_user` 表包含 `store_id`、`username`、`password_hash`、`real_name`、`phone`、`wechat_openid`、`wechat_unionid`、`status` 等字段，没有独立的 `user_type` / `account_type` 字段。

方案 A：短期用角色推断访问端。

- `SUPER_ADMIN` / `STORE_ADMIN` / `FINANCE` 可访问 admin。
- `TECHNICIAN_FRONT_DESK` 可访问 staff。
- 优点是不需要 migration，M15B-1 可快速闭合登录和端隔离。
- 缺点是角色和登录端类型耦合，后续一个用户同时兼任后台管理和门店前台时表达不够清晰。

方案 B：后续补 `user_type` / `account_type` 字段。

- 建议枚举：`ADMIN`、`STAFF`、`BOTH`。
- 端隔离用账户类型判断，权限授权仍用 permissionCode 判断。
- 优点是“能从哪个端登录”和“登录后能做什么”分离，模型更清晰。
- 缺点是需要数据库 migration、seed、接口和前端适配。

推荐：

- M15B 阶段不新增 migration，先采用方案 A，基于角色做粗粒度端隔离。
- 后续进入正式账号体系时建议补 `account_type` 字段。
- 角色和登录端类型不完全等价：角色表示职责和权限集合，登录端类型表示入口和体验边界。一个财务用户可能只允许 admin 登录，一个店长可能既看 admin 又处理 staff 场景，这不应靠硬编码角色名长期维持。

## 6. 权限模型设计

沿用现有 RBAC：

```text
User
  ↓ sys_user_role
Role
  ↓ sys_role_permission
Permission
```

设计原则：

- 权限判断以 `permissionCode` 为准。
- `roleCode` 只用于聚合权限、默认授权、粗粒度端隔离，不建议在业务代码中写死 `roleName` 或 `roleCode` 判断业务操作权限。
- 权限点来源以 `sys_permission.permission_code` 为准，Java 代码中的常量应只作为引用，不能成为第二套权限源。
- V2 seed 已有的权限点直接复用：`WORK_ORDER_SUBMIT`、`WORK_ORDER_CANCEL`、`WORK_ORDER_SETTLE`、`PAYMENT_RECORD`、`REFUND_RECORD`、`OFFICIAL_SETTLEMENT_MANAGE`、`REIMBURSEMENT_CONFIRM`、`FINANCE_VIEW`、`EXCEL_EXPORT`、`INVENTORY_INBOUND`、`INVENTORY_ADJUST` 等。
- 如发现权限点不足，应以最小 migration/seed 补充，不应为了某个接口临时写死角色判断。

## 7. 授权拦截策略

采用两层授权。

第一层：URL 级认证保护。

```text
/api/auth/**    permitAll
/api/health     permitAll
/api/admin/**   authenticated
/api/staff/**   authenticated
```

第二层：方法级权限保护。

短期推荐直接使用 Spring Security 标准注解：

```java
@PreAuthorize("hasAuthority('FINANCE_VIEW')")
```

长期可以封装自定义注解：

```java
@RequirePermission("FINANCE_VIEW")
```

短期用 `@PreAuthorize` 的原因是实现成本低、语义标准、测试工具成熟。等权限点覆盖稳定后，再考虑封装 `@RequirePermission`，统一错误码、审计日志和权限常量引用。

高风险接口与权限点映射建议：

| 端 | 能力 | 权限点 |
| --- | --- | --- |
| Admin | 官方结算 | `OFFICIAL_SETTLEMENT_MANAGE` |
| Admin | 报销确认/驳回 | `REIMBURSEMENT_CONFIRM` |
| Admin | 财务查看 | `FINANCE_VIEW` |
| Admin | Excel 导出 | `EXCEL_EXPORT` |
| Admin | 库存调整 | `INVENTORY_ADJUST` |
| Admin | 用户管理 | `USER_MANAGE` |
| Admin | 角色管理 | `ROLE_MANAGE` |
| Staff | 工单提交 | `WORK_ORDER_SUBMIT` |
| Staff | 工单取消 | `WORK_ORDER_CANCEL` |
| Staff | 工单结算 | `WORK_ORDER_SETTLE` |
| Staff | 支付记录 | `PAYMENT_RECORD` |
| Staff | 退款记录 | `REFUND_RECORD` |
| Staff | 报销提交 | `REIMBURSEMENT_SUBMIT` |
| Staff | 配件入库 | `INVENTORY_INBOUND` |

## 8. dev/test header fallback 设计

必须保留兼容策略，但严格限制环境。

- `dev` / `test` profile 可以保留 `X-User-Id` / `X-Store-Id` fallback。
- 认证顺序：JWT 优先，header fallback 其次。已有合法 Bearer token 时不读取 dev header 覆盖身份。
- `prod` profile 禁用 header fallback。生产环境即使传入 `X-User-Id` / `X-Store-Id` 也不能通过认证。
- 旧测试大量依赖 header，不应逐个粗暴改成登录拿 token。M15B-1 可保留 test profile fallback，M15B-3 再补统一 test helper，逐步把高风险权限测试迁移到 JWT。
- 必须有 prod-like profile 测试证明：仅携带 `X-User-Id` / `X-Store-Id` 访问 `/api/admin/**` 或 `/api/staff/**` 返回 401。

实现边界建议：

- 不继续扩大 `DevCurrentUserInterceptor` 的职责。
- 可把 fallback 设计成 Spring Security filter chain 中的 dev/test-only filter，或保留拦截器但确保它不绕过 Security 的 authenticated 判断。
- 无论哪种实现，prod profile 必须不注册 fallback 组件。

## 9. 异常语义

认证与授权错误统一为：

- 未登录、token 缺失、token 无效、token 过期：`401 + UNAUTHORIZED`。
- 已登录但权限不足：`403 + FORBIDDEN`。

兼容现有 `ApiResponse`：

```json
{
  "code": "UNAUTHORIZED",
  "message": "未登录或登录已过期",
  "data": null,
  "traceId": "..."
}
```

```json
{
  "code": "FORBIDDEN",
  "message": "无权限访问该资源",
  "data": null,
  "traceId": "..."
}
```

实现建议：

- 配置 `AuthenticationEntryPoint` 输出 401 的统一 `ApiResponse`。
- 配置 `AccessDeniedHandler` 输出 403 的统一 `ApiResponse`。
- `ErrorCode` 如当前没有 `UNAUTHORIZED` / `FORBIDDEN`，M15B-1 应最小补充。
- 不把权限不足包装成业务校验失败，避免前端无法区分登录过期和权限不足。

## 10. 后端实现拆分建议

M15B-1：Security 基座。

- 引入 Spring Security。
- 引入 JWT 工具或 provider。
- 新增登录接口。
- 新增 `/api/auth/me`。
- 新增 `/api/auth/logout` 占位，第一阶段可只返回成功，由前端删除本地 token。
- 新增 `JwtAuthenticationFilter`。
- 建立 `SecurityContext`。
- 建立 `CurrentUserContext` 桥接。
- 建立 401/403 语义。
- 保留 dev/test header fallback。

M15B-2：权限点接入。

- `PermissionQueryService` 接入 `GrantedAuthority`。
- 对高风险接口增加 `@PreAuthorize`。
- 补充 403 测试。

M15B-3：测试兼容与回归。

- 保留 test header fallback 或新增统一 test helper。
- 全量执行 `mvn test`。
- 对 prod-like profile 增加 header 不生效测试。

M15C：admin-web 登录/token 接入。

- 登录页。
- token 保存。
- `request.ts` 改为携带 `Authorization: Bearer <token>`。
- 401 跳登录。
- `/api/auth/me` 初始化用户信息。

M15D：mini-program token 接入。

- 员工账号/手机号登录占位。
- wx storage 保存 token。
- request wrapper 携带 `Authorization`。
- 401 回登录页。

M15E：权限体验层。

- 菜单过滤。
- 按钮显隐。
- 用户权限页从 mock 转真实。
- 可后置，不能替代后端授权。

## 11. 测试计划

认证测试：

- 登录成功返回 token。
- 密码错误返回 401。
- 用户不存在返回 401。
- disabled/deleted 用户无法登录。
- `/api/auth/me` 返回当前用户。
- logout 返回成功。

JWT 测试：

- 无 token 访问 admin/staff 返回 401。
- 无效 token 返回 401。
- 过期 token 返回 401。
- 合法 token 建立 `CurrentUserContext`。
- token 中的 `storeId` 不能被 body、query、header 覆盖。

权限测试：

- 无权限访问财务返回 403。
- 无权限导出返回 403。
- 无权限报销确认返回 403。
- 无权限官方结算返回 403。
- 有权限访问成功。

隔离测试：

- staff token 不能访问 admin 高风险接口。
- admin token 不能越权访问非授权接口。
- `storeId` 隔离保持有效。

兼容测试：

- dev/test header fallback 仍支持旧测试。
- prod-like profile 下 header 不生效。
- `mvn test` 全量通过。

## 12. 改动范围评估

M15B 实现预计可能影响：

- `backend/pom.xml`
- security config
- jwt util/provider
- auth controller/service/dto
- user/permission service
- `CurrentUser` / `CurrentUserContext`
- interceptor/filter
- global exception handler
- `ErrorCode`
- affected controller permission annotations
- tests
- docs

M15B 实现不应触碰：

- 库存核心逻辑。
- 工单状态机。
- 支付退款逻辑。
- 官方结算业务规则。
- 报销入账规则。
- 财务聚合口径。
- Excel 导出口径。
- `admin-web`。
- `mini-program`。

## 13. 风险与取舍

- Spring Security 首次接入风险：filter 顺序、CORS、异常处理、测试配置都可能影响现有接口。通过 M15B-1 先闭合基座，M15B-2 再加权限注解降低风险。
- 测试迁移风险：旧测试依赖 `X-User-Id` / `X-Store-Id`，一次性迁移成本高。通过 test profile fallback 和统一 test helper 分阶段解决。
- dev/test fallback 与 prod 隔离风险：如果 profile 控制不严，header 伪造可能进入生产。必须用 prod-like 测试固定该边界。
- 权限放 token 的实时性问题：用户权限变更后旧 token 在过期前仍有效。第一阶段用短 token 有效期接受该取舍，后续用 `permissionVersion` 或实时查库增强。
- userType/accountType 缺失风险：短期角色推断端类型会耦合职责和入口。后续正式账号体系建议补字段。
- 前后端分阶段导致短期混合认证状态风险：后端 M15B 完成后，admin-web 和 mini-program 尚未接入 token。通过 dev/test fallback 保持联调，prod-like 环境以 JWT 为准。
- 权限注解覆盖风险：高风险接口遗漏注解会形成权限空洞。M15B-2 应以清单驱动补测试，不靠人工记忆。

## 14. 最终建议

1. 建议采用 Spring Security + JWT，作为后端正式认证授权基座。
2. 建议保留 `CurrentUserContext`，作为业务上下文门面，避免业务代码直接依赖 Spring Security。
3. M15B 应先完成 docs/design/spec，再进入实现；本文件即为 M15B-Spec 输出。
4. M15B 实现建议先做 Security 基座，再做权限注解接入。不要把首次 Spring Security 接入和所有高风险接口授权一次性混在一个不可控变更里。
5. 本阶段不建议做 refresh token。access token + 重新登录足够支撑 MVP，能避免黑名单、续期、多端踢出等复杂问题。
6. 本阶段不建议同时做 admin-web / mini-program 登录。前端 token 接入应放到 M15C / M15D，避免后端安全基座未稳定时扩散改动面。
