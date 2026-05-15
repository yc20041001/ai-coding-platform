# Milestone 17: GitHub OAuth + PR Review 工作流实施文档

## 1. 背景与目标

Milestone 16 已完成真实模型网关接入与生产级加固：

- Mock Provider 保留
- OpenAI Compatible / Claude Provider 基础能力
- Model Gateway 配置页面
- API Key masking
- Prompt Safety
- Retry / fallback
- Token 与成本统计
- 后端 52/52 测试通过
- 前端 E2E 12/12 通过

当前平台已经具备 AI Coding Console 的核心基础能力，但 GitHub 集成仍停留在仓库绑定、clone、pull、branch、diff 等基础仓库操作层面，缺少真实协作开发中最关键的 Pull Request Review 工作流。

Milestone 17 的目标是：

> 完成 GitHub OAuth 授权、GitHub Repository/PR 只读接入、PR Diff 拉取、AI PR Review 建议生成与展示，形成安全的只读 PR Review 闭环。

本阶段重点是：

- 只读 GitHub 授权
- 只读 PR 数据同步
- 只读 Diff 分析
- AI Review 建议生成
- Review 结果入库与前端展示

本阶段不做自动 push、自动 merge、自动向 GitHub 写评论。

## 2. 实施边界

### 2.1 本阶段要做

- GitHub OAuth App 配置接入
- GitHub 账号绑定
- GitHub Access Token 安全存储或环境隔离策略
- 获取用户 GitHub repositories
- 获取 repository pull requests
- 获取 pull request detail
- 获取 pull request changed files / diff / patch
- 创建 AI PR Review 任务
- 调用 Agent Orchestrator / Model Gateway 生成 Review 建议
- 保存 PR Review 记录
- 保存 Review Findings
- 前端新增 GitHub / PR Review 页面
- 前端展示 PR diff、AI review summary、findings
- 审计 GitHub OAuth / PR Review 操作

### 2.2 本阶段不做

- 不自动 push
- 不自动 merge
- 不自动 close PR
- 不自动 approve/request changes
- 不自动向 GitHub 写 review comment
- 不执行真实 Git 写操作
- 不执行 shell 修改代码
- 不做 GitHub App 安装模式
- 不做企业 SSO
- 不做多 Git Provider 抽象

## 3. 安全原则

必须遵守：

- GitHub token 不返回前端明文
- GitHub token 不写日志
- GitHub token 不进入 Model Prompt
- GitHub token 不提交到 Git
- 默认只申请最小 OAuth scope
- PR Review 只生成平台内部建议
- 不调用 GitHub 写接口
- 所有 GitHub API 错误要有清晰错误码
- 所有 GitHub 操作写审计日志

建议 OAuth scopes：

```text
read:user
user:email
repo
```

如果只支持 public repo，可以使用：

```text
public_repo
read:user
user:email
```

本阶段建议先支持 `repo`，方便私有仓库测试，但必须在 README 中明确说明权限用途。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V11__init_github_pr_review_tables.sql
```

### 4.1 github_oauth_state

OAuth state 防 CSRF 表。

字段：

```sql
id BIGINT PRIMARY KEY
state VARCHAR(128) NOT NULL
user_id BIGINT NOT NULL
redirect_uri VARCHAR(512) NULL
status VARCHAR(32) NOT NULL
expires_at DATETIME NOT NULL
create_time DATETIME NOT NULL
```

索引：

```sql
uk_github_oauth_state(state)
idx_github_oauth_user(user_id)
idx_github_oauth_expires(expires_at)
```

### 4.2 github_repository_cache

GitHub 仓库缓存表。

字段：

```sql
id BIGINT PRIMARY KEY
user_id BIGINT NOT NULL
github_repo_id BIGINT NOT NULL
owner VARCHAR(128) NOT NULL
repo_name VARCHAR(128) NOT NULL
full_name VARCHAR(256) NOT NULL
private_repo TINYINT NOT NULL DEFAULT 0
default_branch VARCHAR(128) NULL
html_url VARCHAR(512) NULL
description TEXT NULL
language VARCHAR(64) NULL
updated_at DATETIME NULL
create_time DATETIME NOT NULL
update_time DATETIME NOT NULL
```

索引：

```sql
uk_user_github_repo(user_id, github_repo_id)
idx_github_repo_full_name(full_name)
idx_github_repo_updated(updated_at)
```

### 4.3 github_pull_request_cache

GitHub PR 缓存表。

字段：

```sql
id BIGINT PRIMARY KEY
project_id BIGINT NULL
repository_id BIGINT NULL
github_pr_id BIGINT NOT NULL
github_repo_id BIGINT NOT NULL
number INT NOT NULL
title VARCHAR(512) NOT NULL
state VARCHAR(32) NOT NULL
author_login VARCHAR(128) NULL
base_branch VARCHAR(128) NULL
head_branch VARCHAR(128) NULL
html_url VARCHAR(512) NULL
diff_url VARCHAR(512) NULL
patch_url VARCHAR(512) NULL
additions INT DEFAULT 0
deletions INT DEFAULT 0
changed_files INT DEFAULT 0
github_created_at DATETIME NULL
github_updated_at DATETIME NULL
create_time DATETIME NOT NULL
update_time DATETIME NOT NULL
```

索引：

```sql
uk_github_pr(github_repo_id, number)
idx_github_pr_project(project_id)
idx_github_pr_repo(repository_id)
idx_github_pr_state(state)
idx_github_pr_updated(github_updated_at)
```

### 4.4 pr_review_job

PR Review 任务表。

字段：

```sql
id BIGINT PRIMARY KEY
project_id BIGINT NOT NULL
repository_id BIGINT NULL
pull_request_id BIGINT NOT NULL
agent_id BIGINT NULL
status VARCHAR(32) NOT NULL
review_mode VARCHAR(32) NOT NULL
summary MEDIUMTEXT NULL
risk_level VARCHAR(32) NULL
model_provider VARCHAR(64) NULL
model_name VARCHAR(128) NULL
token_usage BIGINT DEFAULT 0
error_message TEXT NULL
started_at DATETIME NULL
finished_at DATETIME NULL
creator_id BIGINT NOT NULL
create_time DATETIME NOT NULL
update_time DATETIME NOT NULL
```

索引：

```sql
idx_pr_review_project_time(project_id, create_time)
idx_pr_review_pr(pull_request_id)
idx_pr_review_status(status)
idx_pr_review_creator(creator_id)
```

### 4.5 pr_review_finding

PR Review 发现项表。

字段：

```sql
id BIGINT PRIMARY KEY
review_job_id BIGINT NOT NULL
project_id BIGINT NOT NULL
severity VARCHAR(32) NOT NULL
category VARCHAR(64) NOT NULL
file_path VARCHAR(512) NULL
line_number INT NULL
title VARCHAR(512) NOT NULL
description MEDIUMTEXT NULL
suggestion MEDIUMTEXT NULL
code_snippet MEDIUMTEXT NULL
create_time DATETIME NOT NULL
```

索引：

```sql
idx_review_finding_job(review_job_id)
idx_review_finding_project(project_id)
idx_review_finding_severity(severity)
idx_review_finding_file(file_path)
```

## 5. 后端模块设计

新增模块：

```text
github/
  application/
  controller/
  domain/
  dto/
  infrastructure/
```

### 5.1 domain 枚举

```text
GithubOAuthStateStatus
- PENDING
- USED
- EXPIRED

GithubPullRequestState
- OPEN
- CLOSED
- MERGED

PrReviewJobStatus
- PENDING
- RUNNING
- COMPLETED
- FAILED
- CANCELED

PrReviewMode
- SUMMARY
- SECURITY
- QUALITY
- FULL

PrReviewRiskLevel
- LOW
- MEDIUM
- HIGH
- CRITICAL

PrReviewFindingSeverity
- INFO
- WARNING
- ERROR
- CRITICAL

PrReviewFindingCategory
- BUG
- SECURITY
- PERFORMANCE
- STYLE
- MAINTAINABILITY
- TEST
- DOCUMENTATION
```

### 5.2 domain 实体

```text
GithubOAuthStateEntity
GithubRepositoryCacheEntity
GithubPullRequestCacheEntity
PrReviewJobEntity
PrReviewFindingEntity
```

要求：

- MyBatis-Plus 注解
- 手写 getter/setter
- 无 Lombok
- 时间字段使用 LocalDateTime
- ID 对外转 String

### 5.3 infrastructure Mapper

```text
GithubOAuthStateMapper
GithubRepositoryCacheMapper
GithubPullRequestCacheMapper
PrReviewJobMapper
PrReviewFindingMapper
```

均继承 `BaseMapper<T>`。

## 6. GitHub Client 设计

新增：

```text
github/application/GithubClient.java
github/application/GithubOAuthService.java
github/application/GithubRepositoryService.java
github/application/GithubPullRequestService.java
github/application/PrReviewApplicationService.java
```

### 6.1 GithubClient

职责：

- 封装 GitHub REST API
- 统一 Authorization header
- 统一错误处理
- 统一 rate limit 处理
- 不输出 token 日志

建议使用：

```text
RestClient 或 WebClient
```

接口：

```java
GithubUserResponse getCurrentUser(String accessToken);
List<GithubRepositoryResponse> listRepositories(String accessToken);
List<GithubPullRequestResponse> listPullRequests(String accessToken, String owner, String repo, String state);
GithubPullRequestDetailResponse getPullRequest(String accessToken, String owner, String repo, int number);
List<GithubPullRequestFileResponse> listPullRequestFiles(String accessToken, String owner, String repo, int number);
String getPullRequestPatch(String accessToken, String patchUrl);
```

### 6.2 GitHub API 错误码

新增：

```text
GithubErrorCode
```

建议值：

```text
OAUTH_NOT_CONFIGURED
OAUTH_STATE_INVALID
OAUTH_TOKEN_EXCHANGE_FAILED
GITHUB_TOKEN_MISSING
GITHUB_AUTH_FAILED
GITHUB_RATE_LIMITED
GITHUB_REPO_NOT_FOUND
GITHUB_PR_NOT_FOUND
GITHUB_API_ERROR
GITHUB_BAD_RESPONSE
```

## 7. GitHub OAuth 流程

### 7.1 环境变量

`.env.example` 新增：

```bash
# GitHub OAuth
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
GITHUB_REDIRECT_URI=http://localhost:8080/api/github/oauth/callback
GITHUB_OAUTH_SCOPES=repo,read:user,user:email
```

`application.yml` 新增：

```yaml
app:
  github:
    client-id: ${GITHUB_CLIENT_ID:}
    client-secret: ${GITHUB_CLIENT_SECRET:}
    redirect-uri: ${GITHUB_REDIRECT_URI:http://localhost:8080/api/github/oauth/callback}
    scopes: ${GITHUB_OAUTH_SCOPES:repo,read:user,user:email}
```

### 7.2 OAuth endpoints

```http
GET /api/github/oauth/authorize
GET /api/github/oauth/callback?code=&state=
GET /api/github/oauth/status
DELETE /api/github/oauth/bindings/{bindingId}
```

权限：

- authorize/status/delete：登录用户
- callback：公开，但必须校验 state

authorize 返回：

```json
{
  "authorizeUrl": "https://github.com/login/oauth/authorize?...",
  "state": "..."
}
```

callback 行为：

- 校验 state
- code 换 access token
- 调 GitHub `/user`
- 绑定或更新 github_account
- state 标记 USED
- 返回一个简单 HTML，提示授权成功，可关闭窗口

## 8. Repository / PR API 设计

### 8.1 仓库列表

```http
GET /api/github/repositories/sync
GET /api/github/repositories
```

说明：

- sync 从 GitHub 拉取并缓存
- repositories 返回缓存数据

### 8.2 PR 列表

```http
GET /api/github/repositories/{owner}/{repo}/pull-requests?state=open
```

返回 PR 列表，同时缓存到 `github_pull_request_cache`。

### 8.3 PR 详情

```http
GET /api/github/repositories/{owner}/{repo}/pull-requests/{number}
GET /api/github/repositories/{owner}/{repo}/pull-requests/{number}/files
GET /api/github/repositories/{owner}/{repo}/pull-requests/{number}/patch
```

说明：

- files 返回 changed files
- patch 返回 patch 文本
- patch 内容需要限制大小，避免 prompt 过长

## 9. AI PR Review 设计

### 9.1 创建 Review

```http
POST /api/projects/{projectId}/github/pr-reviews
```

请求：

```json
{
  "owner": "yc20041001",
  "repo": "ai-coding-platform",
  "pullRequestNumber": 12,
  "reviewMode": "FULL",
  "agentId": "300005"
}
```

权限：

```text
MAINTAINER+
```

### 9.2 查询 Review

```http
GET /api/projects/{projectId}/github/pr-reviews
GET /api/github/pr-reviews/{reviewJobId}
GET /api/github/pr-reviews/{reviewJobId}/findings
```

权限：

```text
VIEWER+
```

### 9.3 Review Prompt

Prompt 必须包含：

- PR title
- PR description
- base/head branch
- changed files summary
- patch 内容
- review mode
- 输出格式约束

要求模型输出 JSON：

```json
{
  "summary": "整体评价",
  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
  "findings": [
    {
      "severity": "WARNING",
      "category": "BUG",
      "filePath": "src/App.vue",
      "lineNumber": 42,
      "title": "潜在空指针",
      "description": "问题说明",
      "suggestion": "修改建议",
      "codeSnippet": "相关代码片段"
    }
  ]
}
```

如模型输出非 JSON：

- 保存原始 summary
- findings 为空
- 不让流程失败

### 9.4 Review 安全限制

本阶段：

- 只生成平台内部建议
- 不写 GitHub comments
- 不 approve
- 不 request changes
- 不 merge

## 10. 前端设计

### 10.1 新增页面

```text
frontend/src/modules/github/api.ts
frontend/src/modules/github/pages/GithubIntegrationPage.vue
frontend/src/modules/github/pages/PullRequestReviewPage.vue
frontend/src/modules/github/components/GithubOAuthCard.vue
frontend/src/modules/github/components/RepositoryPicker.vue
frontend/src/modules/github/components/PullRequestList.vue
frontend/src/modules/github/components/PullRequestDiffViewer.vue
frontend/src/modules/github/components/PrReviewResultPanel.vue
frontend/src/modules/github/components/PrReviewFindingList.vue
```

### 10.2 路由

新增：

```text
/github
/projects/:projectId/github/pr-review
```

在 FloatingDock 或 ProjectDetail Tabs 中加入入口。

### 10.3 页面能力

GitHub Integration 页面：

- 显示 OAuth 绑定状态
- 发起 GitHub 授权
- 同步 repositories
- 查看 repository 列表

PR Review 页面：

- 选择 repository
- 选择 PR
- 查看 PR 基本信息
- 查看 changed files
- 查看 patch/diff
- 选择 Review Mode
- 点击 Run AI Review
- 展示 review summary
- 展示 findings 列表

## 11. 审计与可观测性

需要记录 Audit Log：

- GITHUB_OAUTH_START
- GITHUB_OAUTH_CALLBACK
- GITHUB_REPOSITORY_SYNC
- GITHUB_PR_FETCH
- PR_REVIEW_START
- PR_REVIEW_COMPLETE
- PR_REVIEW_FAILED

Model Request Log：

- PR Review 调用必须写入 model_request_log
- 标记 requestType=CODE_REVIEW
- 记录 token、latency、success、fallback

## 12. 测试要求

### 12.1 后端

新增测试建议：

```text
GithubOAuthServiceTest
GithubClientTest
GithubRepositoryServiceTest
PrReviewApplicationServiceTest
PrReviewPromptBuilderTest
```

必须覆盖：

- OAuth 未配置返回 OAUTH_NOT_CONFIGURED
- state 过期/错误返回 OAUTH_STATE_INVALID
- token 不返回前端明文
- GitHub API 401 映射 GITHUB_AUTH_FAILED
- GitHub API 429 映射 GITHUB_RATE_LIMITED
- PR Review 创建成功
- PR Review 模型输出 JSON 可解析 findings
- PR Review 模型输出非 JSON 不失败

### 12.2 前端

新增 E2E 建议：

- GitHub 页面可打开
- OAuth 未配置时显示清晰提示
- Repository 列表空状态
- PR Review 页面空状态
- 已有 Mock 数据时可展示 Review Result

## 13. 验证命令

后端：

```bash
cd backend
mvn clean compile
mvn test
```

前端：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e
```

## 14. 验收标准

必须满足：

- GitHub OAuth 未配置时系统可正常启动
- GitHub OAuth 配置后可发起授权
- OAuth callback 可绑定 GitHub 账号
- Repository sync 可缓存仓库
- PR list/detail/files/patch 可读取
- PR Review 可生成 summary 和 findings
- Review 结果可入库
- 前端可展示 PR Review 结果
- 不调用 GitHub 写接口
- 不泄露 GitHub token
- 后端测试通过
- 前端 typecheck/build/e2e 通过

## 15. 已知限制

本阶段允许保留：

- 不自动发布 GitHub review comment
- 不支持 GitHub App 安装模式
- 不支持 GitLab/Gitee
- patch 大文件截断
- Review findings 行号可能不完全精准
- 模型 JSON 解析失败时 findings 为空

## 16. 完成报告模板

```markdown
# Milestone 17 完成报告

## 1. 新增/修改文件清单

| 文件 | 说明 |
|---|---|
|  |  |

## 2. GitHub OAuth

| 场景 | 结果 |
|---|---|
| 未配置 OAuth |  |
| authorize URL |  |
| callback state 校验 |  |
| GitHub 账号绑定 |  |

## 3. Repository / PR 只读能力

| 能力 | 结果 |
|---|---|
| repository sync |  |
| PR list |  |
| PR detail |  |
| PR files |  |
| PR patch |  |

## 4. AI PR Review

| 能力 | 结果 |
|---|---|
| 创建 review job |  |
| 生成 summary |  |
| 生成 findings |  |
| 入库 |  |
| 查询 |  |

## 5. 前端页面

| 页面/组件 | 结果 |
|---|---|
| GitHub Integration |  |
| PR Review Page |  |
| Diff Viewer |  |
| Review Result Panel |  |

## 6. 安全验证

| 项目 | 结果 |
|---|---|
| token 不返回前端明文 |  |
| token 不写日志 |  |
| 不调用 GitHub 写接口 |  |
| 审计日志 |  |

## 7. 测试结果

| 命令 | 结果 |
|---|---|
| backend mvn clean compile |  |
| backend mvn test |  |
| frontend npm run typecheck |  |
| frontend npm run build |  |
| frontend npm run test:e2e |  |

## 8. 已知限制

- 待补充。

## 9. 是否可以进入 Milestone 18：CI/CD + Docker 镜像 + 部署

- [ ] 是
- [ ] 否
```

## 17. Claude 执行提示词

可以直接发送以下内容给 Claude：

```text
请根据项目中的文档执行 Milestone 17：GitHub OAuth + PR Review 工作流。

文档路径：
docs/milestone-17-github-oauth-pr-review.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend、frontend、docs、scripts 目录结构。
2. 本阶段是在 Milestone 16 已完成真实模型网关加固后，补齐 GitHub OAuth 与只读 PR Review 工作流。
3. 不重构已验证通过的 Auth、Project、Repository、Task、Agent、Chat、RAG、Model Gateway 核心逻辑。
4. 不破坏 Mock Provider。
5. 不破坏 Chat SSE。
6. 不破坏动态科技感 UI，不要恢复传统左侧 sidebar。
7. 不执行 GitHub 写操作。
8. 不自动 push、merge、close PR、approve PR、request changes。
9. 不向 GitHub 写 review comment。
10. 不把 GitHub token 返回前端明文。
11. 不把 GitHub token 写日志。
12. 不把 GitHub token 放入 Model Prompt。
13. GitHub OAuth 未配置时系统必须能正常启动，并给出清晰提示。
14. 可以修复实现过程中发现的明确 bug，但必须说明原因和影响范围。

需要实现：
1. V11 数据库迁移：github_oauth_state、github_repository_cache、github_pull_request_cache、pr_review_job、pr_review_finding。
2. GitHub OAuth 配置项与 .env.example 更新。
3. GitHub OAuth authorize / callback / status / unbind 接口。
4. GitHub Client，只读封装 GitHub REST API。
5. Repository sync 与 repository cache。
6. PR list / detail / files / patch 只读接口。
7. PR Review Job 创建、执行、查询。
8. PR Review Prompt 构建。
9. 调用 Model Gateway 生成 Review summary/findings。
10. Review findings JSON 解析与入库。
11. 审计日志：OAuth、Repo Sync、PR Fetch、PR Review。
12. 前端 GitHub Integration 页面。
13. 前端 PR Review 页面。
14. 前端 Diff Viewer。
15. 前端 Review Result / Findings 展示。
16. 后端测试覆盖 OAuth 未配置、state 校验、PR Review JSON 解析、非 JSON 降级。
17. 前端 E2E 覆盖 GitHub 页面基础展示和 OAuth 未配置提示。

完成后必须执行：
后端：
cd backend
mvn clean compile
mvn test

前端：
cd frontend
npm run typecheck
npm run build
npm run test:e2e

手动验证：
1. 未配置 GitHub OAuth 时，后端正常启动。
2. 前端 GitHub 页面显示 OAuth 未配置提示。
3. 配置 OAuth 后，可生成 authorize URL。
4. callback state 校验有效。
5. Repository sync 可缓存仓库。
6. PR list/detail/files/patch 可读取。
7. 创建 PR Review 后可生成 summary 和 findings。
8. PR Review 结果可在前端展示。
9. 不调用任何 GitHub 写接口。
10. GitHub token 不出现在前端响应和日志中。

完成后按以下格式输出：
1. 新增/修改文件清单
2. GitHub OAuth 实现结果
3. Repository / PR 只读能力结果
4. AI PR Review 实现结果
5. 前端页面实现结果
6. 安全验证结果
7. 构建与测试结果
8. 手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 18：CI/CD + Docker 镜像 + 部署

现在开始执行，不要只给计划。
```
