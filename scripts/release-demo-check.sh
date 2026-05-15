#!/usr/bin/env bash
set -euo pipefail

# Release Demo Check Script
# Runs automated checks for Milestone 19 release QA acceptance.
# Usage: bash scripts/release-demo-check.sh [--with-e2e]

WITH_E2E=false
if [[ "${1:-}" == "--with-e2e" ]]; then
  WITH_E2E=true
fi

echo "========================================="
echo " Release Demo Check"
echo "========================================="

echo ""
echo "== Backend checks =="
(cd backend && mvn clean compile -q)
echo "  [PASS] mvn clean compile"

(cd backend && mvn test -q)
echo "  [PASS] mvn test"

(cd backend && mvn package -DskipTests -q)
echo "  [PASS] mvn package -DskipTests"

echo ""
echo "== Frontend checks =="
(cd frontend && npm run typecheck --silent 2>/dev/null)
echo "  [PASS] npm run typecheck"

(cd frontend && npm run build --silent 2>/dev/null)
echo "  [PASS] npm run build"

if $WITH_E2E; then
  echo ""
  echo "== Frontend E2E (sequential) =="
  (cd frontend && npx playwright test --workers=1)
  echo "  [PASS] npm run test:e2e"
else
  echo ""
  echo "  [SKIP] E2E tests (use --with-e2e to run)"
fi

echo ""
echo "== Docker images =="
docker build -t ai-coding-platform-backend:local ./backend > /dev/null 2>&1
echo "  [PASS] docker build backend"

docker build -t ai-coding-platform-frontend:local ./frontend > /dev/null 2>&1
echo "  [PASS] docker build frontend"

echo ""
echo "== Git status =="
git status --short
if [ -z "$(git status --short)" ]; then
  echo "  [PASS] working tree clean"
fi

echo ""
echo "== Secrets check =="
SECRET_FILES=$(git ls-files '*.env' '*.pem' '*.key' '*.p12' '*.jks' 2>/dev/null || true)
if [ -z "$SECRET_FILES" ]; then
  echo "  [PASS] no secrets tracked in git"
else
  echo "  [FAIL] secrets found: $SECRET_FILES"
fi

echo ""
echo "========================================="
echo " Release demo check completed."
echo "========================================="
