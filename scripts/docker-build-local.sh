#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

BACKEND_TAG="${BACKEND_TAG:-ai-coding-platform-backend:local}"
FRONTEND_TAG="${FRONTEND_TAG:-ai-coding-platform-frontend:local}"

echo "=== Building backend image: $BACKEND_TAG ==="
docker build -t "$BACKEND_TAG" "$PROJECT_DIR/backend"

echo ""
echo "=== Building frontend image: $FRONTEND_TAG ==="
docker build \
  --build-arg VITE_API_BASE_URL=/api \
  -t "$FRONTEND_TAG" \
  "$PROJECT_DIR/frontend"

echo ""
echo "=== Done ==="
echo "Backend:  $BACKEND_TAG"
echo "Frontend: $FRONTEND_TAG"
