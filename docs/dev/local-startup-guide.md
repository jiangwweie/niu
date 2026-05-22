# 本地启动与登录指南

> 最后更新：2026-05-22

## 一、环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| Java | 17 | OpenJDK 17.0.18 |
| Maven | 3.x | /opt/homebrew/bin/mvn |
| Node.js | 18+ | admin-web 和 mini-program |
| MySQL | 8.0 | Docker 容器 `xiaoniu-mysql-dev` |
| 微信开发者工具 | 最新稳定版 | 小程序调试 |

## 二、启动步骤

### 1. 启动 MySQL

```bash
docker start xiaoniu-mysql-dev
```

- 容器名：`xiaoniu-mysql-dev`
- 端口：3306
- 数据库：`xiaoniu_aftermarket_dev`
- 用户名/密码：`root` / `root`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 激活 profile：`dev`（application.yml 默认）
- 端口：8080
- Flyway 自动执行迁移（当前 V1-V13）
- 健康检查：`curl http://localhost:8080/api/health`

### 3. 启动 admin-web

```bash
cd admin-web
npm install   # 首次
npm run dev
```

- 端口：3000
- Vite proxy：`/api` → `http://localhost:8080`
- 登录页：`http://localhost:3000/login`

### 4. 打开小程序

1. 打开微信开发者工具
2. 导入项目，选择 `mini-program/` 目录
3. AppID：`wxe828b5fb41d2ceb1`（或使用测试号）
4. 勾选"详情 → 本地设置 → 不校验合法域名"
5. 首次需在 DevTools 中执行"工具 → 构建 npm"

当前 `API_MODE` 已设为 `real`，`BASE_URL` 为 `http://localhost:8080`。

真机调试时需将 `BASE_URL` 改为电脑局域网 IP（如 `http://192.168.39.112:8080`），手机与电脑在同一局域网。

## 三、账号密码

### 试运行账号（M16 初始化）

| 账号 | 密码 | 角色 | 用途 | 入口 |
|---|---|---|---|---|
| system_admin | Trial@2026! | SUPER_ADMIN | 软件商/系统拥有者超管 | admin-web |
| trial_store_admin | Trial@2026! | STORE_ADMIN | 门店管理员试运行 | admin-web |
| trial_finance | Trial@2026! | FINANCE | 财务报表/导出验证 | admin-web |
| trial_staff | Trial@2026! | TECHNICIAN_FRONT_DESK | 员工小程序主链路 | mini-program |

注意事项：
- 示例密码仅用于试运行，首次登录后必须修改
- system_admin 是软件商/系统拥有者超管，不是普通门店员工
- 当前绑定 store_id=1 是单门店 MVP 兼容
- SQL 文件：`docs/dev/m16-trial-initial-users.sql`（手动执行，不在 Flyway 中）

### 开发环境账号（dev 种子）

| 账号 | 密码 | 角色 | 用途 | 入口 |
|---|---|---|---|---|
| admin01 | dev123 | STORE_ADMIN | 开发环境门店管理员 | admin-web |
| tech01 | dev123 | TECHNICIAN_FRONT_DESK | 开发环境技师 | mini-program |
| front01 | dev123 | TECHNICIAN_FRONT_DESK | 开发环境前台 | mini-program |

注意事项：
- 使用 `{noop}` 明文密码，仅适合开发环境
- 不作为正式试运行账号

## 四、登录验证要点

- 登录接口：`POST /api/auth/login/password`，请求体 `{"username":"xxx","password":"xxx"}`
- 返回 `accessToken`（Bearer 类型）+ `user`（含 userId、storeId、username、realName、roleCodes、permissionCodes）
- admin-web：token 存 localStorage，401 清 token 跳 /login，403 提示无权限
- mini-program：token 存 wx storage，401 清 token 跳登录页，403 toast 提示无权限（不清 token）
- 认证方式：`Authorization: Bearer <token>`，不再发送 X-User-Id / X-Store-Id

## 五、Smoke 测试

后端启动后，运行 smoke 脚本验证核心业务链路（JWT 登录 → 配件 → 入库 → 工单 → 预占 → 支付 → 结算 → 取消释放）：

```bash
cd backend
bash scripts/smoke-mysql-dev.sh
```

默认使用 dev seed 账号 `admin01` / `dev123`，可通过环境变量覆盖：

```bash
DEV_LOGIN_USERNAME=admin01 DEV_LOGIN_PASSWORD=dev123 bash scripts/smoke-mysql-dev.sh
```

## 六、MySQL 8 认证说明

`application-dev.yml` 已配置 `allowPublicKeyRetrieval=true`，兼容 MySQL 8 默认的 `caching_sha2_password` 认证，无需手动切换为 `mysql_native_password`。

## 七、已知问题

以下问题待后续迭代处理：

1. **小程序工作台仍是占位页面** — dashboard 页面内容为 placeholder，无实际数据展示
2. **新建工单按钮位置不友好** — 按钮在最底部，用户需要滚动才能找到
3. **入库按钮位置不友好** — 按钮在最底部，操作不便
4. **姓名显示乱码** — 用户真实姓名在某些页面显示为乱码（可能是字符编码问题）
5. **快捷操作/提交报销/退出登录按钮不显眼** — UI 样式不佳，按钮视觉层级不够
6. **整体 UI 粗糙** — 小程序端整体界面缺乏设计感，需要系统性 UI 优化
