# 腾讯云 MySQL 生产库初始化

> 最后更新：2026-05-24
> 目标库：`niu_prod`

## 前提

- 腾讯云 MySQL 实例已购买。
- 业务库 `niu_prod` 已创建。
- 业务表结构继续由项目内 Flyway migration 管理。
- 不要在腾讯云 SQL 窗口手动创建业务表。
- 不要把真实密码、JWT secret、微信 secret 写入文档或提交到 git。

## Migration 权限检查结论

已检查 `backend/src/main/resources/db/migration` 下 V1-V13。

- 存在：`CREATE TABLE`、`ALTER TABLE`、普通索引/唯一索引、`INSERT`、`UPDATE`、`SELECT`。
- 不存在：`DROP`、`TRIGGER`、`EVENT`、`PROCEDURE`、`FUNCTION`、`LOCK TABLES`、`CREATE VIEW`、`SHOW VIEW`。
- V5 位于 `db/dev-migration`，只应在 dev profile 运行，不属于生产 migration 路径。

因此 `niu_migration` 最小权限建议为：

```sql
SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON niu_prod.*
```

当前 migration 没有显式 `DELETE`，但 Flyway schema history 和未来数据修正 migration 可能需要 DML 完整能力；若 Owner 要进一步收紧，可以先去掉 `DELETE`，待实际 migration 需要时再临时授权。

## 创建生产账号 SQL 模板

以下模板只能由腾讯云 MySQL 管理账号或控制台授权能力执行。密码必须替换为临时生成的强密码，不要使用真实密码提交到 git。

```sql
-- 1. Flyway 迁移账号：仅用于建表和升级，不给应用运行使用。
CREATE USER 'niu_migration'@'%' IDENTIFIED BY '<NIU_MIGRATION_PASSWORD>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON niu_prod.*
TO 'niu_migration'@'%';

-- 2. Spring Boot 运行账号：仅用于日常业务读写，不具备 DDL 权限。
CREATE USER 'niu_app'@'%' IDENTIFIED BY '<NIU_APP_PASSWORD>';

GRANT SELECT, INSERT, UPDATE, DELETE
ON niu_prod.*
TO 'niu_app'@'%';

FLUSH PRIVILEGES;
```

禁止授予：

- `ON *.*`
- `GRANT OPTION`
- `CREATE USER`
- `DROP`
- `RELOAD`
- `PROCESS`
- `SHOW DATABASES`
- 任何全局高危权限

## 权限检查

```sql
SHOW GRANTS FOR 'niu_migration'@'%';
SHOW GRANTS FOR 'niu_app'@'%';
```

期望结果：

- `niu_migration` 只出现 `niu_prod.*` 上的 migration 所需权限。
- `niu_app` 只出现 `niu_prod.*` 上的 `SELECT, INSERT, UPDATE, DELETE`。
- 不应出现 `*.*`、`WITH GRANT OPTION` 或全局管理权限。

## 腾讯云控制台执行顺序

1. 确认数据库 `niu_prod` 已创建，字符集使用 `utf8mb4`。
2. 在腾讯云 MySQL 控制台或安全 SQL 工具中执行上方账号创建模板。
3. 执行 `SHOW GRANTS` 校验权限。
4. 在服务器环境变量中配置 `DB_USERNAME=niu_app`、`DB_MIGRATION_USERNAME=niu_migration`。
5. 以 `prod` profile 启动后端，由 Flyway 自动执行 `db/migration`。
6. 确认 `flyway_schema_history` 记录 V1-V13 成功。
7. 按“一次性生产管理员初始化”方案创建首个管理员账号。

## 不要手动建业务表

腾讯云 SQL 窗口只用于创建账号、授权、检查权限和必要的一次性管理员初始化。业务表、索引、字段和种子数据必须由 Flyway migration 管理。

