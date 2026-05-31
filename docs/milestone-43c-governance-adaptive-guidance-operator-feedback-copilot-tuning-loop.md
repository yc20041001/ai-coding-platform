# Milestone 43C: Governance Adaptive Guidance, Operator Feedback & Copilot Tuning Loop

## 1. 背景

截至 Milestone 43B，平台已经形成了一条非常完整的治理主线：

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
```

现在平台已经可以：

```text
记录 operator 如何使用 workspace
统计哪些 next-step 被接受或忽略
识别 dominant action pattern
把高价值 remediation flow 沉淀成 reuse bundle
```

但还有一个关键问题没有闭环：

```text
copilot 现在给出的 next-step recommendation，
到底哪些建议被操作员认为“真有用”？
哪些建议虽然被展示了，但总被忽略？
哪些 bundle 或导航卡片虽然常出现，但实际上帮助不大？
不同 focus mode 下，建议排序是否应该调整？
```

换句话说，平台已经具备：

```text
操作记忆与行为沉淀
```

但还缺少：

```text
基于 operator 明确反馈与行为结果，对 copilot guidance 进行持续调优的闭环
```

Milestone 43C 的目标就是新增：

```text
Governance Adaptive Guidance, Operator Feedback & Copilot Tuning Loop
```

让平台从：

```text
copilot 会给建议，但建议权重基本固定
```

升级为：

```text
copilot 会根据历史反馈、接受率、完成结果和 bundle 效果，持续调整建议排序与推荐倾向
```

---

## 2. 总目标

实现 copilot 调优闭环：

1. 新增 Operator Feedback 数据模型。
2. 新增 Adaptive Guidance Signal 数据模型。
3. 新增 Copilot Tuning Snapshot 数据模型。
4. 支持记录 operator 对 next-step recommendation / bundle / guided task 的显式反馈。
5. 支持归因 recommendation acceptance / dismissal reason。
6. 支持生成 adaptive guidance signals。
7. 支持对 suggestion type / focus mode / recommendation category 做排序偏好统计。
8. 支持生成 copilot tuning dashboard。
9. 支持导出 Copilot Tuning Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
copilot 能导航，也能记忆
```

升级为：

```text
copilot 能根据操作员反馈持续调优自己的导航与推荐逻辑
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
7. 不调用真实 AI 自动理解 free-text 反馈。
8. copilot tuning 只基于结构化反馈、accepted/dismissed 行为、guided task outcome 与 bundle 使用结果。
9. adaptive guidance 只做排序权重与建议倾向调整，不自动执行任何治理动作。
10. 不破坏 1-43B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 feedback / tuning signal / tuning snapshot 三张表。
2. 新增 operator feedback 记录与统计。
3. 生成 adaptive guidance signal 与 tuning snapshot。
4. 在 workspace 中展示“为什么这个建议排得更靠前”。
5. 导出 tuning report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V55__init_governance_copilot_tuning_tables.sql
```

### 4.1 governance_operator_feedback

```sql
CREATE TABLE governance_operator_feedback (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    suggestion_type VARCHAR(64) NULL,
    suggestion_id BIGINT NULL,
    guided_task_id BIGINT NULL,
    reuse_bundle_id BIGINT NULL,
    feedback_target_type VARCHAR(64) NOT NULL,
    feedback_rating INT NOT NULL,
    helpful_flag TINYINT(1) NOT NULL DEFAULT 0,
    accepted_flag TINYINT(1) NOT NULL DEFAULT 0,
    reason_code VARCHAR(64) NULL,
    note_text TEXT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_operator_feedback_session(session_id, create_time),
    KEY idx_governance_operator_feedback_operator(operator_id, create_time),
    KEY idx_governance_operator_feedback_target(feedback_target_type, create_time)
);
```

### 4.2 governance_adaptive_guidance_signal

```sql
CREATE TABLE governance_adaptive_guidance_signal (
    id BIGINT PRIMARY KEY,
    signal_type VARCHAR(64) NOT NULL,
    focus_mode VARCHAR(32) NULL,
    category VARCHAR(64) NULL,
    suggestion_type VARCHAR(64) NULL,
    recommendation_priority VARCHAR(32) NULL,
    acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    completion_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    weight_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    signal_level VARCHAR(32) NOT NULL,
    rationale_text TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_adaptive_guidance_signal_type(signal_type, captured_at),
    KEY idx_governance_adaptive_guidance_signal_focus(focus_mode, captured_at)
);
```

### 4.3 governance_copilot_tuning_snapshot

```sql
CREATE TABLE governance_copilot_tuning_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_window VARCHAR(32) NOT NULL,
    total_feedback_count INT NOT NULL DEFAULT 0,
    acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    dismissal_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    top_suggestion_type VARCHAR(64) NULL,
    weakest_suggestion_type VARCHAR(64) NULL,
    top_focus_mode VARCHAR(32) NULL,
    weakest_focus_mode VARCHAR(32) NULL,
    tuning_confidence_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_copilot_tuning_snapshot_window(snapshot_window, captured_at),
    KEY idx_governance_copilot_tuning_snapshot_captured(captured_at)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceFeedbackTargetType.java
GovernanceFeedbackReasonCode.java
GovernanceAdaptiveSignalType.java
GovernanceAdaptiveSignalLevel.java
GovernanceCopilotTuningWindow.java
```

### 5.1 GovernanceFeedbackTargetType

```text
NEXT_STEP
GUIDED_TASK
REUSE_BUNDLE
WORKSPACE_SESSION
```

### 5.2 GovernanceFeedbackReasonCode

```text
HELPFUL
TOO_GENERIC
NOT_RELEVANT
TOO_COMPLEX
MISSING_CONTEXT
LOW_IMPACT
GOOD_BUNDLE
BAD_ORDERING
```

### 5.3 GovernanceAdaptiveSignalType

```text
SUGGESTION_TYPE_WEIGHT
FOCUS_MODE_WEIGHT
CATEGORY_WEIGHT
BUNDLE_REUSE_SIGNAL
DISMISSAL_RISK_SIGNAL
```

### 5.4 GovernanceAdaptiveSignalLevel

```text
BOOST
KEEP
WATCH
DOWNRANK
```

### 5.5 GovernanceCopilotTuningWindow

```text
DAY_7
DAY_14
DAY_30
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceOperatorFeedbackEntity.java
GovernanceAdaptiveGuidanceSignalEntity.java
GovernanceCopilotTuningSnapshotEntity.java

GovernanceOperatorFeedbackMapper.java
GovernanceAdaptiveGuidanceSignalMapper.java
GovernanceCopilotTuningSnapshotMapper.java
```

DTO 建议：

```text
RecordGovernanceOperatorFeedbackRequest.java
GovernanceOperatorFeedbackResponse.java

GovernanceAdaptiveGuidanceSignalResponse.java
GovernanceCopilotTuningSnapshotResponse.java
GovernanceCopilotTuningDashboardResponse.java
GovernanceCopilotTuningReportResponse.java
```

### 6.1 GovernanceCopilotTuningDashboardResponse

建议字段：

```text
totalFeedbackCount
acceptanceRate
dismissalRate
avgFeedbackRating
topSuggestionTypes
weakestSuggestionTypes
topFocusModes
dismissalRiskSignals
latestSignals
latestSnapshot
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceOperatorFeedbackService.java
GovernanceAdaptiveGuidanceService.java
GovernanceCopilotTuningService.java
```

### 7.1 GovernanceOperatorFeedbackService

职责：

1. 记录 operator 对 next-step / guided task / reuse bundle 的显式反馈。
2. 采集 rating、helpfulFlag、acceptedFlag、reasonCode。
3. 支持按 session、operator、target 查询反馈。
4. 为 tuning signal 提供原始反馈基础。

### 7.2 GovernanceAdaptiveGuidanceService

职责：

1. 聚合 operator feedback、43A next-step acceptance、43B action memory。
2. 计算 suggestion type / focus mode / category 的权重信号。
3. 对高接受率、高评分建议生成 `BOOST`。
4. 对高 dismissal、高低评分建议生成 `DOWNRANK`。
5. 输出 rationale_text，解释调优原因。

建议公式：

```text
weightScore =
  acceptanceRate * 0.4
  + completionRate * 0.3
  + avgFeedbackRating * 12
  - dismissalRate * 0.25
```

建议分级：

```text
>= 80 BOOST
>= 55 KEEP
>= 30 WATCH
< 30 DOWNRANK
```

### 7.3 GovernanceCopilotTuningService

职责：

1. 刷新 tuning snapshot。
2. 聚合 top / weakest suggestion type 与 focus mode。
3. 输出 tuning confidence score。
4. 导出 Copilot Tuning Report Markdown。
5. 为 workspace 提供“为什么当前建议排位变化”的解释依据。

建议公式：

```text
tuningConfidenceScore =
  min(totalFeedbackCount, 50) * 1.2
  + acceptanceRate * 0.25
  + avgFeedbackRating * 8
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceCopilotTuningController.java
```

建议端点：

### 8.1 Feedback

```text
POST   /api/governance-copilot/feedback
GET    /api/governance-copilot/feedback
GET    /api/governance-copilot/sessions/{sessionId}/feedback
```

### 8.2 Adaptive Guidance Signals

```text
POST   /api/governance-copilot/signals/refresh
GET    /api/governance-copilot/signals
GET    /api/governance-copilot/signals/dashboard
```

### 8.3 Tuning Snapshot / Report

```text
POST   /api/governance-copilot/tuning/refresh
GET    /api/governance-copilot/tuning/snapshots
GET    /api/governance-copilot/tuning/dashboard
GET    /api/governance-copilot/tuning/report
```

权限建议：

```text
查看：ADMIN
记录 feedback：ADMIN
刷新 signals / tuning：ADMIN
```

---

## 9. 自适应与调优规则建议

### 9.1 Suggestion Type 调优

若某 suggestion type 在近 14 天：

```text
acceptanceRate >= 70
avgFeedbackRating >= 4
completionRate >= 60
```

则生成：

```text
BOOST
```

若满足：

```text
dismissalRate >= 50
avgFeedbackRating <= 2
```

则生成：

```text
DOWNRANK
```

### 9.2 Focus Mode 调优

若某 focus mode 下：

```text
guidedTask completion 更高
avgFeedbackRating 更高
```

则提升该 focus mode 的推荐优先级参考值。

### 9.3 Bundle Effectiveness Feedback

若某 reuse bundle：

```text
reuseCount >= 3
avgFeedbackRating >= 4
accepted related next-step >= 70%
```

则在 signal 中输出：

```text
BUNDLE_REUSE_SIGNAL / BOOST
```

### 9.4 Dismissal Risk Signal

若某 suggestion type 或 category：

```text
dismissalRate >= 60
reasonCode 主要集中在 NOT_RELEVANT / TOO_GENERIC
```

则生成：

```text
DISMISSAL_RISK_SIGNAL / DOWNRANK
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceOperatorFeedbackPanel.vue
GovernanceAdaptiveGuidancePanel.vue
GovernanceCopilotTuningPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceOperatorFeedbackPanel

展示：

1. operator feedback 列表
2. rating / helpful / accepted / reasonCode
3. target type 过滤
4. session 过滤

### 10.2 GovernanceAdaptiveGuidancePanel

展示：

1. adaptive guidance signal 列表
2. suggestion type / focus mode / level / weight score
3. rationale_text
4. boost / downrank 标签

### 10.3 GovernanceCopilotTuningPanel

展示：

1. tuning snapshot 指标卡
2. top / weakest suggestion type
3. top / weakest focus mode
4. tuning confidence score
5. report 导出入口

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. “为什么提升 / 降权”必须在 UI 上可解释
4. tuning 面板要突出“copilot 是否在变得更有用”

---

## 11. 后端测试要求

新增：

```text
GovernanceCopilotTuningIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. record operator feedback success
2. list feedback by session
3. list feedback by target type
4. feedback preserves rating
5. feedback preserves reason code
6. feedback preserves helpful flag
7. refresh adaptive signals success
8. refresh signals idempotent for same window
9. high acceptance signal becomes boost
10. low rating signal becomes downrank
11. high dismissal signal creates dismissal risk
12. bundle reuse signal created
13. focus mode weight signal created
14. category weight signal created
15. signal rationale populated
16. weight score computed
17. refresh tuning snapshot success
18. snapshot computes totalFeedbackCount
19. snapshot computes acceptanceRate
20. snapshot computes dismissalRate
21. snapshot computes avgFeedbackRating
22. snapshot computes topSuggestionType
23. snapshot computes weakestSuggestionType
24. snapshot computes topFocusMode
25. tuning confidence score computed
26. tuning dashboard returns latest signals
27. tuning dashboard returns latest snapshot
28. tuning report export markdown success
29. empty data returns empty signals safely
30. empty data returns empty tuning dashboard safely
31. unauthorized access reject
32. non-admin feedback mutation reject
33. helpful feedback increases avg rating
34. accepted feedback increases boost tendency
35. repeated not relevant feedback causes downrank
36. bundle feedback raises reuse signal score

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-copilot-tuning.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 feedback 面板
2. adaptive guidance 面板可见
3. copilot tuning 面板可见
4. feedback rating / reason 标签可见
5. boost / downrank 信号标签可见
6. tuning confidence 指标卡可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 feedback / tuning 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-43c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 feedback / adaptive guidance / tuning snapshot 表说明
3. GovernanceOperatorFeedbackService 设计说明
4. GovernanceAdaptiveGuidanceService 设计说明
5. GovernanceCopilotTuningService 设计说明
6. GovernanceOperatorFeedbackPanel 说明
7. GovernanceAdaptiveGuidancePanel 说明
8. GovernanceCopilotTuningPanel 说明
9. Copilot Tuning / Adaptive Guidance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44A

---

## 14. 验收标准

必须全部满足：

1. governance_operator_feedback / governance_adaptive_guidance_signal / governance_copilot_tuning_snapshot 三张表已落库
2. operator feedback 可记录 / 查询
3. adaptive signals 可刷新 / 查询 / dashboard
4. tuning snapshot / report 可导出
5. suggestion type / focus mode / category 调优逻辑可计算
6. boost / keep / watch / downrank 可解释
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 43C 后，平台将从：

```text
copilot 能导航，也能记忆 operator 行为
```

升级为：

```text
copilot 能根据 operator 明确反馈和历史结果调整自己的推荐倾向
```

这一步会让 Governance Copilot 从“有经验记忆”进一步升级成“会调优的操作助手”。

---

## 16. 后续建议

Milestone 43C 完成后，建议进入：

```text
Milestone 44A: Governance Autonomous Draft Planning & Safe Assistive Actions
```

重点可包括：

1. 自动生成 draft remediation plan
2. safe assistive action checklist
3. recommendation package assembly
4. operator pre-submit workspace
5. copilot outcome review loop

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 43C。

文档路径：
docs/milestone-43c-governance-adaptive-guidance-operator-feedback-copilot-tuning-loop.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 43B operator memory / learning loop / reuse 基础上，新增 operator feedback、adaptive guidance 与 copilot tuning loop。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不调用真实 AI 自动理解 free-text 反馈。
10. adaptive guidance 只做排序倾向与解释，不做真实自动治理动作。
11. 不要破坏 1-43B 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
15. 优先复用 43A 的 workspace / next-step、43B 的 action memory / reuse bundle，不要重复造概念。
16. feedback / signals / tuning snapshot 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V55__init_governance_copilot_tuning_tables.sql。
2. 新增 governance_operator_feedback / governance_adaptive_guidance_signal / governance_copilot_tuning_snapshot 三张表。
3. 新增 5 个枚举：
   - GovernanceFeedbackTargetType
   - GovernanceFeedbackReasonCode
   - GovernanceAdaptiveSignalType
   - GovernanceAdaptiveSignalLevel
   - GovernanceCopilotTuningWindow
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceOperatorFeedbackService。
6. 新增 GovernanceAdaptiveGuidanceService。
7. 新增 GovernanceCopilotTuningService。
8. 新增 API：
   - feedback record / list
   - adaptive signals refresh / list / dashboard
   - tuning refresh / snapshots / dashboard / report
9. 前端新增：
   - GovernanceOperatorFeedbackPanel.vue
   - GovernanceAdaptiveGuidancePanel.vue
   - GovernanceCopilotTuningPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-43c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 feedback / adaptive guidance / tuning snapshot 表说明
3. GovernanceOperatorFeedbackService 设计说明
4. GovernanceAdaptiveGuidanceService 设计说明
5. GovernanceCopilotTuningService 设计说明
6. GovernanceOperatorFeedbackPanel 说明
7. GovernanceAdaptiveGuidancePanel 说明
8. GovernanceCopilotTuningPanel 说明
9. Copilot Tuning / Adaptive Guidance 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44A

现在开始实现，不要只给计划。
```
