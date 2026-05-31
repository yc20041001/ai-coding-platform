# Milestone 43A: Governance Copilot Workspace & Guided Operations Console

## 1. 背景

截至 Milestone 42C，平台已经形成了一条很完整的治理能力链：

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
```

现在系统已经可以回答很多问题：

```text
有哪些治理事项？
哪些事项快逾期？
哪些 owner 超载？
有哪些风险预测？
有哪些 recipe 可以复用？
哪些 recipe 最有效？
有哪些优化建议？
```

但对于一线治理操作员来说，仍然存在一个很现实的问题：

```text
这些能力分散在多个面板和多个上下文里。
我现在接手一个 recommendation，第一步该看什么？
当前最值得先处理的是哪几件事？
该用哪个 playbook、哪个 recipe、哪个知识条目？
如果要 handoff，我应该先确认哪些信息？
```

换句话说，平台已经具备：

```text
强大的治理能力模块
```

但还缺少：

```text
面向治理操作员的统一工作台与引导式操作视图
```

Milestone 43A 的目标就是新增：

```text
Governance Copilot Workspace & Guided Operations Console
```

让平台从：

```text
有很多强能力模块
```

升级为：

```text
有一个统一入口，把 recommendation、forecast、recipe、knowledge、execution 串成一条操作路径
```

---

## 2. 总目标

实现统一的治理操作工作台：

1. 新增 Governance Workspace Session 数据模型。
2. 新增 Governance Guided Task 数据模型。
3. 新增 Governance Next Step Recommendation 数据模型。
4. 支持把 recommendation / escalation / forecast / recipe / handoff 聚合进同一工作台。
5. 支持为操作员生成“当前最值得处理的事项队列”。
6. 支持 guided remediation 视图。
7. 支持 recommendation -> playbook -> recipe -> knowledge -> handoff 的串联导航。
8. 支持生成 context-aware next-step recommendation。
9. 支持导出 Workspace Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
操作员需要自己在多个面板来回切换
```

升级为：

```text
操作员可以在单一 workspace 中看到优先事项、推荐动作、可复用资产和执行上下文
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改 recommendation / waiver / execution plan / recipe / knowledge 原始记录，除非显式更新 workspace/session/task 本身状态。
4. 不自动批准 waiver。
5. 不自动完成 recommendation。
6. 不自动分配 owner。
7. 不调用真实 AI 自动执行治理动作。
8. copilot 只做导航、聚合、排序、建议，不做真实自动操作。
9. next-step recommendation 必须基于已有结构化规则与上下文，不调用外部模型。
10. 不破坏 1-42C 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 workspace session / guided task / next-step recommendation 表。
2. 聚合 recommendation、forecast、recipe、knowledge、execution plan、handoff、waiver 数据。
3. 提供 guided operations console、上下文面板、工作队列、建议卡片。
4. 导出 Markdown summary。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V53__init_governance_copilot_workspace_tables.sql
```

### 4.1 governance_workspace_session

```sql
CREATE TABLE governance_workspace_session (
    id BIGINT PRIMARY KEY,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    session_status VARCHAR(32) NOT NULL,
    focus_mode VARCHAR(32) NOT NULL,
    selected_project_id BIGINT NULL,
    selected_recommendation_id BIGINT NULL,
    selected_owner_id BIGINT NULL,
    context_summary TEXT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_workspace_session_operator(operator_id, session_status),
    KEY idx_governance_workspace_session_project(selected_project_id, session_status)
);
```

### 4.2 governance_guided_task

```sql
CREATE TABLE governance_guided_task (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    recommendation_id BIGINT NULL,
    task_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL,
    linked_playbook_key VARCHAR(64) NULL,
    linked_recipe_key VARCHAR(64) NULL,
    linked_knowledge_entry_id BIGINT NULL,
    due_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_guided_task_session(session_id, task_status),
    KEY idx_governance_guided_task_priority(priority, task_status),
    KEY idx_governance_guided_task_recommendation(recommendation_id)
);
```

### 4.3 governance_next_step_recommendation

```sql
CREATE TABLE governance_next_step_recommendation (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL,
    suggestion_rank INT NOT NULL DEFAULT 0,
    suggestion_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary_text TEXT NULL,
    rationale_text TEXT NULL,
    expected_outcome_text TEXT NULL,
    action_payload_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_next_step_session(session_id, suggestion_rank),
    KEY idx_governance_next_step_task(guided_task_id),
    KEY idx_governance_next_step_recommendation(recommendation_id)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceWorkspaceSessionStatus.java
GovernanceWorkspaceFocusMode.java
GovernanceGuidedTaskType.java
GovernanceGuidedTaskStatus.java
GovernanceNextStepSuggestionType.java
```

### 5.1 GovernanceWorkspaceSessionStatus

```text
ACTIVE
PAUSED
COMPLETED
ARCHIVED
```

### 5.2 GovernanceWorkspaceFocusMode

```text
PRIORITY_FIRST
OWNER_CENTRIC
PROJECT_CENTRIC
WAIVER_REDUCTION
BACKLOG_REDUCTION
```

### 5.3 GovernanceGuidedTaskType

```text
TRIAGE_RECOMMENDATION
RUN_PLAYBOOK
APPLY_RECIPE_GUIDANCE
PREPARE_HANDOFF
REVIEW_WAIVER
REDUCE_BACKLOG
```

### 5.4 GovernanceGuidedTaskStatus

```text
OPEN
IN_PROGRESS
DONE
SKIPPED
BLOCKED
```

### 5.5 GovernanceNextStepSuggestionType

```text
OPEN_PLAYBOOK
OPEN_RECIPE
OPEN_KNOWLEDGE
START_HANDOFF
REVIEW_WAIVER
REVIEW_FORECAST
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceWorkspaceSessionEntity.java
GovernanceGuidedTaskEntity.java
GovernanceNextStepRecommendationEntity.java

GovernanceWorkspaceSessionMapper.java
GovernanceGuidedTaskMapper.java
GovernanceNextStepRecommendationMapper.java
```

DTO 建议：

```text
CreateGovernanceWorkspaceSessionRequest.java
UpdateGovernanceWorkspaceSessionRequest.java
GovernanceWorkspaceSessionResponse.java

GovernanceGuidedTaskResponse.java
UpdateGovernanceGuidedTaskRequest.java

GovernanceNextStepRecommendationResponse.java
GovernanceWorkspaceDashboardResponse.java
GovernanceWorkspaceSummaryResponse.java
```

### 6.1 GovernanceWorkspaceDashboardResponse

建议字段：

```text
activeSession
focusMode
openTaskCount
inProgressTaskCount
blockedTaskCount
topGuidedTasks
topNextStepRecommendations
selectedRecommendationContext
selectedProjectContext
selectedOwnerContext
```

### 6.2 GovernanceGuidedTaskResponse

建议字段：

```text
id
sessionId
recommendationId
taskType
priority
taskStatus
title
summary
sourceType
sourceId
linkedPlaybookKey
linkedRecipeKey
linkedKnowledgeEntryId
dueAt
createTime
updateTime
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceWorkspaceService.java
GovernanceGuidedOperationsService.java
GovernanceNextStepRecommendationService.java
```

### 7.1 GovernanceWorkspaceService

职责：

1. 创建与更新 workspace session。
2. 支持 focus mode 切换。
3. 聚合当前 session 的 recommendation / project / owner / forecast 上下文。
4. 管理 session 状态：

```text
ACTIVE -> PAUSED -> ACTIVE
ACTIVE -> COMPLETED
COMPLETED -> ARCHIVED
```

### 7.2 GovernanceGuidedOperationsService

职责：

1. 基于当前上下文生成 guided task 列表。
2. 关联 recommendation / playbook / recipe / knowledge / handoff。
3. 按优先级、逾期、阻塞风险排序。
4. 支持 task 状态流转：

```text
OPEN -> IN_PROGRESS -> DONE
OPEN -> SKIPPED
OPEN/IN_PROGRESS -> BLOCKED
BLOCKED -> IN_PROGRESS
```

### 7.3 GovernanceNextStepRecommendationService

职责：

1. 生成 context-aware next-step recommendations。
2. 根据当前 selected recommendation / project / owner / focus mode 给出 3-5 条下一步建议。
3. 输出 rationale 与 expected outcome。
4. 不执行动作，只生成可点击导航建议。

示例规则：

```text
若 recommendation 高优先级且无 execution plan -> OPEN_PLAYBOOK
若有 execution plan 但无 recipe -> OPEN_RECIPE
若 owner 变更待处理 -> START_HANDOFF
若有活跃 waiver -> REVIEW_WAIVER
若项目 forecast 高风险 -> REVIEW_FORECAST
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceWorkspaceController.java
```

建议端点：

### 8.1 Workspace Session

```text
POST   /api/governance-workspace/sessions
GET    /api/governance-workspace/sessions
GET    /api/governance-workspace/sessions/{sessionId}
PUT    /api/governance-workspace/sessions/{sessionId}
POST   /api/governance-workspace/sessions/{sessionId}/status
```

### 8.2 Guided Tasks

```text
GET    /api/governance-workspace/sessions/{sessionId}/tasks
PUT    /api/governance-workspace/tasks/{taskId}
POST   /api/governance-workspace/tasks/{taskId}/status
```

### 8.3 Next-step Recommendations

```text
POST   /api/governance-workspace/sessions/{sessionId}/refresh
GET    /api/governance-workspace/sessions/{sessionId}/next-steps
GET    /api/governance-workspace/dashboard
GET    /api/governance-workspace/report
```

权限建议：

```text
查看：ADMIN
创建 / 更新 session：ADMIN
更新 guided task：ADMIN
refresh workspace：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Guided Task 排序

默认排序建议：

```text
1. priority
2. overdue recommendation
3. blocked recommendation
4. owner overload risk
5. active waiver risk
```

### 9.2 Next-step Recommendation 数量

每个 session 默认输出：

```text
3 ~ 5 条建议
```

优先级示例：

```text
高优 recommendation 无 plan -> 优先
有 plan 但 blocked -> 次优
有 waiver 且即将过期 -> 次优
forecast 高风险 -> 次优
```

### 9.3 Workspace Summary

建议至少包含：

```text
当前 focus mode
开放 guided task 数
阻塞 task 数
最高优先 recommendation
最值得先做的 3 个 next step
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceWorkspaceConsole.vue
GovernanceGuidedTaskPanel.vue
GovernanceNextStepPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceWorkspaceConsole

展示：

1. 当前 active session
2. focus mode 切换
3. 选中的 recommendation / project / owner 上下文卡片
4. refresh workspace 按钮

### 10.2 GovernanceGuidedTaskPanel

展示：

1. guided task 列表
2. priority / status / dueAt
3. linked playbook / recipe / knowledge 标签
4. task 状态流转按钮

### 10.3 GovernanceNextStepPanel

展示：

1. 3-5 条 next-step 建议卡
2. suggestion type / rationale / expected outcome
3. 可跳转到对应 playbook / recipe / knowledge / handoff 的按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. Workspace 作为统一入口，布局要更聚焦“当前应该做什么”
4. next-step 卡片要清楚、短促、可执行

---

## 11. 后端测试要求

新增：

```text
GovernanceWorkspaceCopilotIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. create workspace session success
2. update workspace session success
3. status active -> paused
4. paused -> active
5. active -> completed
6. completed -> archived
7. invalid session transition reject
8. refresh workspace success
9. guided task generated from high priority recommendation
10. guided task links playbook
11. guided task links recipe
12. guided task links knowledge
13. guided task status open -> in_progress
14. in_progress -> done
15. open -> blocked
16. blocked -> in_progress
17. invalid guided task transition reject
18. next-step recommendation count between 3 and 5
19. next-step recommendation open playbook generated
20. next-step recommendation open recipe generated
21. next-step recommendation start handoff generated
22. next-step recommendation review waiver generated
23. next-step recommendation review forecast generated
24. dashboard returns active session
25. dashboard includes selected recommendation context
26. dashboard includes selected owner context
27. dashboard includes selected project context
28. report export markdown success
29. unauthorized access reject
30. non-admin update reject
31. empty dataset returns empty dashboard
32. focus mode changes task ordering
33. high overdue recommendation ranked first
34. active waiver raises suggestion priority
35. owner overload raises forecast-related suggestion
36. refresh is idempotent for same snapshot context

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-workspace.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 workspace console
2. guided task panel renders
3. next-step panel renders
4. focus mode 控件可见
5. refresh workspace 按钮可见
6. linked playbook / recipe / knowledge 标签可见
7. next-step 卡片可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 workspace 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-43a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance workspace / guided task / next-step 表说明
3. GovernanceWorkspaceService 设计说明
4. GovernanceGuidedOperationsService 设计说明
5. GovernanceNextStepRecommendationService 设计说明
6. GovernanceWorkspaceConsole 说明
7. GovernanceGuidedTaskPanel 说明
8. GovernanceNextStepPanel 说明
9. Workspace / Copilot 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43B

---

## 14. 验收标准

必须全部满足：

1. governance_workspace_session / governance_guided_task / governance_next_step_recommendation 三张表已落库
2. workspace session 可创建 / 更新 / 状态流转
3. guided task 可生成 / 更新 / 状态流转
4. next-step recommendation 可生成 / 查询
5. dashboard / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. 工作台导航和建议逻辑清晰、可解释、可追踪

---

## 15. 完成后的价值

完成 43A 后，平台将从：

```text
有很多治理面板和治理能力模块
```

升级为：

```text
有一个统一的治理工作台，帮助操作员快速知道“现在最该做什么”
```

这一步会让平台从“治理系统”真正开始接近“治理 Copilot 工作台”。

---

## 16. 后续建议

Milestone 43A 完成后，建议进入：

```text
Milestone 43B: Governance Operator Productivity, Session Insights & Action Reuse
```

重点可包括：

1. operator session productivity metrics
2. common action sequence mining
3. next-step success feedback
4. reusable action bundles
5. workspace session insights

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 43A。

文档路径：
docs/milestone-43a-governance-copilot-workspace-guided-operations-console.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 42C effectiveness analytics / optimization 基础上，新增 governance copilot workspace 与 guided operations console。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录，除非显式更新 workspace/task/session 自身状态。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不调用真实 AI 自动执行治理动作。
10. next-step recommendation 只做导航与建议，不做真实操作。
11. 不要破坏 1-42C 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V53__init_governance_copilot_workspace_tables.sql。
2. 新增 governance_workspace_session / governance_guided_task / governance_next_step_recommendation 三张表。
3. 新增 5 个枚举：
   - GovernanceWorkspaceSessionStatus
   - GovernanceWorkspaceFocusMode
   - GovernanceGuidedTaskType
   - GovernanceGuidedTaskStatus
   - GovernanceNextStepSuggestionType
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceWorkspaceService。
6. 新增 GovernanceGuidedOperationsService。
7. 新增 GovernanceNextStepRecommendationService。
8. 新增 API：
   - workspace session CRUD / status
   - guided task list / update / status
   - workspace refresh / next-steps / dashboard / report
9. 前端新增：
   - GovernanceWorkspaceConsole.vue
   - GovernanceGuidedTaskPanel.vue
   - GovernanceNextStepPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-43a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance workspace / guided task / next-step 表说明
3. GovernanceWorkspaceService 设计说明
4. GovernanceGuidedOperationsService 设计说明
5. GovernanceNextStepRecommendationService 设计说明
6. GovernanceWorkspaceConsole 说明
7. GovernanceGuidedTaskPanel 说明
8. GovernanceNextStepPanel 说明
9. Workspace / Copilot 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43B

现在开始实现，不要只给计划。
```
