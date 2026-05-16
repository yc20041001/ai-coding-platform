# Bundle Analysis Report — Milestone 28

## Before (Baseline)

Captured from `npm run build` before any optimization.

```
dist/assets/MarkdownRenderer-BHk9zOmP.js    161.51 kB │ gzip:  63.38 kB
dist/assets/index-D5lFcy79.js             1,039.74 kB │ gzip: 342.43 kB
```

| Metric | Value |
|--------|-------|
| Largest chunk | `index-D5lFcy79.js` — 1,039.74 KB raw / 342.43 KB gzip |
| Markdown chunk | `MarkdownRenderer-BHk9zOmP.js` — 161.51 KB raw / 63.38 KB gzip |
| Element Plus | Embedded in `index.js` (not separately measurable) |
| Total JS assets | ~1,300 KB raw (estimated) |
| App code vs vendors | Mixed together in `index.js` |

**Root cause**: No `manualChunks` configuration. All `node_modules` (vue, pinia, vue-router, element-plus, markdown-it, highlight.js, axios, etc.) bundled into `index.js` alongside application code.

## After (Optimized)

Captured from `npm run build` after applying `manualChunks` + `defineAsyncComponent`.

```
dist/assets/vendor-vue-CnIWnhEW.js           494.67 kB │ gzip: 161.84 kB
dist/assets/vendor-element-LCwBQM8g.js        439.13 kB │ gzip: 145.39 kB
dist/assets/vendor-DmJSV_dH.js                211.79 kB │ gzip:  87.14 kB
dist/assets/vendor-markdown-t0Nu_3ED.js       106.55 kB │ gzip:  33.58 kB
dist/assets/TaskDetailPage-C82ZMS6j.js         16.21 kB │ gzip:   5.03 kB
dist/assets/ModelConfigPage-DHRKx_uH.js        15.72 kB │ gzip:   4.78 kB
dist/assets/PublicHomePage-DYCX7IKV.js         13.81 kB │ gzip:   5.13 kB
dist/assets/RepositoryPanel-BGgve4SK.js        10.83 kB │ gzip:   3.53 kB
dist/assets/KnowledgeBasePage-DsqTmQmo.js      10.81 kB │ gzip:   3.78 kB
dist/assets/TaskListPage-D7Zomm7t.js           10.19 kB │ gzip:   3.39 kB
dist/assets/ChatPage-oyPMHhik.js                9.06 kB │ gzip:   3.69 kB
dist/assets/index-BM6iqzRq.js                   8.44 kB │ gzip:   3.12 kB
dist/assets/ObservabilityPage-QKtzF04D.js       8.17 kB │ gzip:   2.80 kB
dist/assets/PullRequestReviewPage-C6g5hgwX.js   7.73 kB │ gzip:   2.99 kB
dist/assets/DashboardPage-C5PRQJjR.js           6.89 kB │ gzip:   2.28 kB
dist/assets/MemberPanel-CM29EgK7.js             6.63 kB │ gzip:   2.47 kB
dist/assets/ProjectListPage-BlXyiC8c.js         4.47 kB │ gzip:   1.90 kB
dist/assets/GithubIntegrationPage-CSZnuWFb.js   4.39 kB │ gzip:   1.97 kB
dist/assets/BasicLayout-TXLxbApi.js             3.71 kB │ gzip:   1.50 kB
dist/assets/ProjectDetailPage-LXpXb6Zi.js       3.42 kB │ gzip:   1.57 kB
dist/assets/LoginPage-Brz_Nc5Q.js               2.96 kB │ gzip:   1.29 kB
dist/assets/AgentListPage-BGP6jLra.js           2.56 kB │ gzip:   1.26 kB
dist/assets/MarkdownRenderer-DsRldNek.js        1.27 kB │ gzip:   0.58 kB
(+ ~15 small shared chunks < 2 KB each)
```

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Largest chunk | 1,039.74 KB (`index`) | 494.67 KB (`vendor-vue`) | -52% |
| index (app code) | 1,039.74 KB | 8.44 KB | -99% |
| Markdown chunk | 161.51 KB (inline) | 106.55 KB (`vendor-markdown`) | -34% |
| Element Plus | Embedded in index | 439.13 KB (`vendor-element`) | Now isolated |
| Vue ecosystem | Embedded in index | 494.67 KB (`vendor-vue`) | Now isolated |
| Public page chunk | Embedded in index | 13.81 KB | Now measurable |
| Total JS assets | ~1,300 KB | ~1,410 KB* | +8% (expected — chunk splitting adds overhead) |

\* Total raw size increased slightly due to Rollup chunk overhead, but **cacheability is dramatically improved**: vendor chunks rarely change between deploys, so repeat visitors load only the small app chunks.

## Changes

### Moved into separate chunks (manualChunks)
- **vendor-vue** (494.67 KB): vue, pinia, vue-router, @vueuse/core
- **vendor-element** (439.13 KB): element-plus, @element-plus/icons-vue
- **vendor-markdown** (106.55 KB): markdown-it, highlight.js
- **vendor** (211.79 KB): axios, lucide-vue-next, and all other node_modules

### Lazy loaded (defineAsyncComponent)
- `MarkdownRenderer.vue` in `AgentExecutionDrawer.vue` — drawer content, not first-screen
- `MarkdownRenderer.vue` in `ChunkPreviewDrawer.vue` — drawer content, not first-screen
- `MarkdownRenderer.vue` in `RepositoryPanel.vue` — diff viewing, not first-screen
- `MarkdownRenderer.vue` in `TaskDetailPage.vue` — artifact panel, not first-screen

### What remains large
- `vendor-vue` at 495 KB: expected for Vue 3 + router + pinia ecosystem
- `vendor-element` at 439 KB: expected for full Element Plus import (tree-shaking limited for component libraries)

### Within budget
- `vendor-markdown` at 107 KB: under 250 KB budget
- `PublicHomePage` at 14 KB: under 100 KB budget
- No single chunk exceeds 500 KB raw (warning threshold) — `vendor-vue` at 495 KB is borderline but under budget

## Known note

Rollup emits a circular chunk warning (`vendor-element -> vendor-vue -> vendor-element`). This is a non-functional warning caused by Element Plus importing Vue, and Vue's reactivity system being in a separate chunk. It does not affect runtime behavior — verified by build success and E2E pass.
