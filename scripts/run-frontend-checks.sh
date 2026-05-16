#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_DIR/frontend"

PASS=0; FAIL=0

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

echo "=========================================="
echo "  前端检查脚本"
echo "=========================================="
echo ""

cd "$FRONTEND_DIR"

# Step 1: Install dependencies
echo "[1/4] 安装依赖..."
npm install --silent
echo "  => 依赖安装完成"
echo ""

# Step 2: Type check
echo "[2/4] TypeScript 类型检查..."
if npm run typecheck; then
  pass "TypeScript typecheck"
else
  fail "TypeScript typecheck"
fi
echo ""

# Step 3: Build
echo "[3/4] 构建前端..."
if npm run build; then
  pass "Frontend build"
else
  fail "Frontend build"
fi
echo ""

# Step 4: E2E tests (if Playwright is installed)
echo "[4/4] 前端 E2E 测试..."
if npx playwright test --version > /dev/null 2>&1; then
  if npx playwright test --workers=1; then
    pass "E2E tests"
  else
    fail "E2E tests — 阻塞发布"
  fi
else
  echo "  [SKIP] Playwright 未安装，跳过 E2E 测试"
  echo "  安装方法：cd frontend && npm install -D @playwright/test && npx playwright install chromium"
fi
echo ""

echo "=========================================="
echo "  前端检查结果: $PASS passed, $FAIL failed"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
  echo "Status: FAILED — 阻塞发布"
  exit 1
else
  echo "Status: PASSED"
  exit 0
fi
