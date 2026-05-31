# Milestone 42C: Governance Effectiveness Analytics & Recipe Optimization Loop

## 1. 背景

截至 Milestone 42B，平台已经具备治理知识沉淀与复用能力：

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
```

现在系统已经能回答：

```text
以前类似 recommendation 是怎么处理的？
有哪些 recipe 可复用？
哪些 pattern / recipe 匹配当前 recommendation？
哪些知识条目效果更高、复用更多？
```

但知识库和 recipe 库一旦建立，很快会面临下一个治理问题：

```text
哪些 recipe 真正有效？
哪些 playbook 虽然经常用，但完成效率很差？
某类 recommendation 与某类 recipe 的组合效果是否更好？
哪些低价值 recipe 应该被淘汰或降级？
哪些高价值 recipe 应该被优先推荐？
```

也就是说，42B 让平台具备了：

```text
经验沉淀与复用能力
```

但还没有：

```text
围绕 recipe / playbook / knowledge 的效果评估与持续优化闭环
```

Milestone 42C 的目标就是新增：

```text
Governance Effectiveness Analytics & Recipe Optimization Loop
```

让平台从：

```text
知道哪些经验可以复用
```

升级为：

```text
知道哪些经验最有效、哪些应该优化、哪些应该淘汰
```

---

## 2. 总目标

实现治理资产效果分析与优化闭环：

1. 新增 Governance Recipe Effectiveness Snapshot 数据模型。
2. 新增 Governance Playbook Analytics Record 数据模型。
3. 新增 Governance Optimization Suggestion 数据模型。
4. 支持统计 recipe 使用次数、完成率、平均耗时、失败率。
5. 支持统计 playbook / execution plan completion efficiency。
6. 支持建立 recommendation outcome 与 recipe/pattern 之间的关联分析。
7. 支持生成 recipe effectiveness trend。
8. 支持生成 low-value / high-value recipe 优化建议。
9. 支持导出 Governance Optimization Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有可复用的 recipe 和知识库
```

升级为：

```text
有可衡量、可比较、可优化的治理资产闭环
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改已有 recommendation / execution plan / recipe / pattern 原始记录。
4. 不自动启停 recipe。
5. 不自动删除低价值 recipe。
6. 不自动重写知识条目内容。
7. 不调用真实 AI 自动生成优化结论。
8. analytics 只基于平台内历史执行记录、recipe 匹配记录和 outcome 字段。
9. optimization suggestion 只提供建议，不自动应用。
10. 不破坏 1-42B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 effectiveness snapshot / playbook analytics / optimization suggestion 表。
2. 聚合 execution plan、recipe usage、knowledge reuse、recommendation completion 数据。
3. 新增 analytics dashboard、trend、ranking、suggestion 面板。
4. 导出 Markdown summary / report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V52__init_governance_effectiveness_analytics_tables.sql
```

### 4.1 governance_recipe_effectiveness_snapshot

```sql
CREATE TABLE governance_recipe_effectiveness_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    recipe_id BIGINT NOT NULL,
    recipe_key VARCHAR(64) NOT NULL,
    recipe_name VARCHAR(255) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_completion_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    failure_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_recipe_effectiveness_date(snapshot_date, effectiveness_score),
    KEY idx_governance_recipe_effectiveness_recipe(recipe_id, snapshot_date),
    KEY idx_governance_recipe_effectiveness_level(snapshot_date, effectiveness_level)
);
```

### 4.2 governance_playbook_analytics_record

```sql
CREATE TABLE governance_playbook_analytics_record (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    template_key VARCHAR(64) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    plan_count INT NOT NULL DEFAULT 0,
    completed_plan_count INT NOT NULL DEFAULT 0,
    blocked_plan_count INT NOT NULL DEFAULT 0,
    avg_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_step_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_resolution_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    related_recipe_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    KEY idx_governance_playbook_analytics_date(snapshot_date, avg_completion_rate),
    KEY idx_governance_playbook_analytics_template(template_key, snapshot_date)
);
```

### 4.3 governance_optimization_suggestion

```sql
CREATE TABLE governance_optimization_suggestion (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    current_metric_value VARCHAR(255) NULL,
    suggested_action TEXT NOT NULL,
    expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_optimization_suggestion_date(snapshot_date, priority),
    KEY idx_governance_optimization_suggestion_target(target_type, target_key)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceEffectivenessLevelV2.java
GovernanceOptimizationSuggestionType.java
GovernanceOptimizationTargetType.java
GovernanceAnalyticsWindow.java
```

### 5.1 GovernanceEffectivenessLevelV2

```text
LOW
MEDIUM
HIGH
TOP
```

### 5.2 GovernanceOptimizationSuggestionType

```text
PROMOTE_RECIPE
PRUNE_RECIPE
REFINE_PLAYBOOK
SPLIT_PATTERN
MERGE_DUPLICATE_RECIPES
```

### 5.3 GovernanceOptimizationTargetType

```text
RECIPE
PLAYBOOK
PATTERN
KNOWLEDGE_ENTRY
```

### 5.4 GovernanceAnalyticsWindow

```text
LAST_7_DAYS
LAST_30_DAYS
LAST_90_DAYS
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceRecipeEffectivenessSnapshotEntity.java
GovernancePlaybookAnalyticsRecordEntity.java
GovernanceOptimizationSuggestionEntity.java

GovernanceRecipeEffectivenessSnapshotMapper.java
GovernancePlaybookAnalyticsRecordMapper.java
GovernanceOptimizationSuggestionMapper.java
```

DTO 建议：

```text
GovernanceRecipeEffectivenessSnapshotResponse.java
GovernanceRecipeEffectivenessDashboardResponse.java

GovernancePlaybookAnalyticsRecordResponse.java
GovernancePlaybookAnalyticsDashboardResponse.java

GovernanceOptimizationSuggestionResponse.java
GovernanceOptimizationDashboardResponse.java

GovernanceOptimizationReportResponse.java
```

### 6.1 GovernanceRecipeEffectivenessDashboardResponse

建议字段：

```text
snapshotDate
recipeCount
topRecipeCount
highRecipeCount
lowRecipeCount
averageEffectivenessScore
topRecipes
lowValueRecipes
trendWindow
```

### 6.2 GovernanceOptimizationDashboardResponse

建议字段：

```text
snapshotDate
suggestionCount
highPrioritySuggestionCount
promoteSuggestionCount
pruneSuggestionCount
refinePlaybookCount
topSuggestions
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceEffectivenessAnalyticsService.java
GovernanceRecipeOptimizationService.java
GovernancePlaybookPerformanceService.java
```

### 7.1 GovernanceEffectivenessAnalyticsService

职责：

1. 统计 recipe usage / completion / success / avg completion hours。
2. 生成 recipe effectiveness snapshot。
3. 支持按时间窗口输出 trend。
4. 计算 effectiveness level。

建议评分示例：

```text
effectivenessScore =
  successRate * 0.5
  + min(usageCount, 20) * 2
  + max(0, 100 - avgCompletionHours) * 0.2
  - failureRate * 0.3
```

### 7.2 GovernancePlaybookPerformanceService

职责：

1. 统计 playbook template 的计划使用次数。
2. 统计平均完成率、平均步骤完成率、平均解决时间。
3. 输出 playbook analytics ranking。
4. 支持 recipe / playbook / outcome 关联分析。

### 7.3 GovernanceRecipeOptimizationService

职责：

1. 识别 high-value recipe。
2. 识别 low-value recipe。
3. 识别冗余 recipe / pattern。
4. 生成 optimization suggestions：
   - promote
   - prune
   - refine
   - split
   - merge
5. 导出 optimization report。

---

## 8. API 设计

新增 Controller：

```text
GovernanceEffectivenessController.java
```

建议端点：

### 8.1 Recipe Effectiveness

```text
POST   /api/governance-effectiveness/recipes/refresh
GET    /api/governance-effectiveness/recipes
GET    /api/governance-effectiveness/recipes/dashboard
GET    /api/governance-effectiveness/recipes/trend
```

### 8.2 Playbook Analytics

```text
POST   /api/governance-effectiveness/playbooks/refresh
GET    /api/governance-effectiveness/playbooks
GET    /api/governance-effectiveness/playbooks/dashboard
```

### 8.3 Optimization Suggestions

```text
POST   /api/governance-effectiveness/optimizations/refresh
GET    /api/governance-effectiveness/optimizations
GET    /api/governance-effectiveness/optimizations/dashboard
GET    /api/governance-effectiveness/report
```

权限建议：

```text
查看：ADMIN
refresh analytics / suggestions：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 High-value Recipe

可定义为：

```text
usageCount >= 5
and successRate >= 80
and failureRate <= 10
and effectivenessScore >= 75
```

### 9.2 Low-value Recipe

可定义为：

```text
usageCount >= 3
and (successRate < 40 or effectivenessScore < 35)
```

### 9.3 Recipe 优化建议示例

```text
高使用高分 -> PROMOTE_RECIPE
低分低完成率 -> PRUNE_RECIPE
高 usage 但高 blocked / 低 step completion -> REFINE_PLAYBOOK
category/guardrail 重叠严重 -> MERGE_DUPLICATE_RECIPES
单 recipe 覆盖过宽且效果分化大 -> SPLIT_PATTERN
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceRecipeEffectivenessPanel.vue
GovernancePlaybookAnalyticsPanel.vue
GovernanceOptimizationSuggestionPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceRecipeEffectivenessPanel

展示：

1. effectiveness 指标卡
2. top recipes
3. low-value recipes
4. trend 窗口切换（7/30/90 天）

### 10.2 GovernancePlaybookAnalyticsPanel

展示：

1. playbook analytics 列表
2. avg completion / avg resolution hours
3. blocked plan 指标
4. recipe 关联数

### 10.3 GovernanceOptimizationSuggestionPanel

展示：

1. optimization suggestion 列表
2. priority / type / target
3. current metric / suggested action
4. expected impact / rationale

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 优化建议要突出优先级和预期收益
4. analytics 面板强调排序、趋势和对比

---

## 11. 后端测试要求

新增：

```text
GovernanceEffectivenessOptimizationIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. refresh recipe effectiveness success
2. refresh playbook analytics success
3. refresh optimization suggestions success
4. recipe effectiveness score calculation correct
5. effectiveness level low
6. effectiveness level medium
7. effectiveness level high
8. effectiveness level top
9. top recipe ordering correct
10. low-value recipe detection works
11. playbook analytics completion rate correct
12. playbook analytics resolution hours correct
13. promote recipe suggestion generated
14. prune recipe suggestion generated
15. refine playbook suggestion generated
16. merge duplicate recipes suggestion generated
17. split pattern suggestion generated
18. recipe trend 7d returns data
19. recipe trend 30d returns data
20. recipe trend 90d returns data
21. optimization dashboard counts correct
22. report export markdown success
23. unauthorized access reject
24. non-admin refresh reject
25. empty dataset returns empty dashboard
26. high usage but low score triggers prune
27. high score and high usage triggers promote
28. blocked-heavy playbook triggers refine
29. duplicate overlap triggers merge
30. broad low-consistency recipe triggers split
31. average effectiveness score correct
32. suggestion sorting by priority correct
33. recipe count summary correct
34. playbook analytics ranking returned
35. trend data ordered by snapshotDate
36. rationale text populated

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-effectiveness.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 recipe effectiveness panel
2. playbook analytics panel renders
3. optimization suggestion panel renders
4. trend 窗口切换控件可见
5. top / low-value recipe 区域可见
6. suggestion priority / impact 可见
7. report / dashboard 区域可见
8. no JS errors on page load

如果测试环境没有 seeded governance execution 数据：

1. 显式断言空态
2. 不把“无 effectiveness 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-42c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance effectiveness / analytics / optimization 表说明
3. GovernanceEffectivenessAnalyticsService 设计说明
4. GovernanceRecipeOptimizationService 设计说明
5. GovernancePlaybookPerformanceService 设计说明
6. GovernanceRecipeEffectivenessPanel 说明
7. GovernancePlaybookAnalyticsPanel 说明
8. GovernanceOptimizationSuggestionPanel 说明
9. Effectiveness / Optimization 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43A

---

## 14. 验收标准

必须全部满足：

1. governance_recipe_effectiveness_snapshot / governance_playbook_analytics_record / governance_optimization_suggestion 三张表已落库
2. recipe effectiveness 可刷新 / 查询 / trend
3. playbook analytics 可刷新 / 查询
4. optimization suggestions 可刷新 / 查询
5. dashboard / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. effectiveness / optimization 逻辑清晰、可解释

---

## 15. 完成后的价值

完成 42C 后，平台将从：

```text
有可复用的 recipe 和知识库
```

升级为：

```text
有 recipe 效果分析、playbook 表现分析和持续优化闭环
```

这一步会让治理知识系统从“沉淀库”进化为“自我优化的执行资产系统”。

---

## 16. 后续建议

Milestone 42C 完成后，建议进入：

```text
Milestone 43A: Governance Copilot Workspace & Guided Operations Console
```

重点可包括：

1. 面向治理操作员的统一工作台
2. workflow / forecast / recipe / knowledge 统一入口
3. 引导式 remediation 视图
4. context-aware next-step recommendation
5. governance copilot shell（仍保持非自动执行）

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 42C。

文档路径：
docs/milestone-42c-governance-effectiveness-analytics-recipe-optimization-loop.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 42B governance knowledge / pattern / recipe 基础上，新增 effectiveness analytics 与 recipe optimization loop。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recipe / pattern / knowledge 原始记录。
6. 不要自动启停 recipe。
7. 不要自动删除低价值 recipe。
8. 不调用真实 AI 自动生成优化结论。
9. analytics 只基于历史执行记录、recipe 使用记录和 outcome 字段。
10. optimization suggestion 只做建议，不自动应用。
11. 不要破坏 1-42B 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V52__init_governance_effectiveness_analytics_tables.sql。
2. 新增 governance_recipe_effectiveness_snapshot / governance_playbook_analytics_record / governance_optimization_suggestion 三张表。
3. 新增 4 个枚举：
   - GovernanceEffectivenessLevelV2
   - GovernanceOptimizationSuggestionType
   - GovernanceOptimizationTargetType
   - GovernanceAnalyticsWindow
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceEffectivenessAnalyticsService。
6. 新增 GovernanceRecipeOptimizationService。
7. 新增 GovernancePlaybookPerformanceService。
8. 新增 API：
   - recipe effectiveness refresh / list / dashboard / trend
   - playbook analytics refresh / list / dashboard
   - optimization suggestions refresh / list / dashboard / report
9. 前端新增：
   - GovernanceRecipeEffectivenessPanel.vue
   - GovernancePlaybookAnalyticsPanel.vue
   - GovernanceOptimizationSuggestionPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-42c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance effectiveness / analytics / optimization 表说明
3. GovernanceEffectivenessAnalyticsService 设计说明
4. GovernanceRecipeOptimizationService 设计说明
5. GovernancePlaybookPerformanceService 设计说明
6. GovernanceRecipeEffectivenessPanel 说明
7. GovernancePlaybookAnalyticsPanel 说明
8. GovernanceOptimizationSuggestionPanel 说明
9. Effectiveness / Optimization 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 43A

现在开始实现，不要只给计划。
```
