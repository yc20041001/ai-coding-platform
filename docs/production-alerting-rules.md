# Production Alerting Rules

## Overview

This document defines alerting thresholds and response procedures for the AI Coding Platform production deployment. These rules can be checked manually via `bash scripts/prod-alert-check.sh` or integrated with external monitoring tools.

## Alert Severity Levels

| Level | Meaning | Response Time | Example |
|-------|---------|---------------|---------|
| **P0** | Service outage, data loss, secret leak | Immediate (5 min) | Backend down, MySQL down, secret in logs |
| **P1** | Major degradation, security issue | < 30 min | Frontend 5xx spike, CORS misconfig |
| **P2** | Partial degradation, performance issue | < 2 hours | Model failure > 20%, GitHub rate limit |
| **P3** | Minor anomaly, warning | Next business day | Prompt safety blocks, login failure spike |

## 1. Service Availability

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| Frontend Down | GET / returns non-2xx | 3 consecutive failures | P1 | `prod-health-check.sh` | Check nginx and frontend containers |
| Backend Down | `/actuator/health` not UP | Any | P1 | Docker healthcheck | Check backend logs, restart if needed |
| MySQL Down | Healthcheck fails | Any | P1 | Docker healthcheck | Check MySQL container, disk space |
| Nginx 5xx Spike | 5xx ratio > 5% | Over 5 minutes | P1 | nginx access log | Check upstream backends |
| Service Restart Loop | Container restart count > 3 | Over 10 minutes | P1 | `docker ps` | Check logs, disk, memory |

## 2. Model Gateway

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| Model Failure Rate High | `success=false` ratio | > 20% in 15 min | P2 | Model usage API | Check provider API key and status page |
| Model Fallback Rate High | `fallback_used` ratio | > 30% in 15 min | P2 | Model usage API | Verify real provider is reachable |
| Model Cost Spike | Daily estimated cost | > 2x baseline | P2 | Cost summary API | Review model usage patterns |
| Model Timeout Spike | `TIMEOUT` errors | > 10 in 15 min | P2 | ModelRequestLog | Check provider base URL and latency |
| Model Auth Failure | `AUTH_ERROR` repeated | > 3 in 15 min | P2 | ModelRequestLog | Verify API key is valid |
| Prompt Safety Block Spike | Safety blocks | > 10 in 15 min | P3 | ModelRequestLog | Review blocked prompts for abuse |
| Provider Disabled | `enabled=false` for active provider | Any | P2 | Provider config | Enable provider or switch default |

## 3. GitHub Integration

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| GitHub Auth Error | `AUTH_ERROR` consecutive | > 3 in 15 min | P2 | Audit log | Check GitHub token validity |
| GitHub Rate Limit | `RATE_LIMIT` error | Any | P2 | Audit log | Check API rate limit dashboard |
| GitHub API Failure | `GITHUB_API_ERROR` rate | > 20% in 15 min | P3 | Audit log | Check GitHub status page |
| PR Review Failure Rate | Review job FAILED ratio | > 20% in 15 min | P2 | PR Review API | Check model provider and PR access |
| OAuth Token Expired | Token exchange failure | Repeated | P2 | OAuth logs | User re-authorize or check secret |
| GitHub Unconfigured | `configured=false` | After expected setup | P3 | OAuth status API | Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` |

## 4. Authentication & Security

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| Login Failure Spike | Failed logins | > 10 in 5 min | P2 | Audit log | Check for brute force, review IPs |
| Unauthorized Spike | 401/403 responses | > 20 in 5 min | P3 | nginx/backend logs | Check JWT validity and permissions |
| **Secret Leak in Logs** | Pattern: `sk-`, `ghp_`, `github_pat_` | **ANY** | **P0** | `prod-log-scan.sh` | **Immediately rotate key, remove from logs** |
| CORS Misconfiguration | Wildcard `*` origin in prod | Any | P1 | `prod-security-check.sh` | Set `APP_CORS_ALLOWED_ORIGINS` |
| Actuator Exposed | `/actuator/health` details public | Any | P1 | `prod-security-check.sh` | Set `show-details: never` in prod profile |
| `.env.production` Tracked | File in git | Any | P0 | `prod-security-check.sh` | Remove from git history, rotate all keys |

## 5. Chat SSE

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| SSE Connection Errors | Stream failure rate | > 10% in 15 min | P3 | Backend logs | Check model provider streaming |
| SSE Timeout | Connection timeout | Repeated | P3 | Backend logs | Check `proxy_read_timeout` and model latency |
| SSE Abort Errors | Client abort causing exceptions | Repeated | P3 | Backend logs | Normal (user action), no action needed |

## 6. Database & Storage

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| Disk Space Low | Disk usage | > 80% | P1 | `df -h` | Clean logs, expand disk |
| Disk Space Critical | Disk usage | > 95% | P0 | `df -h` | Emergency cleanup, expand disk |
| Backup Missing | No backup in 24h | > 24 hours | P2 | `ls backups/` | Check cron for backup script |
| MySQL Connection Errors | `CommunicationsException` | Repeated | P2 | Backend logs | Check MySQL health, restart if needed |

## 7. Docker / Infrastructure

| Alert | Condition | Threshold | Severity | Check | Action |
|-------|-----------|-----------|----------|-------|--------|
| Container Not Running | Expected container status != Up | Any | P1 | `docker ps` | Check logs, restart |
| High CPU | Container CPU | > 80% sustained | P3 | `docker stats` | Check for runaway processes |
| High Memory | Container memory | > 85% limit | P2 | `docker stats` | Increase `JAVA_OPTS -Xmx` or scale |
| Volume Space | Named volume disk usage | > 90% | P2 | `docker system df` | Prune unused volumes |

## 8. Using prod-alert-check.sh

```bash
# Full alert check
bash scripts/prod-alert-check.sh

# With specific base URL
bash scripts/prod-alert-check.sh https://example.com
```

The script outputs:
- **PASS** — Metric is within normal range
- **WARN** — Metric approaching threshold
- **FAIL** — Threshold exceeded, action required

## 9. Setting Up External Alerting (Optional)

### Webhook Integration

For Slack/Discord/email alerts, add a cron job that runs `prod-alert-check.sh` and sends FAIL results:

```bash
#!/bin/bash
# /etc/cron.d/ai-coding-alerts: Run every 5 minutes
*/5 * * * * cd /path/to/project && bash scripts/prod-alert-check.sh https://example.com | grep -q "FAIL" && \
  curl -X POST -H "Content-Type: application/json" \
  -d '{"text":"AI Coding Platform alert: check server"}' \
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### Uptime Monitoring

Use external uptime monitoring (e.g., UptimeRobot, Pingdom) to check:
- `https://example.com/` — Should return 200
- `https://example.com/api/health` — Should return 200
