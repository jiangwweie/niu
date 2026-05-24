# 生产部署准备检查清单

> 最后更新：2026-05-24
> 目标：单门店 MVP 试运行上线

## 1. 后端配置

- [x] 已新增 `backend/src/main/resources/application-prod.yml`。
- [x] prod profile 数据库连接使用 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。
- [x] Flyway 支持独立迁移账号 `DB_MIGRATION_USERNAME`、`DB_MIGRATION_PASSWORD`。
- [x] prod Flyway 路径只包含 `classpath:db/migration`，不包含 `db/dev-migration`。
- [x] prod JWT secret 使用 `JWT_SECRET`，缺失或弱 secret 会启动失败。
- [x] CORS 使用 `CORS_ALLOWED_ORIGINS`，prod profile 禁止配置为 `*`。
- [x] 微信小程序配置使用 `WX_MINIAPP_APPID`、`WX_MINIAPP_SECRET`。

## 2. 前端配置

- [x] `admin-web` 支持 `VITE_API_BASE_URL=https://api.xxx.com`。
- [x] `mini-program` 支持 `API_ENV='dev' | 'prod'` 切换，生产地址模板为 `https://api.xxx.com`。
- [ ] 生产发布前替换真实 API 域名，并在微信公众平台配置 request 合法域名。

## 3. 数据库

- [x] 腾讯云 MySQL 已创建业务库 `niu_prod`。
- [x] V1-V13 migration 可在空 MySQL 业务库初始化。
- [x] 已检查 migration 不包含 `DROP`、`TRIGGER`、`EVENT`、`PROCEDURE`、`FUNCTION`、`LOCK TABLES`。
- [x] 已输出 `niu_migration` 和 `niu_app` 最小授权模板，见 `docs/deploy/tencent-cloud-mysql-bootstrap.md`。
- [x] 明确禁止在腾讯云 SQL 窗口手动创建业务表。
- [ ] 执行腾讯云账号创建 SQL 模板。
- [ ] 使用 `SHOW GRANTS` 检查权限。
- [ ] 首次 prod 启动后确认 `flyway_schema_history` 记录 V1-V13。

## 4. 生产管理员账号

- [x] 不在 prod migration 中写默认 admin 密码。
- [x] 不提交真实密码或 hash。
- [x] 采用一次性 SQL 模板 + 本地生成 bcrypt hash 方案。
- [x] 初始化后无需保留任何一次性入口；只需删除本地临时密码和 SQL 草稿。

详见 `docs/deploy/tencent-cloud-production-deploy.md`。

## 5. 部署架构

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

## 6. 环境变量清单

最终清单见 `docs/deploy/production-env-vars.md`。生产必填项：

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME=niu_prod`
- `DB_USERNAME=niu_app`
- `DB_PASSWORD`
- `DB_MIGRATION_USERNAME=niu_migration`
- `DB_MIGRATION_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `WX_MINIAPP_APPID`
- `WX_MINIAPP_SECRET`
- `VITE_API_BASE_URL=https://api.xxx.com`

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
