#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# frontend-bundle-check.sh — Bundle Size Budget Check
# ============================================================
# Checks frontend/dist/assets for chunk sizes against budgets.
# Outputs PASS/WARN lines. Designed for release-checklist.sh.
# ============================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/frontend/dist/assets"

PASS=0; WARN=0; FAIL=0

# Budgets (raw KB)
MAX_CHUNK_WARN=500
MAX_CHUNK_FAIL=1000
MAX_MARKDOWN_WARN=250
MAX_PUBLIC_WARN=100

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
warn() { echo "  [WARN] $1"; WARN=$((WARN + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

if [ ! -d "$DIST_DIR" ]; then
  echo "[SKIP] dist/assets not found — run 'cd frontend && npm run build' first"
  exit 0
fi

pass "dist exists"

# Find largest JS chunk
largest_file=$(find "$DIST_DIR" -name "*.js" -type f -exec ls -l {} \; | sort -k5 -n -r | head -1)
largest_name=$(echo "$largest_file" | awk '{print $NF}' | xargs basename)
largest_size=$(echo "$largest_file" | awk '{print $5}')
largest_kb=$((largest_size / 1024))

echo "  [INFO] Largest JS chunk: $largest_name $largest_kb KB"

if [ "$largest_kb" -gt "$MAX_CHUNK_FAIL" ]; then
  fail "Largest chunk exceeds $MAX_CHUNK_FAIL KB raw ($largest_kb KB)"
elif [ "$largest_kb" -gt "$MAX_CHUNK_WARN" ]; then
  # vendor-element exemption: expected to be large
  if echo "$largest_name" | grep -q "vendor-element"; then
    warn "vendor-element chunk is $largest_kb KB raw (> ${MAX_CHUNK_WARN} KB, documented exemption)"
  else
    warn "Largest chunk exceeds $MAX_CHUNK_WARN KB raw ($largest_kb KB)"
  fi
fi

# Check markdown chunk
markdown_chunk=$(find "$DIST_DIR" -name "vendor-markdown*.js" -type f 2>/dev/null | head -1)
if [ -n "$markdown_chunk" ]; then
  markdown_size=$(stat -f%z "$markdown_chunk" 2>/dev/null || stat -c%s "$markdown_chunk" 2>/dev/null)
  markdown_kb=$((markdown_size / 1024))
  if [ "$markdown_kb" -gt "$MAX_MARKDOWN_WARN" ]; then
    warn "Markdown chunk exceeds $MAX_MARKDOWN_WARN KB raw ($markdown_kb KB)"
  else
    pass "Markdown chunk under $MAX_MARKDOWN_WARN KB raw ($markdown_kb KB)"
  fi
else
  warn "No vendor-markdown chunk found — check manualChunks"
fi

# Check public page chunk
public_chunk=$(find "$DIST_DIR" -name "PublicHomePage*.js" -type f 2>/dev/null | head -1)
if [ -n "$public_chunk" ]; then
  public_size=$(stat -f%z "$public_chunk" 2>/dev/null || stat -c%s "$public_chunk" 2>/dev/null)
  public_kb=$((public_size / 1024))
  if [ "$public_kb" -gt "$MAX_PUBLIC_WARN" ]; then
    warn "Public page chunk exceeds $MAX_PUBLIC_WARN KB raw ($public_kb KB)"
  else
    pass "Public page chunk under $MAX_PUBLIC_WARN KB raw ($public_kb KB)"
  fi
else
  warn "No PublicHomePage chunk found"
fi

echo ""
echo "  Bundle check: $PASS passed, $FAIL failed, $WARN warnings"
