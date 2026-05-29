# Milestone 40B: Organization-level Trial Policy & Release Guardrail Automation

## 1. 背景

截至 Milestone 40A，平台已经具备多项目 release governance 能力：

```text
38C
  Beta Release Gate / Go-No-Go Decision

39A
  Controlled Rollout

39B
  Rollback Drill / Release Audit / Postmortem

39C
  Release Evidence Center / Executive Summary / Confidence Snapshot

40A
  Multi-project Release Governance
  -> Portfolio Snapshot
  -> Governance Baseline Template
  -> Risk Heatmap
```

现在系统已经能回答：

```text
多个项目里，谁最接近可以扩大试用？
哪些项目风险最高？
哪些项目的 confidence 在下降？
哪些项目缺少 sign-off 或 rollback readiness？
```

但从组织级治理角度，平台仍缺少一层“统一规则”和“自动护栏”：

```text
哪些 guardrail 是全组织统一的？
哪些项目偏离了组织基线？
哪些 threshold 应该自动建议或升级？
哪些项目出现了 portfolio drift？
哪些风险应该被系统主动标记为治理建议？
```

Milestone 40B 的目标就是新增：

```text
Organization-level Trial Policy & Release Guardrail Automation
```

让平台从：

```text
能看见多项目 release 状态
```

升级为：

```text
能定义组织级策略、检测偏离、输出治理建议和 guardrail 自动化判断
```

---

## 2. 总目标

实现组织级 release governance policy 与 guardrail automation：

1. 新增 Organization Trial Policy 数据模型。
2. 新增 Release Guardrail Evaluation 数据模型。
3. 新增 Portfolio Drift Snapshot 数据模型。
4. 支持定义 organization-level threshold / sign-off / rollback / verification policy。
5. 支持自动评估项目是否偏离组织基线。
6. 支持自动生成 guardrail evaluation 结果。
7. 支持检测 portfolio drift（项目与组织基线的偏离趋势）。
8. 支持输出治理建议（recommendation）与优先级。
9. 支持导出 Organization Governance Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
只能观察多个项目的 release 状态
```

升级为：

```text
能在组织层面对 release policy、风险护栏和偏离趋势进行统一管理
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动发布、回滚或修改生产环境。
4. 不自动更改已有 release / rollout / sign-off / verification 原始记录。
5. 不自动关闭 incident / alert / feedback。
6. 不自动替代人工审批。
7. 不调用真实 AI 自动生成治理结论。
8. guardrail evaluation 只基于已有结构化数据与明确规则。
9. drift detection 只做识别与提示，不自动应用修复。
10. 不破坏 1-40A 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 organization policy / guardrail evaluation / drift snapshot 表。
2. 聚合 40A portfolio、39C confidence、39A rollout、39B rollback / audit 数据。
3. 新增 policy、guardrail、drift、recommendation 面板。
4. 新增 Markdown summary / report 导出。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V45__init_org_policy_guardrail_tables.sql
```

### 4.1 organization_trial_policy

```sql
CREATE TABLE organization_trial_policy (
    id BIGINT PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    policy_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    threshold_json JSON NULL,
    signoff_policy_json JSON NULL,
    rollback_policy_json JSON NULL,
    verification_policy_json JSON NULL,
    recommendation_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_organization_trial_policy(policy_key),
    KEY idx_organization_trial_policy_scope(policy_scope, enabled)
);
```

### 4.2 release_guardrail_evaluation

```sql
CREATE TABLE release_guardrail_evaluation (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    policy_key VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NOT NULL,
    guardrail_category VARCHAR(64) NOT NULL,
    evaluation_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actual_value DECIMAL(18,6) NULL,
    threshold_value DECIMAL(18,6) NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    recommendation_text TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_guardrail_eval_date(snapshot_date, project_id),
    KEY idx_release_guardrail_eval_policy(policy_key, evaluation_status),
    KEY idx_release_guardrail_eval_severity(snapshot_date, severity)
);
```

### 4.3 portfolio_drift_snapshot

```sql
CREATE TABLE portfolio_drift_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    drift_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    drift_level VARCHAR(32) NOT NULL,
    baseline_template_key VARCHAR(64) NULL,
    confidence_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    signoff_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    verification_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    rollback_readiness_changed TINYINT NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_portfolio_drift_snapshot_date(snapshot_date, drift_score),
    KEY idx_portfolio_drift_snapshot_project(project_id, snapshot_date),
    KEY idx_portfolio_drift_snapshot_level(snapshot_date, drift_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
OrganizationPolicyScope.java
GuardrailEvaluationStatus.java
GuardrailSeverity.java
PortfolioDriftLevel.java
GovernanceRecommendationPriority.java
```

### 5.1 OrganizationPolicyScope

```text
GLOBAL
PROJECT_GROUP
PROJECT_OVERRIDE
```

### 5.2 GuardrailEvaluationStatus

```text
PASS
WARN
BLOCK
SKIP
```

### 5.3 GuardrailSeverity

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.4 PortfolioDriftLevel

```text
STABLE
WATCH
HIGH
CRITICAL
```

### 5.5 GovernanceRecommendationPriority

```text
P3
P2
P1
P0
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
OrganizationTrialPolicyEntity.java
ReleaseGuardrailEvaluationEntity.java
PortfolioDriftSnapshotEntity.java

OrganizationTrialPolicyMapper.java
ReleaseGuardrailEvaluationMapper.java
PortfolioDriftSnapshotMapper.java
```

DTO 建议：

```text
OrganizationTrialPolicyResponse.java
CreateOrganizationTrialPolicyRequest.java
UpdateOrganizationTrialPolicyRequest.java

ReleaseGuardrailEvaluationResponse.java
ReleaseGuardrailDashboardResponse.java

PortfolioDriftSnapshotResponse.java
PortfolioDriftDashboardResponse.java

GovernanceRecommendationResponse.java
OrganizationGovernanceSummaryResponse.java
```

### 6.1 ReleaseGuardrailDashboardResponse

建议字段：

```text
snapshotDate
projectCount
passCount
warnCount
blockCount
criticalCount
topBlockedProjects
topWarningProjects
recommendationCount
```

### 6.2 PortfolioDriftDashboardResponse

建议字段：

```text
snapshotDate
stableCount
watchCount
highCount
criticalCount
topDriftProjects
driftTrendSummary
```

### 6.3 GovernanceRecommendationResponse

建议字段：

```text
projectId
projectName
priority
category
title
summary
sourceType
policyKey
guardrailKey
snapshotDate
```

---

## 7. 服务设计

新增应用服务：

```text
OrganizationTrialPolicyService.java
ReleaseGuardrailAutomationService.java
PortfolioDriftDetectionService.java
```

### 7.1 OrganizationTrialPolicyService

职责：

1. 管理 organization-level trial policy。
2. 支持 CRUD、启停、按 scope 查询。
3. 提供默认 policy 初始化能力。
4. 将 policy 解析为 guardrail evaluation 可用的结构化规则。

### 7.2 ReleaseGuardrailAutomationService

职责：

1. 基于 organization policy 评估每个项目的 guardrail。
2. 聚合 40A 的 portfolio snapshot、39C confidence、39A/39B 核心信号。
3. 输出 PASS / WARN / BLOCK / SKIP。
4. 根据 severity 与规则生成 recommendation。
5. 输出 dashboard 和 summary。

建议 guardrail 示例：

```text
MIN_CONFIDENCE_SCORE
MAX_BLOCKING_ISSUES
MAX_FAILED_VERIFICATIONS
REQUIRE_ROLLBACK_READY
MIN_SIGNOFF_COMPLETION
MAX_OPEN_INCIDENTS
MAX_ACTIVE_ALERTS
```

### 7.3 PortfolioDriftDetectionService

职责：

1. 对比项目当前 snapshot 与基线模板 / 上次 snapshot。
2. 识别 confidence、signoff、verification、rollback readiness 的变化。
3. 计算 drift score 与 drift level。
4. 生成 drift summary。
5. 输出 heatmap / list / trend 数据。

建议 drift score 示例：

```text
abs(confidenceDelta) * 0.6
+ abs(signoffDelta) * 0.2
+ abs(verificationDelta) * 0.15
+ rollbackReadinessChanged ? 15 : 0
```

---

## 8. API 设计

新增 Controller：

```text
OrganizationGovernanceController.java
```

建议端点：

### 8.1 Organization Policy

```text
POST   /api/organization-governance/policies
GET    /api/organization-governance/policies
GET    /api/organization-governance/policies/{policyId}
PUT    /api/organization-governance/policies/{policyId}
POST   /api/organization-governance/policies/{policyId}/status
```

### 8.2 Guardrail Evaluation

```text
POST   /api/organization-governance/guardrails/refresh
GET    /api/organization-governance/guardrails
GET    /api/organization-governance/guardrails/dashboard
GET    /api/organization-governance/recommendations
```

### 8.3 Drift

```text
POST   /api/organization-governance/drift/refresh
GET    /api/organization-governance/drift
GET    /api/organization-governance/drift/dashboard
```

### 8.4 Summary

```text
GET    /api/organization-governance/summary
GET    /api/organization-governance/report
```

权限建议：

```text
查看：ADMIN
编辑 policy：ADMIN
refresh snapshot / evaluation：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Guardrail Evaluation

对每个项目按 policy 输出：

```text
PASS：满足组织 guardrail
WARN：接近阈值或存在轻微偏差
BLOCK：突破阈值，不建议扩大试用
SKIP：数据不足或不适用
```

### 9.2 Recommendation 生成

示例：

```text
confidence 过低 -> P1 / “暂停扩展，优先修复 blocking issue”
rollbackReady=false -> P1 / “补齐 rollback drill 再推进”
signoff 缺失严重 -> P2 / “补齐签字角色”
drift 连续恶化 -> P1 / “纳入重点治理项目”
```

### 9.3 Organization Governance Summary

建议至少包含：

```text
项目总数
BLOCK 项目数
WARN 项目数
最高风险项目 Top N
drift 最大项目 Top N
推荐优先治理事项 Top N
```

---

## 10. 前端设计

新增组件建议：

```text
OrganizationTrialPolicyPanel.vue
ReleaseGuardrailDashboardPanel.vue
PortfolioDriftDashboardPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 OrganizationTrialPolicyPanel

展示：

1. policy 列表
2. create / edit dialog
3. scope / enabled / notes
4. threshold / signoff / rollback / verification policy 摘要

### 10.2 ReleaseGuardrailDashboardPanel

展示：

1. guardrail 概览指标卡
2. blocked / warned 项目列表
3. recommendation 列表
4. refresh 按钮

### 10.3 PortfolioDriftDashboardPanel

展示：

1. drift 概览指标卡
2. top drift projects
3. drift level tag
4. drift trend summary
5. refresh 按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 以治理控制台风格为主，强调排序、比对、异常高亮
4. recommendation 区域要便于快速扫描，不做复杂嵌套

---

## 11. 后端测试要求

新增：

```text
OrganizationGovernanceGuardrailIntegrationTest.java
```

不少于 30 个集成测试，建议覆盖：

1. create organization policy success
2. update organization policy success
3. disable organization policy success
4. duplicate policyKey reject
5. list policies by scope works
6. refresh guardrail evaluation success
7. guardrail pass count correct
8. guardrail warn count correct
9. guardrail block count correct
10. critical severity assigned correctly
11. recommendation generated for low confidence
12. recommendation generated for missing rollback readiness
13. recommendation generated for low signoff completion
14. refresh drift snapshot success
15. drift score calculation correct
16. drift level stable
17. drift level watch
18. drift level high
19. drift level critical
20. summary response returns blocked projects
21. summary response returns top drift projects
22. report export returns markdown
23. unauthorized access reject
24. non-admin edit reject
25. empty portfolio returns empty dashboard
26. policy threshold affects evaluation
27. disabled policy excluded from evaluation
28. rollback readiness affects guardrail
29. signoff completion affects drift/recommendation
30. confidence delta reflected in drift summary

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/organization-governance.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 organization policy panel
2. guardrail dashboard renders
3. drift dashboard renders
4. create policy dialog works
5. recommendation 列表可见
6. refresh buttons visible
7. blocked / warned 项目列表可见
8. no JS errors on page load

如果测试环境没有 seeded portfolio 数据：

1. 显式断言空态
2. 不把“无治理数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-40b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 organization governance 表说明
3. OrganizationTrialPolicyService 设计说明
4. ReleaseGuardrailAutomationService 设计说明
5. PortfolioDriftDetectionService 设计说明
6. OrganizationTrialPolicyPanel 说明
7. ReleaseGuardrailDashboardPanel 说明
8. PortfolioDriftDashboardPanel 说明
9. Organization-level governance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40C

---

## 14. 验收标准

必须全部满足：

1. organization_trial_policy / release_guardrail_evaluation / portfolio_drift_snapshot 三张表已落库
2. organization policy 可创建 / 编辑 / 启停
3. guardrail evaluation 可聚合多个项目并输出 PASS/WARN/BLOCK
4. recommendation 列表可查询
5. drift snapshot 可生成并展示项目偏离
6. organization summary / report 可导出或结构化返回
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 40B 后，平台将从：

```text
多项目 release portfolio 治理
```

升级为：

```text
组织级 trial policy、guardrail 自动化和 drift 治理中枢
```

这一步会让平台更接近真正的 organization-level release governance system，而不只是项目汇总看板。

---

## 16. 后续建议

Milestone 40B 完成后，建议进入：

```text
Milestone 40C: Governance Recommendation Workflow & Exception Waiver Management
```

重点可包括：

1. 推荐事项工作流
2. exception / waiver 申请与审批
3. waiver 到期追踪
4. recommendation 执行状态
5. 治理闭环完成率

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 40B。

文档路径：
docs/milestone-40b-organization-level-trial-policy-release-guardrail-automation.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 40A Multi-project Release Governance 基础上，新增组织级 trial policy、guardrail 自动化和 portfolio drift detection。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动发布、回滚或修改生产环境。
6. 不要自动更改已有 release / rollout / sign-off / verification 原始记录。
7. 不要自动关闭 incident / alert / feedback。
8. 不要调用真实 AI 自动生成治理结论。
9. drift detection 只识别与提示，不自动修复。
10. 不要破坏 1-40A 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V45__init_org_policy_guardrail_tables.sql。
2. 新增 organization_trial_policy / release_guardrail_evaluation / portfolio_drift_snapshot 三张表。
3. 新增 5 个枚举：
   - OrganizationPolicyScope
   - GuardrailEvaluationStatus
   - GuardrailSeverity
   - PortfolioDriftLevel
   - GovernanceRecommendationPriority
4. 新增实体、Mapper、DTO。
5. 新增 OrganizationTrialPolicyService。
6. 新增 ReleaseGuardrailAutomationService。
7. 新增 PortfolioDriftDetectionService。
8. 新增 API：
   - organization policy CRUD / status
   - guardrail refresh / list / dashboard / recommendations
   - drift refresh / list / dashboard
   - organization summary / report
9. 前端新增：
   - OrganizationTrialPolicyPanel.vue
   - ReleaseGuardrailDashboardPanel.vue
   - PortfolioDriftDashboardPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 30 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-40b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 organization governance 表说明
3. OrganizationTrialPolicyService 设计说明
4. ReleaseGuardrailAutomationService 设计说明
5. PortfolioDriftDetectionService 设计说明
6. OrganizationTrialPolicyPanel 说明
7. ReleaseGuardrailDashboardPanel 说明
8. PortfolioDriftDashboardPanel 说明
9. Organization-level governance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 40C

现在开始实现，不要只给计划。
```
