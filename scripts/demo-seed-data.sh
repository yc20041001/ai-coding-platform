#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# demo-seed-data.sh — Demo Data Initialization Script
# ============================================================
# Idempotent: checks for existing Demo-prefixed data before creating.
# Dependency: backend running at BASE_URL
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
DEMO_PROJECT_NAME="${DEMO_PROJECT_NAME:-Demo AI Workspace}"
AGENT_ID="${AGENT_ID:-300002}"

PASS=0; WARN=0; FAIL=0; SKIP=0

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
warn() { echo "  [WARN] $1"; WARN=$((WARN + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }
skip() { echo "  [SKIP] $1"; SKIP=$((SKIP + 1)); }

echo "=========================================="
echo "  Demo Data Initialization"
echo "=========================================="
echo "Base URL: $BASE_URL"
echo ""

# ---- helpers ----
http_post() {
  local url="$1"; local data="$2"; local token="$3"
  local headers=(-H "Content-Type: application/json")
  [ -n "$token" ] && headers+=(-H "Authorization: Bearer $token")
  curl -s -X POST "${headers[@]}" -d "$data" "$url"
}

http_get() {
  local url="$1"; local token="$2"
  curl -s -H "Authorization: Bearer $token" "$url"
}

extract_field() {
  local json="$1"; local path="$2"
  echo "$json" | python3 -c "
import sys, json
data = json.load(sys.stdin)
parts = '$path'.split('.')
for p in parts:
    if isinstance(data, dict):
        data = data.get(p, '')
    else:
        data = ''
print(data)
" 2>/dev/null || echo ""
}

contains_name() {
  local json="$1"; local name="$2"
  echo "$json" | python3 -c "
import sys, json
data = json.load(sys.stdin)
records = data.get('data', {}).get('records', [])
for r in records:
    if r.get('name') == '$name' or r.get('title') == '$name':
        print(r.get('id', ''))
        break
" 2>/dev/null || echo ""
}

# ---- Step 1: Login ----
echo "[1/8] Login as admin"
LOGIN_RESP=$(http_post "$BASE_URL/api/auth/login" \
  "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "")
TOKEN=$(extract_field "$LOGIN_RESP" "data.accessToken")

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  fail "Login failed — is backend running?"
  echo "  Response: ${LOGIN_RESP:0:200}"
  exit 1
fi
pass "Login successful"
echo ""

# ---- Step 2: Demo Project ----
echo "[2/8] Demo Project"
PROJECTS_RESP=$(http_get "$BASE_URL/api/projects?page=1&pageSize=50" "$TOKEN")
EXISTING_PROJ=$(contains_name "$PROJECTS_RESP" "$DEMO_PROJECT_NAME")

if [ -n "$EXISTING_PROJ" ]; then
  PROJECT_ID="$EXISTING_PROJ"
  pass "Demo Project already exists: $PROJECT_ID"
else
  CREATE_RESP=$(http_post "$BASE_URL/api/projects" \
    "{\"name\":\"$DEMO_PROJECT_NAME\",\"description\":\"Product demo workspace — explore Chat, RAG, Task, Agent and Model Gateway capabilities\",\"techStack\":[\"Java\",\"Spring Boot\",\"Vue 3\",\"TypeScript\",\"RAG\",\"AI Agent\"]}" \
    "$TOKEN")
  PROJECT_ID=$(extract_field "$CREATE_RESP" "data.id")
  if [ -z "$PROJECT_ID" ] || [ "$PROJECT_ID" = "null" ]; then
    fail "Failed to create Demo Project"
    echo "  Response: ${CREATE_RESP:0:200}"
    exit 1
  fi
  pass "Demo Project created: $PROJECT_ID"
fi
echo ""

# ---- Step 3: Demo Knowledge Base + Documents ----
echo "[3/8] Demo Knowledge Base & Documents"
KBS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases?page=1&pageSize=20" "$TOKEN")
EXISTING_KB=$(contains_name "$KBS_RESP" "Product Knowledge Base")

if [ -n "$EXISTING_KB" ]; then
  KB_ID="$EXISTING_KB"
  pass "Knowledge Base already exists: $KB_ID"
else
  KB_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases" \
    "{\"name\":\"Product Knowledge Base\",\"description\":\"Platform capabilities, agent workflow, and repository review guides\",\"chunkSize\":300,\"chunkOverlap\":30}" \
    "$TOKEN")
  KB_ID=$(extract_field "$KB_RESP" "data.id")
  if [ -z "$KB_ID" ] || [ "$KB_ID" = "null" ]; then
    warn "Failed to create Knowledge Base"
  else
    pass "Knowledge Base created: $KB_ID"
  fi
fi

# Upload 3 demo documents
if [ -n "${KB_ID:-}" ]; then
  DOCS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents?page=1&pageSize=50" "$TOKEN")

  # Doc 1: Platform Overview
  DOC1_NAME="AI Coding Platform Overview"
  DOC1_EXISTS=$(contains_name "$DOCS_RESP" "$DOC1_NAME")
  if [ -n "$DOC1_EXISTS" ]; then
    pass "Document \"$DOC1_NAME\" already exists"
  else
    http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents" \
      "{\"knowledgeBaseId\":\"$KB_ID\",\"title\":\"$DOC1_NAME\",\"documentType\":\"MARKDOWN\",\"sourceType\":\"MANUAL\",\"fileName\":\"platform-overview.md\",\"content\":\"# AI Coding Platform Overview\n\n## What It Does\n\nThe AI Coding Platform is an enterprise-grade collaborative coding environment powered by AI agents.\n\n## Core Capabilities\n\n- **Chat with Context**: Multi-session chat with SSE streaming, RAG-augmented replies, code references, and task-linked discussions.\n- **Task Orchestration**: AI agents execute feature development, code review, refactoring, and architecture analysis tasks.\n- **RAG Knowledge Base**: Upload project documents, chunk them automatically, and retrieve relevant context during Chat and Task execution.\n- **Model Gateway**: Unified LLM access with multi-provider support (OpenAI, Claude, DeepSeek, Qwen, Gemini), fallback, and cost tracking.\n- **GitHub Integration**: Read-only PR review with AI-generated suggestions, repository browsing, and OAuth authentication.\n- **Observability**: Audit logs, model usage tracking, token consumption, and cost analysis.\n\n## Architecture\n\n- Backend: Spring Boot 3.3 + MyBatis-Plus + Flyway + MySQL 8.0\n- Frontend: Vue 3 + TypeScript + Element Plus\n- Infra: Docker Compose + Nginx + Redis + RabbitMQ\"}" \
      "$TOKEN" > /dev/null 2>&1 && pass "Document \"$DOC1_NAME\" created" || warn "Document \"$DOC1_NAME\" upload failed"
  fi

  # Doc 2: Agent Workflow Guide
  DOC2_NAME="Agent Workflow Guide"
  if echo "$DOCS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(any(r.get('title')=='$DOC2_NAME' for r in records))" 2>/dev/null | grep -q "True"; then
    pass "Document \"$DOC2_NAME\" already exists"
  else
    http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents" \
      "{\"knowledgeBaseId\":\"$KB_ID\",\"title\":\"$DOC2_NAME\",\"documentType\":\"MARKDOWN\",\"sourceType\":\"MANUAL\",\"fileName\":\"agent-workflow-guide.md\",\"content\":\"# Agent Workflow Guide\n\n## Overview\n\nThe Agent Orchestrator is the core execution engine. It receives Tasks, analyzes instructions, and executes steps to produce code artifacts.\n\n## Execution Flow\n\n1. **Task Created** — User creates a Task with a title, description, task type (FEATURE/BUGFIX/REVIEW/REFACTOR), and priority.\n2. **Execute Triggered** — User clicks Execute, optionally providing a custom instruction and enabling RAG context.\n3. **Orchestrator Runs** — The orchestrator:\n   - Loads relevant RAG chunks if useRag=true\n   - Builds a prompt with task context + instruction + RAG context\n   - Calls the Model Gateway (resolved provider → fallback to MOCK if needed)\n   - Parses the response for code artifacts\n4. **Artifacts Generated** — Code files, logs, and execution records are saved.\n5. **Task Completes** — Status transitions: PENDING → RUNNING → COMPLETED (or FAILED).\n\n## Task States\n\n- PENDING: Ready to execute\n- RUNNING: Orchestrator is processing\n- COMPLETED: Execution succeeded, artifacts available\n- FAILED: Execution failed, error logs available\n- CANCELLED: Manually cancelled by user\n\n## Integration Points\n\n- **RAG**: Injects knowledge base context into the prompt\n- **Model Gateway**: Routes LLM calls through provider resolution → fallback\n- **Model Request Log**: Records every LLM call with tokens, cost, and status\"}" \
      "$TOKEN" > /dev/null 2>&1 && pass "Document \"$DOC2_NAME\" created" || warn "Document \"$DOC2_NAME\" upload failed"
  fi

  # Doc 3: Repository Review Guide
  DOC3_NAME="Repository Review Guide"
  if echo "$DOCS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(any(r.get('title')=='$DOC3_NAME' for r in records))" 2>/dev/null | grep -q "True"; then
    pass "Document \"$DOC3_NAME\" already exists"
  else
    http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents" \
      "{\"knowledgeBaseId\":\"$KB_ID\",\"title\":\"$DOC3_NAME\",\"documentType\":\"MARKDOWN\",\"sourceType\":\"MANUAL\",\"fileName\":\"repository-review-guide.md\",\"content\":\"# Repository Review Guide\n\n## GitHub PR Review (Read-Only)\n\nThe platform can review GitHub Pull Requests without making any write operations.\n\n## How It Works\n\n1. User connects their GitHub account via OAuth (read:user, user:email, repo scopes).\n2. User selects a repository and a PR number.\n3. The platform fetches PR metadata (title, author, branches, file list, diff/patch).\n4. An AI Agent analyzes the diff and generates review suggestions.\n5. Review results are displayed in the UI — no automatic comments or commits.\n\n## Security\n\n- GitHub token is encrypted at rest (accessTokenEnc), never returned in API responses.\n- No GitHub write operations are ever performed.\n- PR review prompt contains only PR metadata and diff — no tokens, no secrets.\n- OAuth state parameter uses UUID with 10-minute expiry (PENDING → USED).\n\n## Repository Browsing\n\n- Browse public repositories without OAuth.\n- Full repository access requires OAuth with repo scope.\n- File tree, file content, and commit history are available.\"}" \
      "$TOKEN" > /dev/null 2>&1 && pass "Document \"$DOC3_NAME\" created" || warn "Document \"$DOC3_NAME\" upload failed"
  fi
fi
echo ""

# ---- Step 4: Demo Chat Session ----
echo "[4/8] Demo Chat Session"
SESSIONS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/chat/sessions?page=1&pageSize=20" "$TOKEN")
EXISTING_SESSION=$(echo "$SESSIONS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    if r.get('title') == 'Ask Product Knowledge':
        print(r.get('id',''))
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_SESSION" ]; then
  SESSION_ID="$EXISTING_SESSION"
  pass "Chat Session already exists: $SESSION_ID"
else
  SESSION_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/chat/sessions" \
    "{\"title\":\"Ask Product Knowledge\",\"sessionType\":\"PROJECT\"}" \
    "$TOKEN")
  SESSION_ID=$(extract_field "$SESSION_RESP" "data.id")
  if [ -z "$SESSION_ID" ] || [ "$SESSION_ID" = "null" ]; then
    warn "Failed to create Chat Session"
  else
    pass "Chat Session created: $SESSION_ID"
  fi
fi

# Send a demo message
if [ -n "${SESSION_ID:-}" ]; then
  MSGS_RESP=$(http_get "$BASE_URL/api/chat/sessions/$SESSION_ID/messages?limit=50" "$TOKEN")
  MSG_COUNT=$(echo "$MSGS_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
  if [ "${MSG_COUNT:-0}" -gt 0 ]; then
    pass "Chat messages already exist ($MSG_COUNT messages)"
  else
    MSG_RESP=$(http_post "$BASE_URL/api/chat/sessions/$SESSION_ID/messages" \
      "{\"content\":\"Please summarize how the platform integrates RAG, Agents, and Task execution.\",\"agentIds\":[\"$AGENT_ID\"],\"stream\":false,\"useRag\":true,\"ragLimit\":5}" \
      "$TOKEN")
    USER_MSG=$(extract_field "$MSG_RESP" "data.userMessageId")
    ASSIST_MSG=$(extract_field "$MSG_RESP" "data.assistantMessageId")
    if [ -n "$USER_MSG" ] && [ "$USER_MSG" != "null" ]; then
      pass "Demo chat message sent (user=$USER_MSG, assistant=$ASSIST_MSG)"
    else
      warn "Failed to send demo chat message"
    fi
  fi
fi
echo ""

# ---- Step 5: Demo Task ----
echo "[5/8] Demo Task"
TASKS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/tasks?page=1&pageSize=20" "$TOKEN")
EXISTING_TASK=$(echo "$TASKS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    if r.get('title') == 'Generate architecture review summary':
        print(r.get('id',''))
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_TASK" ]; then
  TASK_ID="$EXISTING_TASK"
  pass "Demo Task already exists: $TASK_ID"
else
  TASK_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
    "{\"title\":\"Generate architecture review summary\",\"description\":\"Ask the AI agent to analyze the platform architecture based on the knowledge base and generate a review summary\",\"taskType\":\"REVIEW\",\"priority\":\"MEDIUM\",\"agentId\":\"$AGENT_ID\"}" \
    "$TOKEN")
  TASK_ID=$(extract_field "$TASK_RESP" "data.id")
  if [ -z "$TASK_ID" ] || [ "$TASK_ID" = "null" ]; then
    warn "Failed to create Demo Task"
  else
    pass "Demo Task created: $TASK_ID"
  fi
fi
echo ""

# ---- Step 6: Create a second Demo Task ----
echo "[6/8] Second Demo Task (Code Generation)"
TASK2_NAME="Implement health check endpoint"
EXISTING_TASK2=$(echo "$TASKS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    if r.get('title') == '$TASK2_NAME':
        print(r.get('id',''))
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_TASK2" ]; then
  TASK2_ID="$EXISTING_TASK2"
  pass "Task \"$TASK2_NAME\" already exists: $TASK2_ID"
else
  TASK2_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
    "{\"title\":\"$TASK2_NAME\",\"description\":\"Implement a REST health check endpoint that returns service status\",\"taskType\":\"FEATURE\",\"priority\":\"HIGH\",\"agentId\":\"$AGENT_ID\"}" \
    "$TOKEN")
  TASK2_ID=$(extract_field "$TASK2_RESP" "data.id")
  if [ -z "$TASK2_ID" ] || [ "$TASK2_ID" = "null" ]; then
    warn "Failed to create second Demo Task"
  else
    pass "Task \"$TASK2_NAME\" created: $TASK2_ID"
  fi
fi
echo ""

# ---- Step 7: Execute Demo Task (if PENDING) ----
echo "[7/8] Execute Demo Tasks"

execute_if_pending() {
  local task_id="$1"
  local label="$2"
  local detail_resp
  detail_resp=$(http_get "$BASE_URL/api/tasks/$task_id" "$TOKEN")
  local status
  status=$(extract_field "$detail_resp" "data.status")

  if [ "$status" = "PENDING" ]; then
    local exec_resp
    exec_resp=$(http_post "$BASE_URL/api/tasks/$task_id/execute" \
      "{\"instruction\":\"Execute this task using the knowledge base for context\",\"agentId\":\"$AGENT_ID\",\"useRag\":true,\"ragLimit\":5}" \
      "$TOKEN")
    local exec_status
    exec_status=$(extract_field "$exec_resp" "data.status")
    if [ "$exec_status" = "COMPLETED" ]; then
      pass "$label executed: COMPLETED"
    elif [ "$exec_status" = "RUNNING" ]; then
      warn "$label execution started (RUNNING — may still be processing)"
    else
      warn "$label execution status: $exec_status"
    fi
  elif [ "$status" = "COMPLETED" ]; then
    pass "$label already COMPLETED"
  elif [ "$status" = "RUNNING" ]; then
    pass "$label currently RUNNING"
  else
    warn "$label status: $status (cannot execute)"
  fi
}

if [ -n "${TASK_ID:-}" ]; then
  execute_if_pending "$TASK_ID" "Task 1 (Review)"
fi
if [ -n "${TASK2_ID:-}" ]; then
  execute_if_pending "$TASK2_ID" "Task 2 (Feature)"
fi
echo ""

# ---- Step 8: Verify Demo Data Availability ----
echo "[8/8] Verify Demo Data"

check_endpoint() {
  local url="$1"
  local label="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$url" 2>/dev/null || echo "000")
  if [ "$code" = "200" ]; then
    pass "$label accessible"
  elif [ "$code" = "403" ]; then
    warn "$label returned 403 (may need ADMIN role)"
  else
    warn "$label returned HTTP $code"
  fi
}

check_endpoint "$BASE_URL/api/observability/overview" "Observability Overview"
check_endpoint "$BASE_URL/api/audit/logs?page=1&pageSize=5" "Audit Logs"

# Model usage summary check
USAGE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/observability/model-usage/summary" 2>/dev/null || echo "000")
if [ "$USAGE_CODE" = "200" ]; then
  pass "Model Usage Summary accessible"
else
  skip "Model Usage Summary (HTTP $USAGE_CODE, may need admin)"
fi

# Check Model Gateway providers
PROV_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/model-gateway/providers" 2>/dev/null || echo "000")
if [ "$PROV_CODE" = "200" ]; then
  pass "Model Gateway providers accessible"
else
  skip "Model Gateway providers (HTTP $PROV_CODE)"
fi

# Check if MOCK is active
MODEL_PROVIDER="${MODEL_GATEWAY_PROVIDER:-MOCK}"
if [ "$MODEL_PROVIDER" = "MOCK" ]; then
  warn "Using MOCK model provider — chat replies are simulated"
else
  pass "Using real model provider: $MODEL_PROVIDER"
fi
echo ""

# ---- Summary ----
echo "=========================================="
echo "  Demo Data Initialization Complete"
echo "=========================================="
echo "  Project:           ${PROJECT_ID:-N/A}"
echo "  Knowledge Base:    ${KB_ID:-N/A}"
echo "  Chat Session:      ${SESSION_ID:-N/A}"
echo "  Task 1 (Review):   ${TASK_ID:-N/A}"
echo "  Task 2 (Feature):  ${TASK2_ID:-N/A}"
echo "=========================================="
echo " Results: $PASS passed, $WARN warnings, $FAIL failed, $SKIP skipped"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "Status: FAILED — some items could not be created"
  exit 1
else
  echo "Status: PASSED — demo data ready"
  echo ""
  echo "Next steps:"
  echo "  1. Open http://localhost:5173 and login: admin@example.com / Admin@123456"
  echo "  2. Dashboard → Open Demo AI Workspace project"
  echo "  3. Explore Knowledge → Chat → Tasks → Observability"
  exit 0
fi
