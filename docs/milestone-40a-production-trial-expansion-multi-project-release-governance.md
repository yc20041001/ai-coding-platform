# Milestone 40A: Production Trial Expansion & Multi-Project Release Governance

## 1. 背景

截至 Milestone 39C，平台已经具备完整的单项目 release 治理能力：

```text
38C
  Beta Release Gate / Go-No-Go Decision

39A
  Controlled Rollout

39B
  Rollback Drill / Release Audit / Postmortem

39C
  Release Evidence Center / Executive Summary / Confidence Snapshot
```

对于单个项目，系统已经能回答：

```text
这个版本能不能发？
怎么发？
怎么观察？
怎么回滚？
上线后怎么复盘？
管理层怎么看这次 release 的证据与风险？
```

但当试用和 production rollout 扩展到多个项目后，又出现了新的治理需求：

```text
多个项目里，谁最接近可以正式扩大试用？
哪些项目的 release confidence 在下降？
哪个项目的 incident 风险最高？
哪些项目的 sign-off 完整度不足？
有没有跨项目可复用的 rollout baseline？
```

换句话说，平台已经具备：

```text
单项目 release 管理能力
```

但还不具备：

```text
多项目 release portfolio 治理能力
```

Milestone 40A 的目标就是新增：

```text
Production Trial Expansion & Multi-Project Release Governance
```

让平台从：

```text
围绕单个项目做 release 管理
```

升级为：

```text
围绕多个项目做统一的 production trial 扩展治理与跨项目风险管理
```

---

## 2. 总目标

实现多项目 release governance 中心：

1. 新增 Release Portfolio Snapshot 数据模型。
2. 新增 Cross-project Governance Baseline Template 数据模型。
3. 新增 Release Risk Heatmap Snapshot 数据模型。
4. 支持聚合多个项目的 38C / 39A / 39B / 39C 数据。
5. 支持生成 organization-level release confidence ranking。
6. 支持展示多项目 sign-off completion 排名与缺失项。
7. 支持展示跨项目 incident / verification / rollback 风险热力图。
8. 支持维护 rollout governance baseline template。
9. 支持导出 Multi-project Release Governance Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
每个项目单独看 release 状态
```

升级为：

```text
统一查看所有项目的 release readiness、风险热力分布和扩展优先级
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动发布到生产环境。
4. 不自动操作 Docker / Kubernetes / 云服务。
5. 不自动修改已有 release / rollout / audit / evidence 数据。
6. 不自动创建项目成员、签字或审批记录。
7. 不自动关闭 incident / alert / feedback。
8. 不调用真实 AI 自动生成治理结论。
9. governance baseline 只作为模板，不自动应用到 rollout plan。
10. 不破坏 1-39C 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 portfolio / baseline / heatmap snapshot 表。
2. 聚合已有 release confidence / sign-off / incident / verification / rollback 数据。
3. 新增跨项目 dashboard、ranking、heatmap、template 管理。
4. 新增 Markdown summary 导出。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V44__init_multi_project_release_governance_tables.sql
```

### 4.1 release_portfolio_snapshot

```sql
CREATE TABLE release_portfolio_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    latest_release_label VARCHAR(128) NULL,
    confidence_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    confidence_level VARCHAR(32) NOT NULL,
    rollout_status VARCHAR(32) NULL,
    decision_status VARCHAR(32) NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    open_incident_count INT NOT NULL DEFAULT 0,
    active_alert_count INT NOT NULL DEFAULT 0,
    failed_verification_count INT NOT NULL DEFAULT 0,
    rollback_ready TINYINT NOT NULL DEFAULT 0,
    signoff_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    portfolio_rank INT NULL,
    expansion_recommendation VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_portfolio_snapshot_date(snapshot_date, confidence_score),
    KEY idx_release_portfolio_snapshot_project(project_id, snapshot_date),
    KEY idx_release_portfolio_snapshot_rank(snapshot_date, portfolio_rank)
);
```

### 4.2 governance_baseline_template

```sql
CREATE TABLE governance_baseline_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    template_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    default_signoff_roles_json JSON NULL,
    default_verification_rules_json JSON NULL,
    default_rollback_requirements_json JSON NULL,
    default_confidence_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_baseline_template(template_key),
    KEY idx_governance_baseline_template_scope(template_scope, enabled)
);
```

### 4.3 release_risk_heatmap_snapshot

```sql
CREATE TABLE release_risk_heatmap_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    risk_category VARCHAR(64) NOT NULL,
    risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    risk_level VARCHAR(32) NOT NULL,
    source_count INT NOT NULL DEFAULT 0,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_risk_heatmap_date(snapshot_date, risk_category),
    KEY idx_release_risk_heatmap_project(project_id, snapshot_date),
    KEY idx_release_risk_heatmap_level(snapshot_date, risk_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
ReleaseExpansionRecommendation.java
GovernanceTemplateScope.java
ReleaseRiskCategory.java
ReleaseRiskLevel.java
```

### 5.1 ReleaseExpansionRecommendation

```text
EXPAND_NOW
EXPAND_WITH_GUARDRAILS
HOLD
BLOCK
```

### 5.2 GovernanceTemplateScope

```text
GLOBAL
PROJECT_TYPE
PROJECT_OVERRIDE
```

### 5.3 ReleaseRiskCategory

```text
INCIDENT
ALERT
VERIFICATION
ROLLOUT
SIGNOFF
COST
PR_QUALITY
```

### 5.4 ReleaseRiskLevel

```text
LOW
MEDIUM
HIGH
CRITICAL
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ReleasePortfolioSnapshotEntity.java
GovernanceBaselineTemplateEntity.java
ReleaseRiskHeatmapSnapshotEntity.java

ReleasePortfolioSnapshotMapper.java
GovernanceBaselineTemplateMapper.java
ReleaseRiskHeatmapSnapshotMapper.java
```

DTO 建议：

```text
ReleasePortfolioSnapshotResponse.java
ReleasePortfolioDashboardResponse.java
ReleasePortfolioRankingResponse.java

GovernanceBaselineTemplateResponse.java
CreateGovernanceBaselineTemplateRequest.java
UpdateGovernanceBaselineTemplateRequest.java

ReleaseRiskHeatmapCellResponse.java
ReleaseRiskHeatmapResponse.java

MultiProjectGovernanceSummaryResponse.java
```

### 6.1 ReleasePortfolioDashboardResponse

建议字段：

```text
snapshotDate
projectCount
highConfidenceCount
mediumConfidenceCount
lowConfidenceCount
criticalConfidenceCount
expandNowCount
expandWithGuardrailsCount
holdCount
blockCount
averageConfidenceScore
topProjects
bottomProjects
```

### 6.2 ReleasePortfolioRankingResponse

建议字段：

```text
projectId
projectName
latestReleaseLabel
confidenceScore
confidenceLevel
portfolioRank
expansionRecommendation
blockingIssueCount
warningIssueCount
rollbackReady
signoffCompletionRate
summaryText
```

---

## 7. 服务设计

新增应用服务：

```text
ReleasePortfolioGovernanceService.java
GovernanceBaselineTemplateService.java
ReleaseRiskHeatmapService.java
```

### 7.1 ReleasePortfolioGovernanceService

职责：

1. 聚合多个项目的最新 release confidence snapshot。
2. 生成 portfolio snapshot。
3. 按 confidence score 排名。
4. 给出 expansion recommendation。
5. 生成 multi-project governance summary。

建议 expansion recommendation 规则示例：

```text
confidence >= 85 且无 blocking issue 且 rollbackReady=true -> EXPAND_NOW
confidence >= 65 且 warning 可控 -> EXPAND_WITH_GUARDRAILS
confidence >= 40 但存在明显风险 -> HOLD
confidence < 40 或 blocking issue > 0 -> BLOCK
```

### 7.2 GovernanceBaselineTemplateService

职责：

1. 管理 baseline template 的 CRUD。
2. 保存默认 sign-off / verification / rollback / confidence policy 模板。
3. 供后续 rollout plan 初始化时参考。
4. 支持按 scope 过滤模板。

### 7.3 ReleaseRiskHeatmapService

职责：

1. 按项目聚合 risk category。
2. 生成 heatmap snapshot。
3. 输出 risk score / risk level。
4. 支持跨项目 heatmap 查询。

risk score 示例来源：

```text
incident count
active alert count
failed verification count
rollback not ready
signoff incomplete
cost alerts
pr quality warnings
```

---

## 8. API 设计

新增 Controller：

```text
ReleaseGovernanceController.java
```

建议端点：

### 8.1 Portfolio

```text
POST   /api/release-governance/portfolio/refresh
GET    /api/release-governance/portfolio/dashboard
GET    /api/release-governance/portfolio/ranking
GET    /api/release-governance/summary
```

### 8.2 Baseline Template

```text
POST   /api/release-governance/baseline-templates
GET    /api/release-governance/baseline-templates
GET    /api/release-governance/baseline-templates/{templateId}
PUT    /api/release-governance/baseline-templates/{templateId}
POST   /api/release-governance/baseline-templates/{templateId}/status
```

### 8.3 Heatmap

```text
POST   /api/release-governance/heatmap/refresh
GET    /api/release-governance/heatmap
```

权限建议：

```text
查看：ADMIN
编辑 baseline：ADMIN
refresh snapshot：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Portfolio Ranking

默认按：

```text
confidence_score DESC
blocking_issue_count ASC
rollback_ready DESC
signoff_completion_rate DESC
```

### 9.2 Heatmap Risk Score

每个 risk category 可分别计算，例如：

```text
INCIDENT: open_incident_count * 20
ALERT: active_alert_count * 10
VERIFICATION: failed_verification_count * 15
ROLLOUT: rollbackReady=false ? 25 : 0
SIGNOFF: (100 - signoffCompletionRate) * 0.2
COST: costAlertCount * 12
PR_QUALITY: qualityWarnCount * 8
```

最终统一归一到：

```text
0 ~ 100
```

### 9.3 Summary 输出建议

Multi-project summary 建议至少包含：

```text
今日快照时间
项目总数
适合扩大的项目数
需要观望的项目数
阻塞项目数
风险最高的前 3 个项目
confidence 上升最快 / 下降最快的项目
```

---

## 10. 前端设计

新增组件建议：

```text
ReleasePortfolioDashboardPanel.vue
ReleaseGovernanceBaselinePanel.vue
ReleaseRiskHeatmapPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 ReleasePortfolioDashboardPanel

展示：

1. portfolio 概览指标卡
2. top projects / bottom projects
3. ranking table
4. expansion recommendation tag
5. summary text

### 10.2 ReleaseGovernanceBaselinePanel

展示：

1. baseline template 列表
2. create / edit dialog
3. scope / enabled / notes
4. 默认 sign-off / verification / rollback / confidence policy 摘要

### 10.3 ReleaseRiskHeatmapPanel

展示：

1. 项目 x 风险类别 heatmap
2. risk score / risk level 色块
3. hover 或 drawer 查看 detail
4. refresh snapshot 按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 以控制台风格为主，适合扫读、比较和排序
4. heatmap 要强调色阶清晰，不要花哨

---

## 11. 后端测试要求

新增：

```text
ReleaseGovernancePortfolioIntegrationTest.java
```

不少于 30 个集成测试，建议覆盖：

1. refresh portfolio snapshot success
2. ranking ordered by confidence score
3. expansion recommendation = EXPAND_NOW
4. expansion recommendation = EXPAND_WITH_GUARDRAILS
5. expansion recommendation = HOLD
6. expansion recommendation = BLOCK
7. dashboard counts correct
8. average confidence score correct
9. top projects list returns
10. bottom projects list returns
11. create baseline template success
12. update baseline template success
13. disable baseline template success
14. duplicate templateKey reject
15. list baseline by scope works
16. refresh heatmap success
17. heatmap returns all categories
18. risk level low
19. risk level medium
20. risk level high
21. risk level critical
22. summary response returns riskiest projects
23. summary response returns improving / declining projects
24. unauthorized access reject
25. non-admin edit reject
26. empty project set returns empty dashboard
27. confidence trend source integrated into summary
28. rollback readiness affects recommendation
29. signoff completion affects risk score
30. baseline template notes persisted

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/release-governance-portfolio.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 portfolio dashboard
2. ranking table renders
3. heatmap panel renders
4. baseline template panel renders
5. create baseline template dialog works
6. expansion recommendation tag visible
7. refresh buttons visible
8. no JS errors on page load

如果测试环境没有 seeded release 数据：

1. 显式断言空态
2. 不把“无 portfolio 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-40a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 multi-project governance 表说明
3. ReleasePortfolioGovernanceService 设计说明
4. GovernanceBaselineTemplateService 设计说明
5. ReleaseRiskHeatmapService 设计说明
6. ReleasePortfolioDashboardPanel 说明
7. ReleaseGovernanceBaselinePanel 说明
8. ReleaseRiskHeatmapPanel 说明
9. Multi-project governance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40B

---

## 14. 验收标准

必须全部满足：

1. release_portfolio_snapshot / governance_baseline_template / release_risk_heatmap_snapshot 三张表已落库
2. portfolio dashboard 可聚合多个项目
3. ranking 可按 confidence score 排序
4. heatmap 可展示跨项目风险分布
5. baseline template 可创建 / 编辑 / 启停
6. governance summary 可导出或结构化返回
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 40A 后，平台将从：

```text
单项目 release 治理
```

升级为：

```text
多项目 production trial 扩展治理与 release portfolio 管理
```

这会让平台不只是“一个项目的发布控制台”，而开始具备“组织级 release governance 中枢”的雏形。

---

## 16. 后续建议

Milestone 40A 完成后，建议进入：

```text
Milestone 40B: Organization-level Trial Policy & Release Guardrail Automation
```

重点可包括：

1. 组织级 guardrail policy
2. baseline 自动建议
3. cross-project alerting thresholds
4. portfolio drift detection
5. release governance recommendations

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 40A。

文档路径：
docs/milestone-40a-production-trial-expansion-multi-project-release-governance.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 38C-39C 单项目 release / rollout / rollback / evidence / summary 基础上，扩展到多项目 release portfolio 治理。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动发布到生产环境。
6. 不要自动修改已有 release / rollout / audit / evidence 原始记录。
7. 不要自动关闭 incident / alert / feedback。
8. 不要调用真实 AI 自动生成治理结论。
9. governance baseline 只作为模板，不自动应用到 rollout plan。
10. 不要破坏 1-39C 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V44__init_multi_project_release_governance_tables.sql。
2. 新增 release_portfolio_snapshot / governance_baseline_template / release_risk_heatmap_snapshot 三张表。
3. 新增 4 个枚举：
   - ReleaseExpansionRecommendation
   - GovernanceTemplateScope
   - ReleaseRiskCategory
   - ReleaseRiskLevel
4. 新增实体、Mapper、DTO。
5. 新增 ReleasePortfolioGovernanceService。
6. 新增 GovernanceBaselineTemplateService。
7. 新增 ReleaseRiskHeatmapService。
8. 新增 API：
   - portfolio refresh / dashboard / ranking / summary
   - baseline template CRUD / status
   - heatmap refresh / get
9. 前端新增：
   - ReleasePortfolioDashboardPanel.vue
   - ReleaseGovernanceBaselinePanel.vue
   - ReleaseRiskHeatmapPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 30 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-40a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 multi-project governance 表说明
3. ReleasePortfolioGovernanceService 设计说明
4. GovernanceBaselineTemplateService 设计说明
5. ReleaseRiskHeatmapService 设计说明
6. ReleasePortfolioDashboardPanel 说明
7. ReleaseGovernanceBaselinePanel 说明
8. ReleaseRiskHeatmapPanel 说明
9. Multi-project governance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40B

现在开始实现，不要只给计划。
```
