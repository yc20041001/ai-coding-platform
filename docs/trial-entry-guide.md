# Trial Entry Guide

How to start a trial of AI Coding Platform — locally, via Docker, or on a demo server.

## 1. Prerequisites

| Requirement | Minimum | Notes |
|-------------|---------|-------|
| Java | 17 | OpenJDK or compatible |
| Maven | 3.x | Wrapper included (`mvnw`) |
| Node.js | 18+ | LTS recommended |
| Docker | 20+ | For infrastructure (MySQL, Redis, RabbitMQ) |
| Git | any | To clone the repo |

## 2. Clone & Setup

```bash
git clone https://github.com/yc20041001/ai-coding-platform.git
cd ai-coding-platform
```

## 3. Local Trial (Recommended First Step)

### 3.1 Start Infrastructure

```bash
docker compose -f deploy/docker-compose.yml up -d
```

This starts MySQL 8 (port 3307), Redis 7 (port 6379), and RabbitMQ 3 (ports 5672, 15672).

Check services:

```bash
docker compose -f deploy/docker-compose.yml ps
```

### 3.2 Configure Environment

```bash
cp .env.example .env
# Default values work out of the box for local development.
# No changes needed for a Mock-only trial.
```

### 3.3 Start Backend

```bash
cd backend
source ../.env
mvn spring-boot:run
```

Wait for `Started Application in ... seconds`.

Verify:

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 3.4 Start Frontend

```bash
cd frontend
npm install       # First time only
npm run dev -- --host 0.0.0.0
```

Open http://localhost:5173

### 3.5 Initialize Demo Data

```bash
# From project root, with backend running
bash scripts/demo-seed-data.sh
```

This creates:
- Demo AI Workspace project
- Product Knowledge Base with 3 Markdown documents
- Chat session "Ask Product Knowledge"
- 2 executed Tasks (REVIEW + FEATURE)

The script is idempotent — safe to run multiple times.

### 3.6 Login

| Field | Value |
|-------|-------|
| URL | http://localhost:5173/login |
| Email | `admin@example.com` |
| Password | `Admin@123456` |

## 4. Docker Trial (Full Stack)

```bash
# Build and start all services
docker compose -f deploy/docker-compose.app.yml up -d --build

# Check services
docker compose -f deploy/docker-compose.app.yml ps

# View logs
docker compose -f deploy/docker-compose.app.yml logs -f backend

# Stop
docker compose -f deploy/docker-compose.app.yml down
```

Access:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Health: http://localhost:8080/actuator/health

Then initialize demo data:

```bash
bash scripts/demo-seed-data.sh
```

## 5. Production Demo Server Trial

If a demo server has been deployed, the organizer will provide:
- Demo server URL
- Demo credentials
- Any environment-specific notes

The demo walkthrough is the same once logged in — see [Demo Walkthrough](demo-walkthrough.md).

## 6. What You Can Explore

| Area | What to Try |
|------|-------------|
| Dashboard | System metrics overview, project count |
| Project Workspace | 6 tabs: Overview, Tasks, Chat, Knowledge, Repository, Members |
| Knowledge Base | Upload docs, view chunks, RAG search with relevance scores |
| Chat | SSE streaming, RAG references, session management |
| Task Execution | Create/execute tasks, state machine (PENDING→RUNNING→COMPLETED), logs, artifacts |
| Model Gateway | Provider status, connection test, usage & cost panel |
| GitHub | Repository browsing, PR review (requires OAuth config) |
| Observability | System overview, model usage summary, audit logs |

## 7. Provider Configuration

### Default: Mock Provider

All Chat and Task responses are simulated. No API keys, no costs, no network calls.
Full feature exploration without any external dependencies.

### Adding Real Models

Edit `.env`:

```bash
# Example: Enable OpenAI
OPENAI_ENABLED=true
OPENAI_API_KEY=sk-your-key-here
OPENAI_MODEL=gpt-4.1-mini

# Or Claude
CLAUDE_ENABLED=true
CLAUDE_API_KEY=sk-ant-your-key-here
CLAUDE_MODEL=claude-3-5-sonnet-latest
```

Restart the backend after changing `.env`.

Then use the Model Gateway page in the console to:
1. Verify provider status (should show "Enabled")
2. Run a Connection Test
3. Check Usage & Cost panel

### Supported Providers

| Provider | Env Var Prefix | Requires |
|----------|---------------|----------|
| MOCK | (none) | Nothing — always available |
| OpenAI Compatible | `OPENAI_` | API Key |
| Claude (Anthropic) | `CLAUDE_` | API Key |
| DeepSeek | `DEEPSEEK_` | API Key |
| Qwen (Tongyi) | `QWEN_` | API Key |
| Gemini (Google) | `GEMINI_` | API Key |

## 8. Common Issues

### Backend won't start

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Access denied for user" | MySQL not running or wrong password | `docker compose -f deploy/docker-compose.yml up -d` |
| "Unknown database" | Database not created | Flyway auto-creates; ensure MySQL user has CREATE DATABASE privilege |
| "Port 8080 already in use" | Another process on 8080 | `lsof -i :8080` and kill the process |

### Frontend won't start

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Cannot find module" | Dependencies not installed | `npm install` |
| "Port 5173 already in use" | Another dev server | `lsof -i :5173` or use `--port 5174` |

### Demo data script fails

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Backend not reachable" | Backend not started | Start backend first |
| "Login failed" | Wrong credentials or backend error | Verify backend health: `curl http://localhost:8080/actuator/health` |

### Chat shows no RAG references

Make sure demo data is initialized (`bash scripts/demo-seed-data.sh`).
If using your own documents, re-upload and wait for chunking + embedding.

### Model Gateway shows all providers as Disabled

This is expected. MOCK is always enabled internally. Real providers only show
as Enabled after configuring their `*_ENABLED=true` and `*_API_KEY` env vars.

## 9. Reset & Cleanup

### Reset Demo Data

```bash
bash scripts/demo-reset-data.sh --yes
bash scripts/demo-seed-data.sh
```

### Full Teardown

```bash
# Stop Docker infrastructure
docker compose -f deploy/docker-compose.yml down

# Stop backend/frontend (Ctrl+C in each terminal)
```

## 10. Submitting Feedback

After your trial, please:

1. Fill in the [User Feedback Template](user-feedback-template.md)
2. Submit as a [GitHub Issue](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)

Your feedback will be:
- **Masked** — any accidentally included secrets removed
- **Classified** — using the [Product Feedback Taxonomy](product-feedback-taxonomy.md)
- **Prioritized** — P0 (immediate) through P3 (backlog)
- **Scheduled** — added to the [Roadmap](roadmap.md)

## 11. Next Steps After Trial

- Read the [Product Feedback Taxonomy](product-feedback-taxonomy.md) to understand how feedback is classified
- Check the [Roadmap](roadmap.md) to see planned features
- Review the [Changelog](../CHANGELOG.md) for version history
- Explore [Production Deployment Runbook](production-deployment-runbook.md) if deploying
