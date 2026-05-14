#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_DIR/backend"

echo "=========================================="
echo "  后端检查脚本"
echo "=========================================="
echo ""

# Step 1: Compile
echo "[1/3] 编译后端..."
cd "$BACKEND_DIR"
mvn clean compile -q
echo "  => 编译成功"
echo ""

# Step 2: Run tests
echo "[2/3] 运行集成测试..."
mvn test
echo "  => 测试通过"
echo ""

# Step 3: Package
echo "[3/3] 打包后端..."
mvn package -DskipTests -q
echo "  => 打包成功"
echo ""

echo "=========================================="
echo "  后端检查全部通过"
echo "=========================================="
