#!/usr/bin/env bash
set -euo pipefail

# Production external services smoke test.
# Tests model providers and GitHub OAuth integration against a running instance.
# No real API keys are printed.
# Unconfigured services → SKIP, not FAIL.
# Login failure → FAIL.

if [ $# -lt 1 ]; then
  echo "Usage: bash scripts/prod-external-services-smoke-test.sh <base-url>"
  echo "Example: bash scripts/prod-external-services-smoke-test.sh https://example.com"
  echo "         bash scripts/prod-external-services-smoke-test.sh http://localhost"
  exit 1
fi

BASE_URL="${1%/}"

TEST_ADMIN_EMAIL="${TEST_ADMIN_EMAIL:-admin@example.com}"
TEST_ADMIN_PASSWORD="${TEST_ADMIN_PASSWORD:-Admin@123456}"
TEST_MODEL_PROVIDER="${TEST_MODEL_PROVIDER:-MOCK}"
TEST_MODEL_NAME="${TEST_MODEL_NAME:-mock-agent-model}"
TEST_GITHUB_REPO_FULL_NAME="${TEST_GITHUB_REPO_FULL_NAME:-}"
TEST_GITHUB_PR_NUMBER="${TEST_GITHUB_PR_NUMBER:-}"

PASS=0
FAIL=0
SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " Production External Services Smoke Test"
echo " Target: $BASE_URL"
echo "============================================"
echo ""

# ---- 1. Login ----
echo "[1] Authentication"
LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${TEST_ADMIN_EMAIL}\",\"password\":\"${TEST_ADMIN_PASSWORD}\"}" \
  "${BASE_URL}/api/auth/login" 2>/dev/null || echo "")

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
  fail "Login failed — cannot proceed with authenticated tests"
  echo "  Raw response: ${LOGIN_RESP:0:200}"
  echo ""
  echo "============================================"
  echo " Results: $PASS passed, $FAIL failed, $SKIP skipped"
  echo " Status:  FAILED"
  exit 1
fi
pass "Login successful"
AUTH=(-H "Authorization: Bearer $TOKEN")
echo ""

# ---- 2. Model Provider Options ----
echo "[2] Model Provider Options"
PROVIDERS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "${BASE_URL}/api/model-gateway/providers" 2>/dev/null || echo "")
if echo "$PROVIDERS_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',''))" 2>/dev/null | grep -q .; then
  pass "GET /api/model-gateway/providers"
else
  fail "GET /api/model-gateway/providers — no data returned"
fi
echo ""

# ---- 3. Connection Test ----
echo "[3] Connection Test"

test_connection() {
  local provider="$1"
  local model="$2"
  local label="$3"

  local resp
  resp=$(curl -s -k --max-time 30 -X POST \
    -H "Content-Type: application/json" \
    "${AUTH[@]}" \
    -d "{\"provider\":\"${provider}\",\"modelName\":\"${model}\"}" \
    "${BASE_URL}/api/model-gateway/test-connection" 2>/dev/null || echo "")

  local success
  success=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('success',''))" 2>/dev/null || echo "")

  local message
  message=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('message',''))" 2>/dev/null || echo "")

  local error_code
  error_code=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('errorCode',''))" 2>/dev/null || echo "")

  if [ "$success" = "True" ]; then
    pass "${label} connection test — success (${message})"
  elif [ -n "$error_code" ]; then
    echo "  INFO: ${label} connection test — ${error_code}: ${message}"
    if [ "$error_code" = "CONFIG_ERROR" ] || [ "$error_code" = "PROVIDER_NOT_FOUND" ]; then
      skip "${label} — not configured or not found"
    else
      fail "${label} connection test failed: ${error_code} — ${message}"
    fi
  else
    fail "${label} connection test — unexpected response"
  fi
}

# Always test MOCK
test_connection "MOCK" "mock-agent-model" "MOCK"

# Test additional providers if configured
for prov in OPENAI CLAUDE DEEPSEEK QWEN GEMINI; do
  enabled_var="${prov}_ENABLED"
  if [ "${!enabled_var:-false}" = "true" ]; then
    test_connection "$prov" "" "$prov"
  fi
done
echo ""

# ---- 4. Chat SSE ----
echo "[4] Chat Session & Message"

# Get a project ID
PROJECTS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "${BASE_URL}/api/projects?page=1&size=10" 2>/dev/null || echo "")
PROJECT_ID=$(echo "$PROJECTS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(records[0].get('id','') if records else '')" 2>/dev/null || echo "")

if [ -z "$PROJECT_ID" ]; then
  skip "No project found — chat tests skipped"
else
  # Create session
  SESSION_RESP=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    "${AUTH[@]}" \
    -d '{"title":"Smoke Test Session","sessionType":"PROJECT"}' \
    "${BASE_URL}/api/projects/${PROJECT_ID}/chat/sessions" 2>/dev/null || echo "")

  SESSION_ID=$(echo "$SESSION_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")

  if [ -z "$SESSION_ID" ]; then
    fail "Failed to create chat session"
  else
    pass "Chat session created (id=${SESSION_ID})"

    # Send message
    MSG_RESP=$(curl -s -k --max-time 10 -X POST \
      -H "Content-Type: application/json" \
      "${AUTH[@]}" \
      -d '{"content":"Hello, this is a smoke test. Reply briefly.","stream":false}' \
      "${BASE_URL}/api/chat/sessions/${SESSION_ID}/messages" 2>/dev/null || echo "")

    ASSISTANT_ID=$(echo "$MSG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('assistantMessageId',''))" 2>/dev/null || echo "")

    if [ -n "$ASSISTANT_ID" ]; then
      pass "Chat message sent (assistantMessageId=${ASSISTANT_ID})"
      echo "  INFO: SSE stream available at GET /api/chat/sessions/${SESSION_ID}/stream?messageId=${ASSISTANT_ID}"
    else
      fail "Chat message send failed"
    fi
  fi
fi
echo ""

# ---- 5. GitHub OAuth Status ----
echo "[5] GitHub OAuth Status"

GH_STATUS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "${BASE_URL}/api/github/oauth/status" 2>/dev/null || echo "")
GH_CONFIGURED=$(echo "$GH_STATUS_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('configured',''))" 2>/dev/null || echo "")
GH_BOUND=$(echo "$GH_STATUS_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('bound',''))" 2>/dev/null || echo "")
GH_LOGIN=$(echo "$GH_STATUS_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('githubLogin',''))" 2>/dev/null || echo "")

if [ "$GH_CONFIGURED" = "True" ]; then
  pass "GitHub OAuth configured"
  if [ "$GH_BOUND" = "True" ]; then
    pass "GitHub account bound (login=${GH_LOGIN})"
  else
    skip "GitHub OAuth configured but no account bound yet"
  fi
else
  skip "GitHub OAuth not configured (GITHUB_CLIENT_ID/GITHUB_CLIENT_SECRET not set)"
fi
echo ""

# ---- 6. Repository / PR (if bound) ----
echo "[6] Repository & PR Check"

if [ "$GH_BOUND" = "True" ] && [ -n "$TEST_GITHUB_REPO_FULL_NAME" ]; then
  OWNER=$(echo "$TEST_GITHUB_REPO_FULL_NAME" | cut -d/ -f1)
  REPO=$(echo "$TEST_GITHUB_REPO_FULL_NAME" | cut -d/ -f2)

  REPOS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "${BASE_URL}/api/github/repositories?page=1&size=10" 2>/dev/null || echo "")
  REPO_COUNT=$(echo "$REPOS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(len(records))" 2>/dev/null || echo "0")

  if [ "${REPO_COUNT:-0}" -gt 0 ]; then
    pass "Repository list returned ${REPO_COUNT} repos"

    if [ -n "$TEST_GITHUB_PR_NUMBER" ]; then
      PR_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" \
        "${BASE_URL}/api/github/repositories/${OWNER}/${REPO}/pulls/${TEST_GITHUB_PR_NUMBER}" 2>/dev/null || echo "")
      PR_TITLE=$(echo "$PR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('title',''))" 2>/dev/null || echo "")
      if [ -n "$PR_TITLE" ]; then
        pass "PR #${TEST_GITHUB_PR_NUMBER} loaded: ${PR_TITLE}"
      else
        fail "Failed to load PR #${TEST_GITHUB_PR_NUMBER}"
      fi
    fi
  else
    fail "No repositories found (check GitHub binding and permissions)"
  fi
else
  skip "GitHub not bound or TEST_GITHUB_REPO_FULL_NAME not set"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL + SKIP))
echo "============================================"
echo " Results: $PASS passed, $FAIL failed, $SKIP skipped ($TOTAL total)"
if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED ($FAIL failures)"
  exit 1
else
  echo " Status:  PASSED"
  exit 0
fi
