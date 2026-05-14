#!/bin/bash
set -e

# ============================================================
# dev-seed-demo-data.sh — 演示数据初始化脚本
# ============================================================
# 幂等性：通过检查项目名是否存在来跳过已有的 Demo 数据
# 依赖：后端已启动在 localhost:8080
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
AGENT_ID="300002"

echo "=========================================="
echo "  演示数据初始化"
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

# ---- Step 1: Login ----
echo "[1/8] 登录 admin..."
LOGIN_RESP=$(http_post "$BASE_URL/api/auth/login" \
  "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "")
TOKEN=$(extract_field "$LOGIN_RESP" "data.accessToken")

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "  [FAIL] 登录失败，请确保后端已启动"
  echo "  Response: $LOGIN_RESP"
  exit 1
fi
echo "  => Token obtained"
echo ""

# ---- Step 2: Find or create Demo Project ----
echo "[2/8] 创建/复用 Demo Project..."
PROJECTS_RESP=$(http_get "$BASE_URL/api/projects?page=1&pageSize=50" "$TOKEN")
EXISTING_ID=$(echo "$PROJECTS_RESP" | python3 -c "
import sys, json
data = json.load(sys.stdin)
records = data.get('data', {}).get('records', [])
for r in records:
    if r.get('name') == 'Demo Project':
        print(r.get('id', ''))
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_ID" ]; then
  PROJECT_ID="$EXISTING_ID"
  echo "  => 复用已有 Demo Project: $PROJECT_ID"
else
  CREATE_RESP=$(http_post "$BASE_URL/api/projects" \
    "{\"name\":\"Demo Project\",\"description\":\"Automated demo data project for Milestone 15\",\"techStack\":[\"Java\",\"Spring Boot\",\"Vue 3\",\"TypeScript\"]}" \
    "$TOKEN")
  PROJECT_ID=$(extract_field "$CREATE_RESP" "data.id")
  if [ -z "$PROJECT_ID" ] || [ "$PROJECT_ID" = "null" ]; then
    echo "  [FAIL] 创建项目失败"
    echo "  Response: $CREATE_RESP"
    exit 1
  fi
  echo "  => 创建 Demo Project: $PROJECT_ID"
fi
echo ""

# ---- Step 3: Create or reuse Demo Knowledge Base ----
echo "[3/8] 创建/复用 Demo Knowledge Base..."
KBS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases?page=1&pageSize=20" "$TOKEN")
EXISTING_KB_ID=$(echo "$KBS_RESP" | python3 -c "
import sys, json
data = json.load(sys.stdin)
records = data.get('data', {}).get('records', [])
for r in records:
    if r.get('name') == 'Demo Knowledge Base':
        print(r.get('id', ''))
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_KB_ID" ]; then
  KB_ID="$EXISTING_KB_ID"
  echo "  => 复用已有 Demo Knowledge Base: $KB_ID"
else
  KB_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases" \
    "{\"name\":\"Demo Knowledge Base\",\"description\":\"Automated demo KB for Milestone 15\",\"chunkSize\":300,\"chunkOverlap\":30}" \
    "$TOKEN")
  KB_ID=$(extract_field "$KB_RESP" "data.id")
  if [ -z "$KB_ID" ] || [ "$KB_ID" = "null" ]; then
    echo "  [WARN] 创建 KB 失败: $KB_RESP"
  else
    echo "  => 创建 Demo Knowledge Base: $KB_ID"
  fi
fi
echo ""

# ---- Step 4: Upload Demo Markdown Document ----
echo "[4/8] 上传 Demo Markdown Document..."
if [ -n "$KB_ID" ]; then
  DOC_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents" \
    "{\"knowledgeBaseId\":\"$KB_ID\",\"title\":\"Agent Orchestrator Guide\",\"documentType\":\"MARKDOWN\",\"sourceType\":\"MANUAL\",\"fileName\":\"agent-orchestrator-guide.md\",\"content\":\"# Agent Orchestrator Guide\n\n## Overview\n\nThe Agent Orchestrator is the core execution engine of the AI Coding Platform.\n\n## How It Works\n\n1. Receives a Task with an instruction\n2. Analyzes the instruction and determines required steps\n3. Executes steps sequentially or in parallel\n4. Generates code artifacts and logs\n5. Reports completion status\n\n## Integration with RAG\n\nThe orchestrator can query the knowledge base via RAG search to retrieve relevant context before execution. This improves code generation accuracy by providing project-specific context.\n\n## Configuration\n\n- `agentId`: The AI Agent to use\n- `useRag`: Enable/disable RAG context injection\n- `ragLimit`: Max chunks to retrieve (default 5)\"}" \
    "$TOKEN")
  DOC_ID=$(extract_field "$DOC_RESP" "data.id")
  if [ -z "$DOC_ID" ] || [ "$DOC_ID" = "null" ]; then
    echo "  [WARN] 上传文档失败: $DOC_RESP"
  else
    echo "  => Document uploaded: $DOC_ID"
  fi
else
  echo "  [SKIP] 无可用 Knowledge Base"
fi
echo ""

# ---- Step 5: Create Chat Session ----
echo "[5/8] 创建 Chat Session..."
SESSION_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/chat/sessions" \
  "{\"title\":\"Demo Chat Session\",\"sessionType\":\"PROJECT\"}" \
  "$TOKEN")
SESSION_ID=$(extract_field "$SESSION_RESP" "data.id")

if [ -z "$SESSION_ID" ] || [ "$SESSION_ID" = "null" ]; then
  echo "  [WARN] 创建 Session 失败: $SESSION_RESP"
else
  echo "  => Chat Session created: $SESSION_ID"
fi
echo ""

# ---- Step 6: Send Chat Message ----
echo "[6/8] 发送 Chat Message..."
if [ -n "$SESSION_ID" ]; then
  MSG_RESP=$(http_post "$BASE_URL/api/chat/sessions/$SESSION_ID/messages" \
    "{\"content\":\"Hello! Please explain what the Agent Orchestrator does.\",\"agentIds\":[\"$AGENT_ID\"],\"stream\":false,\"useRag\":false,\"ragLimit\":5}" \
    "$TOKEN")
  USER_MSG_ID=$(extract_field "$MSG_RESP" "data.userMessageId")
  ASSIST_MSG_ID=$(extract_field "$MSG_RESP" "data.assistantMessageId")
  if [ -z "$USER_MSG_ID" ] || [ "$USER_MSG_ID" = "null" ]; then
    echo "  [WARN] 发送消息失败: $MSG_RESP"
  else
    echo "  => User message: $USER_MSG_ID"
    echo "  => Assistant message: $ASSIST_MSG_ID"
  fi
else
  echo "  [SKIP] 无可用 Chat Session"
fi
echo ""

# ---- Step 7: Create Task ----
echo "[7/8] 创建 Demo Task..."
TASK_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
  "{\"title\":\"Demo Task - Implement Greeting API\",\"description\":\"Create a REST endpoint that returns a greeting message\",\"taskType\":\"FEATURE\",\"priority\":\"MEDIUM\",\"agentId\":\"$AGENT_ID\"}" \
  "$TOKEN")
TASK_ID=$(extract_field "$TASK_RESP" "data.id")

if [ -z "$TASK_ID" ] || [ "$TASK_ID" = "null" ]; then
  echo "  [WARN] 创建 Task 失败: $TASK_RESP"
else
  echo "  => Task created: $TASK_ID (status: PENDING)"
fi
echo ""

# ---- Step 8: Execute Task ----
echo "[8/8] 执行 Demo Task..."
if [ -n "$TASK_ID" ]; then
  EXEC_RESP=$(http_post "$BASE_URL/api/tasks/$TASK_ID/execute" \
    "{\"instruction\":\"Create a REST controller that returns a JSON greeting message\",\"agentId\":\"$AGENT_ID\",\"useRag\":false,\"ragLimit\":5}" \
    "$TOKEN")
  EXEC_STATUS=$(extract_field "$EXEC_RESP" "data.status")
  if [ "$EXEC_STATUS" = "COMPLETED" ]; then
    echo "  => Task executed successfully: COMPLETED"
  else
    echo "  [WARN] Task execution status: $EXEC_STATUS"
    echo "  Response: $EXEC_RESP"
  fi
else
  echo "  [SKIP] 无可用 Task"
fi
echo ""

# ---- Summary ----
echo "=========================================="
echo "  演示数据初始化完成"
echo "=========================================="
echo "  Project ID:       $PROJECT_ID"
echo "  Knowledge Base:   ${KB_ID:-N/A}"
echo "  Document:         ${DOC_ID:-N/A}"
echo "  Chat Session:     ${SESSION_ID:-N/A}"
echo "  Task:             ${TASK_ID:-N/A}"
echo "=========================================="
