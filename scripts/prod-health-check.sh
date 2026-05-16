#!/usr/bin/env bash
set -euo pipefail

# Production health check script.
# Usage: bash scripts/prod-health-check.sh <base-url>
# Example: bash scripts/prod-health-check.sh https://example.com
#          bash scripts/prod-health-check.sh http://localhost

if [ $# -lt 1 ]; then
  echo "Usage: bash scripts/prod-health-check.sh <base-url>"
  echo "Example: bash scripts/prod-health-check.sh https://example.com"
  exit 1
fi

BASE_URL="${1%/}"
PASS=0; FAIL=0; WARN=0; SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
warn() { echo "  WARN: $1"; WARN=$((WARN + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " Production Health Check"
echo " Target: $BASE_URL"
echo "============================================"
echo ""

# ---- 1. Frontend ----
echo "[1] Frontend"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "304" ]; then
  pass "GET / returned HTTP $HTTP_CODE"
else
  fail "GET / returned HTTP $HTTP_CODE (expected 200)"
fi
echo ""

# ---- 2. Login ----
echo "[2] Authentication"

LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' \
  "$BASE_URL/api/auth/login" 2>/dev/null || echo "")

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
  fail "Login failed — cannot proceed with authenticated tests"
  echo "  DEBUG: ${LOGIN_RESP:0:300}"
  echo ""
  echo "============================================"
  echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped"
  echo " Status:  FAILED"
  exit 1
fi
pass "Login successful"
AUTH=(-H "Authorization: Bearer $TOKEN")

# Trace ID from login response
TRACE_ID=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; h=json.load(sys.stdin).get('headers',{}); print(h.get('X-Trace-Id',''))" 2>/dev/null || echo "")
[ -n "$TRACE_ID" ] && echo "  INFO: X-Trace-Id: $TRACE_ID"

# Bad password test
BAD_RESP=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"wrongpassword"}' \
  "$BASE_URL/api/auth/login" 2>/dev/null || echo "000")
if [ "$BAD_RESP" = "401" ]; then
  pass "Login with bad password returned 401"
else
  warn "Login with bad password returned HTTP $BAD_RESP (expected 401)"
fi
echo ""

# ---- 3. Authenticated endpoints ----
echo "[3] Core APIs"

check_api() {
  local method="${1:-GET}"
  local url="$2"
  local expected="${3:-200}"
  local label="$4"
  local data="${5:-}"

  local curl_args=(-s -o /dev/null -w "%{http_code}" -k --max-time 10 "${AUTH[@]}")
  if [ "$method" = "POST" ]; then
    curl_args+=(-X POST -H "Content-Type: application/json")
    [ -n "$data" ] && curl_args+=(-d "$data")
  fi
  curl_args+=("$url")

  local code
  code=$(curl "${curl_args[@]}" 2>/dev/null || echo "000")

  if [ "$code" = "$expected" ]; then
    pass "$label (HTTP $code)"
  else
    fail "$label — expected HTTP $expected, got $code"
  fi
}

check_api "GET"  "$BASE_URL/api/auth/me"                   200 "GET /api/auth/me"
check_api "GET"  "$BASE_URL/api/projects?page=1&size=1"     200 "GET /api/projects"
check_api "GET"  "$BASE_URL/api/agents"                      200 "GET /api/agents"
echo ""

# ---- 4. Observability ----
echo "[4] Observability"

OBS_OVERVIEW=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/observability/overview" 2>/dev/null || echo "000")
if [ "$OBS_OVERVIEW" = "200" ]; then
  pass "GET /api/observability/overview (HTTP 200)"
elif [ "$OBS_OVERVIEW" = "403" ]; then
  warn "GET /api/observability/overview — HTTP 403 (requires ADMIN role, token user may not be admin)"
else
  fail "GET /api/observability/overview — HTTP $OBS_OVERVIEW"
fi

MODEL_USAGE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/observability/model-usage/summary" 2>/dev/null || echo "000")
if [ "$MODEL_USAGE" = "200" ]; then
  pass "GET /api/observability/model-usage/summary (HTTP 200)"
elif [ "$MODEL_USAGE" = "403" ]; then
  warn "GET /api/observability/model-usage/summary — HTTP 403 (requires ADMIN)"
else
  fail "GET /api/observability/model-usage/summary — HTTP $MODEL_USAGE"
fi
echo ""

# ---- 5. Model Gateway ----
echo "[5] Model Gateway"

PROVIDERS=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/model-gateway/providers" 2>/dev/null || echo "000")
if [ "$PROVIDERS" = "200" ]; then
  pass "GET /api/model-gateway/providers (HTTP 200)"
else
  fail "GET /api/model-gateway/providers — HTTP $PROVIDERS"
fi

# MOCK connection test
CONN_RESP=$(curl -s -k --max-time 30 -X POST \
  -H "Content-Type: application/json" \
  "${AUTH[@]}" \
  -d '{"provider":"MOCK","modelName":"mock-agent-model"}' \
  "$BASE_URL/api/model-gateway/test-connection" 2>/dev/null || echo "")

CONN_SUCCESS=$(echo "$CONN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('success',''))" 2>/dev/null || echo "")
if [ "$CONN_SUCCESS" = "True" ]; then
  pass "MOCK connection test — success"
else
  fail "MOCK connection test — unexpected response"
fi
echo ""

# ---- 6. GitHub OAuth ----
echo "[6] GitHub OAuth"

GH_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/github/oauth/status" 2>/dev/null || echo "")
GH_CONFIGURED=$(echo "$GH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('configured',''))" 2>/dev/null || echo "")
GH_BOUND=$(echo "$GH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('bound',''))" 2>/dev/null || echo "")

if [ "$GH_CONFIGURED" = "True" ]; then
  pass "GitHub OAuth configured"
  if [ "$GH_BOUND" = "True" ]; then
    pass "GitHub account bound"
  else
    warn "GitHub OAuth configured but no account bound"
  fi
elif [ "$GH_CONFIGURED" = "False" ]; then
  skip "GitHub OAuth not configured"
else
  warn "GitHub OAuth status unexpected: ${GH_RESP:0:200}"
fi
echo ""

# ---- 7. Chat (optional, creates data) ----
echo "[7] Chat (basic)"

PROJECTS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/projects?page=1&size=10" 2>/dev/null || echo "")
PROJECT_ID=$(echo "$PROJECTS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(records[0].get('id','') if records else '')" 2>/dev/null || echo "")

if [ -z "$PROJECT_ID" ]; then
  skip "No project found — chat test skipped"
else
  SESSION_RESP=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    "${AUTH[@]}" \
    -d '{"title":"Health Check Session","sessionType":"PROJECT"}' \
    "$BASE_URL/api/projects/${PROJECT_ID}/chat/sessions" 2>/dev/null || echo "")

  SESSION_CODE=$(echo "$SESSION_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null || echo "")

  if [ "$SESSION_CODE" = "OK" ]; then
    pass "Chat session created"
  else
    warn "Chat session creation may have failed: ${SESSION_RESP:0:200}"
  fi
fi
echo ""

# ---- 8. Unauthenticated check ----
echo "[8] Unauthenticated Access"
UNAUTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/api/projects" 2>/dev/null || echo "000")
if [ "$UNAUTH_CODE" = "401" ] || [ "$UNAUTH_CODE" = "403" ]; then
  pass "GET /api/projects (no token) returned HTTP $UNAUTH_CODE"
else
  fail "GET /api/projects (no token) returned HTTP $UNAUTH_CODE (expected 401)"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL + WARN + SKIP))
echo "============================================"
echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped ($TOTAL total)"
if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED"
  exit 1
else
  echo " Status:  PASSED"
  exit 0
fi
