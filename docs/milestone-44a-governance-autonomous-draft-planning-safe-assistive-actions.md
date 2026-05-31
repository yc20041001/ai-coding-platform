# Milestone 44A: Governance Autonomous Draft Planning & Safe Assistive Actions

## 1. 背景

截至 Milestone 43C，平台已经形成了一条完整的治理闭环：

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
```

现在平台已经可以：

```text
发现治理问题
跟踪 recommendation 与 waiver
做 forecast、simulation、policy tuning
沉淀 playbook、recipe、knowledge
记录 operator 行为与反馈
对 copilot 建议做排序调优
```

但仍有一个很实际的“最后一公里”问题：

```text
copilot 已经能告诉操作员下一步做什么，
但在真正准备执行治理动作时，
操作员还需要自己手工整理 remediation plan、
组装 recommendation package、
核对 checklist、
确认哪些动作是安全的、哪些动作只是草稿。
```

换句话说，平台现在已经有：

```text
强大的观察、分析、学习、调优能力
```

但还缺少：

```text
一个“安全的起草层”——
能帮操作员把下一步操作预组装成 draft plan / package / checklist，
同时严格保持“只辅助、不越权执行”。
```

Milestone 44A 的目标就是新增：

```text
Governance Autonomous Draft Planning & Safe Assistive Actions
```

让平台从：

```text
copilot 会提示下一步
```

升级为：

```text
copilot 会把下一步自动整理成可审阅的 draft plan，
并明确标记哪些只是建议、哪些需要人工确认、哪些绝不自动执行
```

---

## 2. 总目标

实现安全辅助起草层：

1. 新增 Draft Remediation Plan 数据模型。
2. 新增 Safe Assistive Action 数据模型。
3. 新增 Recommendation Package 数据模型。
4. 支持根据 recommendation / guided task / bundle / forecast 自动组装 draft remediation plan。
5. 支持生成 safe assistive action checklist。
6. 支持 recommendation package 预组装。
7. 支持 operator pre-submit workspace 视图。
8. 支持 outcome review loop，把 draft plan 的采用与否记录回系统。
9. 支持导出 Markdown Draft Plan / Recommendation Package。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
copilot 只能告诉操作员该做什么
```

升级为：

```text
copilot 能先把操作草稿、安全核对项、提交包都整理好，
再交给操作员确认执行
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
7. 不自动触发外部通知、发布、变更、回滚、审批。
8. 不调用真实 AI 自动执行治理动作。
9. autonomous draft planning 只负责“起草 / 组装 / 标注 / 提示”，绝不直接执行。
10. safe assistive action 只能是 checklist / package / prefill / draft，不得成为真实操作入口。
11. 不破坏 1-43C 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 draft plan / safe action / package 三张表。
2. 生成可编辑但未提交的草稿内容。
3. 生成 safety note、confirmation flag、risk tag。
4. 提供 operator pre-submit workspace。
5. 导出 Markdown 草稿。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V56__init_governance_draft_planning_tables.sql
```

### 4.1 governance_draft_remediation_plan

```sql
CREATE TABLE governance_draft_remediation_plan (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NULL,
    session_id BIGINT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    plan_status VARCHAR(32) NOT NULL,
    plan_title VARCHAR(255) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    summary_text TEXT NULL,
    goal_text TEXT NULL,
    proposed_steps_json JSON NOT NULL,
    linked_bundle_id BIGINT NULL,
    linked_playbook_key VARCHAR(64) NULL,
    linked_recipe_key VARCHAR(64) NULL,
    risk_level VARCHAR(32) NOT NULL,
    human_confirmation_required TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_draft_remediation_plan_recommendation(recommendation_id, plan_status),
    KEY idx_governance_draft_remediation_plan_session(session_id, plan_status)
);
```

### 4.2 governance_safe_assistive_action

```sql
CREATE TABLE governance_safe_assistive_action (
    id BIGINT PRIMARY KEY,
    draft_plan_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    action_status VARCHAR(32) NOT NULL,
    action_title VARCHAR(255) NOT NULL,
    action_summary TEXT NULL,
    safety_level VARCHAR(32) NOT NULL,
    confirmation_required TINYINT(1) NOT NULL DEFAULT 1,
    checklist_json JSON NOT NULL,
    prefill_payload_json JSON NULL,
    action_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_safe_assistive_action_plan(draft_plan_id, action_order),
    KEY idx_governance_safe_assistive_action_status(action_status, safety_level)
);
```

### 4.3 governance_recommendation_package

```sql
CREATE TABLE governance_recommendation_package (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NULL,
    draft_plan_id BIGINT NULL,
    package_status VARCHAR(32) NOT NULL,
    package_title VARCHAR(255) NOT NULL,
    package_summary TEXT NULL,
    recommendation_context_json JSON NOT NULL,
    attachments_json JSON NULL,
    review_notes_text TEXT NULL,
    submit_ready_flag TINYINT(1) NOT NULL DEFAULT 0,
    submitted_flag TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_recommendation_package_recommendation(recommendation_id, package_status),
    KEY idx_governance_recommendation_package_plan(draft_plan_id, package_status)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceDraftPlanStatus.java
GovernanceDraftPlanScopeType.java
GovernanceAssistiveActionType.java
GovernanceAssistiveActionStatus.java
GovernanceAssistiveSafetyLevel.java
GovernanceRecommendationPackageStatus.java
```

### 5.1 GovernanceDraftPlanStatus

```text
DRAFT
READY_FOR_REVIEW
REVIEWED
ARCHIVED
```

### 5.2 GovernanceDraftPlanScopeType

```text
RECOMMENDATION
PROJECT
OWNER
PORTFOLIO
```

### 5.3 GovernanceAssistiveActionType

```text
OPEN_PLAYBOOK_DRAFT
OPEN_RECIPE_DRAFT
PREPARE_HANDOFF_NOTE
PREPARE_WAIVER_REVIEW
PREPARE_FORECAST_CHECK
PREPARE_RISK_SUMMARY
```

### 5.4 GovernanceAssistiveActionStatus

```text
PENDING
REVIEWED
SKIPPED
READY
```

### 5.5 GovernanceAssistiveSafetyLevel

```text
INFO
SAFE
CAUTION
REVIEW_REQUIRED
```

### 5.6 GovernanceRecommendationPackageStatus

```text
DRAFT
READY
REVIEWED
ARCHIVED
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceDraftRemediationPlanEntity.java
GovernanceSafeAssistiveActionEntity.java
GovernanceRecommendationPackageEntity.java

GovernanceDraftRemediationPlanMapper.java
GovernanceSafeAssistiveActionMapper.java
GovernanceRecommendationPackageMapper.java
```

DTO 建议：

```text
CreateGovernanceDraftPlanRequest.java
UpdateGovernanceDraftPlanRequest.java
GovernanceDraftRemediationPlanResponse.java

GovernanceSafeAssistiveActionResponse.java
UpdateGovernanceSafeAssistiveActionRequest.java

GovernanceRecommendationPackageResponse.java
GovernanceDraftPlanningDashboardResponse.java
GovernanceDraftPlanningReportResponse.java
```

### 6.1 GovernanceDraftPlanningDashboardResponse

建议字段：

```text
draftPlanCount
readyForReviewCount
reviewRequiredActionCount
submitReadyPackageCount
topDraftPlans
topAssistiveActions
topPackages
latestReviewSignals
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceDraftPlanningService.java
GovernanceSafeAssistiveActionService.java
GovernanceRecommendationPackageService.java
```

### 7.1 GovernanceDraftPlanningService

职责：

1. 根据 recommendation / guided task / reuse bundle / forecast 自动组装 draft remediation plan。
2. 生成 proposed steps、goal、summary、risk level。
3. 支持 draft plan 状态流转：

```text
DRAFT -> READY_FOR_REVIEW -> REVIEWED
REVIEWED -> ARCHIVED
```

4. 支持从 recommendation context 自动预填 package 内容。
5. 支持导出 Markdown plan。

组装原则建议：

```text
高优 recommendation -> 优先纳入 remediation plan
有 bundle -> 引用 bundle action sequence
有 playbook / recipe -> 引用对应 key
有 waiver / forecast 风险 -> 增加 review step
```

### 7.2 GovernanceSafeAssistiveActionService

职责：

1. 为 draft plan 生成 safe assistive action checklist。
2. 每个 action 只做 prefill / note / checklist，不做真实执行。
3. 支持 action 状态流转：

```text
PENDING -> REVIEWED
PENDING -> SKIPPED
REVIEWED -> READY
```

4. 输出 safety level 与 confirmation_required。
5. 支持在 UI 中解释“为什么这个动作只能作为草稿辅助”。

### 7.3 GovernanceRecommendationPackageService

职责：

1. 把 recommendation context、draft plan、review note 组装成 package。
2. 计算 submit_ready_flag。
3. 管理 package 状态：

```text
DRAFT -> READY -> REVIEWED -> ARCHIVED
```

4. 导出 Markdown package。
5. 不提供真实 submit，只提供 pre-submit workspace。

submit_ready_flag 建议规则：

```text
draft plan 已 READY_FOR_REVIEW 或 REVIEWED
至少有 1 个 assistive action 为 READY
review note / summary 非空
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceDraftPlanningController.java
```

建议端点：

### 8.1 Draft Plan

```text
POST   /api/governance-draft-plans
GET    /api/governance-draft-plans
GET    /api/governance-draft-plans/{planId}
PUT    /api/governance-draft-plans/{planId}
POST   /api/governance-draft-plans/{planId}/status
POST   /api/governance-draft-plans/{planId}/refresh
```

### 8.2 Safe Assistive Actions

```text
GET    /api/governance-draft-plans/{planId}/assistive-actions
PUT    /api/governance-assistive-actions/{actionId}
POST   /api/governance-assistive-actions/{actionId}/status
```

### 8.3 Recommendation Package / Dashboard / Report

```text
GET    /api/governance-recommendation-packages
GET    /api/governance-recommendation-packages/{packageId}
POST   /api/governance-recommendation-packages/{packageId}/status
GET    /api/governance-draft-planning/dashboard
GET    /api/governance-draft-planning/report
```

权限建议：

```text
查看：ADMIN
创建 / 更新 draft plan：ADMIN
更新 assistive action：ADMIN
更新 package 状态：ADMIN
```

---

## 9. Draft / Safe Assistive 规则建议

### 9.1 Draft Plan 组装

默认组装内容建议：

```text
1. 背景摘要
2. 风险与目标
3. 建议步骤（3-6 条）
4. 关联 playbook / recipe / bundle
5. 需要人工确认的事项
6. 预期结果
```

### 9.2 Safe Assistive Action 生成

默认可生成的辅助动作：

```text
准备 playbook 草稿
准备 recipe 草稿
准备 handoff note
准备 waiver 审阅摘要
准备 forecast 风险检查清单
准备 recommendation context summary
```

### 9.3 Safety Level 判定

建议规则：

```text
纯摘要 / 纯预填 / 纯导出 -> SAFE
涉及 owner / waiver / overdue 风险解释 -> CAUTION
涉及最终确认与提交 -> REVIEW_REQUIRED
信息展示型 -> INFO
```

### 9.4 Outcome Review Loop

draft plan 与 package 后续应至少记录：

```text
是否被采用
是否被修改后采用
是否被放弃
```

本阶段只要求预留字段或 report 结构，不要求做完整自动闭环执行。

---

## 10. 前端设计

新增组件建议：

```text
GovernanceDraftPlanningPanel.vue
GovernanceAssistiveActionPanel.vue
GovernanceRecommendationPackagePanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceDraftPlanningPanel

展示：

1. draft remediation plan 列表
2. plan status / risk level / linked bundle
3. goal / summary / proposed steps
4. refresh / status 按钮

### 10.2 GovernanceAssistiveActionPanel

展示：

1. 当前 draft plan 的 assistive actions
2. safety level / confirmation required
3. checklist 展开区
4. 状态流转按钮

### 10.3 GovernanceRecommendationPackagePanel

展示：

1. recommendation package 列表
2. submit ready 标签
3. summary / attachments / review notes
4. Markdown report 导出按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 必须明确显示“草稿 / 安全辅助 / 不自动执行”提示
4. 对 REVIEW_REQUIRED 的动作要突出显示

---

## 11. 后端测试要求

新增：

```text
GovernanceDraftPlanningIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. create draft plan success
2. update draft plan success
3. refresh draft plan success
4. draft plan status draft -> ready_for_review
5. ready_for_review -> reviewed
6. reviewed -> archived
7. invalid draft plan transition reject
8. draft plan generated from recommendation
9. draft plan links bundle
10. draft plan links playbook
11. draft plan links recipe
12. draft plan calculates risk level
13. assistive actions generated for plan
14. assistive action list ordered by actionOrder
15. assistive action pending -> reviewed
16. reviewed -> ready
17. pending -> skipped
18. invalid assistive action transition reject
19. assistive action safety level info
20. assistive action safety level safe
21. assistive action safety level caution
22. assistive action safety level review_required
23. package generated from recommendation context
24. package submit ready false when incomplete
25. package submit ready true when prerequisites met
26. package status draft -> ready
27. ready -> reviewed
28. reviewed -> archived
29. dashboard returns top draft plans
30. dashboard returns reviewRequiredActionCount
31. report export markdown success
32. empty data returns empty dashboard safely
33. unauthorized access reject
34. non-admin update reject
35. refresh is idempotent for same source context
36. humanConfirmationRequired defaults true

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-draft-planning.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 draft planning 面板
2. assistive action 面板可见
3. recommendation package 面板可见
4. risk level / safety level 标签可见
5. review required 标签可见
6. submit ready 标签可见或空态可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 draft plan / package 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-44a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 draft remediation plan / safe assistive action / recommendation package 表说明
3. GovernanceDraftPlanningService 设计说明
4. GovernanceSafeAssistiveActionService 设计说明
5. GovernanceRecommendationPackageService 设计说明
6. GovernanceDraftPlanningPanel 说明
7. GovernanceAssistiveActionPanel 说明
8. GovernanceRecommendationPackagePanel 说明
9. Draft Planning / Safe Assistive 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44B

---

## 14. 验收标准

必须全部满足：

1. governance_draft_remediation_plan / governance_safe_assistive_action / governance_recommendation_package 三张表已落库
2. draft plan 可创建 / 更新 / 刷新 / 状态流转
3. assistive action 可生成 / 查询 / 状态流转
4. package 可查询 / 状态流转
5. dashboard / report 可导出
6. 安全辅助动作不执行真实操作
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 44A 后，平台将从：

```text
copilot 能导航、记忆、学习、调优
```

升级为：

```text
copilot 能在不越权执行的前提下，先帮操作员把治理动作整理成可审阅的草稿与提交包
```

这一步会让 Governance Copilot 从“会给建议的治理助手”进一步升级成“会起草但不擅自执行的安全辅助台”。

---

## 16. 后续建议

Milestone 44A 完成后，建议进入：

```text
Milestone 44B: Governance Outcome Review, Draft Adoption Tracking & Assistive Quality Evaluation
```

重点可包括：

1. draft adoption / modification / rejection tracking
2. assistive action usefulness rating
3. package review quality scoring
4. operator outcome review loop
5. assistive planning quality dashboard

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 44A。

文档路径：
docs/milestone-44a-governance-autonomous-draft-planning-safe-assistive-actions.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 43C adaptive guidance / copilot tuning 基础上，新增 autonomous draft planning 与 safe assistive actions。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动触发外部通知、发布、回滚、审批。
10. draft planning 只做起草、预填、组装、提示，不做真实执行。
11. assistive action 只做 checklist / prefill / summary / package，不做真实动作。
12. 不要破坏 1-43C 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 43A workspace / 43B reuse bundle / 43C tuning 与 next-step 数据结构，不要重复造概念。
17. draft plan / package / assistive action 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V56__init_governance_draft_planning_tables.sql。
2. 新增 governance_draft_remediation_plan / governance_safe_assistive_action / governance_recommendation_package 三张表。
3. 新增 6 个枚举：
   - GovernanceDraftPlanStatus
   - GovernanceDraftPlanScopeType
   - GovernanceAssistiveActionType
   - GovernanceAssistiveActionStatus
   - GovernanceAssistiveSafetyLevel
   - GovernanceRecommendationPackageStatus
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceDraftPlanningService。
6. 新增 GovernanceSafeAssistiveActionService。
7. 新增 GovernanceRecommendationPackageService。
8. 新增 API：
   - draft plan CRUD / refresh / status
   - assistive action list / update / status
   - recommendation package list / detail / status
   - dashboard / report
9. 前端新增：
   - GovernanceDraftPlanningPanel.vue
   - GovernanceAssistiveActionPanel.vue
   - GovernanceRecommendationPackagePanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-44a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 draft remediation plan / safe assistive action / recommendation package 表说明
3. GovernanceDraftPlanningService 设计说明
4. GovernanceSafeAssistiveActionService 设计说明
5. GovernanceRecommendationPackageService 设计说明
6. GovernanceDraftPlanningPanel 说明
7. GovernanceAssistiveActionPanel 说明
8. GovernanceRecommendationPackagePanel 说明
9. Draft Planning / Safe Assistive 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44B

现在开始实现，不要只给计划。
```
