#!/usr/bin/env bash
set -euo pipefail

# Production diagnostics — collects troubleshooting info without secrets.
# Usage: bash scripts/prod-diagnostics.sh
# Output: diagnostics/diagnostics_YYYYmmdd_HHMMSS/

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.production"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/prod/docker-compose.prod.yml"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DIAG_DIR="${PROJECT_ROOT}/diagnostics/diagnostics_${TIMESTAMP}"

mkdir -p "$DIAG_DIR"

# ---- Helper: run command and save output with masking ----
capture() {
  local filename="$1"
  shift
  local cmd="$*"
  echo "  Collecting: $filename"
  # Run command, mask sensitive patterns
  $cmd 2>&1 | sed \
    -e 's/sk-[a-zA-Z0-9]\{20,\}/sk-****/g' \
    -e 's/ghp_[a-zA-Z0-9]\{20,\}/ghp_****/g' \
    -e 's/github_pat_[a-zA-Z0-9]\{20,\}/github_pat_****/g' \
    -e 's/Bearer [a-zA-Z0-9_\-]\{32,\}/Bearer ****/g' \
    -e 's/password=[^& ]\{6,\}/password=****/g' \
    -e 's/\(DB_PASSWORD=\).*/\1****/' \
    -e 's/\(JWT_SECRET=\).*/\1****/' \
    -e 's/\(OPENAI_API_KEY=\).*/\1****/' \
    -e 's/\(CLAUDE_API_KEY=\).*/\1****/' \
    -e 's/\(DEEPSEEK_API_KEY=\).*/\1****/' \
    -e 's/\(QWEN_API_KEY=\).*/\1****/' \
    -e 's/\(GEMINI_API_KEY=\).*/\1****/' \
    -e 's/\(GITHUB_CLIENT_SECRET=\).*/\1****/' \
    -e 's/\(RABBITMQ_DEFAULT_PASS=\).*/\1****/' \
    > "$DIAG_DIR/$filename" 2>/dev/null || echo "(command failed or service not running)" > "$DIAG_DIR/$filename"
}

echo "============================================"
echo " Production Diagnostics"
echo " Output: $DIAG_DIR"
echo "============================================"
echo ""

# ---- 1. Docker info ----
echo "[1] Docker Status"

if command -v docker &>/dev/null; then
  capture "docker-ps.txt" docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

  if docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" 2>/dev/null; then
    capture "docker-stats.txt" docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
  else
    echo "(docker stats not available)" > "$DIAG_DIR/docker-stats.txt"
  fi

  if [ -f "$ENV_FILE" ]; then
    capture "compose-ps.txt" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
  fi
else
  echo "  SKIP: Docker not available"
fi
echo ""

# ---- 2. Container logs (last 100 lines each) ----
echo "[2] Container Logs"

if command -v docker &>/dev/null && [ -f "$ENV_FILE" ]; then
  for svc in backend nginx mysql; do
    if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps "$svc" 2>/dev/null | grep -q "Up\|running"; then
      capture "${svc}-logs-tail.txt" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=100 "$svc"
    else
      echo "  SKIP: $svc not running"
    fi
  done
else
  echo "  SKIP: Docker or .env.production not available"
fi
echo ""

# ---- 3. Backend health ----
echo "[3] Backend Health"

if command -v docker &>/dev/null && [ -f "$ENV_FILE" ]; then
  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps backend 2>/dev/null | grep -q "Up\|running"; then
    capture "backend-health.json" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T backend wget -qO- http://localhost:8080/actuator/health
  else
    echo "  SKIP: backend not running"
  fi
else
  echo "  SKIP: Docker not available"
fi
echo ""

# ---- 4. System resources ----
echo "[4] System Resources"

capture "disk-usage.txt" df -h
capture "memory.txt" free -h
echo ""

# ---- 5. Smoke test ----
echo "[5] Quick Smoke Test"

BASE_URL="${APP_BASE_URL:-http://localhost}"

if command -v curl &>/dev/null; then
  {
    echo "Target: $BASE_URL"
    echo ""

    # Homepage
    HOME_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 "$BASE_URL/" 2>/dev/null || echo "000")
    echo "GET / => HTTP $HOME_CODE"

    # Login
    LOGIN_RESP=$(curl -s -k --max-time 10 -X POST \
      -H "Content-Type: application/json" \
      -d '{"email":"admin@example.com","password":"Admin@123456"}' \
      "$BASE_URL/api/auth/login" 2>/dev/null || echo "{}")

    TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
    if [ -n "$TOKEN" ]; then
      echo "POST /api/auth/login => 200 (login OK)"

      ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 \
        -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/auth/me" 2>/dev/null || echo "000")
      echo "GET /api/auth/me => HTTP $ME_CODE"

      PROJ_CODE=$(curl -s -o /dev/null -w "%{http_code}" -k --max-time 10 \
        -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/projects?page=1&size=1" 2>/dev/null || echo "000")
      echo "GET /api/projects => HTTP $PROJ_CODE"
    else
      echo "POST /api/auth/login => FAILED"
    fi
  } > "$DIAG_DIR/smoke-test.txt"
  echo "  Collected: smoke-test.txt"
else
  echo "  SKIP: curl not available"
fi
echo ""

# ---- 6. File permissions ----
echo "[6] File Permissions"

capture "env-file-check.txt" sh -c "
  cd '$PROJECT_ROOT'
  echo '.env.production present: ' && test -f .env.production && echo 'yes (permissions: '$(stat -f '%p' .env.production 2>/dev/null || stat -c '%a' .env.production 2>/dev/null || echo '?')')' || echo 'no'
  echo 'backups/ present: ' && test -d backups && echo 'yes' || echo 'no'
  echo 'backups/ file count: ' && ls backups/*.sql 2>/dev/null | wc -l || echo '0'
"
echo ""

# ---- 7. Git status ----
echo "[7] Git Status"

capture "git-status.txt" git -C "$PROJECT_ROOT" status --short
echo ""

# ---- Done ----
echo "============================================"
echo " Diagnostics collected in: $DIAG_DIR"
echo ""
ls -la "$DIAG_DIR/"
echo ""
echo "============================================"
echo " To review:  cat $DIAG_DIR/*.txt"
echo " To package: tar -czf $DIAG_DIR.tar.gz -C $(dirname "$DIAG_DIR") $(basename "$DIAG_DIR")"
echo " Remember:   diagnostics/ is in .gitignore — do NOT commit."
echo "============================================"
