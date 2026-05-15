# Mini-program Auth Token 接入 (M15D) Smoke Test 报告

## 环境配置
- **端**: mini-program
- **API_MODE**: `real` (与真实后端通信) / `mock` (兼容本地 Mock)

## 测试目标
验证员工小程序在 `real` 模式下正确使用 JWT Token 作为身份凭证，废弃之前的 X-User-Id 硬编码，对接全局路由拦截与状态清理。

## 测试步骤与验证结果

### 1. 登录与路由守卫
- **场景**: 小程序首次启动（未登录）。
- **验证点与结果**:
  - [x] 所有底部 Tab 页 (`dashboard`, `work-orders`, `inventory`, `mine`) 的 `onShow` 增加鉴权校验。检测到无 `accessToken` 时，强制跳转至 `/pages/login/index`。
  - [x] 登录页面 (`login`) 收集账号密码，调用 `POST /api/auth/login/password`。
  - [x] 登录成功后提取 `accessToken` 与 `user` 保存至 `wx.getStorageSync` 并更新 `authStore`，随后自动回调跳转到原访问路径或工作台。

### 2. Header 注入
- **场景**: 发起业务请求，如加载库存或工单列表。
- **验证点与结果**:
  - [x] 在 `request.ts` 封装中移除了 `X-User-Id` 的注入。
  - [x] 检测到存在 `accessToken` 时，自动于 Header 中附带 `Authorization: Bearer <token>`。
  - [x] mock 模式的独立处理逻辑不受干扰，继续返回本地数据。

### 3. 401 与 403 异常处理
- **场景**: Token 失效，或进行越权操作。
- **验证点与结果**:
  - [x] **401**: 拦截器捕获 401 后立即调用 `authStore.clearAuth()` 销毁本地 Token，提示过期，跳回 `/pages/login/index` 并携带重定向参数。
  - [x] **403**: 后端返回 403 (FORBIDDEN) 时，前端 Toast 提示“无权限访问该资源”，但不会强制跳走，保护当前操作现场。

### 4. 启动恢复 /me
- **场景**: 冷启动小程序。
- **验证点与结果**:
  - [x] `app.ts` 的 `onLaunch` 阶段检查：如果在 real 模式且有 Token 但无 `user`，则隐式调用 `GET /api/auth/me`。
  - [x] 若调用成功，补充写入内存 store。
  - [x] 若调用失败（通常为过期），按 401 处理，最终强制要求重新登录。

### 5. 退出登录
- **场景**: 在“我的”页面点击退出。
- **验证点与结果**:
  - [x] 请求 `POST /api/auth/logout` 销毁服务端（若有 Session 状态）。
  - [x] 前端闭环 `authStore.clearAuth()` 强制清理本地数据，页面随后跳转登录页。
  - [x] “我的”页面在 `real` 模式下自动屏蔽 Mock 用户切换区，只显示真实信息与退出按钮。

## 当前边界与留待事项
本次 M15D 仅仅完成了“有无 Token 的拦截和接通”，未触及以下范畴：
1. **未实施微信特有登录**: 未接入手机号授权或 wx.login 的 openid 静默登录。目前纯粹以“内嵌型应用”使用账号密码。
2. **Refresh Token**: 暂未支持过期静默换发，过期必须重新走账号密码。
3. **按钮级权限控制 (v-if)**: 目前暂未按 `permissionCodes` 过滤如新建工单的入口显隐，只依靠后端 403 阻断（M15E 工作）。
4. **Mock 降级**: 在 `mock` 模式下，登录状态始终为 `true`，用户管理依靠页面列表快速切换，与真实网络隔绝。

## 结论
**PASS**. M15D 小程序 JWT 接入闭环已完成。原有业务（工单、配件、结算等）通过 Bearer 鉴权无损运行，满足交付要求。
