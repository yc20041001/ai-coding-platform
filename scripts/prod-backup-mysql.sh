#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"
BACKUP_DIR="${PROJECT_ROOT}/backups"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env.production not found at $ENV_FILE"
  echo "Run: cp .env.production.example .env.production and edit values."
  exit 1
fi

# Source env to get MYSQL_ROOT_PASSWORD without printing it
set -a
source "$ENV_FILE"
set +a

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/ai_coding_platform_${TIMESTAMP}.sql"

echo "Backing up MySQL to ${BACKUP_FILE}..."
cd "$PROJECT_ROOT"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T mysql \
  mysqldump -uroot -p"${DB_PASSWORD}" --single-transaction --routines --triggers \
  "${MYSQL_DATABASE:-ai_coding_platform}" > "$BACKUP_FILE"

echo "Backup complete: ${BACKUP_FILE} ($(du -h "$BACKUP_FILE" | cut -f1))"
