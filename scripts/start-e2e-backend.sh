#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# E2E Backend Startup Script
# ============================================================
# Purpose: Start a backend Docker container configured for
#          Playwright E2E testing with captcha and login
#          protection disabled.
#
# Usage:
#   bash scripts/start-e2e-backend.sh
#
# After running, the backend is available at:
#   http://localhost:9080
#
# The frontend Vite dev server proxies /api to localhost:9080,
# so E2E tests run against this container.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

IMAGE="ai-coding-platform-backend:local"
CONTAINER="ai-coding-platform-backend-test"
NETWORK="deploy_default"
PORT="9080"

# Ensure Docker image is built
echo "=== Building backend Docker image ==="
docker build -t "$IMAGE" -f "$PROJECT_DIR/backend/Dockerfile" "$PROJECT_DIR/backend"

# Remove old test container if exists
echo "=== Removing old test container (if any) ==="
docker rm -f "$CONTAINER" 2>/dev/null || true

# Start container with E2E-safe auth configuration
echo "=== Starting E2E backend container ==="
docker run -d \
  --name "$CONTAINER" \
  --network "$NETWORK" \
  -p "$PORT:8080" \
  -e AUTH_CAPTCHA_ENABLED=false \
  -e AUTH_LOGIN_PROTECTION_ENABLED=false \
  -e SPRING_DATA_REDIS_HOST=ai-coding-platform-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=ai-coding-platform-redis \
  -e 'DB_URL=jdbc:mysql://ai-coding-platform-mysql:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=platform123 \
  "$IMAGE"

# Wait for health check
echo "=== Waiting for backend health ==="
ATTEMPTS=0
MAX_ATTEMPTS=40
until curl -sf "http://localhost:$PORT/actuator/health" >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo "ERROR: Backend health check timed out after ${MAX_ATTEMPTS} attempts"
    echo "--- Container logs ---"
    docker logs "$CONTAINER" 2>&1 | tail -30
    exit 1
  fi
  printf "."
  sleep 3
done

echo ""
echo "=== Backend is ready ==="
curl -s "http://localhost:$PORT/actuator/health"
echo ""
echo ""
echo "E2E backend running at: http://localhost:$PORT"
echo "Container name: $CONTAINER"
echo ""
echo "E2E Auth configuration:"
echo "  AUTH_CAPTCHA_ENABLED=false"
echo "  AUTH_LOGIN_PROTECTION_ENABLED=false"
echo ""
echo "Ready to run E2E tests:"
echo "  cd frontend && npm run test:e2e -- --workers=1"
