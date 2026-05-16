#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# collect-trial-report.sh — Trial Report Collection Script
# ============================================================
# Generates a trial report directory with feedback template,
# environment summary, and smoke test output.
# Does NOT collect secrets, tokens, API keys, or user data.
# ============================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="${PROJECT_ROOT}/trial-reports/trial_${TIMESTAMP}"

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "=========================================="
echo "  Trial Report Collection"
echo "=========================================="
echo ""

mkdir -p "$REPORT_DIR"

# ---- 1. Environment Summary ----
echo "[1/5] Environment Summary"
{
  echo "Trial Report — $TIMESTAMP"
  echo ""
  echo "=== Environment ==="
  echo "Date: $(date)"
  echo "Host: $(hostname 2>/dev/null || echo 'unknown')"
  echo "OS: $(uname -a 2>/dev/null || echo 'unknown')"
  echo ""
  echo "=== Git ==="
  echo "Branch: $(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
  echo "Commit: $(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
  echo "Status:"
  git -C "$PROJECT_ROOT" status --short 2>/dev/null || echo "(git unavailable)"
  echo ""
  echo "=== Backend ==="
  BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BASE_URL/actuator/health" 2>/dev/null || echo "unreachable")
  echo "Backend health: HTTP $BACKEND_STATUS"
  echo ""
  echo "=== Provider ==="
  echo "MODEL_GATEWAY_PROVIDER: ${MODEL_GATEWAY_PROVIDER:-MOCK}"
  echo ""
  echo "=== Files ==="
  echo ".env.production present: $(test -f "$PROJECT_ROOT/.env.production" && echo 'yes' || echo 'no')"
  echo ""
  echo "=== Note ==="
  echo "No secrets, tokens, API keys, or user data are included in this report."
} > "$REPORT_DIR/environment-summary.txt"
echo "  Collected: environment-summary.txt"

# ---- 2. Copy Feedback Template ----
echo "[2/5] Feedback Template"
FEEDBACK_TEMPLATE="${PROJECT_ROOT}/docs/user-feedback-template.md"
if [ -f "$FEEDBACK_TEMPLATE" ]; then
  cp "$FEEDBACK_TEMPLATE" "$REPORT_DIR/feedback-template.md"
  echo "  Collected: feedback-template.md (copy)"
else
  echo "  [WARN] Feedback template not found at docs/user-feedback-template.md"
fi

# ---- 3. Copy Acceptance Checklist ----
echo "[3/5] Acceptance Checklist"
CHECKLIST="${PROJECT_ROOT}/docs/demo-acceptance-checklist.md"
if [ -f "$CHECKLIST" ]; then
  cp "$CHECKLIST" "$REPORT_DIR/acceptance-checklist.md"
  echo "  Collected: acceptance-checklist.md (copy)"
else
  echo "  [WARN] Acceptance checklist not found"
fi

# ---- 4. Smoke Test Output ----
echo "[4/5] Demo Smoke Test"
SMOKE_SCRIPT="${PROJECT_ROOT}/scripts/demo-smoke-test.sh"
if [ -f "$SMOKE_SCRIPT" ] && command -v curl &>/dev/null; then
  bash "$SMOKE_SCRIPT" > "$REPORT_DIR/smoke-test-output.txt" 2>&1 || true
  echo "  Collected: smoke-test-output.txt"
else
  echo "  [SKIP] Smoke test script not available or curl missing"
fi

# ---- 5. Recent Commits ----
echo "[5/5] Recent Commits"
{
  echo "Recent commits (last 10):"
  git -C "$PROJECT_ROOT" log --oneline -10 2>/dev/null || echo "(git unavailable)"
} > "$REPORT_DIR/recent-commits.txt"
echo "  Collected: recent-commits.txt"

# ---- Done ----
echo ""
echo "=========================================="
echo "  Trial Report Generated"
echo "=========================================="
echo "  Directory: $REPORT_DIR"
echo ""
ls -la "$REPORT_DIR/"
echo ""
echo "  To view:  open $REPORT_DIR/"
echo "  To share: zip -r trial_${TIMESTAMP}.zip $REPORT_DIR"
echo ""
echo "  Reminder: trial-reports/ is in .gitignore."
echo "  Do NOT commit trial reports containing user data."
echo ""
echo "  Next steps:"
echo "    1. Fill in feedback-template.md"
echo "    2. Complete acceptance-checklist.md"
echo "    3. Review smoke-test-output.txt"
echo "    4. Submit feedback: https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml"
echo "=========================================="
