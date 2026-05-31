# Milestone 44C: Governance Assistive Planning Optimization & Outcome-Driven Draft Tuning

## 1. 背景

截至 Milestone 44B，平台已经完成了一整条治理 Copilot 主线：

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

44A
  Governance Autonomous Draft Planning / Safe Assistive Actions

44B
  Governance Outcome Review / Draft Adoption Tracking / Assistive Quality Evaluation
```

现在平台已经能做到：

```text
生成 draft remediation plan
生成 safe assistive action checklist
组装 recommendation package
记录草稿是否被采用
记录辅助动作是否有用
记录 package 质量评分
```

但还有最后一个很关键的问题：

```text
系统已经知道哪些 draft 被采用、哪些被拒绝，
那它是否能基于这些结果去优化下一版 draft 的结构？

哪些模板段落经常被保留？
哪些 assistive action 排序更有效？
哪些 package 组合方式更容易得到高质量评价？
哪些 draft 类型需要更保守或更详细的起草策略？
```

也就是说，平台现在已经有：

```text
起草能力
结果评估能力
```

但还缺少：

```text
基于 outcome 反向优化 draft planning 本身的 tuning 闭环
```

Milestone 44C 的目标就是新增：

```text
Governance Assistive Planning Optimization & Outcome-Driven Draft Tuning
```

让平台从：

```text
知道草稿好不好
```

升级为：

```text
根据 adoption / rejection / usefulness / package quality 结果，
持续优化 draft template、assistive action 排序和 package 组装策略
```

---

## 2. 总目标

实现 outcome-driven 起草优化闭环：

1. 新增 Draft Optimization Signal 数据模型。
2. 新增 Assistive Ordering Optimization 数据模型。
3. 新增 Package Composition Tuning 数据模型。
4. 基于 44B outcome review 结果生成 optimization signals。
5. 支持识别高 adoption draft pattern。
6. 支持识别低价值 assistive action 顺序与组合。
7. 支持识别高质量 package 的组成特征。
8. 支持生成 draft tuning dashboard。
9. 支持导出 Markdown Optimization Report。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
系统知道草稿结果如何
```

升级为：

```text
系统能用这些结果去优化下一轮草稿、辅助动作和 package 组装方式
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
7. 不自动提交 recommendation package。
8. 不自动替换真实 draft plan / assistive action / package 内容。
9. optimization 只做建议、排序权重和模板倾向输出，不自动应用到生产记录。
10. 不调用真实 AI 自动生成优化结论。
11. 不破坏 1-44B 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 optimization signal / ordering / composition 三张表。
2. 基于 outcome review 生成规则化 tuning 建议。
3. 给 future draft planning 提供参考权重。
4. 导出 Markdown 优化报告。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V58__init_governance_draft_optimization_tables.sql
```

### 4.1 governance_draft_optimization_signal

```sql
CREATE TABLE governance_draft_optimization_signal (
    id BIGINT PRIMARY KEY,
    signal_type VARCHAR(64) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    scope_key VARCHAR(128) NULL,
    adoption_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    rejection_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_usefulness_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_package_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    tuning_weight_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    recommendation_text TEXT NULL,
    signal_level VARCHAR(32) NOT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_draft_optimization_signal_type(signal_type, captured_at),
    KEY idx_governance_draft_optimization_signal_scope(scope_type, scope_key)
);
```

### 4.2 governance_assistive_ordering_optimization

```sql
CREATE TABLE governance_assistive_ordering_optimization (
    id BIGINT PRIMARY KEY,
    action_sequence_key VARCHAR(255) NOT NULL,
    category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL,
    ordering_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    completion_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    usefulness_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    suggestion_text TEXT NULL,
    optimization_level VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_assistive_ordering_optimization_seq(action_sequence_key),
    KEY idx_governance_assistive_ordering_optimization_category(category, optimization_level)
);
```

### 4.3 governance_package_composition_tuning

```sql
CREATE TABLE governance_package_composition_tuning (
    id BIGINT PRIMARY KEY,
    composition_key VARCHAR(255) NOT NULL,
    package_type VARCHAR(64) NULL,
    completeness_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    accuracy_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    overall_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    adoption_support_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    tuning_summary_text TEXT NULL,
    tuning_level VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_package_composition_tuning_key(composition_key),
    KEY idx_governance_package_composition_tuning_type(package_type, tuning_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceDraftOptimizationSignalType.java
GovernanceDraftOptimizationScopeType.java
GovernanceDraftOptimizationSignalLevel.java
GovernanceAssistiveOrderingOptimizationLevel.java
GovernancePackageCompositionTuningLevel.java
```

### 5.1 GovernanceDraftOptimizationSignalType

```text
TEMPLATE_STRUCTURE
RISK_LEVEL_STRATEGY
STEP_GRANULARITY
BUNDLE_ATTACHMENT
PACKAGE_COMPLETENESS
```

### 5.2 GovernanceDraftOptimizationScopeType

```text
GLOBAL
CATEGORY
GUARDRAIL
PRIORITY
PACKAGE_TYPE
```

### 5.3 GovernanceDraftOptimizationSignalLevel

```text
PROMOTE
KEEP
WATCH
REWORK
```

### 5.4 GovernanceAssistiveOrderingOptimizationLevel

```text
BEST
GOOD
MIXED
POOR
```

### 5.5 GovernancePackageCompositionTuningLevel

```text
HIGH_VALUE
STABLE
WEAK
REWORK_REQUIRED
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceDraftOptimizationSignalEntity.java
GovernanceAssistiveOrderingOptimizationEntity.java
GovernancePackageCompositionTuningEntity.java

GovernanceDraftOptimizationSignalMapper.java
GovernanceAssistiveOrderingOptimizationMapper.java
GovernancePackageCompositionTuningMapper.java
```

DTO 建议：

```text
GovernanceDraftOptimizationSignalResponse.java
GovernanceAssistiveOrderingOptimizationResponse.java
GovernancePackageCompositionTuningResponse.java
GovernanceDraftOptimizationDashboardResponse.java
GovernanceDraftOptimizationReportResponse.java
```

### 6.1 GovernanceDraftOptimizationDashboardResponse

建议字段：

```text
totalSignals
promoteSignalCount
reworkSignalCount
topTemplateSignals
topOrderingPatterns
topPackageCompositions
latestSignals
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceDraftOptimizationService.java
GovernanceAssistiveOrderingService.java
GovernancePackageCompositionService.java
```

### 7.1 GovernanceDraftOptimizationService

职责：

1. 基于 44B 的 adoption / usefulness / package evaluation 生成 optimization signals。
2. 识别高 adoption、高 usefulness 的 template pattern。
3. 识别高 rejection、高 modification 的 draft 结构风险。
4. 生成 recommendation_text，说明该如何优化起草逻辑。

建议公式：

```text
tuningWeightScore =
  adoptionRate * 0.4
  + avgUsefulnessRating * 12
  + avgPackageScore * 0.25
  - rejectionRate * 0.25
```

建议分级：

```text
>= 80 PROMOTE
>= 55 KEEP
>= 30 WATCH
< 30 REWORK
```

### 7.2 GovernanceAssistiveOrderingService

职责：

1. 分析 assistive action 顺序与最终 usefulness / completion 的关系。
2. 输出高价值 action sequence 排序模式。
3. 标记低价值、低完成率的辅助顺序。
4. 为 future plan 提供排序参考。

排序评分建议：

```text
orderingScore =
  completionRate * 0.45
  + usefulnessRate * 0.45
  + sequenceReuseFactor * 0.10
```

### 7.3 GovernancePackageCompositionService

职责：

1. 分析 package 的组成方式与最终质量评分关系。
2. 识别高 completeness、高 accuracy 的组合方式。
3. 标记缺上下文或结构弱的 composition pattern。
4. 输出 package composition tuning 建议。

---

## 8. API 设计

新增 Controller：

```text
GovernanceDraftOptimizationController.java
```

建议端点：

### 8.1 Draft Optimization Signals

```text
POST   /api/governance-draft-optimization/signals/refresh
GET    /api/governance-draft-optimization/signals
```

### 8.2 Assistive Ordering Optimization

```text
POST   /api/governance-draft-optimization/ordering/refresh
GET    /api/governance-draft-optimization/ordering
```

### 8.3 Package Composition Tuning / Dashboard / Report

```text
POST   /api/governance-draft-optimization/packages/refresh
GET    /api/governance-draft-optimization/packages
GET    /api/governance-draft-optimization/dashboard
GET    /api/governance-draft-optimization/report
```

权限建议：

```text
查看：ADMIN
刷新优化分析：ADMIN
```

---

## 9. 优化规则建议

### 9.1 Draft Template Optimization

若某类 draft：

```text
adoptionRate >= 70
avgUsefulnessRating >= 4
avgPackageScore >= 80
```

则生成：

```text
PROMOTE
```

若某类 draft：

```text
rejectionRate >= 40
major/rewrite modification rate 高
```

则生成：

```text
REWORK
```

### 9.2 Assistive Action Ordering

若某 action sequence：

```text
completionRate >= 70
usefulnessRate >= 70
```

则标记：

```text
BEST
```

若低于阈值则降级到 `GOOD / MIXED / POOR`。

### 9.3 Package Composition

若某 composition：

```text
completenessAvg >= 85
accuracyAvg >= 85
overallAvg >= 85
```

则标记：

```text
HIGH_VALUE
```

若整体低于 40，则标记：

```text
REWORK_REQUIRED
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceDraftOptimizationPanel.vue
GovernanceAssistiveOrderingPanel.vue
GovernancePackageCompositionPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceDraftOptimizationPanel

展示：

1. optimization signal 列表
2. signal type / scope / level / tuning weight
3. recommendation_text
4. promote / rework 标签

### 10.2 GovernanceAssistiveOrderingPanel

展示：

1. assistive ordering pattern 列表
2. ordering score / completion rate / usefulness rate
3. best / mixed / poor 标签
4. sequence 文本展开

### 10.3 GovernancePackageCompositionPanel

展示：

1. composition pattern 列表
2. completeness / accuracy / overall
3. high value / weak / rework required 标签
4. tuning summary 文本

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 优化建议必须可解释
4. 突出“哪些模式值得保留、哪些需要重做”

---

## 11. 后端测试要求

新增：

```text
GovernanceDraftOptimizationIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. refresh draft optimization signals success
2. empty data returns no signal safely
3. promote signal generated for high adoption pattern
4. rework signal generated for high rejection pattern
5. tuning weight score computed
6. template structure signal stored
7. risk strategy signal stored
8. step granularity signal stored
9. bundle attachment signal stored
10. package completeness signal stored
11. refresh assistive ordering success
12. best ordering pattern generated
13. poor ordering pattern generated
14. ordering score computed
15. sequence reuse factor contributes
16. refresh package composition success
17. high value composition generated
18. weak composition generated
19. rework required composition generated
20. package averages computed
21. dashboard returns totalSignals
22. dashboard returns promoteSignalCount
23. dashboard returns reworkSignalCount
24. dashboard returns top ordering patterns
25. dashboard returns top package compositions
26. report export markdown success
27. unauthorized access reject
28. non-admin refresh reject
29. repeated adopted draft increases promote tendency
30. repeated rejected draft increases rework tendency
31. high usefulness assistive action boosts ordering level
32. low usefulness assistive action lowers ordering level
33. high package scores support high value composition
34. low package scores support rework composition
35. refresh is idempotent for same source snapshot
36. recommendation text populated for every signal

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-draft-optimization.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 draft optimization 面板
2. assistive ordering 面板可见
3. package composition 面板可见
4. promote / rework 标签可见
5. ordering score 指标可见
6. package tuning summary 可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 optimization 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-44c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 draft optimization signal / assistive ordering optimization / package composition tuning 表说明
3. GovernanceDraftOptimizationService 设计说明
4. GovernanceAssistiveOrderingService 设计说明
5. GovernancePackageCompositionService 设计说明
6. GovernanceDraftOptimizationPanel 说明
7. GovernanceAssistiveOrderingPanel 说明
8. GovernancePackageCompositionPanel 说明
9. Outcome-driven Draft Tuning 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45A

---

## 14. 验收标准

必须全部满足：

1. governance_draft_optimization_signal / governance_assistive_ordering_optimization / governance_package_composition_tuning 三张表已落库
2. optimization signal 可刷新 / 查询
3. ordering optimization 可刷新 / 查询
4. package composition tuning 可刷新 / 查询
5. dashboard / report 可导出
6. adoption-driven tuning 逻辑可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 44C 后，平台将从：

```text
知道草稿是否被采用、辅助动作是否有用
```

升级为：

```text
能够基于这些结果持续优化下一轮 draft planning、assistive action 排序与 package 组装策略
```

这一步会让治理 Copilot 从“会起草、会评估”进一步升级成“会根据结果优化起草方式”的辅助系统。

---

## 16. 后续建议

Milestone 44C 完成后，建议进入：

```text
Milestone 45A: Governance Portfolio Benchmarking & Cross-Org Best Practice Alignment
```

重点可包括：

1. 跨项目 draft adoption benchmark
2. assistive action best-practice library
3. package quality baseline 对齐
4. org-level planning quality ranking
5. cross-org governance maturity view

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 44C。

文档路径：
docs/milestone-44c-governance-assistive-planning-optimization-outcome-driven-draft-tuning.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 44B outcome review / adoption tracking / assistive quality evaluation 基础上，新增 outcome-driven draft tuning 与 assistive planning optimization。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动提交 recommendation package。
10. optimization 只做建议、排序权重和模板倾向，不自动应用到真实记录。
11. 不调用真实 AI 自动生成优化结论。
12. 不要破坏 1-44B 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 44A draft plan / assistive action / package 和 44B outcome review 数据结构，不要重复造概念。
17. optimization 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V58__init_governance_draft_optimization_tables.sql。
2. 新增 governance_draft_optimization_signal / governance_assistive_ordering_optimization / governance_package_composition_tuning 三张表。
3. 新增 5 个枚举：
   - GovernanceDraftOptimizationSignalType
   - GovernanceDraftOptimizationScopeType
   - GovernanceDraftOptimizationSignalLevel
   - GovernanceAssistiveOrderingOptimizationLevel
   - GovernancePackageCompositionTuningLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceDraftOptimizationService。
6. 新增 GovernanceAssistiveOrderingService。
7. 新增 GovernancePackageCompositionService。
8. 新增 API：
   - optimization signals refresh / list
   - assistive ordering refresh / list
   - package composition refresh / list
   - dashboard / report
9. 前端新增：
   - GovernanceDraftOptimizationPanel.vue
   - GovernanceAssistiveOrderingPanel.vue
   - GovernancePackageCompositionPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-44c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 draft optimization signal / assistive ordering optimization / package composition tuning 表说明
3. GovernanceDraftOptimizationService 设计说明
4. GovernanceAssistiveOrderingService 设计说明
5. GovernancePackageCompositionService 设计说明
6. GovernanceDraftOptimizationPanel 说明
7. GovernanceAssistiveOrderingPanel 说明
8. GovernancePackageCompositionPanel 说明
9. Outcome-driven Draft Tuning 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45A

现在开始实现，不要只给计划。
```
