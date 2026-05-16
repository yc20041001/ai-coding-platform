#!/usr/bin/env bash
set -euo pipefail

# Validates model provider configuration against .env.production.
# No real API keys are printed.
# Unconfigured providers → SKIP, not FAIL.
# Misconfigured providers → FAIL.

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
echo " Model Provider Configuration Validation"
echo "============================================"
echo ""

# Check env file
if [ ! -f "$ENV_FILE" ]; then
  echo "WARNING: .env.production not found at $ENV_FILE"
  echo "All real providers will be SKIPPED."
  echo ""
fi

# Source env file if it exists
if [ -f "$ENV_FILE" ]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

# ---- Helper: check a provider ----
check_provider() {
  local label="$1"
  local enabled_var="$2"
  local key_var="$3"
  local base_var="$4"
  local model_var="$5"
  local requires_key="${6:-true}"

  local enabled="${!enabled_var:-false}"
  local key_val="${!key_var:-}"
  local base_val="${!base_var:-}"
  local model_val="${!model_var:-}"

  if [ "$enabled" != "true" ] && [ "$enabled" != "TRUE" ]; then
    skip "$label is not enabled (${enabled_var}=${enabled:-false})"
    return
  fi

  if [ "$requires_key" = "true" ] && [ -z "$key_val" ]; then
    fail "$label is enabled but ${key_var} is empty"
    return
  fi

  if [ "$requires_key" = "true" ] && [ -z "$base_val" ]; then
    fail "$label is enabled but ${base_var} is empty"
    return
  fi

  local key_display="<empty>"
  if [ -n "$key_val" ]; then
    if [ ${#key_val} -le 8 ]; then
      key_display="****"
    else
      key_display="${key_val:0:3}****${key_val: -4}"
    fi
  fi

  echo "  PASS: $label"
  echo "        enabled=$enabled model=${model_val:-<default>} base=${base_val:-<default>} key=${key_display}"
  PASS=$((PASS + 1))
}

# ---- Mock (always available) ----
echo "[1] Mock Provider"
echo "  PASS: MOCK is always available (built-in, no configuration needed)"
PASS=$((PASS + 1))
echo ""

# ---- Real providers ----
echo "[2] Real Providers"

check_provider "OpenAI"     "OPENAI_ENABLED"     "OPENAI_API_KEY"     "OPENAI_BASE_URL"     "OPENAI_MODEL"     true
check_provider "Claude"     "CLAUDE_ENABLED"     "CLAUDE_API_KEY"     "CLAUDE_BASE_URL"     "CLAUDE_MODEL"     true
check_provider "DeepSeek"   "DEEPSEEK_ENABLED"   "DEEPSEEK_API_KEY"   "DEEPSEEK_BASE_URL"   "DEEPSEEK_MODEL"   true
check_provider "Qwen"       "QWEN_ENABLED"       "QWEN_API_KEY"       "QWEN_BASE_URL"       "QWEN_MODEL"       true
check_provider "Gemini"     "GEMINI_ENABLED"     "GEMINI_API_KEY"     "GEMINI_BASE_URL"     "GEMINI_MODEL"     true
echo ""

# ---- Gateway settings ----
echo "[3] Gateway Settings"
DEFAULT_PROVIDER="${MODEL_GATEWAY_PROVIDER:-MOCK}"
FALLBACK="${MODEL_GATEWAY_FALLBACK_ENABLED:-true}"
TIMEOUT="${MODEL_GATEWAY_TIMEOUT_MS:-30000}"
RETRY="${MODEL_GATEWAY_RETRY_TIMES:-1}"

echo "  INFO: MODEL_GATEWAY_PROVIDER=${DEFAULT_PROVIDER}"
echo "  INFO: MODEL_GATEWAY_FALLBACK_ENABLED=${FALLBACK}"
echo "  INFO: MODEL_GATEWAY_TIMEOUT_MS=${TIMEOUT}"
echo "  INFO: MODEL_GATEWAY_RETRY_TIMES=${RETRY}"
echo ""

# ---- Check Docker connectivity (optional) ----
echo "[4] Docker / Backend Check"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"
if [ -f "$ENV_FILE" ] && command -v docker &>/dev/null && docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps &>/dev/null; then
  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps | grep -q "backend.*Up\|backend.*running\|backend.*healthy"; then
    echo "  INFO: Backend container appears to be running."
    echo "  INFO: For live connection tests, use: bash scripts/prod-external-services-smoke-test.sh <base-url>"
  else
    echo "  INFO: Backend container not running. Live tests skipped."
  fi
else
  echo "  INFO: Docker not available or .env.production missing. Live tests skipped."
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
