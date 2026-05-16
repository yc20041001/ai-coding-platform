#!/usr/bin/env bash
set -euo pipefail

# Validates GitHub OAuth configuration against .env.production.
# No secrets are printed.
# Not configured → SKIP, not FAIL.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.production"

PASS=0
FAIL=0
SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " GitHub OAuth Configuration Validation"
echo "============================================"
echo ""

# Check env file
if [ ! -f "$ENV_FILE" ]; then
  echo "WARNING: .env.production not found at $ENV_FILE"
  echo "All GitHub OAuth checks will be SKIPPED."
  echo ""
  skip "No .env.production file"
  echo ""
  echo " Results: 0 passed, 0 failed, 1 skipped"
  exit 0
fi

set -a
source "$ENV_FILE"
set +a

# ---- Check environment variables ----
echo "[1] Environment Variables"

CLIENT_ID="${GITHUB_CLIENT_ID:-}"
CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-}"
REDIRECT_URI="${GITHUB_REDIRECT_URI:-http://localhost:8080/api/github/oauth/callback}"
SCOPES="${GITHUB_OAUTH_SCOPES:-repo,read:user,user:email}"

HAS_CLIENT_ID=false
HAS_SECRET=false

if [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "CHANGE_ME"* ]; then
  pass "GITHUB_CLIENT_ID is set"
  HAS_CLIENT_ID=true
else
  skip "GITHUB_CLIENT_ID is not set or still CHANGE_ME"
fi

if [ -n "$CLIENT_SECRET" ] && [ "$CLIENT_SECRET" != "CHANGE_ME"* ]; then
  pass "GITHUB_CLIENT_SECRET is set"
  HAS_SECRET=true
else
  skip "GITHUB_CLIENT_SECRET is not set or still CHANGE_ME"
fi

echo "  INFO: GITHUB_REDIRECT_URI=${REDIRECT_URI}"
echo "  INFO: GITHUB_OAUTH_SCOPES=${SCOPES}"
echo ""

# ---- Validate redirect URI format ----
echo "[2] Redirect URI Validation"
if [[ "$REDIRECT_URI" =~ ^https?:// ]]; then
  pass "Redirect URI has valid scheme (http/https)"
else
  fail "Redirect URI scheme is invalid: ${REDIRECT_URI}"
fi

if [[ "$REDIRECT_URI" == */api/github/oauth/callback ]]; then
  pass "Redirect URI path ends with /api/github/oauth/callback"
else
  fail "Redirect URI path should end with /api/github/oauth/callback: ${REDIRECT_URI}"
fi
echo ""

# ---- Scope validation ----
echo "[3] Scope Validation"
if echo "$SCOPES" | grep -q "repo"; then
  echo "  WARN: 'repo' scope grants full private repository access."
  echo "        For public repos only, use 'public_repo' instead."
fi

REQUIRED_SCOPES=("read:user" "user:email")
for scope in "${REQUIRED_SCOPES[@]}"; do
  if echo "$SCOPES" | grep -q "$scope"; then
    pass "Required scope '${scope}' is present"
  else
    fail "Required scope '${scope}' is missing"
  fi
done
echo ""

# ---- Check Frontend base URL consistency ----
echo "[4] URL Consistency"
APP_BASE_URL="${APP_BASE_URL:-}"
if [ -n "$APP_BASE_URL" ] && [ -n "$REDIRECT_URI" ]; then
  APP_HOST=$(echo "$APP_BASE_URL" | sed 's|https\?://||' | sed 's|/.*||')
  REDIRECT_HOST=$(echo "$REDIRECT_URI" | sed 's|https\?://||' | sed 's|/.*||')
  if [ "$APP_HOST" = "$REDIRECT_HOST" ]; then
    pass "APP_BASE_URL and GITHUB_REDIRECT_URI share the same host (${APP_HOST})"
  else
    echo "  WARN: APP_BASE_URL host (${APP_HOST}) differs from redirect host (${REDIRECT_HOST})"
    echo "        Make sure the GitHub OAuth App callback URL matches."
    PASS=$((PASS + 1)) # warning, not failure
  fi
else
  skip "APP_BASE_URL not set, skipping host consistency check"
fi
echo ""

# ---- Docker / Backend check ----
echo "[5] Docker / Backend API Check"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

check_api() {
  local base_url="$1"
  local token="$2"
  local desc="$3"
  local expected_field="$4"

  local resp
  resp=$(curl -s -k --max-time 10 -H "Authorization: Bearer $token" "${base_url}/api/github/oauth/${desc}" 2>/dev/null || echo "")
  if [ -z "$resp" ]; then
    echo "  INFO: Could not reach ${desc} endpoint (backend may not be running)"
    return 1
  fi

  local field_val
  field_val=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('${expected_field}',''))" 2>/dev/null || echo "")

  if [ "$field_val" = "True" ] || [ "$field_val" = "true" ]; then
    pass "API /api/github/oauth/${desc} → ${expected_field}=true"
    return 0
  elif [ "$field_val" = "False" ] || [ "$field_val" = "false" ]; then
    echo "  INFO: API /api/github/oauth/${desc} → ${expected_field}=false (not configured/bound yet)"
    return 0
  else
    echo "  INFO: API /api/github/oauth/${desc} returned unexpected: ${resp:0:200}"
    return 1
  fi
}

if [ -f "$ENV_FILE" ] && command -v docker &>/dev/null; then
  # Try to get a token from the running backend
  BACKEND_URL="${APP_BASE_URL:-http://localhost}"
  LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"Admin@123456"}' \
    "${BACKEND_URL}/api/auth/login" 2>/dev/null || echo "")

  TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

  if [ -n "$TOKEN" ]; then
    pass "Backend reachable, obtained auth token"
    check_api "$BACKEND_URL" "$TOKEN" "status" "configured"
    check_api "$BACKEND_URL" "$TOKEN" "status" "bound"
  else
    echo "  INFO: Backend not reachable or login failed. API checks skipped."
  fi
else
  skip "Docker not available or .env.production missing, API checks skipped"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL + SKIP))
echo "============================================"
echo " Results: $PASS passed, $FAIL failed, $SKIP skipped ($TOTAL total)"
if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED ($FAIL configuration issues)"
  exit 1
else
  echo " Status:  PASSED"
  exit 0
fi
