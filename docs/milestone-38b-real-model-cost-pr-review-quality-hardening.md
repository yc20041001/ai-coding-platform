# Milestone 38B: Real Model Cost & PR Review Quality Hardening

## 1. 背景

Milestone 38A 已经完成了 `External Beta Feedback Loop & Real-world Hardening`：

```text
真实 Beta Session
  -> 结构化 Feedback
  -> Environment Readiness
  -> Pass / Block Summary
```

与此同时，平台在 20-24 阶段已经具备：

```text
Real Model Provider Support
GitHub OAuth
Read-only PR Review
Observability / Audit / Incident / RCA / Retrospective
```

但 `v1.1 External Beta` 的两个关键通过标准仍然没有真正被产品化收口：

```text
模型成本可解释
GitHub OAuth / PR Review 无高危安全问题，且评审结果可用
```

当前系统还缺少的不是“再接一个模型”或“再加一个页面”，而是：

1. 真正按 provider / model / project / feature 解释成本。
2. 识别异常成本、失败成本、fallback 成本。
3. 对 PR Review 的覆盖率、命中率、失败原因、人工采纳情况做质量治理。
4. 把这些信息纳入 Beta Dashboard，形成放行依据。

Milestone 38B 的目标是新增：

```text
Real Model Cost & PR Review Quality Hardening
```

让项目从：

```text
支持真实模型、支持 PR Review
```

升级为：

```text
真实模型可控成本、PR Review 可度量质量、Beta 可判定是否达标
```

---

## 2. 总目标

实现真实模型成本治理与 PR Review 质量硬化能力：

1. 新增 Model Cost Summary 数据模型。
2. 新增 Model Cost Alert 数据模型。
3. 新增 PR Review Quality Record 数据模型。
4. 支持按 provider / model / project / requestType 聚合成本。
5. 支持识别 fallback、失败、重试带来的额外成本。
6. 支持记录 PR Review 是否完成、是否有人审阅、是否采纳建议。
7. 支持记录 PR Review 的 review finding 统计、人工反馈与最终质量状态。
8. 支持项目级 / Beta 级成本与 PR Review 质量仪表盘。
9. 支持导出成本与 PR Review 质量摘要 Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
能调用真实模型，能生成 PR Review
```

升级为：

```text
知道花了多少钱、为什么花、哪里异常、PR Review 是否真的有价值
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch` / `git reset` / `git apply` / `git add` / `git commit` / `git push`。
4. 不发送真实 Slack / PagerDuty / Email。
5. 不自动对 GitHub PR 做 approve / comment / merge / close。
6. 不调用真实 AI 自动给 PR Review 打分。
7. 成本数据只基于现有 ModelRequestLog / PR Review / Audit / Incident 数据聚合，不伪造账单。
8. 不破坏 1-38A 已有 API。
9. 不修改现有真实模型调用协议。
10. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增成本汇总 / 告警 / PR Review 质量表。
2. 聚合现有 model_request_log / pr_review_job / pr_review_finding / operator review / beta feedback 数据。
3. 新增 Dashboard / 明细表 / 导出接口。
4. 新增成本告警和 PR Review 质量状态判定。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V39__init_model_cost_pr_review_quality_tables.sql
```

### 4.1 model_cost_summary

```sql
CREATE TABLE model_cost_summary (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    request_type VARCHAR(64) NOT NULL,
    stat_date DATE NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    fallback_count BIGINT NOT NULL DEFAULT 0,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(18,6) NOT NULL DEFAULT 0,
    avg_latency_ms BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_model_cost_project_date(project_id, stat_date),
    KEY idx_model_cost_provider_model(provider, model_name),
    KEY idx_model_cost_request_type(request_type),
    UNIQUE KEY uk_model_cost_daily(project_id, provider, model_name, request_type, stat_date)
);
```

### 4.2 model_cost_alert

```sql
CREATE TABLE model_cost_alert (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    stat_date DATE NOT NULL,
    threshold_value DECIMAL(18,6) NULL,
    actual_value DECIMAL(18,6) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_model_cost_alert_project(project_id, stat_date),
    KEY idx_model_cost_alert_status(status, severity),
    KEY idx_model_cost_alert_provider(provider, model_name)
);
```

### 4.3 pr_review_quality_record

```sql
CREATE TABLE pr_review_quality_record (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    review_job_id BIGINT NOT NULL,
    github_binding_id BIGINT NULL,
    repository_full_name VARCHAR(255) NOT NULL,
    pull_request_number BIGINT NOT NULL,
    strategy_key VARCHAR(64) NULL,
    model_provider VARCHAR(32) NULL,
    model_name VARCHAR(128) NULL,
    findings_total INT NOT NULL DEFAULT 0,
    high_risk_findings INT NOT NULL DEFAULT 0,
    medium_risk_findings INT NOT NULL DEFAULT 0,
    low_risk_findings INT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    human_feedback_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    adoption_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    usefulness_score INT NULL,
    false_positive_score INT NULL,
    review_comment TEXT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_pr_quality_project_time(project_id, create_time),
    KEY idx_pr_quality_repo_pr(repository_full_name, pull_request_number),
    KEY idx_pr_quality_status(review_status, human_feedback_status, adoption_status),
    UNIQUE KEY uk_pr_quality_job(review_job_id)
);
```

无物理外键，保持项目风格。

---

## 5. 枚举设计

新增：

```text
ModelCostAlertType.java
ModelCostAlertSeverity.java
ModelCostAlertStatus.java
PrReviewHumanFeedbackStatus.java
PrReviewAdoptionStatus.java
PrReviewQualityStatus.java
```

### 5.1 ModelCostAlertType

```text
DAILY_COST_SPIKE
HIGH_FAILURE_COST
HIGH_FALLBACK_RATE
HIGH_RETRY_COST
LATENCY_COST_ANOMALY
```

### 5.2 ModelCostAlertSeverity

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.3 ModelCostAlertStatus

```text
OPEN
ACKNOWLEDGED
RESOLVED
IGNORED
```

### 5.4 PrReviewHumanFeedbackStatus

```text
PENDING
REVIEWED
CONFIRMED
DISMISSED
```

### 5.5 PrReviewAdoptionStatus

```text
UNKNOWN
PARTIAL
ADOPTED
NOT_ADOPTED
```

### 5.6 PrReviewQualityStatus

```text
COMPLETED
FAILED
LOW_SIGNAL
ACTIONABLE
HIGH_VALUE
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ModelCostSummaryEntity.java
ModelCostAlertEntity.java
PrReviewQualityRecordEntity.java

ModelCostSummaryMapper.java
ModelCostAlertMapper.java
PrReviewQualityRecordMapper.java
```

新增 DTO：

```text
ModelCostSummaryResponse.java
ModelCostTrendResponse.java
ModelCostAlertResponse.java
ModelCostDashboardResponse.java

CreatePrReviewQualityRecordRequest.java
UpdatePrReviewQualityRecordRequest.java
PrReviewQualityRecordResponse.java
PrReviewQualityDashboardResponse.java

ExportModelCostReportResponse.java
ExportPrReviewQualityReportResponse.java
```

### 6.1 CreatePrReviewQualityRecordRequest

```java
public class CreatePrReviewQualityRecordRequest {
    private String reviewJobId;
    private String humanFeedbackStatus;
    private String adoptionStatus;
    private Integer usefulnessScore;
    private Integer falsePositiveScore;
    private String reviewComment;
}
```

### 6.2 UpdatePrReviewQualityRecordRequest

```java
public class UpdatePrReviewQualityRecordRequest {
    private String humanFeedbackStatus;
    private String adoptionStatus;
    private Integer usefulnessScore;
    private Integer falsePositiveScore;
    private String reviewComment;
}
```

校验：

1. `usefulnessScore` 范围 `0-5`
2. `falsePositiveScore` 范围 `0-5`
3. 一个 `reviewJobId` 默认只允许一个 quality record

---

## 7. 后端服务设计

### 7.1 ModelCostAnalyticsService

新增：

```text
ModelCostAnalyticsService.java
```

职责：

1. 从 `model_request_log` 聚合 daily cost summary。
2. 按 provider / model / requestType / project 维度输出汇总。
3. 计算失败率、fallback 率、平均延迟、成本分布。
4. 生成成本趋势和 dashboard。

建议方法：

```java
public void refreshDailySummaries(LocalDate statDate)

public List<ModelCostSummaryResponse> listCostSummaries(Long projectId, LocalDate from, LocalDate to, String provider, String modelName)

public List<ModelCostTrendResponse> getCostTrend(Long projectId, Integer days)

public ModelCostDashboardResponse getCostDashboard(Long projectId, Integer days)
```

### 7.2 ModelCostAlertService

新增：

```text
ModelCostAlertService.java
```

职责：

1. 扫描 daily summary 生成成本告警。
2. 识别成本 spike / fallback spike / failure cost。
3. 维护 OPEN / ACKNOWLEDGED / RESOLVED 状态。
4. 提供项目级成本告警列表。

建议规则：

```text
daily cost > recent 7d avg * 2.0 -> DAILY_COST_SPIKE
fallback_rate > 0.30 -> HIGH_FALLBACK_RATE
failure_count > 0 且 estimated_cost > threshold -> HIGH_FAILURE_COST
retry cost / total cost > 0.20 -> HIGH_RETRY_COST
```

建议方法：

```java
public void scanAlerts(LocalDate statDate)

public PageResult<ModelCostAlertResponse> listAlerts(Long projectId, String status, String severity, PageQuery pageQuery)

public ModelCostAlertResponse updateAlertStatus(Long alertId, String status)
```

### 7.3 PrReviewQualityService

新增：

```text
PrReviewQualityService.java
```

职责：

1. 基于 `pr_review_job` 和 `pr_review_finding` 自动生成 quality baseline。
2. 允许人工补充 human feedback / adoption status / usefulness / false positive。
3. 输出项目级 PR Review 质量 dashboard。
4. 计算 actionable ratio / failure ratio / adoption ratio。

建议方法：

```java
public PrReviewQualityRecordResponse createQualityRecord(CreatePrReviewQualityRecordRequest request)

public PrReviewQualityRecordResponse updateQualityRecord(Long recordId, UpdatePrReviewQualityRecordRequest request)

public PageResult<PrReviewQualityRecordResponse> listQualityRecords(Long projectId, String humanFeedbackStatus, String adoptionStatus, PageQuery pageQuery)

public PrReviewQualityDashboardResponse getQualityDashboard(Long projectId, Integer days)
```

### 7.4 导出服务

新增：

```text
ModelCostReportExportService.java
PrReviewQualityExportService.java
```

职责：

1. 导出项目级成本报告 Markdown
2. 导出项目级 PR Review 质量报告 Markdown

要求：

1. 不调用真实 AI
2. 仅基于聚合数据和人工反馈生成

---

## 8. API 设计

### 8.1 Model Cost

```http
GET /api/projects/{projectId}/model-cost/dashboard?days=
GET /api/projects/{projectId}/model-cost/summaries?from=&to=&provider=&modelName=&page=&size=
GET /api/projects/{projectId}/model-cost/trend?days=
GET /api/projects/{projectId}/model-cost/alerts?status=&severity=&page=&size=
PUT /api/model-cost/alerts/{alertId}/status
GET /api/projects/{projectId}/model-cost/report/markdown?days=
```

### 8.2 PR Review Quality

```http
POST /api/projects/{projectId}/pr-review-quality-records
PUT /api/pr-review-quality-records/{recordId}
GET /api/projects/{projectId}/pr-review-quality-records?humanFeedbackStatus=&adoptionStatus=&page=&size=
GET /api/projects/{projectId}/pr-review-quality/dashboard?days=
GET /api/projects/{projectId}/pr-review-quality/report/markdown?days=
```

权限建议：

```text
POST / PUT: MAINTAINER+
GET: VIEWER+
```

---

## 9. 前端设计

### 9.1 ObservabilityPage 增强

在当前 Observability 页面新增两大区块：

1. `真实模型成本治理`
2. `PR Review 质量治理`

推荐 data-testid：

```text
model-cost-dashboard
model-cost-summary-table
model-cost-alert-table
model-cost-report-button
pr-review-quality-dashboard
pr-review-quality-table
pr-review-quality-report-button
```

### 9.2 ModelCostDashboardPanel

新增：

```text
frontend/src/modules/admin/components/ModelCostDashboardPanel.vue
```

功能：

1. 展示总成本、token、fallback、failure、avg latency
2. 展示 provider / model / requestType 成本分布
3. 展示近 N 天趋势
4. 展示 cost alerts

### 9.3 PrReviewQualityPanel

新增：

```text
frontend/src/modules/admin/components/PrReviewQualityPanel.vue
```

功能：

1. 展示 quality dashboard
2. 展示 quality record 列表
3. 录入人工反馈
4. 修改 adoption status / usefulness / false positive 分数
5. 导出 quality report

### 9.4 Beta Dashboard 联动

可选但推荐：

1. 在 38A 的 Beta Dashboard 中增加：
   - 本周真实模型总成本
   - 高风险成本告警数
   - PR Review adoption ratio
   - PR Review failure ratio

---

## 10. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
```

新增：

```ts
export interface ModelCostSummary {
  id: string
  projectId?: string
  provider: string
  modelName: string
  requestType: string
  statDate: string
  requestCount: number
  successCount: number
  failureCount: number
  fallbackCount: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  estimatedCost: number
  avgLatencyMs: number
}

export interface ModelCostAlert {
  id: string
  projectId?: string
  provider: string
  modelName: string
  alertType: string
  severity: string
  status: string
  summary: string
  detail?: string
  statDate: string
  thresholdValue?: number
  actualValue?: number
}

export interface PrReviewQualityRecord {
  id: string
  projectId: string
  reviewJobId: string
  repositoryFullName: string
  pullRequestNumber: number
  strategyKey?: string
  modelProvider?: string
  modelName?: string
  findingsTotal: number
  highRiskFindings: number
  mediumRiskFindings: number
  lowRiskFindings: number
  reviewStatus: string
  humanFeedbackStatus: string
  adoptionStatus: string
  usefulnessScore?: number
  falsePositiveScore?: number
  reviewComment?: string
  reviewedBy?: string
  reviewedAt?: string
}
```

新增 API 函数：

```ts
getModelCostDashboard(projectId: string, days?: number)
listModelCostSummaries(projectId: string, params)
getModelCostTrend(projectId: string, days?: number)
listModelCostAlerts(projectId: string, params)
updateModelCostAlertStatus(alertId: string, status: string)
exportModelCostReportMarkdown(projectId: string, days?: number)

createPrReviewQualityRecord(projectId: string, data)
updatePrReviewQualityRecord(recordId: string, data)
listPrReviewQualityRecords(projectId: string, params)
getPrReviewQualityDashboard(projectId: string, days?: number)
exportPrReviewQualityReportMarkdown(projectId: string, days?: number)
```

---

## 11. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/observability/ModelCostAnalyticsIntegrationTest.java
backend/src/test/java/com/aicoding/platform/github/PrReviewQualityIntegrationTest.java
backend/src/test/java/com/aicoding/platform/observability/ModelCostAlertIntegrationTest.java
```

至少 40 个测试。

### 11.1 Model Cost

1. dashboard 可查询
2. summaries 分页成功
3. trend 返回按天排序
4. project 维度过滤成功
5. provider 维度过滤成功
6. modelName 维度过滤成功
7. requestType 聚合正确
8. estimatedCost 聚合正确
9. fallbackCount 聚合正确
10. failureCount 聚合正确
11. avgLatencyMs 计算正确
12. 无数据返回空结果

### 11.2 Cost Alert

13. spike 规则生成 DAILY_COST_SPIKE
14. fallback 规则生成 HIGH_FALLBACK_RATE
15. failure cost 规则生效
16. update alert status 成功
17. status 过滤成功
18. severity 过滤成功
19. project 隔离正确

### 11.3 PR Review Quality

20. 可创建 quality record
21. reviewJobId 不存在返回 BAD_REQUEST 或 NOT_FOUND
22. usefulnessScore 范围校验
23. falsePositiveScore 范围校验
24. update quality record 成功
25. dashboard 可查询
26. findings 统计正确
27. adoption ratio 正确
28. failure ratio 正确
29. humanFeedbackStatus 过滤成功
30. adoptionStatus 过滤成功
31. 同一 reviewJobId 重复创建返回 CONFLICT

### 11.4 权限与导出

32. 未登录查询 cost dashboard 返回 UNAUTHORIZED
33. 未登录查询 pr review dashboard 返回 UNAUTHORIZED
34. VIEWER 可查询
35. MAINTAINER 可创建 / 更新
36. 非项目成员拒绝访问
37. 成本报告 markdown 导出成功
38. PR Review 质量报告 markdown 导出成功
39. markdown 不泄露 secret
40. 跨项目数据不串

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/model-cost-pr-review-quality.spec.ts
```

至少 8 个 E2E：

1. Observability 页面显示模型成本区块
2. 模型成本 dashboard 可见
3. cost alerts 表格可见
4. PR Review 质量区块可见
5. quality record 列表可见
6. 可编辑 human feedback / adoption status
7. markdown 导出入口可见
8. 页面无 JS error
9. 筛选功能可用

---

## 13. 文档与报告

完成后新增：

```text
docs/milestone-38b-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. model_cost_summary 表说明
3. model_cost_alert 表说明
4. pr_review_quality_record 表说明
5. ModelCostAnalyticsService 设计说明
6. ModelCostAlertService 设计说明
7. PrReviewQualityService 设计说明
8. API 清单
9. 前端模型成本 / PR Review 质量 UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 38C

---

## 14. 验收标准

完成后必须满足：

1. 可查看项目级真实模型成本 dashboard
2. 可查看 provider / model / requestType 成本汇总
3. 可查看成本告警
4. 可更新告警状态
5. 可创建 / 更新 PR Review quality record
6. 可查看 PR Review quality dashboard
7. 可导出成本和 PR Review 质量 Markdown 报告
8. 所有 API 有权限校验
9. 不调用真实 AI 自动打分
10. 不执行真实工具或 Git 写操作
11. 后端测试通过
12. 前端 typecheck / build / E2E 通过

---

## 15. 非目标

本阶段不做：

1. 不做真实账单对账
2. 不做 provider 官方 billing API 对接
3. 不做真实 PR merge 结果自动采集
4. 不做复杂 ROI 模型
5. 不做跨组织成本中心
6. 不做自动审批 release gate

这些可以放到后续 Milestone。

---

## 16. 建议后续 Milestone

完成 38B 后，建议进入：

```text
Milestone 38C: Beta Release Gate & Go/No-Go Decision Center
```

候选能力：

1. 将 38A 的试用反馈、38B 的成本/质量、37H-37K 的 incident / retrospective 汇总为统一发布门禁
2. 形成 Go / No-Go 决策面板
3. 输出版本级 Beta Readiness Report

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 38B。

文档路径：
docs/milestone-38b-real-model-cost-pr-review-quality-hardening.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 20-38A 基础上，新增 Real Model Cost & PR Review Quality Hardening。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要发送真实 Slack / PagerDuty / Email。
7. 不要自动对 GitHub PR 做 approve / comment / merge / close。
8. 不调用真实 AI 自动给 PR Review 打分。
9. 成本数据只基于现有 model_request_log / pr_review_job / pr_review_finding / audit / beta 数据聚合，不伪造账单。
10. 不要破坏 1-38A 已有 API。
11. 不修改现有真实模型调用协议。
12. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V39__init_model_cost_pr_review_quality_tables.sql。
2. 新增 model_cost_summary / model_cost_alert / pr_review_quality_record 三张表。
3. 新增相关枚举、Entity、Mapper、DTO。
4. 新增 ModelCostAnalyticsService。
5. 新增 ModelCostAlertService。
6. 新增 PrReviewQualityService。
7. 新增导出服务。
8. 新增 API：
   - GET /api/projects/{projectId}/model-cost/dashboard
   - GET /api/projects/{projectId}/model-cost/summaries
   - GET /api/projects/{projectId}/model-cost/trend
   - GET /api/projects/{projectId}/model-cost/alerts
   - PUT /api/model-cost/alerts/{alertId}/status
   - GET /api/projects/{projectId}/model-cost/report/markdown
   - POST /api/projects/{projectId}/pr-review-quality-records
   - PUT /api/pr-review-quality-records/{recordId}
   - GET /api/projects/{projectId}/pr-review-quality-records
   - GET /api/projects/{projectId}/pr-review-quality/dashboard
   - GET /api/projects/{projectId}/pr-review-quality/report/markdown
9. 前端在 ObservabilityPage 中新增模型成本区块和 PR Review 质量区块。
10. 新增 ModelCostDashboardPanel.vue。
11. 新增 PrReviewQualityPanel.vue。
12. 后端测试不少于 40 个。
13. 前端 E2E 不少于 8 个。
14. 新增 docs/milestone-38b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. model_cost_summary 表说明
3. model_cost_alert 表说明
4. pr_review_quality_record 表说明
5. ModelCostAnalyticsService 设计说明
6. ModelCostAlertService 设计说明
7. PrReviewQualityService 设计说明
8. API 清单
9. 前端模型成本 / PR Review 质量 UI 说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 38C

现在开始实现，不要只给计划。
```
