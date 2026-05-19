# AI Coding Platform

**AI-native workspace for projects, agents, knowledge, and code review.**

A unified collaboration console that connects project context, knowledge base RAG,
agent task execution, and GitHub PR review — with full observability and audit trail.

[![Backend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml)
[![Docker Build](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml)

**Status:** Internal Alpha v1.0 — functional and tested, suitable for demos and trials.
[Public Overview Page](http://localhost:5173/public) · [Live Demo](#demo-quick-start) · [Roadmap](docs/roadmap.md) · [Changelog](CHANGELOG.md)

> **Mock Provider by Default.** No API keys needed to explore. Real models (OpenAI, Claude, DeepSeek, etc.)
> require explicit configuration. GitHub OAuth is optional. See [Security](#security) for data handling.

---

## Core Capabilities

| Module | Description | Status |
|--------|-------------|--------|
| **Project Workspace** | Multi-tab console: Overview, Tasks, Chat, Knowledge, Repository, Members | Ready |
| **Knowledge Base & RAG** | Document upload, auto-chunking, embedding, semantic search with relevance scoring | Ready |
| **Chat with SSE** | Real-time streaming chat with RAG reference highlighting and source attribution | Ready |
| **Agent Task Execution** | FEATURE / BUGFIX / REVIEW / REFACTOR tasks with full state machine and artifact tracking | Ready |
| **Model Gateway** | Unified LLM access — OpenAI, Claude, DeepSeek, Qwen, Gemini + Mock. Fallback, cost estimation, safety filtering. | Ready |
| **GitHub PR Review** | Read-only OAuth integration for repository browsing and AI-assisted PR review | Ready |
| **Observability & Audit** | System metrics, per-project model usage/cost, full audit log, model request traceability | Ready |

---

## Demo Quick Start

```bash
# 1. Clone
git clone https://github.com/yc20041001/ai-coding-platform.git
cd ai-coding-platform

# 2. Start infrastructure (MySQL, Redis, RabbitMQ)
docker compose -f deploy/docker-compose.yml up -d

# 3. Start backend
cd backend && source ../.env && mvn spring-boot:run

# 4. Start frontend (new terminal)
cd frontend && npm install && npm run dev -- --host 0.0.0.0

# 5. Initialize demo data (new terminal)
bash scripts/demo-seed-data.sh
```

Open **http://localhost:5173** and login:
- Email: `admin@example.com`
- Password: `Admin@123456`

Then follow the [Demo Walkthrough](docs/demo-walkthrough.md) for a guided experience.

See the [Trial Entry Guide](docs/trial-entry-guide.md) for Docker trial and production demo options.

---

## Architecture

```
Frontend Console (Vue 3 SPA, Dark Tech UI, SSE Streaming)
  → Spring Boot API (REST Controllers, JWT Auth, RBAC)
  → Core Modules (Project, Task, Chat, RAG, Agent, Repository)
  → Model Gateway (Multi-Provider, Fallback, Cost Tracking, Safety)
  → Integrations (GitHub OAuth, PR Review)
  → Infrastructure (MySQL 8, Redis 7, RabbitMQ, Docker)

Observability & Audit spans all layers.
```

---

## Security

- **Mock Provider by default** — no data leaves your machine unless real models are configured
- **API keys masked** in all responses and logs (`sk-****abcd`)
- **No secrets tracked** — `.env`, `.env.production`, cert/key files in `.gitignore`
- **Prompt safety** — 32 high-risk patterns intercepted before reaching any model
- **JWT authentication** on all API endpoints; no backdoor
- **Read-only GitHub OAuth** — no automatic comments, commits, or pushes
- All real model API keys must be explicitly set via `*_ENABLED=true` + `*_API_KEY`

For production deployment security, see [Security Hardening Checklist](docs/production-security-hardening-checklist.md).

## Authentication & Security

- **Login Captcha** — 4-character code with configurable expiry and attempt limits (Redis-backed, memory fallback)
- **Login Attempt Protection** — Brute-force protection with email/IP-based lockout (5 failures locks for 10 minutes)
- **JWT tokens** — Access + refresh token flow with configurable expiry
- **RBAC** — Role-based permissions on projects and modules

---

## Feedback & Iteration

- [Bug Report](https://github.com/yc20041001/ai-coding-platform/issues/new?template=bug_report.yml)
- [Feature Request](https://github.com/yc20041001/ai-coding-platform/issues/new?template=feature_request.yml)
- [Trial Feedback](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
- [Feedback Taxonomy](docs/product-feedback-taxonomy.md)
- [Triage Guide](docs/user-trial-triage-guide.md)
- [Roadmap](docs/roadmap.md)
- [Changelog](CHANGELOG.md)
- [Alpha/Beta Trial Plan](docs/alpha-beta-trial-plan.md)

---

## Technology Stack

| Component | Version / Tech |
|-----------|---------------|
| Java | 17 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7 |
| Flyway | 10.20.1 |
| MySQL | 8.0 |
| Redis | 7 |
| RabbitMQ | 3 (management) |
| Maven | 3.x |
| JWT | jjwt 0.12.x |
| Spring Security | 6.x |
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + Pinia |

---

## Project Structure

```
ai-coding-platform/
├── backend/                    # Spring Boot backend
│   └── src/main/java/com/aicoding/platform/
│       ├── agent/              # Agent module
│       ├── audit/              # Audit log module
│       ├── auth/               # Authentication & authorization
│       ├── chat/               # Chat sessions / messages / SSE
│       ├── common/             # Shared (exception / pagination / response)
│       ├── member/             # Project members / permissions
│       ├── modelgateway/       # Model gateway (multi-provider)
│       ├── observability/      # Observability (usage / overview)
│       ├── orchestrator/       # Agent orchestrator
│       ├── project/            # Project module
│       ├── rag/                # RAG knowledge base
│       ├── repository/         # Code repository (GitHub)
│       ├── security/           # Security (JWT / filters)
│       └── task/               # Task module
├── frontend/                   # Vue 3 enterprise console
│   └── src/modules/
│       ├── admin/              # Observability / audit logs
│       ├── agent/              # Agent management
│       ├── auth/               # Login / auth store
│       ├── chat/               # Chat sessions (SSE)
│       ├── dashboard/          # Dashboard
│       ├── knowledge/          # Knowledge base
│       ├── member/             # Member management
│       ├── project/            # Project management
│       ├── public/             # Public landing page (/public)
│       ├── repository/         # Repository management
│       └── task/               # Task management / execution details
├── deploy/                     # Deployment config
│   └── docker-compose.yml      # Local Docker Compose
├── docs/                       # Documentation
├── scripts/                    # Utility scripts
├── .env.example                # Environment variable template
└── README.md                   # This file
```

---

## Local Dependencies

Start infrastructure before running the application:

```bash
# Start all infrastructure
docker compose -f deploy/docker-compose.yml up -d

# Check service status
docker compose -f deploy/docker-compose.yml ps

# Stop services
docker compose -f deploy/docker-compose.yml down
```

- MySQL 8.0 (port 3307)
- Redis 7 (port 6379)
- RabbitMQ 3 (port 5672, Management 15672)

---

## Environment Variables

```bash
cp .env.example .env
source .env
```

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| DB_URL | Database connection | jdbc:mysql://127.0.0.1:3307/ai_coding_platform |
| DB_USERNAME | Database user | root |
| DB_PASSWORD | Database password | platform123 |
| JWT_SECRET | JWT signing key | (min 256 bits) |
| MODEL_GATEWAY_PROVIDER | Default model provider | MOCK |
| MODEL_GATEWAY_TIMEOUT_MS | Request timeout (ms) | 60000 |
| MODEL_GATEWAY_RETRY_TIMES | Retry attempts | 1 |
| OPENAI_ENABLED | Enable OpenAI | false |
| OPENAI_API_KEY | OpenAI API Key | (requires setup) |
| CLAUDE_ENABLED | Enable Claude | false |
| CLAUDE_API_KEY | Claude API Key | (requires setup) |
| DEEPSEEK_ENABLED | Enable DeepSeek | false |
| DEEPSEEK_API_KEY | DeepSeek API Key | (requires setup) |
| QWEN_ENABLED | Enable Qwen | false |
| QWEN_API_KEY | Qwen API Key | (requires setup) |
| GEMINI_ENABLED | Enable Gemini | false |
| GEMINI_API_KEY | Gemini API Key | (requires setup) |

---

## Backend

```bash
cd backend
mvn clean compile
mvn test
source ../.env
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/actuator/health
```

Login to get a token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

---

## Frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev -- --host 0.0.0.0
```

Open http://localhost:5173.

**Public Entry:** http://localhost:5173/public — product overview, no login required.

Default login:

```
Email: admin@example.com
Password: Admin@123456
```

See [frontend/README.md](frontend/README.md) for details.

---

## Testing

| Layer | Technology | Command |
|-------|-----------|---------|
| Backend integration | JUnit 5 + Spring Boot Test | `cd backend && mvn test` |
| Frontend E2E | Playwright | `cd frontend && npm run test:e2e` |
| Smoke test | Shell (curl) | `bash scripts/demo-smoke-test.sh` |

### Quality Gates

| Gate | Type | Command |
|------|------|---------|
| Backend tests | **Blocking** | `cd backend && mvn test` |
| Frontend typecheck | **Blocking** | `cd frontend && npm run typecheck` |
| Frontend build | **Blocking** | `cd frontend && npm run build` |
| E2E tests (×2) | **Blocking** | `cd frontend && npm run test:e2e -- --workers=1` |
| Bundle check | Warning | `bash scripts/frontend-bundle-check.sh` |
| Secret scan | **Blocking** | Checked in `scripts/release-checklist.sh` |

See [Testing Strategy](docs/testing-strategy.md), [Backend Testing Guide](docs/backend-testing-guide.md), [Backend Test Matrix](docs/backend-test-matrix.md).

### One-Click Checks

```bash
bash scripts/run-backend-checks.sh   # Backend compile + test + package
bash scripts/run-frontend-checks.sh  # Frontend typecheck + build + E2E
bash scripts/run-all-checks.sh       # Everything
```

### Demo Scripts

```bash
bash scripts/demo-seed-data.sh       # Initialize demo data (idempotent)
bash scripts/demo-smoke-test.sh      # Run demo smoke test
bash scripts/demo-reset-data.sh --yes # Clean up demo data
```

See [Demo Data Guide](docs/demo-data-guide.md), [Demo Walkthrough](docs/demo-walkthrough.md), [Acceptance Checklist](docs/demo-acceptance-checklist.md), [Feedback Template](docs/user-feedback-template.md).

---

## Docker Deployment

### Full Stack

```bash
docker compose -f deploy/docker-compose.app.yml up -d --build
docker compose -f deploy/docker-compose.app.yml logs -f backend
docker compose -f deploy/docker-compose.app.yml down
```

### Production

```bash
cp .env.production.example .env.production
# Edit .env.production, replace all CHANGE_ME values
bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost
```

Access:

| Service | URL |
|---------|-----|
| Frontend Console | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Health Check | http://localhost:8080/actuator/health |
| RabbitMQ Management | http://localhost:15672 |

**Production Operations:** [Health Check](scripts/prod-health-check.sh) · [Alerting](docs/production-alerting-rules.md) · [Observability](docs/production-observability-runbook.md) · [Security Hardening](docs/production-security-hardening-checklist.md) · [Incident Response](docs/incident-response-runbook.md)

---

## Documentation & Handoff

**Final Delivery Package (v1.0 Alpha):**

| Document | Purpose |
|----------|---------|
| [Final Delivery Report](docs/final-delivery-report.md) | Completion status, module inventory, quality gates, known limitations |
| [Project Handoff Guide](docs/project-handoff-guide.md) | Day 1 setup, testing, troubleshooting, releasing |
| [Documentation Index](docs/documentation-index.md) | All ~70 docs organized by topic |
| [API / Page / Script Index](docs/api-page-script-index.md) | All endpoints, routes, and utility scripts |
| [Environment Variable Index](docs/environment-variable-index.md) | All env vars with security levels |
| [Final Release Checklist](docs/final-release-checklist.md) | 10-section pre-release verification |

**Quick Reference:** [Known Limitations](docs/final-delivery-report.md#6-known-limitations) · [Quality Gates](docs/final-delivery-report.md#4-quality-gate-summary) · [Deployment Modes](docs/final-delivery-report.md#5-deployment-modes) · [Roadmap](docs/roadmap.md) · [Changelog](CHANGELOG.md)

---

## CI/CD

| Workflow | |
|----------|-----|
| [![Backend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml) | Backend compile → test → package |
| [![Frontend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml) | Frontend typecheck → build |
| [![Docker Build](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml) | Docker image build → GHCR |

Images:

```
ghcr.io/yc20041001/ai-coding-platform-backend:latest
ghcr.io/yc20041001/ai-coding-platform-frontend:latest
```

---

## API Documentation

### Audit Logs

| Method | Endpoint | Permission |
|--------|----------|------------|
| GET | /api/audit/logs | ADMIN |
| GET | /api/projects/{id}/audit/logs | ADMIN |

### Observability

| Method | Endpoint | Permission |
|--------|----------|------------|
| GET | /api/observability/overview | ADMIN |
| GET | /api/projects/{id}/observability/overview | ADMIN |
| GET | /api/observability/model-usage/summary | ADMIN |
| GET | /api/projects/{id}/observability/model-usage/summary | ADMIN |
| GET | /api/projects/{id}/observability/model-usage/daily | ADMIN |

---

## Database

Flyway manages schema migrations:

```
backend/src/main/resources/db/migration/
```

Migration versions:
- V1: Auth tables
- V2: Admin seed data
- V3: Project tables
- V4: Repository tables
- V5: Agent and Task tables
- V6: Chat session/message/reference tables
- V7: Orchestrator and model gateway tables
- V8: RAG knowledge base tables
- V9: Audit log tables
- V10: Model request log enhancements (fallback/error codes/cost estimation)

---

## Model Gateway

Multi-provider unified LLM access:

| Provider | Non-Streaming | Streaming | API Key Required |
|----------|--------------|-----------|-----------------|
| MOCK | Yes | Yes | No |
| OpenAI Compatible | Yes | Yes | Yes |
| Claude (Anthropic) | Yes | Yes | Yes |
| DeepSeek | Yes | Yes | Yes |
| Qwen | Yes | Yes | Yes |
| Gemini (Google) | Yes | Yes | Yes |

### API Key Safety
- API keys masked in all responses and logs (`sk-****abcd`)
- Frontend config page shows only masked keys
- Prompt Safety: 32 high-risk patterns intercepted before reaching any model

### Fallback Strategy
1. Provider unavailable/disabled → Fallback to MOCK
2. Network timeout/rate limit → Auto-retry then Fallback to MOCK
3. Prompt Safety rejection → No Fallback, direct rejection
4. Request-level `fallbackEnabled=false` → Skip Fallback, return error

---

## FAQ

**Q: Startup fails with "Access denied for user"?**
Ensure MySQL is running via Docker Compose and `DB_PASSWORD` matches `docker-compose.yml`.

**Q: Flyway migration fails?**
Check that database `ai_coding_platform` exists, or create it and restart.

**Q: JWT secret too short?**
Ensure `JWT_SECRET` is at least 256 bits (32 characters).

**Q: Model calls fail?**
Default provider is Mock, no API key needed. For real models, configure `*_ENABLED=true` and `*_API_KEY`.

**Q: SSE streaming has no response?**
Check `MODEL_GATEWAY_PROVIDER` config. Mock mode has 150ms character interval output.
