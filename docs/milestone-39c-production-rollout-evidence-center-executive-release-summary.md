# Milestone 39C: Production Rollout Evidence Center & Executive Release Summary

## 1. 背景

截至 Milestone 39B，平台已经具备较完整的发布治理链路：

```text
38C
  Beta Release Gate / Go-No-Go Decision Center

39A
  Controlled Rollout
  -> Rollout Plan
  -> Rollout Step
  -> Verification Record

39B
  Rollback Drill
  -> Release Audit Event
  -> Postmortem Review
```

这意味着系统已经能回答：

```text
这次版本能不能发？
怎么发？
出了问题怎么回滚？
上线之后如何复盘？
```

但对于更高层的发布管理视角，还缺少一个统一“证据中心”和“管理摘要层”：

```text
这次 release 的所有证据是否能一处查看？
给业务负责人、技术负责人、运维负责人看的摘要长什么样？
跨多个 release，风险趋势是在变好还是变坏？
哪些项目具备更高的 release confidence？
```

Milestone 39C 的目标就是新增：

```text
Production Rollout Evidence Center & Executive Release Summary
```

让平台从：

```text
有 rollout / rollback / audit / postmortem 数据
```

升级为：

```text
有统一证据中心、跨 release 对比能力和面向管理层的执行摘要
```

---

## 2. 总目标

实现统一的 Release Evidence Center 与 Executive Summary：

1. 新增 Release Evidence Bundle 数据模型。
2. 新增 Release Sign-off Record 数据模型。
3. 新增 Release Confidence Snapshot 数据模型。
4. 支持聚合 38C 的 decision、39A 的 rollout、39B 的 rollback / audit / postmortem。
5. 支持生成结构化 release evidence bundle。
6. 支持记录不同角色的 sign-off 状态与备注。
7. 支持计算 release confidence score 与 confidence trend。
8. 支持跨 release 对比 summary。
9. 支持导出 Executive Release Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有很多 release 数据，但仍需人工拼装汇报材料
```

升级为：

```text
系统自动聚合 release 证据，并生成面向管理与审计的执行摘要
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动操作生产基础设施。
4. 不自动生成真实领导签字或代替人工审批。
5. 不自动修改 38C/39A/39B 原始业务记录。
6. 不自动关闭 incident / alert / feedback。
7. 不调用真实 AI 自动写管理结论。
8. evidence bundle 只聚合已有结构化数据，不发起外部系统调用。
9. sign-off 只记录状态、角色、备注、时间，不代表外部合规签章。
10. 不破坏 1-39B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 release evidence / sign-off / confidence snapshot 表。
2. 聚合现有 decision / rollout / verification / incident / postmortem 数据。
3. 新增 dashboard、comparison、summary、export。
4. 新增 confidence score 与趋势展示。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V43__init_release_evidence_summary_tables.sql
```

### 4.1 release_evidence_bundle

```sql
CREATE TABLE release_evidence_bundle (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    bundle_status VARCHAR(32) NOT NULL,
    summary_markdown MEDIUMTEXT NULL,
    evidence_json JSON NULL,
    generated_by BIGINT NULL,
    generated_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_evidence_bundle_project(project_id, create_time),
    KEY idx_release_evidence_bundle_plan(plan_id),
    KEY idx_release_evidence_bundle_status(bundle_status),
    UNIQUE KEY uk_release_evidence_bundle(plan_id)
);
```

### 4.2 release_signoff_record

```sql
CREATE TABLE release_signoff_record (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    signoff_role VARCHAR(64) NOT NULL,
    signoff_status VARCHAR(32) NOT NULL,
    signer_id BIGINT NULL,
    signer_name VARCHAR(128) NULL,
    comment_text TEXT NULL,
    signed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_signoff_project(project_id, create_time),
    KEY idx_release_signoff_plan(plan_id, signoff_role),
    KEY idx_release_signoff_status(signoff_status),
    UNIQUE KEY uk_release_signoff(plan_id, signoff_role)
);
```

### 4.3 release_confidence_snapshot

```sql
CREATE TABLE release_confidence_snapshot (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    confidence_score DECIMAL(8,2) NOT NULL,
    confidence_level VARCHAR(32) NOT NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    open_incident_count INT NOT NULL DEFAULT 0,
    active_alert_count INT NOT NULL DEFAULT 0,
    failed_verification_count INT NOT NULL DEFAULT 0,
    rollback_ready TINYINT NOT NULL DEFAULT 0,
    signoff_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    snapshot_summary VARCHAR(255) NOT NULL,
    snapshot_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_confidence_project(project_id, snapshot_time),
    KEY idx_release_confidence_plan(plan_id),
    KEY idx_release_confidence_level(confidence_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
ReleaseEvidenceBundleStatus.java
ReleaseSignoffStatus.java
ReleaseSignoffRole.java
ReleaseConfidenceLevel.java
```

### 5.1 ReleaseEvidenceBundleStatus

```text
DRAFT
GENERATED
PUBLISHED
ARCHIVED
```

### 5.2 ReleaseSignoffStatus

```text
PENDING
APPROVED
CONDITIONAL
REJECTED
SKIPPED
```

### 5.3 ReleaseSignoffRole

```text
TECH_OWNER
PRODUCT_OWNER
OPS_OWNER
SECURITY_REVIEWER
QA_REVIEWER
```

### 5.4 ReleaseConfidenceLevel

```text
HIGH
MEDIUM
LOW
CRITICAL
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ReleaseEvidenceBundleEntity.java
ReleaseSignoffRecordEntity.java
ReleaseConfidenceSnapshotEntity.java

ReleaseEvidenceBundleMapper.java
ReleaseSignoffRecordMapper.java
ReleaseConfidenceSnapshotMapper.java
```

DTO 建议：

```text
ReleaseEvidenceBundleResponse.java
GenerateReleaseEvidenceBundleRequest.java

CreateReleaseSignoffRecordRequest.java
UpdateReleaseSignoffRecordRequest.java
ReleaseSignoffRecordResponse.java

ReleaseConfidenceSnapshotResponse.java
ReleaseExecutiveSummaryResponse.java
ReleaseComparisonResponse.java
ReleaseConfidenceTrendResponse.java
ReleaseExecutiveReportResponse.java
```

### 6.1 ReleaseExecutiveSummaryResponse

建议字段：

```text
projectId
planId
releaseLabel
decisionStatus
rolloutStatus
overallOutcome
confidenceScore
confidenceLevel
blockingIssueCount
warningIssueCount
rollbackReady
signoffCompletionRate
openIncidentCount
activeAlertCount
failedVerificationCount
latestPostmortemOutcome
summaryText
lastUpdatedAt
```

### 6.2 ReleaseComparisonResponse

建议字段：

```text
projectId
currentReleaseLabel
baselineReleaseLabel
confidenceScoreDelta
blockingIssueDelta
warningIssueDelta
failedVerificationDelta
rollbackReadyChanged
signoffCompletionDelta
trendSummary
```

---

## 7. 服务设计

新增应用服务：

```text
ReleaseEvidenceCenterService.java
ReleaseSignoffService.java
ReleaseExecutiveSummaryService.java
```

### 7.1 ReleaseEvidenceCenterService

职责：

1. 生成 evidence bundle。
2. 聚合来源：
   - 38C decision / gate evaluation
   - 39A rollout plan / steps / verification
   - 39B rollback drill / audit event / postmortem review
   - 37H-37K incident / retrospective / knowledge quality（按需摘要）
3. 生成 `summary_markdown`。
4. 支持 bundle 状态流转：

```text
DRAFT -> GENERATED -> PUBLISHED -> ARCHIVED
```

### 7.2 ReleaseSignoffService

职责：

1. 初始化默认 sign-off 角色集合。
2. 支持更新 sign-off 状态与备注。
3. 计算 signoff completion rate。
4. 提供 sign-off 缺失项列表。
5. 拒绝对已归档 release 的 sign-off 更新。

### 7.3 ReleaseExecutiveSummaryService

职责：

1. 计算 confidence score。
2. 输出 confidence level。
3. 生成 executive summary。
4. 生成 cross-release comparison。
5. 生成年化 / 最近 N 次 release confidence trend。

建议 confidence score 计算示例：

```text
基础分 100
- blockingIssueCount * 20
- warningIssueCount * 5
- openIncidentCount * 10
- activeAlertCount * 6
- failedVerificationCount * 12
+ rollbackReady ? 8 : -12
+ signoffCompletionRate * 0.1
```

最终截断到：

```text
0 ~ 100
```

confidence level 示例：

```text
>= 85 HIGH
>= 60 MEDIUM
>= 30 LOW
< 30 CRITICAL
```

---

## 8. API 设计

新增 Controller：

```text
ReleaseEvidenceController.java
```

建议端点：

### 8.1 Evidence Bundle

```text
POST   /api/release-rollouts/{planId}/evidence-bundle/generate
GET    /api/release-rollouts/{planId}/evidence-bundle
POST   /api/release-rollouts/{planId}/evidence-bundle/status
GET    /api/release-rollouts/{planId}/executive-report
```

### 8.2 Sign-off

```text
GET    /api/release-rollouts/{planId}/signoffs
POST   /api/release-rollouts/{planId}/signoffs
PUT    /api/release-rollouts/{planId}/signoffs/{signoffId}
POST   /api/release-rollouts/{planId}/signoffs/{signoffId}/status
```

### 8.3 Summary / Comparison / Trend

```text
GET    /api/release-rollouts/{planId}/executive-summary
GET    /api/release-rollouts/{planId}/confidence-snapshot
GET    /api/release-rollouts/{planId}/comparison
GET    /api/release-confidence/trend
```

权限建议：

```text
查看：ADMIN / OWNER / MAINTAINER
编辑：ADMIN / OWNER
发布 bundle / sign-off：ADMIN / OWNER
```

---

## 9. 聚合规则建议

### 9.1 Evidence Bundle 内容建议

```text
Release Label
Go / No-Go Decision
Rollout Summary
Verification Summary
Rollback Drill Result
Audit Timeline Summary
Postmortem Outcome
Open Risks / Residual Risks
Sign-off Status
Confidence Snapshot
```

### 9.2 Executive Summary 聚合规则

若存在以下任一条件，则 summary 应突出标红或强提醒：

1. confidence level = CRITICAL
2. blockingIssueCount > 0
3. failedVerificationCount > 0
4. rollbackReady = false
5. 存在 `REJECTED` sign-off

### 9.3 Cross-release Comparison

默认比较对象：

1. 当前 release vs 同项目上一个 release
2. 若无历史 release，则返回空态，不报错

---

## 10. 前端设计

新增组件建议：

```text
ReleaseEvidenceCenterPanel.vue
ReleaseSignoffPanel.vue
ReleaseExecutiveSummaryPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 ReleaseEvidenceCenterPanel

展示：

1. evidence bundle 状态
2. generate / publish / archive 按钮
3. summary markdown 预览
4. evidence sections 统计
5. executive report 导出按钮

### 10.2 ReleaseSignoffPanel

展示：

1. sign-off 列表
2. role / status / signer / signedAt
3. comment 预览
4. create / edit dialog
5. completion rate 进度展示

### 10.3 ReleaseExecutiveSummaryPanel

展示：

1. confidence score / level
2. rollback readiness
3. blocking / warning / incidents / alerts / failed verification
4. comparison card
5. confidence trend chart
6. executive summary text

UI 要求：

1. 保持中文暗色科技风
2. 复用 `StatusPulse`、`GlowButton`、`MetricTile`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 仍以控制台风格为主，不做营销风页面
4. 对管理摘要使用更紧凑、清晰、可扫描的布局

---

## 11. 后端测试要求

新增：

```text
ReleaseEvidenceSummaryIntegrationTest.java
```

不少于 28 个集成测试，建议覆盖：

1. generate evidence bundle success
2. regenerate evidence bundle success
3. publish evidence bundle success
4. archive evidence bundle success
5. invalid evidence bundle status transition reject
6. create signoff record success
7. update signoff record success
8. approve signoff success
9. conditional signoff success
10. reject signoff success
11. signoff completion rate calculation correct
12. duplicate signoff role reject
13. confidence snapshot generated
14. confidence level high
15. confidence level medium
16. confidence level low
17. confidence level critical
18. rollbackReady affects score
19. failed verification affects score
20. rejected signoff appears in executive summary
21. comparison with previous release success
22. no previous release returns empty comparison
23. confidence trend returns ordered snapshots
24. executive report markdown export success
25. unauthorized access reject
26. non-owner update reject
27. archived bundle blocks update
28. signoff data appears in bundle markdown

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/release-evidence-summary.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 executive summary panel
2. evidence center panel renders
3. signoff panel renders
4. generate evidence bundle button visible
5. signoff dialog works
6. confidence trend / comparison 区块可见
7. executive report export button visible
8. no JS errors on page load

如果测试环境没有 seeded release rollout：

1. 显式断言空态
2. 不把“无 release 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-39c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 release evidence / signoff / confidence 表说明
3. ReleaseEvidenceCenterService 设计说明
4. ReleaseSignoffService 设计说明
5. ReleaseExecutiveSummaryService 设计说明
6. ReleaseEvidenceCenterPanel 说明
7. ReleaseSignoffPanel 说明
8. ReleaseExecutiveSummaryPanel 说明
9. Evidence / Sign-off 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40A

---

## 14. 验收标准

必须全部满足：

1. release_evidence_bundle / release_signoff_record / release_confidence_snapshot 三张表已落库
2. evidence bundle 可生成 / 发布 / 归档
3. sign-off 可创建 / 编辑 / 状态流转
4. executive summary 可聚合 decision / rollout / rollback / postmortem
5. comparison 与 trend 能返回结构化结果
6. executive report markdown 可导出
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无 release 前置条件显式降级处理

---

## 15. 完成后的价值

完成 39C 后，平台将从：

```text
能管理 rollout / rollback / audit
```

升级为：

```text
能输出统一 release evidence、管理层摘要、签字状态与 confidence 趋势
```

这会让平台在“工程可执行”之外，进一步具备“对外可汇报、对内可审计、跨 release 可比较”的能力。

---

## 16. 后续建议

Milestone 39C 完成后，建议进入：

```text
Milestone 40A: Production Trial Expansion & Multi-Project Release Governance
```

重点可包括：

1. 多项目 release portfolio dashboard
2. organization-level release confidence ranking
3. multi-project sign-off aggregation
4. cross-project incident risk heatmap
5. rollout governance baseline 模板化

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 39C。

文档路径：
docs/milestone-39c-production-rollout-evidence-center-executive-release-summary.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 38C Beta Release Gate、39A Controlled Rollout、39B Rollback Drill / Release Audit / Postmortem 基础上，新增 Release Evidence Center 与 Executive Release Summary。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动操作生产基础设施。
6. 不要自动代替人工签字或审批。
7. 不要自动修改已有 release / rollout / audit 原始记录。
8. 不要自动关闭 incident / alert / feedback。
9. evidence bundle 只聚合结构化数据，不调用外部系统。
10. 不要破坏 1-39B 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V43__init_release_evidence_summary_tables.sql。
2. 新增 release_evidence_bundle / release_signoff_record / release_confidence_snapshot 三张表。
3. 新增 4 个枚举：
   - ReleaseEvidenceBundleStatus
   - ReleaseSignoffStatus
   - ReleaseSignoffRole
   - ReleaseConfidenceLevel
4. 新增实体、Mapper、DTO。
5. 新增 ReleaseEvidenceCenterService。
6. 新增 ReleaseSignoffService。
7. 新增 ReleaseExecutiveSummaryService。
8. 新增 API：
   - evidence bundle generate / get / status
   - signoff CRUD / status
   - executive summary / confidence snapshot / comparison / trend / report
9. 前端新增：
   - ReleaseEvidenceCenterPanel.vue
   - ReleaseSignoffPanel.vue
   - ReleaseExecutiveSummaryPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 28 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-39c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 release evidence / signoff / confidence 表说明
3. ReleaseEvidenceCenterService 设计说明
4. ReleaseSignoffService 设计说明
5. ReleaseExecutiveSummaryService 设计说明
6. ReleaseEvidenceCenterPanel 说明
7. ReleaseSignoffPanel 说明
8. ReleaseExecutiveSummaryPanel 说明
9. Evidence / Sign-off 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40A

现在开始实现，不要只给计划。
```
