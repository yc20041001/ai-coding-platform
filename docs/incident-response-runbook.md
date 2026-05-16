# Incident Response Runbook

## Overview

This runbook provides step-by-step procedures for common production incidents. Always start with **Triage** to assess severity before taking action.

## Severity Classification

| Severity | Definition | Response Time | Example |
|----------|-----------|---------------|---------|
| P0 | Service outage, data loss, secret leak | Immediate | Backend completely down |
| P1 | Major feature broken | < 30 min | Login broken, 502 on all API |
| P2 | Partial degradation | < 2 hours | Model provider down, GitHub API errors |
| P3 | Minor issue | Next business day | Non-critical UI bug |

## Triage (Always Do First)

```bash
# 1. Check service status
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps

# 2. Quick health check
bash scripts/prod-health-check.sh https://example.com

# 3. Check recent logs
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs --tail=100

# 4. Check disk and memory
df -h
free -h
```

---

## 1. Frontend Access Failure

**Symptoms:** Browser shows "Connection refused", "502 Bad Gateway", or blank page.

### Triage

```bash
# Check if frontend/nginx containers are running
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps frontend nginx

# Check nginx logs
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs nginx --tail=50

# Test frontend directly (from server)
curl -I http://localhost/
```

### Recovery

1. **Nginx is down:**
   ```bash
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production restart nginx
   ```

2. **Frontend container down:**
   ```bash
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production up -d frontend
   ```

3. **Port conflict:**
   ```bash
   sudo lsof -i :80
   sudo lsof -i :443
   # Kill conflicting process or change NGINX_HTTP_PORT in .env.production
   ```

4. **All containers healthy but 502:**
   - Check nginx can resolve `frontend:80`: `docker compose exec nginx wget -qO- http://frontend:80/`
   - Check Docker network: `docker network inspect ai-coding-platform-prod_prod-net`

---

## 2. Backend 502 / 5xx

**Symptoms:** API calls return 502, 503, or 504.

### Triage

```bash
# Check backend health
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps backend

# Internal health check
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec backend \
  wget -qO- http://localhost:8080/actuator/health

# Backend logs (last 100 lines, filter errors)
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=100 2>&1 | \
  grep -E "ERROR|Exception|FAIL|Down"
```

### Recovery

1. **Backend not healthy:**
   ```bash
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production restart backend
   # Wait 40s for healthcheck, then verify
   ```

2. **MySQL connection error:**
   ```bash
   # Check MySQL health
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps mysql
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production restart mysql
   ```

3. **Out of memory:**
   ```bash
   # Check memory usage
   docker stats --no-stream | grep backend
   # Increase heap: edit .env.production JAVA_OPTS=-Xmx1024m, then restart
   ```

4. **Flyway migration failure:**
   ```bash
   # Check backend logs for FlywayException
   # If migration is stuck: manually fix in DB, then restart
   ```

---

## 3. MySQL Down

**Symptoms:** All API calls fail, backend logs show `CommunicationsException`.

### Triage

```bash
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps mysql
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs mysql --tail=50
```

### Recovery

1. **Container stopped:**
   ```bash
   docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production up -d mysql
   ```

2. **Disk full (MySQL can't write):**
   ```bash
   df -h /var/lib/docker
   docker system prune -a  # Caution: removes unused images/containers
   ```

3. **Data corruption:**
   - If volume is intact: restart MySQL
   - If volume is corrupted: restore from latest backup
   ```bash
   # Find latest backup
   ls -t backups/*.sql | head -1
   # Restore (see Section 12: Database Recovery)
   ```

---

## 4. Login Failure

**Symptoms:** Users cannot log in. All return 401.

### Triage

```bash
# Test login
curl -s -X POST https://example.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'

# Check backend logs for auth errors
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=50 | grep -i auth
```

### Recovery

1. **JWT secret changed:** If `JWT_SECRET` changed, all existing tokens are invalid. Users must re-login.
2. **Database issue:** Check MySQL health (Section 3).
3. **Rate limiting:** Too many failed attempts — wait or clear audit log locks.
4. **Admin password forgotten:** Reset via DB:
   ```bash
   # Generate new BCrypt hash for a known password
   # Update: UPDATE user SET password = '<new-hash>' WHERE email = 'admin@example.com';
   ```

---

## 5. Chat SSE Streaming Failure

**Symptoms:** Chat messages don't stream, SSE connection drops immediately.

### Triage

```bash
# Check model gateway provider
curl -s https://example.com/api/model-gateway/providers \
  -H "Authorization: Bearer $TOKEN"

# Test connection
curl -s -X POST https://example.com/api/model-gateway/test-connection \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"MOCK","modelName":"mock-agent-model"}'

# Check backend SSE logs
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=50 | \
  grep -E "stream|SSE|fallback"
```

### Recovery

1. **Model provider down:** Set `MODEL_GATEWAY_PROVIDER=MOCK` as temporary workaround.
2. **Nginx buffering:** Verify nginx config has `proxy_buffering off` for `/api/` location.
3. **Timeout:** Check `proxy_read_timeout` is at least 300s.
4. **Fallback to Mock:** Should happen automatically if `FALLBACK_ENABLED=true`.

---

## 6. Model Provider Failure

**Symptoms:** AI calls fail, Model Usage shows high error rate.

### Triage

```bash
# Check model usage
curl -s https://example.com/api/observability/model-usage/summary \
  -H "Authorization: Bearer $TOKEN"

# Check provider config
bash scripts/validate-model-provider.sh
```

### Recovery

1. **API key expired/revoked:** Update key in `.env.production`, restart backend.
2. **Provider outage:** Check provider status page. Switch to another provider:
   ```bash
   # Edit .env.production: MODEL_GATEWAY_PROVIDER=MOCK (or another provider)
   bash scripts/prod-deploy.sh restart backend
   ```
3. **Rate limit:** Wait and retry, or reduce request frequency.
4. **Base URL wrong:** Verify `{PROVIDER}_BASE_URL` in `.env.production`.

---

## 7. Model Cost Spike

**Symptoms:** Daily estimated cost is significantly higher than normal.

### Triage

```bash
# Check detailed usage
curl -s "https://example.com/api/projects/{id}/observability/model-usage/daily?days=7" \
  -H "Authorization: Bearer $TOKEN"
```

### Recovery

1. **Review audit logs** for unusual MODEL_CALL patterns.
2. **Switch to cheaper model:** e.g., from `gpt-4.1-mini` to `deepseek-chat`.
3. **Reduce maxTokens** in model gateway settings.
4. **Set cost alerts** in [Alerting Rules](./production-alerting-rules.md).

---

## 8. GitHub OAuth Callback Failure

**Symptoms:** After GitHub authorization, redirect fails with error.

### Triage

```bash
# Check OAuth status
curl -s https://example.com/api/github/oauth/status \
  -H "Authorization: Bearer $TOKEN"

# Check backend logs
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=50 | \
  grep -i github
```

### Recovery

1. **Redirect URI mismatch:** Ensure `GITHUB_REDIRECT_URI` matches GitHub OAuth App setting exactly.
2. **Client secret wrong:** Verify `GITHUB_CLIENT_SECRET` in `.env.production`.
3. **OAuth App not configured:** Create a GitHub OAuth App (see [GitHub OAuth Setup](./github-oauth-production-setup.md)).
4. **Token expired:** Re-authorize (state expires after 10 minutes).

---

## 9. PR Review Failure

**Symptoms:** Review jobs stuck in PENDING or fail.

### Triage

```bash
# Check review job status via API
# Check backend logs for PR review errors
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=100 | \
  grep -i "PR.*[Rr]eview\|FAILED"
```

### Recovery

1. **Model provider down:** See Section 6.
2. **GitHub API error:** Check GitHub status, rate limits.
3. **Patch too large:** The PR diff exceeds model context. Try a smaller PR or increase maxTokens.
4. **JSON parse failure:** Non-critical — review uses raw summary as fallback.

---

## 10. Suspected Secret Leak

**Symptoms:** `prod-log-scan.sh` finds secret patterns in logs.

**This is P0 — act immediately.**

### Triage

```bash
# Run secret scan
bash scripts/prod-log-scan.sh

# Check git for committed secrets
rg "sk-|ghp_|github_pat_" .
git log --all --full-history -S "sk-" --oneline
```

### Immediate Recovery

1. **If API key found in logs:**
   - **Rotate the key immediately** at the provider's console.
   - Update `.env.production` with new key.
   - Restart backend: `bash scripts/prod-deploy.sh restart backend`
   - Clean logs: `docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production down && docker compose ... up -d`

2. **If GitHub token found:**
   - Revoke the token at [GitHub Settings → Applications → Authorized OAuth Apps](https://github.com/settings/applications).
   - Re-authorize to get a new token.
   - Clean logs.

3. **If `.env.production` committed to git:**
   - **Do NOT just delete the file and commit.**
   - Rotate ALL keys in `.env.production`.
   - Use `git filter-branch` or `BFG Repo-Cleaner` to remove from history.
   - Force push (coordinate with team).

---

## 11. Rollback Procedure

To rollback to a previous version:

```bash
# 1. View recent commits
git log --oneline -10

# 2. Checkout previous version
git checkout <previous-commit-hash>

# 3. Rebuild and redeploy
bash scripts/prod-deploy.sh up --build

# 4. Verify
bash scripts/prod-health-check.sh https://example.com
```

### Rollback Considerations

- Database migrations are NOT automatically rolled back. If the new version added migrations, manual DB intervention may be needed.
- User sessions (JWT tokens) are unaffected by rollback.
- Model provider configurations (`.env.production`) are unchanged by git operations.

---

## 12. Database Recovery

### Restore from Backup

```bash
# 1. Stop backend to prevent writes during restore
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production stop backend

# 2. Restore
bash scripts/prod-restore-mysql.sh backups/ai_coding_platform_TIMESTAMP.sql

# 3. Restart backend
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production start backend

# 4. Verify
bash scripts/prod-health-check.sh https://example.com
```

---

## 13. Emergency Contacts Template

Fill in before going to production:

| Role | Name | Phone | Email | Notes |
|------|------|-------|-------|-------|
| Primary On-Call | | | | |
| Secondary On-Call | | | | |
| Security Incident | | | | |
| Infrastructure | | | | |

## 14. Post-Incident Review

After every P0/P1 incident:

1. **Document** what happened, when, and how it was resolved.
2. **Identify root cause** — was it a bug, configuration error, external dependency?
3. **Add prevention** — update monitoring rules, add automated check, improve docs.
4. **Update this runbook** if the recovery procedure changed.
