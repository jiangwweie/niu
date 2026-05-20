# M20A 微信 OpenID 绑定与小程序快捷登录 Smoke 文档

## 目标

为小程序员工端提供微信快捷登录，员工可绑定微信 OpenID 后免输账号密码快速登录后台管理系统。

### 不做内容

- 手机号一键登录
- getPhoneNumber
- 微信支付
- 客户小程序
- 自动创建员工账号

---

## 技术方案

### WxJava code2Session 说明

后端使用 `com.github.binarywang:weixin-java-miniapp:4.7.5.B`（WxJava SDK），通过 `wxMaService.getUserService().getSessionInfo(code)` 调用微信 `jscode2session` 接口，获取用户的 `openid`。

- `session_key` 仅用于本次请求，**不落库、不返回前端、不打印日志**
- `openid` 唯一标识小程序用户，存储在 `sys_user.wechat_openid` 字段
- `appSecret` 仅存在于后端环境变量 `WX_MINIAPP_SECRET`，不进入 Git

---

## 流程说明

### OpenID 登录流程

1. 小程序调用 `wx.login()` 获取临时 `code`
2. 小程序调用 `POST /api/auth/login/wechat`（`{ "code": "..." }`）
3. 后端用 `code` 调用微信 `jscode2session` 获取 `openid`
4. 后端在 `sys_user` 中查找 `wechat_openid = openid` 且 `deleted = 0` 的记录
5. 校验：未绑定 → 401 `WECHAT_NOT_BOUND`；PLATFORM 账号 → 401；已停用 → 401
6. 校验通过 → 生成 JWT，返回 `LoginResponse`

### STORE 用户绑定微信流程

1. 用户已通过账号密码登录小程序
2. 进入「我的」页面，点击「绑定微信」
3. 小程序调用 `wx.login()` 获取 `code`
4. 小程序调用 `POST /api/auth/wechat/bind`（需 Bearer Token）
5. 后端校验：用户存在、非 PLATFORM、未绑定、openid 未被其他用户占用
6. 校验通过 → 写入 `wechat_openid` 和 `wechat_bound_at`
7. 前端直接更新本地用户状态（后端已校验成功，无需重新请求 `/api/auth/me`）

### 管理员解绑微信流程

1. 管理员在 admin-web 用户管理页查看用户列表，「微信绑定」列显示绑定状态
2. 有 `USER_MANAGE` 权限的用户可看到「解绑微信」按钮
3. 点击后二次确认弹窗，确认后调用 `POST /api/admin/users/{id}/wechat/unbind`
4. 后端清除 `wechat_openid`、`wechat_unionid`、`wechat_bound_at`
5. 该用户无法再使用微信快捷登录

---

## 验收点

### admin-web 验收点

1. 用户列表新增「微信绑定」列，显示已绑定/未绑定 tag
2. 已绑定用户显示绑定日期（YYYY-MM-DD）
3. 操作列「解绑微信」按钮：仅当 `wechatBound=true` 且当前用户有 `USER_MANAGE` 权限时显示
4. 解绑前弹出二次确认，说明解绑后果
5. 解绑成功后列表刷新，该用户微信绑定状态变为「未绑定」
6. vue-tsc 零错误，build 成功

### mini-program 验收点

1. 登录页新增「微信快捷登录」按钮，与密码登录之间有分隔线
2. 点击微信快捷登录 → 调用 `wx.login` → 调用 `/api/auth/login/wechat` → 成功后进入工作台
3. 未绑定用户点击微信快捷登录 → 弹出「请先使用账号密码登录并绑定微信」提示，留在登录页
4. 「我的」页面显示微信绑定状态
5. 未绑定时显示「绑定微信」按钮，点击后调用绑定接口，成功后刷新状态
6. 已绑定时显示「已绑定微信」和绑定时间
7. 不展示完整 openid

---

## 安全边界

| 项目 | 说明 |
|------|------|
| appSecret | 仅存在于后端环境变量 `WX_MINIAPP_SECRET`，不进入 Git |
| openid | 不展示给前端，不打印完整 openid 到日志 |
| session_key | 不落库，不返回前端，不打印到日志 |
| PLATFORM 账号 | 禁止微信绑定和微信登录（后端硬拦截） |
| disabled/deleted 用户 | 微信登录失败，返回 401 |
| openid 唯一约束 | `UNIQUE KEY uk_user_wechat_openid` 数据库层保证 |
| 跨门店解绑 | 管理员只能解绑自己门店的用户（storeId 校验） |

---

## 相关文件

### 后端

- `backend/src/main/java/.../auth/config/WxMaConfig.java` — WxMaService Bean 配置
- `backend/src/main/java/.../auth/service/WechatService.java` — 微信登录/绑定/解绑核心逻辑
- `backend/src/main/java/.../auth/controller/AuthController.java` — `/api/auth/login/wechat`、`/api/auth/wechat/bind`
- `backend/src/main/java/.../user/controller/AdminUserController.java` — `/api/admin/users/{id}/wechat/unbind`
- `backend/src/main/resources/db/migration/V12__add_wechat_bound_at.sql` — 数据库迁移
- `backend/src/test/java/.../auth/controller/WechatAuthTest.java` — 13 个集成测试

### admin-web

- `admin-web/src/types/userPermission.ts` — `wechatBound`、`wechatBoundAt` 字段
- `admin-web/src/api/userPermission.ts` — `unbindWechat(id)` API
- `admin-web/src/views/user/index.vue` — 微信绑定列 + 解绑按钮

### mini-program

- `mini-program/src/types/auth.ts` — `wechatBound`、`wechatBoundAt` 字段
- `mini-program/src/api/auth.ts` — `wechatLogin(code)`、`bindWechat(code)`
- `mini-program/src/pages/login/index.ts` — 微信快捷登录处理
- `mini-program/src/pages/login/index.wxml` — 微信快捷登录按钮
- `mini-program/src/pages/mine/index.ts` — 绑定微信处理
- `mini-program/src/pages/mine/index.wxml` — 绑定状态展示
