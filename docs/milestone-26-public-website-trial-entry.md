# Milestone 26: 产品官网 / 对外展示与试用入口

## 1. 背景

当前项目已经完成：

- Milestone 24：产品化 Demo 与真实用户试用准备。
- Milestone 25：试用反馈闭环与产品化迭代管理。

系统已经具备：

- 可运行的控制台产品。
- Demo 数据初始化与 smoke test。
- 用户试用 walkthrough。
- 反馈模板、Issue 模板、Roadmap、Changelog。
- 生产部署、监控、安全 Runbook。

下一步需要补齐的是“对外展示入口”：

> 让一个不了解项目的人打开页面后，能在 1 分钟内知道它是什么、解决什么问题、适合谁、怎么试用、从哪里进入 Demo。

Milestone 26 的目标是建设一个不影响现有控制台的产品展示入口，用于演示、试用邀请、GitHub 项目介绍和未来真实部署。

## 2. 严格边界

执行本阶段必须遵守：

1. 不重写现有前端控制台。
2. 不更换技术栈。
3. 不更换 UI 框架。
4. 不改后端业务逻辑。
5. 不改 Auth / Project / Task / Chat / RAG / Model Gateway / GitHub / Observability 的既有接口。
6. 不把控制台首页改成营销页。
7. 不破坏 `/login`、`/dashboard`、`/projects` 等已有路由。
8. 不引入真实注册、支付、订阅、邮件系统。
9. 不接外部表单服务、CRM、埋点平台。
10. 不提交真实用户信息、密钥、API Key、OAuth Secret。
11. 不做复杂 SEO / SSR / 多语言。
12. 不做移动端深度适配，但页面必须在常见桌面和基础移动宽度下可读。

允许做：

- 新增公开展示路由或静态页面。
- 新增产品官网组件。
- 新增演示截图占位区。
- 新增文档和 README 对外展示优化。
- 新增 Trial 入口说明。
- 新增 Trial request / feedback 的本地文档链接。
- 小范围调整路由守卫，确保公开页面不需要登录。

## 3. 总目标

实现 5 个能力：

1. Public Website / Landing Entry
   - 公开访问，不需要登录。
   - 清楚表达产品定位。
   - 提供 Demo / Login / GitHub / Docs 入口。

2. Product Narrative
   - 谁使用。
   - 解决什么问题。
   - 核心价值。
   - 当前能力边界。

3. Feature Showcase
   - AI Coding Console。
   - Project / Task / Agent。
   - Chat SSE + RAG references。
   - Model Gateway。
   - GitHub PR Review。
   - Observability / Audit。

4. Trial Entry
   - 如何启动本地 Demo。
   - 如何使用 Demo 账号。
   - 如何初始化 Demo 数据。
   - 如何提交反馈。

5. External Readiness
   - README 对外说明更清晰。
   - Demo walkthrough / feedback / roadmap 可被外部用户找到。
   - 页面不误导用户以为已有商业生产服务。

## 4. 执行前必须阅读

执行前先阅读：

```text
README.md
docs/demo-walkthrough.md
docs/user-feedback-template.md
docs/roadmap.md
docs/release-notes-template.md
docs/product-feedback-taxonomy.md
docs/milestone-24-productized-demo-user-trial.md
docs/milestone-25-feedback-loop-product-iteration.md
frontend/src/app/router/index.ts
frontend/src/app/guards/authGuard.ts
frontend/src/App.vue
frontend/src/app/layouts/AuthLayout.vue
frontend/src/app/layouts/BasicLayout.vue
frontend/src/modules/auth/pages/LoginPage.vue
frontend/src/modules/dashboard/pages/DashboardPage.vue
frontend/src/shared/components/DynamicWorkspace.vue
frontend/src/shared/components/GlowButton.vue
frontend/src/shared/components/StatusPulse.vue
frontend/src/shared/components/DemoBadge.vue
```

如果某些文件不存在，先说明实际情况，再选择最小可行替代方案。

## 5. 建议新增 / 修改文件

### 5.1 前端页面

建议新增：

```text
frontend/src/modules/public/pages/PublicHomePage.vue
```

可选新增组件：

```text
frontend/src/modules/public/components/PublicHero.vue
frontend/src/modules/public/components/FeatureShowcase.vue
frontend/src/modules/public/components/ArchitecturePreview.vue
frontend/src/modules/public/components/TrialEntryPanel.vue
frontend/src/modules/public/components/PublicFaq.vue
```

修改：

```text
frontend/src/app/router/index.ts
frontend/src/app/guards/authGuard.ts
```

要求：

- 新增公开路由建议为 `/public` 或 `/home`。
- 根路径 `/` 当前若已重定向 dashboard，可以改为：
  - 未登录 → public home。
  - 已登录 → dashboard。
- `/login` 必须保持可用。
- 登录后控制台体验不变。
- 不要让公开页套 `BasicLayout`。
- 公开页可以使用独立页面布局，但应继承当前 Dark Tech Console 视觉语言。

### 5.2 文档

新增：

```text
docs/public-website-content.md
docs/trial-entry-guide.md
docs/milestone-26-validation-report-template.md
```

修改：

```text
README.md
frontend/README.md
docs/demo-walkthrough.md
```

### 5.3 截图 / 图片策略

本阶段不强制生成真实图片。允许使用：

- CSS / HTML 构建的产品预览。
- 代码风格 UI mock panel。
- Dashboard / Chat / Task / RAG 的文字型 preview。
- 后续再替换为真实截图。

不要：

- 使用外部 stock 图片。
- 使用模糊、无关、无法说明产品的背景图。
- 把 Hero 做成纯装饰渐变。

## 6. Public Home 页面内容要求

### 6.1 Hero

Hero 必须在首屏明确表达：

```text
AI Coding Platform
AI-native workspace for projects, agents, knowledge, and code review.
```

必须包含：

- 产品名。
- 一句话定位。
- Primary CTA：Enter Demo / Open Console。
- Secondary CTA：Read Walkthrough / View GitHub。
- Demo / Mock 状态说明。

不要：

- 使用空泛口号。
- 只写 “next generation platform” 这类无信息文案。
- 把所有信息塞进卡片。

### 6.2 Problem / Value

说明当前解决的问题：

- AI 工具分散。
- 项目上下文难以沉淀。
- Chat 与任务执行脱节。
- RAG、Agent、GitHub Review、模型调用日志缺少统一闭环。

价值表达：

- 把项目、知识库、Chat、Agent Task、PR Review 放进同一工作台。
- 可追踪模型请求、审计日志、任务状态和产物。
- 支持 Mock-first 演示，也支持真实模型接入。

### 6.3 Feature Showcase

至少展示 6 个能力：

1. Project Workspace
2. Knowledge Base / RAG
3. Chat SSE + References
4. Agent Task Execution
5. Model Gateway
6. GitHub PR Review
7. Observability / Audit

每个能力包含：

- 标题。
- 一句话说明。
- 当前状态：Ready / Mock / Optional Config / Planned。

### 6.4 Architecture Preview

使用简洁结构展示：

```text
Frontend Console
  → Spring Boot API
  → Auth / Project / Task / Chat / RAG
  → Model Gateway
  → GitHub OAuth / PR Review
  → Observability / Audit
```

可以用前端组件、CSS 网格或 Mermaid 文档表达。

### 6.5 Trial Entry

必须说明：

```bash
cd backend && mvn spring-boot:run
cd frontend && npm run dev
bash scripts/demo-seed-data.sh
```

必须说明 Demo 登录：

```text
admin@example.com / Admin@123456
```

必须说明：

- 默认使用 Mock Provider。
- GitHub OAuth 可选。
- 真实模型需要配置 API Key。

### 6.6 FAQ

至少包含：

1. 这是生产可用产品吗？
2. 默认会调用真实大模型吗？
3. GitHub OAuth 必须配置吗？
4. 数据会上传到外部服务吗？
5. 如何启动 Demo？
6. 如何提交反馈？
7. 如何接入真实模型？
8. 当前最大限制是什么？

## 7. 路由与权限要求

必须保证：

- Public page 不需要登录。
- `/login` 不需要登录。
- 控制台页面仍需要登录。
- 已登录用户访问 public page 时可以点击进入 dashboard。
- 未登录用户访问 console protected route 时仍跳转 login。
- 401 逻辑不变。

建议规则：

```text
/public          public
/home            optional alias
/login           public
/dashboard       protected
/projects        protected
/agents          protected
/model-gateway   protected
/github          protected
/observability   protected
```

如果根路由 `/` 当前已有逻辑，不要粗暴覆盖。可以选择：

- `/` 指向 public home；
- 或 `/` 根据 token 判断 public home / dashboard；
- 但必须说明最终行为。

## 8. README 对外展示要求

README 需要更适合外部读者：

必须包含：

1. 一句话产品定位。
2. Core Capabilities。
3. Demo quick start。
4. Screens / Experience path。
5. Architecture overview。
6. Current status。
7. Roadmap link。
8. Feedback link。
9. Security note。

不要把 README 写成只有开发启动命令的文件。

## 9. 文档要求

### 9.1 public-website-content.md

新增：

```text
docs/public-website-content.md
```

包含：

- Hero 文案。
- Feature 文案。
- FAQ 文案。
- Trial CTA 文案。
- Mock / Real Provider 声明。

### 9.2 trial-entry-guide.md

新增：

```text
docs/trial-entry-guide.md
```

包含：

- 本地试用。
- Docker 试用。
- 生产演示环境试用。
- Demo 数据初始化。
- 常见错误。
- 反馈提交。

### 9.3 milestone-26-validation-report-template.md

新增：

```text
docs/milestone-26-validation-report-template.md
```

用于记录：

- 页面访问结果。
- 路由守卫结果。
- Login / Dashboard 跳转结果。
- CTA 链接结果。
- README 链接检查。
- Build / E2E 结果。

## 10. 验证要求

完成后必须执行：

```bash
test -f docs/public-website-content.md
test -f docs/trial-entry-guide.md
test -f docs/milestone-26-validation-report-template.md
```

如果改了前端代码，必须执行：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果只改 README / docs，不需要跑前端 build，但必须说明原因。

## 11. 浏览器人工验证

如果前端 dev server 可用，手动验证：

1. 未登录访问 `/` 或 `/public`，能看到 Public Home。
2. Public Home 首屏能看懂产品定位。
3. CTA “Enter Demo / Open Console” 可进入 login 或 dashboard。
4. CTA “Read Walkthrough” 链接或说明有效。
5. `/login` 正常。
6. 登录后进入 dashboard 正常。
7. 登录后控制台页面不受影响。
8. 未登录访问 `/projects` 仍被拦截到 login。
9. 移动窄屏基本可读。
10. 页面没有误导用户已默认接入真实模型。

## 12. 完成后输出格式

完成后按以下格式输出：

```text
Milestone 26 完成报告

1. 新增/修改文件清单
2. Public Home 页面实现
3. 路由与权限处理
4. Product Narrative / Feature Showcase 说明
5. Trial Entry / FAQ 说明
6. README / Docs 更新说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 27
```

## 13. 不做事项

本阶段明确不做：

- 不实现真实公开注册。
- 不实现试用申请表单后端。
- 不实现邮件通知。
- 不实现 SEO 深度优化。
- 不实现多语言。
- 不实现博客系统。
- 不实现 CMS。
- 不接入统计平台。
- 不接支付。
- 不改控制台主业务流程。

## 14. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 26。

文档路径：
docs/milestone-26-public-website-trial-entry.md

执行要求：
1. 先完整阅读该文档，再检查当前 frontend 路由、auth guard、layout 和 README/docs。
2. 本阶段目标是新增产品官网 / 对外展示与试用入口，不是重写控制台。
3. 不要更换技术栈，不要更换 UI 框架，不要改后端业务逻辑。
4. 不要破坏 /login、/dashboard、/projects 等既有控制台路由。
5. 不要把控制台首页改成营销页；公开展示页应该是独立 public route。
6. Public page 不需要登录，控制台页面仍必须登录。
7. 保持当前 Dark Tech Console / DynamicWorkspace / FloatingDock 的视觉方向，但 public page 不要套 BasicLayout。
8. 不要接外部表单、CRM、埋点、支付、邮件系统。
9. 不提交真实用户信息、API Key、OAuth Secret、.env 文件。
10. 如果只改前端和文档，按要求运行 frontend typecheck/build/e2e。

需要实现：
1. 新增 Public Home 页面，例如 frontend/src/modules/public/pages/PublicHomePage.vue。
2. 可按需新增 public components，例如 PublicHero、FeatureShowcase、ArchitecturePreview、TrialEntryPanel、PublicFaq。
3. 修改 frontend/src/app/router/index.ts，新增公开路由 /public 或 /home，并明确 / 的行为。
4. 修改 authGuard，确保 public route 和 /login 不需要登录，控制台路由仍需要登录。
5. Public Home 必须包含 Hero、Problem/Value、Feature Showcase、Architecture Preview、Trial Entry、FAQ。
6. CTA 至少包含 Open Console / Enter Demo、Read Walkthrough、View Roadmap 或 GitHub。
7. 明确标注默认 Mock Provider、GitHub OAuth 可选、真实模型需配置 API Key。
8. 新增 docs/public-website-content.md。
9. 新增 docs/trial-entry-guide.md。
10. 新增 docs/milestone-26-validation-report-template.md。
11. 更新 README.md，使其更适合外部读者：产品定位、核心能力、Demo quick start、架构、Roadmap、Feedback、安全说明。
12. 更新 frontend/README.md 或 docs/demo-walkthrough.md，补充 public entry 说明。

完成后必须执行：
1. test -f docs/public-website-content.md
2. test -f docs/trial-entry-guide.md
3. test -f docs/milestone-26-validation-report-template.md
4. cd frontend && npm run typecheck
5. cd frontend && npm run build
6. cd frontend && npm run test:e2e -- --workers=1

如果前端 dev server 可用，请手动验证：
1. 未登录访问 /public 可看到 Public Home。
2. /login 正常。
3. 未登录访问 /projects 仍跳转 login。
4. 登录后 dashboard 正常。
5. Public Home 的 CTA 链接可用。
6. 页面没有误导用户默认接入真实模型。

完成后按以下格式输出：
1. 新增/修改文件清单
2. Public Home 页面实现
3. 路由与权限处理
4. Product Narrative / Feature Showcase 说明
5. Trial Entry / FAQ 说明
6. README / Docs 更新说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 27

现在开始实现，不要只给计划。
```
