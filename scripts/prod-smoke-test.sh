#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: bash scripts/prod-smoke-test.sh <base-url>"
  echo "Example: bash scripts/prod-smoke-test.sh https://example.com"
  echo "         bash scripts/prod-smoke-test.sh http://localhost"
  exit 1
fi

BASE_URL="${1%/}"
PASS=0
FAIL=0

check() {
  local label="$1"
  local method="${2:-GET}"
  local url="${3:-}"
  local expected_status="${4:-200}"
  local data="${5:-}"

  local curl_args=(-s -o /dev/null -w "%{http_code}" -k --max-time 10)
  if [ "$method" = "POST" ]; then
    curl_args+=(-X POST -H "Content-Type: application/json")
    [ -n "$data" ] && curl_args+=(-d "$data")
  fi
  curl_args+=("$url")

  local status
  status=$(curl "${curl_args[@]}" 2>/dev/null || echo "000")

  if [ "$status" = "$expected_status" ]; then
    echo "  PASS: $label (HTTP $status)"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $label (expected HTTP $expected_status, got HTTP $status)"
    FAIL=$((FAIL + 1))
  fi
}

echo "============================================"
echo " Production Smoke Test"
echo " Target: $BASE_URL"
echo "============================================"
echo ""

# ---- Public endpoints ----
echo "[1] Public endpoints"
check "GET /"              GET "$BASE_URL/"
echo ""

# ---- Auth ----
echo "[2] Authentication"
check "POST /api/auth/login (valid)"  POST "$BASE_URL/api/auth/login" 200 \
  '{"email":"admin@example.com","password":"Admin@123456"}'
check "POST /api/auth/login (bad pw)" POST "$BASE_URL/api/auth/login" 401 \
  '{"email":"admin@example.com","password":"wrong"}'
echo ""

# ---- Authenticated: login and use token ----
echo "[3] Authenticated endpoints"

LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' \
  "$BASE_URL/api/auth/login")

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
  echo "  FAIL: Could not extract access token from login response"
  echo "  Raw response: ${LOGIN_RESP:0:200}"
  FAIL=$((FAIL + 1))
else
  PASS=$((PASS + 1))
  echo "  PASS: Extracted access token"

  AUTH=(-H "Authorization: Bearer $TOKEN")

  check "GET /api/auth/me"      GET "$BASE_URL/api/auth/me"      200 "" "${AUTH[*]}"

  # Projects
  PROJECTS_RESP=$(curl -s -k --max-time 10 "${AUTH[@]}" "$BASE_URL/api/projects?page=1&size=10")
  PROJECT_ID=$(echo "$PROJECTS_RESP" | python3 -c "import sys,json; records=json.load(sys.stdin).get('data',{}).get('records',[]); print(records[0].get('id','') if records else '')" 2>/dev/null || echo "")

  if [ -n "$PROJECT_ID" ]; then
    echo "  PASS: GET /api/projects (found project $PROJECT_ID)"

    check "GET /api/projects/{id}/members" GET "$BASE_URL/api/projects/$PROJECT_ID/members" 200 "" "${AUTH[*]}"

    check "GET /api/agents" GET "$BASE_URL/api/agents" 200 "" "${AUTH[*]}"
  else
    echo "  FAIL: GET /api/projects (no projects found)"
    FAIL=$((FAIL + 1))
  fi
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL))
echo "============================================"
echo " Results: $PASS/$TOTAL passed"
if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED ($FAIL failures)"
  exit 1
else
  echo " Status:  PASSED"
  exit 0
fi
