# 腾讯云生产部署步骤

> 最后更新：2026-05-24
> 范围：单门店 MVP，不引入 K8s、微服务、Redis、MQ 或复杂 CI/CD。

## 执行顺序

1. 等 ICP 备案通过，配置 API 域名和管理后台域名解析。
2. 创建并校验腾讯云 MySQL 账号，见 `docs/deploy/tencent-cloud-mysql-bootstrap.md`。
3. 在服务器配置生产环境变量，见 `docs/deploy/production-env-vars.md`。
4. 构建后端 jar，并以 `SPRING_PROFILES_ACTIVE=prod` 启动。
5. 观察启动日志，确认 Flyway 只执行 `classpath:db/migration`，不执行 `db/dev-migration`。
6. 确认 `flyway_schema_history` 中 V1-V13 成功。
7. 构建 `admin-web`，设置 `VITE_API_BASE_URL=https://api.xxx.com`。
8. Nginx 托管 admin-web 静态文件，并把 API 域名反向代理到后端。
9. 小程序切换 `API_ENV='prod'`，确认 `PROD_BASE_URL=https://api.xxx.com`。
10. 按一次性管理员初始化方案创建首个管理员账号。
11. 登录后立即修改初始密码，验证基础业务链路。

## 一次性生产管理员初始化

本方案采用“一次性 SQL 模板 + 本地生成 bcrypt hash”，不新增 prod migration，不提交真实密码或 hash，不影响 dev/test seed。

### 1. 本地生成 bcrypt hash

可用 Spring Security 的 delegating password 格式生成，最终值应形如 `{bcrypt}$2a$10$...`。

```bash
cd backend
mvn -q -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=/tmp/niu-backend-cp.txt
jshell --class-path "target/classes:$(cat /tmp/niu-backend-cp.txt)" <<'EOF'
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
System.out.println(encoder.encode("<TEMP_ADMIN_PASSWORD>"));
EOF
```

输出应自带 `{bcrypt}` 前缀。不要把明文密码或 hash 写入仓库。

### 2. 一次性 SQL 模板

确认 `sys_role` 中 `SUPER_ADMIN` 已由 V2/V11 创建。生产初始化推荐创建平台管理员账号，`store_id` 为空，`account_type='PLATFORM'`。

```sql
START TRANSACTION;

INSERT INTO sys_user (
    store_id, username, password_hash, real_name, phone,
    status, password_must_change, password_changed_at,
    account_type, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES (
    NULL, '<ADMIN_USERNAME>', '<BCRYPT_HASH_WITH_PREFIX>', '<ADMIN_REAL_NAME>', '<ADMIN_PHONE>',
    'ENABLED', 1, NULL,
    'PLATFORM', '一次性生产初始化管理员',
    NULL, NOW(), NULL, NOW(), 0
);

SET @admin_user_id = LAST_INSERT_ID();

INSERT INTO sys_user_role (user_id, role_id, created_by, created_at)
SELECT @admin_user_id, id, NULL, NOW()
FROM sys_role
WHERE role_code = 'SUPER_ADMIN'
  AND deleted = 0
LIMIT 1;

COMMIT;
```

### 3. 初始化后处理

1. 用初始账号登录管理后台。
2. 立即修改密码，确认 `password_must_change` 已解除。
3. 删除本地临时密码记录和 SQL 执行草稿。
4. 不保留任何一次性入口；本方案没有新增后端接口、seed 或 migration，因此无需清理代码。
