# Release-based production deploy

> Scope: single-store MVP production deployment on Tencent Cloud. This does not enable formal domain HTTPS and does not submit the mini-program for review.

## Directory layout

Production releases live under `/opt/niu/releases`. Runtime secrets stay outside releases.

```text
/opt/niu/releases/<release_id>/backend/app.jar
/opt/niu/releases/<release_id>/admin-web/dist
/opt/niu/releases/<release_id>/REVISION
/opt/niu/current -> /opt/niu/releases/<release_id>

/opt/niu/backend/env/niu-backend.env
```

`/opt/niu/backend/env/niu-backend.env` is the only production backend environment file. It must not be copied into a release and must not be committed to git.

## systemd template

`niu-backend.service` should point at `/opt/niu/current`, while keeping the environment file independent:

```ini
[Unit]
Description=Niu Backend Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/niu/current/backend
EnvironmentFile=/opt/niu/backend/env/niu-backend.env
ExecStart=/usr/bin/java -jar /opt/niu/current/backend/app.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Do not copy this into `/etc/systemd/system/` or restart the service without Owner confirmation.

## Nginx template

Admin static files should also point at `/opt/niu/current`:

```nginx
server {
    listen 80;
    server_name admin.example.com;

    root /opt/niu/current/admin-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Enable HTTPS only after ICP, DNS, and SSL are ready.
```

Do not enable this Nginx configuration or reload Nginx until the domain and ICP work is ready.

## First deploy

Set SSH parameters locally. Example:

```bash
export NIU_SSH_HOST=175.24.130.183
export NIU_SSH_USER=ubuntu
export NIU_SSH_KEY=~/.ssh/niu.pem
export NIU_SERVER_BASE=/opt/niu
# Optional when local default Java is not 17:
export NIU_JAVA_HOME=/opt/homebrew/opt/openjdk@17
```

Preview commands:

```bash
scripts/deploy-prod-release.sh --dry-run
```

Deploy a release and switch `/opt/niu/current` without restarting backend:

```bash
scripts/deploy-prod-release.sh
```

Deploy and restart backend only when Owner has confirmed:

```bash
scripts/deploy-prod-release.sh --restart
```

The release id format is:

```text
YYYYMMDDHHMMSS-<short_commit>
```

Each release writes a `REVISION` file containing release id, full commit hash, UTC creation time, and source branch.

## Rollback

List releases:

```bash
scripts/rollback-prod-release.sh
```

Switch `/opt/niu/current` to a known release without restarting:

```bash
scripts/rollback-prod-release.sh --release <release_id>
```

Switch and restart backend only after Owner confirmation:

```bash
scripts/rollback-prod-release.sh --release <release_id> --restart
```

Rollback never deletes release directories.

## Migration boundary

Flyway migrations run when the Spring Boot backend starts with the production profile. Code rollback does not automatically roll back database schema or data.

Before starting a new backend against production:

1. Confirm the target database and environment file.
2. Confirm whether the release contains new Flyway migrations.
3. Confirm rollback risk if migrations are not backward compatible.

For this MVP, avoid destructive migration patterns. Do not run `DROP`, `TRUNCATE`, manual cleanup, or ad hoc schema reversal unless Owner explicitly approves a reviewed recovery plan.

## Current phase limits

- Do not enable official domain HTTPS before ICP, DNS, and SSL are ready.
- Do not submit the mini-program for review in this phase.
- Do not store real DB passwords, JWT secrets, or WeChat secrets in git or release artifacts.
