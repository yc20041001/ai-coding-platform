#!/bin/bash
set -e

# ============================================================
# dev-reset-db.sh — 数据库重置脚本
# ============================================================
# 安全保护：
#   - 仅允许操作名称包含 "ai_coding_platform" 的数据库
#   - 必须显式传入 --yes 才执行
#   - 执行前打印完整警告信息
# ============================================================

usage() {
  echo "Usage: $0 [--yes] [--url <jdbc-url>]"
  echo ""
  echo "  重置开发或测试数据库（drop + create），由 Flyway 在下次后端启动时自动迁移。"
  echo ""
  echo "  --yes       跳过确认提示，直接执行"
  echo "  --url URL   指定数据库 JDBC URL"
  echo "              默认: jdbc:mysql://127.0.0.1:3306/ai_coding_platform"
  echo "              测试: jdbc:mysql://127.0.0.1:3306/ai_coding_platform_test"
  echo ""
  echo "安全策略："
  echo "  - 只允许操作名称包含 'ai_coding_platform' 的数据库"
  echo "  - 拒绝操作生产库（名称包含 'prod' 或 'production'）"
  exit 1
}

# ---- Defaults ----
FORCE_YES=false
DB_URL="${TEST_DB_URL:-jdbc:mysql://127.0.0.1:3306/ai_coding_platform_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}"
DB_USER="${TEST_DB_USERNAME:-aicoding}"
DB_PASS="${TEST_DB_PASSWORD:-aicoding123}"

# ---- Parse args ----
while [ $# -gt 0 ]; do
  case "$1" in
    --yes) FORCE_YES=true; shift ;;
    --url) DB_URL="$2"; shift 2 ;;
    --help|-h) usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

# ---- Extract database name from JDBC URL ----
# Example: jdbc:mysql://127.0.0.1:3306/ai_coding_platform_test?params...
DB_NAME=$(echo "$DB_URL" | sed -n 's|.*/\([^/?]*\).*|\1|p')
DB_HOST=$(echo "$DB_URL" | sed -n 's|.*://\([^:/]*\).*|\1|p')
DB_PORT=$(echo "$DB_URL" | sed -n 's|.*:\([0-9]\+\)/.*|\1|p')
[ -z "$DB_PORT" ] && DB_PORT="3306"

if [ -z "$DB_NAME" ]; then
  echo "[ERROR] 无法从 URL 解析数据库名: $DB_URL"
  exit 1
fi

echo "=========================================="
echo "  数据库重置脚本"
echo "=========================================="
echo "  Host:     $DB_HOST:$DB_PORT"
echo "  Database: $DB_NAME"
echo "  User:     $DB_USER"
echo "=========================================="
echo ""

# ---- Safety checks ----

# Check 1: database name must contain ai_coding_platform
if ! echo "$DB_NAME" | grep -q "ai_coding_platform"; then
  echo "[ERROR] 安全策略拒绝：数据库名 '$DB_NAME' 不包含 'ai_coding_platform'"
  echo "  本脚本只允许操作项目开发库或测试库。"
  exit 1
fi

# Check 2: reject production databases
if echo "$DB_NAME" | grep -qiE "prod|production"; then
  echo "[ERROR] 安全策略拒绝：数据库名 '$DB_NAME' 疑似为生产库"
  echo "  本脚本禁止操作名称包含 'prod' 或 'production' 的数据库。"
  exit 1
fi

# Check 3: must be localhost or 127.0.0.1
if [ "$DB_HOST" != "127.0.0.1" ] && [ "$DB_HOST" != "localhost" ]; then
  echo "[ERROR] 安全策略拒绝：数据库主机 '$DB_HOST' 不是本地地址"
  echo "  本脚本只允许操作本地数据库。"
  exit 1
fi

# Check 4: confirm with user (unless --yes)
if [ "$FORCE_YES" != true ]; then
  echo "=========================================="
  echo "  ⚠️  警告: 即将执行以下操作："
  echo ""
  echo "  1. DROP DATABASE IF EXISTS \`$DB_NAME\`"
  echo "  2. CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4"
  echo ""
  echo "  此操作不可逆，所有数据将永久丢失！"
  echo "=========================================="
  echo ""
  read -r -p "确认执行？(输入 YES 继续): " CONFIRM
  if [ "$CONFIRM" != "YES" ]; then
    echo "操作已取消。"
    exit 0
  fi
fi

echo ""
echo "执行数据库重置..."

# ---- Execute ----
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" <<SQL
DROP DATABASE IF EXISTS \`$DB_NAME\`;
CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

if [ $? -eq 0 ]; then
  echo ""
  echo "=========================================="
  echo "  数据库重置成功"
  echo "=========================================="
  echo "  数据库 '$DB_NAME' 已重建。"
  echo "  下次启动后端时 Flyway 将自动执行全部迁移。"
  echo ""
  echo "  启动命令："
  echo "    cd backend && mvn spring-boot:run"
  echo "=========================================="
else
  echo "[ERROR] 数据库重置失败，请检查连接参数和权限。"
  exit 1
fi
