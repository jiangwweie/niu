# Xiaoniu Aftermarket Backend

Spring Boot backend skeleton for the single-store MVP.

## Requirements

- JDK 17
- Maven 3.9+
- MySQL 8, when database-backed modules are enabled

## Run

```bash
mvn test
mvn package -DskipTests
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "status": "UP",
    "service": "xiaoniu-aftermarket-backend"
  },
  "traceId": null
}
```

## Database Configuration

`application-dev.yml` reads MySQL settings from environment variables first:

```bash
export MYSQL_URL='jdbc:mysql://localhost:3306/xiaoniu_aftermarket?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=root
```

This task does not create migrations or business tables. If local startup needs a database before the migration task, create only the empty schema:

```sql
CREATE DATABASE IF NOT EXISTS xiaoniu_aftermarket
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```
