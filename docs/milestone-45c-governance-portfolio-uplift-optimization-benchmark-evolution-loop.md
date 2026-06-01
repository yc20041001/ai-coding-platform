# Milestone 45C: Governance Portfolio Uplift Optimization & Benchmark Evolution Loop

## 1. 背景

截至 Milestone 45B，平台已经完成了一整条非常完整的治理主线，并进入了跨项目 adoption 与 uplift 跟踪阶段：

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

44C
  Governance Assistive Planning Optimization / Outcome-Driven Draft Tuning

45A
  Governance Portfolio Benchmarking / Cross-Org Best Practice Alignment

45B
  Governance Benchmark Adoption Tracking / Cross-Team Improvement Loop
```

现在平台已经能做到：

```text
识别 best practice
记录 benchmark adoption
跟踪 cross-team improvement campaign
测量 adoption 前后的 uplift
```

但这里仍然有一个更高阶的组织级问题：

```text
哪些 benchmark 在长期上越来越有效？
哪些 campaign 虽然完成了，但真实 uplift 很弱？
哪些最佳实践需要被升级、合并或淘汰？
不同组织或项目群的 maturity 进展轨迹是否改善？
portfolio 层面的 uplift 是阶段性波动，还是可持续演进？
```

换句话说，平台现在已经有：

```text
benchmark adoption 跟踪
campaign 跟踪
uplift before/after 测量
```

但还缺少：

```text
把这些长期变化真正纳入 benchmark evolution 和 portfolio-level uplift optimization 的闭环
```

Milestone 45C 的目标就是新增：

```text
Governance Portfolio Uplift Optimization & Benchmark Evolution Loop
```

让平台从：

```text
知道哪些实践被采用，以及 adoption 后有没有提升
```

升级为：

```text
知道哪些 benchmark 正在变强、哪些 campaign 值得复制、哪些最佳实践应该演化，以及 portfolio 整体治理能力是否在持续上升
```

---

## 2. 总目标

实现 benchmark evolution 与 uplift optimization 闭环：

1. 新增 Benchmark Evolution Snapshot 数据模型。
2. 新增 Campaign Effectiveness Ranking 数据模型。
3. 新增 Governance Progress Map 数据模型。
4. 支持统计 benchmark drift trend。
5. 支持对 improvement campaign 做 effectiveness ranking。
6. 支持识别 uplift-driven best practice evolution 候选。
7. 支持生成 portfolio-level governance progress map。
8. 支持输出 maturity improvement trend。
9. 支持导出 Markdown Uplift Optimization Report。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
知道 adoption 是否产生了 uplift
```

升级为：

```text
知道哪些 uplift 是可持续的，哪些 benchmark 应该被演进，哪些 campaign 最值得复制
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
7. 不自动把某个 benchmark 直接升级成全局规则。
8. optimization / evolution 只做趋势分析、排名、建议和映射，不自动应用到生产配置。
9. 不调用真实 AI 自动生成 evolution 结论。
10. 不破坏 1-45B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 evolution / ranking / progress map 三张表。
2. 聚合长期 uplift 数据。
3. 输出 benchmark 演化与 campaign 排名建议。
4. 生成跨项目 progress map。
5. 导出 report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V61__init_governance_uplift_optimization_tables.sql
```

### 4.1 governance_benchmark_evolution_snapshot

```sql
CREATE TABLE governance_benchmark_evolution_snapshot (
    id BIGINT PRIMARY KEY,
    benchmark_key VARCHAR(128) NOT NULL,
    benchmark_type VARCHAR(64) NOT NULL,
    benchmark_window VARCHAR(32) NOT NULL,
    baseline_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    current_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    drift_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_support_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    evolution_signal_level VARCHAR(32) NOT NULL,
    recommendation_text TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_benchmark_evolution_snapshot_key(benchmark_key, benchmark_window, captured_at),
    KEY idx_governance_benchmark_evolution_snapshot_type(benchmark_type, evolution_signal_level),
    KEY idx_governance_benchmark_evolution_snapshot_window(benchmark_window, captured_at)
);
```

### 4.2 governance_campaign_effectiveness_ranking

```sql
CREATE TABLE governance_campaign_effectiveness_ranking (
    id BIGINT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    campaign_key VARCHAR(64) NOT NULL,
    campaign_title VARCHAR(255) NOT NULL,
    ranking_window VARCHAR(32) NOT NULL,
    adoption_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_uplift_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    maturity_gain_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    campaign_effectiveness_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL,
    ranking_position INT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_campaign_effectiveness_ranking_window(ranking_window, ranking_position),
    KEY idx_governance_campaign_effectiveness_ranking_level(effectiveness_level, ranking_position)
);
```

### 4.3 governance_progress_map_snapshot

```sql
CREATE TABLE governance_progress_map_snapshot (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    org_unit VARCHAR(128) NULL,
    map_window VARCHAR(32) NOT NULL,
    maturity_level_before VARCHAR(32) NOT NULL,
    maturity_level_after VARCHAR(32) NOT NULL,
    maturity_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_level VARCHAR(32) NOT NULL,
    progress_signal_level VARCHAR(32) NOT NULL,
    summary_text TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_progress_map_snapshot_window(map_window, progress_signal_level),
    KEY idx_governance_progress_map_snapshot_org(org_unit, map_window)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceBenchmarkType.java
GovernanceEvolutionSignalLevel.java
GovernanceCampaignEffectivenessLevel.java
GovernanceProgressSignalLevel.java
GovernanceRankingWindow.java
```

### 5.1 GovernanceBenchmarkType

```text
DRAFT_ADOPTION
ASSISTIVE_USEFULNESS
PACKAGE_QUALITY
WORKFLOW_COMPLETION
MATURITY_PROGRESS
```

### 5.2 GovernanceEvolutionSignalLevel

```text
PROMOTE
STABLE
WATCH
RETIRE
```

### 5.3 GovernanceCampaignEffectivenessLevel

```text
TOP_TIER
STRONG
MIXED
LOW_IMPACT
```

### 5.4 GovernanceProgressSignalLevel

```text
LEADING_UPLIFT
STABLE_GROWTH
FLAT
REGRESSING
```

### 5.5 GovernanceRankingWindow

```text
DAY_30
DAY_60
DAY_90
DAY_180
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceBenchmarkEvolutionSnapshotEntity.java
GovernanceCampaignEffectivenessRankingEntity.java
GovernanceProgressMapSnapshotEntity.java

GovernanceBenchmarkEvolutionSnapshotMapper.java
GovernanceCampaignEffectivenessRankingMapper.java
GovernanceProgressMapSnapshotMapper.java
```

DTO 建议：

```text
GovernanceBenchmarkEvolutionSnapshotResponse.java
GovernanceCampaignEffectivenessRankingResponse.java
GovernanceProgressMapSnapshotResponse.java
GovernanceUpliftOptimizationDashboardResponse.java
GovernanceUpliftOptimizationReportResponse.java
```

### 6.1 GovernanceUpliftOptimizationDashboardResponse

建议字段：

```text
totalBenchmarks
promoteBenchmarkCount
retireBenchmarkCount
topCampaigns
lowImpactCampaigns
leadingProjects
regressingProjects
latestEvolutionSignals
latestProgressMap
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceBenchmarkEvolutionService.java
GovernanceCampaignRankingService.java
GovernanceProgressMapService.java
```

### 7.1 GovernanceBenchmarkEvolutionService

职责：

1. 基于 benchmark adoption 与 uplift 数据生成 evolution snapshot。
2. 比较 baseline score 与 current score。
3. 识别 should promote / stable / watch / retire 的 benchmark。
4. 输出 recommendation_text，说明演化方向。

建议公式：

```text
driftDelta = currentScore - baselineScore

evolutionSupportScore =
  driftDelta * 0.4
  + upliftSupportRate * 0.4
  + adoptionRateSupport * 0.2
```

建议分级：

```text
>= 15 PROMOTE
>= 5 STABLE
>= 0 WATCH
< 0 RETIRE
```

### 7.2 GovernanceCampaignRankingService

职责：

1. 对 cross-team improvement campaign 进行 effectiveness ranking。
2. 综合 adoptionRate、avgUpliftScore、maturityGainAvg 输出排名。
3. 识别 top-tier 与 low-impact campaign。
4. 支持按 ranking window 做时间维度比较。

建议评分公式：

```text
campaignEffectivenessScore =
  adoptionRate * 0.35
  + avgUpliftScore * 0.40
  + maturityGainAvg * 0.25
```

### 7.3 GovernanceProgressMapService

职责：

1. 生成跨项目 / 跨 org_unit 的 progress map。
2. 标记 leading uplift、stable growth、flat、regressing。
3. 识别 maturity 提升最明显与最弱的项目。
4. 输出 summary_text 供 dashboard 和 report 使用。

建议分级：

```text
maturityDelta >= 15 -> LEADING_UPLIFT
maturityDelta >= 5  -> STABLE_GROWTH
maturityDelta >= 0  -> FLAT
maturityDelta < 0   -> REGRESSING
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceUpliftOptimizationController.java
```

建议端点：

### 8.1 Benchmark Evolution

```text
POST   /api/governance-uplift-optimization/evolution/refresh
GET    /api/governance-uplift-optimization/evolution
```

### 8.2 Campaign Effectiveness Ranking

```text
POST   /api/governance-uplift-optimization/campaigns/refresh
GET    /api/governance-uplift-optimization/campaigns
```

### 8.3 Progress Map / Dashboard / Report

```text
POST   /api/governance-uplift-optimization/progress/refresh
GET    /api/governance-uplift-optimization/progress
GET    /api/governance-uplift-optimization/dashboard
GET    /api/governance-uplift-optimization/report
```

权限建议：

```text
查看：ADMIN
刷新 evolution / ranking / progress：ADMIN
```

---

## 9. Uplift Optimization 规则建议

### 9.1 Benchmark Evolution

若某 benchmark：

```text
currentScore 明显高于 baselineScore
upliftSupportRate >= 70
```

则标记：

```text
PROMOTE
```

若 benchmark 长期：

```text
driftDelta < 0
upliftSupportRate 低
```

则标记：

```text
RETIRE
```

### 9.2 Campaign Ranking

若某 campaign：

```text
adoptionRate 高
avgUpliftScore 高
maturityGainAvg 高
```

则进入：

```text
TOP_TIER
```

### 9.3 Progress Map

progress map 至少反映：

```text
项目维度 maturity 变化
uplift level
org_unit 聚合趋势
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceBenchmarkEvolutionPanel.vue
GovernanceCampaignRankingPanel.vue
GovernanceProgressMapPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceBenchmarkEvolutionPanel

展示：

1. benchmark evolution 列表
2. baseline vs current
3. drift delta
4. promote / stable / watch / retire 标签

### 10.2 GovernanceCampaignRankingPanel

展示：

1. top campaign 排名
2. campaignEffectivenessScore
3. top-tier / low-impact 标签
4. ranking window 切换

### 10.3 GovernanceProgressMapPanel

展示：

1. progress map 列表
2. maturity before/after
3. uplift level / progress signal
4. leading / regressing 项目标识

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 强调“哪些基准应推广、哪些应退役”
4. progress map 要突出 portfolio 趋势感

---

## 11. 后端测试要求

新增：

```text
GovernanceUpliftOptimizationIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. refresh benchmark evolution success
2. empty data returns no evolution safely
3. promote evolution signal generated
4. stable evolution signal generated
5. watch evolution signal generated
6. retire evolution signal generated
7. drift delta computed
8. recommendation text populated
9. refresh campaign ranking success
10. campaign effectiveness score computed
11. ranking position computed
12. top tier campaign generated
13. strong campaign generated
14. mixed campaign generated
15. low impact campaign generated
16. refresh progress map success
17. leading uplift project generated
18. stable growth project generated
19. flat project generated
20. regressing project generated
21. dashboard returns promoteBenchmarkCount
22. dashboard returns retireBenchmarkCount
23. dashboard returns topCampaigns
24. dashboard returns lowImpactCampaigns
25. dashboard returns leadingProjects
26. dashboard returns regressingProjects
27. report export markdown success
28. unauthorized access reject
29. non-admin refresh reject
30. high uplift benchmarks support promote
31. low uplift benchmarks support retire
32. strong campaigns sort above mixed campaigns
33. negative maturity delta marks regressing
34. refresh idempotent for same ranking window
35. latest progress map returned
36. latest evolution signals returned

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-uplift-optimization.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 benchmark evolution 面板
2. campaign ranking 面板可见
3. progress map 面板可见
4. promote / retire 标签可见
5. ranking score 可见
6. uplift / progress 标签可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 uplift optimization 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-45c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 benchmark evolution / campaign effectiveness ranking / progress map 表说明
3. GovernanceBenchmarkEvolutionService 设计说明
4. GovernanceCampaignRankingService 设计说明
5. GovernanceProgressMapService 设计说明
6. GovernanceBenchmarkEvolutionPanel 说明
7. GovernanceCampaignRankingPanel 说明
8. GovernanceProgressMapPanel 说明
9. Portfolio Uplift Optimization / Benchmark Evolution 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 46A

---

## 14. 验收标准

必须全部满足：

1. governance_benchmark_evolution_snapshot / governance_campaign_effectiveness_ranking / governance_progress_map_snapshot 三张表已落库
2. evolution 可刷新 / 查询
3. ranking 可刷新 / 查询
4. progress map 可刷新 / 查询
5. dashboard / report 可导出
6. evolution / ranking / progress 逻辑可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 45C 后，平台将从：

```text
能跟踪 benchmark adoption 和 uplift
```

升级为：

```text
能持续进化 benchmark、识别最有效的 campaign，并观察 portfolio 级治理能力是否真正长期提升
```

这一步会让治理平台从“跨团队改进跟踪系统”进一步升级成“组织级 benchmark 演化与 uplift 优化系统”。

---

## 16. 后续建议

Milestone 45C 完成后，建议进入：

```text
Milestone 46A: Governance Strategy Office Dashboard & Executive Portfolio Review
```

重点可包括：

1. executive-ready portfolio summary
2. strategy office review packs
3. quarterly governance health deck
4. risk concentration map
5. org-wide maturity direction view

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 45C。

文档路径：
docs/milestone-45c-governance-portfolio-uplift-optimization-benchmark-evolution-loop.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 45B benchmark adoption tracking / cross-team improvement loop 基础上，新增 portfolio uplift optimization 与 benchmark evolution loop。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动把 benchmark 直接升级为组织级强制规则。
10. evolution / ranking / progress 只做分析、排名、建议和趋势映射，不做自动应用。
11. 不调用真实 AI 自动生成 evolution 结论。
12. 不要破坏 1-45B 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 45A benchmark / maturity / alignment 和 45B adoption / campaign / uplift 数据结构，不要重复造概念。
17. optimization 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V61__init_governance_uplift_optimization_tables.sql。
2. 新增 governance_benchmark_evolution_snapshot / governance_campaign_effectiveness_ranking / governance_progress_map_snapshot 三张表。
3. 新增 5 个枚举：
   - GovernanceBenchmarkType
   - GovernanceEvolutionSignalLevel
   - GovernanceCampaignEffectivenessLevel
   - GovernanceProgressSignalLevel
   - GovernanceRankingWindow
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceBenchmarkEvolutionService。
6. 新增 GovernanceCampaignRankingService。
7. 新增 GovernanceProgressMapService。
8. 新增 API：
   - evolution refresh / list
   - campaign ranking refresh / list
   - progress map refresh / list
   - dashboard / report
9. 前端新增：
   - GovernanceBenchmarkEvolutionPanel.vue
   - GovernanceCampaignRankingPanel.vue
   - GovernanceProgressMapPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-45c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 benchmark evolution / campaign effectiveness ranking / progress map 表说明
3. GovernanceBenchmarkEvolutionService 设计说明
4. GovernanceCampaignRankingService 设计说明
5. GovernanceProgressMapService 设计说明
6. GovernanceBenchmarkEvolutionPanel 说明
7. GovernanceCampaignRankingPanel 说明
8. GovernanceProgressMapPanel 说明
9. Portfolio Uplift Optimization / Benchmark Evolution 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 46A

现在开始实现，不要只给计划。
```
