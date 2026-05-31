# Milestone 42B: Governance Knowledge Base, Pattern Library & Reusable Remediation Recipes

## 1. 背景

截至 Milestone 42A，平台已经具备治理执行辅助能力：

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
  Recommendation Playbooks / Execution Plans / Handoff Checklists
```

现在系统已经能回答：

```text
当前 recommendation 应该怎么执行？
应该用哪个 playbook？
handoff 应该怎么做？
```

但随着治理事项越来越多，团队很快会遇到下一个问题：

```text
以前类似问题是怎么处理的？
哪些 remediation recipe 被证明有效？
这个 recommendation 有没有类似的处理模式可复用？
哪个 playbook 的效果最好？
```

换句话说，42A 让平台具备了：

```text
结构化执行能力
```

但还没有：

```text
经验沉淀、模式复用和 recipe 效果反馈
```

Milestone 42B 的目标就是新增：

```text
Governance Knowledge Base, Pattern Library & Reusable Remediation Recipes
```

让平台从：

```text
每次按模板执行
```

升级为：

```text
能积累经验、搜索相似治理案例、复用 remediation recipe，并衡量 playbook 效果
```

---

## 2. 总目标

实现治理知识库与 recipe 复用能力：

1. 新增 Governance Knowledge Entry 数据模型。
2. 新增 Governance Pattern Library Item 数据模型。
3. 新增 Governance Remediation Recipe 数据模型。
4. 支持从 execution plan / handoff / recommendation 结果沉淀知识条目。
5. 支持 pattern-based playbook reuse。
6. 支持相似 recommendation / 相似 remediation 搜索。
7. 支持 recipe 与 playbook 的 effectiveness scoring。
8. 支持 recipe 推荐与效果排序。
9. 支持导出 Governance Knowledge Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有模板，但经验还是散落在历史记录里
```

升级为：

```text
有可搜索、可复用、可评分的治理知识库与 remediation recipe 库
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改已有 recommendation / execution plan / waiver / escalation 原始记录。
4. 不自动关闭 recommendation。
5. 不自动批准 waiver。
6. 不调用真实 AI 自动生成知识内容。
7. 不接入外部向量数据库或外部搜索服务。
8. 相似推荐只使用规则法 / 简单文本或结构化字段匹配。
9. recipe recommendation 只做建议，不自动应用。
10. 不破坏 1-42A 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 knowledge entry / pattern item / remediation recipe 表。
2. 基于现有 recommendation / playbook / execution 结果生成知识沉淀记录。
3. 新增搜索、相似项推荐、effectiveness scoring。
4. 新增 knowledge / recipe / pattern 面板与导出。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V51__init_governance_knowledge_recipe_tables.sql
```

### 4.1 governance_knowledge_entry

```sql
CREATE TABLE governance_knowledge_entry (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    tags_json JSON NULL,
    summary_text TEXT NULL,
    detail_markdown MEDIUMTEXT NULL,
    effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    reuse_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_knowledge_entry_category(category),
    KEY idx_governance_knowledge_entry_project(project_id, create_time),
    KEY idx_governance_knowledge_entry_score(effectiveness_score, reuse_count)
);
```

### 4.2 governance_pattern_library_item

```sql
CREATE TABLE governance_pattern_library_item (
    id BIGINT PRIMARY KEY,
    pattern_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL,
    priority VARCHAR(32) NULL,
    pattern_json JSON NOT NULL,
    notes TEXT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_pattern_library(pattern_key),
    KEY idx_governance_pattern_library_match(recommendation_category, guardrail_key, priority, enabled)
);
```

### 4.3 governance_remediation_recipe

```sql
CREATE TABLE governance_remediation_recipe (
    id BIGINT PRIMARY KEY,
    recipe_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    recipe_type VARCHAR(64) NOT NULL,
    recommendation_category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL,
    steps_json JSON NOT NULL,
    prerequisites_json JSON NULL,
    success_criteria_json JSON NULL,
    effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_remediation_recipe(recipe_key),
    KEY idx_governance_remediation_recipe_match(recipe_type, recommendation_category, guardrail_key, enabled)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceKnowledgeSourceType.java
GovernanceRecipeType.java
GovernanceSimilarityMatchMode.java
GovernanceEffectivenessLevel.java
```

### 5.1 GovernanceKnowledgeSourceType

```text
RECOMMENDATION
EXECUTION_PLAN
HANDOFF
WAIVER
PLAYBOOK
```

### 5.2 GovernanceRecipeType

```text
REMEDIATION
WAIVER_MITIGATION
HANDOFF_SUPPORT
ESCALATION_RESPONSE
```

### 5.3 GovernanceSimilarityMatchMode

```text
EXACT
CATEGORY_GUARDRAIL
CATEGORY_PRIORITY
TAG_OVERLAP
DEFAULT
```

### 5.4 GovernanceEffectivenessLevel

```text
LOW
MEDIUM
HIGH
TOP
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceKnowledgeEntryEntity.java
GovernancePatternLibraryItemEntity.java
GovernanceRemediationRecipeEntity.java

GovernanceKnowledgeEntryMapper.java
GovernancePatternLibraryItemMapper.java
GovernanceRemediationRecipeMapper.java
```

DTO 建议：

```text
CreateGovernanceKnowledgeEntryRequest.java
UpdateGovernanceKnowledgeEntryRequest.java
GovernanceKnowledgeEntryResponse.java

CreateGovernancePatternLibraryItemRequest.java
UpdateGovernancePatternLibraryItemRequest.java
GovernancePatternLibraryItemResponse.java

CreateGovernanceRemediationRecipeRequest.java
UpdateGovernanceRemediationRecipeRequest.java
GovernanceRemediationRecipeResponse.java

GovernanceKnowledgeSearchResponse.java
GovernanceSimilaritySuggestionResponse.java
GovernanceRecipeRecommendationResponse.java
GovernanceKnowledgeDashboardResponse.java
```

### 6.1 GovernanceKnowledgeDashboardResponse

建议字段：

```text
entryCount
patternCount
recipeCount
topKnowledgeEntries
topRecipes
topPatterns
averageEffectivenessScore
highReuseCount
```

### 6.2 GovernanceSimilaritySuggestionResponse

建议字段：

```text
sourceRecommendationId
matchMode
matchedEntryId
matchedTitle
matchedCategory
matchedGuardrailKey
similarityScore
summaryText
```

### 6.3 GovernanceRecipeRecommendationResponse

建议字段：

```text
recommendationId
recipeId
recipeKey
displayName
recipeType
effectivenessScore
usageCount
matchMode
summaryText
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceKnowledgeBaseService.java
GovernancePatternLibraryService.java
GovernanceRemediationRecipeService.java
```

### 7.1 GovernanceKnowledgeBaseService

职责：

1. 管理 knowledge entry CRUD。
2. 支持从 recommendation / execution / handoff 手动或规则化生成知识条目。
3. 支持按 category / tag / keyword 查询。
4. 维护 reuseCount 和 effectivenessScore。

### 7.2 GovernancePatternLibraryService

职责：

1. 管理 pattern library item CRUD。
2. 支持按 recommendation category / guardrail / priority 匹配 pattern。
3. 支持 pattern-based playbook reuse 建议。

### 7.3 GovernanceRemediationRecipeService

职责：

1. 管理 recipe CRUD。
2. 按 recommendation category / guardrail / recipe type 匹配 recipe。
3. 生成相似 recipe 与 similar remediation suggestion。
4. 维护 `usageCount` 与 `effectivenessScore`。

简单 effectiveness 评分建议：

```text
effectivenessScore =
  min(100,
      baseScore
      + reusedSuccessfully * 10
      - failedReuseCount * 8
      + completionRateFactor)
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceKnowledgeController.java
```

建议端点：

### 8.1 Knowledge Entry

```text
POST   /api/governance-knowledge/entries
GET    /api/governance-knowledge/entries
GET    /api/governance-knowledge/entries/{entryId}
PUT    /api/governance-knowledge/entries/{entryId}
GET    /api/governance-knowledge/search
```

### 8.2 Pattern Library

```text
POST   /api/governance-knowledge/patterns
GET    /api/governance-knowledge/patterns
GET    /api/governance-knowledge/patterns/{patternId}
PUT    /api/governance-knowledge/patterns/{patternId}
POST   /api/governance-knowledge/patterns/{patternId}/status
```

### 8.3 Recipe

```text
POST   /api/governance-knowledge/recipes
GET    /api/governance-knowledge/recipes
GET    /api/governance-knowledge/recipes/{recipeId}
PUT    /api/governance-knowledge/recipes/{recipeId}
POST   /api/governance-knowledge/recipes/{recipeId}/status
GET    /api/governance-knowledge/recipe-recommendations/{recommendationId}
GET    /api/governance-knowledge/similar-suggestions/{recommendationId}
```

### 8.4 Dashboard / Report

```text
GET    /api/governance-knowledge/dashboard
GET    /api/governance-knowledge/report
```

权限建议：

```text
查看：ADMIN
编辑 knowledge / pattern / recipe：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 相似推荐匹配顺序

建议顺序：

```text
1. recommendation category + guardrail 精确匹配
2. category + priority
3. tag overlap
4. 默认 recipe / pattern
```

### 9.2 Recipe 推荐排序

可按：

```text
effectivenessScore DESC
usageCount DESC
matchMode 优先级 DESC
```

### 9.3 知识沉淀建议

可优先为以下情况生成 knowledge entry：

```text
已完成 recommendation
高优先级 recommendation 的执行总结
waiver mitigation 已关闭的案例
多次复用的 execution plan
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceKnowledgeBasePanel.vue
GovernancePatternLibraryPanel.vue
GovernanceRemediationRecipePanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceKnowledgeBasePanel

展示：

1. knowledge entry 列表
2. search / filter
3. effectiveness / reuse 标签
4. entry 详情抽屉

### 10.2 GovernancePatternLibraryPanel

展示：

1. pattern 列表
2. create / edit dialog
3. 匹配字段摘要
4. enabled 状态

### 10.3 GovernanceRemediationRecipePanel

展示：

1. recipe 列表
2. effectiveness / usage 排序
3. recipe 详情步骤
4. recommendation 匹配预览

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. knowledge / recipe 面板要强调搜索与复用
4. recipe 列表要便于比较效果分数与使用次数

---

## 11. 后端测试要求

新增：

```text
GovernanceKnowledgeRecipeIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. create knowledge entry success
2. update knowledge entry success
3. search by keyword success
4. search by category success
5. create pattern item success
6. update pattern item success
7. disable pattern item success
8. duplicate patternKey reject
9. create recipe success
10. update recipe success
11. disable recipe success
12. duplicate recipeKey reject
13. recipe recommendation exact match
14. recipe recommendation category+guardrail match
15. recipe recommendation category+priority match
16. recipe recommendation default match
17. similar suggestion exact
18. similar suggestion tag overlap
19. dashboard counts correct
20. top knowledge entries ordered
21. top recipes ordered by effectiveness
22. report export markdown success
23. unauthorized access reject
24. non-admin update reject
25. empty dataset returns empty dashboard
26. reuseCount increments correctly
27. effectivenessScore persists
28. disabled recipe excluded from recommendation
29. disabled pattern excluded from match
30. recommendation preview returns recipe
31. knowledge detail markdown persists
32. tags json persists
33. source type filtering works
34. recipe usage count sorting works
35. pattern list filtering works
36. suggestion summary populated

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-knowledge.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 knowledge base panel
2. pattern library panel renders
3. remediation recipe panel renders
4. search/filter 区域可见
5. recipe score / usage 标签可见
6. recommendation preview 区域可见
7. detail drawer / dialog 可见
8. no JS errors on page load

如果测试环境没有 seeded governance execution 数据：

1. 显式断言空态
2. 不把“无 knowledge 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-42b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance knowledge / pattern / recipe 表说明
3. GovernanceKnowledgeBaseService 设计说明
4. GovernancePatternLibraryService 设计说明
5. GovernanceRemediationRecipeService 设计说明
6. GovernanceKnowledgeBasePanel 说明
7. GovernancePatternLibraryPanel 说明
8. GovernanceRemediationRecipePanel 说明
9. Knowledge / Recipe 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42C

---

## 14. 验收标准

必须全部满足：

1. governance_knowledge_entry / governance_pattern_library_item / governance_remediation_recipe 三张表已落库
2. knowledge entry 可创建 / 搜索 / 查看
3. pattern item 可创建 / 编辑 / 启停
4. recipe 可创建 / 编辑 / 启停 / 推荐
5. dashboard / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. recipe / pattern / knowledge 的推荐与复用逻辑清晰

---

## 15. 完成后的价值

完成 42B 后，平台将从：

```text
有 playbook 和 execution plan
```

升级为：

```text
有可搜索、可复用、可评分的治理知识库、模式库和 remediation recipe 库
```

这一步会让治理执行能力从“单次辅助”进化为“经验沉淀与持续复用系统”。

---

## 16. 后续建议

Milestone 42B 完成后，建议进入：

```text
Milestone 42C: Governance Effectiveness Analytics & Recipe Optimization Loop
```

重点可包括：

1. recipe effectiveness trend
2. playbook completion efficiency
3. recipe vs outcome correlation
4. optimization suggestions
5. low-value recipe pruning

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 42B。

文档路径：
docs/milestone-42b-governance-knowledge-base-pattern-library-remediation-recipes.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 42A execution automation / recommendation playbooks 基础上，新增 governance knowledge base、pattern library 与 reusable remediation recipes。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / execution plan / waiver / escalation 原始记录。
6. 不要自动关闭 recommendation。
7. 不要自动批准 waiver。
8. 不调用真实 AI 自动生成知识内容。
9. 相似匹配只使用规则法 / 简单字段匹配。
10. recipe recommendation 只做建议，不自动应用。
11. 不要破坏 1-42A 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V51__init_governance_knowledge_recipe_tables.sql。
2. 新增 governance_knowledge_entry / governance_pattern_library_item / governance_remediation_recipe 三张表。
3. 新增 4 个枚举：
   - GovernanceKnowledgeSourceType
   - GovernanceRecipeType
   - GovernanceSimilarityMatchMode
   - GovernanceEffectivenessLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceKnowledgeBaseService。
6. 新增 GovernancePatternLibraryService。
7. 新增 GovernanceRemediationRecipeService。
8. 新增 API：
   - knowledge entry CRUD / search
   - pattern library CRUD / status
   - recipe CRUD / status / recommendation / similar-suggestions
   - dashboard / report
9. 前端新增：
   - GovernanceKnowledgeBasePanel.vue
   - GovernancePatternLibraryPanel.vue
   - GovernanceRemediationRecipePanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-42b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance knowledge / pattern / recipe 表说明
3. GovernanceKnowledgeBaseService 设计说明
4. GovernancePatternLibraryService 设计说明
5. GovernanceRemediationRecipeService 设计说明
6. GovernanceKnowledgeBasePanel 说明
7. GovernancePatternLibraryPanel 说明
8. GovernanceRemediationRecipePanel 说明
9. Knowledge / Recipe 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42C

现在开始实现，不要只给计划。
```
