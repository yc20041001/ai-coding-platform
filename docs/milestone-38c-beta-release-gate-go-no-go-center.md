# Milestone 38C: Beta Release Gate & Go/No-Go Decision Center

## 1. 背景

Milestone 38A 已完成 External Beta 试用闭环：

```text
Beta Trial Session
  -> Feedback
  -> Environment Readiness
  -> Pass / Block Summary
```

Milestone 38B 已完成真实模型成本与 PR Review 质量硬化：

```text
Model Cost Summary / Alert
  -> PR Review Quality Record
  -> Cost Dashboard
  -> PR Review Quality Dashboard
```

再往前，37H-37K 已经把事故与知识治理链路补齐：

```text
Incident
  -> SLA / Escalation / Timeline
  -> RCA
  -> Retrospective
  -> Knowledge Quality Review
```

现在系统已经有很多“信号”，但产品层面仍然缺少一个明确的结论层：

```text
当前 Beta 能不能放？
阻塞项有哪些？
是模型成本的问题、PR Review 质量的问题、Incident 风险的问题，还是试用反馈的问题？
哪些条件满足后才能从 Beta 继续推进？
```

Milestone 38C 的目标是新增：

```text
Beta Release Gate & Go/No-Go Decision Center
```

让平台从：

```text
有很多分散的监控、反馈和质量面板
```

升级为：

```text
有统一的 Beta 放行判断、阻塞项聚合、决策结论与审计轨迹
```

---

## 2. 总目标

实现统一的 Beta 放行与 Go/No-Go 决策中心：

1. 新增 Beta Release Gate Rule 数据模型。
2. 新增 Beta Release Gate Evaluation 数据模型。
3. 新增 Beta Release Decision 数据模型。
4. 支持聚合 38A 的试用反馈与 readiness 数据。
5. 支持聚合 38B 的成本 / 告警 / PR Review 质量数据。
6. 支持聚合 37H-37K 的 incident / RCA / retrospective / knowledge quality 风险数据。
7. 支持自动生成 Gate Evaluation 结果。
8. 支持人工填写最终 Go / No-Go / Conditional Go 决策。
9. 支持导出版本级 Beta Readiness Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
靠人手动阅读多个页面判断能不能放行
```

升级为：

```text
系统化展示 Beta 放行状态、阻塞原因和最终决策依据
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch` / `git reset` / `git apply` / `git add` / `git commit` / `git push`。
4. 不自动触发发布。
5. 不自动关闭 Incident / Feedback / Alert。
6. 不自动批准 Go 决策。
7. 不调用真实 AI 自动生成决策结论。
8. Gate Evaluation 只基于已有结构化数据与阈值规则。
9. 不破坏 1-38B 已有 API。
10. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 rule / evaluation / decision 表。
2. 聚合已有 Beta、Cost、PR Review、Incident、Retrospective、Knowledge Quality 数据。
3. 新增 dashboard、列表、详情抽屉、导出 Markdown。
4. 新增 Go / No-Go / Conditional Go 状态与人工说明。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V40__init_beta_release_gate_tables.sql
```

### 4.1 beta_release_gate_rule

```sql
CREATE TABLE beta_release_gate_rule (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    rule_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    threshold_operator VARCHAR(16) NOT NULL,
    threshold_value DECIMAL(18,6) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    blocking TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_gate_rule_project(project_id),
    KEY idx_beta_gate_rule_category(category, enabled),
    UNIQUE KEY uk_beta_gate_rule(project_id, rule_key)
);
```

### 4.2 beta_release_gate_evaluation

```sql
CREATE TABLE beta_release_gate_evaluation (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    evaluation_target VARCHAR(128) NOT NULL,
    evaluation_type VARCHAR(32) NOT NULL,
    rule_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    gate_status VARCHAR(32) NOT NULL,
    actual_value DECIMAL(18,6) NULL,
    threshold_value DECIMAL(18,6) NULL,
    blocking TINYINT NOT NULL DEFAULT 1,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    evidence_json JSON NULL,
    evaluated_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_gate_eval_project(project_id, evaluated_at),
    KEY idx_beta_gate_eval_target(evaluation_target, evaluation_type),
    KEY idx_beta_gate_eval_status(gate_status, category)
);
```

### 4.3 beta_release_decision

```sql
CREATE TABLE beta_release_decision (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    decision_status VARCHAR(32) NOT NULL,
    decision_reason TEXT NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    approver_id BIGINT NULL,
    approved_at DATETIME NULL,
    report_markdown MEDIUMTEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_release_decision_project(project_id, create_time),
    KEY idx_beta_release_decision_status(decision_status),
    UNIQUE KEY uk_beta_release_label(project_id, release_label)
);
```

无物理外键，保持项目当前风格。

---

## 5. 枚举设计

新增：

```text
BetaReleaseGateCategory.java
BetaReleaseGateStatus.java
BetaReleaseDecisionStatus.java
BetaReleaseEvaluationType.java
BetaThresholdOperator.java
```

### 5.1 BetaReleaseGateCategory

```text
TRIAL_FEEDBACK
ENVIRONMENT_READINESS
MODEL_COST
PR_REVIEW_QUALITY
INCIDENT_RISK
KNOWLEDGE_QUALITY
```

### 5.2 BetaReleaseGateStatus

```text
PASS
WARN
BLOCK
SKIP
```

### 5.3 BetaReleaseDecisionStatus

```text
GO
CONDITIONAL_GO
NO_GO
DRAFT
```

### 5.4 BetaReleaseEvaluationType

```text
PROJECT
SESSION
GLOBAL
```

### 5.5 BetaThresholdOperator

```text
GT
GTE
LT
LTE
EQ
NEQ
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
BetaReleaseGateRuleEntity.java
BetaReleaseGateEvaluationEntity.java
BetaReleaseDecisionEntity.java

BetaReleaseGateRuleMapper.java
BetaReleaseGateEvaluationMapper.java
BetaReleaseDecisionMapper.java
```

新增 DTO：

```text
CreateBetaReleaseDecisionRequest.java
UpdateBetaReleaseDecisionRequest.java
BetaReleaseDecisionResponse.java

BetaReleaseGateRuleResponse.java
BetaReleaseGateEvaluationResponse.java
BetaReleaseGateDashboardResponse.java
BetaReleaseReadinessReportResponse.java
```

### 6.1 CreateBetaReleaseDecisionRequest

```java
public class CreateBetaReleaseDecisionRequest {
    private String projectId;
    private String releaseLabel;
    private String decisionStatus;
    private String decisionReason;
}
```

### 6.2 UpdateBetaReleaseDecisionRequest

```java
public class UpdateBetaReleaseDecisionRequest {
    private String decisionStatus;
    private String decisionReason;
}
```

---

## 7. 后端服务设计

### 7.1 BetaReleaseGateRuleService

新增：

```text
BetaReleaseGateRuleService.java
```

职责：

1. 提供内置 gate rules。
2. 支持项目级 rule 覆盖。
3. 提供当前生效规则列表。

建议内置规则：

1. `P0_FEEDBACK_COUNT == 0`
2. `P1_FEEDBACK_COUNT <= 3`
3. `RELEASE_BLOCKING_FEEDBACK_COUNT == 0`
4. `READINESS_FAIL_COUNT == 0`
5. `MODEL_COST_ALERT_HIGH_COUNT == 0`
6. `PR_REVIEW_FAILURE_RATIO <= 0.20`
7. `PR_REVIEW_ADOPTION_RATIO >= 0.30`
8. `OPEN_CRITICAL_INCIDENT_COUNT == 0`
9. `KNOWLEDGE_QUALITY_REJECTED_COUNT == 0`

### 7.2 BetaReleaseGateEvaluationService

新增：

```text
BetaReleaseGateEvaluationService.java
```

职责：

1. 从 38A / 38B / 37H-37K 数据聚合 gate signals。
2. 逐条规则产出 evaluation 结果。
3. 保存 evaluation 快照。
4. 生成 dashboard。

建议方法：

```java
public List<BetaReleaseGateEvaluationResponse> evaluateProject(Long projectId)

public List<BetaReleaseGateEvaluationResponse> listEvaluations(Long projectId, String gateStatus)

public BetaReleaseGateDashboardResponse getDashboard(Long projectId)
```

### 7.3 BetaReleaseDecisionService

新增：

```text
BetaReleaseDecisionService.java
```

职责：

1. 创建版本级 release decision 草稿。
2. 自动汇总 blocking / warning issue count。
3. 允许人工给出 `GO / CONDITIONAL_GO / NO_GO`。
4. 导出 Readiness Report Markdown。

建议方法：

```java
public BetaReleaseDecisionResponse createDecision(CreateBetaReleaseDecisionRequest request)

public BetaReleaseDecisionResponse updateDecision(Long decisionId, UpdateBetaReleaseDecisionRequest request)

public BetaReleaseDecisionResponse getDecision(Long decisionId)

public PageResult<BetaReleaseDecisionResponse> listProjectDecisions(Long projectId, PageQuery pageQuery)

public String exportReadinessReport(Long decisionId)
```

### 7.4 Readiness Report 拼装来源

报告必须聚合：

1. 38A 的 Beta Trial Sessions / Feedback / Readiness
2. 38B 的 Model Cost Dashboard / Alerts / PR Review Quality
3. 37H-37K 的 Incident / RCA / Retrospective / Knowledge Quality

Markdown 建议结构：

```markdown
# Beta Readiness Report: {releaseLabel}

## Decision Summary

## Trial Feedback Summary

## Environment Readiness

## Model Cost Risk

## PR Review Quality

## Incident & Retrospective Risk

## Knowledge Quality Risk

## Blocking Issues

## Recommended Decision
```

说明：

1. “Recommended Decision” 可以由规则推导出建议值
2. 最终 `decisionStatus` 仍由人工提交

---

## 8. API 设计

### 8.1 Gate Rules / Evaluations

```http
GET /api/projects/{projectId}/beta-release-gates/rules
POST /api/projects/{projectId}/beta-release-gates/evaluate
GET /api/projects/{projectId}/beta-release-gates/evaluations?gateStatus=&page=&size=
GET /api/projects/{projectId}/beta-release-gates/dashboard
```

### 8.2 Release Decision

```http
POST /api/beta-release-decisions
PUT /api/beta-release-decisions/{decisionId}
GET /api/beta-release-decisions/{decisionId}
GET /api/projects/{projectId}/beta-release-decisions?page=&size=
GET /api/beta-release-decisions/{decisionId}/report/markdown
```

权限建议：

```text
POST / PUT / evaluate: MAINTAINER+
GET: VIEWER+
```

---

## 9. 前端设计

### 9.1 ObservabilityPage 增强

在当前 Observability 页面新增：

1. `Beta Release Gate Dashboard`
2. `Gate Evaluations Table`
3. `Release Decision Panel`

推荐 data-testid：

```text
beta-release-gate-dashboard
beta-release-gate-table
beta-release-decision-panel
beta-release-evaluate-button
beta-release-create-decision-button
beta-release-report-button
```

### 9.2 BetaReleaseGateDashboardPanel

新增：

```text
frontend/src/modules/admin/components/BetaReleaseGateDashboardPanel.vue
```

功能：

1. 展示 PASS / WARN / BLOCK / SKIP 数量
2. 展示关键 blocking rules
3. 展示建议决策

### 9.3 BetaReleaseDecisionPanel

新增：

```text
frontend/src/modules/admin/components/BetaReleaseDecisionPanel.vue
```

功能：

1. 创建决策草稿
2. 选择 `GO / CONDITIONAL_GO / NO_GO`
3. 填写决策理由
4. 查看 blocking / warning 计数
5. 导出 readiness report

---

## 10. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
```

新增：

```ts
export interface BetaReleaseGateRule {
  id: string
  projectId?: string
  ruleKey: string
  category: string
  displayName: string
  thresholdOperator: string
  thresholdValue?: number
  enabled: boolean
  blocking: boolean
  sortOrder: number
  description?: string
}

export interface BetaReleaseGateEvaluation {
  id: string
  projectId?: string
  evaluationTarget: string
  evaluationType: string
  ruleKey: string
  category: string
  gateStatus: string
  actualValue?: number
  thresholdValue?: number
  blocking: boolean
  summary: string
  detail?: string
  evidenceJson?: string
  evaluatedAt: string
}

export interface BetaReleaseDecision {
  id: string
  projectId?: string
  releaseLabel: string
  decisionStatus: string
  decisionReason?: string
  blockingIssueCount: number
  warningIssueCount: number
  approverId?: string
  approvedAt?: string
  reportMarkdown?: string
  createTime: string
  updateTime: string
}
```

新增 API 函数：

```ts
getBetaReleaseGateRules(projectId: string)
evaluateBetaReleaseGates(projectId: string)
listBetaReleaseGateEvaluations(projectId: string, params)
getBetaReleaseGateDashboard(projectId: string)

createBetaReleaseDecision(data)
updateBetaReleaseDecision(decisionId: string, data)
getBetaReleaseDecision(decisionId: string)
listProjectBetaReleaseDecisions(projectId: string, params)
exportBetaReleaseDecisionReportMarkdown(decisionId: string)
```

---

## 11. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/beta/BetaReleaseGateIntegrationTest.java
backend/src/test/java/com/aicoding/platform/beta/BetaReleaseDecisionIntegrationTest.java
```

至少 34 个测试。

### 11.1 Gate Evaluation

1. 可查询规则列表
2. evaluate 成功生成 evaluation
3. P0 feedback count 规则生效
4. release blocking 规则生效
5. readiness fail 规则生效
6. model cost alert 规则生效
7. pr review failure ratio 规则生效
8. knowledge quality rejected 规则生效
9. open critical incident 规则生效
10. dashboard 聚合正确
11. blocking / warning / pass 计数正确
12. evidenceJson 正确保存

### 11.2 Release Decision

13. 可创建 decision 草稿
14. 可更新为 GO
15. 可更新为 CONDITIONAL_GO
16. 可更新为 NO_GO
17. blocking issue count 自动汇总
18. warning issue count 自动汇总
19. 可分页查询 decision
20. 可导出 markdown
21. markdown 包含各分类章节

### 11.3 权限

22. 未登录 evaluate 返回 UNAUTHORIZED
23. 未登录创建 decision 返回 UNAUTHORIZED
24. VIEWER 可查询 dashboard
25. MAINTAINER 可 evaluate
26. MAINTAINER 可创建 / 更新 decision
27. 非项目成员拒绝访问

### 11.4 项目隔离与边界

28. 不串项目
29. 无数据时返回空 dashboard
30. draft decision 可创建
31. 重复 releaseLabel 返回 CONFLICT
32. export 不泄露敏感字段
33. GO 决策不自动触发发布
34. 规则禁用后不参与 evaluate

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/beta-release-gate.spec.ts
```

至少 8 个 E2E：

1. Observability 页面显示 Beta Release Gate Dashboard
2. Gate table 可见
3. 点击 evaluate 后结果刷新
4. 创建 release decision 成功
5. 更新 decisionStatus 成功
6. markdown 导出入口可见
7. blocking / warning 计数可见
8. 页面无 JS error
9. 决策理由编辑可用

---

## 13. 文档与报告

完成后新增：

```text
docs/milestone-38c-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. beta_release_gate_rule 表说明
3. beta_release_gate_evaluation 表说明
4. beta_release_decision 表说明
5. BetaReleaseGateRuleService 设计说明
6. BetaReleaseGateEvaluationService 设计说明
7. BetaReleaseDecisionService 设计说明
8. API 清单
9. 前端 Beta Release Gate / Decision UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 39A

---

## 14. 验收标准

完成后必须满足：

1. 可查看项目级 Beta Release Gate dashboard
2. 可执行 gate evaluate
3. 可查看每条 gate evaluation
4. 可创建 / 更新 release decision
5. 可导出 readiness report markdown
6. 可显示 blocking / warning / pass 汇总
7. 所有 API 有权限校验
8. 不调用真实 AI 自动决策
9. 不自动发布
10. 后端测试通过
11. 前端 typecheck / build / E2E 通过

---

## 15. 非目标

本阶段不做：

1. 不做真实自动发布
2. 不做 CI/CD 环境一键切换
3. 不做多租户 release board
4. 不做自动审批链
5. 不做复杂 SLA 报告引擎

这些可以放到后续 Milestone。

---

## 16. 建议后续 Milestone

完成 38C 后，建议进入：

```text
Milestone 39A: Beta-to-Production Readiness & Controlled Rollout
```

候选能力：

1. 生产前核对清单自动化
2. 版本级 rollout plan
3. 回滚策略与验证脚本
4. 生产观察窗口与 post-release review

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 38C。

文档路径：
docs/milestone-38c-beta-release-gate-go-no-go-center.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37H-37K 与 38A-38B 基础上，新增 Beta Release Gate & Go/No-Go Decision Center。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不自动触发发布。
7. 不自动关闭 Incident / Feedback / Alert。
8. 不自动批准 Go 决策。
9. 不调用真实 AI 自动生成决策结论。
10. Gate Evaluation 只基于已有结构化数据与阈值规则。
11. 不破坏 1-38B 已有 API。
12. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V40__init_beta_release_gate_tables.sql。
2. 新增 beta_release_gate_rule / beta_release_gate_evaluation / beta_release_decision 三张表。
3. 新增相关枚举、Entity、Mapper、DTO。
4. 新增 BetaReleaseGateRuleService。
5. 新增 BetaReleaseGateEvaluationService。
6. 新增 BetaReleaseDecisionService。
7. 新增 API：
   - GET /api/projects/{projectId}/beta-release-gates/rules
   - POST /api/projects/{projectId}/beta-release-gates/evaluate
   - GET /api/projects/{projectId}/beta-release-gates/evaluations
   - GET /api/projects/{projectId}/beta-release-gates/dashboard
   - POST /api/beta-release-decisions
   - PUT /api/beta-release-decisions/{decisionId}
   - GET /api/beta-release-decisions/{decisionId}
   - GET /api/projects/{projectId}/beta-release-decisions
   - GET /api/beta-release-decisions/{decisionId}/report/markdown
8. 前端在 ObservabilityPage 中新增 Beta Release Gate Dashboard / Gate Table / Release Decision Panel。
9. 新增 BetaReleaseGateDashboardPanel.vue。
10. 新增 BetaReleaseDecisionPanel.vue。
11. 后端测试不少于 34 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-38c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. beta_release_gate_rule 表说明
3. beta_release_gate_evaluation 表说明
4. beta_release_decision 表说明
5. BetaReleaseGateRuleService 设计说明
6. BetaReleaseGateEvaluationService 设计说明
7. BetaReleaseDecisionService 设计说明
8. API 清单
9. 前端 Beta Release Gate / Decision UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 39A

现在开始实现，不要只给计划。
```
