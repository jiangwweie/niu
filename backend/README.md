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

Flyway runs automatically on application startup for non-test profiles. Create the empty schema first, then let Flyway apply versioned migrations:

```sql
CREATE DATABASE IF NOT EXISTS xiaoniu_aftermarket
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Migration files live under `src/main/resources/db/migration/`.

## Authentication & Authorization

- JWT-based authentication with HMAC-SHA256 signing.
- Access token TTL is configurable via `SECURITY_JWT_ACCESSTOKENTTLSECONDS` (default 4h).
- **`JWT_SECRET` must be set in production** — the app will refuse to start with a dev placeholder secret outside dev/test profiles.
- Permissions are snapshot into the JWT at login time. Changes to a user's permissions take effect only after the token expires and is re-issued. If immediate revocation is needed (e.g. employee offboarding), a token version or blacklist mechanism must be introduced.
- Logout is client-side only (discard the token). Server-side token invalidation is not yet implemented.
