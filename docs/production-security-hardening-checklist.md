# Production Security Hardening Checklist

## Pre-Deployment

### 1. Secrets Management

- [ ] `.env.production` is NOT committed to git (verify: `git status`)
- [ ] `.env.production` is in `.gitignore`
- [ ] All `CHANGE_ME` values replaced with real secrets
- [ ] `JWT_SECRET` is at least 32 characters (generate: `openssl rand -base64 32`)
- [ ] `DB_PASSWORD` is strong (not `platform123`)
- [ ] `RABBITMQ_DEFAULT_PASS` is strong
- [ ] `GITHUB_CLIENT_SECRET` is strong (if GitHub OAuth configured)
- [ ] Model API keys stored only in `.env.production`, never in code or config files

### 2. CORS Configuration

- [ ] `APP_CORS_ALLOWED_ORIGINS` set to production domain (e.g., `https://example.com`)
- [ ] NOT using wildcard `*` in production
- [ ] Multiple origins comma-separated if needed
- [ ] `localhost:*` fallback only for emergency local access
- [ ] Verify: `bash scripts/prod-security-check.sh https://example.com`

### 3. Authentication & JWT

- [ ] `JWT_SECRET` is strong (256-bit min, 32+ characters)
- [ ] Access token expiry is reasonable (default: 7200s = 2 hours)
- [ ] Refresh token expiry is reasonable (default: 604800s = 7 days)
- [ ] Login rate limiting considered (manual check via audit log)
- [ ] No hardcoded credentials in code

### 4. GitHub OAuth

- [ ] OAuth App callback URL matches `GITHUB_REDIRECT_URI`
- [ ] `GITHUB_CLIENT_SECRET` is NOT in any committed file
- [ ] OAuth scopes are appropriate (consider `public_repo` instead of `repo`)
- [ ] GitHub token is stored encrypted in database
- [ ] GitHub token is NEVER returned in API responses
- [ ] OAuth state parameter has 10-minute expiry
- [ ] OAuth state is single-use (PENDING → USED)

### 5. Model Provider Security

- [ ] API keys stored only in environment variables
- [ ] API keys masked in all logs (verify: `sk-a1****yz`)
- [ ] API keys masked in all API responses
- [ ] Connection test does not persist keys
- [ ] Prompt Safety is enabled (`MODEL_GATEWAY_PROMPT_SAFETY_ENABLED=true`)
- [ ] Fallback to Mock enabled for resilience
- [ ] Fallback does NOT expose real provider error to end users (internal only)

## Post-Deployment

### 6. Nginx Security Headers

- [ ] `X-Content-Type-Options: nosniff` is set
- [ ] `X-Frame-Options: SAMEORIGIN` is set
- [ ] `Referrer-Policy: strict-origin-when-cross-origin` is set
- [ ] `Strict-Transport-Security` is set (HTTPS mode)
- [ ] `server_tokens` is off (hide nginx version)
- [ ] Verify: `curl -I https://example.com`

### 7. Actuator Security

- [ ] `show-details: never` in production profile
- [ ] `/actuator/health` NOT exposed externally via nginx
- [ ] Only `health,info,metrics` endpoints exposed (no `env`, `configprops`, `beans`)
- [ ] Actuator endpoints only accessible within Compose network

### 8. Log Security

- [ ] No API keys in Docker logs (scan: `bash scripts/prod-log-scan.sh`)
- [ ] No GitHub tokens in Docker logs
- [ ] No Bearer tokens in Docker logs
- [ ] Error messages do NOT contain secrets
- [ ] Trace IDs present for request correlation
- [ ] Sensitive request bodies not logged at DEBUG level in production

### 9. Data Protection

- [ ] Database backups encrypted or stored securely
- [ ] `backups/` directory is in `.gitignore`
- [ ] MySQL port not exposed to public internet (127.0.0.1 bind only)
- [ ] Redis not exposed to public internet
- [ ] RabbitMQ not exposed to public internet
- [ ] MySQL `MYSQL_ROOT_PASSWORD` is strong

### 10. Incident Response Readiness

- [ ] [Incident Response Runbook](./incident-response-runbook.md) is accessible
- [ ] Backup restoration procedure tested
- [ ] Rollback procedure documented and tested
- [ ] Contact information for key holders available
- [ ] API key rotation procedure documented

## Automated Security Checks

Run these regularly (daily via cron recommended):

```bash
# Full security scan
bash scripts/prod-security-check.sh https://example.com

# Log scan for secret leaks
bash scripts/prod-log-scan.sh

# Git repository check
git status --short
rg "sk-|ghp_|github_pat_" .
```

## Periodic Manual Checks

### Weekly

- [ ] Review audit logs for suspicious activity
- [ ] Check model usage for cost anomalies
- [ ] Verify backup integrity (restore to test DB)
- [ ] Review GitHub OAuth bound accounts

### Monthly

- [ ] Rotate `JWT_SECRET` (requires all users to re-login)
- [ ] Review and update API keys
- [ ] Update Docker base images (`docker compose pull`)
- [ ] Review firewall rules
- [ ] Check for security updates (`apt update && apt upgrade`)

### Quarterly

- [ ] Penetration test (basic: run `prod-security-check.sh` with extended checks)
- [ ] Review OAuth App permissions
- [ ] Audit user roles and project memberships
- [ ] Review and prune old audit logs
- [ ] Test full disaster recovery (restore from backup on fresh server)

## Security Contacts

Define who to contact for security incidents:

| Role | Name | Contact |
|------|------|---------|
| Primary On-Call | (TBD) | (TBD) |
| Security Lead | (TBD) | (TBD) |
| Infrastructure Lead | (TBD) | (TBD) |
