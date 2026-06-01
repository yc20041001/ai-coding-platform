# Milestone 45B: Governance Benchmark Adoption Tracking & Cross-Team Improvement Loop

## 1. 背景

截至 Milestone 45A，平台已经完成了一条非常完整的治理主线，并进一步进入了组织级 benchmark 阶段：

```text
40A
  Multi-project Release Governance

40B
  Organization Policy / Guardrail / Drift

40C
  Recommendation Workflow / Waiver Management

41A
  SLA / Escalation / Ownership Health

41B
  Capacity Forecast / Predictive Risk / Backlog Health

41C
  Simulation / What-if Planning / Policy Tuning

42A
  Execution Automation / Recommendation Playbooks

42B
  Governance Knowledge Base / Pattern Library / Remediation Recipes

42C
  Effectiveness Analytics / Recipe Optimization Loop

43A
  Governance Copilot Workspace / Guided Operations Console

43B
  Governance Operator Memory / Learning Loop / Guided Remediation Reuse

43C
  Governance Adaptive Guidance / Operator Feedback / Copilot Tuning Loop

44A
  Governance Autonomous Draft Planning / Safe Assistive Actions

44B
  Governance Outcome Review / Draft Adoption Tracking / Assistive Quality Evaluation

44C
  Governance Assistive Planning Optimization / Outcome-Driven Draft Tuning

45A
  Governance Portfolio Benchmarking / Cross-Org Best Practice Alignment
```

现在平台已经能做到：

```text
识别 top / bottom project
输出跨项目 benchmark
识别 best-practice candidate
给出 alignment gap 和 maturity scorecard
```

但这里仍然缺少一个关键闭环：

```text
最佳实践被推荐之后，目标项目到底有没有采用？
采用之后，成熟度是否真的提升了？
某次 alignment campaign 是否真的带来了 uplift？
哪类对齐建议常被接受，哪类常被搁置？
不同团队之间，改进节奏是否可追踪、可量化？
```

也就是说，平台现在已经有：

```text
跨项目 benchmark 和对齐建议
```

但还缺少：

```text
对 benchmark 建议后续 adoption、执行进度和跨团队改进效果的持续跟踪
```

Milestone 45B 的目标就是新增：

```text
Governance Benchmark Adoption Tracking & Cross-Team Improvement Loop
```

让平台从：

```text
知道谁更强、谁落后、该参考谁
```

升级为：

```text
知道这些 best practice 是否真的被采用、 adoption 之后是否带来 uplift、哪些改进 campaign 有效
```

---

## 2. 总目标

实现 benchmark adoption 与跨团队改进闭环：

1. 新增 Benchmark Adoption Record 数据模型。
2. 新增 Cross-Team Improvement Campaign 数据模型。
3. 新增 Governance Uplift Measurement Snapshot 数据模型。
4. 支持记录 target project 对 best practice alignment 的 adoption 状态。
5. 支持跟踪 alignment recommendation 的执行进度。
6. 支持组织级 improvement campaign。
7. 支持衡量 adoption 前后 maturity / package quality / workflow completion 的 uplift。
8. 支持 cross-team improvement dashboard。
9. 支持导出 Markdown Benchmark Adoption Report。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
知道哪些实践值得对齐
```

升级为：

```text
知道哪些实践真的被采用、 adoption 之后有没有形成跨团队提升
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
4. 不自动批准 waiver。
5. 不自动完成 recommendation。
6. 不自动分配 owner。
7. 不自动把 source project 的配置/模板直接同步到 target project。
8. adoption tracking 只记录状态、进度、结果和效果，不自动应用 benchmark 建议。
9. campaign 只做跟踪、聚合、统计，不自动发起真实治理动作。
10. 不调用真实 AI 自动生成 adoption 结论。
11. 不破坏 1-45A 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 adoption / campaign / uplift 三张表。
2. 记录 alignment adoption 状态与进度。
3. 记录跨团队改进活动和阶段。
4. 统计 adoption 后的 uplift 指标。
5. 导出 report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V60__init_governance_benchmark_adoption_tables.sql
```

### 4.1 governance_benchmark_adoption_record

```sql
CREATE TABLE governance_benchmark_adoption_record (
    id BIGINT PRIMARY KEY,
    alignment_item_id BIGINT NOT NULL,
    source_project_id BIGINT NOT NULL,
    source_project_name VARCHAR(255) NOT NULL,
    target_project_id BIGINT NOT NULL,
    target_project_name VARCHAR(255) NOT NULL,
    adoption_status VARCHAR(32) NOT NULL,
    adoption_progress_percent INT NOT NULL DEFAULT 0,
    target_owner_name VARCHAR(128) NULL,
    planned_due_at DATETIME NULL,
    adopted_at DATETIME NULL,
    blocked_reason_text TEXT NULL,
    summary_text TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_benchmark_adoption_alignment(alignment_item_id),
    KEY idx_governance_benchmark_adoption_target(target_project_id, adoption_status),
    KEY idx_governance_benchmark_adoption_source(source_project_id, adoption_status)
);
```

### 4.2 governance_cross_team_improvement_campaign

```sql
CREATE TABLE governance_cross_team_improvement_campaign (
    id BIGINT PRIMARY KEY,
    campaign_key VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    campaign_status VARCHAR(32) NOT NULL,
    benchmark_window VARCHAR(32) NOT NULL,
    scope_summary_text TEXT NULL,
    goal_text TEXT NULL,
    participating_project_count INT NOT NULL DEFAULT 0,
    adoption_target_count INT NOT NULL DEFAULT 0,
    completion_percent INT NOT NULL DEFAULT 0,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_cross_team_improvement_campaign_key(campaign_key),
    KEY idx_governance_cross_team_improvement_campaign_status(campaign_status, benchmark_window)
);
```

### 4.3 governance_uplift_measurement_snapshot

```sql
CREATE TABLE governance_uplift_measurement_snapshot (
    id BIGINT PRIMARY KEY,
    target_project_id BIGINT NOT NULL,
    target_project_name VARCHAR(255) NOT NULL,
    adoption_record_id BIGINT NULL,
    campaign_id BIGINT NULL,
    measurement_window VARCHAR(32) NOT NULL,
    maturity_score_before DECIMAL(10,2) NOT NULL DEFAULT 0,
    maturity_score_after DECIMAL(10,2) NOT NULL DEFAULT 0,
    package_quality_before DECIMAL(10,2) NOT NULL DEFAULT 0,
    package_quality_after DECIMAL(10,2) NOT NULL DEFAULT 0,
    workflow_completion_before DECIMAL(10,2) NOT NULL DEFAULT 0,
    workflow_completion_after DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_level VARCHAR(32) NOT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_uplift_measurement_snapshot_target(target_project_id, captured_at),
    KEY idx_governance_uplift_measurement_snapshot_campaign(campaign_id, uplift_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceBenchmarkAdoptionStatus.java
GovernanceImprovementCampaignStatus.java
GovernanceUpliftLevel.java
GovernanceAdoptionBlockerType.java
GovernanceImprovementWindow.java
```

### 5.1 GovernanceBenchmarkAdoptionStatus

```text
PROPOSED
IN_PROGRESS
ADOPTED
BLOCKED
ABANDONED
```

### 5.2 GovernanceImprovementCampaignStatus

```text
PLANNED
ACTIVE
AT_RISK
COMPLETED
ARCHIVED
```

### 5.3 GovernanceUpliftLevel

```text
STRONG
MODERATE
MINIMAL
NEGATIVE
```

### 5.4 GovernanceAdoptionBlockerType

```text
OWNER_CAPACITY
LOW_CONFIDENCE
MISSING_CONTEXT
LOW_PRIORITY
DEPENDENCY_BLOCKED
```

### 5.5 GovernanceImprovementWindow

```text
DAY_14
DAY_30
DAY_60
DAY_90
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceBenchmarkAdoptionRecordEntity.java
GovernanceCrossTeamImprovementCampaignEntity.java
GovernanceUpliftMeasurementSnapshotEntity.java

GovernanceBenchmarkAdoptionRecordMapper.java
GovernanceCrossTeamImprovementCampaignMapper.java
GovernanceUpliftMeasurementSnapshotMapper.java
```

DTO 建议：

```text
CreateGovernanceBenchmarkAdoptionRecordRequest.java
UpdateGovernanceBenchmarkAdoptionRecordRequest.java
GovernanceBenchmarkAdoptionRecordResponse.java

CreateGovernanceImprovementCampaignRequest.java
UpdateGovernanceImprovementCampaignRequest.java
GovernanceCrossTeamImprovementCampaignResponse.java

GovernanceUpliftMeasurementSnapshotResponse.java
GovernanceBenchmarkAdoptionDashboardResponse.java
GovernanceBenchmarkAdoptionReportResponse.java
```

### 6.1 GovernanceBenchmarkAdoptionDashboardResponse

建议字段：

```text
totalAdoptionRecords
adoptedCount
blockedCount
adoptionRate
activeCampaignCount
topUpliftProjects
blockedProjects
campaignProgressSummary
latestUpliftSnapshots
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceBenchmarkAdoptionService.java
GovernanceImprovementCampaignService.java
GovernanceUpliftMeasurementService.java
```

### 7.1 GovernanceBenchmarkAdoptionService

职责：

1. 记录 alignment item 的 adoption 状态与进度。
2. 支持 adoption record CRUD / 状态流转。
3. 聚合 adopted / blocked / abandoned 比例。
4. 记录 blocker 原因、owner、目标时间。

状态流转建议：

```text
PROPOSED -> IN_PROGRESS -> ADOPTED
PROPOSED / IN_PROGRESS -> BLOCKED
PROPOSED / IN_PROGRESS -> ABANDONED
BLOCKED -> IN_PROGRESS
ADOPTED -> 保持终态
```

### 7.2 GovernanceImprovementCampaignService

职责：

1. 创建与更新跨团队 improvement campaign。
2. 聚合 participating projects 与 adoption target count。
3. 计算 campaign completion percent。
4. 支持 campaign 状态流转。

状态流转建议：

```text
PLANNED -> ACTIVE
ACTIVE -> AT_RISK
ACTIVE / AT_RISK -> COMPLETED
COMPLETED -> ARCHIVED
```

### 7.3 GovernanceUpliftMeasurementService

职责：

1. 计算 adoption 前后 uplift。
2. 聚合 maturity / package quality / workflow completion 的 before/after 差异。
3. 计算 upliftScore。
4. 生成 top uplift / low uplift 项目。

建议 upliftScore 公式：

```text
upliftScore =
  (maturityScoreAfter - maturityScoreBefore) * 0.4
  + (packageQualityAfter - packageQualityBefore) * 0.3
  + (workflowCompletionAfter - workflowCompletionBefore) * 0.3
```

建议 upliftLevel：

```text
>= 15 STRONG
>= 5 MODERATE
>= 0 MINIMAL
< 0 NEGATIVE
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceBenchmarkAdoptionController.java
```

建议端点：

### 8.1 Adoption Records

```text
POST   /api/governance-benchmark-adoption/records
GET    /api/governance-benchmark-adoption/records
GET    /api/governance-benchmark-adoption/records/{recordId}
PUT    /api/governance-benchmark-adoption/records/{recordId}
POST   /api/governance-benchmark-adoption/records/{recordId}/status
```

### 8.2 Improvement Campaign

```text
POST   /api/governance-benchmark-adoption/campaigns
GET    /api/governance-benchmark-adoption/campaigns
GET    /api/governance-benchmark-adoption/campaigns/{campaignId}
PUT    /api/governance-benchmark-adoption/campaigns/{campaignId}
POST   /api/governance-benchmark-adoption/campaigns/{campaignId}/status
```

### 8.3 Uplift / Dashboard / Report

```text
POST   /api/governance-benchmark-adoption/uplift/refresh
GET    /api/governance-benchmark-adoption/uplift
GET    /api/governance-benchmark-adoption/dashboard
GET    /api/governance-benchmark-adoption/report
```

权限建议：

```text
查看：ADMIN
创建 / 更新 adoption / campaign：ADMIN
刷新 uplift：ADMIN
```

---

## 9. Adoption / Improvement 规则建议

### 9.1 Adoption Rate

建议公式：

```text
adoptionRate = ADOPTED / totalAdoptionRecords
blockedRate = BLOCKED / totalAdoptionRecords
```

### 9.2 Campaign Completion

建议公式：

```text
completionPercent =
  adoptedTargetCount / max(1, adoptionTargetCount) * 100
```

### 9.3 Uplift Measurement

至少对比：

```text
maturity score
package quality
workflow completion
```

### 9.4 High-value Improvement Signal

若某 campaign：

```text
completionPercent >= 70
avg upliftScore >= 10
```

则可视为：

```text
有效 cross-team improvement campaign
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceBenchmarkAdoptionPanel.vue
GovernanceImprovementCampaignPanel.vue
GovernanceUpliftMeasurementPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceBenchmarkAdoptionPanel

展示：

1. adoption record 列表
2. source -> target project
3. status / progress / blocker / dueAt
4. adoption rate 指标卡

### 10.2 GovernanceImprovementCampaignPanel

展示：

1. campaign 列表
2. status / participating project count / target count
3. completion percent
4. active / at-risk / completed 标签

### 10.3 GovernanceUpliftMeasurementPanel

展示：

1. uplift snapshot 列表
2. before / after 指标对比
3. upliftScore / upliftLevel
4. top uplift / negative uplift 标签

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 要突出 adoption progress 和 uplift outcome
4. campaign 与 uplift 之间的关系要清楚可读

---

## 11. 后端测试要求

新增：

```text
GovernanceBenchmarkAdoptionIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. create adoption record success
2. update adoption record success
3. proposed -> in_progress
4. in_progress -> adopted
5. in_progress -> blocked
6. blocked -> in_progress
7. in_progress -> abandoned
8. invalid adoption transition reject
9. blocker reason stored
10. adoption progress percent stored
11. create campaign success
12. update campaign success
13. planned -> active
14. active -> at_risk
15. at_risk -> completed
16. completed -> archived
17. invalid campaign transition reject
18. refresh uplift success
19. uplift score computed
20. strong uplift level computed
21. moderate uplift level computed
22. minimal uplift level computed
23. negative uplift level computed
24. dashboard returns adoptionRate
25. dashboard returns blockedCount
26. dashboard returns activeCampaignCount
27. dashboard returns topUpliftProjects
28. dashboard returns blockedProjects
29. report export markdown success
30. empty data returns empty dashboard safely
31. unauthorized access reject
32. non-admin mutation reject
33. adopted projects increase completion percent
34. blocked projects reduce campaign confidence
35. higher after scores produce positive uplift
36. refresh uplift idempotent for same snapshot window

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-benchmark-adoption.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 benchmark adoption 面板
2. improvement campaign 面板可见
3. uplift measurement 面板可见
4. adoption rate 指标卡可见
5. completion percent 标签可见
6. uplift level 标签可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 adoption / campaign / uplift 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-45b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 benchmark adoption / improvement campaign / uplift snapshot 表说明
3. GovernanceBenchmarkAdoptionService 设计说明
4. GovernanceImprovementCampaignService 设计说明
5. GovernanceUpliftMeasurementService 设计说明
6. GovernanceBenchmarkAdoptionPanel 说明
7. GovernanceImprovementCampaignPanel 说明
8. GovernanceUpliftMeasurementPanel 说明
9. Benchmark Adoption / Cross-Team Improvement 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45C

---

## 14. 验收标准

必须全部满足：

1. governance_benchmark_adoption_record / governance_cross_team_improvement_campaign / governance_uplift_measurement_snapshot 三张表已落库
2. adoption record 可 CRUD / 状态流转
3. campaign 可 CRUD / 状态流转
4. uplift 可刷新 / 查询
5. dashboard / report 可导出
6. adoption / campaign / uplift 逻辑可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 45B 后，平台将从：

```text
知道哪些项目值得参考、哪些实践值得对齐
```

升级为：

```text
能持续跟踪这些 best practice 是否真的被采用，以及 adoption 后是否带来了跨团队 uplift
```

这一步会让治理平台从“benchmark 观察系统”进一步升级成“跨团队改进跟踪系统”。

---

## 16. 后续建议

Milestone 45B 完成后，建议进入：

```text
Milestone 45C: Governance Portfolio Uplift Optimization & Benchmark Evolution Loop
```

重点可包括：

1. benchmark drift trend
2. campaign effectiveness ranking
3. uplift-driven best practice evolution
4. maturity improvement campaign analytics
5. cross-org governance progress map

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 45B。

文档路径：
docs/milestone-45b-governance-benchmark-adoption-tracking-cross-team-improvement-loop.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 45A portfolio benchmarking / cross-org best-practice alignment 基础上，新增 benchmark adoption tracking 与 cross-team improvement loop。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动把 source project 的最佳实践直接同步到 target project。
10. adoption / campaign / uplift 只做跟踪、统计、状态和效果衡量，不做真实自动应用。
11. 不调用真实 AI 自动生成 adoption 结论。
12. 不要破坏 1-45A 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 45A benchmark / alignment / maturity 数据结构，不要重复造概念。
17. adoption / campaign / uplift 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V60__init_governance_benchmark_adoption_tables.sql。
2. 新增 governance_benchmark_adoption_record / governance_cross_team_improvement_campaign / governance_uplift_measurement_snapshot 三张表。
3. 新增 5 个枚举：
   - GovernanceBenchmarkAdoptionStatus
   - GovernanceImprovementCampaignStatus
   - GovernanceUpliftLevel
   - GovernanceAdoptionBlockerType
   - GovernanceImprovementWindow
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceBenchmarkAdoptionService。
6. 新增 GovernanceImprovementCampaignService。
7. 新增 GovernanceUpliftMeasurementService。
8. 新增 API：
   - adoption records CRUD / status
   - improvement campaigns CRUD / status
   - uplift refresh / list
   - dashboard / report
9. 前端新增：
   - GovernanceBenchmarkAdoptionPanel.vue
   - GovernanceImprovementCampaignPanel.vue
   - GovernanceUpliftMeasurementPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-45b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 benchmark adoption / improvement campaign / uplift snapshot 表说明
3. GovernanceBenchmarkAdoptionService 设计说明
4. GovernanceImprovementCampaignService 设计说明
5. GovernanceUpliftMeasurementService 设计说明
6. GovernanceBenchmarkAdoptionPanel 说明
7. GovernanceImprovementCampaignPanel 说明
8. GovernanceUpliftMeasurementPanel 说明
9. Benchmark Adoption / Cross-Team Improvement 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45C

现在开始实现，不要只给计划。
```
