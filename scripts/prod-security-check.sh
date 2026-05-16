#!/usr/bin/env bash
set -euo pipefail

# Production security check script.
# Usage: bash scripts/prod-security-check.sh <base-url>
# Example: bash scripts/prod-security-check.sh https://example.com

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PASS=0; FAIL=0; WARN=0; SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
warn() { echo "  WARN: $1"; WARN=$((WARN + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " Production Security Check"
echo "============================================"
echo ""

# ---- 1. .env.production not tracked ----
echo "[1] Secret Files"

cd "$PROJECT_ROOT"

if git ls-files --error-unmatch .env.production >/dev/null 2>&1; then
  fail ".env.production is TRACKED by git — REMOVE IMMEDIATELY"
else
  pass ".env.production is not tracked by git"
fi

if git diff --cached --name-only | grep -q ".env.production" 2>/dev/null; then
  fail ".env.production is STAGED for commit"
else
  pass ".env.production is not staged"
fi

if git ls-files --error-unmatch backups/ >/dev/null 2>&1; then
  fail "backups/ directory is TRACKED by git"
else
  pass "backups/ is not tracked by git"
fi
echo ""

# ---- 2. Secret patterns in repo ----
echo "[2] Repository Secret Scan"

scan_pattern() {
  local pattern="$1"
  local label="$2"
  local matches
  matches=$(rg --no-filename -l "$pattern" . 2>/dev/null | grep -v node_modules | grep -v target/ | grep -v dist/ | grep -v '.git/' | grep -v test-results/ || echo "")

  # Filter out known-safe documentation files
  local real_matches=""
  while IFS= read -r file; do
    [ -z "$file" ] && continue
    # Only flag if the match contains a real-looking key (not placeholder)
    if rg --no-filename "$pattern" "$file" 2>/dev/null | grep -qv "CHANGE_ME\|your-key\|sk-xxxx\|sk-****\|example\|placeholder" ; then
      real_matches="${real_matches}${file}\n"
    fi
  done <<< "$matches"

  if [ -n "$real_matches" ]; then
    fail "$label pattern found in repo — check: $(echo -e "$real_matches" | tr '\n' ' ')"
  else
    pass "No real $label pattern found in repo"
  fi
}

scan_pattern "sk-[a-zA-Z0-9]{20,}" "API key (sk-)"
scan_pattern "ghp_[a-zA-Z0-9]{20,}" "GitHub token (ghp_)"
scan_pattern "github_pat_[a-zA-Z0-9]{20,}" "GitHub fine-grained token"
echo ""

# ---- 3. HTTP Security Headers (if base URL provided) ----
BASE_URL="${1:-}"

if [ -n "$BASE_URL" ]; then
  BASE_URL="${BASE_URL%/}"
  echo "[3] HTTP Security Headers"

  HEADERS=$(curl -s -I -k --max-time 10 "$BASE_URL/" 2>/dev/null || echo "")

  check_header() {
    local header="$1"
    local label="$2"
    if echo "$HEADERS" | grep -qi "$header"; then
      pass "Header '$label' is present"
    else
      warn "Header '$label' is missing"
    fi
  }

  check_header "X-Content-Type-Options" "X-Content-Type-Options"
  check_header "X-Frame-Options" "X-Frame-Options"
  check_header "Referrer-Policy" "Referrer-Policy"
  echo ""

  # ---- 4. CORS check ----
  echo "[4] CORS Configuration"

  CORS_HEADER=$(curl -s -I -k --max-time 10 -H "Origin: https://evil.example.com" "$BASE_URL/" 2>/dev/null || echo "")
  ACAO=$(echo "$CORS_HEADER" | grep -i "Access-Control-Allow-Origin" | tr -d '\r' || echo "")

  if echo "$ACAO" | grep -q "\*"; then
    fail "CORS allows wildcard origin (*) — restrict to production domain"
  elif [ -n "$ACAO" ]; then
    pass "CORS returns specific origin (not wildcard): ${ACAO:0:80}"
  else
    pass "CORS does not echo arbitrary origin (secure default)"
  fi
  echo ""

  # ---- 5. Actuator exposure ----
  echo "[5] Actuator Exposure"

  ACTUATOR_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
  if [ "$ACTUATOR_CODE" = "404" ] || [ "$ACTUATOR_CODE" = "403" ] || [ "$ACTUATOR_CODE" = "000" ]; then
    pass "Actuator /actuator/health not publicly accessible (HTTP $ACTUATOR_CODE)"
  elif [ "$ACTUATOR_CODE" = "200" ]; then
    ACTUATOR_BODY=$(curl -s -k --max-time 10 "$BASE_URL/actuator/health" 2>/dev/null || echo "")
    if echo "$ACTUATOR_BODY" | grep -q '"details"'; then
      fail "Actuator exposes detailed health info publicly!"
    else
      warn "Actuator /actuator/health publicly accessible (HTTP 200) — but details may be hidden"
    fi
  else
    warn "Actuator returned HTTP $ACTUATOR_CODE"
  fi

  ACTUATOR_API=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/api/actuator" 2>/dev/null || echo "000")
  if [ "$ACTUATOR_API" = "404" ] || [ "$ACTUATOR_API" = "000" ]; then
    pass "/api/actuator not routed"
  else
    warn "/api/actuator returned HTTP $ACTUATOR_API"
  fi
  echo ""

  # ---- 6. Unauthenticated API check ----
  echo "[6] Unauthenticated Access"

  check_unauth() {
    local url="$1"
    local label="$2"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$url" 2>/dev/null || echo "000")
    if [ "$code" = "401" ] || [ "$code" = "403" ]; then
      pass "$label — HTTP $code"
    else
      fail "$label — HTTP $code (expected 401/403)"
    fi
  }

  check_unauth "$BASE_URL/api/projects"           "GET /api/projects"
  check_unauth "$BASE_URL/api/auth/me"             "GET /api/auth/me"
  check_unauth "$BASE_URL/api/agents"              "GET /api/agents"
  check_unauth "$BASE_URL/api/observability/overview" "GET /api/observability/overview"
  echo ""

  # ---- 7. Token leak in responses ----
  echo "[7] Response Token Leak Check"

  TOKEN=$(curl -s -k --max-time 10 -X POST \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"Admin@123456"}' \
    "$BASE_URL/api/auth/login" 2>/dev/null | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

  if [ -n "$TOKEN" ]; then

    GH_STATUS=$(curl -s -k --max-time 10 -H "Authorization: Bearer $TOKEN" \
      "$BASE_URL/api/github/oauth/status" 2>/dev/null || echo "")

    if echo "$GH_STATUS" | grep -q '"accessToken"'; then
      fail "GitHub OAuth status response contains 'accessToken' field"
    else
      pass "GitHub OAuth status does not leak access token"
    fi

    # Check model gateway test connection response
    CONN_RESP=$(curl -s -k --max-time 30 -X POST \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"provider":"MOCK","modelName":"mock-agent-model"}' \
      "$BASE_URL/api/model-gateway/test-connection" 2>/dev/null || echo "")

    if echo "$CONN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('apiKey',''))" 2>/dev/null | grep -qv "^\*\|^<empty>$"; then
      fail "Model connection test response may contain unmasked API key"
    else
      pass "Model connection test response masks API key"
    fi
  else
    skip "Cannot obtain auth token for response leak checks"
  fi
else
  skip "No base URL provided — skipping HTTP checks (sections 3-7)"
fi
echo ""

# ---- 8. .gitignore coverage ----
echo "[8] .gitignore Coverage"

check_gitignore() {
  local pattern="$1"
  local label="$2"
  if grep -q "^${pattern}" "$PROJECT_ROOT/.gitignore" 2>/dev/null; then
    pass ".gitignore covers: $label"
  else
    warn ".gitignore may be missing: $label"
  fi
}

check_gitignore "backups/" "backups/"
check_gitignore ".env.production" ".env.production"
check_gitignore "*.sql" "*.sql"
check_gitignore "*.dump" "*.dump"
check_gitignore "logs/" "logs/"
check_gitignore "diagnostics/" "diagnostics/"
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
