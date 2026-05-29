# Milestone 42A: Governance Execution Automation & Recommendation Playbooks

## 1. 背景

截至 Milestone 41C，平台已经具备较完整的治理分析与决策支持能力：

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
  Governance Simulation / What-if Planning / Policy Tuning
```

现在系统已经能回答：

```text
当前有哪些治理事项？
哪些项目和 owner 风险最高？
未来 7 天 / 14 天会发生什么？
如果我们调整 SLA、owner 分配或 waiver，会有什么变化？
```

但治理体系要真正帮助团队“落地执行”，还缺少一层执行支持：

```text
发现问题之后，下一步到底怎么做？
针对不同 recommendation，有没有标准 remediation playbook？
owner 交接时，有没有统一 handoff checklist？
waiver 风险怎么有结构化缓解步骤？
哪些动作可以自动建议，而不是每次靠人从头想？
```

换句话说，41C 让平台具备了：

```text
知道什么问题最值得处理，以及不同方案会带来什么结果
```

但还不具备：

```text
把 recommendation 变成结构化执行步骤和操作模板
```

Milestone 42A 的目标就是新增：

```text
Governance Execution Automation & Recommendation Playbooks
```

让平台从：

```text
能识别、预测、模拟治理问题
```

升级为：

```text
能提供标准 playbook、handoff 指南和 remediation checklist，帮助团队更快执行
```

---

## 2. 总目标

实现 recommendation 执行辅助与 playbook 能力：

1. 新增 Governance Recommendation Playbook Template 数据模型。
2. 新增 Governance Recommendation Execution Plan 数据模型。
3. 新增 Governance Handoff Checklist 数据模型。
4. 支持为不同 recommendation / guardrail / priority 匹配默认 playbook。
5. 支持生成 recommendation execution plan。
6. 支持 owner handoff checklist。
7. 支持 waiver mitigation playbook。
8. 支持 guided remediation checklist 展示与完成状态记录。
9. 支持导出 Recommendation Execution Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
平台知道应该做什么
```

升级为：

```text
平台知道应该怎么做，并给出可执行的标准步骤
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改 recommendation / waiver / escalation / SLA / policy 原始记录，除非显式更新 execution plan/checklist 自身状态。
4. 不自动分配 owner。
5. 不自动关闭 recommendation 或 waiver。
6. 不自动批准 waiver。
7. 不自动执行 remediation 动作。
8. 不调用真实 AI 自动生成 playbook 内容。
9. playbook 只提供模板、步骤、建议，不调用外部系统。
10. 不破坏 1-41C 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 playbook template / execution plan / handoff checklist 表。
2. 根据 recommendation 类型自动匹配模板并生成 execution plan。
3. 支持 checklist 项状态记录、handoff 记录、Markdown summary 导出。
4. 提供结构化 remediation 辅助，而不真正执行操作。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V50__init_governance_execution_playbook_tables.sql
```

### 4.1 governance_recommendation_playbook_template

```sql
CREATE TABLE governance_recommendation_playbook_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL,
    priority VARCHAR(32) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    template_steps_json JSON NOT NULL,
    success_criteria_json JSON NULL,
    handoff_notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_playbook_template(template_key),
    KEY idx_governance_playbook_template_match(recommendation_category, guardrail_key, priority, enabled)
);
```

### 4.2 governance_recommendation_execution_plan

```sql
CREATE TABLE governance_recommendation_execution_plan (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    plan_status VARCHAR(32) NOT NULL,
    template_key VARCHAR(64) NULL,
    owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL,
    due_at DATETIME NULL,
    steps_json JSON NOT NULL,
    completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_execution_plan_recommendation(recommendation_id),
    KEY idx_governance_execution_plan_project(project_id, plan_status),
    KEY idx_governance_execution_plan_due(due_at, plan_status)
);
```

### 4.3 governance_handoff_checklist

```sql
CREATE TABLE governance_handoff_checklist (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    execution_plan_id BIGINT NULL,
    from_owner_id BIGINT NULL,
    from_owner_name VARCHAR(128) NULL,
    to_owner_id BIGINT NULL,
    to_owner_name VARCHAR(128) NULL,
    checklist_status VARCHAR(32) NOT NULL,
    checklist_items_json JSON NOT NULL,
    handoff_note TEXT NULL,
    handed_off_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_handoff_recommendation(recommendation_id),
    KEY idx_governance_handoff_plan(execution_plan_id),
    KEY idx_governance_handoff_status(checklist_status)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceExecutionPlanStatus.java
GovernanceChecklistStatus.java
GovernancePlaybookStepStatus.java
GovernancePlaybookTemplateMatchMode.java
```

### 5.1 GovernanceExecutionPlanStatus

```text
DRAFT
READY
IN_PROGRESS
BLOCKED
COMPLETED
ARCHIVED
```

### 5.2 GovernanceChecklistStatus

```text
OPEN
IN_PROGRESS
COMPLETED
CANCELLED
```

### 5.3 GovernancePlaybookStepStatus

```text
TODO
DOING
DONE
SKIPPED
BLOCKED
```

### 5.4 GovernancePlaybookTemplateMatchMode

```text
EXACT
CATEGORY_PRIORITY
CATEGORY_ONLY
DEFAULT
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceRecommendationPlaybookTemplateEntity.java
GovernanceRecommendationExecutionPlanEntity.java
GovernanceHandoffChecklistEntity.java

GovernanceRecommendationPlaybookTemplateMapper.java
GovernanceRecommendationExecutionPlanMapper.java
GovernanceHandoffChecklistMapper.java
```

DTO 建议：

```text
CreateGovernancePlaybookTemplateRequest.java
UpdateGovernancePlaybookTemplateRequest.java
GovernancePlaybookTemplateResponse.java

CreateGovernanceExecutionPlanRequest.java
UpdateGovernanceExecutionPlanRequest.java
GovernanceExecutionPlanResponse.java

CreateGovernanceHandoffChecklistRequest.java
UpdateGovernanceHandoffChecklistRequest.java
GovernanceHandoffChecklistResponse.java

GovernanceExecutionDashboardResponse.java
GovernancePlaybookMatchPreviewResponse.java
```

### 6.1 GovernanceExecutionDashboardResponse

建议字段：

```text
totalPlanCount
readyPlanCount
inProgressPlanCount
blockedPlanCount
completedPlanCount
averageCompletionRate
handoffOpenCount
topBlockedPlans
topNearDuePlans
```

### 6.2 GovernanceExecutionPlanResponse

建议字段：

```text
id
recommendationId
projectId
planStatus
templateKey
ownerId
ownerName
dueAt
completionRate
summaryText
steps
createTime
updateTime
```

其中 `steps` 建议为结构化 JSON 数组，每项至少包含：

```text
stepKey
title
description
status
required
notes
```

---

## 7. 服务设计

新增应用服务：

```text
GovernancePlaybookTemplateService.java
GovernanceExecutionPlanService.java
GovernanceHandoffAssistantService.java
```

### 7.1 GovernancePlaybookTemplateService

职责：

1. 管理 playbook template 的 CRUD。
2. 按 recommendation category / guardrail / priority 匹配模板。
3. 提供默认模板初始化。
4. 输出匹配模式（EXACT / CATEGORY_PRIORITY / CATEGORY_ONLY / DEFAULT）。

### 7.2 GovernanceExecutionPlanService

职责：

1. 基于 recommendation + template 生成 execution plan。
2. 支持步骤状态流转：

```text
TODO -> DOING -> DONE
TODO -> SKIPPED
TODO/DOING -> BLOCKED
BLOCKED -> DOING
```

3. 根据步骤完成情况计算 `completionRate`。
4. 管理 execution plan 状态：

```text
DRAFT -> READY -> IN_PROGRESS -> COMPLETED
                   \-> BLOCKED
COMPLETED -> ARCHIVED
```

5. 生成 recommendation execution summary markdown。

### 7.3 GovernanceHandoffAssistantService

职责：

1. 创建 handoff checklist。
2. 为 owner 变更提供标准交接项。
3. 支持 handoff checklist 状态流转。
4. 提供 waiver mitigation 默认 checklist。

示例默认交接项：

```text
确认 recommendation 当前状态
确认已存在的 blocker / waiver
确认下一个 SLA 截止时间
确认已完成步骤与剩余步骤
确认需要同步的 incident / alert / evidence
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceExecutionController.java
```

建议端点：

### 8.1 Playbook Template

```text
POST   /api/governance-execution/playbook-templates
GET    /api/governance-execution/playbook-templates
GET    /api/governance-execution/playbook-templates/{templateId}
PUT    /api/governance-execution/playbook-templates/{templateId}
POST   /api/governance-execution/playbook-templates/{templateId}/status
GET    /api/governance-execution/playbook-match-preview/{recommendationId}
```

### 8.2 Execution Plan

```text
POST   /api/governance-execution/plans
GET    /api/governance-execution/plans
GET    /api/governance-execution/plans/{planId}
PUT    /api/governance-execution/plans/{planId}
POST   /api/governance-execution/plans/{planId}/status
POST   /api/governance-execution/plans/{planId}/steps/{stepKey}/status
GET    /api/governance-execution/dashboard
GET    /api/governance-execution/report
```

### 8.3 Handoff Checklist

```text
POST   /api/governance-execution/handoffs
GET    /api/governance-execution/handoffs
GET    /api/governance-execution/handoffs/{checklistId}
PUT    /api/governance-execution/handoffs/{checklistId}
POST   /api/governance-execution/handoffs/{checklistId}/status
```

权限建议：

```text
查看：ADMIN
编辑模板：ADMIN
创建/更新 plan：ADMIN
handoff：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Playbook 匹配顺序

建议顺序：

```text
1. guardrail + priority 精确匹配
2. recommendation category + priority
3. recommendation category
4. 默认模板
```

### 9.2 Completion Rate

建议：

```text
DONE / (required 且非 SKIPPED 的步骤数) * 100
```

### 9.3 默认 Playbook 场景建议

至少支持：

```text
低 confidence / 高 overdue
rollback readiness 缺失
signoff completion 过低
waiver cluster 过高
owner overload
```

### 9.4 Waiver Mitigation Playbook 示例

```text
确认 waiver 原因与有效期
确认是否可提前消除例外条件
识别相关 recommendation / blocker
明确 owner 和执行期限
在到期前完成替代性治理动作
```

---

## 10. 前端设计

新增组件建议：

```text
GovernancePlaybookTemplatePanel.vue
GovernanceExecutionPlanPanel.vue
GovernanceHandoffChecklistPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernancePlaybookTemplatePanel

展示：

1. template 列表
2. create / edit dialog
3. 匹配条件摘要
4. enabled 状态
5. match preview 入口

### 10.2 GovernanceExecutionPlanPanel

展示：

1. execution plan 列表
2. completion rate
3. plan 状态
4. steps checklist
5. step 状态按钮
6. top blocked / near due 展示

### 10.3 GovernanceHandoffChecklistPanel

展示：

1. handoff checklist 列表
2. from/to owner
3. checklist 状态
4. 交接项勾选状态
5. handoff note

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. checklist / step 视图要清晰显示状态推进
4. execution plan 重点突出 blocked / near due

---

## 11. 后端测试要求

新增：

```text
GovernanceExecutionPlaybookIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. create playbook template success
2. update playbook template success
3. disable playbook template success
4. duplicate templateKey reject
5. exact match mode selected
6. category+priority match selected
7. category match selected
8. default match selected
9. create execution plan success
10. execution plan generated from matched template
11. update execution plan success
12. step status todo -> doing
13. step status doing -> done
14. step status todo -> blocked
15. blocked -> doing
16. invalid step transition reject
17. completion rate computed correctly
18. plan status draft -> ready
19. ready -> in_progress
20. in_progress -> completed
21. blocked plan status set correctly
22. completed -> archived
23. report export markdown success
24. create handoff checklist success
25. update handoff checklist success
26. handoff status open -> in_progress
27. in_progress -> completed
28. waiver mitigation checklist generated
29. dashboard counts correct
30. top blocked plans returned
31. top near due plans returned
32. unauthorized access reject
33. non-admin template edit reject
34. empty dataset returns empty dashboard
35. disabled template not used in match
36. match preview returns chosen template

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-execution.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 playbook template panel
2. execution plan panel renders
3. handoff checklist panel renders
4. create playbook dialog works
5. execution plan checklist visible
6. handoff checklist status tag visible
7. report / dashboard 区域可见
8. no JS errors on page load

如果测试环境没有 seeded governance simulation/workflow 数据：

1. 显式断言空态
2. 不把“无 execution 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-42a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance execution / playbook 表说明
3. GovernancePlaybookTemplateService 设计说明
4. GovernanceExecutionPlanService 设计说明
5. GovernanceHandoffAssistantService 设计说明
6. GovernancePlaybookTemplatePanel 说明
7. GovernanceExecutionPlanPanel 说明
8. GovernanceHandoffChecklistPanel 说明
9. Execution / Playbook 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42B

---

## 14. 验收标准

必须全部满足：

1. governance_recommendation_playbook_template / governance_recommendation_execution_plan / governance_handoff_checklist 三张表已落库
2. playbook template 可创建 / 编辑 / 启停
3. execution plan 可创建 / 更新 / 状态流转
4. checklist / handoff 可创建 / 更新 / 状态流转
5. dashboard / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. execution 辅助逻辑清晰、可解释、可追踪

---

## 15. 完成后的价值

完成 42A 后，平台将从：

```text
能给出 recommendation 和调优建议
```

升级为：

```text
能给出 recommendation 的标准执行路径、交接方式和 remediation playbook
```

这一步会把治理能力从“决策支持”继续推进到“执行辅助工作台”。

---

## 16. 后续建议

Milestone 42A 完成后，建议进入：

```text
Milestone 42B: Governance Knowledge Base, Pattern Library & Reusable Remediation Recipes
```

重点可包括：

1. remediation recipe library
2. pattern-based playbook reuse
3. governance knowledge search
4. similar remediation suggestion
5. playbook effectiveness scoring

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 42A。

文档路径：
docs/milestone-42a-governance-execution-automation-recommendation-playbooks.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 41C governance simulation / policy tuning 基础上，新增 execution automation 辅助与 recommendation playbook。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / escalation / policy 原始记录，除非显式更新 execution plan/checklist 自身状态。
6. 不要自动分配 owner。
7. 不要自动关闭 recommendation 或 waiver。
8. 不要自动批准 waiver。
9. 不要调用真实 AI 自动生成 playbook 内容。
10. 不要破坏 1-41C 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V50__init_governance_execution_playbook_tables.sql。
2. 新增 governance_recommendation_playbook_template / governance_recommendation_execution_plan / governance_handoff_checklist 三张表。
3. 新增 4 个枚举：
   - GovernanceExecutionPlanStatus
   - GovernanceChecklistStatus
   - GovernancePlaybookStepStatus
   - GovernancePlaybookTemplateMatchMode
4. 新增实体、Mapper、DTO。
5. 新增 GovernancePlaybookTemplateService。
6. 新增 GovernanceExecutionPlanService。
7. 新增 GovernanceHandoffAssistantService。
8. 新增 API：
   - playbook template CRUD / status / match preview
   - execution plan CRUD / status / step status / dashboard / report
   - handoff checklist CRUD / status
9. 前端新增：
   - GovernancePlaybookTemplatePanel.vue
   - GovernanceExecutionPlanPanel.vue
   - GovernanceHandoffChecklistPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-42a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance execution / playbook 表说明
3. GovernancePlaybookTemplateService 设计说明
4. GovernanceExecutionPlanService 设计说明
5. GovernanceHandoffAssistantService 设计说明
6. GovernancePlaybookTemplatePanel 说明
7. GovernanceExecutionPlanPanel 说明
8. GovernanceHandoffChecklistPanel 说明
9. Execution / Playbook 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42B

现在开始实现，不要只给计划。
```
