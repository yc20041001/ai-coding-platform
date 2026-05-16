# Public Website Content

Copy and messaging guidelines for the AI Coding Platform public website (`/public`).

## Hero

**Headline:** AI Coding Platform

**Tagline:** AI-native workspace for projects, agents, knowledge, and code review.

**Description:**
A unified collaboration console that connects project context, knowledge base RAG,
agent task execution, and GitHub PR review — with full observability and audit trail.
Start exploring with built-in Mock provider, or bring your own API keys for real models.

**Primary CTA:** Open Console → `/login`
**Secondary CTAs:**
- View GitHub → `https://github.com/yc20041001/ai-coding-platform`
- Read Walkthrough → `docs/demo-walkthrough.md`
- View Roadmap → `docs/roadmap.md`

**Status Badge:** Internal Alpha — Mock Provider by Default

**Disclaimer:**
No API key required to explore. Default: Mock Provider.
GitHub OAuth is optional. Real model access requires your own API keys.

---

## Capabilities (Feature Showcase)

### 1. Project Workspace
Multi-tab project console with Overview, Tasks, Chat, Knowledge, Repository, and Members.
Each project is a self-contained AI collaboration unit.
**Status:** Ready

### 2. Knowledge Base & RAG
Upload documents, auto-chunk, embed, and search. RAG context is automatically injected
into Chat prompts and Task executions with relevance scoring.
**Status:** Ready

### 3. Chat with SSE Streaming
Real-time SSE streaming chat with RAG reference highlighting. Each response shows
which knowledge chunks were used, with relevance scores.
**Status:** Ready

### 4. Agent Task Execution
Create and execute AI agent tasks (FEATURE, BUGFIX, REVIEW, REFACTOR). Full state
machine: PENDING → RUNNING → COMPLETED, with logs, artifacts, and model call traces.
**Status:** Ready

### 5. Model Gateway
Unified LLM access layer supporting OpenAI, Claude, DeepSeek, Qwen, Gemini + Mock.
Connection testing, fallback strategy, cost estimation, and prompt safety filtering.
**Status:** Ready

### 6. GitHub PR Review
Read-only GitHub OAuth integration. Browse repositories and review pull requests
with AI-assisted analysis. No automatic comments or pushes.
**Status:** Ready

### 7. Observability & Audit
System-wide metrics dashboard, per-project model usage and cost summaries,
full audit log with action filtering, and model request traceability.
**Status:** Ready

---

## Architecture (Preview Text)

```
Frontend Console (Vue 3 SPA, Dark Tech UI, SSE Streaming)
  → Spring Boot API (REST Controllers, JWT Auth, RBAC Permissions)
  → Core Modules (Project, Task, Chat, RAG, Agent, Repository)
  → Model Gateway (Multi-Provider, Fallback, Cost Tracking, Safety)
  → Integrations (GitHub OAuth, PR Review)
  → Infrastructure (MySQL 8, Redis 7, RabbitMQ, Docker)

Observability & Audit spans all layers.
```

---

## Trial Entry

### Quick Start

```bash
# 1. Start infrastructure
docker compose -f deploy/docker-compose.yml up -d

# 2. Start backend
cd backend
source ../.env
mvn spring-boot:run

# 3. Start frontend
cd frontend
npm install
npm run dev -- --host 0.0.0.0

# 4. Initialize demo data
bash scripts/demo-seed-data.sh
```

### Demo Login

- Email: `admin@example.com`
- Password: `Admin@123456`

### Important Notes

- **Mock Provider by Default:** Chat and Task responses are simulated. No real LLM API calls are made unless you configure API keys.
- **GitHub OAuth is Optional:** Repository browsing and PR review work only after configuring GitHub OAuth credentials.
- **Real Models Need API Keys:** Set `*_ENABLED=true` and `*_API_KEY` in your `.env` file.

---

## FAQ

### 1. Is this a production-ready product?
The platform is currently in Internal Alpha (v1.0). All core modules are functional
and tested, but it has not been battle-tested under production load. Suitable for
demos, trials, and internal team use.

### 2. Does it call real AI models by default?
No. The platform defaults to a built-in Mock Provider that returns simulated responses.
This means you can explore every feature without any API keys or external costs.

### 3. Is GitHub OAuth required?
No. GitHub OAuth is entirely optional. The Repository and PR Review features show
a "Not Configured" state until you set up credentials.

### 4. Does any data leave my machine?
When using Mock Provider: no. All data stays local. When using real model providers,
prompts and RAG context are sent to that provider's API per their data policies.

### 5. How do I start the demo?
Start infrastructure, backend, frontend, run `bash scripts/demo-seed-data.sh`,
login with `admin@example.com` / `Admin@123456`. See Trial Entry Guide for details.

### 6. How do I submit feedback?
File a GitHub Issue using the Bug Report, Feature Request, or Trial Feedback templates.
We triage using an 8-step process and 6-category taxonomy.

### 7. How do I connect a real AI model?
Edit `.env` and set `*_ENABLED=true` plus `*_API_KEY` for each provider. Then
configure models in the Model Gateway console page.

### 8. What are the current limitations?
Single-admin user, Mock responses by default, no multi-user registration (planned v1.2),
no real-time collaboration, webhook/CI integration planned but not yet implemented,
no mobile optimization.

---

## Provider Status Declarations

| Provider | Default | Requires |
|----------|---------|----------|
| MOCK | Enabled | Nothing |
| OpenAI | Disabled | `OPENAI_ENABLED=true` + `OPENAI_API_KEY` |
| Claude | Disabled | `CLAUDE_ENABLED=true` + `CLAUDE_API_KEY` |
| DeepSeek | Disabled | `DEEPSEEK_ENABLED=true` + `DEEPSEEK_API_KEY` |
| Qwen | Disabled | `QWEN_ENABLED=true` + `QWEN_API_KEY` |
| Gemini | Disabled | `GEMINI_ENABLED=true` + `GEMINI_API_KEY` |

**MOCK is the only provider enabled by default.** All real model providers must be
explicitly enabled and configured with valid API keys.

---

## CTA Link Targets

| CTA | Target | Auth Required |
|-----|--------|---------------|
| Open Console | `/login` | Yes (login page) |
| View GitHub | `https://github.com/yc20041001/ai-coding-platform` | No |
| Read Walkthrough | `docs/demo-walkthrough.md` (GitHub) | No |
| View Roadmap | `docs/roadmap.md` (GitHub) | No |
| Submit Feedback | GitHub Issues `/new?template=user_trial_feedback.yml` | No (GitHub account) |

---

## Content Principles

1. **No hype language** — state what exists, not what could be.
2. **No misleading** — clearly separate Mock vs Real, Demo vs Production.
3. **No registration wall** — public page and docs are freely accessible.
4. **Dark tech console aesthetic** — consistent with product UI but standalone layout.
5. **Action-oriented** — every section has clear next steps.
