#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

check_env() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env.production not found at $ENV_FILE"
    echo "Run: cp .env.production.example .env.production and edit values."
    exit 1
  fi
}

cmd_build() {
  check_env
  echo "Building production images..."
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache
}

cmd_up() {
  check_env
  echo "Starting production stack..."
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d "$@"
  echo ""
  echo "Services started. Check status with: bash scripts/prod-deploy.sh status"
}

cmd_pull() {
  check_env
  echo "Pulling images from registry..."
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
}

cmd_restart() {
  check_env
  echo "Restarting production stack..."
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" restart "$@"
}

cmd_status() {
  check_env
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
}

cmd_down() {
  check_env
  echo "Stopping production stack..."
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down "$@"
}

cmd_logs() {
  check_env
  cd "$PROJECT_ROOT"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f "$@"
}

usage() {
  echo "Usage: bash scripts/prod-deploy.sh <command> [options]"
  echo ""
  echo "Commands:"
  echo "  build      Build production Docker images"
  echo "  up         Start all services (--build to rebuild)"
  echo "  down       Stop all services"
  echo "  pull       Pull images from container registry"
  echo "  restart    Restart services"
  echo "  status     Show service status"
  echo "  logs       Tail logs (optionally pass service name)"
  echo ""
  echo "Examples:"
  echo "  bash scripts/prod-deploy.sh up --build"
  echo "  bash scripts/prod-deploy.sh status"
  echo "  bash scripts/prod-deploy.sh logs backend"
  exit 1
}

CMD="${1:-}"
shift 2>/dev/null || true

case "$CMD" in
  build)   cmd_build "$@";;
  up)      cmd_up "$@";;
  down)    cmd_down "$@";;
  pull)    cmd_pull "$@";;
  restart) cmd_restart "$@";;
  status)  cmd_status "$@";;
  logs)    cmd_logs "$@";;
  *)       usage;;
esac
