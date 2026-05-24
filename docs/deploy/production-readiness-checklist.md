# 生产部署准备检查清单

> 最后更新：2026-05-24
> 目标：单门店 MVP 试运行上线

---

## 1. 后端配置

### 1.1 application-prod.yml（需创建）

当前只有 `application.yml`（默认 dev）和 `application-dev.yml`，缺少 `application-prod.yml`。

生产环境需通过 `--spring.profiles.active=prod` 启动，需要创建 `src/main/resources/application-prod.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${MYSQL_URL}          # 必须通过环境变量注入
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration   # 不含 dev-migration
    clean-disabled: true
    validate-on-migrate: true

server:
  port: ${SERVER_PORT:8080}
```

**注意**：prod profile 不应包含 `classpath:db/dev-migration`，避免开发种子数据进入生产。

### 1.2 JWT Secret（必须修改）

`application.yml` 中默认值：

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:xiaoniu-aftermarket-dev-secret-change-me-please-2026}
```

**生产必须**通过环境变量 `JWT_SECRET` 覆盖，要求：
- 长度 >= 32 字符
- 随机生成，不可使用示例值
- 生成命令：`openssl rand -base64 48`

### 1.3 CORS 配置（需修改）

`SecurityConfig.java` 硬编码了 localhost 源：

```java
configuration.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "http://localhost:3000",
    "http://localhost:8081"
));
```

生产需要添加实际域名，或改为可配置。建议方案：
- 通过环境变量 `CORS_ALLOWED_ORIGINS` 注入
- 或在 `application-prod.yml` 中配置

### 1.4 微信小程序配置

```yaml
wx:
  miniapp:
    app-id: ${WX_MINIAPP_APPID:}
    app-secret: ${WX_MINIAPP_SECRET:}
```

生产必须设置 `WX_MINIAPP_APPID` 和 `WX_MINIAPP_SECRET`。

---

## 2. 前端配置

### 2.1 admin-web

当前 `vite.config.ts` 的 proxy 仅用于开发模式。生产部署方式：

1. `npm run build` 生成 `dist/`
2. 由 Nginx 承载静态文件 + 反向代理 `/api` 到后端

Nginx 参考配置：

```nginx
server {
    listen 80;
    server_name admin.example.com;  # 替换为实际域名

    location / {
        root /path/to/admin-web/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 2.2 mini-program

`mini-program/src/utils/config.ts` 硬编码了 localhost：

```typescript
export const BASE_URL = 'http://localhost:8080';
```

生产需要改为实际域名（必须 HTTPS，微信小程序要求）：

```typescript
export const BASE_URL = 'https://api.example.com';
```

同时需要在微信公众平台配置服务器域名（request 合法域名）。

---

## 3. 数据库

### 3.1 Flyway Migration

主迁移路径 `db/migration/`：V1-V13（跳过 V5），可在空 MySQL 上完整初始化。

| 版本 | 内容 |
|------|------|
| V1 | 全部 22 张表建表 |
| V2 | 门店、角色、权限、字典种子数据 |
| V3 | 库存流水平均成本字段 |
| V4 | 工单实收金额字段 |
| V6 | 官方售后订单号唯一键 |
| V7 | 报销取消字段 |
| V8 | 报销状态字典拆分 |
| V9 | 账号门店管理字段 |
| V10 | 客户车辆权限 |
| V11 | 账号类型平台支持 |
| V12 | 微信绑定时间 |
| V13 | M24b 状态机清理 |

**V5 说明**：V5 是 `db/dev-migration/` 中的开发种子数据，仅在 dev profile 下运行，不在生产迁移路径中。

### 3.2 生产初始账号

Flyway V2 创建了角色和权限，但**不创建用户账号**。

生产上线后需要手动创建第一个管理员账号。建议：
- 通过 SQL 直接插入（需使用 BCrypt 或 `{noop}` 前缀的密码哈希）
- 或开发一个初始化脚本/接口

示例 SQL（仅供参考，密码需替换）：

```sql
INSERT INTO sys_user (store_id, username, password_hash, real_name, phone, status, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES (1, 'admin', '{bcrypt}$2a$10$...实际哈希...', '管理员', '手机号', 'ENABLED', '初始管理员', NULL, NOW(), NULL, NOW(), 0);

INSERT INTO sys_user_role (user_id, role_id, created_by, created_at)
VALUES (LAST_INSERT_ID(), 1, NULL, NOW());  -- role_id=1 是 SUPER_ADMIN
```

### 3.3 云数据库注意事项

- 腾讯云 MySQL 需确保字符集为 `utf8mb4`
- 排序规则建议 `utf8mb4_0900_ai_ci`
- 确保 Flyway 用户有 DDL 权限（CREATE TABLE, ALTER TABLE 等）
- 生产数据库连接建议使用 SSL

---

## 4. 部署架构

```
                    +------------------+
                    |   微信小程序      |
                    +--------+---------+
                             |
                    HTTPS (443)
                             |
                    +--------v---------+
                    |   腾讯云 Nginx    |
                    |  (SSL 终止)       |
                    +--+-----------+---+
                       |           |
            /api/*     |           |  /*
                       |           |
              +--------v---+  +----v-----------+
              |  后端 :8080 |  | admin-web/dist |
              |  Spring Boot|  | (静态文件)     |
              +--------+---+  +----------------+
                       |
              +--------v---+
              | 云数据库 MySQL|
              | (腾讯云)     |
              +-------------+
```

---

## 5. 环境变量清单

生产部署需要设置以下环境变量：

| 变量 | 必须 | 说明 |
|------|------|------|
| `MYSQL_URL` | 是 | 云数据库 JDBC URL |
| `MYSQL_USERNAME` | 是 | 数据库用户名 |
| `MYSQL_PASSWORD` | 是 | 数据库密码 |
| `JWT_SECRET` | 是 | JWT 签名密钥（>=32 字符随机值） |
| `SERVER_PORT` | 否 | 后端端口，默认 8080 |
| `WX_MINIAPP_APPID` | 小程序需要 | 微信小程序 AppID |
| `WX_MINIAPP_SECRET` | 小程序需要 | 微信小程序 AppSecret |
| `CORS_ALLOWED_ORIGINS` | 视实现 | CORS 允许的源（如已改为可配置） |

---

## 6. 上线前必须完成

- [ ] 创建 `application-prod.yml`
- [ ] 生成并设置 `JWT_SECRET` 环境变量
- [ ] 修改 CORS 配置支持生产域名
- [ ] 配置腾讯云 MySQL 连接信息
- [ ] `npm run build` 构建 admin-web
- [ ] 修改 mini-program `BASE_URL` 为生产域名
- [ ] 微信公众平台配置 request 合法域名
- [ ] Nginx 配置 SSL + 反向代理
- [ ] 创建生产管理员账号
- [ ] Flyway migration 在云数据库执行验证
- [ ] ICP 备案通过后配置域名解析

## 7. 上线后验证

- [ ] admin-web 登录正常
- [ ] 小程序登录正常
- [ ] 工单全流程：创建 → 提交 → 维修完成 → 交付
- [ ] 库存：入库 → 预占 → 扣减 → 释放
- [ ] 支付 → 退款
- [ ] Excel 导出
- [ ] 飞书/钉钉通知（如有配置）
