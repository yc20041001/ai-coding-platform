#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# demo-smoke-test.sh — Demo Smoke Test
# ============================================================
# Validates that the demo environment is ready for user trials.
# Checks frontend, login, demo data, core APIs, model gateway,
# chat, task, observability, and security.
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"
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
echo "  Demo Smoke Test"
echo "=========================================="
echo "Backend:  $BASE_URL"
echo "Frontend: $FRONTEND_URL"
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

# ---- 1. Frontend ----
echo "[1] Frontend Availability"
FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$FRONTEND_URL/" 2>/dev/null || echo "000")
if [ "$FRONTEND_CODE" = "200" ] || [ "$FRONTEND_CODE" = "304" ]; then
  pass "Frontend reachable (HTTP $FRONTEND_CODE)"
else
  warn "Frontend returned HTTP $FRONTEND_CODE (may not be running)"
fi
echo ""

# ---- 2. Login ----
echo "[2] Authentication"
LOGIN_RESP=$(http_post "$BASE_URL/api/auth/login" \
  "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "")
TOKEN=$(extract_field "$LOGIN_RESP" "data.accessToken")

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  fail "Login failed — backend may not be running"
  echo "  Response: ${LOGIN_RESP:0:200}"
  echo ""
  echo "=========================================="
  echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped"
  echo " Status:  FAILED — cannot proceed without login"
  exit 1
fi
pass "Login successful"
echo ""

# ---- 3. Auth/me ----
echo "[3] Core Identity"
ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/auth/me" 2>/dev/null || echo "000")
if [ "$ME_CODE" = "200" ]; then
  pass "/api/auth/me OK"
else
  fail "/api/auth/me returned HTTP $ME_CODE"
fi
echo ""

# ---- 4. Demo Project ----
echo "[4] Demo Project"
PROJECTS_RESP=$(http_get "$BASE_URL/api/projects?page=1&pageSize=50" "$TOKEN")
PROJECT_ID=$(echo "$PROJECTS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    n = r.get('name','')
    if '$DEMO_PROJECT_NAME' in n or 'Demo Project' in n:
        print(r.get('id',''))
        break
" 2>/dev/null || echo "")

if [ -n "$PROJECT_ID" ]; then
  pass "Demo Project found: $PROJECT_ID"
else
  fail "Demo Project not found — run: bash scripts/demo-seed-data.sh"
fi
echo ""

# ---- 5. Knowledge Base ----
echo "[5] Knowledge Base & RAG"
if [ -n "${PROJECT_ID:-}" ]; then
  KBS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases?page=1&pageSize=20" "$TOKEN")
  KB_COUNT=$(echo "$KBS_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('records',[])))" 2>/dev/null || echo "0")

  if [ "${KB_COUNT:-0}" -gt 0 ]; then
    pass "Knowledge Bases found: $KB_COUNT"
    KB_ID=$(echo "$KBS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
print(records[0].get('id','') if records else '')
" 2>/dev/null || echo "")

    if [ -n "$KB_ID" ]; then
      DOCS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/knowledge-documents?page=1&pageSize=20" "$TOKEN")
      DOC_COUNT=$(echo "$DOCS_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('records',[])))" 2>/dev/null || echo "0")
      if [ "${DOC_COUNT:-0}" -gt 0 ]; then
        pass "Documents found: $DOC_COUNT"
      else
        warn "No documents in Knowledge Base"
      fi

      RAG_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/knowledge-bases/$KB_ID/rag-search" \
        "{\"query\":\"platform architecture\",\"topK\":3}" "$TOKEN")
      RAG_HITS=$(echo "$RAG_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
      if [ "${RAG_HITS:-0}" -gt 0 ]; then
        pass "RAG search returned $RAG_HITS results"
      else
        warn "RAG search returned 0 results (may need documents indexed)"
      fi
    fi
  else
    warn "No Knowledge Bases found — run demo-seed-data.sh"
  fi
else
  skip "No project — Knowledge Base checks skipped"
fi
echo ""

# ---- 6. Chat ----
echo "[6] Chat"
if [ -n "${PROJECT_ID:-}" ]; then
  SESSIONS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/chat/sessions?page=1&pageSize=20" "$TOKEN")
  SESSION_ID=$(echo "$SESSIONS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
print(records[0].get('id','') if records else '')
" 2>/dev/null || echo "")

  if [ -n "$SESSION_ID" ]; then
    pass "Chat session exists: $SESSION_ID"
  else
    warn "No existing chat session — creating one"

    SESSION_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/chat/sessions" \
      "{\"title\":\"Smoke Test Chat\",\"sessionType\":\"PROJECT\"}" "$TOKEN")
    SESSION_ID=$(extract_field "$SESSION_RESP" "data.id")

    if [ -z "$SESSION_ID" ] || [ "$SESSION_ID" = "null" ]; then
      fail "Failed to create chat session"
    else
      pass "Chat session created"
    fi
  fi

  if [ -n "${SESSION_ID:-}" ]; then
    MSG_RESP=$(http_post "$BASE_URL/api/chat/sessions/$SESSION_ID/messages" \
      "{\"content\":\"Hello, this is a smoke test message.\",\"agentIds\":[\"$AGENT_ID\"],\"stream\":false}" "$TOKEN")
    USER_MSG=$(extract_field "$MSG_RESP" "data.userMessageId")
    if [ -n "$USER_MSG" ] && [ "$USER_MSG" != "null" ]; then
      pass "Chat message sent successfully"
    else
      fail "Chat message send failed"
    fi
  fi
else
  skip "No project — Chat checks skipped"
fi
echo ""

# ---- 7. Task ----
echo "[7] Task"
if [ -n "${PROJECT_ID:-}" ]; then
  TASKS_RESP=$(http_get "$BASE_URL/api/projects/$PROJECT_ID/tasks?page=1&pageSize=20" "$TOKEN")
  TASK_ID=$(echo "$TASKS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
print(records[0].get('id','') if records else '')
" 2>/dev/null || echo "")

  if [ -n "$TASK_ID" ]; then
    pass "Task exists: $TASK_ID"

    TASK_DETAIL=$(http_get "$BASE_URL/api/tasks/$TASK_ID" "$TOKEN")
    TASK_STATUS=$(extract_field "$TASK_DETAIL" "data.status")
    if [ "$TASK_STATUS" = "COMPLETED" ]; then
      pass "Task status: COMPLETED"
    elif [ "$TASK_STATUS" = "PENDING" ]; then
      pass "Task status: PENDING (ready to execute)"
    else
      pass "Task status: $TASK_STATUS"
    fi
  else
    warn "No tasks found — creating one"

    TASK_RESP=$(http_post "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
      "{\"title\":\"Smoke Test Task\",\"description\":\"Auto-created by smoke test\",\"taskType\":\"FEATURE\",\"priority\":\"LOW\",\"agentId\":\"$AGENT_ID\"}" "$TOKEN")
    TASK_ID=$(extract_field "$TASK_RESP" "data.id")

    if [ -n "$TASK_ID" ] && [ "$TASK_ID" != "null" ]; then
      pass "Task created: $TASK_ID"
    else
      fail "Failed to create task"
    fi
  fi
else
  skip "No project — Task checks skipped"
fi
echo ""

# ---- 8. Model Gateway ----
echo "[8] Model Gateway"
PROVIDERS_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/model-gateway/providers" 2>/dev/null || echo "000")
if [ "$PROVIDERS_CODE" = "200" ]; then
  pass "Model providers list accessible"

  PROV_RESP=$(http_get "$BASE_URL/api/model-gateway/providers" "$TOKEN")
  MOCK_AVAILABLE=$(echo "$PROV_RESP" | python3 -c "import sys,json; providers=json.load(sys.stdin).get('data',[]); print(any(p.get('provider')=='MOCK' for p in providers))" 2>/dev/null || echo "False")
  if [ "$MOCK_AVAILABLE" = "True" ]; then
    pass "MOCK provider available (always-on fallback)"
  fi

  CONN_RESP=$(curl -s -k --max-time 30 -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"provider":"MOCK","modelName":"mock-agent-model"}' \
    "$BASE_URL/api/model-gateway/test-connection" 2>/dev/null || echo "")
  CONN_SUCCESS=$(echo "$CONN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('success',''))" 2>/dev/null || echo "")
  if [ "$CONN_SUCCESS" = "True" ]; then
    pass "MOCK connection test successful"
  else
    warn "MOCK connection test returned unexpected response"
  fi
else
  warn "Model Gateway providers not accessible (HTTP $PROVIDERS_CODE)"
fi

# Check if real provider is configured
MODEL_PROVIDER="${MODEL_GATEWAY_PROVIDER:-MOCK}"
if [ "$MODEL_PROVIDER" = "MOCK" ]; then
  warn "Running on MOCK provider — chat replies are simulated. Set MODEL_GATEWAY_PROVIDER + *_API_KEY for real models."
else
  pass "Real model provider configured: $MODEL_PROVIDER"
fi
echo ""

# ---- 9. Observability & Audit ----
echo "[9] Observability & Audit"
OVERVIEW_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/observability/overview" 2>/dev/null || echo "000")
if [ "$OVERVIEW_CODE" = "200" ]; then
  pass "Observability overview accessible"
elif [ "$OVERVIEW_CODE" = "403" ]; then
  warn "Observability overview requires ADMIN role"
else
  warn "Observability overview returned HTTP $OVERVIEW_CODE"
fi

AUDIT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/audit/logs?page=1&pageSize=5" 2>/dev/null || echo "000")
if [ "$AUDIT_CODE" = "200" ]; then
  pass "Audit logs accessible"
elif [ "$AUDIT_CODE" = "403" ]; then
  warn "Audit logs require ADMIN role"
else
  warn "Audit logs returned HTTP $AUDIT_CODE"
fi
echo ""

# ---- 10. Unauthenticated Access ----
echo "[10] Security — Unauthenticated Access"
UNAUTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/api/projects" 2>/dev/null || echo "000")
if [ "$UNAUTH_CODE" = "401" ] || [ "$UNAUTH_CODE" = "403" ]; then
  pass "Unauthenticated access blocked (HTTP $UNAUTH_CODE)"
else
  fail "Unauthenticated access NOT blocked — HTTP $UNAUTH_CODE (expected 401/403)"
fi
echo ""

# ---- Summary ----
echo "=========================================="
echo "  Demo Smoke Test Complete"
echo "=========================================="
echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "Status: FAILED — some checks did not pass"
  echo ""
  echo "Troubleshooting:"
  echo "  1. Ensure backend is running: cd backend && mvn spring-boot:run"
  echo "  2. Run demo data seed: bash scripts/demo-seed-data.sh"
  echo "  3. Check backend logs for errors"
  exit 1
else
  echo "Status: PASSED — demo environment is ready for user trials"
  echo ""
  echo "Next steps:"
  echo "  1. Open $FRONTEND_URL in browser"
  echo "  2. Login: $ADMIN_EMAIL / $ADMIN_PASSWORD"
  echo "  3. Follow the demo walkthrough: docs/demo-walkthrough.md"
  exit 0
fi
