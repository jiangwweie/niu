# 生产环境变量清单

> 最后更新：2026-05-24

## 后端 Spring Boot

| 变量 | 必须 | 示例 | 说明 |
|------|------|------|------|
| `SPRING_PROFILES_ACTIVE` | 是 | `prod` | 必须显式启用生产 profile |
| `SERVER_PORT` | 否 | `8080` | 后端监听端口，默认 8080 |
| `DB_HOST` | 是 | `<tencent-mysql-host>` | 腾讯云 MySQL 内网或外网地址 |
| `DB_PORT` | 否 | `3306` | 数据库端口，默认 3306 |
| `DB_NAME` | 是 | `niu_prod` | 生产业务库名 |
| `DB_USERNAME` | 是 | `niu_app` | Spring Boot 日常运行账号 |
| `DB_PASSWORD` | 是 | `<NIU_APP_PASSWORD>` | 不提交到 git |
| `DB_MIGRATION_USERNAME` | 是 | `niu_migration` | Flyway 独立迁移账号 |
| `DB_MIGRATION_PASSWORD` | 是 | `<NIU_MIGRATION_PASSWORD>` | 不提交到 git |
| `JWT_SECRET` | 是 | `<random-48-byte-base64>` | 必须 >= 32 字符，prod 缺失或使用弱示例会启动失败 |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | 否 | `14400` | 访问令牌有效期 |
| `CORS_ALLOWED_ORIGINS` | 是 | `https://admin.xxx.com` | 多个源用逗号分隔；prod 禁止 `*` |
| `WX_MINIAPP_APPID` | 是 | `<wx-appid>` | 微信小程序 AppID |
| `WX_MINIAPP_SECRET` | 是 | `<wx-secret>` | 微信小程序 AppSecret，不提交到 git |

生成 JWT secret 示例：

```bash
openssl rand -base64 48
```

## admin-web

| 变量 | 必须 | 示例 | 说明 |
|------|------|------|------|
| `VITE_API_BASE_URL` | 是 | `https://api.xxx.com` | 生产构建时注入，浏览器直连后端 API 域名 |

本地开发可不设置，默认使用 Vite `/api` 代理。

## mini-program

小程序没有运行时环境变量。本项目在 `mini-program/src/utils/config.ts` 里维护：

- `API_ENV='dev'`：使用 `http://localhost:8080`，用于本地开发。
- `API_ENV='prod'`：使用 `https://api.xxx.com`，用于生产体验版/正式版。

生产发布前需要把 `PROD_BASE_URL` 替换为真实 HTTPS API 域名，并在微信公众平台配置 request 合法域名。

