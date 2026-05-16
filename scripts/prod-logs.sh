#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env.production not found at $ENV_FILE"
  echo "Run: cp .env.production.example .env.production and edit values."
  exit 1
fi

SERVICE="${1:-}"

cd "$PROJECT_ROOT"
if [ -n "$SERVICE" ]; then
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f "$SERVICE"
else
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f
fi
