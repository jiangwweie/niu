# Admin Web Auth Token 接入 (M15C) Smoke Test 报告

## 环境配置
- **端**: admin-web
- **API_MODE**: `real` (对接后端本地开发环境)

## 测试目标
验证 admin-web 前端能够正确使用 JWT Token 作为登录凭证，替换之前的硬编码 Header，同时对接路由守卫进行鉴权拦截与错误跳转。

## 测试步骤与验证结果

### 1. 登录功能与 Token 存储
- **场景**: 在未登录态下进入系统，输入合法的账号密码。
- **验证点与结果**:
  - [x] 未登录（无 `accessToken`）访问除 `/login` 外的任何路径（如 `/dashboard`），均会被前端路由守卫拦截并携带 `redirect` 参数重定向至 `/login`。
  - [x] 在 `/login` 页面输入正确的账号（如 admin）与密码，点击登录发起 `POST /api/auth/login/password`。
  - [x] 登录成功后，前端获取到 `LoginResponse`，将其中的 `accessToken` 存入 `localStorage`，并在 Pinia Store `auth` 中保存完整的用户信息 (`username`, `realName`, `roleCodes`, `permissionCodes` 等)。
  - [x] 页面自动跳转至原本想要访问的目标页，或者默认跳转至首页 `/`。

### 2. Header 注入与请求机制
- **场景**: 登录后，触发其他页面数据的拉取请求（如查询工单或财务报表）。
- **验证点与结果**:
  - [x] 移除了原有的写死 `X-User-Id` / `X-Store-Id`。
  - [x] 全局 `request.ts` 拦截器正确地在每个 HTTP 请求的 Headers 中带上了 `Authorization: Bearer <accessToken>`。

### 3. 未授权(401) 与 无权限(403) 拦截处理
- **场景**: 模拟请求接口返回 401 或 403 错误。
- **验证点与结果**:
  - [x] **401**: 如果用户由于后端服务重启、Token 篡改或过期导致后端返回 401 `UNAUTHORIZED`，拦截器能立即清理本地所有授权态并强行重定向至 `/login`。
  - [x] **403**: 当前端无权限按钮却试图调用高危接口（如 `POST /api/admin/inventory/adjust`）被后端拦截触发 403 `FORBIDDEN`，拦截器能在当前页面以 `ElMessage.error` 弹出“无权限访问该资源”，但不执行踢出跳转行为，保护当前上下文。

### 4. 路由守卫与 /me 接口自愈
- **场景**: 用户已登录并有 Token，刷新当前页面。
- **验证点与结果**:
  - [x] 刷新页面后，内存中的 `user` 对象丢失，但 `localStorage` 中的 `accessToken` 还在。
  - [x] 路由守卫 `router.beforeEach` 检测到无 `authStore.user`，即自动同步阻断并调用 `GET /api/auth/me` 以重构用户信息。
  - [x] 如果 `/me` 成功则放行；如果失败（Token失效）则走 401 报错流程并跳转至登录页。

### 5. 退出登录
- **场景**: 在右上角个人头像下拉框点击“退出登录”。
- **验证点与结果**:
  - [x] 前端调用 `POST /api/auth/logout` 接口通知后端。
  - [x] 无论后端结果如何，前端立刻销毁 Pinia store 和 `localStorage` 中的 token 数据。
  - [x] 页面瞬间跳回 `/login`。

## 当前强边界与仍未实现内容 (留待 M15E)
为遵守迭代节奏和克制要求，本次 M15C **并未实施** 以下功能：
- **无基于权限码 (permissionCodes) 的左侧菜单动态渲染过滤**。
- **无针对页面按钮（如导出、确认、调剂）的指令级显隐拦截 (`v-has-permission`)**。
- **未正式开放用户管理与角色编辑页面的后端持久化数据拉取**。
- **未做 Refresh Token 与自动续期。Token 过期即要求重新登录**。

## 结论
**PASS**. M15C 目标已全部达成，JWT 前后端完整闭环贯通。原先各业务页面（如财务、库存）能够继续在持有合法 Token 的情况下降级兼容使用。
