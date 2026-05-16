# Frontend Performance Budget

## 1. Chunk Size Budgets

| Metric | Warning | Fail | Notes |
|--------|---------|------|-------|
| Single chunk (raw) | > 500 KB | > 1000 KB | Except `vendor-element` with documented reason |
| Markdown chunk (raw) | > 250 KB | — | `vendor-markdown` (markdown-it + highlight.js) |
| Public page chunk (raw) | > 100 KB | — | `/public` route chunk |
| Total JS assets (raw) | > 2 MB | > 3 MB | Sum of all `.js` files in `dist/assets/` |
| Initial route JS (gzip) | > 500 KB | — | Estimated first-load JS over network |

## 2. First Screen Load Budget

| Metric | Target | Notes |
|--------|--------|-------|
| `/public` FCP | < 2.0s | Static marketing page |
| `/login` FCP | < 2.5s | Auth page |
| `/dashboard` FCP | < 3.0s | Post-login console |

## 3. Release Gate

- **Typecheck**: `npm run typecheck` must pass (blocking)
- **Build**: `npm run build` must pass (blocking)
- **E2E**: 13/13 must pass, 2 consecutive runs (blocking)
- **Bundle size**: Warning-only for initial rollout; will escalate to blocking in a future milestone
- **`scripts/frontend-bundle-check.sh`**: Runs as WARN in release checklist

## 4. Bundle Check Script Output

```
[PASS] dist exists
[INFO] Largest JS chunk: <name> <size>
[WARN] <chunk> exceeds <budget> KB raw
[PASS] Public page chunk under 100 KB raw
```

## 5. Known Exemptions

- `vendor-element` (Element Plus): expected to be 400-800 KB raw due to full component library import. Monitor for growth but do not block release.
- `vendor-markdown` (markdown-it + highlight.js): expected 200-350 KB raw. Highlight.js uses `lib/core` with per-language registration, not full build.

## 6. Review Cadence

- Review bundle sizes after each milestone that touches frontend dependencies
- Re-run `scripts/frontend-bundle-check.sh` as part of `scripts/release-checklist.sh`
- Tighten budgets in future milestones as optimizations mature
