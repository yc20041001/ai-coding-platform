# Milestone 43B: Governance Operator Memory, Learning Loop & Guided Remediation Reuse

## 1. 背景

截至 Milestone 43A，平台已经形成一条完整的治理工作流主线：

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
```

现在系统已经能够把 recommendation、playbook、recipe、knowledge、handoff、forecast 聚合到一个统一工作台中，并给出规则化 next-step recommendation。

但在治理操作层，还有一个非常实际的问题没有沉淀：

```text
某个操作员在处理类似事项时，通常会怎么走？
哪些 guided task 序列最常被完成？
哪些 next-step recommendation 经常被接受，哪些经常被跳过？
哪些 remediation flow 片段值得复用？
不同 operator 的 session 里，哪些动作序列实际提升了关闭率和处理效率？
```

也就是说，平台现在已经有：

```text
治理工作台与引导式操作入口
```

但还缺少：

```text
面向“操作员行为沉淀与复用”的学习回路
```

Milestone 43B 的目标，就是新增：

```text
Governance Operator Memory, Learning Loop & Guided Remediation Reuse
```

让平台从：

```text
给出当前建议与导航
```

升级为：

```text
记住历史操作模式，识别哪些引导最有效，并把高价值 remediation 片段沉淀为可复用资产
```

---

## 2. 总目标

实现治理操作员学习回路：

1. 新增 Operator Action Memory 数据模型。
2. 新增 Workspace Session Insight 数据模型。
3. 新增 Guided Remediation Reuse Bundle 数据模型。
4. 记录 operator 在 workspace 中的关键动作。
5. 统计 guided task / next-step recommendation 的接受、跳过、完成表现。
6. 挖掘常见 action sequence pattern。
7. 生成 session productivity / action reuse / recommendation acceptance 洞察。
8. 支持把高价值 action sequence 沉淀为 reusable remediation bundle。
9. 支持导出 Operator Learning Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
copilot 只告诉操作员接下来做什么
```

升级为：

```text
copilot 能告诉操作员：历史上什么做法最有效、哪些操作路径最值得复用
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改 recommendation / waiver / execution plan / recipe / knowledge 原始记录。
4. 不自动修改 workspace session 的业务上下文，除非用户显式操作 session 自身状态。
5. 不自动批准 waiver。
6. 不自动完成 recommendation。
7. 不自动分配 owner。
8. 不调用真实 AI 自动总结 operator 行为。
9. learning loop 只基于历史结构化动作、task 状态变化、session 结果与 recommendation 结果。
10. reusable remediation bundle 只做建议与复用入口，不自动注入真实执行。
11. 不破坏 1-43A 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 operator memory / session insight / reuse bundle 三张表。
2. 新增 operator 行为记录、session 洞察、复用 bundle 管理能力。
3. 对 guided task、next-step recommendation、session 结果做聚合分析。
4. 导出学习报告与复用建议。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V54__init_governance_operator_learning_tables.sql
```

### 4.1 governance_operator_action_memory

```sql
CREATE TABLE governance_operator_action_memory (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    action_type VARCHAR(64) NOT NULL,
    action_target_type VARCHAR(64) NOT NULL,
    action_target_id BIGINT NULL,
    accepted_flag TINYINT(1) NOT NULL DEFAULT 0,
    success_flag TINYINT(1) NOT NULL DEFAULT 0,
    duration_seconds INT NULL,
    note_text TEXT NULL,
    action_payload_json JSON NULL,
    occurred_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_operator_action_memory_session(session_id, occurred_at),
    KEY idx_governance_operator_action_memory_operator(operator_id, occurred_at),
    KEY idx_governance_operator_action_memory_guided_task(guided_task_id)
);
```

### 4.2 governance_workspace_session_insight

```sql
CREATE TABLE governance_workspace_session_insight (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    insight_window VARCHAR(32) NOT NULL,
    total_actions INT NOT NULL DEFAULT 0,
    accepted_recommendation_count INT NOT NULL DEFAULT 0,
    dismissed_recommendation_count INT NOT NULL DEFAULT 0,
    completed_guided_task_count INT NOT NULL DEFAULT 0,
    blocked_guided_task_count INT NOT NULL DEFAULT 0,
    avg_action_duration_seconds INT NULL,
    productivity_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    dominant_action_pattern VARCHAR(128) NULL,
    summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_workspace_session_insight_session(session_id, insight_window),
    KEY idx_governance_workspace_session_insight_operator(operator_id, captured_at)
);
```

### 4.3 governance_remediation_reuse_bundle

```sql
CREATE TABLE governance_remediation_reuse_bundle (
    id BIGINT PRIMARY KEY,
    bundle_key VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NULL,
    priority VARCHAR(32) NULL,
    effectiveness_level VARCHAR(32) NOT NULL,
    reuse_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    action_sequence_json JSON NOT NULL,
    source_session_id BIGINT NULL,
    source_operator_id BIGINT NULL,
    source_operator_name VARCHAR(128) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    summary_text TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_remediation_reuse_bundle_key(bundle_key),
    KEY idx_governance_remediation_reuse_bundle_category(category, enabled),
    KEY idx_governance_remediation_reuse_bundle_guardrail(guardrail_key, enabled)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceOperatorActionType.java
GovernanceActionTargetType.java
GovernanceSessionInsightWindow.java
GovernanceProductivityLevel.java
GovernanceReuseBundleEffectivenessLevel.java
```

### 5.1 GovernanceOperatorActionType

```text
OPEN_RECOMMENDATION
OPEN_PLAYBOOK
OPEN_RECIPE
OPEN_KNOWLEDGE
START_HANDOFF
UPDATE_GUIDED_TASK
COMPLETE_GUIDED_TASK
DISMISS_NEXT_STEP
ACCEPT_NEXT_STEP
REVIEW_WAIVER
REVIEW_FORECAST
EXPORT_REPORT
```

### 5.2 GovernanceActionTargetType

```text
RECOMMENDATION
GUIDED_TASK
PLAYBOOK
RECIPE
KNOWLEDGE
HANDOFF
WAIVER
FORECAST
SESSION
REPORT
```

### 5.3 GovernanceSessionInsightWindow

```text
SESSION
DAY_7
DAY_14
```

### 5.4 GovernanceProductivityLevel

```text
HIGH
MEDIUM
LOW
AT_RISK
```

### 5.5 GovernanceReuseBundleEffectivenessLevel

```text
TOP
USEFUL
LIMITED
LOW_VALUE
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceOperatorActionMemoryEntity.java
GovernanceWorkspaceSessionInsightEntity.java
GovernanceRemediationReuseBundleEntity.java

GovernanceOperatorActionMemoryMapper.java
GovernanceWorkspaceSessionInsightMapper.java
GovernanceRemediationReuseBundleMapper.java
```

DTO 建议：

```text
RecordGovernanceOperatorActionRequest.java
GovernanceOperatorActionMemoryResponse.java

GovernanceWorkspaceSessionInsightResponse.java
GovernanceOperatorLearningDashboardResponse.java

GovernanceRemediationReuseBundleResponse.java
CreateGovernanceRemediationReuseBundleRequest.java
UpdateGovernanceRemediationReuseBundleRequest.java

GovernanceOperatorLearningReportResponse.java
```

### 6.1 GovernanceOperatorLearningDashboardResponse

建议字段：

```text
totalSessions
totalActions
acceptanceRate
guidedTaskCompletionRate
avgActionDurationSeconds
topOperators
topActionPatterns
topReuseBundles
latestSessionInsights
```

### 6.2 GovernanceRemediationReuseBundleResponse

建议字段：

```text
id
bundleKey
title
category
guardrailKey
priority
effectivenessLevel
reuseCount
successRate
actionSequence
sourceSessionId
sourceOperatorId
sourceOperatorName
enabled
summaryText
createTime
updateTime
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceOperatorMemoryService.java
GovernanceSessionLearningService.java
GovernanceRemediationReuseService.java
```

### 7.1 GovernanceOperatorMemoryService

职责：

1. 记录 operator 在 workspace 中的关键动作。
2. 支持 accepted / dismissed / success / duration 采集。
3. 支持按 session、operator、guided task 查询动作序列。
4. 为后续 insight 与 reuse bundle 提供原始事件流。

建议方法：

```text
recordAction(...)
listSessionActions(...)
listOperatorActions(...)
```

### 7.2 GovernanceSessionLearningService

职责：

1. 基于 session action memory 生成 session insight。
2. 统计 recommendation acceptance / dismissal。
3. 统计 guided task 完成率、阻塞率、平均动作耗时。
4. 挖掘 dominant action pattern。
5. 生成 Operator Learning Report Markdown。

示例公式：

```text
productivityScore =
  acceptanceRate * 0.35
  + guidedTaskCompletionRate * 0.35
  + max(0, 100 - avgActionDurationMinutes) * 0.15
  + successActionRate * 0.15
```

等级建议：

```text
>= 80 HIGH
>= 60 MEDIUM
>= 35 LOW
< 35 AT_RISK
```

### 7.3 GovernanceRemediationReuseService

职责：

1. 从高成功 action sequence 中生成 reusable remediation bundle。
2. 支持 bundle CRUD 与启停。
3. 按 category / guardrail / priority 匹配复用 bundle。
4. 统计 bundle reuseCount / successRate。
5. 对低复用、低成功 bundle 提供降权，但不自动删除。

匹配优先级建议：

```text
EXACT(category + guardrail + priority)
CATEGORY_GUARDRAIL(category + guardrail)
CATEGORY_ONLY(category)
DEFAULT
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceOperatorLearningController.java
```

建议端点：

### 8.1 Operator Action Memory

```text
POST   /api/governance-operator-memory/actions
GET    /api/governance-operator-memory/actions
GET    /api/governance-operator-memory/sessions/{sessionId}/actions
```

### 8.2 Session Insight / Dashboard / Report

```text
POST   /api/governance-operator-memory/insights/refresh
GET    /api/governance-operator-memory/insights
GET    /api/governance-operator-memory/dashboard
GET    /api/governance-operator-memory/report
```

### 8.3 Reuse Bundles

```text
POST   /api/governance-operator-memory/reuse-bundles
GET    /api/governance-operator-memory/reuse-bundles
GET    /api/governance-operator-memory/reuse-bundles/{bundleId}
PUT    /api/governance-operator-memory/reuse-bundles/{bundleId}
POST   /api/governance-operator-memory/reuse-bundles/{bundleId}/status
POST   /api/governance-operator-memory/reuse-bundles/refresh
```

权限建议：

```text
查看：ADMIN
记录 action：ADMIN
刷新 insight / bundle：ADMIN
创建 / 更新 bundle：ADMIN
```

---

## 9. 学习与复用规则建议

### 9.1 Dominant Action Pattern

按 session 内动作序列统计，示例：

```text
OPEN_PLAYBOOK -> OPEN_RECIPE -> UPDATE_GUIDED_TASK -> COMPLETE_GUIDED_TASK
```

如果同一模式在近 14 天内出现 >= 3 次且成功率 >= 70%，可作为 dominant pattern。

### 9.2 Reuse Bundle 生成建议

满足以下条件可生成 bundle：

```text
1. action sequence 长度 >= 3
2. successFlag 为 true 的关键动作比例 >= 70%
3. guided task 最终为 DONE
4. 对应 recommendation 为 COMPLETED 或已显著推进
```

### 9.3 Recommendation Acceptance 指标

```text
acceptanceRate = acceptedNextStep / totalNextStepShown
dismissalRate = dismissedNextStep / totalNextStepShown
```

### 9.4 Learning Report

至少包含：

```text
总 session 数
总 action 数
acceptance rate
guided task completion rate
top operator
dominant action patterns
top reuse bundles
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceOperatorMemoryPanel.vue
GovernanceSessionInsightPanel.vue
GovernanceRemediationReusePanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceOperatorMemoryPanel

展示：

1. operator action memory 列表
2. action type / target / accepted / success / duration
3. session 过滤
4. operator 过滤

### 10.2 GovernanceSessionInsightPanel

展示：

1. session productivity 指标卡
2. acceptance rate / completion rate / avg duration
3. dominant action pattern
4. top operator 与最近 session insight

### 10.3 GovernanceRemediationReusePanel

展示：

1. reuse bundle 列表
2. effectiveness level / reuseCount / successRate
3. action sequence 展开
4. 启停状态
5. refresh bundle 按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. action sequence 要可展开查看
4. insight 与 reuse 面板要突出“哪些做法值得复用”

---

## 11. 后端测试要求

新增：

```text
GovernanceOperatorLearningIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. record action success
2. list actions by session
3. list actions by operator
4. action memory preserves accepted flag
5. action memory preserves success flag
6. refresh session insight success
7. refresh insight idempotent for same session/window
8. session insight computes totalActions
9. session insight computes acceptanceRate
10. session insight computes completionRate
11. session insight computes avgActionDuration
12. productivityScore computed
13. productivity level high
14. productivity level medium
15. productivity level low
16. productivity level at_risk
17. dominant action pattern extracted
18. empty session returns zero insight
19. learning dashboard returns top operators
20. learning dashboard returns top bundles
21. learning report export markdown success
22. create reuse bundle success
23. update reuse bundle success
24. enable reuse bundle success
25. disable reuse bundle success
26. reuse bundle exact match success
27. reuse bundle category_guardrail match success
28. reuse bundle category_only match success
29. refresh reuse bundles from successful sequences success
30. low success sequence not promoted to bundle
31. duplicate bundle key reject
32. unauthorized access reject
33. non-admin update reject
34. report with empty data returns empty markdown sections
35. accepted recommendation raises acceptance rate
36. completed guided task raises success pattern score

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-operator-learning.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 operator memory 面板
2. session insight 面板可见
3. reuse bundle 面板可见
4. action sequence 可展开
5. productivity 指标卡可见
6. reuse count / success rate 标签可见
7. refresh 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 operator memory 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-43b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 operator memory / session insight / reuse bundle 表说明
3. GovernanceOperatorMemoryService 设计说明
4. GovernanceSessionLearningService 设计说明
5. GovernanceRemediationReuseService 设计说明
6. GovernanceOperatorMemoryPanel 说明
7. GovernanceSessionInsightPanel 说明
8. GovernanceRemediationReusePanel 说明
9. Operator Learning / Reuse 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43C

---

## 14. 验收标准

必须全部满足：

1. governance_operator_action_memory / governance_workspace_session_insight / governance_remediation_reuse_bundle 三张表已落库
2. operator action 可记录 / 查询
3. session insight 可刷新 / 查询 / 聚合
4. learning dashboard / report 可导出
5. reuse bundle 可 CRUD / 启停 / 刷新
6. dominant pattern / acceptance / completion 指标可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 43B 后，平台将从：

```text
有统一治理工作台，但操作经验仍主要停留在个体脑内
```

升级为：

```text
平台开始记住哪些治理操作最有效，并把高价值动作序列沉淀成可复用资产
```

这会让 governance copilot 从“导航器”进一步升级成“有经验沉淀的工作助手”。

---

## 16. 后续建议

Milestone 43B 完成后，建议进入：

```text
Milestone 43C: Governance Adaptive Guidance, Operator Feedback & Copilot Tuning Loop
```

重点可包括：

1. operator 对 next-step recommendation 的反馈评分
2. recommendation acceptance / dismissal 原因归因
3. adaptive recommendation ranking
4. reuse bundle effectiveness feedback
5. copilot tuning dashboard

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 43B。

文档路径：
docs/milestone-43b-governance-operator-memory-learning-loop-guided-remediation-reuse.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 43A governance copilot workspace / guided operations 基础上，新增 operator memory、learning loop 与 guided remediation reuse。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不调用真实 AI 自动总结 operator 行为。
10. reusable remediation bundle 只做建议与复用入口，不自动执行。
11. 不要破坏 1-43A 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
15. 优先复用 43A 已有 workspace session、guided task、next-step recommendation 数据结构，不要重复发明平行概念。
16. insight / dashboard / report 若历史数据不足，必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V54__init_governance_operator_learning_tables.sql。
2. 新增 governance_operator_action_memory / governance_workspace_session_insight / governance_remediation_reuse_bundle 三张表。
3. 新增 5 个枚举：
   - GovernanceOperatorActionType
   - GovernanceActionTargetType
   - GovernanceSessionInsightWindow
   - GovernanceProductivityLevel
   - GovernanceReuseBundleEffectivenessLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceOperatorMemoryService。
6. 新增 GovernanceSessionLearningService。
7. 新增 GovernanceRemediationReuseService。
8. 新增 API：
   - operator action record / list
   - session insight refresh / list / dashboard / report
   - remediation reuse bundle CRUD / status / refresh
9. 前端新增：
   - GovernanceOperatorMemoryPanel.vue
   - GovernanceSessionInsightPanel.vue
   - GovernanceRemediationReusePanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-43b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 operator memory / session insight / reuse bundle 表说明
3. GovernanceOperatorMemoryService 设计说明
4. GovernanceSessionLearningService 设计说明
5. GovernanceRemediationReuseService 设计说明
6. GovernanceOperatorMemoryPanel 说明
7. GovernanceSessionInsightPanel 说明
8. GovernanceRemediationReusePanel 说明
9. Operator Learning / Reuse 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43C

现在开始实现，不要只给计划。
```
