# Demo Walkthrough

面向演示者和试用者的操作指南。无需理解数据库、Flyway、MyBatis-Plus 等内部细节。

## 0. Public Entry（公开展示入口）

项目提供公开产品页面，无需登录即可了解平台定位和能力：

- 访问 `http://localhost:5173/public` 查看产品展示页
- 包含：产品定位、7 大功能展示、系统架构预览、试用入口、FAQ
- 明确标注 Mock Provider 默认、GitHub OAuth 可选、真实模型需配置 API Key
- 从 Public Home 点击 "Open Console" 进入登录页

如果你已了解产品，可直接跳到 [第 1 节](#1-before-you-start)。

## 1. Before You Start

### Prerequisites

- Backend running at `http://localhost:8080`
- Frontend running at `http://localhost:5173`（or access via nginx proxy）
- `curl` and `python3` available

### Start Backend

```bash
cd backend
source ../.env
mvn spring-boot:run
```

### Start Frontend

```bash
cd frontend
npm run dev -- --host 0.0.0.0
```

### Initialize Demo Data

```bash
bash scripts/demo-seed-data.sh
```

This creates: Demo Project, Knowledge Base (3 docs), Chat Session, 2 Tasks, and executes them. Idempotent — safe to run multiple times.

### Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@example.com | Admin@123456 |

---

## 2. 5-Minute Quick Demo

Target: Show the core value in 5 minutes flat.

### Step 1: Login (30s)

1. Open `http://localhost:5173/login`
2. Enter `admin@example.com` / `Admin@123456`
3. Click **Login**
4. Expected: Dashboard with metrics (Projects, Tasks, Chat, RAG, Model Calls)

**If login fails**: Check that backend is running (`curl http://localhost:8080/actuator/health`).

### Step 2: Open Demo Project (30s)

1. On the Dashboard, note the **Projects** count
2. Click **Projects** in the sidebar
3. Click **Demo AI Workspace** (or "Demo Project")
4. Expected: Project detail with 6 tabs (Overview, Tasks, Chat, Knowledge, Repository, Members)

### Step 3: Explore Knowledge Base (1min)

1. Click the **Knowledge** tab
2. Select **Product Knowledge Base**
3. You should see 3 documents: Platform Overview, Agent Workflow Guide, Repository Review Guide
4. Click a document to view its content
5. Enter "platform architecture" in the RAG search box and click **Search**
6. Expected: Chunks with relevance scores, click to preview

**If no documents appear**: Run `bash scripts/demo-seed-data.sh`.

### Step 4: Chat with RAG (1.5min)

1. Click the **Chat** tab
2. Select **Ask Product Knowledge** session (or create a new one)
3. Type: "请总结这个平台如何把 RAG、Agent 和任务执行串起来。"
4. Click **Send**
5. Expected:
   - SSE streaming output (characters appear one by one)
   - References section shows knowledge base chunks used
   - Message status changes to COMPLETED
6. Try another question: "What are the task states?"

**If SSE doesn't stream**: Check that nginx/CORS config has `proxy_buffering off` for `/api/`. Direct frontend-to-backend (port 5173→8080) should always work.

### Step 5: Execute a Task (1min)

1. Click the **Tasks** tab
2. Click on **Generate architecture review summary**
3. Click **Execute** (if status is PENDING)
4. Expected: Status transitions PENDING → RUNNING → COMPLETED
5. Click **Logs** / **Artifacts** / **Executions** tabs to see generated content
6. Check **Model Logs** to see the LLM call details

### Step 6: Observability (30s)

1. Click **Observability** in the sidebar
2. Expected:
   - System Overview: project/user/task/agent counts
   - Model Usage Summary: total calls, tokens, cost estimate
   - Audit Logs: recent actions
3. Point out: Every model call is logged with provider, model, tokens, cost

**If you see 403**: The admin account must have ADMIN role. Check the user's role in the database.

---

## 3. 15-Minute Deep Demo

Target: Full feature walkthrough with explanations.

### Part 1: Platform Architecture (2 min)

1. Dashboard → explain the 5 metrics in the flow strip (Projects → Tasks → Chat → RAG → Model Calls)
2. Explain: This is an enterprise AI coding collaboration platform
3. Tech stack: Spring Boot 3.3 + Vue 3 + MySQL + Redis + RabbitMQ
4. The platform is designed for teams to collaborate on code with AI assistance

### Part 2: Knowledge Base Deep Dive (3 min)

1. Create a NEW Knowledge Base (not the demo one)
   - Name: "Test KB"
   - chunkSize: 300, chunkOverlap: 30
2. Upload a sample Markdown document:
   ```markdown
   # API Design Guidelines
   - All APIs use RESTful conventions
   - Authentication via JWT Bearer token
   - Responses follow ApiResponse<T> format
   - Pagination uses page + pageSize
   ```
3. Show chunk preview — the document is automatically split into chunks
4. Run RAG search: "API authentication"
5. Explain: RAG retrieves relevant context for Chat and Task execution
6. Delete the Test KB (cleanup)

### Part 3: Model Gateway (3 min)

1. Click **Model Gateway** in the sidebar
2. Explain the Provider concept:
   - MOCK: Always available, returns simulated responses
   - OpenAI / Claude / DeepSeek / Qwen / Gemini: Requires API key
3. Show Provider status cards — which are enabled/disabled
4. Run a **Connection Test**:
   - Provider: MOCK
   - Model: mock-agent-model
   - Click **Test**
   - Expected: Success
5. Explain Fallback:
   - If a real provider fails → falls back to MOCK
   - Safety rejections do NOT fallback
6. Show the **Usage & Cost** panel:
   - Total calls, tokens, estimated cost
   - Per-provider breakdown

### Part 4: Chat with RAG References (2 min)

1. Go back to Demo Project → Chat
2. Create a new session: "Deep Dive Chat"
3. Send: "Explain the agent execution flow based on the knowledge base"
4. Expected:
   - Streaming reply (SSE)
   - References section shows matching knowledge base chunks
   - Each reference shows: title, chunk content, relevance score
5. Explain: The model receives RAG context in the prompt automatically

### Part 5: Task Execution & State Machine (2 min)

1. Go to Tasks tab
2. Click **Implement health check endpoint** (or create a new one)
3. Before executing, explain the Task form:
   - taskType: FEATURE / BUGFIX / REVIEW / REFACTOR
   - priority: LOW / MEDIUM / HIGH
   - agentId: which AI agent to use
4. Click **Execute**
5. Watch the execution: PENDING → RUNNING → COMPLETED
6. Explore the result tabs:
   - **Logs**: Step-by-step execution log
   - **Artifacts**: Generated code files
   - **Executions**: Execution records with timestamps
   - **Model Logs**: LLM call details (provider, model, tokens, cost)
7. Explain: Tasks can't be re-executed once COMPLETED (state machine safety)

### Part 6: GitHub Integration (2 min)

1. Click **Repository** in the sidebar
2. Explain the GitHub OAuth flow:
   - Read-only access via OAuth
   - Scopes: read:user, user:email, repo
   - Token encrypted at rest, never returned in API responses
3. If GitHub OAuth IS configured:
   - Show repository browsing
   - Show PR review feature (read-only — no automatic comments)
4. If GitHub OAuth is NOT configured:
   - Explain: This page shows a "Not Configured" state
   - Show the `.env.production.example` variables needed
   - No real OAuth required for the demo

### Part 7: Observability & Audit (1 min)

1. Click **Observability**
2. Walk through:
   - **Overview**: System-wide metrics
   - **Model Usage**: Token consumption, cost estimates
   - **Audit Logs**: Filter by action type (LOGIN, CREATE_PROJECT, etc.)
   - **Model Request Logs**: Every LLM call with status and cost
3. Explain: This is critical for production — track cost, debug issues, audit actions

### Part 8: Security Features (1 min)

1. Open an incognito/private browser window
2. Try to access `http://localhost:5173/projects` without logging in
3. Expected: Redirected to login page
4. Try the API directly: `curl http://localhost:8080/api/projects`
5. Expected: HTTP 401 Unauthorized
6. Explain: All APIs require JWT authentication. No backdoor.

---

## 4. Common Failures & Troubleshooting

### Login Fails

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Network Error" | Backend not running | `cd backend && mvn spring-boot:run` |
| "Invalid credentials" | Wrong password | Use `admin@example.com` / `Admin@123456` |
| 502 Bad Gateway | nginx misconfiguration | Check backend upstream is `backend:8080` |

### No Demo Data

| Symptom | Cause | Fix |
|---------|-------|-----|
| Empty project list | Demo data not seeded | `bash scripts/demo-seed-data.sh` |
| "Demo Project" missing | Wrong project name | Set `DEMO_PROJECT_NAME` env var |
| No documents in KB | Upload failed silently | Check backend logs for errors |

### Chat SSE Not Streaming

| Symptom | Cause | Fix |
|---------|-------|-----|
| Message appears all at once | Buffering in proxy | Check `proxy_buffering off` in nginx config |
| No reply at all | MOCK provider issue | Run connection test in Model Gateway page |
| "RAG search returned 0" | Documents not indexed | Re-upload documents, wait for chunking |

### Task Won't Execute

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Status must be PENDING" | Task already executed | Create a new task |
| "Agent not found" | Invalid agent ID | Check agent exists in Agents page |
| Timeout | Backend processing slow | Check backend logs, increase timeout |

### Model Provider Not Working

| Symptom | Cause | Fix |
|---------|-------|-----|
| Fallback to MOCK | Real provider not configured | Set `OPENAI_ENABLED=true` + `OPENAI_API_KEY` |
| AUTH_ERROR | Invalid API key | Check API key is correct and not expired |
| RATE_LIMITED | Too many requests | Wait and retry, or switch provider |
| TIMEOUT | Network issue | Check connectivity to provider's API |

### GitHub OAuth Not Working

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Not Configured" | No OAuth credentials | This is expected for demo — set up in `.env.production` |
| Redirect URI mismatch | Wrong callback URL | GitHub App settings → callback URL must match exactly |
| "State expired" | OAuth flow timed out | Restart OAuth flow (10-min state expiry) |

---

## 5. Demo Environment Detection

The platform identifies its environment mode:

- **MOCK Provider**: Frontend shows "Mock" badge on model-related pages, chat replies are simulated
- **Real Provider**: When real API keys are configured, model responses come from actual LLMs
- **GitHub Status**: Repository page shows whether OAuth is configured and bound

Users always know whether they're seeing real AI output or mock demonstrations.

---

## 6. After the Demo

### Collect Feedback

Share [docs/user-feedback-template.md](user-feedback-template.md) with trial users and ask them to:
1. Fill in the feedback template
2. Submit as a [GitHub Issue](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)

### Triage Feedback

Organizers should follow the [User Trial Triage Guide](user-trial-triage-guide.md):
1. **Collect** — gather all feedback forms and Issues
2. **Mask** — remove any secrets, tokens, or personal data
3. **Classify** — use [Product Feedback Taxonomy](product-feedback-taxonomy.md)
4. **Prioritize** — assign P0/P1/P2/P3
5. **Reproduce** — document environment and steps
6. **Schedule** — add to [Roadmap](roadmap.md)
7. **Verify** — test fixes
8. **Close** — update [Changelog](../CHANGELOG.md)

### Reset Demo Data (if needed)

```bash
bash scripts/demo-reset-data.sh --yes
bash scripts/demo-seed-data.sh
```

### Validate Demo Readiness

```bash
bash scripts/demo-smoke-test.sh
```
