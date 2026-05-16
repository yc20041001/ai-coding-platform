#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# release-checklist.sh — Release Readiness Check
# ============================================================
# Checks that all required files exist, no secrets tracked,
# and basic quality gates pass. Outputs a release readiness
# summary without printing any secrets or tokens.
# ============================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

PASS=0; WARN=0; FAIL=0; SKIP=0

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
warn() { echo "  [WARN] $1"; WARN=$((WARN + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }
skip() { echo "  [SKIP] $1"; SKIP=$((SKIP + 1)); }

echo "=========================================="
echo "  Release Readiness Check"
echo "=========================================="
echo ""

# ---- 1. Required Files ----
echo "[1] Required Files"
check_file() {
  local file="$1"
  local label="$2"
  if [ -f "$PROJECT_ROOT/$file" ]; then
    pass "$label exists"
  else
    fail "$label MISSING"
  fi
}

check_file "README.md" "README.md"
check_file "CHANGELOG.md" "CHANGELOG.md"
check_file "docs/roadmap.md" "Roadmap"
check_file "docs/release-notes-template.md" "Release Notes Template"
check_file "docs/demo-walkthrough.md" "Demo Walkthrough"
check_file "docs/user-feedback-template.md" "Feedback Template"
check_file "docs/demo-acceptance-checklist.md" "Acceptance Checklist"
check_file "docs/product-feedback-taxonomy.md" "Feedback Taxonomy"
check_file "docs/user-trial-triage-guide.md" "Triage Guide"
check_file "docs/alpha-beta-trial-plan.md" "Alpha/Beta Trial Plan"
check_file "docs/production-deployment-runbook.md" "Production Runbook"
check_file "docs/incident-response-runbook.md" "Incident Response Runbook"
check_file ".github/ISSUE_TEMPLATE/bug_report.yml" "Bug Report Template"
check_file ".github/ISSUE_TEMPLATE/feature_request.yml" "Feature Request Template"
check_file ".github/ISSUE_TEMPLATE/user_trial_feedback.yml" "Trial Feedback Template"
check_file ".github/ISSUE_TEMPLATE/release_checklist.yml" "Release Checklist Template"
check_file ".github/pull_request_template.md" "PR Template"
check_file ".gitignore" ".gitignore"
echo ""

# ---- 2. Secrets Check ----
echo "[2] Secrets in Git"

if command -v git &>/dev/null; then
  cd "$PROJECT_ROOT"

  if git ls-files --error-unmatch .env.production >/dev/null 2>&1; then
    fail ".env.production is TRACKED by git"
  else
    pass ".env.production not tracked"
  fi

  if git ls-files --error-unmatch .env >/dev/null 2>&1; then
    fail ".env is TRACKED by git"
  else
    pass ".env not tracked"
  fi

  if git ls-files '*.pem' '*.key' '*.p12' '*.jks' 2>/dev/null | grep -q .; then
    fail "Certificate/key files tracked by git"
  else
    pass "No certificate/key files tracked"
  fi
else
  skip "Git not available"
fi
echo ""

# ---- 3. .gitignore Coverage ----
echo "[3] .gitignore Coverage"
check_gitignore() {
  local pattern="$1"
  local label="$2"
  if grep -q "^${pattern}" "$PROJECT_ROOT/.gitignore" 2>/dev/null; then
    pass ".gitignore covers: $label"
  else
    warn ".gitignore may be missing: $label"
  fi
}

check_gitignore ".env.production" ".env.production"
check_gitignore "backups/" "backups/"
check_gitignore "logs/" "logs/"
check_gitignore "diagnostics/" "diagnostics/"
check_gitignore "trial-reports/" "trial-reports/"
echo ""

# ---- 4. Script Syntax Check ----
echo "[4] Script Syntax"
check_syntax() {
  local script="$1"
  if [ -f "$PROJECT_ROOT/$script" ]; then
    if bash -n "$PROJECT_ROOT/$script" 2>/dev/null; then
      pass "$script syntax OK"
    else
      fail "$script syntax ERROR"
    fi
  else
    skip "$script not found"
  fi
}

check_syntax "scripts/demo-seed-data.sh"
check_syntax "scripts/demo-smoke-test.sh"
check_syntax "scripts/demo-reset-data.sh"
check_syntax "scripts/prod-health-check.sh"
check_syntax "scripts/prod-security-check.sh"
check_syntax "scripts/prod-log-scan.sh"
check_syntax "scripts/prod-alert-check.sh"
check_syntax "scripts/prod-diagnostics.sh"
check_syntax "scripts/collect-trial-report.sh"
check_syntax "scripts/release-checklist.sh"
echo ""

# ---- 5. Backend Quick Check ----
echo "[5] Backend Quick Check"
if [ -f "$PROJECT_ROOT/backend/pom.xml" ]; then
  pass "backend/pom.xml exists"
else
  warn "backend/pom.xml not found"
fi

if [ -f "$PROJECT_ROOT/frontend/package.json" ]; then
  pass "frontend/package.json exists"
else
  warn "frontend/package.json not found"
fi
echo ""

# ---- 5b. Backend Test Gate (Blocking) ----
echo "[5b] Backend Test Gate"
if command -v mvn &>/dev/null && [ -f "$PROJECT_ROOT/backend/pom.xml" ]; then
  cd "$PROJECT_ROOT/backend"
  if mvn test > /dev/null 2>&1; then
    pass "Backend tests pass"
  else
    fail "Backend tests FAILED — blocking release"
  fi
  cd "$PROJECT_ROOT"
else
  skip "Maven not available or backend/pom.xml not found"
fi
echo ""

# ---- 6. Docker Config ----
echo "[6] Docker Config"
if [ -f "$PROJECT_ROOT/deploy/prod/docker-compose.prod.yml" ]; then
  pass "Production docker-compose exists"
else
  warn "Production docker-compose not found"
fi

if [ -f "$PROJECT_ROOT/deploy/prod/nginx.http.conf" ]; then
  pass "nginx config exists"
else
  warn "nginx config not found"
fi
echo ""

# ---- 7. E2E Test Gate (Blocking) ----
echo "[7] E2E Test Gate"
if command -v npx &>/dev/null && [ -f "$PROJECT_ROOT/frontend/playwright.config.ts" ]; then
  cd "$PROJECT_ROOT/frontend"
  if npx playwright test --version > /dev/null 2>&1; then
    if npx playwright test --workers=1 > /dev/null 2>&1; then
      pass "E2E tests pass"
    else
      fail "E2E tests FAILED — blocking release"
    fi
  else
    skip "Playwright not installed (cd frontend && npm install -D @playwright/test && npx playwright install chromium)"
  fi
  cd "$PROJECT_ROOT"
else
  skip "Playwright config not found"
fi
echo ""

# ---- 8. Frontend Bundle Check (Warning) ----
echo "[8] Frontend Bundle Check"
if [ -f "$PROJECT_ROOT/scripts/frontend-bundle-check.sh" ]; then
  bash "$PROJECT_ROOT/scripts/frontend-bundle-check.sh"
else
  skip "frontend-bundle-check.sh not found"
fi
echo ""

# ---- Summary ----
TOTAL=$((PASS + WARN + FAIL + SKIP))
echo "=========================================="
echo "  Release Readiness Summary"
echo "=========================================="
echo " Results: $PASS passed, $FAIL failed, $WARN warnings, $SKIP skipped ($TOTAL total)"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "Status: NOT READY — resolve FAIL items above before release"
  exit 1
else
  echo "Status: READY — all checks passed"
  echo ""
  echo "Next steps:"
  echo "  1. Update CHANGELOG.md with this release's changes"
  echo "  2. Fill in release notes using docs/release-notes-template.md"
  echo "  3. Run full tests: cd backend && mvn test"
  echo "  4. Run full tests: cd frontend && npm run typecheck && npm run build && npm run test:e2e"
  echo "  5. Create release checklist Issue: https://github.com/yc20041001/ai-coding-platform/issues/new?template=release_checklist.yml"
  exit 0
fi
