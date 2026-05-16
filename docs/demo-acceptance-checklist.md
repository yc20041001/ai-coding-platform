# Demo Acceptance Checklist

Demo 验收清单 — 确保演示环境在演示前处于就绪状态。

## 1. Environment Check

- [ ] Backend is running (`curl http://localhost:8080/actuator/health` returns UP)
- [ ] Frontend is running (`curl http://localhost:5173` returns 200)
- [ ] Database is migrated (Flyway)
- [ ] Demo data is initialized (`bash scripts/demo-seed-data.sh` exits 0)
- [ ] Demo smoke test passes (`bash scripts/demo-smoke-test.sh` exits 0)

## 2. Data Check

- [ ] Demo Project exists ("Demo AI Workspace" or "Demo Project")
- [ ] Knowledge Base exists with ≥ 2 documents
- [ ] RAG search returns results for "platform architecture"
- [ ] Chat session "Ask Product Knowledge" exists
- [ ] At least 1 Task exists (PENDING or COMPLETED)
- [ ] At least 1 Task has been executed (status = COMPLETED)
- [ ] Model Usage Summary shows data
- [ ] Audit Logs show login and data creation events

## 3. Core Flow Check

### Login
- [ ] Login with admin@example.com / Admin@123456 succeeds
- [ ] Wrong password returns error (not 200)
- [ ] Unauthenticated API access returns 401

### Dashboard
- [ ] Dashboard shows non-zero project count
- [ ] Dashboard shows non-zero task count
- [ ] Dashboard shows non-zero model request count

### Project
- [ ] Demo Project is listed in project list
- [ ] Project detail shows 6 tabs (Overview, Tasks, Chat, Knowledge, Repository, Members)
- [ ] Overview tab shows project telemetry

### Knowledge Base
- [ ] Knowledge Base list is non-empty
- [ ] Documents are visible with titles
- [ ] RAG search input works
- [ ] RAG search returns relevant chunks
- [ ] Chunk preview drawer opens with content

### Chat
- [ ] Chat session list is non-empty (or can create a new one)
- [ ] Creating a new session works
- [ ] Sending a message works
- [ ] SSE streaming output is visible (characters appear progressively)
- [ ] Message status shows COMPLETED after streaming
- [ ] References section shows when RAG is enabled
- [ ] Chat persists across page navigation (back to session)

### Task
- [ ] Task list is non-empty
- [ ] Task detail shows description, status, priority, task type
- [ ] Execute button works (on PENDING task)
- [ ] Task transitions: PENDING → RUNNING → COMPLETED
- [ ] Logs tab shows execution steps
- [ ] Artifacts tab shows generated files
- [ ] Executions tab shows execution records
- [ ] Model Logs tab shows LLM call details

### Model Gateway
- [ ] Provider list is accessible
- [ ] MOCK provider is listed as available
- [ ] Connection test for MOCK succeeds
- [ ] Provider status (enabled/disabled) is clearly shown
- [ ] Current provider mode (MOCK vs real) is visible

### Observability
- [ ] System overview metrics load
- [ ] Model usage summary loads
- [ ] Audit logs are visible
- [ ] Audit log filters work (if implemented)

### GitHub
- [ ] Repository page loads (may show "Not Configured")
- [ ] If OAuth configured: OAuth status shows configured/bound
- [ ] If OAuth not configured: clear message shown, no error

## 4. Negative Path Check

- [ ] Login with wrong password: rejected (401 or error message)
- [ ] Access protected API without token: rejected (401)
- [ ] Access admin-only API without admin role: rejected (403)
- [ ] Visit non-existent project: handled gracefully (404 or redirect)
- [ ] Send empty chat message: validation error (not crash)
- [ ] Execute already-COMPLETED task: rejected with clear message

## 5. Security Check

- [ ] No API keys or tokens are visible in frontend responses
- [ ] No API keys or tokens are visible in browser console/network tab
- [ ] `.env.production` is NOT tracked by git
- [ ] No real API key patterns in the repository
- [ ] JWT token is stored securely (httpOnly cookie or memory, not localStorage ideally)
- [ ] CORS does not echo arbitrary origins

## 6. UX Check

- [ ] Dark theme is consistent across all pages
- [ ] Loading states are shown (skeleton or spinner)
- [ ] Error states show meaningful messages (not raw stack traces)
- [ ] Empty states show helpful guidance (not blank pages)
- [ ] Navigation between pages does not lose context
- [ ] Demo/MOCK mode is clearly indicated where relevant
- [ ] No broken images or missing icons
- [ ] Text is readable (no contrast issues)

## 7. Pass / Block Criteria

### Pass Criteria
- All "Core Flow Check" items must pass
- Zero security violations (section 5)
- No critical UX issues (section 6)
- Demo smoke test exits 0

### Block Criteria (cannot proceed with demo)
- Backend won't start or crashes
- Login fails
- Chat SSE streaming is broken (no output at all)
- Task execution throws unhandled error
- Security vulnerability: unauthenticated access to protected data
- Real API keys or secrets exposed in responses

### Warning Criteria (demo OK but note issues)
- Real model provider not configured (using MOCK — explain to audience)
- GitHub OAuth not configured (explain to audience)
- Some pages slightly slow (>3s load)
- Minor UI glitch (alignment, overflow)
- E2E test failure unrelated to demo path

## 8. Pre-Demo Checklist (Day Of)

- [ ] `git status` — working tree clean (or intentional changes)
- [ ] `bash -n scripts/demo-seed-data.sh` — no syntax errors
- [ ] `bash scripts/demo-seed-data.sh` — data ready
- [ ] `bash scripts/demo-smoke-test.sh` — all checks pass
- [ ] Browser cache cleared
- [ ] Demo login credentials handy
- [ ] Demo walkthrough script open: [docs/demo-walkthrough.md](demo-walkthrough.md)
- [ ] Feedback form ready: [docs/user-feedback-template.md](user-feedback-template.md)
- [ ] Backend logs tail running (for debugging if needed)
