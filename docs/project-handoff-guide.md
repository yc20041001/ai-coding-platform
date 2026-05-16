# Project Handoff Guide

How to take over, run, test, and maintain this project.

## 1. Day 1 — Get It Running

```bash
# 1. Clone
git clone <repo-url>
cd ai-coding-platform

# 2. Start infrastructure
docker compose -f deploy/docker-compose.yml up -d

# 3. Copy and review env
cp .env.example .env
# Review: DB credentials, JWT_SECRET, MODEL_GATEWAY_PROVIDER=MOCK

# 4. Start backend (terminal 1)
cd backend && source ../.env && mvn spring-boot:run
# Verify: curl http://localhost:8080/actuator/health

# 5. Start frontend (terminal 2)
cd frontend && npm install && npm run dev -- --host 0.0.0.0
# Open: http://localhost:5173

# 6. Initialize demo data (terminal 3)
bash scripts/demo-seed-data.sh
```

Login: `admin@example.com` / `Admin@123456`

## 2. Project Structure at a Glance

```
ai-coding-platform/
├── backend/          # Spring Boot 3.3.5, Java 17, Maven
│   └── src/main/java/com/aicoding/platform/
│       ├── auth/     # Authentication, JWT, RBAC
│       ├── project/  # Project CRUD
│       ├── task/     # Task state machine, execution
│       ├── chat/     # Chat sessions, messages, SSE
│       ├── rag/      # Knowledge base, chunking, search
│       ├── agent/    # AI Agent definitions
│       ├── orchestrator/  # Agent execution engine
│       ├── modelgateway/  # Multi-provider LLM gateway
│       ├── github/   # GitHub OAuth, PR review
│       ├── repository/    # Repository management
│       ├── member/   # Project members, permissions
│       ├── audit/    # Audit logging
│       ├── observability/ # System metrics
│       ├── security/ # JWT, Spring Security filter
│       └── common/   # Shared exception, pagination, utils
├── frontend/         # Vue 3 + TypeScript + Vite + Element Plus
│   └── src/modules/  # 13 feature modules
├── deploy/           # Docker Compose files
│   ├── docker-compose.yml          # Infrastructure only
│   ├── docker-compose.app.yml      # Full stack demo
│   └── prod/                       # Production configs
├── docs/             # ~70 documentation files
├── scripts/          # ~28 utility scripts
└── .github/          # CI workflows, Issue/PR templates
```

## 3. Running Tests

### Backend

```bash
cd backend
mvn test                              # All 144 tests
mvn test -Dtest=JwtTokenProviderTest  # Single class
bash ../scripts/run-backend-checks.sh # Compile + Test + Package
```

Test profile: `application-test.yml` — MOCK provider, no real APIs.

### Frontend

```bash
cd frontend
npm run typecheck                     # vue-tsc --noEmit
npm run build                         # Vite production build
npm run test:e2e -- --workers=1       # Playwright 13 tests
bash ../scripts/run-frontend-checks.sh # Full check suite
```

### Release Gate

```bash
bash scripts/release-checklist.sh     # All quality gates
```

## 4. Demo Data

```bash
bash scripts/demo-seed-data.sh        # Initialize (idempotent)
bash scripts/demo-smoke-test.sh       # Verify demo works
bash scripts/demo-reset-data.sh --yes # Clean up demo data
```

Demo creates: Demo AI Workspace project with tasks, chat sessions, knowledge base, agent executions.

## 5. Docker Deployment

### Full-stack Demo

```bash
docker compose -f deploy/docker-compose.app.yml up -d --build
docker compose -f deploy/docker-compose.app.yml logs -f backend
docker compose -f deploy/docker-compose.app.yml down
```

### Production

```bash
cp .env.production.example .env.production
# Edit .env.production — replace ALL CHANGE_ME values
bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost
```

## 6. Logs

### Backend

```bash
# Local
tail -f backend/logs/application.log

# Docker
docker compose -f deploy/docker-compose.app.yml logs -f backend
```

### Frontend

```bash
# Dev server logs shown in terminal
# Docker
docker compose -f deploy/docker-compose.app.yml logs -f frontend
```

### Production

```bash
bash scripts/prod-logs.sh             # All services
bash scripts/prod-log-scan.sh         # Scan for errors/warnings
```

## 7. Common Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Backend won't start | MySQL not running | `docker compose -f deploy/docker-compose.yml up -d mysql` |
| "Access denied for user" | Wrong DB credentials | Check `.env` DB_USERNAME/DB_PASSWORD match `docker-compose.yml` |
| Flyway migration fails | Database doesn't exist | Create DB: `mysql -u root -e "CREATE DATABASE ai_coding_platform"` |
| JWT_SECRET too short | < 32 chars | Generate: `openssl rand -base64 32` |
| Frontend can't reach backend | CORS or wrong API URL | Check `frontend/.env` VITE_API_BASE_URL |
| Model calls fail | MOCK provider not set | Ensure `MODEL_GATEWAY_PROVIDER=MOCK` in `.env` |
| E2E tests fail | Backend not running or stale data | Ensure backend is up, restart if needed |
| Port conflict (3307) | Local MySQL running | `docker compose` uses port 3307; check with `lsof -i :3307` |
| Build too slow | Large node_modules cache | `cd frontend && rm -rf node_modules && npm install` |

## 8. Releasing

```bash
# 1. Run full check
bash scripts/run-all-checks.sh

# 2. Run release checklist
bash scripts/release-checklist.sh

# 3. Update versions (if applicable)
# backend/pom.xml: <version>
# frontend/package.json: "version"

# 4. Update changelog
# Add changes to CHANGELOG.md under [Unreleased]

# 5. Tag and push
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin main --tags

# 6. Build Docker images
bash scripts/docker-build-local.sh
```

## 9. Handling User Feedback

1. User submits feedback via [Issue template](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
2. Follow [Triage Guide](docs/user-trial-triage-guide.md) to classify and prioritize
3. Reference [Feedback Taxonomy](docs/product-feedback-taxonomy.md) for categorization
4. Track in [GitHub Issues](https://github.com/yc20041001/ai-coding-platform/issues)
5. Update [Roadmap](docs/roadmap.md) with high-priority items

## 10. Where to Go Next

| Need | Document |
|------|----------|
| Understand architecture | [System Architecture](system-architecture.md) |
| All docs at a glance | [Documentation Index](documentation-index.md) |
| API endpoints | [API / Page / Script Index](api-page-script-index.md) |
| Environment variables | [Environment Variable Index](environment-variable-index.md) |
| Deploy to production | [Production Deployment Runbook](production-deployment-runbook.md) |
| Set up real models | [Model Provider Setup](model-provider-production-setup.md) |
| Set up GitHub OAuth | [GitHub OAuth Setup](github-oauth-production-setup.md) |
| Run a user trial | [Alpha/Beta Trial Plan](alpha-beta-trial-plan.md) |
| Final delivery overview | [Final Delivery Report](final-delivery-report.md) |
| Release readiness | [Final Release Checklist](final-release-checklist.md) |
