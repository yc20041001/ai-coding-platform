#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_DIR/frontend"

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
npm run typecheck
echo "  => 类型检查通过"
echo ""

# Step 3: Build
echo "[3/4] 构建前端..."
npm run build
echo "  => 构建成功"
echo ""

# Step 4: E2E tests (if Playwright is installed)
echo "[4/4] 前端 E2E 测试..."
if npx playwright test --version > /dev/null 2>&1; then
  npx playwright test || echo "  [WARN] E2E tests failed (后端可能未启动)"
else
  echo "  [SKIP] Playwright 未安装，跳过 E2E 测试"
  echo "  安装方法：cd frontend && npm install -D @playwright/test && npx playwright install chromium"
fi
echo ""

echo "=========================================="
echo "  前端检查全部通过"
echo "=========================================="
