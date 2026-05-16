# Milestone 28: 前端性能优化与包体治理

## 1. 背景

当前项目已经完成：

- Milestone 26：产品官网 / 对外展示与试用入口。
- Milestone 27：E2E 稳定性修复与发布质量收口。

最近构建报告中反复出现：

```text
index.js around 1039 KB
MarkdownRenderer.js around 162 KB
Element Plus / markdown-it / highlight.js chunks are large
```

现在功能链路和 E2E 稳定性已经基本收口，下一阶段应减少首屏负担、治理依赖包体、明确性能预算，让前端更接近可发布产品状态。

> 验收目标：不改变业务逻辑和 UI 风格的前提下，完成前端 bundle 分析、拆包治理、懒加载优化和性能预算文档，并保证 typecheck / build / E2E 全部通过。

## 2. 严格边界

执行本阶段必须遵守：

1. 不改后端代码。
2. 不改 API 契约。
3. 不改认证逻辑。
4. 不改核心业务流程。
5. 不重写前端页面。
6. 不更换 UI 框架。
7. 不删除现有功能。
8. 不牺牲 Chat SSE。
9. 不破坏 Markdown 渲染。
10. 不破坏 Public Home `/public`。
11. 不通过隐藏 warning 伪装优化。
12. 不引入复杂 SSR / SSG / Nuxt。
13. 不接外部性能监控 SaaS。
14. 不让 E2E 变成 skip。

允许做：

- Vite `manualChunks` 优化。
- 动态 import 重型组件。
- MarkdownRenderer 懒加载。
- highlight.js 语言按需注册。
- 路由级 chunk 命名。
- 基础 bundle analysis。
- 性能预算文档。
- 小范围 loading fallback。
- 小范围组件异步化。

## 3. 总目标

实现 6 个能力：

1. Bundle 分析
   - 明确当前 chunk 组成。
   - 找出 Element Plus、markdown-it、highlight.js、图标库、页面组件的包体占比。

2. Chunk 治理
   - Vendor 拆包。
   - UI vendor 拆包。
   - Markdown / code highlight 拆包。
   - Charts / heavy feature 拆包（如果存在）。

3. Lazy Loading
   - 路由懒加载保持有效。
   - 重型组件按需加载。
   - MarkdownRenderer 在需要时加载。
   - Drawer / Dialog 内的重内容尽量延迟加载。

4. Highlight.js 优化
   - 只注册常用语言。
   - 避免引入 full build。
   - 确认 build 产物中没有不必要语言包。

5. 性能预算
   - 定义 chunk size 预算。
   - 定义首屏加载预算。
   - 定义 release 阻塞 / warning 条件。

6. 质量门
   - typecheck 通过。
   - build 通过。
   - E2E 13/13 通过。
   - 生成性能验证报告。

## 4. 执行前必须阅读

执行前先阅读：

```text
frontend/vite.config.ts
frontend/package.json
frontend/src/app/router/index.ts
frontend/src/shared/components/MarkdownRenderer.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/task/components/AgentExecutionDrawer.vue
frontend/src/modules/knowledge/components/ChunkPreviewDrawer.vue
frontend/src/modules/repository/pages/RepositoryPanel.vue
frontend/src/modules/github/pages/PullRequestReviewPage.vue
frontend/src/modules/public/pages/PublicHomePage.vue
frontend/e2e/project-task-chat.spec.ts
frontend/e2e/auth.spec.ts
frontend/playwright.config.ts
docs/milestone-27-e2e-stability-release-quality.md
docs/e2e-stability-guide.md
scripts/run-frontend-checks.sh
scripts/release-checklist.sh
```

如果某些文件不存在，先说明实际情况，再选择最小可行替代方案。

## 5. 建议新增 / 修改文件

### 5.1 前端构建配置

重点检查 / 修改：

```text
frontend/vite.config.ts
frontend/package.json
```

可能新增：

```text
frontend/scripts/analyze-bundle.mjs
```

要求：

- 不破坏当前 Vite build。
- manualChunks 命名清晰。
- 不引入过度复杂的 build 插件。
- 如果引入 bundle visualizer，必须说明用途，并避免默认生成大文件提交。

### 5.2 组件懒加载

可能修改：

```text
frontend/src/shared/components/MarkdownRenderer.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/task/components/AgentExecutionDrawer.vue
frontend/src/modules/knowledge/components/ChunkPreviewDrawer.vue
frontend/src/modules/repository/pages/RepositoryPanel.vue
frontend/src/modules/github/pages/PullRequestReviewPage.vue
```

要求：

- 只针对重型渲染组件做懒加载。
- 不改变 UI 行为。
- 加载中状态必须自然，不闪烁。
- 不破坏 E2E selectors。

### 5.3 文档

新增：

```text
docs/frontend-performance-budget.md
docs/bundle-analysis-report.md
docs/milestone-28-validation-report-template.md
```

修改：

```text
docs/testing-strategy.md
docs/release-qa-report.md
README.md
```

### 5.4 脚本

可选修改：

```text
scripts/run-frontend-checks.sh
scripts/release-checklist.sh
```

可选新增：

```text
scripts/frontend-bundle-check.sh
```

要求：

- 输出 chunk size summary。
- 大于预算时 WARN 或 FAIL。
- 初期可设为 WARN，避免阻塞已有发布。

## 6. Bundle 分析要求

必须执行：

```bash
cd frontend
npm run build
```

记录：

- 总 JS 体积。
- 最大 chunk。
- Element Plus chunk。
- Markdown / highlight chunk。
- Public page chunk。
- Console page chunk。

建议输出到：

```text
docs/bundle-analysis-report.md
```

报告至少包含：

```text
Before
- largest chunk:
- markdown chunk:
- element plus chunk:
- total assets:

After
- largest chunk:
- markdown chunk:
- element plus chunk:
- total assets:

Changes
- what moved into separate chunks
- what was lazy loaded
- what remains large
```

## 7. Vite manualChunks 建议

可以考虑：

```ts
manualChunks(id) {
  if (id.includes('node_modules')) {
    if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
      return 'vendor-vue'
    }
    if (id.includes('element-plus')) {
      return 'vendor-element'
    }
    if (id.includes('markdown-it') || id.includes('highlight.js')) {
      return 'vendor-markdown'
    }
    if (id.includes('@element-plus/icons-vue') || id.includes('lucide')) {
      return 'vendor-icons'
    }
    return 'vendor'
  }
}
```

注意：

- 不要过度拆成几十个碎片。
- 不要造成循环依赖或运行时报错。
- build 后必须浏览器验证。

## 8. MarkdownRenderer 优化要求

当前 Markdown / highlight 是大 chunk 来源之一。

要求：

1. 确认是否使用 `highlight.js/lib/core`。
2. 只注册常用语言：
   - javascript
   - typescript
   - java
   - json
   - bash
   - sql
   - markdown
   - yaml
   - xml
   - css
3. 不引入 highlight.js 全量语言包。
4. 对 MarkdownRenderer 使用异步加载或确保其已单独 chunk。
5. Chat / Task artifacts / PR diff / chunk preview 中 Markdown 渲染仍正常。

## 9. Lazy Loading 建议

### 9.1 路由级

确认路由已经使用动态 import：

```ts
component: () => import('@/modules/.../Page.vue')
```

如果发现静态 import 页面组件，改为动态 import。

### 9.2 组件级

可以使用：

```ts
const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))
```

适用位置：

- Task Artifact。
- Agent Execution Drawer。
- Repository Diff Drawer。
- PR Review Diff。
- Knowledge Chunk Preview。

不适用：

- 高频首屏基础组件。
- 小组件。

## 10. 性能预算建议

新增：

```text
docs/frontend-performance-budget.md
```

建议预算：

```text
Initial route JS warning: > 500 KB gzip
Single chunk warning: > 500 KB raw
Single chunk fail: > 1000 KB raw, except vendor-element with documented reason
Markdown chunk warning: > 250 KB raw
Public page chunk warning: > 100 KB raw
E2E must pass before release
```

初期建议：

- 大 chunk 先 WARN，不直接 FAIL。
- release checklist 中记录 warning。
- 后续 Milestone 再逐步收紧。

## 11. 脚本要求

可选新增：

```text
scripts/frontend-bundle-check.sh
```

功能：

- 运行或读取 `frontend/dist/assets`。
- 输出最大 JS chunk。
- 输出超过预算的 WARN。
- 不依赖 macOS-only 命令。
- 不打印无关噪音。

示例输出：

```text
[PASS] dist exists
[INFO] Largest JS chunk: vendor-element-xxx.js 420 KB
[WARN] markdown chunk exceeds 250 KB raw
[PASS] Public page chunk under 100 KB raw
```

如果新增脚本，必须：

```bash
bash -n scripts/frontend-bundle-check.sh
```

## 12. 验证要求

完成后必须执行：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果 Milestone 27 已修复稳定性，建议额外执行第二遍：

```bash
npm run test:e2e -- --workers=1
```

必须执行文档检查：

```bash
test -f docs/frontend-performance-budget.md
test -f docs/bundle-analysis-report.md
test -f docs/milestone-28-validation-report-template.md
```

如果新增脚本：

```bash
bash -n scripts/frontend-bundle-check.sh
bash scripts/frontend-bundle-check.sh
```

## 13. 浏览器人工验证

如果前端 dev server 可用，手动验证：

1. `/public` 正常加载。
2. `/login` 正常。
3. 登录后 `/dashboard` 正常。
4. `/projects` 正常。
5. Project Detail 6 个 tabs 正常。
6. Chat SSE 正常。
7. Markdown message 渲染正常。
8. Task Detail artifact markdown 正常。
9. Repository diff / PR review diff 正常。
10. Knowledge chunk preview 正常。
11. Model Gateway 页面正常。
12. Observability 页面正常。

## 14. 完成后输出格式

完成后必须按以下格式输出：

```text
Milestone 28 完成报告

1. 新增/修改文件清单
2. Bundle 分析结果 Before / After
3. manualChunks / 拆包策略说明
4. MarkdownRenderer / highlight.js 优化说明
5. Lazy Loading 优化说明
6. 性能预算与脚本说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 29
```

## 15. 不做事项

本阶段明确不做：

- 不改后端。
- 不改业务逻辑。
- 不重写 UI。
- 不做 SSR / SSG。
- 不引入 Nuxt。
- 不引入外部监控平台。
- 不做完整 Lighthouse 自动化 CI。
- 不强行把所有 warning 清零。
- 不删除 Markdown / syntax highlight 功能。

## 16. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 28。

文档路径：
docs/milestone-28-frontend-performance-bundle-governance.md

执行要求：
1. 先完整阅读该文档，再检查 frontend/vite.config.ts、package.json、router、MarkdownRenderer、Chat/Task/Knowledge/Repository/GitHub 等使用 Markdown 或重型组件的页面。
2. 本阶段目标是前端性能优化与包体治理，不是增加业务功能。
3. 不要改后端代码，不要改 API 契约，不要改认证逻辑，不要重写前端页面。
4. 不要破坏 Chat SSE、Markdown 渲染、/public 公开页、控制台主流程。
5. 不要通过隐藏 build warning 伪装优化。
6. 不要删除功能来降低包体。
7. 优先做 bundle 分析、manualChunks、MarkdownRenderer / highlight.js 优化、重型组件懒加载和性能预算文档。
8. 所有改动必须通过 typecheck、build、E2E。

需要实现：
1. 运行 npm run build，记录当前 largest chunk、markdown chunk、element plus chunk、public page chunk 等数据。
2. 优化 frontend/vite.config.ts 的 build.rollupOptions.output.manualChunks。
3. 确认 MarkdownRenderer 使用 highlight.js/core 和按需语言注册，避免 full build。
4. 对 MarkdownRenderer 或使用 Markdown 的重型区域做合理异步加载。
5. 检查路由组件是否保持动态 import。
6. 新增 docs/frontend-performance-budget.md。
7. 新增 docs/bundle-analysis-report.md，记录 Before / After。
8. 新增 docs/milestone-28-validation-report-template.md。
9. 可选新增 scripts/frontend-bundle-check.sh，并在 release-checklist 中以 WARN 方式接入。
10. 更新 docs/testing-strategy.md 或 README.md，补充前端性能质量门说明。

完成后必须执行：
1. cd frontend && npm run typecheck
2. cd frontend && npm run build
3. cd frontend && npm run test:e2e -- --workers=1
4. 建议再次执行 cd frontend && npm run test:e2e -- --workers=1
5. test -f docs/frontend-performance-budget.md
6. test -f docs/bundle-analysis-report.md
7. test -f docs/milestone-28-validation-report-template.md
8. 如果新增脚本：bash -n scripts/frontend-bundle-check.sh && bash scripts/frontend-bundle-check.sh

如果前端 dev server 可用，请手动验证：
1. /public 正常加载
2. /login 正常
3. 登录后 dashboard/projects 正常
4. Chat SSE 正常
5. Markdown message / artifact / diff / chunk preview 渲染正常
6. Model Gateway / GitHub / Observability 页面正常

完成后按以下格式输出：
1. 新增/修改文件清单
2. Bundle 分析结果 Before / After
3. manualChunks / 拆包策略说明
4. MarkdownRenderer / highlight.js 优化说明
5. Lazy Loading 优化说明
6. 性能预算与脚本说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 29

现在开始实现，不要只给计划。
```
