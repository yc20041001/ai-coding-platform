# Milestone 44B: Governance Outcome Review, Draft Adoption Tracking & Assistive Quality Evaluation

## 1. 背景

截至 Milestone 44A，平台已经完成了一整条治理 Copilot 主线：

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
```

现在平台已经能做到：

```text
根据 recommendation / bundle / recipe / forecast 生成 draft remediation plan
生成 safe assistive action checklist
组装 recommendation package
在 operator pre-submit workspace 中呈现可审阅草稿
```

但仍然存在一个关键闭环缺口：

```text
这些 draft plan 最后有没有被采用？
被采用时是原样采用、修改后采用，还是被放弃？
哪些 assistive action 对 operator 真有帮助？
哪些 package 虽然能组装出来，但 review 质量不高？
哪些草稿经常被退回、哪些类型的草稿更容易转化？
```

换句话说，平台现在已经有：

```text
安全起草与预提交辅助能力
```

但还缺少：

```text
对“起草结果是否有价值”的 outcome review 闭环
```

Milestone 44B 的目标就是新增：

```text
Governance Outcome Review, Draft Adoption Tracking & Assistive Quality Evaluation
```

让平台从：

```text
copilot 会起草、会组装、会辅助
```

升级为：

```text
copilot 能知道这些草稿最终有没有被采用、哪里需要改进、哪些辅助动作最有价值
```

---

## 2. 总目标

实现草稿结果评估闭环：

1. 新增 Draft Adoption Review 数据模型。
2. 新增 Assistive Action Quality Review 数据模型。
3. 新增 Package Review Evaluation 数据模型。
4. 支持记录 draft plan 的采用 / 修改 / 拒绝结果。
5. 支持记录 assistive action 的有用性评分与原因。
6. 支持记录 recommendation package 的 review 质量评分。
7. 支持生成 outcome review dashboard。
8. 支持把 adoption / rejection / modification 结果回流到治理分析层。
9. 支持导出 Markdown Outcome Review Report。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
系统知道自己生成了哪些 draft
```

升级为：

```text
系统知道这些 draft 最终是否被采用，以及辅助质量究竟如何
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
8. 不自动把 draft 状态改成 adopted，只记录人工 review 结果。
9. 不调用真实 AI 自动评价 draft / action / package 质量。
10. outcome review 只记录结构化评估、评分、原因与说明，不自动触发治理动作。
11. 不破坏 1-44A 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 adoption review / assistive quality / package evaluation 三张表。
2. 记录人工 review 结论。
3. 聚合 adoption / rejection / modification / usefulness / quality 数据。
4. 在 dashboard 中展示 trend 和 top issue 类型。
5. 导出 Markdown report。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V57__init_governance_outcome_review_tables.sql
```

### 4.1 governance_draft_adoption_review

```sql
CREATE TABLE governance_draft_adoption_review (
    id BIGINT PRIMARY KEY,
    draft_plan_id BIGINT NOT NULL,
    recommendation_id BIGINT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL,
    adoption_result VARCHAR(32) NOT NULL,
    modification_level VARCHAR(32) NOT NULL,
    usefulness_rating INT NOT NULL,
    reason_code VARCHAR(64) NULL,
    outcome_note_text TEXT NULL,
    reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_draft_adoption_review_plan(draft_plan_id),
    KEY idx_governance_draft_adoption_review_result(adoption_result, reviewed_at),
    KEY idx_governance_draft_adoption_review_recommendation(recommendation_id, reviewed_at)
);
```

### 4.2 governance_assistive_action_quality_review

```sql
CREATE TABLE governance_assistive_action_quality_review (
    id BIGINT PRIMARY KEY,
    assistive_action_id BIGINT NOT NULL,
    draft_plan_id BIGINT NULL,
    action_type VARCHAR(64) NOT NULL,
    usefulness_rating INT NOT NULL,
    clarity_rating INT NOT NULL,
    safety_confidence_rating INT NOT NULL,
    outcome_result VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NULL,
    note_text TEXT NULL,
    reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_assistive_action_quality_review_action(assistive_action_id),
    KEY idx_governance_assistive_action_quality_review_type(action_type, reviewed_at),
    KEY idx_governance_assistive_action_quality_review_result(outcome_result, reviewed_at)
);
```

### 4.3 governance_package_review_evaluation

```sql
CREATE TABLE governance_package_review_evaluation (
    id BIGINT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    draft_plan_id BIGINT NULL,
    recommendation_id BIGINT NULL,
    review_quality_score INT NOT NULL,
    completeness_score INT NOT NULL,
    submit_readiness_score INT NOT NULL,
    evaluation_result VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NULL,
    reviewer_note_text TEXT NULL,
    reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_package_review_evaluation_package(package_id),
    KEY idx_governance_package_review_evaluation_result(evaluation_result, reviewed_at),
    KEY idx_governance_package_review_evaluation_recommendation(recommendation_id, reviewed_at)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceDraftAdoptionResult.java
GovernanceDraftModificationLevel.java
GovernanceAssistiveOutcomeResult.java
GovernancePackageEvaluationResult.java
GovernanceOutcomeReviewReasonCode.java
```

### 5.1 GovernanceDraftAdoptionResult

```text
ADOPTED
ADOPTED_WITH_CHANGES
REJECTED
DEFERRED
```

### 5.2 GovernanceDraftModificationLevel

```text
NONE
MINOR
MAJOR
REWRITTEN
```

### 5.3 GovernanceAssistiveOutcomeResult

```text
USEFUL
PARTIALLY_USEFUL
NOT_USEFUL
MISLEADING
```

### 5.4 GovernancePackageEvaluationResult

```text
STRONG
ACCEPTABLE
WEAK
REWORK_REQUIRED
```

### 5.5 GovernanceOutcomeReviewReasonCode

```text
CLEAR_AND_ACTIONABLE
TOO_GENERIC
MISSING_CONTEXT
TOO_COMPLEX
LOW_CONFIDENCE
GOOD_STRUCTURE
INCOMPLETE_PACKAGE
SAFE_BUT_LOW_VALUE
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceDraftAdoptionReviewEntity.java
GovernanceAssistiveActionQualityReviewEntity.java
GovernancePackageReviewEvaluationEntity.java

GovernanceDraftAdoptionReviewMapper.java
GovernanceAssistiveActionQualityReviewMapper.java
GovernancePackageReviewEvaluationMapper.java
```

DTO 建议：

```text
RecordGovernanceDraftAdoptionReviewRequest.java
GovernanceDraftAdoptionReviewResponse.java

RecordGovernanceAssistiveActionQualityReviewRequest.java
GovernanceAssistiveActionQualityReviewResponse.java

RecordGovernancePackageReviewEvaluationRequest.java
GovernancePackageReviewEvaluationResponse.java

GovernanceOutcomeReviewDashboardResponse.java
GovernanceOutcomeReviewReportResponse.java
```

### 6.1 GovernanceOutcomeReviewDashboardResponse

建议字段：

```text
totalDraftReviewCount
adoptedRate
adoptedWithChangesRate
rejectedRate
avgDraftUsefulnessRating
avgAssistiveUsefulnessRating
avgPackageQualityScore
topRejectionReasons
topUsefulAssistiveActionTypes
latestDraftReviews
latestPackageEvaluations
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceDraftOutcomeReviewService.java
GovernanceAssistiveQualityService.java
GovernancePackageEvaluationService.java
```

### 7.1 GovernanceDraftOutcomeReviewService

职责：

1. 记录 draft plan 的 adoption review。
2. 支持 adoption / adoption with changes / rejected / deferred。
3. 记录 modification level 与 usefulness rating。
4. 聚合 adopted rate / rejected rate / modification profile。
5. 导出 Markdown review summary。

### 7.2 GovernanceAssistiveQualityService

职责：

1. 记录 assistive action 的 usefulness / clarity / safety confidence。
2. 聚合最有价值和最弱的 action type。
3. 输出 assistive action quality trend。
4. 标记“安全但价值低”的辅助动作模式。

建议评分聚合：

```text
assistiveQualityScore =
  usefulnessRating * 0.45
  + clarityRating * 0.30
  + safetyConfidenceRating * 0.25
```

### 7.3 GovernancePackageEvaluationService

职责：

1. 记录 package review quality、completeness、submit readiness。
2. 聚合 package 的整体质量趋势。
3. 标记低质量 package 的主要原因。
4. 为后续 44C/45A 的 package 优化提供结构化输入。

建议综合评分：

```text
packageCompositeScore =
  reviewQualityScore * 0.45
  + completenessScore * 0.30
  + submitReadinessScore * 0.25
```

结果建议：

```text
>= 85 STRONG
>= 65 ACCEPTABLE
>= 40 WEAK
< 40 REWORK_REQUIRED
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceOutcomeReviewController.java
```

建议端点：

### 8.1 Draft Adoption Review

```text
POST   /api/governance-outcome/draft-reviews
GET    /api/governance-outcome/draft-reviews
GET    /api/governance-outcome/draft-reviews/{reviewId}
```

### 8.2 Assistive Action Quality Review

```text
POST   /api/governance-outcome/assistive-reviews
GET    /api/governance-outcome/assistive-reviews
GET    /api/governance-outcome/assistive-reviews/{reviewId}
```

### 8.3 Package Review Evaluation / Dashboard / Report

```text
POST   /api/governance-outcome/package-evaluations
GET    /api/governance-outcome/package-evaluations
GET    /api/governance-outcome/dashboard
GET    /api/governance-outcome/report
```

权限建议：

```text
查看：ADMIN
记录 review / evaluation：ADMIN
```

---

## 9. Outcome Review 规则建议

### 9.1 Draft Adoption

adopted rate 公式：

```text
adoptedRate = ADOPTED / totalDraftReviewCount
adoptedWithChangesRate = ADOPTED_WITH_CHANGES / totalDraftReviewCount
rejectedRate = REJECTED / totalDraftReviewCount
```

### 9.2 Modification Profile

如果某类 draft 经常是：

```text
ADOPTED_WITH_CHANGES + MAJOR / REWRITTEN
```

则说明 draft 的方向可能对，但质量或上下文不足。

### 9.3 Assistive Action Quality

若某类 assistive action：

```text
safetyConfidence 高
但 usefulness 低
```

则标记为：

```text
安全但低价值
```

### 9.4 Package Quality

若 package：

```text
submitReadinessScore 高
但 completenessScore 低
```

则说明 package 结构可提交，但内容不完整，需要优化预填与上下文组装。

---

## 10. 前端设计

新增组件建议：

```text
GovernanceDraftOutcomeReviewPanel.vue
GovernanceAssistiveQualityPanel.vue
GovernancePackageEvaluationPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceDraftOutcomeReviewPanel

展示：

1. draft adoption review 列表
2. adoption result / modification level / usefulness rating
3. reason code / reviewer note
4. adopted / rejected 指标卡

### 10.2 GovernanceAssistiveQualityPanel

展示：

1. assistive action 质量列表
2. usefulness / clarity / safety confidence
3. top useful action types
4. safe but low value 标签

### 10.3 GovernancePackageEvaluationPanel

展示：

1. package 质量评估列表
2. review quality / completeness / submit readiness
3. evaluation result 标签
4. package composite score

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 要清楚区分 adopted / adopted with changes / rejected
4. 需要突出“哪些辅助动作真正有帮助”

---

## 11. 后端测试要求

新增：

```text
GovernanceOutcomeReviewIntegrationTest.java
```

不少于 36 个集成测试，建议覆盖：

1. record draft adoption review success
2. draft review unique per draft plan
3. adopted review stored correctly
4. adopted with changes stored correctly
5. rejected review stored correctly
6. deferred review stored correctly
7. modification level none stored
8. modification level major stored
9. draft usefulness rating aggregated
10. adopted rate computed
11. rejected rate computed
12. record assistive quality review success
13. assistive review unique per assistive action
14. assistive usefulness rating aggregated
15. assistive clarity rating aggregated
16. safety confidence rating aggregated
17. safe but low value pattern detected
18. top useful assistive action type returned
19. record package evaluation success
20. package evaluation unique per package
21. package composite score computed
22. strong evaluation result stored
23. acceptable evaluation result stored
24. weak evaluation result stored
25. rework required result stored
26. dashboard returns draft review metrics
27. dashboard returns assistive metrics
28. dashboard returns package metrics
29. report export markdown success
30. empty data returns empty dashboard safely
31. unauthorized access reject
32. non-admin mutation reject
33. low completeness lowers package result
34. high modification level affects summary
35. repeated rejection reason appears in top reasons
36. accepted draft with low usefulness still tracked correctly

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-outcome-review.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 draft outcome review 面板
2. assistive quality 面板可见
3. package evaluation 面板可见
4. adopted / rejected 指标卡可见
5. usefulness / clarity / safety 标签可见
6. package quality 分数可见
7. report 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 outcome review 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-44b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 draft adoption review / assistive action quality review / package review evaluation 表说明
3. GovernanceDraftOutcomeReviewService 设计说明
4. GovernanceAssistiveQualityService 设计说明
5. GovernancePackageEvaluationService 设计说明
6. GovernanceDraftOutcomeReviewPanel 说明
7. GovernanceAssistiveQualityPanel 说明
8. GovernancePackageEvaluationPanel 说明
9. Outcome Review / Adoption Tracking 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44C

---

## 14. 验收标准

必须全部满足：

1. governance_draft_adoption_review / governance_assistive_action_quality_review / governance_package_review_evaluation 三张表已落库
2. draft adoption review 可记录 / 查询
3. assistive quality review 可记录 / 查询
4. package evaluation 可记录 / 查询
5. dashboard / report 可导出
6. adoption / rejection / modification / usefulness / quality 指标可计算
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无数据前置条件显式降级处理

---

## 15. 完成后的价值

完成 44B 后，平台将从：

```text
copilot 会起草并生成安全辅助动作
```

升级为：

```text
copilot 能评估这些草稿与辅助动作最终是否真的帮助了操作员
```

这一步会让治理 Copilot 从“能安全起草”进一步升级成“能衡量自己起草质量的辅助系统”。

---

## 16. 后续建议

Milestone 44B 完成后，建议进入：

```text
Milestone 44C: Governance Assistive Planning Optimization & Outcome-Driven Draft Tuning
```

重点可包括：

1. draft template optimization suggestions
2. assistive action ordering optimization
3. package composition optimization
4. adoption-driven tuning signals
5. assistive quality trend dashboard

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 44B。

文档路径：
docs/milestone-44b-governance-outcome-review-draft-adoption-assistive-quality-evaluation.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 44A draft planning / safe assistive actions 基础上，新增 outcome review、draft adoption tracking 与 assistive quality evaluation。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / execution / recipe / knowledge 原始记录。
6. 不要自动批准 waiver。
7. 不要自动完成 recommendation。
8. 不要自动分配 owner。
9. 不要自动提交 recommendation package。
10. outcome review 只记录人工评估与结构化结果，不自动触发治理动作。
11. 不调用真实 AI 自动评价 draft / package / assistive quality。
12. 不要破坏 1-44A 已有 API。
13. 前端保持中文暗色科技风 UI，复用现有组件。
14. IDs 对外保持 String。
15. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
16. 优先复用 44A draft plan / assistive action / package 数据结构，不要重复造概念。
17. outcome review 数据不足时必须返回明确空态或降级结果，不得抛出 500。

需要实现：
1. 新增 V57__init_governance_outcome_review_tables.sql。
2. 新增 governance_draft_adoption_review / governance_assistive_action_quality_review / governance_package_review_evaluation 三张表。
3. 新增 5 个枚举：
   - GovernanceDraftAdoptionResult
   - GovernanceDraftModificationLevel
   - GovernanceAssistiveOutcomeResult
   - GovernancePackageEvaluationResult
   - GovernanceOutcomeReviewReasonCode
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceDraftOutcomeReviewService。
6. 新增 GovernanceAssistiveQualityService。
7. 新增 GovernancePackageEvaluationService。
8. 新增 API：
   - draft adoption review record / list / detail
   - assistive quality review record / list / detail
   - package evaluation record / list
   - dashboard / report
9. 前端新增：
   - GovernanceDraftOutcomeReviewPanel.vue
   - GovernanceAssistiveQualityPanel.vue
   - GovernancePackageEvaluationPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 36 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-44b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 draft adoption review / assistive action quality review / package review evaluation 表说明
3. GovernanceDraftOutcomeReviewService 设计说明
4. GovernanceAssistiveQualityService 设计说明
5. GovernancePackageEvaluationService 设计说明
6. GovernanceDraftOutcomeReviewPanel 说明
7. GovernanceAssistiveQualityPanel 说明
8. GovernancePackageEvaluationPanel 说明
9. Outcome Review / Adoption Tracking 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 44C

现在开始实现，不要只给计划。
```
