# Milestone 25: 试用反馈闭环与产品化迭代管理

## 1. 背景

当前项目已经完成：

- Milestone 19：Release Demo 与 QA 验收。
- Milestone 20A / 20B：前端视觉升级和一致性 Polish。
- Milestone 21：真实部署 / 云端演示环境。
- Milestone 22：真实模型与 GitHub OAuth 生产联调准备。
- Milestone 23：生产监控、告警与安全加固。
- Milestone 24：产品化 Demo 与真实用户试用准备。

现在系统已经具备：

- 可运行的本地 / Docker / 生产单机演示环境。
- 可初始化的 Demo 数据。
- 可讲解的 5 分钟 / 15 分钟 Demo walkthrough。
- 可收集反馈的用户试用模板。
- 可执行的 demo smoke test 和生产健康检查脚本。

Milestone 25 的目标不是继续增加大功能，而是建立“真实用户试用 → 反馈收集 → 问题分级 → 迭代排期 → 发布说明 → 再验证”的产品化迭代闭环。

> 验收目标：每一次用户试用后，都能把反馈稳定沉淀为可追踪、可排序、可执行、可验证的工程任务，并能形成下一轮版本计划。

## 2. 严格边界

执行本阶段必须遵守：

1. 不重写前端工程。
2. 不更换技术栈。
3. 不改核心业务逻辑。
4. 不做大型新功能。
5. 不接新的外部服务。
6. 不引入 Jira、Linear、Notion、Sentry、PostHog 等强依赖。
7. 不要求用户必须使用某个第三方产品才能完成反馈闭环。
8. 不提交真实用户隐私信息。
9. 不提交用户访谈录音、截图、敏感业务数据。
10. 不把反馈入口做成复杂客服系统。
11. 不破坏已有 Demo、E2E、CI、Docker、生产脚本。
12. 不做 Git 写操作，除非用户明确要求。

允许做：

- 文档体系补齐。
- Issue / PR 模板。
- Changelog / release notes 模板。
- Roadmap 文档。
- 反馈 triage 流程。
- 试用记录模板。
- 小范围前端反馈入口或文案引导。
- 小范围脚本辅助整理报告。

## 3. 总目标

实现 6 个能力：

1. 用户反馈收集标准化
   - 明确反馈分类。
   - 明确反馈字段。
   - 明确截图 / 录屏 / traceId / 环境信息要求。
   - 明确哪些信息不能收集。

2. 反馈 triage 流程
   - Bug / UX / Product / Model Quality / Deployment / Security 分类。
   - P0 / P1 / P2 / P3 优先级规则。
   - 接收、复现、归因、排期、验收流程。

3. GitHub 协作模板
   - Bug report issue template。
   - Feature request issue template。
   - User trial feedback issue template。
   - PR template。
   - Release checklist issue template。

4. 产品 Roadmap
   - v1.0：内部试用版。
   - v1.1：真实模型与 GitHub OAuth 试点版。
   - v1.2：团队协作增强版。
   - v2.0：商业化 / 多租户 / 生产平台版。

5. Release Notes / Changelog
   - 建立 `CHANGELOG.md`。
   - 建立 release note 模板。
   - 明确每次发版必须包含新增、修复、已知限制、升级步骤、回滚说明。

6. Alpha / Beta 试用验收
   - Alpha 用户选择标准。
   - Beta 用户选择标准。
   - 试用周期。
   - 通过 / 阻塞标准。
   - 下一轮迭代输入。

## 4. 执行前必须阅读

执行前先完整阅读：

```text
README.md
docs/demo-walkthrough.md
docs/user-feedback-template.md
docs/demo-acceptance-checklist.md
docs/release-qa-report.md
docs/testing-strategy.md
docs/frontend-smoke-test-plan.md
docs/production-observability-runbook.md
docs/incident-response-runbook.md
docs/production-security-hardening-checklist.md
docs/milestone-24-validation-report-template.md
```

同时检查是否已有：

```text
.github/ISSUE_TEMPLATE/
.github/pull_request_template.md
CHANGELOG.md
docs/roadmap.md
docs/release-notes-template.md
```

如果已存在，优先增量修改，不要覆盖已有内容。

## 5. 建议新增 / 修改文件

### 5.1 GitHub 模板

新增：

```text
.github/ISSUE_TEMPLATE/bug_report.yml
.github/ISSUE_TEMPLATE/feature_request.yml
.github/ISSUE_TEMPLATE/user_trial_feedback.yml
.github/ISSUE_TEMPLATE/release_checklist.yml
.github/pull_request_template.md
```

要求：

- 使用 GitHub Issue Forms YAML。
- 字段清晰、可筛选。
- 不要求用户填写敏感信息。
- 引导用户提供 traceId、环境、复现步骤、截图链接。
- Security / secret leak 明确要求不要公开贴密钥。

### 5.2 产品与发布文档

新增：

```text
CHANGELOG.md
docs/roadmap.md
docs/release-notes-template.md
docs/user-trial-triage-guide.md
docs/alpha-beta-trial-plan.md
docs/product-feedback-taxonomy.md
docs/milestone-25-validation-report-template.md
```

修改：

```text
README.md
docs/user-feedback-template.md
docs/demo-walkthrough.md
docs/demo-acceptance-checklist.md
```

### 5.3 脚本可选

可选新增：

```text
scripts/collect-trial-report.sh
scripts/release-checklist.sh
```

限制：

- 不依赖外部 SaaS。
- 不上传文件。
- 不读取 `.env.production` 中的敏感内容。
- 不打印 token、password、API Key。

## 6. 反馈分类体系

新增：

```text
docs/product-feedback-taxonomy.md
```

必须包含以下分类：

### 6.1 Bug

定义：功能与预期不一致，或导致错误、崩溃、数据异常。

子类：

- Backend API。
- Frontend UI。
- Auth / Permission。
- Chat SSE。
- RAG / Knowledge。
- Task / Agent。
- Model Gateway。
- GitHub / PR Review。
- Deployment / Docker。
- Observability / Audit。

### 6.2 UX / Usability

定义：功能能用，但用户难以理解、路径过长、反馈不清晰。

子类：

- Navigation。
- Empty / Loading / Error state。
- Form / Dialog / Drawer。
- Table / Filter / Pagination。
- Visual hierarchy。
- Onboarding。

### 6.3 Product Value

定义：用户认为功能价值高 / 低、场景不匹配、缺少关键能力。

子类：

- Developer workflow。
- Team collaboration。
- AI code review。
- RAG knowledge management。
- Project management。
- Model provider management。

### 6.4 Model Quality

定义：模型输出质量、引用质量、成本、延迟、fallback 体验问题。

子类：

- Answer accuracy。
- Hallucination。
- Citation relevance。
- Prompt quality。
- Latency。
- Token cost。
- Provider failure。

### 6.5 Security / Compliance

定义：权限、密钥、日志、审计、数据暴露风险。

子类：

- Auth bypass。
- Token leakage。
- Secret logging。
- Over-permission。
- Audit missing。
- Unsafe CORS。

### 6.6 Deployment / Operations

定义：安装、启动、部署、监控、备份、升级相关问题。

子类：

- Docker。
- Nginx。
- MySQL。
- Environment variables。
- CI/CD。
- Health check。
- Backup / restore。

## 7. 优先级规则

新增：

```text
docs/user-trial-triage-guide.md
```

必须定义：

### P0

立即处理。

标准：

- 数据泄露。
- 密钥泄露。
- 登录绕过。
- 生产服务完全不可用。
- 数据破坏。
- 付费模型失控调用。

响应：

- 立即停止演示 / 试用。
- 保留日志与 traceId。
- 轮换密钥。
- 修复后重新做安全验收。

### P1

24 小时内处理或明确 workaround。

标准：

- 核心 Demo 链路中断。
- Chat SSE 大面积失败。
- Task Execute 无法完成。
- RAG 上传 / 搜索不可用。
- 登录频繁失败。
- GitHub OAuth 阻塞 PR Review 试用。

### P2

进入下一轮迭代。

标准：

- 可复现但有 workaround。
- UI 明显影响理解。
- 模型输出质量一般但不阻塞。
- 某些页面加载慢。
- 文档不清楚。

### P3

记录，批量处理。

标准：

- 文案问题。
- 小视觉问题。
- 低频场景。
- 长期增强建议。

## 8. GitHub Issue 模板要求

### 8.1 bug_report.yml

字段：

- Summary。
- Severity。
- Area。
- Environment。
- Steps to reproduce。
- Expected behavior。
- Actual behavior。
- Trace ID。
- Screenshot / recording link。
- Logs excerpt。
- Regression?。
- Sensitive data confirmation。

### 8.2 feature_request.yml

字段：

- Problem。
- Target user。
- Workflow。
- Proposed solution。
- Alternatives considered。
- Business value。
- Acceptance criteria。
- Non-goals。

### 8.3 user_trial_feedback.yml

字段：

- Trial date。
- User role。
- Scenario。
- Completed path。
- Blocked step。
- Most valuable feature。
- Most confusing part。
- UI rating。
- Model quality rating。
- Performance rating。
- Continue using?。
- Follow-up notes。

### 8.4 release_checklist.yml

字段：

- Release version。
- Scope。
- Backend checks。
- Frontend checks。
- E2E checks。
- Docker checks。
- Security checks。
- Known limitations。
- Rollback plan。

## 9. PR 模板要求

新增：

```text
.github/pull_request_template.md
```

必须包含：

- Summary。
- Type of change。
- User impact。
- Screenshots / recordings。
- Backend verification。
- Frontend verification。
- Security / secrets check。
- Migration / rollback notes。
- Known limitations。
- Linked issue。

## 10. Roadmap 要求

新增：

```text
docs/roadmap.md
```

必须包含：

### v1.0 Internal Alpha

目标：

- 完成内部试用。
- 5-10 个用户。
- 重点验证 Demo 主链路和 UI 可理解性。

必须包含：

- Auth。
- Project。
- Knowledge / RAG。
- Chat SSE。
- Task / Agent Execute。
- Model Gateway Mock / Real Provider basics。
- Observability。

### v1.1 External Beta

目标：

- 真实模型配置。
- GitHub OAuth / PR Review 试点。
- 20-30 个用户。

重点：

- 模型成本与延迟。
- PR Review 质量。
- 权限体验。
- 部署稳定性。

### v1.2 Team Collaboration

目标：

- 成员协作体验增强。
- 项目级配置完善。
- 审计和通知增强。

### v2.0 Production Platform

目标：

- 多租户。
- 组织 / 团队。
- 计费。
- SLA。
- 监控告警平台化。
- 合规与数据治理。

## 11. Release Notes / Changelog 要求

新增：

```text
CHANGELOG.md
docs/release-notes-template.md
```

格式建议：

```text
## [Unreleased]

### Added
### Changed
### Fixed
### Security
### Documentation
### Known Limitations
```

Release notes 必须包含：

- Version。
- Date。
- Audience。
- Highlights。
- Breaking changes。
- Upgrade steps。
- Verification results。
- Rollback notes。
- Known limitations。

## 12. Alpha / Beta 试用计划

新增：

```text
docs/alpha-beta-trial-plan.md
```

必须包含：

### Alpha

- 目标用户：内部开发者、技术负责人、产品同学。
- 人数：5-10。
- 周期：1-2 周。
- 目标：验证主链路、UI、Demo 文档。
- 通过标准：
  - 80% 用户能独立完成 Demo 主链路。
  - 无 P0 / 未解决 P1。
  - P2 有明确计划。

### Beta

- 目标用户：外部早期用户或真实业务团队。
- 人数：20-30。
- 周期：3-4 周。
- 目标：验证真实模型、GitHub OAuth、PR Review、部署稳定性。
- 通过标准：
  - 至少 3 个真实场景完成端到端试用。
  - 模型成本可解释。
  - GitHub OAuth / PR Review 无高危安全问题。
  - 用户愿意继续使用或二次试用。

## 13. 试用反馈流程

必须在 `docs/user-trial-triage-guide.md` 中定义：

1. 收集
   - 用户反馈模板。
   - Issue template。
   - 演示者记录。

2. 去敏
   - 移除 token、API Key、业务敏感内容。
   - 截图检查。
   - 日志脱敏。

3. 分类
   - 使用 feedback taxonomy。

4. 定级
   - 使用 P0-P3。

5. 复现
   - 环境。
   - 账号。
   - 数据。
   - 步骤。
   - traceId。

6. 排期
   - 当前迭代。
   - 下一迭代。
   - Backlog。

7. 验收
   - 自动化测试。
   - 手动验证。
   - 用户确认。

8. 关闭
   - Release notes。
   - 关联 issue。
   - 记录已知限制。

## 14. 可选脚本

### 14.1 collect-trial-report.sh

可选新增：

```text
scripts/collect-trial-report.sh
```

功能：

- 生成试用报告目录。
- 复制反馈模板。
- 收集 git commit、build 状态、smoke test 输出。
- 不收集敏感日志。

输出示例：

```text
trial-reports/2026-05-15-demo-session/
  feedback.md
  acceptance-checklist.md
  smoke-test-output.txt
  environment-summary.txt
```

### 14.2 release-checklist.sh

可选新增：

```text
scripts/release-checklist.sh
```

功能：

- 检查 README / CHANGELOG / release notes 是否存在。
- 检查 `.env` 未被追踪。
- 检查 backend / frontend 基础命令是否通过。
- 输出 release readiness summary。

## 15. 验证要求

完成后必须执行：

```bash
test -f .github/ISSUE_TEMPLATE/bug_report.yml
test -f .github/ISSUE_TEMPLATE/feature_request.yml
test -f .github/ISSUE_TEMPLATE/user_trial_feedback.yml
test -f .github/ISSUE_TEMPLATE/release_checklist.yml
test -f .github/pull_request_template.md
test -f CHANGELOG.md
test -f docs/roadmap.md
test -f docs/release-notes-template.md
test -f docs/user-trial-triage-guide.md
test -f docs/alpha-beta-trial-plan.md
test -f docs/product-feedback-taxonomy.md
test -f docs/milestone-25-validation-report-template.md
```

如果新增脚本：

```bash
bash -n scripts/collect-trial-report.sh
bash -n scripts/release-checklist.sh
```

如果改了前端代码：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果改了后端代码：

```bash
cd backend
mvn test
```

如果只改文档和 GitHub 模板，可以不跑完整前后端测试，但必须说明未运行原因。

## 16. 完成后输出格式

完成后必须按以下格式输出：

```text
Milestone 25 完成报告

1. 新增/修改文件清单
2. Feedback Taxonomy 说明
3. Triage 流程说明
4. GitHub Issue / PR 模板说明
5. Roadmap 说明
6. Release Notes / Changelog 说明
7. Alpha / Beta 试用计划说明
8. 可选脚本说明
9. 验证结果
10. 已知限制
11. 是否可以进入 Milestone 26
```

## 17. 不做事项

本阶段明确不做：

- 不接 Jira / Linear / Notion。
- 不实现后台客服系统。
- 不实现用户行为埋点平台。
- 不实现 NPS 系统。
- 不实现自动邮件发送。
- 不实现在线表单服务。
- 不新增数据库反馈表，除非用户明确要求。
- 不改变产品主链路。
- 不修复所有历史 E2E flaky，只记录为已知限制。

## 18. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 25。

文档路径：
docs/milestone-25-feedback-loop-product-iteration.md

执行要求：
1. 先完整阅读该文档，再检查当前项目结构。
2. 本阶段目标是建立真实用户试用后的反馈闭环和产品迭代管理机制，不是继续增加大功能。
3. 不要重写前端工程，不要更换技术栈，不要更换 UI 框架。
4. 不要改 Auth、Project、Member、Repository、Task、Agent、Chat、RAG、Model Gateway、GitHub、Observability 已验证通过的核心逻辑。
5. 不接新的外部 SaaS，不依赖 Jira、Linear、Notion、Sentry、PostHog。
6. 不提交真实用户隐私信息、截图、录音、业务数据、token、API Key、OAuth Secret。
7. 不做 Git 写操作，除非用户明确要求。
8. 已有文档或模板如果存在，优先增量修改，不要覆盖用户已有内容。
9. 所有新增文档要能被试用组织者、开发者、维护者直接使用。
10. Issue / PR 模板必须引导用户不要公开粘贴密钥或敏感信息。
11. 如果只改文档和 GitHub 模板，可以不跑完整前后端测试，但必须执行文件存在性检查，并说明未运行测试原因。

需要实现：
1. 新增 .github/ISSUE_TEMPLATE/bug_report.yml。
2. 新增 .github/ISSUE_TEMPLATE/feature_request.yml。
3. 新增 .github/ISSUE_TEMPLATE/user_trial_feedback.yml。
4. 新增 .github/ISSUE_TEMPLATE/release_checklist.yml。
5. 新增 .github/pull_request_template.md。
6. 新增 CHANGELOG.md。
7. 新增 docs/roadmap.md。
8. 新增 docs/release-notes-template.md。
9. 新增 docs/user-trial-triage-guide.md。
10. 新增 docs/alpha-beta-trial-plan.md。
11. 新增 docs/product-feedback-taxonomy.md。
12. 新增 docs/milestone-25-validation-report-template.md。
13. 更新 README.md，加入反馈、Roadmap、Release Notes、Alpha/Beta 试用文档入口。
14. 更新 docs/user-feedback-template.md 和 docs/demo-walkthrough.md，补充反馈提交和 triage 链接。
15. 可选新增 scripts/collect-trial-report.sh 和 scripts/release-checklist.sh；如果新增，必须 set -euo pipefail 且不收集敏感信息。

完成后必须执行：
1. test -f .github/ISSUE_TEMPLATE/bug_report.yml
2. test -f .github/ISSUE_TEMPLATE/feature_request.yml
3. test -f .github/ISSUE_TEMPLATE/user_trial_feedback.yml
4. test -f .github/ISSUE_TEMPLATE/release_checklist.yml
5. test -f .github/pull_request_template.md
6. test -f CHANGELOG.md
7. test -f docs/roadmap.md
8. test -f docs/release-notes-template.md
9. test -f docs/user-trial-triage-guide.md
10. test -f docs/alpha-beta-trial-plan.md
11. test -f docs/product-feedback-taxonomy.md
12. test -f docs/milestone-25-validation-report-template.md
13. 如果新增脚本：bash -n scripts/collect-trial-report.sh 和 bash -n scripts/release-checklist.sh

完成后按以下格式输出：
1. 新增/修改文件清单
2. Feedback Taxonomy 说明
3. Triage 流程说明
4. GitHub Issue / PR 模板说明
5. Roadmap 说明
6. Release Notes / Changelog 说明
7. Alpha / Beta 试用计划说明
8. 可选脚本说明
9. 验证结果
10. 已知限制
11. 是否可以进入 Milestone 26

现在开始实现，不要只给计划。
```
