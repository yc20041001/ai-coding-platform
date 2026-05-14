#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
AGENT_ID="${AGENT_ID:-300002}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

TOKEN=""
PROJECT_ID=""
KB_ID=""
DOCUMENT_ID=""
SESSION_ID=""
ASSISTANT_MESSAGE_ID=""
TASK_ID=""
EXECUTION_ID=""

log() {
  printf '\n\033[1;34m==> %s\033[0m\n' "$1"
}

pass() {
  printf '\033[1;32mPASS\033[0m %s\n' "$1"
}

fail() {
  printf '\033[1;31mFAIL\033[0m %s\n' "$1" >&2
  exit 1
}

json_value() {
  local file="$1"
  local expr="$2"
  python3 - "$file" "$expr" <<'PY'
import json
import sys

path = sys.argv[2].split(".")
with open(sys.argv[1], "r", encoding="utf-8") as f:
    data = json.load(f)

cur = data
for item in path:
    if item == "":
        continue
    if isinstance(cur, list):
        cur = cur[int(item)]
    else:
        cur = cur.get(item)
    if cur is None:
        print("")
        sys.exit(0)
print(cur)
PY
}

assert_code() {
  local file="$1"
  local expected="$2"
  local actual
  actual="$(json_value "$file" "code")"
  if [[ "$actual" != "$expected" ]]; then
    cat "$file" >&2
    fail "expected code=$expected, actual=$actual"
  fi
}

assert_non_empty() {
  local value="$1"
  local label="$2"
  if [[ -z "$value" || "$value" == "None" || "$value" == "null" ]]; then
    fail "$label is empty"
  fi
}

request_json() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local output="$4"

  if [[ -n "$body" ]]; then
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "$body" > "$output"
  else
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $TOKEN" > "$output"
  fi
}

log "Health check"
curl -sS "$BASE_URL/actuator/health" > "$TMP_DIR/health.json"
HEALTH_STATUS="$(json_value "$TMP_DIR/health.json" "status")"
[[ "$HEALTH_STATUS" == "UP" ]] || { cat "$TMP_DIR/health.json"; fail "actuator health is not UP"; }
pass "actuator health UP"

log "Login admin"
curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" > "$TMP_DIR/login.json"
assert_code "$TMP_DIR/login.json" "OK"
TOKEN="$(json_value "$TMP_DIR/login.json" "data.accessToken")"
assert_non_empty "$TOKEN" "accessToken"
pass "login admin"

log "Current user"
request_json GET "/api/auth/me" "" "$TMP_DIR/me.json"
assert_code "$TMP_DIR/me.json" "OK"
pass "current user"

UNIQUE="$(date +%s)"

log "Create project"
request_json POST "/api/projects" "{
  \"name\":\"Unified Smoke Project $UNIQUE\",
  \"description\":\"Unified regression smoke test project\",
  \"techStack\":[\"Java\",\"Spring Boot\",\"RAG\"]
}" "$TMP_DIR/project.json"
assert_code "$TMP_DIR/project.json" "OK"
PROJECT_ID="$(json_value "$TMP_DIR/project.json" "data.id")"
assert_non_empty "$PROJECT_ID" "projectId"
pass "created project $PROJECT_ID"

log "List projects"
request_json GET "/api/projects?page=1&pageSize=10" "" "$TMP_DIR/projects.json"
assert_code "$TMP_DIR/projects.json" "OK"
pass "list projects"

log "List agents"
request_json GET "/api/agents" "" "$TMP_DIR/agents.json"
assert_code "$TMP_DIR/agents.json" "OK"
pass "list agents"

log "Create knowledge base"
request_json POST "/api/projects/$PROJECT_ID/knowledge-bases" "{
  \"name\":\"Unified Test Knowledge Base $UNIQUE\",
  \"description\":\"Knowledge base for unified regression smoke test\",
  \"chunkSize\":200,
  \"chunkOverlap\":20
}" "$TMP_DIR/kb.json"
assert_code "$TMP_DIR/kb.json" "OK"
KB_ID="$(json_value "$TMP_DIR/kb.json" "data.id")"
assert_non_empty "$KB_ID" "knowledgeBaseId"
pass "created knowledge base $KB_ID"

log "Upload markdown document"
DOC_CONTENT="# Unified Test Document\n\nAgent Orchestrator uses RAG context from Knowledge Base. Chat SSE returns references. Model Gateway supports Mock fallback and OpenAI compatible providers."
request_json POST "/api/projects/$PROJECT_ID/knowledge-documents" "{
  \"knowledgeBaseId\":\"$KB_ID\",
  \"title\":\"Unified Test Document\",
  \"documentType\":\"MARKDOWN\",
  \"sourceType\":\"MANUAL\",
  \"fileName\":\"unified-test.md\",
  \"filePath\":\"docs/unified-test.md\",
  \"content\":\"$DOC_CONTENT\"
}" "$TMP_DIR/document.json"
assert_code "$TMP_DIR/document.json" "OK"
DOCUMENT_ID="$(json_value "$TMP_DIR/document.json" "data.id")"
assert_non_empty "$DOCUMENT_ID" "documentId"
pass "uploaded document $DOCUMENT_ID"

log "RAG search"
request_json POST "/api/projects/$PROJECT_ID/rag/search" "{
  \"query\":\"Agent Orchestrator\",
  \"knowledgeBaseId\":\"$KB_ID\",
  \"limit\":5,
  \"includeContent\":true
}" "$TMP_DIR/rag.json"
assert_code "$TMP_DIR/rag.json" "OK"
RAG_TOTAL="$(json_value "$TMP_DIR/rag.json" "data.total")"
[[ "${RAG_TOTAL:-0}" != "0" ]] || { cat "$TMP_DIR/rag.json"; fail "RAG search returned no results"; }
pass "RAG search returned $RAG_TOTAL result(s)"

log "Create chat session"
request_json POST "/api/projects/$PROJECT_ID/chat/sessions" "{
  \"title\":\"Unified Smoke Chat\",
  \"sessionType\":\"PROJECT\"
}" "$TMP_DIR/session.json"
assert_code "$TMP_DIR/session.json" "OK"
SESSION_ID="$(json_value "$TMP_DIR/session.json" "data.id")"
assert_non_empty "$SESSION_ID" "sessionId"
pass "created chat session $SESSION_ID"

log "Send chat message with RAG"
request_json POST "/api/chat/sessions/$SESSION_ID/messages" "{
  \"content\":\"Agent Orchestrator\",
  \"agentIds\":[\"$AGENT_ID\"],
  \"stream\":true,
  \"useRag\":true,
  \"knowledgeBaseId\":\"$KB_ID\",
  \"ragLimit\":5
}" "$TMP_DIR/send-message.json"
assert_code "$TMP_DIR/send-message.json" "OK"
ASSISTANT_MESSAGE_ID="$(json_value "$TMP_DIR/send-message.json" "data.assistantMessageId")"
assert_non_empty "$ASSISTANT_MESSAGE_ID" "assistantMessageId"
pass "send chat message with assistantMessageId=$ASSISTANT_MESSAGE_ID"

log "Chat SSE stream"
set +e
curl -sS -N --max-time 30 "$BASE_URL/api/chat/sessions/$SESSION_ID/stream?messageId=$ASSISTANT_MESSAGE_ID" \
  -H "Authorization: Bearer $TOKEN" > "$TMP_DIR/sse.txt"
SSE_EXIT=$?
set -e
grep -Eq "event:[[:space:]]*done" "$TMP_DIR/sse.txt" || { cat "$TMP_DIR/sse.txt" >&2; fail "SSE done event not found"; }
if [[ "$SSE_EXIT" != "0" && "$SSE_EXIT" != "18" && "$SSE_EXIT" != "28" ]]; then
  cat "$TMP_DIR/sse.txt" >&2
  fail "SSE request failed with exit=$SSE_EXIT"
fi
pass "SSE done event received"

log "Get chat messages"
request_json GET "/api/chat/sessions/$SESSION_ID/messages?limit=20" "" "$TMP_DIR/messages.json"
assert_code "$TMP_DIR/messages.json" "OK"
pass "get chat messages"

log "Create task"
request_json POST "/api/projects/$PROJECT_ID/tasks" "{
  \"title\":\"Unified Smoke Task\",
  \"description\":\"Execute task with Agent Orchestrator RAG context and model gateway fallback.\",
  \"taskType\":\"FEATURE\",
  \"priority\":\"MEDIUM\",
  \"agentId\":\"$AGENT_ID\"
}" "$TMP_DIR/task.json"
assert_code "$TMP_DIR/task.json" "OK"
TASK_ID="$(json_value "$TMP_DIR/task.json" "data.id")"
assert_non_empty "$TASK_ID" "taskId"
pass "created task $TASK_ID"

log "Execute task with RAG"
request_json POST "/api/tasks/$TASK_ID/execute" "{
  \"instruction\":\"请结合项目知识库执行这个任务\",
  \"useRag\":true,
  \"knowledgeBaseId\":\"$KB_ID\",
  \"ragLimit\":5
}" "$TMP_DIR/execute.json"
assert_code "$TMP_DIR/execute.json" "OK"
EXECUTION_ID="$(json_value "$TMP_DIR/execute.json" "data.id")"
assert_non_empty "$EXECUTION_ID" "executionId"
EXECUTION_STATUS="$(json_value "$TMP_DIR/execute.json" "data.status")"
[[ "$EXECUTION_STATUS" == "COMPLETED" ]] || { cat "$TMP_DIR/execute.json"; fail "execution status is not COMPLETED"; }
pass "execute task completed with executionId=$EXECUTION_ID"

log "Get task detail"
request_json GET "/api/tasks/$TASK_ID" "" "$TMP_DIR/task-detail.json"
assert_code "$TMP_DIR/task-detail.json" "OK"
pass "get task detail"

log "Get task logs"
request_json GET "/api/tasks/$TASK_ID/logs" "" "$TMP_DIR/task-logs.json"
assert_code "$TMP_DIR/task-logs.json" "OK"
pass "get task logs"

log "Get task artifacts"
request_json GET "/api/tasks/$TASK_ID/artifacts" "" "$TMP_DIR/task-artifacts.json"
assert_code "$TMP_DIR/task-artifacts.json" "OK"
pass "get task artifacts"

log "Get executions"
request_json GET "/api/tasks/$TASK_ID/executions" "" "$TMP_DIR/executions.json"
assert_code "$TMP_DIR/executions.json" "OK"
pass "get executions"

log "Get model logs"
request_json GET "/api/agent-executions/$EXECUTION_ID/model-logs" "" "$TMP_DIR/model-logs.json"
assert_code "$TMP_DIR/model-logs.json" "OK"
pass "get model logs"

log "Negative: no token"
curl -sS "$BASE_URL/api/projects" > "$TMP_DIR/no-token.json"
assert_code "$TMP_DIR/no-token.json" "UNAUTHORIZED"
pass "no token returns UNAUTHORIZED"

log "Negative: repeat execute completed task"
request_json POST "/api/tasks/$TASK_ID/execute" "{}" "$TMP_DIR/repeat-execute.json"
REPEAT_CODE="$(json_value "$TMP_DIR/repeat-execute.json" "code")"
[[ "$REPEAT_CODE" == "CONFLICT" ]] || { cat "$TMP_DIR/repeat-execute.json"; fail "repeat execute expected CONFLICT, actual=$REPEAT_CODE"; }
pass "repeat execute returns CONFLICT"

log "Unified backend smoke test completed"
printf 'Project ID: %s\nKnowledgeBase ID: %s\nDocument ID: %s\nSession ID: %s\nTask ID: %s\nExecution ID: %s\n' \
  "$PROJECT_ID" "$KB_ID" "$DOCUMENT_ID" "$SESSION_ID" "$TASK_ID" "$EXECUTION_ID"
