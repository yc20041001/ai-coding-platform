# Milestone 45A: Governance Portfolio Benchmarking & Cross-Org Best Practice Alignment

## 1. 背景

截至 Milestone 44C，平台已经完成了一条非常完整的治理 Copilot 主线：

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
```

现在平台已经能做到：

```text
对单个项目、单类 recommendation、单条 draft / package / assistive action 做完整闭环
从 adoption / rejection / usefulness / package quality 中提取优化信号
持续改进 copilot guidance、draft planning 和 assistive ordering
```

但到这里，仍然有一个重要的组织级问题没有被显式回答：

```text
不同项目之间，谁的治理成熟度更高？
哪些项目在 draft adoption 上明显领先？
哪些组织团队的 assistive action 组合更有效？
哪些 package composition 已经可以视为“最佳实践基线”？
不同项目之间是否存在明显的治理能力差距？
```

换句话说，平台现在已经具备：

```text
单项目、单流程内部的治理闭环与优化能力
```

但还缺少：

```text
跨项目、跨团队、跨组织维度的 benchmark 与 best practice 对齐能力
```

Milestone 45A 的目标就是新增：

```text
Governance Portfolio Benchmarking & Cross-Org Best Practice Alignment
```

让平台从：

```text
知道每个项目各自表现如何
```

升级为：

```text
知道不同项目之间谁更强、差距在哪里、哪些实践可以被跨团队复用和对齐
```

---

## 2. 总目标

实现 portfolio benchmarking 与 best practice 对齐层：

1. 新增 Governance Portfolio Benchmark Snapshot 数据模型。
2. 新增 Governance Best Practice Alignment Item 数据模型。
3. 新增 Governance Maturity Scorecard 数据模型。
4. 支持跨项目聚合 adoption / rejection / package quality / assistive usefulness / workflow completion 指标。
5. 支持生成 org-level ranking。
6. 支持识别 best-practice candidate（高 adoption、高 package quality、高 assistive usefulness）。
7. 支持输出 maturity level 与 improvement gap。
8. 支持展示 cross-org benchmark dashboard。
9. 支持导出 Markdown Benchmark Report。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
平台能优化单项目治理效果
```

升级为：

```text
平台能识别跨项目最佳实践，并推动治理能力在 portfolio 级别对齐
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
7. 不自动把某项目的 best practice 直接应用到其他项目。
8. benchmarking 只做统计、排名、差距分析、对齐建议，不自动同步配置。
9. 不调用真实 AI 自动给出跨组织结论。
10. 不破坏 1-44C 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 benchmark / alignment / maturity 三张表。
2. 聚合 portfolio 级治理表现。
3. 输出 best-practice candidate 与 gap 建议。
4. 展示 org-level ranking 和 maturity scorecard。
5. 导出 Markdown report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V59__init_governance_portfolio_benchmark_tables.sql
```

### 4.1 governance_portfolio_benchmark_snapshot

```sql
CREATE TABLE governance_portfolio_benchmark_snapshot (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    benchmark_window VARCHAR(32) NOT NULL,
    adoption_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    rejection_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_package_quality_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_assistive_usefulness_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    workflow_completion_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    maturity_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    ranking_position INT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_portfolio_benchmark_snapshot_window(benchmark_window, ranking_position),
    KEY idx_governance_portfolio_benchmark_snapshot_project(project_id, captured_at)
);
```

### 4.2 governance_best_practice_alignment_item

```sql
CREATE TABLE governance_best_practice_alignment_item (
    id BIGINT PRIMARY KEY,
    source_project_id BIGINT NOT NULL,
    source_project_name VARCHAR(255) NOT NULL,
    target_project_id BIGINT NULL,
    target_project_name VARCHAR(255) NULL,
    practice_type VARCHAR(64) NOT NULL,
    alignment_level VARCHAR(32) NOT NULL,
    recommendation_text TEXT NULL,
    evidence_summary_text TEXT NULL,
    gap_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    transferable_flag TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_best_practice_alignment_item_source(source_project_id, practice_type),
    KEY idx_governance_best_practice_alignment_item_target(target_project_id, alignment_level)
);
```

### 4.3 governance_maturity_scorecard

```sql
CREATE TABLE governance_maturity_scorecard (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    maturity_level VARCHAR(32) NOT NULL,
    planning_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    execution_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    learning_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    optimization_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    benchmark_percentile DECIMAL(10,2) NOT NULL DEFAULT 0,
    improvement_gap_text TEXT NULL,
    captured_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_maturity_scorecard_project_time(project_id, captured_at),
    KEY idx_governance_maturity_scorecard_level(maturity_level, benchmark_percentile)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceBenchmarkWindow.java
GovernanceBestPracticeType.java
GovernanceAlignmentLevel.java
GovernanceMaturityLevel.java
GovernanceBenchmarkSignalLevel.java
```

### 5.1 GovernanceBenchmarkWindow

```text
DAY_7
DAY_14
DAY_30
DAY_90
```

### 5.2 GovernanceBestPracticeType

```text
DRAFT_ADOPTION
ASSISTIVE_ORDERING
PACKAGE_COMPOSITION
WORKFLOW_COMPLETION
OPERATOR_PRODUCTIVITY
```

### 5.3 GovernanceAlignmentLevel

```text
BEST_PRACTICE
CLOSE_TO_BASELINE
NEEDS_ALIGNMENT
HIGH_GAP
```

### 5.4 GovernanceMaturityLevel

```text
LEADING
STRONG
EMERGING
AT_RISK
```

### 5.5 GovernanceBenchmarkSignalLevel

```text
OUTPERFORM
BASELINE
LAGGING
CRITICAL_GAP
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernancePortfolioBenchmarkSnapshotEntity.java
GovernanceBestPracticeAlignmentItemEntity.java
GovernanceMaturityScorecardEntity.java

GovernancePortfolioBenchmarkSnapshotMapper.java
GovernanceBestPracticeAlignmentItemMapper.java
GovernanceMaturityScorecardMapper.java
```

DTO 建议：

```text
GovernancePortfolioBenchmarkSnapshotResponse.java
GovernanceBestPracticeAlignmentItemResponse.java
GovernanceMaturityScorecardResponse.java
GovernancePortfolioBenchmarkDashboardResponse.java
GovernancePortfolioBenchmarkReportResponse.java
```

### 6.1 GovernancePortfolioBenchmarkDashboardResponse

建议字段：

```text
totalProjects
topRankedProjects
lowestRankedProjects
bestPracticeCandidates
highGapProjects
maturityDistribution
latestBenchmarks
latestScorecards
```

---

## 7. 服务设计

新增应用服务：

```text
GovernancePortfolioBenchmarkService.java
GovernanceBestPracticeAlignmentService.java
GovernanceMaturityScorecardService.java
```

### 7.1 GovernancePortfolioBenchmarkService

职责：

1. 聚合跨项目 adoption / rejection / package quality / assistive usefulness / workflow completion 指标。
2. 生成 portfolio benchmark snapshot。
3. 计算 ranking_position。
4. 输出 top / bottom projects。

建议 maturity_score 公式：

```text
maturityScore =
  adoptionRate * 0.25
  + avgPackageQualityScore * 0.25
  + avgAssistiveUsefulnessScore * 0.20
  + workflowCompletionRate * 0.20
  + max(0, 100 - rejectionRate) * 0.10
```

### 7.2 GovernanceBestPracticeAlignmentService

职责：

1. 从高分项目中识别 best-practice candidate。
2. 对比目标项目与 source project 的 gap。
3. 输出可迁移实践建议。
4. 标记 alignment level 与 transferable flag。

gap_score 建议公式：

```text
gapScore =
  abs(sourceMaturityScore - targetMaturityScore) * 0.5
  + abs(sourcePracticeScore - targetPracticeScore) * 0.5
```

### 7.3 GovernanceMaturityScorecardService

职责：

1. 将治理能力拆分为：

```text
planning_score
execution_score
learning_score
optimization_score
```

2. 计算 benchmark_percentile。
3. 输出 maturity_level。
4. 生成 improvement_gap_text。

建议 maturity_level：

```text
>= 85 LEADING
>= 65 STRONG
>= 40 EMERGING
< 40 AT_RISK
```

---

## 8. API 设计

新增 Controller：

```text
GovernancePortfolioBenchmarkController.java
```

建议端点：

### 8.1 Benchmark Snapshot

```text
POST   /api/governance-benchmark/snapshots/refresh
GET    /api/governance-benchmark/snapshots
```

### 8.2 Best Practice Alignment

```text
POST   /api/governance-benchmark/alignment/refresh
GET    /api/governance-benchmark/alignment
```

### 8.3 Maturity Scorecard / Dashboard / Report

```text
POST   /api/governance-benchmark/scorecards/refresh
GET    /api/governance-benchmark/scorecards
GET    /api/governance-benchmark/dashboard
GET    /api/governance-benchmark/report
```

权限建议：

```text
查看：ADMIN
刷新 benchmark / alignment / scorecard：ADMIN
```

---

## 9. Benchmark 与 Alignment 规则建议

### 9.1 Best Practice Candidate

若某项目：

```text
maturityScore >= 85
adoptionRate >= 75
avgPackageQualityScore >= 80
avgAssistiveUsefulnessScore >= 80
```

则视为：

```text
BEST_PRACTICE candidate
```

### 9.2 Alignment Level

若 target 项目与 source 项目差距：

```text
gapScore <= 10 -> CLOSE_TO_BASELINE
gapScore <= 25 -> NEEDS_ALIGNMENT
gapScore > 25 -> HIGH_GAP
```

source 高分项目可标记为：

```text
BEST_PRACTICE
```

### 9.3 Improvement Gap

建议 improvement_gap_text 至少指出：

```text
哪个维度最弱
与 top benchmark 差距多大
最适合参考哪类 best practice
```

---

## 10. 前端设计

新增组件建议：

```text
GovernancePortfolioBenchmarkPanel.vue
GovernanceBestPracticeAlignmentPanel.vue
GovernanceMaturityScorecardPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernancePortfolioBenchmarkPanel

展示：

1. top / bottom 项目排名
2. maturity score / adoption rate / package quality
3. rank badge
4. benchmark window 筛选

### 10.2 GovernanceBestPracticeAlignmentPanel

展示：

1. best practice candidate 列表
2. source -> target alignment item
3. practice type / alignment level / gap score
4. transferable flag

### 10.3 GovernanceMaturityScorecardPanel

展示：

1. planning / execution / learning / optimization 四维评分
2. maturity level
3. benchmark percentile
4. improvement gap 摘要

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 强调“谁是 best practice、谁需要对齐”
4. scorecard 需要清楚可比较

---

## 11. 后端测试要求

新增：

```text
GovernancePortfolioBenchmarkIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. refresh benchmark snapshots success
2. empty data returns no benchmark safely
3. ranking position computed
4. maturity score computed
5. top ranked project returned
6. lowest ranked project returned
7. refresh alignment items success
8. best practice candidate generated
9. close to baseline alignment generated
10. needs alignment generated
11. high gap alignment generated
12. transferable flag set true for high-quality source
13. gap score computed
14. refresh scorecards success
15. planning score computed
16. execution score computed
17. learning score computed
18. optimization score computed
19. benchmark percentile computed
20. maturity level leading
21. maturity level strong
22. maturity level emerging
23. maturity level at_risk
24. dashboard returns top ranked projects
25. dashboard returns high gap projects
26. dashboard returns maturity distribution
27. report export markdown success
28. unauthorized access reject
29. non-admin refresh reject
30. project with high rejection is ranked lower
31. project with high package quality is ranked higher
32. project with low assistive usefulness lowers maturity
33. best practice source includes evidence summary
34. improvement gap text populated
35. refresh is idempotent for same benchmark window
36. scorecard stored for each project

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-portfolio-benchmark.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 portfolio benchmark 面板
2. best practice alignment 面板可见
3. maturity scorecard 面板可见
4. rank badge 可见
5. alignment level 标签可见
6. maturity percentile 可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 benchmark 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-45a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 portfolio benchmark / best practice alignment / maturity scorecard 表说明
3. GovernancePortfolioBenchmarkService 设计说明
4. GovernanceBestPracticeAlignmentService 设计说明
5. GovernanceMaturityScorecardService 设计说明
6. GovernancePortfolioBenchmarkPanel 说明
7. GovernanceBestPracticeAlignmentPanel 说明
8. GovernanceMaturityScorecardPanel 说明
9. Portfolio Benchmarking / Cross-Org Alignment 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45B

---

## 14. 验收标准

必须全部满足：

1. governance_portfolio_benchmark_snapshot / governance_best_practice_alignment_item / governance_maturity_scorecard 三张表已落库
2. benchmark snapshot 可刷新 / 查询
3. best practice alignment 可刷新 / 查询
4. maturity scorecard 可刷新 / 查询
5. dashboard / report 可导出
6. ranking / maturity / gap / alignment 逻辑可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 45A 后，平台将从：

```text
能优化单项目治理效果
```

升级为：

```text
能识别跨项目最佳实践、做 portfolio benchmark，并推动治理能力跨团队对齐
```

这一步会让治理 Copilot 从“项目级辅助系统”进一步升级成“组织级治理 benchmark 平台”。

---

## 16. 后续建议

Milestone 45A 完成后，建议进入：

```text
Milestone 45B: Governance Benchmark Adoption Tracking & Cross-Team Improvement Loop
```

重点可包括：

1. best practice adoption tracking
2. alignment recommendation follow-up
3. cross-team uplift measurement
4. benchmark drift trend
5. maturity improvement campaign dashboard

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 45A。

文档路径：
docs/milestone-45a-governance-portfolio-benchmarking-cross-org-best-practice-alignment.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 44C outcome-driven draft tuning 基础上，新增 portfolio benchmarking 与 cross-org best-practice alignment。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动把某项目实践直接同步到其他项目。
10. benchmarking / alignment 只做统计、排名、差距分析、建议，不做真实应用。
11. 不调用真实 AI 自动生成跨组织结论。
12. 不要破坏 1-44C 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 44B outcome review / 44C optimization / 43B operator learning 数据结构，不要重复造概念。
17. benchmark 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V59__init_governance_portfolio_benchmark_tables.sql。
2. 新增 governance_portfolio_benchmark_snapshot / governance_best_practice_alignment_item / governance_maturity_scorecard 三张表。
3. 新增 5 个枚举：
   - GovernanceBenchmarkWindow
   - GovernanceBestPracticeType
   - GovernanceAlignmentLevel
   - GovernanceMaturityLevel
   - GovernanceBenchmarkSignalLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernancePortfolioBenchmarkService。
6. 新增 GovernanceBestPracticeAlignmentService。
7. 新增 GovernanceMaturityScorecardService。
8. 新增 API：
   - benchmark snapshots refresh / list
   - alignment refresh / list
   - scorecards refresh / list
   - dashboard / report
9. 前端新增：
   - GovernancePortfolioBenchmarkPanel.vue
   - GovernanceBestPracticeAlignmentPanel.vue
   - GovernanceMaturityScorecardPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-45a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 portfolio benchmark / best practice alignment / maturity scorecard 表说明
3. GovernancePortfolioBenchmarkService 设计说明
4. GovernanceBestPracticeAlignmentService 设计说明
5. GovernanceMaturityScorecardService 设计说明
6. GovernancePortfolioBenchmarkPanel 说明
7. GovernanceBestPracticeAlignmentPanel 说明
8. GovernanceMaturityScorecardPanel 说明
9. Portfolio Benchmarking / Cross-Org Alignment 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 45B

现在开始实现，不要只给计划。
```
