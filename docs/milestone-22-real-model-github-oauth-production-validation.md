# Milestone 22: 真实模型与 GitHub OAuth 生产联调

## 1. 背景

当前项目已经完成：

- Milestone 16：真实模型网关基础能力与生产级加固。
- Milestone 17：GitHub OAuth + Pull Request Review 只读闭环。
- Milestone 21：单机 Docker Compose 云端演示部署基础，包括生产 Compose、Nginx、CORS、prod profile、部署脚本和 Runbook。

现在系统已经具备部署到云服务器的基础能力，但仍需要对真实外部服务做生产联调：

1. 真实模型 Provider。
2. Chat SSE 真实流式输出。
3. Task Execute / Agent Orchestrator 使用真实模型。
4. GitHub OAuth 真实授权回调。
5. GitHub 仓库同步、PR Diff 拉取、AI Review 真实模型输出。

Milestone 22 的目标是：

> 在不提交任何真实密钥、不破坏 Mock fallback、不改变业务架构的前提下，补齐真实模型和 GitHub OAuth 的生产联调配置、脚本、文档、验收清单与必要的小修复，让部署环境可以通过环境变量完成真实外部服务验证。

## 2. 严格边界

执行本阶段必须遵守：

1. 不提交真实 API Key。
2. 不提交 GitHub Client Secret。
3. 不提交 `.env.production`。
4. 不把 token、API Key、OAuth code、refresh token 打到日志。
5. 不调用 GitHub 写接口。
6. 不自动 push、merge、approve、comment PR。
7. 不执行真实 Git 写操作。
8. 不绕过 Prompt Safety。
9. 不移除 Mock Provider 和 Mock fallback。
10. 不破坏本地 MOCK 模式测试。
11. 不改变 Auth、Project、Task、Chat、RAG、GitHub PR Review 的核心业务流程。
12. 不新增大型依赖。
13. 不改数据库结构，除非发现字段缺失导致生产联调无法完成，且必须说明原因。

## 3. 总目标

实现 5 个能力：

1. 真实模型 Provider 生产配置
   - OpenAI Compatible / Claude / DeepSeek / Qwen / Gemini 至少支持配置说明。
   - 至少选择 1-2 个 Provider 做真实端到端验收。
   - Connection Test 可验证真实模型。
   - Chat SSE 能使用真实模型流式输出。
   - Task Execute 能使用真实模型完成执行。

2. 模型安全与降级
   - API Key 不出现在响应、日志、审计、ModelRequestLog 明文中。
   - 失败时错误码清晰。
   - Rate limit / timeout / auth error 有明确提示。
   - Mock fallback 保留，并可通过配置开关控制。

3. 模型用量与成本
   - 真实模型调用产生 ModelRequestLog。
   - token usage / estimated cost 正常展示。
   - fallback_used / error_code / estimated_cost 字段正常。

4. GitHub OAuth 生产联调
   - 生成 GitHub OAuth App 配置文档。
   - 生产 callback URL 可配置。
   - 绑定 GitHub 账号。
   - 同步 repositories。
   - 读取 PR 列表、PR detail、changed files / patch。
   - 不调用 GitHub 写接口。

5. PR Review 真实模型验收
   - 选择一个测试仓库和测试 PR。
   - 拉取 patch。
   - 调用真实模型生成 review summary / findings。
   - 保存 review job 和 findings。
   - 前端展示 findings。

## 4. 需要检查的现有文件

执行前先阅读：

```text
docs/milestone-16-real-model-gateway-production-hardening.md
docs/milestone-17-github-oauth-pr-review.md
docs/milestone-21-cloud-demo-production-deployment.md
docs/production-deployment-runbook.md
.env.production.example
backend/src/main/resources/application.yml
backend/src/main/resources/application-prod.yml
backend/src/main/java/com/aicoding/platform/modelgateway/config/ModelGatewayProperties.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/DefaultModelGateway.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelConfigResolver.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelConnectionTestService.java
backend/src/main/java/com/aicoding/platform/modelgateway/provider/OpenAiCompatibleModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/provider/ClaudeModelProvider.java
backend/src/main/java/com/aicoding/platform/github/application/GithubProperties.java
backend/src/main/java/com/aicoding/platform/github/application/GithubClient.java
backend/src/main/java/com/aicoding/platform/github/application/GithubOAuthService.java
backend/src/main/java/com/aicoding/platform/github/application/PrReviewApplicationService.java
frontend/src/modules/model/pages/ModelConfigPage.vue
frontend/src/modules/github/pages/GithubIntegrationPage.vue
frontend/src/modules/github/pages/PullRequestReviewPage.vue
```

## 5. 建议新增 / 修改文件

### 5.1 文档

新增：

```text
docs/model-provider-production-setup.md
docs/github-oauth-production-setup.md
docs/milestone-22-validation-report-template.md
```

修改：

```text
docs/production-deployment-runbook.md
docs/deployment-guide.md
README.md
.env.production.example
```

### 5.2 脚本

新增：

```text
scripts/validate-model-provider.sh
scripts/validate-github-oauth-config.sh
scripts/prod-external-services-smoke-test.sh
```

脚本要求：

- 不打印密钥。
- 从环境变量读取配置。
- 失败时输出清晰原因。
- 可在本地和云服务器运行。
- 对没有配置真实 Provider 的情况给出 SKIP，而不是失败。

### 5.3 后端可选小修复

如检查发现生产联调缺少以下能力，可做小范围修复：

- GitHub callback URL 从环境变量读取不完整。
- Model Provider baseUrl / apiKey env 映射缺失。
- Connection Test 返回错误不清晰。
- SSE 流式错误未正确结束。
- GitHub OAuth 未配置时前端提示不清晰。
- Prompt Safety 误杀连接测试 prompt。

限制：

- 不允许重写 Model Gateway。
- 不允许重写 GitHub OAuth。
- 不允许改 PR Review 的只读原则。

## 6. 真实模型配置要求

### 6.1 环境变量标准

`.env.production.example` 应包含清晰占位：

```text
MODEL_GATEWAY_PROVIDER=MOCK
MODEL_GATEWAY_FALLBACK_ENABLED=true
MODEL_GATEWAY_TIMEOUT_MS=30000
MODEL_GATEWAY_MAX_RETRIES=2
MODEL_GATEWAY_PROMPT_SAFETY_ENABLED=true

OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini

CLAUDE_BASE_URL=https://api.anthropic.com
CLAUDE_API_KEY=
CLAUDE_MODEL=claude-3-5-sonnet-latest

DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=
DEEPSEEK_MODEL=deepseek-chat

QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_API_KEY=
QWEN_MODEL=qwen-plus

GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash
```

实际代码可使用已有命名，但文档必须明确：

- 每个 provider 需要什么变量。
- baseUrl 是否需要 `/v1`。
- modelName 推荐值。
- 是否支持 streaming。
- 是否支持 OpenAI Compatible。

### 6.2 Connection Test

必须验证：

1. MOCK provider connection test。
2. 至少一个真实 provider connection test。
3. API Key 错误时返回清晰 `UNAUTHORIZED` 或 provider auth error。
4. Base URL 错误时返回 network/timeout error。
5. Rate limit 时不崩溃。

### 6.3 Chat SSE 真实模型

必须验证：

1. Chat session 创建。
2. Send message。
3. SSE token 事件持续输出。
4. done event 正常。
5. Message status 从 `STREAMING` 变 `COMPLETED`。
6. 刷新页面后消息内容完整。
7. 关闭浏览器或切页时 abort 不导致后端异常刷屏。
8. Provider 失败时 fallback 到 Mock 或返回清晰错误，取决于配置。

### 6.4 Task Execute 真实模型

必须验证：

1. 创建 task。
2. `POST /api/tasks/{taskId}/execute`。
3. AgentExecution status `COMPLETED`。
4. Task status `COMPLETED`。
5. Artifact 生成。
6. ModelRequestLog 记录真实 provider / modelName。
7. Usage / cost panel 有数据。

## 7. GitHub OAuth 配置要求

### 7.1 GitHub OAuth App 设置文档

`docs/github-oauth-production-setup.md` 必须说明：

1. GitHub Developer Settings 入口。
2. OAuth App 创建步骤。
3. Homepage URL。
4. Authorization callback URL。
5. 本地 callback 示例：

```text
http://localhost:8080/api/github/oauth/callback
```

6. 生产 callback 示例：

```text
https://example.com/api/github/oauth/callback
```

7. Required scopes：

```text
read:user
user:email
repo
```

8. `repo` scope 的风险说明。
9. 如何切换 public repo only。
10. Secret 如何注入 `.env.production`。

### 7.2 GitHub 环境变量

`.env.production.example` 应包含：

```text
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
GITHUB_REDIRECT_URI=https://example.com/api/github/oauth/callback
GITHUB_OAUTH_SCOPES=read:user,user:email,repo
```

### 7.3 OAuth 验收

必须验证：

1. 未配置 GitHub OAuth 时系统正常启动。
2. 前端显示未配置或未绑定状态。
3. 配置后点击 authorize。
4. 跳转 GitHub 授权页。
5. callback 返回平台。
6. 绑定状态变为 bound。
7. token 不返回前端。
8. 解绑或重新绑定流程安全。

## 8. GitHub Repository / PR Review 验收

必须验证：

1. Sync repositories。
2. List repositories。
3. Select repository。
4. List PRs。
5. Load PR detail。
6. Load changed files / patch。
7. Create review job。
8. Execute review with real model。
9. Show summary。
10. Show findings。
11. Model JSON 解析失败时不崩溃。
12. Patch 超长时截断并提示。
13. 审计日志记录 GitHub OAuth / PR Review 操作。

明确禁止：

- 不向 GitHub 写评论。
- 不 approve PR。
- 不 request changes。
- 不 push。
- 不 merge。

## 9. 生产 Smoke Test 脚本

新增 `scripts/prod-external-services-smoke-test.sh`。

参数建议：

```bash
bash scripts/prod-external-services-smoke-test.sh https://example.com
```

行为：

1. 登录 admin。
2. 检查 `/api/auth/me`。
3. 检查 model provider options。
4. 如配置 `TEST_MODEL_PROVIDER`，调用 connection test。
5. 创建 chat session。
6. 发送 chat message。
7. 可选检查 SSE done。
8. 检查 GitHub OAuth status。
9. 如配置 `TEST_GITHUB_REPO_FULL_NAME` 和已绑定 GitHub，检查 repo / PR list。
10. 输出 PASS / SKIP / FAIL。

环境变量：

```text
TEST_ADMIN_EMAIL=admin@example.com
TEST_ADMIN_PASSWORD=Admin@123456
TEST_MODEL_PROVIDER=MOCK
TEST_MODEL_NAME=mock-agent-model
TEST_GITHUB_REPO_FULL_NAME=
TEST_GITHUB_PR_NUMBER=
```

要求：

- 没有真实 key 时 provider 测试 SKIP，不失败。
- 没有 GitHub binding 时 GitHub 测试 SKIP，不失败。
- 登录失败必须 FAIL。
- 基础 API 不通必须 FAIL。

## 10. 安全验收

必须检查：

1. `rg "sk-|ghp_|github_pat_|CLAUDE|OPENAI_API_KEY=.*[A-Za-z0-9]"` 不发现真实密钥。
2. 日志中 API Key 被 mask。
3. 前端响应不包含 apiKey 明文。
4. ModelRequestLog 不包含 apiKey。
5. GitHub token 不返回前端。
6. GitHub token 不进入 prompt。
7. PR Review prompt 不包含 OAuth token。
8. `.env.production` 不在 git status。

## 11. 自动化验证

必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如生产环境可用，额外执行：

```bash
bash scripts/prod-smoke-test.sh https://example.com
bash scripts/prod-external-services-smoke-test.sh https://example.com
```

如只在本地验证：

```bash
bash scripts/prod-external-services-smoke-test.sh http://localhost
```

## 12. 手动验收清单

### 12.1 Model Gateway

1. 打开 Model Gateway 页面。
2. MOCK Provider connection test 成功。
3. 真实 Provider connection test 成功。
4. 错误 API Key 显示清晰错误。
5. Chat 页面切到真实 Provider 后 SSE 正常。
6. Task execute 使用真实 Provider。
7. Model Usage / Cost 有真实记录。
8. fallback 开关符合预期。

### 12.2 GitHub

1. 打开 GitHub Integration 页面。
2. 未配置时提示清晰。
3. 配置后 authorize URL 正确。
4. GitHub 授权成功。
5. Binding 状态显示正确。
6. Sync repositories 成功。
7. PR Review 页面能选择仓库和 PR。
8. Diff / patch 展示可读。
9. Run AI Review 成功。
10. Findings 展示可读。

### 12.3 安全

1. 查看 backend logs，无密钥明文。
2. DevTools Network 响应无密钥明文。
3. Git status 无 `.env.production`。
4. Smoke test 不输出密钥。

## 13. 不要做的事

不要：

- 提交真实 `.env.production`。
- 提交真实 provider API Key。
- 提交真实 GitHub Client Secret。
- 修改 GitHub PR。
- 自动评论 PR。
- 将 PR patch 以外的私密 token 放进 prompt。
- 为了通过联调关闭认证或 Prompt Safety。
- 移除 Mock fallback。
- 引入新的模型 SDK 大依赖，优先使用已有 HTTP client。

## 14. 完成报告格式

完成后按以下格式输出：

```text
Milestone 22 完成报告

1. 新增/修改文件清单
2. Model Provider 生产配置说明
3. Connection Test 验证结果
4. Chat SSE 真实模型验证结果
5. Task Execute 真实模型验证结果
6. Usage / Cost / ModelRequestLog 验证结果
7. GitHub OAuth 配置与验证结果
8. Repository / PR / Patch 只读验证结果
9. PR Review 真实模型验证结果
10. 安全检查结果（密钥、日志、前端响应）
11. Smoke Test 脚本结果
12. 自动化测试结果
13. 已知限制
14. 是否可以进入 Milestone 23：生产监控、告警与安全加固
```

## 15. Claude 执行提示词

将下面内容复制给 Claude 执行：

```text
请根据项目中的文档执行 Milestone 22。

文档路径：
docs/milestone-22-real-model-github-oauth-production-validation.md

执行要求：
1. 先完整阅读该文档，再检查 Milestone 16、17、21 的相关代码和文档。
2. 本阶段是生产联调准备与验收，不要重写 Model Gateway，也不要重写 GitHub OAuth。
3. 不要改业务逻辑，不要改后端核心接口，不要改数据库结构，除非发现真实联调必须修复的小 bug。
4. 不要提交真实 API Key、GitHub Client Secret、OAuth token、.env.production。
5. 不要调用 GitHub 写接口，不要 push、merge、approve、comment PR。
6. 不要关闭认证、Prompt Safety 或 Mock fallback。
7. 保持 MOCK 模式测试稳定通过。

需要实现：
1. 新增 docs/model-provider-production-setup.md。
2. 新增 docs/github-oauth-production-setup.md。
3. 新增 docs/milestone-22-validation-report-template.md。
4. 更新 .env.production.example，补齐真实模型 Provider 和 GitHub OAuth 生产变量。
5. 更新 docs/production-deployment-runbook.md、docs/deployment-guide.md、README.md，加入真实模型和 GitHub OAuth 联调入口。
6. 新增 scripts/validate-model-provider.sh。
7. 新增 scripts/validate-github-oauth-config.sh。
8. 新增 scripts/prod-external-services-smoke-test.sh。
9. 检查 ModelGateway Provider env 映射、connection test、fallback、SSE stream，如有小 bug 进行修复。
10. 检查 GitHub OAuth redirect URI、scopes、未配置提示、token masking，如有小 bug 进行修复。
11. 检查 PR Review prompt，确保 GitHub token 不进入 prompt。
12. 检查日志和前端响应，确保不泄露 API Key / GitHub token。

重点要求：
1. 没有真实模型 Key 时，脚本输出 SKIP，不失败。
2. 没有 GitHub OAuth 配置或未绑定账号时，脚本输出 SKIP，不失败。
3. 登录失败、基础 API 不通必须 FAIL。
4. Connection Test 错误要清晰。
5. Chat SSE 真实模型和 Mock fallback 都要保留。
6. GitHub 集成保持只读，不新增任何写接口。
7. 所有新增脚本必须 set -euo pipefail，且不得打印密钥。

完成后必须执行：
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1

回到项目根目录执行：
bash scripts/validate-model-provider.sh || true
bash scripts/validate-github-oauth-config.sh || true

如本地或生产环境可用，执行：
bash scripts/prod-smoke-test.sh http://localhost
bash scripts/prod-external-services-smoke-test.sh http://localhost

安全检查必须执行：
git status --short
rg "sk-|ghp_|github_pat_|OPENAI_API_KEY=.*[A-Za-z0-9]|CLAUDE_API_KEY=.*[A-Za-z0-9]|GITHUB_CLIENT_SECRET=.*[A-Za-z0-9]" .

完成后按以下格式输出：
1. 新增/修改文件清单
2. Model Provider 生产配置说明
3. Connection Test 验证结果
4. Chat SSE 真实模型验证结果
5. Task Execute 真实模型验证结果
6. Usage / Cost / ModelRequestLog 验证结果
7. GitHub OAuth 配置与验证结果
8. Repository / PR / Patch 只读验证结果
9. PR Review 真实模型验证结果
10. 安全检查结果（密钥、日志、前端响应）
11. Smoke Test 脚本结果
12. 自动化测试结果
13. 已知限制
14. 是否可以进入 Milestone 23：生产监控、告警与安全加固

现在开始实现，不要只给计划。
```
