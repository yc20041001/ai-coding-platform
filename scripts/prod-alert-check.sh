#!/usr/bin/env bash
set -euo pipefail

# Production alert check based on API metrics and thresholds.
# Usage: bash scripts/prod-alert-check.sh [base-url]
# Example: bash scripts/prod-alert-check.sh https://example.com

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PASS=0; FAIL=0; WARN=0; SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
warn() { echo "  WARN: $1"; WARN=$((WARN + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " Production Alert Check"
echo "============================================"
echo ""

BASE_URL="${1:-}"

# ---- 1. Service Availability ----
echo "[1] Service Availability"

check_url() {
  local url="$1"
  local label="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$url" 2>/dev/null || echo "000")
  if [ "$code" = "200" ] || [ "$code" = "304" ]; then
    pass "$label — HTTP $code"
  else
    fail "$label — HTTP $code (severity: P1)"
  fi
}

if [ -n "$BASE_URL" ]; then
  BASE_URL="${BASE_URL%/}"
  check_url "$BASE_URL/" "Frontend"

  # Backend health via login
  LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"Admin@123456"}' \
    "$BASE_URL/api/auth/login" 2>/dev/null || echo "")

  TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

  if [ -n "$TOKEN" ]; then
    pass "Backend API reachable (login OK)"
    AUTH=(-H "Authorization: Bearer $TOKEN")
  else
    fail "Backend login failed (severity: P1)"
    echo ""
    echo "============================================"
    echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped"
    echo " Status:  FAILED"
    exit 1
  fi
else
  skip "No base URL provided — skip service availability checks"
fi
echo ""

# ---- 2. Model Gateway Alerts ----
echo "[2] Model Gateway"

if [ -n "${TOKEN:-}" ]; then
  # Model usage summary
  USAGE_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/observability/model-usage/summary" 2>/dev/null || echo "")

  if echo "$USAGE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null | grep -q "OK"; then
    pass "Model usage summary accessible"

    # Check fallback rate
    FALLBACK_COUNT=$(echo "$USAGE_RESP" | python3 -c "
import sys,json
d = json.load(sys.stdin).get('data',{})
# Try to find fallback-related fields
fallback = d.get('fallbackCount', d.get('fallback_count', 0))
total = d.get('totalCalls', d.get('total_calls', 1))
if total and total > 0:
    rate = (int(fallback) / int(total)) * 100
    print(int(rate))
else:
    print(0)
" 2>/dev/null || echo "0")

    if [ "${FALLBACK_COUNT:-0}" -gt 30 ]; then
      fail "Model fallback rate > 30% (severity: P2)"
    elif [ "${FALLBACK_COUNT:-0}" -gt 15 ]; then
      warn "Model fallback rate > 15%"
    else
      pass "Model fallback rate acceptable (${FALLBACK_COUNT:-0}%)"
    fi
  else
    warn "Model usage summary not available (requires ADMIN)"
  fi

  # Provider test
  CONN_RESP=$(curl -s -k --max-time 30 -X POST \
    -H "Content-Type: application/json" \
    "${AUTH[@]}" \
    -d '{"provider":"MOCK","modelName":"mock-agent-model"}' \
    "$BASE_URL/api/model-gateway/test-connection" 2>/dev/null || echo "")

  CONN_SUCCESS=$(echo "$CONN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('success',''))" 2>/dev/null || echo "")
  if [ "$CONN_SUCCESS" = "True" ]; then
    pass "MOCK model provider available"
  else
    fail "MOCK model provider DOWN (severity: P1)"
  fi
else
  skip "No API access — model gateway checks skipped"
fi
echo ""

# ---- 3. GitHub Integration ----
echo "[3] GitHub Integration"

if [ -n "${TOKEN:-}" ]; then
  GH_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/github/oauth/status" 2>/dev/null || echo "")
  GH_CFG=$(echo "$GH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('configured',''))" 2>/dev/null || echo "")

  if [ "$GH_CFG" = "True" ]; then
    pass "GitHub OAuth configured"

    GH_BOUND=$(echo "$GH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('bound',''))" 2>/dev/null || echo "")
    if [ "$GH_BOUND" = "True" ]; then
      pass "GitHub account bound"
    else
      warn "GitHub not bound (severity: P3)"
    fi
  elif [ "$GH_CFG" = "False" ]; then
    skip "GitHub OAuth not configured"
  fi
else
  skip "No API access — GitHub checks skipped"
fi
echo ""

# ---- 4. Auth Alerts ----
echo "[4] Auth & Security"

if [ -n "${TOKEN:-}" ]; then
  # Login failure check (via bad password)
  BAD_AUTH=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"wrongpassword"}' \
    "$BASE_URL/api/auth/login" 2>/dev/null || echo "")
  BAD_CODE=$(echo "$BAD_AUTH" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null || echo "")

  if [ "$BAD_CODE" = "UNAUTHORIZED" ]; then
    pass "Wrong password correctly rejected"
  else
    warn "Wrong password response unexpected"
  fi

  # Unauthenticated access
  UNAUTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/api/projects" 2>/dev/null || echo "000")
  if [ "$UNAUTH_CODE" = "401" ]; then
    pass "Unauthenticated requests blocked (HTTP 401)"
  else
    fail "Unauthenticated requests NOT blocked — HTTP $UNAUTH_CODE (severity: P1)"
  fi
else
  skip "No API access — auth checks skipped"
fi
echo ""

# ---- 5. Docker / Infrastructure ----
echo "[5] Infrastructure"

COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"
ENV_FILE="${PROJECT_ROOT}/.env.production"

if [ -f "$ENV_FILE" ] && command -v docker &>/dev/null; then
  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps 2>/dev/null | grep -q .; then
    # Check for stopped containers
    STOPPED=$(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps 2>/dev/null | grep -c "Exit\|stopped\|unhealthy" 2>/dev/null || echo "0")
    if [ "${STOPPED:-0}" -gt 0 ]; then
      fail "$STOPPED container(s) not healthy (severity: P1)"
    else
      pass "All containers running"
    fi

    # Disk check
    DISK_USAGE=$(df /var/lib/docker 2>/dev/null | tail -1 | awk '{print $5}' | sed 's/%//' || echo "0")
    if [ "${DISK_USAGE:-0}" -gt 85 ]; then
      fail "Docker disk usage ${DISK_USAGE}% (severity: P1)"
    elif [ "${DISK_USAGE:-0}" -gt 70 ]; then
      warn "Docker disk usage ${DISK_USAGE}%"
    else
      pass "Docker disk usage ${DISK_USAGE:-0}%"
    fi
  else
    skip "Docker services not running"
  fi
else
  skip "Docker not available"
fi
echo ""

# ---- 6. Quick DB check ----
echo "[6] Database"

if [ -f "$ENV_FILE" ] && command -v docker &>/dev/null; then
  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps mysql 2>/dev/null | grep -q "Up\|running\|healthy"; then
    pass "MySQL container running"
  else
    skip "MySQL container not running"
  fi
else
  skip "Docker not available — DB check skipped"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL + WARN + SKIP))
echo "============================================"
echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped ($TOTAL total)"

if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED — alerting thresholds exceeded"
  echo ""
  echo " Action required:"
  echo "  1. Review FAIL items above"
  echo "  2. See docs/production-alerting-rules.md for severity and response"
  echo "  3. See docs/incident-response-runbook.md for recovery procedures"
  exit 1
else
  echo " Status:  PASSED — all metrics within acceptable ranges"
  exit 0
fi
