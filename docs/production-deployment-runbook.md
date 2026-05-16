# Production Deployment Runbook

## 1. Server Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| OS | Ubuntu 22.04 / Debian 12 / Rocky 9 | Ubuntu 24.04 LTS |
| CPU | 2 vCPUs | 4 vCPUs |
| RAM | 2 GB | 4 GB |
| Disk | 20 GB | 40 GB SSD |
| Docker | 24+ | 27+ |
| Docker Compose | 2.20+ | 2.30+ |

Install Docker:

```bash
curl -fsSL https://get.docker.com | sudo bash
sudo usermod -aG docker $USER
# Log out and back in for group membership to take effect
```

## 2. Domain and DNS

1. Purchase a domain (e.g., `example.com`) from any registrar.
2. Add an **A record** pointing to your server's public IP:

   ```
   Type: A
   Name: @  (or your subdomain, e.g., "platform")
   Value: <server public IP>
   TTL: 300
   ```

3. Verify DNS propagation:

   ```bash
   dig +short example.com
   ```

## 3. Clone and Configure

```bash
git clone https://github.com/yc20041001/ai-coding-platform.git
cd ai-coding-platform

cp .env.production.example .env.production
# Edit .env.production — EVERY CHANGE_ME must be replaced
```

### Required changes in `.env.production`

| Variable | How to set |
|----------|------------|
| `APP_DOMAIN` | Your domain, e.g. `platform.example.com` |
| `APP_BASE_URL` | `https://` + your domain |
| `APP_CORS_ALLOWED_ORIGINS` | Same as `APP_BASE_URL`, comma-separated for multiple |
| `DB_PASSWORD` | Strong random password |
| `JWT_SECRET` | Output of `openssl rand -base64 32` |
| `RABBITMQ_DEFAULT_PASS` | Strong random password |
| `GITHUB_REDIRECT_URI` | `https://<your-domain>/api/github/oauth/callback` |

## 4. First Deployment

```bash
# Build and start all services
bash scripts/prod-deploy.sh up --build

# Check service health
bash scripts/prod-deploy.sh status

# Run smoke test
bash scripts/prod-smoke-test.sh http://localhost
# Or with your domain:
bash scripts/prod-smoke-test.sh https://example.com
```

## 5. HTTPS Options

### Option A: Cloudflare Proxy (easiest)

1. Add your domain to Cloudflare (change nameservers).
2. Set SSL/TLS mode to **Full (strict)**.
3. Set an Origin Certificate (or use Cloudflare's edge cert).
4. On your server, only port 80 is needed — Cloudflare handles HTTPS.
5. The default `nginx.http.conf` works in this mode.

### Option B: Certbot (Let's Encrypt)

1. Start with `nginx.http.conf` first.
2. Run Certbot:

   ```bash
   sudo certbot certonly --webroot -w /var/www/certbot -d example.com
   ```

3. Copy `deploy/prod/nginx.https.conf.example` to replace the default.conf volume mount.
4. Update `docker-compose.prod.yml` volumes to mount the cert paths.
5. Reload nginx:

   ```bash
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec nginx nginx -s reload
   ```

6. Set up auto-renewal cron:

   ```
   0 3 * * * certbot renew --quiet && docker compose -f /path/to/deploy/prod/docker-compose.prod.yml exec nginx nginx -s reload
   ```

### Option C: Self-signed (local testing only)

```bash
mkdir -p deploy/prod/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout deploy/prod/certs/dummy.key \
  -out deploy/prod/certs/dummy.crt \
  -subj "/CN=localhost"
```

## 6. Environment Variable Reference

| Variable | Required | Default | Notes |
|----------|----------|---------|-------|
| `APP_DOMAIN` | Yes | — | Public domain name |
| `APP_CORS_ALLOWED_ORIGINS` | Yes | — | Comma-separated allowed origins |
| `DB_PASSWORD` | Yes | — | MySQL root password |
| `JWT_SECRET` | Yes | — | Min 32 chars, use `openssl rand -base64 32` |
| `RABBITMQ_DEFAULT_PASS` | Yes | — | RabbitMQ password |
| `MODEL_GATEWAY_PROVIDER` | No | `MOCK` | Model provider for AI features |
| `OPENAI_API_KEY` | No | — | Required for OpenAI provider |
| `CLAUDE_API_KEY` | No | — | Required for Claude provider |
| `DEEPSEEK_API_KEY` | No | — | Required for DeepSeek provider |
| `QWEN_API_KEY` | No | — | Required for Qwen provider |
| `GEMINI_API_KEY` | No | — | Required for Gemini provider |
| `GITHUB_CLIENT_ID` | No | — | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | No | — | GitHub OAuth App Client Secret |
| `JAVA_OPTS` | No | `-Xms256m -Xmx768m -XX:+UseG1GC` | JVM memory settings |

### External Services Setup

- [Model Provider Production Setup](./model-provider-production-setup.md) — Configure real AI model providers (OpenAI, Claude, DeepSeek, Qwen, Gemini).
- [GitHub OAuth Production Setup](./github-oauth-production-setup.md) — Configure GitHub OAuth for repository access and PR review.

### Validation Scripts

```bash
# Validate model provider configuration
bash scripts/validate-model-provider.sh

# Validate GitHub OAuth configuration
bash scripts/validate-github-oauth-config.sh

# Full external services smoke test (requires running backend)
bash scripts/prod-external-services-smoke-test.sh http://localhost
```

## 7. Start, Stop, Update, Rollback

### Start

```bash
bash scripts/prod-deploy.sh up
```

### Stop

```bash
bash scripts/prod-deploy.sh down
```

### Update

```bash
git pull
bash scripts/prod-deploy.sh up --build
```

### Rollback

```bash
# Revert code to previous version
git checkout <previous-commit>
bash scripts/prod-deploy.sh up --build
```

## 8. MySQL Backup and Restore

### Backup

```bash
bash scripts/prod-backup-mysql.sh
```

Backups are stored in `backups/` with timestamp filenames.

### Restore

```bash
bash scripts/prod-restore-mysql.sh backups/ai_coding_platform_20260515_120000.sql
```

### Automated daily backup (cron)

```
0 2 * * * cd /path/to/ai-coding-platform && bash scripts/prod-backup-mysql.sh
```

## 9. Viewing Logs

```bash
# All services
bash scripts/prod-logs.sh

# Specific service
bash scripts/prod-logs.sh backend
bash scripts/prod-logs.sh nginx
```

## 10. Architecture

```
Internet → nginx (:80/:443) → frontend (:80) — static files + Vue Router
                            → backend (:8080) — /api/* + SSE
                            
Internal only (no public ports):
  mysql (:3306), redis (:6379), rabbitmq (:5672)
```

All services communicate over the `prod-net` Docker bridge network. Only nginx exposes ports to the host.

## 11. Troubleshooting

### Containers won't start

```bash
bash scripts/prod-deploy.sh status
bash scripts/prod-deploy.sh logs
```

### Backend healthcheck failing

Check backend logs for errors:

```bash
bash scripts/prod-logs.sh backend
```

Common backend startup issues:
- MySQL not ready → wait for MySQL healthcheck to pass
- Flyway migration failed → check migration history in DB
- JWT_SECRET too short → ensure at least 32 characters

### 502 Bad Gateway on /api/

1. Check backend is healthy: `docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps`
2. Verify nginx can reach backend via Docker DNS (`backend:8080`)
3. Check backend logs for errors

### SSE streaming not working

The nginx config already disables buffering for `/api/`. If streams are still cut off:
1. Check if there's a CDN or reverse proxy in front of nginx that buffers responses
2. Verify `proxy_read_timeout` is at least 60s

### Vue Router — page refresh gives 404

The nginx configuration proxies all non-API, non-asset requests to the frontend container's nginx, which serves `index.html` for unknown paths (Vue Router history mode). If you see 404 on refresh, verify nginx is routing `/` to the frontend upstream.

### Port already in use

```bash
sudo lsof -i :80
sudo lsof -i :443
```

Change `NGINX_HTTP_PORT` and `NGINX_HTTPS_PORT` in `.env.production` if needed.

## 12. Production Health Checks

After deployment, run the production health check to verify all services:

```bash
bash scripts/prod-health-check.sh https://your-domain.com
```

This checks: Frontend availability, Authentication (login + bad password rejection), Core APIs (me/projects/agents), Observability endpoints, Model Gateway (providers + MOCK connection test), GitHub OAuth status, Chat session creation (optional), and Unauthenticated access blocking. Output uses PASS/FAIL/WARN/SKIP format with X-Trace-Id from login response.

## 13. Alerting and Monitoring

### Alert Check

```bash
bash scripts/prod-alert-check.sh https://your-domain.com
```

Checks service availability, model gateway fallback rate (>30% FAIL, >15% WARN), GitHub integration status, authentication security, container health, disk usage (>85% FAIL, >70% WARN), and MySQL status. See [Production Alerting Rules](./production-alerting-rules.md) for severity levels (P0-P3) and response procedures.

### Log Scanning

```bash
bash scripts/prod-log-scan.sh           # All services
bash scripts/prod-log-scan.sh backend   # Single service
```

Scans Docker logs and local files for secret leaks (API keys, tokens, passwords) and error patterns (ERROR, Exception, SQL errors, stack traces, rate limits, timeouts). See [Production Observability Runbook](./production-observability-runbook.md) for daily monitoring commands and log patterns.

### Diagnostics Collection

```bash
bash scripts/prod-diagnostics.sh
```

Collects Docker status, container logs, backend health JSON, disk/memory usage, smoke test results, file permissions, and git status into a timestamped directory (`diagnostics/diagnostics_YYYYmmdd_HHMMSS/`). All sensitive values (API keys, passwords, tokens) are automatically masked.

## 14. Security Hardening

### Pre-Deployment Security Check

```bash
bash scripts/prod-security-check.sh https://your-domain.com
```

Validates: `.env.production` not tracked by git, no real API keys committed to repo, HTTP security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy), CORS configuration (no wildcard origin), Actuator endpoint exposure, unauthenticated API access blocking, response token leak prevention, and `.gitignore` coverage.

### Security Hardening Checklist

See [Production Security Hardening Checklist](./production-security-hardening-checklist.md) for a comprehensive checklist covering secrets management, CORS, JWT, GitHub OAuth, model provider security, nginx headers, actuator security, log security, data protection, and incident response readiness. Includes periodic checks (weekly, monthly, quarterly).

## 15. Incident Response

See [Incident Response Runbook](./incident-response-runbook.md) for detailed procedures covering:

- Severity classification (P0-P3) and triage procedure
- 10 incident scenarios with symptoms/triage/recovery steps
- Rollback procedure and database recovery
- Emergency contacts template and post-incident review
