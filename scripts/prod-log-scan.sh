#!/usr/bin/env bash
set -euo pipefail

# Scan Docker logs and local files for secret leaks and error patterns.
# Usage: bash scripts/prod-log-scan.sh [service-name]
# Examples:
#   bash scripts/prod-log-scan.sh           # Scan all services
#   bash scripts/prod-log-scan.sh backend   # Scan backend only

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

PASS=0; FAIL=0; WARN=0; SKIP=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }
warn() { echo "  WARN: $1"; WARN=$((WARN + 1)); }
skip() { echo "  SKIP: $1"; SKIP=$((SKIP + 1)); }

echo "============================================"
echo " Production Log Scan"
echo "============================================"
echo ""

SERVICE="${1:-}"

# Collect log output
LOG_OUTPUT=""

if [ -f "$ENV_FILE" ] && command -v docker &>/dev/null; then
  echo "[1] Docker Logs"

  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps 2>/dev/null | grep -q "Up\|running"; then
    if [ -n "$SERVICE" ]; then
      LOG_OUTPUT=$(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=500 "$SERVICE" 2>/dev/null || echo "")
    else
      LOG_OUTPUT=$(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=500 2>/dev/null || echo "")
    fi
    pass "Docker logs collected ($(echo "$LOG_OUTPUT" | wc -l) lines)"
  else
    skip "Docker services not running — scanning local files instead"
  fi
else
  skip "Docker not available — scanning local files instead"
fi
echo ""

# ---- Secret Pattern Scan ----
echo "[2] Secret Pattern Scan"

scan_secret() {
  local pattern="$1"
  local label="$2"
  local source="${3:-$LOG_OUTPUT}"

  if [ -z "$source" ]; then
    skip "$label scan — no log data available"
    return
  fi

  # Count matches but exclude known documentation examples
  local matches
  matches=$(echo "$source" | grep -c -E "$pattern" 2>/dev/null || echo "0")

  if [ "$matches" -gt 0 ]; then
    # Check if matches are real keys or just documentation
    local real_count=0
    while IFS= read -r line; do
      # Skip if line is clearly documentation/example
      if echo "$line" | grep -qiE "example|placeholder|your-key|sk-xxxx|CHANGE_ME|mask\("; then
        continue
      fi
      if echo "$line" | grep -qiE "^#|//|/\*|\*|^---|^==="; then
        continue
      fi
      real_count=$((real_count + 1))
    done < <(echo "$source" | grep -E "$pattern" 2>/dev/null || echo "")

    if [ "$real_count" -gt 0 ]; then
      fail "$label: $real_count potential leak(s) in logs!"
    else
      pass "No $label leaks (${matches} doc/examples masked)"
    fi
  else
    pass "No $label pattern found"
  fi
}

scan_secret 'sk-[a-zA-Z0-9]{20,}'                    "OpenAI/Claude API key (sk-)"
scan_secret 'ghp_[a-zA-Z0-9]{20,}'                    "GitHub personal token (ghp_)"
scan_secret 'github_pat_[a-zA-Z0-9]{20,}'             "GitHub fine-grained token"
scan_secret 'Bearer [a-zA-Z0-9_\-]{32,}'               "Bearer token (long)"
scan_secret '(?i)OPENAI_API_KEY\s*=\s*[A-Za-z0-9]'    "OPENAI_API_KEY set to real value"
scan_secret '(?i)CLAUDE_API_KEY\s*=\s*[A-Za-z0-9]'    "CLAUDE_API_KEY set to real value"
scan_secret '(?i)GITHUB_CLIENT_SECRET\s*=\s*[A-Za-z0-9]' "GITHUB_CLIENT_SECRET set to real value"
echo ""

# ---- Error Pattern Scan ----
echo "[3] Error Pattern Scan"

if [ -n "$LOG_OUTPUT" ]; then

  count_pattern() {
    local pattern="$1"
    local label="$2"
    local count
    count=$(echo "$LOG_OUTPUT" | grep -c -E "$pattern" 2>/dev/null || echo "0")
    if [ "$count" -gt 20 ]; then
      warn "$label: $count occurrences (high frequency)"
    elif [ "$count" -gt 5 ]; then
      echo "  INFO: $label: $count occurrences"
    else
      pass "$label: $count occurrences (low)"
    fi
  }

  count_pattern "ERROR" "ERROR log level"
  count_pattern "Exception" "Java Exception"
  count_pattern "SQLSyntaxErrorException\|CommunicationsException" "Database errors"
  count_pattern "Stacktrace" "Stack traces"
else
  skip "No log data for error scan"
fi
echo ""

# ---- Model/GitHub specific ----
echo "[4] External Service Errors"

if [ -n "$LOG_OUTPUT" ]; then
  count_pattern "AUTH_ERROR" "Model auth errors"
  count_pattern "RATE_LIMIT\|RATE_LIMITED" "Rate limit errors"
  count_pattern "TIMEOUT" "Timeout errors"
  count_pattern "fallback\b" "Fallback to Mock"
  count_pattern "GitHub API" "GitHub API errors"
  count_pattern "PR Review.*[Ff]ail" "PR Review failures"
else
  skip "No log data for external service scan"
fi
echo ""

# ---- File System Scan (always available) ----
echo "[5] File System Scan"

cd "$PROJECT_ROOT"

# Scan .env files for real keys
if [ -f ".env.production" ]; then
  if grep -q "sk-[a-zA-Z0-9]\{20,\}" .env.production 2>/dev/null; then
    fail ".env.production contains real API key (sk-...)"
  else
    pass ".env.production: no real API key patterns"
  fi
else
  skip ".env.production not present — skipping file scan"
fi

# Scan backup files
if [ -d "backups" ] && [ "$(ls -A backups/*.sql 2>/dev/null || echo "")" != "" ]; then
  warn "SQL backup files present in backups/ — ensure they're not committed"
else
  pass "No SQL backup files found (or directory empty)"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + FAIL + WARN + SKIP))
echo "============================================"
echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped ($TOTAL total)"
if [ "$FAIL" -gt 0 ]; then
  echo " Status:  FAILED — potential secret leaks or critical issues found"
  exit 1
else
  echo " Status:  PASSED"
  exit 0
fi
