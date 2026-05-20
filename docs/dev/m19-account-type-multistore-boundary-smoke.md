# M19 账号类型与多门店最小边界 Smoke Test

## 1. account_type 设计

### 字段
- `sys_user.account_type VARCHAR(20) NOT NULL DEFAULT 'STORE'`
- 可选值: PLATFORM / STORE

### 规则
| account_type | store_id | 说明 |
|---|---|---|
| PLATFORM | NULL | 软件商/系统拥有者 |
| STORE | 必填 | 门店用户 |

### 迁移
- system_admin → account_type=PLATFORM, store_id=NULL
- 门店账号 → account_type=STORE, store_id 不变

## 2. PLATFORM / STORE 边界

| API 路径 | PLATFORM | STORE |
|---|---|---|
| /api/platform/** | 允许 (PLATFORM_MANAGE) | 403 |
| /api/admin/** | 403 (PlatformAccessGuard) | 允许（原有权限） |
| /api/staff/** | 403 (PlatformAccessGuard) | 允许（原有权限） |
| /api/auth/** | 允许 | 允许 |

## 3. 平台 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/platform/stores | 门店列表 |
| POST | /api/platform/stores | 创建门店（自动创建基础角色） |
| PUT | /api/platform/stores/{id} | 编辑门店 |
| POST | /api/platform/stores/{id}/admin-users | 创建门店管理员 |

### 创建门店自动创建基础角色
- STORE_ADMIN
- FINANCE
- TECHNICIAN_FRONT_DESK
- 权限从默认门店 (store_id=1) 同名角色复制
- 基于 (store_id, role_code) 唯一约束保证幂等

### 创建门店管理员
- accountType=STORE
- storeId=目标门店
- passwordMustChange=true
- 自动绑定 STORE_ADMIN 角色
- 返回一次性初始密码

## 4. 平台账号不操作门店业务

PlatformAccessGuard 统一拦截：
- PLATFORM 账号访问 /api/admin/** → 403 PLATFORM_ACCESS_DENIED
- PLATFORM 账号访问 /api/staff/** → 403 PLATFORM_ACCESS_DENIED
- CurrentUserContext.requireStoreId() 作为防御性兜底

## 5. 新门店角色复制规则

1. 从 store_id=1 的同名角色复制
2. 复制 role_code, role_name, status, sort_order
3. 复制 sys_role_permission 中的 permission_id
4. 不复制用户绑定
5. (store_id, role_code) 唯一约束保证幂等

## 6. 试运行账号迁移

| 账号 | account_type | store_id |
|---|---|---|
| system_admin | PLATFORM | NULL |
| trial_store_admin | STORE | 1 |
| trial_finance | STORE | 1 |
| trial_staff | STORE | 1 |

## 7. 当前不做

- SaaS 套餐 / 计费 / 到期
- 租户付费 / 订阅
- 门店上下文切换（PLATFORM 代操作门店业务）
- 微信绑定 / 支付网关
- 复杂组织架构
- 库存/工单/支付/退款/结算/财务口径改动
- 小程序改动

## 8. Smoke 手动测试

### 登录测试
1. platform_admin 登录 → /api/auth/me 返回 accountType=PLATFORM, storeId=null
2. admin01 登录 → /api/auth/me 返回 accountType=STORE, storeId=1

### 平台 API 测试
1. platform_admin GET /api/platform/stores → 200
2. store_admin01 GET /api/platform/stores → 403
3. platform_admin POST /api/platform/stores → 成功，含基础角色
4. platform_admin POST /api/platform/stores/{id}/admin-users → passwordMustChange=true

### 边界测试
1. platform_admin GET /api/admin/users → 403 PLATFORM_ACCESS_DENIED
2. platform_admin GET /api/staff/... → 403 PLATFORM_ACCESS_DENIED
3. admin01 GET /api/admin/users → 200 (原有功能不受影响)

### 前端测试
1. platform_admin 登录 → 跳转 /platform/stores，只显示门店管理菜单
2. admin01 登录 → 跳转 /dashboard，显示原有门店业务菜单

## 9. 实现说明

### JWT 中不存储 accountType
- accountType 在 JwtProvider.parseAndValidate() 中设为 null
- 每次请求由 JwtAuthenticationFilter 从数据库重建
- storeId 在 JWT 中可为 null（PLATFORM 用户），JwtProvider 使用 asNullableLong 处理

### admin01 测试数据注意
- 测试库中 admin01 (user_id=1) 同时拥有 ADMIN 和 SUPER_ADMIN 角色
- 因此 admin01 实际上有 PLATFORM_MANAGE 权限
- 测试平台访问控制时使用 store_admin01 (user_id=2, role=STORE_ADMIN)，不含 PLATFORM_MANAGE
