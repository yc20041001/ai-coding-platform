#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# demo-reset-data.sh — Demo Data Reset Script
# ============================================================
# Only deletes data with "Demo" prefix in name/title.
# Requires --yes flag. Never touches non-Demo data.
# Never runs DROP DATABASE or any destructive SQL.
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
DEMO_PROJECT_NAME="${DEMO_PROJECT_NAME:-Demo AI Workspace}"

echo "=========================================="
echo "  Demo Data Reset"
echo "=========================================="
echo ""

# ---- Safety: must pass --yes ----
if [ "${1:-}" != "--yes" ]; then
  echo "WARNING: This script deletes ALL Demo-prefixed data."
  echo ""
  echo "It will attempt to delete:"
  echo "  - Projects named \"$DEMO_PROJECT_NAME\" or \"Demo Project\""
  echo "  - Knowledge Bases named \"Product Knowledge Base\" or \"Demo Knowledge Base\""
  echo "  - Chat Sessions named \"Ask Product Knowledge\" or \"Demo Chat Session\""
  echo "  - Tasks with \"Demo\" prefix in title"
  echo ""
  echo "Non-Demo data is NEVER touched."
  echo ""
  echo "To proceed, run:"
  echo "  bash scripts/demo-reset-data.sh --yes"
  echo ""
  exit 0
fi

echo "Proceeding with demo data reset..."
echo ""

# ---- helpers ----
http_get() {
  local url="$1"; local token="$2"
  curl -s -H "Authorization: Bearer $token" "$url"
}

http_delete() {
  local url="$1"; local token="$2"
  curl -s -o /dev/null -w "%{http_code}" -X DELETE -H "Authorization: Bearer $token" "$url"
}

extract_field() {
  local json="$1"; local path="$2"
  echo "$json" | python3 -c "
import sys, json
data = json.load(sys.stdin)
parts = '$path'.split('.')
for p in parts:
    if isinstance(data, dict):
        data = data.get(p, '')
    else:
        data = ''
print(data)
" 2>/dev/null || echo ""
}

# ---- Step 1: Login ----
echo "[1] Login as admin"
LOGIN_RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  "$BASE_URL/api/auth/login")
TOKEN=$(extract_field "$LOGIN_RESP" "data.accessToken")

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "  [FAIL] Login failed — is backend running?"
  exit 1
fi
echo "  [PASS] Login successful"
echo ""

# ---- Step 2: Find and delete Demo Tasks ----
echo "[2] Clean up Demo Tasks"

PROJECTS_RESP=$(http_get "$BASE_URL/api/projects?page=1&pageSize=50" "$TOKEN")
DEMO_PROJECT_IDS=$(echo "$PROJECTS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    name = r.get('name','')
    if 'Demo' in name:
        print(r.get('id',''))
" 2>/dev/null || echo "")

if [ -z "$DEMO_PROJECT_IDS" ]; then
  echo "  [SKIP] No Demo projects found — nothing to reset"
  echo ""
  echo "=========================================="
  echo "  No demo data to remove."
  echo "  Run: bash scripts/demo-seed-data.sh"
  echo "=========================================="
  exit 0
fi

DELETED_TASKS=0
DELETED_SESSIONS=0
DELETED_DOCS=0
DELETED_KBS=0
DELETED_PROJECTS=0

for PID in $DEMO_PROJECT_IDS; do
  # Delete tasks with "Demo" in title
  TASKS_RESP=$(http_get "$BASE_URL/api/projects/$PID/tasks?page=1&pageSize=50" "$TOKEN")
  DEMO_TASK_IDS=$(echo "$TASKS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    t = r.get('title','')
    if 'Demo' in t or 'Generate architecture review' in t or 'Implement health check' in t:
        print(r.get('id',''))
" 2>/dev/null || echo "")

  for TID in $DEMO_TASK_IDS; do
    CODE=$(http_delete "$BASE_URL/api/tasks/$TID" "$TOKEN")
    if [ "$CODE" = "200" ] || [ "$CODE" = "204" ]; then
      echo "  [OK] Deleted Task: $TID"
      DELETED_TASKS=$((DELETED_TASKS + 1))
    else
      echo "  [WARN] Could not delete Task $TID (HTTP $CODE)"
    fi
  done

  # Delete chat sessions with "Demo" in title
  SESSIONS_RESP=$(http_get "$BASE_URL/api/projects/$PID/chat/sessions?page=1&pageSize=50" "$TOKEN")
  DEMO_SESSION_IDS=$(echo "$SESSIONS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    t = r.get('title','')
    if 'Demo' in t or 'Ask Product Knowledge' in t:
        print(r.get('id',''))
" 2>/dev/null || echo "")

  for SID in $DEMO_SESSION_IDS; do
    # Chat sessions don't have a delete API currently — mark as WARN
    echo "  [WARN] Chat Session $SID: delete via UI or API (no DELETE endpoint)"
  done

  # Delete knowledge documents with "Demo" prefix or known names
  DOCS_RESP=$(http_get "$BASE_URL/api/projects/$PID/knowledge-documents?page=1&pageSize=50" "$TOKEN")
  DEMO_DOC_IDS=$(echo "$DOCS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
demo_titles = ['AI Coding Platform Overview','Agent Workflow Guide','Repository Review Guide','Agent Orchestrator Guide']
for r in records:
    t = r.get('title','')
    if 'Demo' in t or t in demo_titles:
        print(r.get('id',''))
" 2>/dev/null || echo "")

  for DID in $DEMO_DOC_IDS; do
    CODE=$(http_delete "$BASE_URL/api/projects/$PID/knowledge-documents/$DID" "$TOKEN")
    if [ "$CODE" = "200" ] || [ "$CODE" = "204" ]; then
      echo "  [OK] Deleted Document: $DID"
      DELETED_DOCS=$((DELETED_DOCS + 1))
    else
      echo "  [WARN] Could not delete Document $DID (HTTP $CODE)"
    fi
  done

  # Delete knowledge bases with "Demo" in name
  KBS_RESP=$(http_get "$BASE_URL/api/projects/$PID/knowledge-bases?page=1&pageSize=20" "$TOKEN")
  DEMO_KB_IDS=$(echo "$KBS_RESP" | python3 -c "
import sys, json
records = json.load(sys.stdin).get('data',{}).get('records',[])
for r in records:
    n = r.get('name','')
    if 'Demo' in n or 'Product Knowledge Base' in n:
        print(r.get('id',''))
" 2>/dev/null || echo "")

  for KID in $DEMO_KB_IDS; do
    CODE=$(http_delete "$BASE_URL/api/projects/$PID/knowledge-bases/$KID" "$TOKEN")
    if [ "$CODE" = "200" ] || [ "$CODE" = "204" ]; then
      echo "  [OK] Deleted Knowledge Base: $KID"
      DELETED_KBS=$((DELETED_KBS + 1))
    else
      echo "  [WARN] Could not delete Knowledge Base $KID (HTTP $CODE)"
    fi
  done

  # Delete the Demo project itself
  CODE=$(http_delete "$BASE_URL/api/projects/$PID" "$TOKEN")
  if [ "$CODE" = "200" ] || [ "$CODE" = "204" ]; then
    echo "  [OK] Deleted Project: $PID"
    DELETED_PROJECTS=$((DELETED_PROJECTS + 1))
  else
    echo "  [WARN] Could not delete Project $PID (HTTP $CODE)"
  fi
done

echo ""
echo "=========================================="
echo "  Demo Data Reset Complete"
echo "=========================================="
echo "  Projects deleted:   $DELETED_PROJECTS"
echo "  Knowledge Bases:    $DELETED_KBS"
echo "  Documents:          $DELETED_DOCS"
echo "  Tasks:              $DELETED_TASKS"
echo "  Chat Sessions:      (delete via UI)"
echo ""
echo "  Non-Demo data was NOT touched."
echo ""
echo "  To re-create demo data:"
echo "    bash scripts/demo-seed-data.sh"
echo "=========================================="
