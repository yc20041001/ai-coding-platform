#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "  项目一键检查脚本"
echo "=========================================="
echo ""

FAILED=0

# Backend checks
echo "----------------------------------------"
echo "  阶段 1/2: 后端检查"
echo "----------------------------------------"
if bash "$SCRIPT_DIR/run-backend-checks.sh"; then
  echo "  => 后端检查通过"
else
  echo "  => 后端检查失败"
  FAILED=1
fi
echo ""

# Frontend checks
echo "----------------------------------------"
echo "  阶段 2/2: 前端检查"
echo "----------------------------------------"
if bash "$SCRIPT_DIR/run-frontend-checks.sh"; then
  echo "  => 前端检查通过"
else
  echo "  => 前端检查失败"
  FAILED=1
fi
echo ""

# Final result
echo "=========================================="
if [ $FAILED -eq 0 ]; then
  echo "  所有检查通过"
else
  echo "  存在失败的检查，请查看上方日志"
fi
echo "=========================================="

exit $FAILED
