# Milestone 38A: External Beta Feedback Loop & Real-world Hardening

## 1. 背景

Milestone 37A-37K 已经把平台从内部 Alpha 能力推进到较完整的 Beta 候选状态：

```text
Auth / Project / Task / Chat / RAG
  + Real Model Gateway
  + GitHub OAuth / PR Review
  + Tool Sandbox / Approval / Patch Proposal
  + Async Worker / DLQ / Metrics / Trace / Incident / RCA / Retrospective
```

当前代码层面的能力已经比较完整，但 `v1.1 External Beta` 的真正目标不是继续横向堆模块，而是验证：

```text
真实用户是否能顺畅使用？
真实模型配置是否稳定？
GitHub OAuth / PR Review 在真实仓库下是否可靠？
生产/演示环境是否可重复部署、可诊断、可回收反馈？
```

Milestone 38A 的目标是新增：

```text
External Beta Feedback Loop & Real-world Hardening
```

让项目从：

```text
功能完备、测试较全
```

升级为：

```text
真实试用可执行、反馈可回收、环境可诊断、问题可闭环
```

---

## 2. 总目标

实现 External Beta 试用闭环与真实环境加固能力：

1. 新增 Beta Trial Session 数据模型。
2. 新增 Beta Trial Feedback 数据模型。
3. 支持记录真实试用 Session、参与者角色、环境、Provider、GitHub OAuth 状态。
4. 支持记录 Trial Outcome、Blocker、满意度、继续使用意愿。
5. 支持把反馈按 taxonomy 自动归档到结构化字段。
6. 支持 Trial Summary Dashboard。
7. 支持 Environment Readiness Check 聚合结果保存。
8. 支持 Trial Report Markdown 导出。
9. 支持把高优先级反馈映射到 Incident / Known Issue / Release Checklist follow-up。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有反馈模板、有文档
```

升级为：

```text
有真实试用记录、有结构化反馈、有质量门、有 Beta 收口依据
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch` / `git reset` / `git apply` / `git add` / `git commit` / `git push`。
4. 不写真实 workspace 业务文件。
5. 不自动创建 GitHub Issue。
6. 不发送真实 Email / Slack / PagerDuty / Webhook。
7. 不调用真实 AI 自动总结用户反馈。
8. Feedback taxonomy 只做规则映射，不做真实 NLP 分类。
9. Environment readiness 只记录已有脚本或已有 API 的检测结果，不新增危险运维动作。
10. 不破坏 1-37K 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 beta trial / feedback / readiness 表。
2. 聚合已有 smoke test / provider / GitHub / observability 状态。
3. 新增 Dashboard / 表格 / 详情抽屉 / Markdown 导出。
4. 新增风险分级与 Beta pass/block 标记。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V38__init_beta_trial_feedback_tables.sql
```

### 4.1 beta_trial_session

```sql
CREATE TABLE beta_trial_session (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    participant_role VARCHAR(64) NOT NULL,
    environment_type VARCHAR(32) NOT NULL,
    provider_mode VARCHAR(32) NOT NULL,
    github_oauth_status VARCHAR(32) NOT NULL,
    session_status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    completed_path_json JSON NULL,
    blocked_at_step VARCHAR(128) NULL,
    blocker_summary TEXT NULL,
    satisfaction_score INT NULL,
    continue_intent VARCHAR(32) NULL,
    summary TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_trial_project_time(project_id, create_time),
    KEY idx_beta_trial_status(session_status),
    KEY idx_beta_trial_env(environment_type, provider_mode),
    KEY idx_beta_trial_github(github_oauth_status)
);
```

### 4.2 beta_trial_feedback

```sql
CREATE TABLE beta_trial_feedback (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    category VARCHAR(64) NOT NULL,
    subcategory VARCHAR(64) NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'P2',
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    title VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    expected_behavior TEXT NULL,
    actual_behavior TEXT NULL,
    suggested_action TEXT NULL,
    triage_status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    mapped_incident_id BIGINT NULL,
    mapped_known_issue_id BIGINT NULL,
    release_blocking TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_feedback_session(session_id),
    KEY idx_beta_feedback_project(project_id),
    KEY idx_beta_feedback_category(category, severity),
    KEY idx_beta_feedback_triage(triage_status, release_blocking)
);
```

### 4.3 beta_environment_readiness

```sql
CREATE TABLE beta_environment_readiness (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    session_id BIGINT NULL,
    target_name VARCHAR(128) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    check_status VARCHAR(32) NOT NULL,
    summary TEXT NULL,
    detail_json JSON NULL,
    checked_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_readiness_project(project_id),
    KEY idx_beta_readiness_session(session_id),
    KEY idx_beta_readiness_target(target_type, check_status)
);
```

无物理外键，保持项目当前风格。

---

## 5. 枚举设计

新增：

```text
BetaTrialSessionStatus.java
BetaEnvironmentType.java
BetaProviderMode.java
BetaGithubOAuthStatus.java
BetaContinueIntent.java
BetaFeedbackSeverity.java
BetaFeedbackSourceType.java
BetaFeedbackTriageStatus.java
BetaReadinessCheckStatus.java
```

### 5.1 BetaTrialSessionStatus

```text
PLANNED
IN_PROGRESS
COMPLETED
BLOCKED
CANCELED
```

### 5.2 BetaEnvironmentType

```text
LOCAL
DOCKER_COMPOSE
PROD_DEMO
SELF_HOSTED
```

### 5.3 BetaProviderMode

```text
MOCK
REAL_MODEL
MIXED
UNKNOWN
```

### 5.4 BetaGithubOAuthStatus

```text
NOT_CONFIGURED
CONFIGURED_NOT_TESTED
TESTED_OK
TESTED_FAILED
NOT_APPLICABLE
```

### 5.5 BetaContinueIntent

```text
YES
MAYBE
NO
FOLLOW_UP_NEEDED
```

### 5.6 BetaFeedbackSeverity

```text
P0
P1
P2
P3
```

### 5.7 BetaFeedbackSourceType

```text
MANUAL
WALKTHROUGH
SMOKE_TEST
OPERATOR_SUMMARY
```

### 5.8 BetaFeedbackTriageStatus

```text
NEW
TRIAGED
SCHEDULED
DONE
WONT_FIX
```

### 5.9 BetaReadinessCheckStatus

```text
PASS
WARN
FAIL
SKIP
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
BetaTrialSessionEntity.java
BetaTrialFeedbackEntity.java
BetaEnvironmentReadinessEntity.java

BetaTrialSessionMapper.java
BetaTrialFeedbackMapper.java
BetaEnvironmentReadinessMapper.java
```

新增 DTO：

```text
CreateBetaTrialSessionRequest.java
UpdateBetaTrialSessionRequest.java
BetaTrialSessionResponse.java
BetaTrialSessionSummaryResponse.java

CreateBetaTrialFeedbackRequest.java
UpdateBetaTrialFeedbackRequest.java
BetaTrialFeedbackResponse.java
BetaTrialFeedbackSummaryResponse.java

CreateBetaEnvironmentReadinessRequest.java
BetaEnvironmentReadinessResponse.java

BetaTrialDashboardResponse.java
BetaPassBlockSummaryResponse.java
```

### 6.1 CreateBetaTrialSessionRequest

```java
public class CreateBetaTrialSessionRequest {
    private String projectId;
    private String title;
    private String participantRole;
    private String environmentType;
    private String providerMode;
    private String githubOauthStatus;
}
```

### 6.2 UpdateBetaTrialSessionRequest

```java
public class UpdateBetaTrialSessionRequest {
    private String sessionStatus;
    private String blockedAtStep;
    private String blockerSummary;
    private String completedPathJson;
    private Integer satisfactionScore;
    private String continueIntent;
    private String summary;
    private String startedAt;
    private String endedAt;
}
```

### 6.3 CreateBetaTrialFeedbackRequest

```java
public class CreateBetaTrialFeedbackRequest {
    private String category;
    private String subcategory;
    private String severity;
    private String sourceType;
    private String title;
    private String detail;
    private String expectedBehavior;
    private String actualBehavior;
    private String suggestedAction;
    private Boolean releaseBlocking;
}
```

### 6.4 CreateBetaEnvironmentReadinessRequest

```java
public class CreateBetaEnvironmentReadinessRequest {
    private String projectId;
    private String sessionId;
    private String targetName;
    private String targetType;
    private String checkStatus;
    private String summary;
    private String detailJson;
}
```

---

## 7. 后端服务设计

### 7.1 BetaTrialSessionService

新增：

```text
BetaTrialSessionService.java
```

职责：

1. 创建 / 更新 / 查询试用 Session。
2. 维护试用状态流转。
3. 聚合完成路径、blocker、满意度、继续使用意愿。
4. 导出单次 session Markdown。

建议方法：

```java
public BetaTrialSessionResponse createSession(CreateBetaTrialSessionRequest request)

public BetaTrialSessionResponse updateSession(Long sessionId, UpdateBetaTrialSessionRequest request)

public BetaTrialSessionResponse getSession(Long sessionId)

public PageResult<BetaTrialSessionSummaryResponse> listSessions(Long projectId, String sessionStatus, PageQuery pageQuery)

public String exportSessionMarkdown(Long sessionId)
```

状态建议：

1. `PLANNED -> IN_PROGRESS`
2. `IN_PROGRESS -> COMPLETED`
3. `IN_PROGRESS -> BLOCKED`
4. `PLANNED -> CANCELED`
5. `BLOCKED -> IN_PROGRESS` 允许恢复

### 7.2 BetaTrialFeedbackService

新增：

```text
BetaTrialFeedbackService.java
```

职责：

1. 创建反馈。
2. 更新 triage 状态。
3. 维护 release-blocking 标记。
4. 支持映射到 incident / known issue。
5. 提供 category / severity / status 聚合统计。

建议方法：

```java
public BetaTrialFeedbackResponse createFeedback(Long sessionId, CreateBetaTrialFeedbackRequest request)

public BetaTrialFeedbackResponse updateFeedback(Long feedbackId, UpdateBetaTrialFeedbackRequest request)

public PageResult<BetaTrialFeedbackSummaryResponse> listFeedback(Long sessionId, Long projectId, String severity, String triageStatus, PageQuery pageQuery)

public BetaPassBlockSummaryResponse getPassBlockSummary(Long projectId)
```

分类规则：

1. 允许前端手动选择 `category/subcategory`
2. 可选提供简单映射 helper：

```text
包含 login/auth -> Auth
包含 chat/sse -> SSE Streaming
包含 rag/search -> RAG
包含 pr/github -> GitHub
包含 deploy/docker -> Deployment
```

但不做真实 NLP。

### 7.3 BetaEnvironmentReadinessService

新增：

```text
BetaEnvironmentReadinessService.java
```

职责：

1. 保存环境 readiness 结果。
2. 聚合 project / session 的 readiness 状态。
3. 对接已有脚本执行结果的结构化保存。

建议方法：

```java
public BetaEnvironmentReadinessResponse createCheck(CreateBetaEnvironmentReadinessRequest request)

public List<BetaEnvironmentReadinessResponse> listChecks(Long projectId, Long sessionId)

public BetaTrialDashboardResponse getDashboard(Long projectId)
```

说明：

1. 本阶段不要求后端自动执行 shell 脚本。
2. 可以由前端或手工录入已有 `prod-smoke-test` / `demo-smoke-test` 结果摘要。

### 7.4 BetaTrialDashboard 聚合

Dashboard 至少包含：

1. Session 总数
2. `COMPLETED / BLOCKED / IN_PROGRESS` 数
3. 平均满意度
4. `continueIntent=YES` 数
5. `P0/P1` 反馈数量
6. `releaseBlocking=true` 数
7. Readiness `PASS/WARN/FAIL` 分布
8. Beta pass/block 总结

---

## 8. API 设计

### 8.1 Beta Trial Session

```http
POST /api/beta-trials/sessions
PUT /api/beta-trials/sessions/{sessionId}
GET /api/beta-trials/sessions/{sessionId}
GET /api/projects/{projectId}/beta-trials/sessions?status=&page=&size=
GET /api/beta-trials/sessions/{sessionId}/markdown
```

### 8.2 Beta Trial Feedback

```http
POST /api/beta-trials/sessions/{sessionId}/feedback
PUT /api/beta-trials/feedback/{feedbackId}
GET /api/beta-trials/feedback?sessionId=&projectId=&severity=&triageStatus=&page=&size=
GET /api/projects/{projectId}/beta-trials/pass-block-summary
```

### 8.3 Environment Readiness

```http
POST /api/beta-trials/readiness
GET /api/beta-trials/readiness?projectId=&sessionId=
GET /api/projects/{projectId}/beta-trials/dashboard
```

权限建议：

```text
POST / PUT: MAINTAINER+
GET: VIEWER+
全局聚合或跨项目能力：ADMIN 可保留
```

---

## 9. 前端设计

### 9.1 ObservabilityPage 增强

在现有 Incident / RCA / Retrospective 基础上新增：

1. Beta Trial Dashboard 区块
2. Trial Session 列表
3. Trial Feedback 列表
4. Environment Readiness 列表
5. Pass / Block Summary 卡片

推荐 data-testid：

```text
beta-trial-dashboard
beta-trial-session-table
beta-trial-feedback-table
beta-trial-readiness-table
beta-pass-block-summary
beta-create-session-button
beta-create-feedback-button
```

### 9.2 BetaTrialSessionPanel

新增：

```text
frontend/src/modules/admin/components/BetaTrialSessionPanel.vue
```

功能：

1. 创建试用 Session。
2. 更新状态。
3. 记录 completed path。
4. 记录 blocker / 满意度 / continue intent。
5. 打开 session 详情。

### 9.3 BetaTrialFeedbackPanel

新增：

```text
frontend/src/modules/admin/components/BetaTrialFeedbackPanel.vue
```

功能：

1. 创建反馈。
2. 按 severity / triageStatus 过滤。
3. 展示是否 release-blocking。
4. 可编辑 triageStatus。

### 9.4 BetaEnvironmentReadinessPanel

新增：

```text
frontend/src/modules/admin/components/BetaEnvironmentReadinessPanel.vue
```

功能：

1. 录入 readiness 结果。
2. 展示 PASS / WARN / FAIL / SKIP 状态。
3. 支持按 session / project 过滤。

---

## 10. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
```

新增：

```ts
export interface BetaTrialSession {
  id: string
  projectId?: string
  title: string
  participantRole: string
  environmentType: string
  providerMode: string
  githubOauthStatus: string
  sessionStatus: string
  startedAt?: string
  endedAt?: string
  completedPathJson?: string
  blockedAtStep?: string
  blockerSummary?: string
  satisfactionScore?: number
  continueIntent?: string
  summary?: string
  createTime: string
  updateTime: string
}

export interface BetaTrialFeedback {
  id: string
  sessionId: string
  projectId?: string
  category: string
  subcategory?: string
  severity: string
  sourceType: string
  title: string
  detail?: string
  expectedBehavior?: string
  actualBehavior?: string
  suggestedAction?: string
  triageStatus: string
  mappedIncidentId?: string
  mappedKnownIssueId?: string
  releaseBlocking: boolean
  createTime: string
  updateTime: string
}

export interface BetaEnvironmentReadiness {
  id: string
  projectId?: string
  sessionId?: string
  targetName: string
  targetType: string
  checkStatus: string
  summary?: string
  detailJson?: string
  checkedAt: string
}

export interface BetaTrialDashboard {
  totalSessions: number
  completedSessions: number
  blockedSessions: number
  inProgressSessions: number
  averageSatisfactionScore: number
  continueYesCount: number
  p0Count: number
  p1Count: number
  releaseBlockingCount: number
  readinessPassCount: number
  readinessWarnCount: number
  readinessFailCount: number
}
```

新增 API 函数：

```ts
createBetaTrialSession(data)
updateBetaTrialSession(sessionId, data)
getBetaTrialSession(sessionId)
listProjectBetaTrialSessions(projectId, params)
exportBetaTrialSessionMarkdown(sessionId)

createBetaTrialFeedback(sessionId, data)
updateBetaTrialFeedback(feedbackId, data)
listBetaTrialFeedback(params)
getBetaPassBlockSummary(projectId)

createBetaEnvironmentReadiness(data)
listBetaEnvironmentReadiness(params)
getBetaTrialDashboard(projectId)
```

---

## 11. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/beta/BetaTrialSessionIntegrationTest.java
backend/src/test/java/com/aicoding/platform/beta/BetaTrialFeedbackIntegrationTest.java
backend/src/test/java/com/aicoding/platform/beta/BetaEnvironmentReadinessIntegrationTest.java
```

至少 36 个测试。

### 11.1 Trial Session

1. MAINTAINER 可创建 session。
2. VIEWER 不可创建 session。
3. 非项目成员不可创建 session。
4. 创建后可查询。
5. `PLANNED -> IN_PROGRESS` 成功。
6. `IN_PROGRESS -> COMPLETED` 成功。
7. `IN_PROGRESS -> BLOCKED` 成功。
8. `BLOCKED -> IN_PROGRESS` 成功。
9. 满意度范围校验。
10. continueIntent 枚举校验。
11. markdown export 成功。
12. list sessions 分页成功。

### 11.2 Feedback

13. 可创建 feedback。
14. severity 枚举校验。
15. triageStatus 更新成功。
16. releaseBlocking 保存成功。
17. 可按 severity 过滤。
18. 可按 triageStatus 过滤。
19. pass/block summary 正确聚合。
20. P0/P1 统计正确。
21. releaseBlocking 数量正确。
22. 非项目成员不可查询。

### 11.3 Readiness

23. 可创建 readiness check。
24. checkStatus 枚举校验。
25. 可按 session 查询。
26. 可按 project 查询。
27. dashboard 可聚合 readiness 分布。
28. FAIL/WARN/PASS 计数正确。

### 11.4 权限与聚合

29. 未登录查询 session 返回 UNAUTHORIZED。
30. 未登录查询 feedback 返回 UNAUTHORIZED。
31. 未登录查询 dashboard 返回 UNAUTHORIZED。
32. VIEWER 可查询 dashboard。
33. dashboard 平均满意度正确。
34. dashboard continueYesCount 正确。
35. project 维度聚合不串项目。
36. markdown export 不泄露敏感字段。

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/beta-trial-feedback.spec.ts
```

至少 8 个 E2E：

1. Observability 页面显示 Beta Trial Dashboard。
2. 创建 Trial Session 成功。
3. 更新 Session 状态成功。
4. 创建 Feedback 成功。
5. Feedback 按 severity 过滤可用。
6. Readiness 列表展示状态。
7. Pass/Block Summary 可见。
8. 导出入口可用。
9. 页面无 JS error。

---

## 13. 文档与报告

完成后新增：

```text
docs/milestone-38a-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. beta_trial_session 表说明
3. beta_trial_feedback 表说明
4. beta_environment_readiness 表说明
5. BetaTrialSessionService 设计说明
6. BetaTrialFeedbackService 设计说明
7. BetaEnvironmentReadinessService 设计说明
8. API 清单
9. 前端 Beta Trial / Feedback / Readiness UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 38B

---

## 14. 验收标准

完成后必须满足：

1. 可创建 / 更新 Beta Trial Session。
2. 可记录 completed path / blocker / 满意度 / continue intent。
3. 可创建结构化 feedback。
4. 可记录 release-blocking 标记。
5. 可记录 readiness 检查结果。
6. 可查看 dashboard 与 pass/block summary。
7. 所有 API 有权限校验。
8. 不调用真实 AI。
9. 不执行真实工具。
10. 不写本地业务文件。
11. 后端测试通过。
12. 前端 typecheck / build / E2E 通过。

---

## 15. 非目标

本阶段不做：

1. 不做真实 GitHub Issue 自动创建。
2. 不做真实通知发送。
3. 不做 AI 自动总结反馈。
4. 不做跨组织多租户 beta 管理。
5. 不做高级 BI 仪表盘。
6. 不做自动 release decision engine。

这些可以放到后续 Milestone。

---

## 16. 建议后续 Milestone

完成 38A 后，建议进入：

```text
Milestone 38B: Real Model Cost & PR Review Quality Hardening
```

候选能力：

1. 真实模型成本看板与异常成本告警。
2. GitHub PR Review 命中率 / 可用性 /安全性增强。
3. Beta 反馈到 release checklist / roadmap 的自动映射增强。

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 38A。

文档路径：
docs/milestone-38a-external-beta-feedback-loop-hardening.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37A-37K 基础上，新增 External Beta Feedback Loop & Real-world Hardening。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要自动创建 GitHub Issue。
8. 不要发送真实 Email / Slack / PagerDuty / Webhook。
9. 不调用真实 AI 自动总结用户反馈。
10. Feedback taxonomy 只做规则映射，不做真实 NLP。
11. Environment readiness 只记录已有脚本或已有 API 的检测结果，不新增危险运维动作。
12. 不要破坏 1-37K 已有 API。
13. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
14. IDs 对外保持 String。
15. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V38__init_beta_trial_feedback_tables.sql。
2. 新增 beta_trial_session / beta_trial_feedback / beta_environment_readiness 三张表。
3. 新增相关枚举、Entity、Mapper、DTO。
4. 新增 BetaTrialSessionService。
5. 新增 BetaTrialFeedbackService。
6. 新增 BetaEnvironmentReadinessService。
7. 新增 API：
   - POST /api/beta-trials/sessions
   - PUT /api/beta-trials/sessions/{sessionId}
   - GET /api/beta-trials/sessions/{sessionId}
   - GET /api/projects/{projectId}/beta-trials/sessions
   - GET /api/beta-trials/sessions/{sessionId}/markdown
   - POST /api/beta-trials/sessions/{sessionId}/feedback
   - PUT /api/beta-trials/feedback/{feedbackId}
   - GET /api/beta-trials/feedback
   - GET /api/projects/{projectId}/beta-trials/pass-block-summary
   - POST /api/beta-trials/readiness
   - GET /api/beta-trials/readiness
   - GET /api/projects/{projectId}/beta-trials/dashboard
8. 前端在 ObservabilityPage 中新增 Beta Trial Dashboard / Session / Feedback / Readiness 区块。
9. 新增 BetaTrialSessionPanel.vue。
10. 新增 BetaTrialFeedbackPanel.vue。
11. 新增 BetaEnvironmentReadinessPanel.vue。
12. 后端测试不少于 36 个。
13. 前端 E2E 不少于 8 个。
14. 新增 docs/milestone-38a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. beta_trial_session 表说明
3. beta_trial_feedback 表说明
4. beta_environment_readiness 表说明
5. BetaTrialSessionService 设计说明
6. BetaTrialFeedbackService 设计说明
7. BetaEnvironmentReadinessService 设计说明
8. API 清单
9. 前端 Beta Trial / Feedback / Readiness UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 38B

现在开始实现，不要只给计划。
```
