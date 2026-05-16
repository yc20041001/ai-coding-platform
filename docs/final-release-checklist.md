# Final Release Checklist

Comprehensive pre-release verification. Run before every release.

## 1. Source Control

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| No uncommitted changes | `git status --porcelain` | Only expected files | Yes |
| Branch is main | `git branch --show-current` | `main` | Yes |
| No untracked secrets | `git ls-files --others --exclude-standard` | No `.env`, `.env.production`, `*.pem`, `*.key` | Yes |

## 2. Secrets

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| No API keys in codebase | `grep -r "sk-" backend/src/ --include="*.java" \| grep -v "mask\|test\|example\|CHANGE_ME"` | No output | Yes |
| No hardcoded passwords | `grep -r "password" backend/src/ --include="*.java" \| grep -v "test\|example\|encode\|hash\|mask"` | No real passwords | Yes |
| `.env.example` has no real secrets | Review `.env.example` | All values are placeholders or safe defaults | Yes |
| `.env.production.example` has no real secrets | Review `.env.production.example` | All secret values are empty or `CHANGE_ME` | Yes |

## 3. Backend Quality

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Compile | `cd backend && mvn compile` | BUILD SUCCESS | Yes |
| All tests pass | `cd backend && mvn test` | 144/144 pass, BUILD SUCCESS | Yes |
| Package | `cd backend && mvn package -DskipTests` | BUILD SUCCESS | Yes |
| Flyway migrations valid | Check `db/migration/` file naming | No duplicate versions, sequential | Yes |

## 4. Frontend Quality

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| TypeCheck | `cd frontend && npm run typecheck` | Exit 0, no errors | Yes |
| Build | `cd frontend && npm run build` | Exit 0, no errors | Yes |
| E2E Test Run 1 | `cd frontend && npm run test:e2e -- --workers=1` | 13/13 pass | Yes |
| E2E Test Run 2 | `cd frontend && npm run test:e2e -- --workers=1` | 13/13 pass (stability) | Yes |
| Bundle Check | `bash scripts/frontend-bundle-check.sh` | Within budget (3/0/0 or better) | Warning |

## 5. Docker

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Docker Compose config valid | `docker compose -f deploy/docker-compose.yml config` | No errors | Yes |
| App Compose config valid | `docker compose -f deploy/docker-compose.app.yml config` | No errors | Yes |
| Production Compose config valid | `docker compose -f deploy/prod/docker-compose.prod.yml config` | No errors | Yes |
| Docker smoke test | `bash scripts/docker-smoke-test.sh` | All checks pass | No (needs Docker env) |

## 6. Demo Data

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Seed script exists | `test -f scripts/demo-seed-data.sh` | File exists | Yes |
| Smoke test exists | `test -f scripts/demo-smoke-test.sh` | File exists | Yes |
| Reset script exists | `test -f scripts/demo-reset-data.sh` | File exists | Yes |

## 7. Production Config

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Production env example exists | `test -f .env.production.example` | File exists | Yes |
| All CHANGE_ME documented | `grep -c "CHANGE_ME" .env.production.example` | > 0 (placeholders present) | Warning |
| Deploy script exists | `test -f scripts/prod-deploy.sh` | File exists | Yes |
| Smoke test script exists | `test -f scripts/prod-smoke-test.sh` | File exists | Yes |
| Backup script exists | `test -f scripts/prod-backup-mysql.sh` | File exists | Yes |
| Restore script exists | `test -f scripts/prod-restore-mysql.sh` | File exists | Yes |

## 8. Documentation

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Final delivery report | `test -f docs/final-delivery-report.md` | File exists | Yes |
| Handoff guide | `test -f docs/project-handoff-guide.md` | File exists | Yes |
| Documentation index | `test -f docs/documentation-index.md` | File exists | Yes |
| API/Page/Script index | `test -f docs/api-page-script-index.md` | File exists | Yes |
| Environment variable index | `test -f docs/environment-variable-index.md` | File exists | Yes |
| README has doc links | `grep -c "docs/" README.md` | > 5 links | No |
| CHANGELOG up to date | Review `CHANGELOG.md` | Current release documented | No |

## 9. Security

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| Security check script exists | `test -f scripts/prod-security-check.sh` | File exists | Yes |
| Health check script exists | `test -f scripts/prod-health-check.sh` | File exists | Yes |
| Log scan script exists | `test -f scripts/prod-log-scan.sh` | File exists | Yes |
| Security hardening doc exists | `test -f docs/production-security-hardening-checklist.md` | File exists | Yes |
| Incident response doc exists | `test -f docs/incident-response-runbook.md` | File exists | Yes |

## 10. Handoff

| Check | Command | Expected Result | Blocking |
|-------|---------|-----------------|----------|
| All index docs present | `test -f docs/documentation-index.md && test -f docs/api-page-script-index.md && test -f docs/environment-variable-index.md` | All files exist | Yes |
| Known limitations documented | `grep -c "Known Limitation\|known limitation\|Limitation" docs/final-delivery-report.md` | > 0 | No |
| Release checklist passes | `bash scripts/release-checklist.sh` | All gates pass | Yes |

---

## One-Command Full Check

```bash
bash scripts/release-checklist.sh
```

This script runs the core blocking checks (backend compile/test/package, secret scan, source control) and reports pass/fail.
