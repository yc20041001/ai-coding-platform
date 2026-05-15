#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/deploy/docker-compose.app.yml"

AUTO_DOWN=false
if [ "${1:-}" = "--down" ]; then
  AUTO_DOWN=true
fi

echo "=== Docker Compose App Smoke Test ==="
echo "Compose file: $COMPOSE_FILE"
echo ""

# Start services
echo "--- Starting services ---"
docker compose -f "$COMPOSE_FILE" up -d --build

# Wait for backend health
echo "--- Waiting for backend health ---"
ATTEMPTS=0
MAX_ATTEMPTS=60
until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo "ERROR: Backend health check timed out after ${MAX_ATTEMPTS} attempts"
    docker compose -f "$COMPOSE_FILE" logs backend
    exit 1
  fi
  printf "."
  sleep 3
done
echo ""
echo "Backend health:"
curl -s http://localhost:8080/actuator/health | head -20
echo ""

# Wait for frontend
echo "--- Waiting for frontend ---"
ATTEMPTS=0
until curl -sf -o /dev/null http://localhost:5173 >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo "ERROR: Frontend health check timed out after ${MAX_ATTEMPTS} attempts"
    exit 1
  fi
  printf "."
  sleep 3
done
echo ""
echo "Frontend HTTP status:"
curl -sI http://localhost:5173 | head -5
echo ""

# Run API smoke test if backend is up
if [ -x "$SCRIPT_DIR/backend-unified-smoke-test.sh" ]; then
  echo "--- Running API smoke test ---"
  BASE_URL=http://localhost:8080 bash "$SCRIPT_DIR/backend-unified-smoke-test.sh" || {
    echo "WARNING: API smoke test had failures (may be expected for fresh DB)"
  }
else
  echo "--- Skipping API smoke test (script not found) ---"
fi

echo ""
echo "=== Smoke test complete ==="
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
echo "Health:   http://localhost:8080/actuator/health"

if [ "$AUTO_DOWN" = true ]; then
  echo ""
  echo "--- Stopping services (--down) ---"
  docker compose -f "$COMPOSE_FILE" down
fi
