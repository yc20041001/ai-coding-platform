# Milestone 37K: Incident Retrospective Report & Knowledge Quality Review

## 1. 背景

Milestone 37H-37J 已经把工具执行运维链路推进到：

```text
Tool Failure / Warning
  -> Incident
  -> SLA
  -> Escalation
  -> Timeline
  -> Operator Review
  -> Root Cause Note
  -> Similar Incident Search
  -> Knowledge Document Link
```

当前系统已经具备：

1. Incident 创建、处理、升级、关闭。
2. SLA / Escalation / Timeline 展示。
3. Root Cause Note 记录与发布。
4. Incident 与 Knowledge Document 关联。
5. Similar Incident Mock 检索。

但还缺少一层真正面向团队复盘与知识治理的能力：

```text
这次事故是否完成了标准复盘？
复盘质量是否达标？
关联知识文档是否完整、可复用、已审核？
后续类似问题有没有被真正吸收进流程？
```

Milestone 37K 的目标是新增：

```text
Incident Retrospective Report & Knowledge Quality Review
```

让 Incident 从：

```text
可处理、可升级、可记录
```

升级为：

```text
可复盘、可评审、可量化、可治理
```

---

## 2. 总目标

实现 Incident 复盘报告与知识质量评审能力：

1. 新增 Incident Retrospective Report 数据模型。
2. 新增 Knowledge Quality Review 数据模型。
3. 支持基于 Incident + Root Cause Note + Timeline + Escalation 自动生成复盘报告草稿。
4. 支持人工补充复盘结论、改进行动项、follow-up owner / due date。
5. 支持对 Incident 关联 Knowledge Document 做质量评审。
6. 支持 Knowledge Quality Status 聚合展示。
7. 支持 Similar Incident Regression Check，标记“是否已有同类问题但仍重复发生”。
8. Incident Detail Drawer / Observability 展示复盘与知识质量状态。
9. 支持导出 Incident Retrospective Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
Incident 有记录、有 RCA
```

升级为：

```text
Incident 有正式复盘、有质量门、有知识治理闭环
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch` / `git reset` / `git apply`。
4. 不写真实代码文件到 workspace。
5. 不调用真实模型自动生成 retrospective 内容。
6. Retrospective 草稿只能基于现有 Incident / RCA / Timeline / Review / Trace 数据拼装。
7. Knowledge Quality Review 只记录人工评审结果，不自动批准。
8. 不破坏 36A-37J 已有 API。
9. 不破坏 RAG / Knowledge Base 现有 API。
10. 不发送真实外部通知。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 retrospective / quality review 表。
2. 新增只读聚合查询 API。
3. 新增导出 Markdown。
4. 新增 review checklist / score / status。
5. 新增相似事故复发判断。
6. 前端增加复盘与质量面板。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V37__init_incident_retrospective_quality_tables.sql
```

### 4.1 tool_incident_retrospective

```sql
CREATE TABLE tool_incident_retrospective (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause_note_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    what_happened TEXT NULL,
    impact_summary TEXT NULL,
    response_summary TEXT NULL,
    lessons_learned TEXT NULL,
    prevention_plan TEXT NULL,
    action_items TEXT NULL,
    owner_id BIGINT NULL,
    due_at DATETIME NULL,
    regression_risk VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    repeated_incident TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_incident_retro_project_time(project_id, create_time),
    KEY idx_incident_retro_incident(incident_id),
    KEY idx_incident_retro_status(status),
    KEY idx_incident_retro_owner(owner_id)
);
```

### 4.2 tool_knowledge_quality_review

```sql
CREATE TABLE tool_knowledge_quality_review (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    knowledge_document_id BIGINT NOT NULL,
    retrospective_id BIGINT NULL,
    completeness_score INT NOT NULL DEFAULT 0,
    accuracy_score INT NOT NULL DEFAULT 0,
    actionability_score INT NOT NULL DEFAULT 0,
    relevance_score INT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    overall_status VARCHAR(32) NOT NULL DEFAULT 'NEEDS_WORK',
    checklist_json JSON NULL,
    review_comment TEXT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_kq_review_project_time(project_id, create_time),
    KEY idx_kq_review_incident(incident_id),
    KEY idx_kq_review_document(knowledge_document_id),
    KEY idx_kq_review_status(review_status, overall_status)
);
```

### 4.3 无物理外键

继续保持项目当前风格：

1. 无物理外键。
2. 通过 service 层校验 project / incident / document 归属。
3. IDs 仍使用 MyBatis-Plus `ASSIGN_ID`。

---

## 5. 枚举设计

新增：

```text
IncidentRetrospectiveStatus.java
IncidentRegressionRisk.java
KnowledgeQualityReviewStatus.java
KnowledgeQualityOverallStatus.java
```

### 5.1 IncidentRetrospectiveStatus

```text
DRAFT
REVIEWED
PUBLISHED
ARCHIVED
```

### 5.2 IncidentRegressionRisk

```text
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.3 KnowledgeQualityReviewStatus

```text
PENDING
IN_REVIEW
COMPLETED
```

### 5.4 KnowledgeQualityOverallStatus

```text
APPROVED
NEEDS_WORK
REJECTED
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ToolIncidentRetrospectiveEntity.java
ToolKnowledgeQualityReviewEntity.java

ToolIncidentRetrospectiveMapper.java
ToolKnowledgeQualityReviewMapper.java
```

新增 DTO：

```text
CreateIncidentRetrospectiveRequest.java
UpdateIncidentRetrospectiveRequest.java
IncidentRetrospectiveResponse.java
IncidentRetrospectiveSummaryResponse.java

CreateKnowledgeQualityReviewRequest.java
UpdateKnowledgeQualityReviewRequest.java
KnowledgeQualityReviewResponse.java
KnowledgeQualityStatusSummaryResponse.java

SimilarIncidentRegressionCheckResponse.java
```

### 6.1 CreateIncidentRetrospectiveRequest

```java
public class CreateIncidentRetrospectiveRequest {
    private String title;
    private Boolean includeTimeline;
    private Boolean includeEscalation;
    private Boolean includeOperatorReview;
    private Boolean includeTraceSummary;
}
```

说明：

1. `title` 可选，默认：

```text
Incident Retrospective: {incident.title}
```

2. `include*` 默认全部 `true`。
3. 创建时自动基于 Incident 数据生成草稿字段。

### 6.2 UpdateIncidentRetrospectiveRequest

```java
public class UpdateIncidentRetrospectiveRequest {
    private String summary;
    private String whatHappened;
    private String impactSummary;
    private String responseSummary;
    private String lessonsLearned;
    private String preventionPlan;
    private String actionItems;
    private String regressionRisk;
    private Boolean repeatedIncident;
    private String status;
    private String ownerId;
    private String dueAt;
}
```

状态规则：

1. `DRAFT -> REVIEWED`
2. `REVIEWED -> PUBLISHED`
3. `PUBLISHED -> ARCHIVED`
4. `DRAFT -> ARCHIVED`
5. `REVIEWED -> DRAFT` 允许退回修改
6. `ARCHIVED` 不可恢复

### 6.3 CreateKnowledgeQualityReviewRequest

```java
public class CreateKnowledgeQualityReviewRequest {
    private String knowledgeDocumentId;
    private Integer completenessScore;
    private Integer accuracyScore;
    private Integer actionabilityScore;
    private Integer relevanceScore;
    private String reviewComment;
    private String checklistJson;
}
```

校验：

1. 4 个 score 范围 `0-5`。
2. `knowledgeDocumentId` 必须属于当前 Incident 对应项目。
3. 一个 Incident + Document 默认只允许一个 active review。

---

## 7. 后端服务设计

### 7.1 IncidentRetrospectiveService

新增：

```text
IncidentRetrospectiveService.java
```

职责：

1. 创建 retrospective 草稿。
2. 聚合 Incident / Root Cause / Timeline / Escalation / Review / Trace 数据。
3. 更新 retrospective。
4. 查询单条 retrospective。
5. 查询项目 retrospective 列表。
6. 导出 retrospective Markdown。
7. 执行 Similar Incident Regression Check。

建议方法：

```java
public IncidentRetrospectiveResponse createRetrospective(Long incidentId, CreateIncidentRetrospectiveRequest request)

public IncidentRetrospectiveResponse updateRetrospective(Long retrospectiveId, UpdateIncidentRetrospectiveRequest request)

public IncidentRetrospectiveResponse getIncidentRetrospective(Long incidentId)

public PageResult<IncidentRetrospectiveSummaryResponse> listProjectRetrospectives(Long projectId, String status, PageQuery pageQuery)

public String exportRetrospectiveMarkdown(Long retrospectiveId)

public SimilarIncidentRegressionCheckResponse checkRegression(Long incidentId)
```

### 7.2 Retrospective 草稿拼装规则

草稿内容只能来源于现有事实数据：

```text
Incident 基本信息
+ Root Cause Note
+ Timeline
+ Escalation Event
+ Operator Review
+ Tool Execution Trace
+ Similar Incident Search
```

例如：

```markdown
## What Happened
基于 Incident summary / title / severity / timeline 自动拼装。

## Impact Summary
基于 incident severity / current status / escalation / root cause note impact。

## Response Summary
基于 operator review / timeline / escalation event。

## Lessons Learned
默认占位“请补充”。

## Prevention Plan
优先使用 root cause note prevention。

## Action Items
优先使用 root cause note followUpActions。
```

要求：

1. 不编造根因。
2. 缺失内容统一使用：

```text
待补充。
```

### 7.3 KnowledgeQualityReviewService

新增：

```text
KnowledgeQualityReviewService.java
```

职责：

1. 创建质量评审。
2. 更新评分与状态。
3. 查询 Incident 下所有 quality reviews。
4. 汇总项目级 knowledge quality 状态。

建议方法：

```java
public KnowledgeQualityReviewResponse createReview(Long incidentId, CreateKnowledgeQualityReviewRequest request)

public KnowledgeQualityReviewResponse updateReview(Long reviewId, UpdateKnowledgeQualityReviewRequest request)

public List<KnowledgeQualityReviewResponse> listIncidentReviews(Long incidentId)

public KnowledgeQualityStatusSummaryResponse getProjectQualitySummary(Long projectId)
```

状态建议：

1. `PENDING`：尚未开始
2. `IN_REVIEW`：已填写部分内容
3. `COMPLETED`：评审完成

overall status 建议：

1. 平均分 `>= 4` -> `APPROVED`
2. 平均分 `>= 2 && < 4` -> `NEEDS_WORK`
3. 平均分 `< 2` -> `REJECTED`

### 7.4 Similar Incident Regression Check

复用 37J 的 SimilarIncidentSearchService，新增一个更偏治理的聚合结果：

```java
public SimilarIncidentRegressionCheckResponse checkRegression(Long incidentId)
```

返回：

1. 是否存在高相似 Incident。
2. 相似 Incident 数量。
3. 最高分。
4. 是否疑似“已知问题重复发生”。
5. 推荐的 regression risk 等级。

规则建议：

```text
最高 score >= 0.90 且历史 Incident >= 1 -> repeatedIncident=true
最高 score >= 0.95 且历史已 PUBLISHED RCA -> regressionRisk=HIGH
无相似 -> repeatedIncident=false, regressionRisk=LOW
```

---

## 8. API 设计

### 8.1 Incident Retrospective

```http
POST /api/orchestration/incidents/{incidentId}/retrospective
PUT /api/orchestration/incident-retrospectives/{retrospectiveId}
GET /api/orchestration/incidents/{incidentId}/retrospective
GET /api/projects/{projectId}/incident-retrospectives?status=&page=&size=
GET /api/orchestration/incident-retrospectives/{retrospectiveId}/markdown
GET /api/orchestration/incidents/{incidentId}/regression-check
```

权限：

```text
POST / PUT: MAINTAINER+
GET: VIEWER+
```

### 8.2 Knowledge Quality Review

```http
POST /api/orchestration/incidents/{incidentId}/knowledge-quality-reviews
PUT /api/orchestration/knowledge-quality-reviews/{reviewId}
GET /api/orchestration/incidents/{incidentId}/knowledge-quality-reviews
GET /api/projects/{projectId}/knowledge-quality-summary
```

权限：

```text
POST / PUT: MAINTAINER+
GET: VIEWER+
```

---

## 9. 前端设计

### 9.1 ToolIncidentDetailDrawer 增强

在当前 Incident Detail Drawer 中新增：

```text
Retrospective
Regression Check
Knowledge Quality Reviews
Knowledge Quality Summary
```

推荐 data-testid：

```text
incident-retrospective-section
incident-retrospective-editor
incident-retrospective-save-button
incident-retrospective-status
incident-regression-check
incident-quality-review-list
incident-quality-review-create-button
incident-retrospective-markdown-button
```

### 9.2 IncidentRetrospectiveEditor

新增：

```text
frontend/src/modules/admin/components/IncidentRetrospectiveEditor.vue
```

字段：

```text
Summary
What Happened
Impact Summary
Response Summary
Lessons Learned
Prevention Plan
Action Items
Regression Risk
Repeated Incident
Owner
Due At
Status
```

行为：

1. 支持从 Incident 自动生成草稿。
2. 支持保存为 DRAFT。
3. 支持更新到 REVIEWED / PUBLISHED / ARCHIVED。
4. 支持导出 Markdown。

### 9.3 KnowledgeQualityReviewPanel

新增：

```text
frontend/src/modules/admin/components/KnowledgeQualityReviewPanel.vue
```

功能：

1. 展示 Incident 下所有关联文档的质量评审。
2. 展示 4 个维度评分。
3. 展示 overall status。
4. 支持创建 / 编辑 review。
5. 支持简单 checklist：

```text
是否包含根因
是否包含解决方案
是否包含预防措施
是否可供后续检索复用
```

### 9.4 ObservabilityPage 集成

在 Observability 页面新增：

1. Retrospective 列表面板。
2. Knowledge Quality Summary 卡片。
3. 可按状态筛选 retrospective。

---

## 10. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
```

新增：

```ts
export interface IncidentRetrospective {
  id: string
  projectId: string
  incidentId: string
  rootCauseNoteId?: string
  title: string
  summary?: string
  whatHappened?: string
  impactSummary?: string
  responseSummary?: string
  lessonsLearned?: string
  preventionPlan?: string
  actionItems?: string
  ownerId?: string
  dueAt?: string
  regressionRisk: string
  repeatedIncident: boolean
  status: string
  publishedAt?: string
  createTime: string
  updateTime: string
}

export interface KnowledgeQualityReview {
  id: string
  projectId: string
  incidentId: string
  knowledgeDocumentId: string
  retrospectiveId?: string
  completenessScore: number
  accuracyScore: number
  actionabilityScore: number
  relevanceScore: number
  reviewStatus: string
  overallStatus: string
  checklistJson?: string
  reviewComment?: string
  reviewerId?: string
  reviewedAt?: string
  createTime: string
  updateTime: string
}

export interface SimilarIncidentRegressionCheck {
  currentIncidentId: string
  repeatedIncident: boolean
  regressionRisk: string
  highestScore: number
  similarIncidentCount: number
  topMatches: SimilarIncident[]
}
```

新增 API 函数：

```ts
createIncidentRetrospective(incidentId: string, data)
updateIncidentRetrospective(retrospectiveId: string, data)
getIncidentRetrospective(incidentId: string)
listProjectIncidentRetrospectives(projectId: string, params)
exportIncidentRetrospectiveMarkdown(retrospectiveId: string)
checkIncidentRegression(incidentId: string)

createKnowledgeQualityReview(incidentId: string, data)
updateKnowledgeQualityReview(reviewId: string, data)
listIncidentKnowledgeQualityReviews(incidentId: string)
getProjectKnowledgeQualitySummary(projectId: string)
```

---

## 11. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/IncidentRetrospectiveIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/KnowledgeQualityReviewIntegrationTest.java
```

至少 42 个测试。

### 11.1 Retrospective

1. MAINTAINER 可创建 retrospective。
2. VIEWER 不可创建 retrospective。
3. 非项目成员不可创建 retrospective。
4. 创建后可查询。
5. 自动带出 incident / rca / timeline 摘要。
6. 缺失字段显示“待补充”。
7. 更新 summary / lessons learned / action items 成功。
8. regressionRisk 枚举校验。
9. repeatedIncident 保存成功。
10. 状态 `DRAFT -> REVIEWED` 成功。
11. 状态 `REVIEWED -> PUBLISHED` 成功并设置 publishedAt。
12. `PUBLISHED -> ARCHIVED` 成功。
13. `ARCHIVED` 不可回退。
14. markdown export 包含核心章节。
15. 同一 incident 重复创建 retrospective 返回 CONFLICT 或已有记录。
16. regression check 返回 repeatedIncident=true。
17. regression check 限定同一 project。

### 11.2 Knowledge Quality Review

18. MAINTAINER 可创建 review。
19. VIEWER 不可创建 review。
20. document 不属于 incident.projectId 返回 BAD_REQUEST。
21. score 小于 0 返回 BAD_REQUEST。
22. score 大于 5 返回 BAD_REQUEST。
23. 创建后可查询列表。
24. update review 成功。
25. reviewStatus 可从 PENDING -> IN_REVIEW -> COMPLETED。
26. average score >= 4 -> APPROVED。
27. average score 2-4 -> NEEDS_WORK。
28. average score < 2 -> REJECTED。
29. checklistJson 可保存。
30. reviewComment 可保存。
31. 一个 incident + doc 重复创建 review 返回 CONFLICT 或已有 review。
32. project quality summary 正确聚合。

### 11.3 权限与查询

33. 未登录查询 retrospective 返回 UNAUTHORIZED。
34. 未登录查询 quality summary 返回 UNAUTHORIZED。
35. VIEWER 可查询 retrospective。
36. VIEWER 可查询 review 列表。
37. 非项目成员查询返回 PROJECT_ACCESS_DENIED。
38. list project retrospectives 分页成功。
39. list retrospectives 按 status 过滤成功。
40. summary 只统计当前 project。
41. regression check 无相似结果时 repeatedIncident=false。
42. markdown export 不泄露 secret。

可以超过 42 个。

---

## 12. 前端 E2E 要求

新增或修改：

```text
frontend/e2e/incident-retrospective.spec.ts
frontend/e2e/incident-knowledge.spec.ts
```

至少 8 个 E2E：

1. Incident Detail Drawer 显示 Retrospective section。
2. 创建 retrospective 成功。
3. 编辑 retrospective 成功。
4. regression check 可见。
5. Knowledge Quality Review 列表可见。
6. 创建 review 成功。
7. overall status 正常显示。
8. retrospective markdown 导出 / 复制可用。
9. 页面无 JS error。

---

## 13. 文档与报告

完成后新增：

```text
docs/milestone-37k-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. retrospective 表说明
3. knowledge quality review 表说明
4. IncidentRetrospectiveService 设计说明
5. KnowledgeQualityReviewService 设计说明
6. regression check 设计说明
7. API 清单
8. 前端 retrospective / quality review UI 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 38A

---

## 14. 验收标准

完成后必须满足：

1. Incident 可创建 retrospective。
2. retrospective 可编辑、发布、归档。
3. retrospective 草稿基于现有事实数据自动拼装。
4. 可执行 regression check。
5. Incident 关联文档可创建 quality review。
6. quality review 可保存分数与 checklist。
7. project 维度 quality summary 可查询。
8. Incident Detail Drawer 展示 retrospective / regression / review。
9. 所有 API 有权限校验。
10. 不调用真实 AI。
11. 不执行真实工具。
12. 不写本地文件。
13. 后端测试通过。
14. 前端 typecheck / build / E2E 通过。

---

## 15. 非目标

本阶段不做：

1. 不做真实 AI 自动生成复盘结论。
2. 不做真实向量回归分析。
3. 不做 PDF / Word 导出。
4. 不做外部审批流。
5. 不做邮件 / Slack 通知。
6. 不做跨项目知识质量治理。
7. 不做多租户 taxonomy。
8. 不做复杂 BI dashboard。

这些可以放到后续 Milestone。

---

## 16. 建议后续 Milestone

完成 37K 后，建议进入：

```text
Milestone 38A: External Beta Trial Feedback Loop & Real-world Hardening
```

候选能力：

1. 真实用户试用反馈闭环。
2. 高频 incident / RCA taxonomy dashboard。
3. 真实模型与 GitHub PR Review 的 beta hardening。
4. 生产 smoke test 与 release checklist 自动化增强。

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37K。

文档路径：
docs/milestone-37k-incident-retrospective-knowledge-quality-review.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37H-37J 的 Incident / SLA / Escalation / Timeline / Root Cause / Knowledge Link 基础上，新增 Incident Retrospective Report & Knowledge Quality Review。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 不调用真实模型自动生成 retrospective 内容。
9. retrospective 草稿只能基于现有 Incident / RCA / Timeline / Review / Trace 数据拼装。
10. Knowledge Quality Review 只记录人工评审结果，不自动批准。
11. 不要破坏 36A-37J 已有 API。
12. 不要破坏 RAG / Knowledge Base 现有 API。
13. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
14. IDs 对外保持 String。
15. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V37__init_incident_retrospective_quality_tables.sql。
2. 新增 tool_incident_retrospective / tool_knowledge_quality_review 两张表。
3. 新增 IncidentRetrospectiveStatus / IncidentRegressionRisk / KnowledgeQualityReviewStatus / KnowledgeQualityOverallStatus 枚举。
4. 新增 Entity / Mapper / DTO。
5. 新增 IncidentRetrospectiveService。
6. 新增 KnowledgeQualityReviewService。
7. 新增 regression check 聚合能力。
8. 新增 API：
   - POST /api/orchestration/incidents/{incidentId}/retrospective
   - PUT /api/orchestration/incident-retrospectives/{retrospectiveId}
   - GET /api/orchestration/incidents/{incidentId}/retrospective
   - GET /api/projects/{projectId}/incident-retrospectives
   - GET /api/orchestration/incident-retrospectives/{retrospectiveId}/markdown
   - GET /api/orchestration/incidents/{incidentId}/regression-check
   - POST /api/orchestration/incidents/{incidentId}/knowledge-quality-reviews
   - PUT /api/orchestration/knowledge-quality-reviews/{reviewId}
   - GET /api/orchestration/incidents/{incidentId}/knowledge-quality-reviews
   - GET /api/projects/{projectId}/knowledge-quality-summary
9. 前端增强 Incident Detail Drawer，展示 Retrospective / Regression Check / Knowledge Quality Reviews。
10. 新增 IncidentRetrospectiveEditor.vue。
11. 新增 KnowledgeQualityReviewPanel.vue。
12. 在 ObservabilityPage 集成 retrospective 列表和 quality summary。
13. 后端测试不少于 42 个。
14. 前端 E2E 不少于 8 个。
15. 新增 docs/milestone-37k-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. retrospective 表说明
3. knowledge quality review 表说明
4. IncidentRetrospectiveService 设计说明
5. KnowledgeQualityReviewService 设计说明
6. regression check 设计说明
7. API 清单
8. 前端 retrospective / quality review UI 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 38A

现在开始实现，不要只给计划。
```
