#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env.production not found at $ENV_FILE"
  exit 1
fi

if [ $# -lt 1 ]; then
  echo "Usage: bash scripts/prod-restore-mysql.sh <backup-file.sql>"
  echo ""
  echo "Available backups:"
  ls -1 "${PROJECT_ROOT}/backups/"*.sql 2>/dev/null || echo "  (none)"
  exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "$BACKUP_FILE" ]; then
  echo "Error: backup file not found: $BACKUP_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

echo "WARNING: This will overwrite the current database (${MYSQL_DATABASE:-ai_coding_platform})."
echo "Backup file: ${BACKUP_FILE}"
echo ""
read -p "Type 'yes' to confirm: " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
  echo "Restore cancelled."
  exit 0
fi

echo "Restoring MySQL from ${BACKUP_FILE}..."
cd "$PROJECT_ROOT"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T mysql \
  mysql -uroot -p"${DB_PASSWORD}" "${MYSQL_DATABASE:-ai_coding_platform}" < "$BACKUP_FILE"

echo "Restore complete."
