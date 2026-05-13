# MySQL Dev 环境搭建指南

本文档说明如何在本地使用 MySQL dev profile 运行后端服务。

## 1. 创建数据库

使用 Docker 启动 MySQL 8.0 容器：

```bash
docker run -d --name xiaoniu-mysql-dev \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=xiaoniu_aftermarket_dev \
  -p 3306:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci
```

首次启动后需要修复 JDBC 兼容性（`caching_sha2_password` -> `mysql_native_password`）：

```bash
docker exec xiaoniu-mysql-dev mysql -uroot -proot -e \
  "ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'root'; FLUSH PRIVILEGES;"
```

## 2. 环境变量配置

后端默认配置已写在 `application-dev.yml`，无需设置环境变量即可连接本地 Docker MySQL：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/xiaoniu_aftermarket_dev?...` | 数据库连接 URL |
| `MYSQL_USERNAME` | `root` | 数据库用户名 |
| `MYSQL_PASSWORD` | `root` | 数据库密码 |
| `SERVER_PORT` | `8080` | 服务端口 |

如果需要自定义，启动前设置环境变量即可：

```bash
export MYSQL_URL="jdbc:mysql://localhost:3306/xiaoniu_aftermarket_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
```

## 3. 用 dev profile 启动

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
cd backend
mvn clean package -DskipTests
java -jar target/xiaoniu-aftermarket-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 4. Flyway Migration

Flyway 在 dev profile 下自动启用。启动应用时会自动执行所有迁移脚本（V1-V4）。

手动执行 Flyway migration（如果需要）：

```bash
cd backend
mvn flyway:migrate -Dspring.profiles.active=dev
```

迁移脚本位于 `src/main/resources/db/migration/`：

- `V1__init_schema.sql` - 全部 22 张表建表
- `V2__init_seed_data.sql` - 门店、角色、权限、字典等基础数据
- `V3__add_inventory_flow_unit_cost.sql` - 库存流水平均成本字段
- `V4__add_work_order_received_amount.sql` - 工单实收金额字段

## 5. 最小 Smoke 测试

后端启动后，运行 smoke 脚本验证核心链路：

```bash
cd backend
bash scripts/smoke-mysql-dev.sh
```

Smoke 覆盖的链路：

1. 健康检查
2. 创建第三方配件
3. 普通入库（INBOUND 流水）
4. 创建 DRAFT 工单（含 PART / LABOR / OTHER 费用项）
5. 提交工单（RESERVE 流水，available 减少，reserved 增加）
6. 记录支付
7. 结算工单（CONSUME 流水，actual 减少，reserved 减少）
8. 取消未结算工单（RELEASE 流水，reserved 释放，available 恢复）

## 6. Dev-only Header 使用方式

所有 `/api/admin/**` 接口需要以下 HTTP Header：

| Header | 类型 | 说明 |
|--------|------|------|
| `X-User-Id` | Long | 当前操作用户 ID |
| `X-Store-Id` | Long | 当前门店 ID |

示例：

```bash
curl -H "X-User-Id: 1" -H "Store-Id: 1" http://localhost:8080/api/admin/work-orders
```

> **重要说明**：`X-User-Id` / `X-Store-Id` 是 dev-only 的临时方案，**不是正式认证系统**。后续会接入真实 Auth 中间件替代。

## 7. 常见问题

### MySQL 容器启动失败

确保 3306 端口未被占用：

```bash
lsof -i :3306
```

### Flyway migration 失败

检查数据库是否存在：

```bash
docker exec xiaoniu-mysql-dev mysql -uroot -proot -e "SHOW DATABASES;"
```

### H2 测试不受影响

MySQL dev profile 和 H2 test profile 完全隔离：

- `application-dev.yml` - MySQL（dev profile）
- `application-test.yml` - H2 in-memory（test profile）
- 运行 `mvn test` 默认使用 test profile，不会连接 MySQL

### 重置数据库

```bash
docker exec xiaoniu-mysql-dev mysql -uroot -proot xiaoniu_aftermarket_dev -e "DROP DATABASE xiaoniu_aftermarket_dev; CREATE DATABASE xiaoniu_aftermarket_dev;"
```

重启应用即可重新执行 Flyway migration 和 seed data。
