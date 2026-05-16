# Production Observability Runbook

## 1. Monitoring Overview

The AI Coding Platform provides built-in observability through:

| Component | Endpoint / Tool | Access |
|-----------|----------------|--------|
| Health Check | `/actuator/health` | Internal (Compose network) |
| System Overview | `/api/observability/overview` | ADMIN |
| Model Usage | `/api/observability/model-usage/summary` | ADMIN |
| Model Usage (Daily) | `/api/projects/{id}/observability/model-usage/daily` | ADMIN |
| Audit Logs | `/api/audit/logs` | ADMIN |
| Project Audit Logs | `/api/projects/{id}/audit/logs` | ADMIN |
| Docker Logs | `docker compose logs` | Server admin |
| Nginx Access/Error | nginx container stdout | Server admin |

## 2. Daily Monitoring Commands

### Quick Health Check

```bash
bash scripts/prod-health-check.sh http://localhost
# or with domain:
bash scripts/prod-health-check.sh https://example.com
```

### Security Check

```bash
bash scripts/prod-security-check.sh https://example.com
```

### Log Scan for Secrets

```bash
bash scripts/prod-log-scan.sh
```

### Alert Check

```bash
bash scripts/prod-alert-check.sh
```

### Full Diagnostics

```bash
bash scripts/prod-diagnostics.sh
```

## 3. Health Check Commands Reference

### Application Health

```bash
# Internal health (from server)
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec backend \
  wget -qO- http://localhost:8080/actuator/health

# Check container status
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps
```

### Service Status

```bash
# All service health
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps

# Specific service logs
bash scripts/prod-logs.sh backend
bash scripts/prod-logs.sh nginx
bash scripts/prod-logs.sh mysql
```

### Model Gateway Health

```bash
# Via API (requires auth)
TOKEN=$(curl -s -X POST https://example.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# List providers
curl -s https://example.com/api/model-gateway/providers \
  -H "Authorization: Bearer $TOKEN"

# Test connection (MOCK)
curl -s -X POST https://example.com/api/model-gateway/test-connection \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"MOCK","modelName":"mock-agent-model"}'

# Model usage summary
curl -s https://example.com/api/observability/model-usage/summary \
  -H "Authorization: Bearer $TOKEN"
```

## 4. Log Viewing Commands

### Backend Logs

```bash
# Live tail
bash scripts/prod-logs.sh backend

# Last 200 lines
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=200

# Filter for errors
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend 2>&1 | grep -E "ERROR|Exception|FAIL"
```

### Nginx Logs

```bash
bash scripts/prod-logs.sh nginx

# Access log
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs nginx 2>&1 | grep -E "GET|POST|PUT|DELETE"

# Error log
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs nginx 2>&1 | grep -E "error|Error|ERROR"
```

### Key Log Patterns

| Pattern | Meaning | Action |
|---------|---------|--------|
| `traceId=` | Request trace ID for correlation | Use to trace a request through logs |
| `Falling back to Mock` | Model provider failed, used Mock | Check provider API key and connectivity |
| `GitHub API` | GitHub API call | Check rate limits and token validity |
| `FlywayException` | Database migration issue | Check DB schema and migration history |
| `CommunicationsException` | MySQL connection lost | Restart MySQL or check network |
| `401` or `403` | Auth failure | Check user credentials or permissions |
| `502` or `503` | Upstream unreachable | Check backend/frontend container health |

## 5. Model Usage Monitoring

### View Usage Dashboard

Navigate to **Observability → Model Usage** in the frontend, or use the API:

```bash
# Global usage summary
curl -s https://example.com/api/observability/model-usage/summary \
  -H "Authorization: Bearer $TOKEN"

# Project-specific daily usage
curl -s "https://example.com/api/projects/{projectId}/observability/model-usage/daily?days=7" \
  -H "Authorization: Bearer $TOKEN"
```

### Cost Tracking

Model costs are estimated based on provider pricing:

| Provider | Input ($/1M tokens) | Output ($/1M tokens) |
|----------|---------------------|----------------------|
| OpenAI GPT-4.1 | $2.50 | $10.00 |
| Claude 3.5 Sonnet | $3.00 | $15.00 |
| DeepSeek Chat | $0.14 | $0.28 |
| Qwen Plus | $0.40 | $0.80 |
| Gemini 2.5 Flash | $0.075 | $0.30 |
| MOCK | $0.00 | $0.00 |

## 6. Audit Logs

### Query Audit Logs

```bash
# Global audit logs (ADMIN only)
curl -s "https://example.com/api/audit/logs?page=1&size=50" \
  -H "Authorization: Bearer $TOKEN"

# Project audit logs
curl -s "https://example.com/api/projects/{projectId}/audit/logs?page=1&size=50" \
  -H "Authorization: Bearer $TOKEN"

# Filter by action type
curl -s "https://example.com/api/audit/logs?actionType=MODEL_CALL&page=1&size=50" \
  -H "Authorization: Bearer $TOKEN"
```

### Audit Action Types

| Action Type | Description |
|-------------|-------------|
| `LOGIN_SUCCESS` / `LOGIN_FAILED` | Authentication events |
| `PROJECT_CREATE` / `PROJECT_DELETE` | Project lifecycle |
| `MODEL_CALL` | Model gateway request |
| `PR_REVIEW_START` / `PR_REVIEW_COMPLETE` / `PR_REVIEW_FAILED` | PR review lifecycle |
| `GITHUB_OAUTH_START` / `GITHUB_OAUTH_CALLBACK` | GitHub OAuth flow |
| `TASK_EXECUTE_START` / `TASK_EXECUTE_COMPLETE` | Task execution |

## 7. GitHub OAuth Status

```bash
# Check OAuth configuration status
curl -s https://example.com/api/github/oauth/status \
  -H "Authorization: Bearer $TOKEN"
```

Response:
- `configured: false` — Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` in `.env.production`.
- `bound: true` — User has authorized. Login shows the GitHub username.
- `bound: false` — Click "Authorize GitHub" to bind.

## 8. Database Status

```bash
# Check MySQL container status
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps mysql

# Check MySQL connectivity
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec mysql \
  mysqladmin ping -h localhost

# Check database size
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec mysql \
  mysql -uroot -p"${DB_PASSWORD}" -e "SELECT table_schema, SUM(data_length + index_length) / 1024 / 1024 AS size_mb FROM information_schema.tables WHERE table_schema = 'ai_coding_platform' GROUP BY table_schema;"
```

## 9. Backup Status

```bash
# List backups
ls -lh backups/

# Latest backup
ls -t backups/*.sql | head -1

# Create a manual backup
bash scripts/prod-backup-mysql.sh
```

## 10. Resource Usage

```bash
# Container resource usage
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"

# Disk usage
df -h /

# Memory
free -h
```

## 11. Alerting Rules

See [Production Alerting Rules](./production-alerting-rules.md) for alert thresholds and response procedures.

## 12. Incident Response

See [Incident Response Runbook](./incident-response-runbook.md) for step-by-step recovery procedures.

## 13. Optional: Prometheus + Grafana

### Future Integration Path

The platform already exposes Spring Boot Actuator metrics at `/actuator/metrics`. To add Prometheus:

1. Add `micrometer-registry-prometheus` to `pom.xml`.
2. Add `management.endpoints.web.exposure.include=health,info,metrics,prometheus` to `application-prod.yml`.
3. Add Prometheus and Grafana to `docker-compose.prod.yml`:

```yaml
prometheus:
  image: prom/prometheus
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "127.0.0.1:9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "127.0.0.1:3000:3000"
  volumes:
    - grafana_data:/var/lib/grafana
```

4. Build dashboards for:
   - JVM Memory (heap, non-heap, GC)
   - HTTP Request Latency (p50/p95/p99)
   - HTTP Status Codes (2xx/4xx/5xx)
   - Model Gateway Requests (by provider, success/failure)
   - Model Token Usage & Cost
   - GitHub API Errors

This is **optional** and not required for the single-machine demo deployment.
