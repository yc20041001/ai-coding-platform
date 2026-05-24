# Milestone 37J: Incident Knowledge Base & Root Cause Notes

## 1. 背景

Milestone 37H / 37I 已经把工具执行运维链路推进到 Incident、Alert Routing、SLA、Escalation 和 Timeline：

- 37H: Tool Execution Incident Workflow & Alert Routing
- 37I: Incident SLA / Escalation Mock / Review Timeline

当前系统已经具备：

```text
Tool Failure / Warning
  -> Incident
  -> SLA
  -> Escalation Mock
  -> Alert Delivery
  -> Timeline
  -> Operator Review
  -> Audit Export
```

但这些处理记录仍然偏“单次事件”。如果同类问题反复出现，团队还需要把处理经验沉淀成可复用知识：

```text
这类问题的根因是什么？
上次怎么处理的？
有哪些预防措施？
有没有类似 Incident？
能不能把复盘内容沉淀到 Knowledge Base，让后续 Agent / Chat / 运维人员检索？
```

Milestone 37J 的目标是新增：

```text
Incident Knowledge Base & Root Cause Notes
```

让 Incident 从：

```text
可处理、可升级、可审计
```

升级为：

```text
可复盘、可沉淀、可检索、可复用
```

---

## 2. 总目标

实现 Incident 知识沉淀与根因记录能力：

1. 新增 Incident Root Cause Note 数据模型。
2. 新增 Known Issue 模板数据模型。
3. 支持 Incident 记录根因、影响、处置、预防措施。
4. 支持从 Incident 生成 Knowledge Document 草稿。
5. 支持 Incident 与 Knowledge Base / Knowledge Document 关联。
6. 支持相似 Incident 搜索（基于 title / summary / root cause / resolution 的 MySQL LIKE Mock 搜索）。
7. 支持从 Known Issue 模板快速填充 Root Cause Note。
8. Incident Detail Drawer 展示 Root Cause / Similar Incidents / Linked Knowledge。
9. 支持将 Root Cause Note 导出为 Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
Incident 处理完就结束
```

升级为：

```text
Incident 处理经验可以进入团队知识库
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch`。
4. 不执行 `git reset` / `git apply`。
5. 不执行 `git add` / `git commit` / `git push`。
6. 不写真实代码文件。
7. 不修改 workspace 文件。
8. 不发送真实 Slack / PagerDuty / Email。
9. 不调用真实模型生成 RCA。
10. Root Cause Note 由用户手动填写或模板填充，不自动编造根因。
11. Knowledge Document 草稿只通过现有 RAG 文档 API / service 落库，不写本地文件。
12. Similar Incident Search 使用 Mock LIKE 检索，不接真实向量库。
13. 不破坏 36A-37I 已有 API。
14. 不破坏 Knowledge Base / RAG 现有 API。
15. 前端保持中文暗色科技风 UI。

允许做：

1. 新增 Incident Note / Known Issue 表。
2. 新增只读搜索 API。
3. 新增 Knowledge Document 关联字段。
4. 调用现有 KnowledgeDocumentApplicationService 创建文档。
5. 前端展示与编辑 Root Cause Note。
6. 前端展示相似 Incident。
7. 前端展示关联知识文档。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V36__init_incident_knowledge_tables.sql
```

### 4.1 tool_incident_root_cause_note

```sql
CREATE TABLE tool_incident_root_cause_note (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause TEXT NULL,
    impact TEXT NULL,
    resolution TEXT NULL,
    prevention TEXT NULL,
    follow_up_actions TEXT NULL,
    tags VARCHAR(512) NULL,
    confidence VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    author_id BIGINT NOT NULL,
    last_editor_id BIGINT NULL,
    published_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_incident_rca_project_time(project_id, create_time),
    KEY idx_incident_rca_incident(incident_id),
    KEY idx_incident_rca_status(status),
    KEY idx_incident_rca_author(author_id)
);
```

### 4.2 tool_known_issue_template

```sql
CREATE TABLE tool_known_issue_template (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    root_cause_template TEXT NULL,
    impact_template TEXT NULL,
    resolution_template TEXT NULL,
    prevention_template TEXT NULL,
    tags VARCHAR(512) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_known_issue_project(project_id),
    KEY idx_known_issue_category(category),
    KEY idx_known_issue_enabled(enabled)
);
```

### 4.3 tool_incident_knowledge_link

```sql
CREATE TABLE tool_incident_knowledge_link (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause_note_id BIGINT NULL,
    knowledge_base_id BIGINT NULL,
    knowledge_document_id BIGINT NULL,
    link_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_incident_knowledge_project(project_id),
    KEY idx_incident_knowledge_incident(incident_id),
    KEY idx_incident_knowledge_document(knowledge_document_id)
);
```

无物理外键，保持项目风格。

---

## 5. 枚举设计

新增：

```text
IncidentRootCauseNoteStatus.java
IncidentRootCauseConfidence.java
KnownIssueCategory.java
IncidentKnowledgeLinkType.java
```

### 5.1 IncidentRootCauseNoteStatus

```text
DRAFT
REVIEWED
PUBLISHED
ARCHIVED
```

### 5.2 IncidentRootCauseConfidence

```text
LOW
MEDIUM
HIGH
CONFIRMED
```

### 5.3 KnownIssueCategory

```text
TOOL_POLICY
READ_ONLY_ADAPTER
CODE_INDEX
WORKER_QUEUE
RABBITMQ
REDIS
MODEL_GATEWAY
GITHUB
RAG
FRONTEND
CONFIGURATION
UNKNOWN
```

### 5.4 IncidentKnowledgeLinkType

```text
GENERATED_FROM_INCIDENT
RELATED_DOCUMENT
MANUAL_LINK
KNOWN_ISSUE_TEMPLATE
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ToolIncidentRootCauseNoteEntity.java
ToolKnownIssueTemplateEntity.java
ToolIncidentKnowledgeLinkEntity.java

ToolIncidentRootCauseNoteMapper.java
ToolKnownIssueTemplateMapper.java
ToolIncidentKnowledgeLinkMapper.java
```

新增 DTO：

```text
CreateIncidentRootCauseNoteRequest.java
UpdateIncidentRootCauseNoteRequest.java
IncidentRootCauseNoteResponse.java

CreateKnownIssueTemplateRequest.java
UpdateKnownIssueTemplateRequest.java
KnownIssueTemplateResponse.java

IncidentKnowledgeLinkResponse.java
GenerateIncidentKnowledgeDocumentRequest.java
IncidentKnowledgeDocumentDraftResponse.java

SimilarIncidentSearchRequest.java
SimilarIncidentResponse.java
IncidentKnowledgeSummaryResponse.java
```

### 6.1 CreateIncidentRootCauseNoteRequest

```java
public class CreateIncidentRootCauseNoteRequest {
    private String rootCause;
    private String impact;
    private String resolution;
    private String prevention;
    private String followUpActions;
    private String tags;
    private String confidence;
}
```

校验：

- `confidence` 可选，默认 `MEDIUM`。
- 文本字段单项最长 8000。
- `tags` 最长 512。
- 一个 Incident 默认只允许一个 active root cause note；重复创建时返回已有 note 或抛 `CONFLICT`，按项目现有风格选择。

### 6.2 UpdateIncidentRootCauseNoteRequest

```java
public class UpdateIncidentRootCauseNoteRequest {
    private String rootCause;
    private String impact;
    private String resolution;
    private String prevention;
    private String followUpActions;
    private String tags;
    private String confidence;
    private String status;
}
```

状态规则：

- `DRAFT -> REVIEWED`
- `REVIEWED -> PUBLISHED`
- `PUBLISHED -> ARCHIVED`
- `DRAFT -> ARCHIVED`
- `REVIEWED -> DRAFT` 允许，用于退回修改。
- `ARCHIVED` 不可再改为其他状态。

`status=PUBLISHED` 时设置 `publishedAt`。

### 6.3 GenerateIncidentKnowledgeDocumentRequest

```java
public class GenerateIncidentKnowledgeDocumentRequest {
    private String knowledgeBaseId;
    private String title;
    private Boolean includeTimeline;
    private Boolean includeTraceSummary;
    private Boolean includeOperatorReview;
    private Boolean includeEscalation;
}
```

说明：

- `knowledgeBaseId` 必填。
- knowledge base 必须属于 Incident 所在 project。
- 默认全部 include 为 true。

---

## 7. 后端服务设计

### 7.1 IncidentRootCauseService

新增：

```text
IncidentRootCauseService.java
```

职责：

1. 创建 Root Cause Note。
2. 更新 Root Cause Note。
3. 查询 Incident 的 Root Cause Note。
4. 按项目查询 notes。
5. 应用 Known Issue Template。
6. 导出 Root Cause Note Markdown。
7. 维护状态流转。

建议方法：

```java
public IncidentRootCauseNoteResponse createNote(Long incidentId, CreateIncidentRootCauseNoteRequest request)

public IncidentRootCauseNoteResponse updateNote(Long noteId, UpdateIncidentRootCauseNoteRequest request)

public IncidentRootCauseNoteResponse getIncidentNote(Long incidentId)

public PageResult<IncidentRootCauseNoteResponse> listProjectNotes(Long projectId, String status, PageQuery pageQuery)

public IncidentRootCauseNoteResponse applyTemplate(Long incidentId, Long templateId)

public String exportNoteMarkdown(Long noteId)
```

权限：

- 创建 / 更新：项目 MAINTAINER+。
- 查询 / 导出：项目 VIEWER+。
- 发布：项目 MAINTAINER+。

### 7.2 KnownIssueTemplateService

新增：

```text
KnownIssueTemplateService.java
```

职责：

1. 创建 template。
2. 更新 template。
3. 启用 / 禁用 template。
4. 查询项目 templates。
5. 查询全局 builtin templates。
6. 根据 category / severity 筛选。

权限：

- 项目模板创建/更新：项目 MAINTAINER+。
- 全局模板：ADMIN，本阶段可不实现全局写入，只读 seed/builtin。
- 查询：项目 VIEWER+。

### 7.3 IncidentKnowledgeService

新增：

```text
IncidentKnowledgeService.java
```

职责：

1. 生成 Knowledge Document Markdown 内容。
2. 调用现有 KnowledgeDocumentApplicationService 创建文档。
3. 创建 incident knowledge link。
4. 查询 Incident 的 knowledge links。
5. 删除 link（不删除知识库文档）。

建议方法：

```java
public IncidentKnowledgeDocumentDraftResponse generateKnowledgeDocument(Long incidentId, GenerateIncidentKnowledgeDocumentRequest request)

public List<IncidentKnowledgeLinkResponse> listIncidentKnowledgeLinks(Long incidentId)

public void deleteKnowledgeLink(Long linkId)
```

生成 Markdown 内容建议：

```markdown
# Incident Knowledge: {title}

## Summary
- Incident:
- Severity:
- Status:
- SLA:
- Created:
- Resolved:

## Root Cause

## Impact

## Resolution

## Prevention

## Follow-up Actions

## Timeline Summary

## Trace / Tool Evidence Summary

## Operator Review

## Escalation Summary
```

要求：

- 不调用真实 AI。
- 不编造内容。
- 没有 note 字段时使用占位：

```text
未填写。
```

### 7.4 SimilarIncidentSearchService

新增：

```text
SimilarIncidentSearchService.java
```

职责：

1. 根据 query 搜索 Incident title / summary / resolution。
2. 搜索 Root Cause Note rootCause / impact / resolution / prevention / tags。
3. 只限定当前 projectId。
4. 排除当前 incidentId。
5. 返回 mock score。

建议方法：

```java
public List<SimilarIncidentResponse> searchSimilar(Long incidentId, String query, Integer limit)

public List<SimilarIncidentResponse> searchByIncident(Long incidentId, Integer limit)
```

Scoring：

```text
title contains query: 0.95
rootCause contains query: 0.90
resolution contains query: 0.85
summary contains query: 0.75
tags contains query: 0.70
default: 0.50
```

---

## 8. API 设计

当前 37H/37I 使用的 Incident API 风格为：

```text
/api/orchestration/incidents/...
/api/projects/{projectId}/incidents/...
```

37J 延续这个风格。

### 8.1 Root Cause Note

```http
POST /api/orchestration/incidents/{incidentId}/root-cause-note
PUT /api/orchestration/incident-root-cause-notes/{noteId}
GET /api/orchestration/incidents/{incidentId}/root-cause-note
GET /api/projects/{projectId}/incident-root-cause-notes?status=&page=&size=
GET /api/orchestration/incident-root-cause-notes/{noteId}/markdown
```

权限：

```text
POST / PUT: MAINTAINER+
GET: VIEWER+
```

### 8.2 Known Issue Templates

```http
POST /api/projects/{projectId}/known-issue-templates
PUT /api/orchestration/known-issue-templates/{templateId}
GET /api/projects/{projectId}/known-issue-templates?category=&enabled=
POST /api/orchestration/incidents/{incidentId}/apply-known-issue-template/{templateId}
```

权限：

```text
POST / PUT / apply: MAINTAINER+
GET: VIEWER+
```

### 8.3 Incident Knowledge Links

```http
POST /api/orchestration/incidents/{incidentId}/knowledge-document
GET /api/orchestration/incidents/{incidentId}/knowledge-links
DELETE /api/orchestration/incident-knowledge-links/{linkId}
```

权限：

```text
POST / DELETE: MAINTAINER+
GET: VIEWER+
```

### 8.4 Similar Incident Search

```http
GET /api/orchestration/incidents/{incidentId}/similar?query=&limit=
```

权限：

```text
项目 VIEWER+
```

如果 query 为空：

- 使用 incident title + summary + root cause note tags 拼接作为查询词。

---

## 9. 与 RAG Knowledge Base 集成

复用现有 RAG 模块：

```text
KnowledgeBaseApplicationService
KnowledgeDocumentApplicationService
RagSearchApplicationService
```

最低要求：

1. `GenerateIncidentKnowledgeDocumentRequest.knowledgeBaseId` 必须属于 incident.projectId。
2. 调用现有 document upload / manual document 创建逻辑。
3. 文档类型建议使用 `MARKDOWN`。
4. sourceType 建议使用 `MANUAL`。
5. title 默认：

```text
Incident {incidentId}: {incident.title}
```

6. 创建成功后写入 `tool_incident_knowledge_link`。
7. 不要求真实 embedding；沿用现有 Mock Embedding。

如果现有 KnowledgeDocumentApplicationService 只支持上传请求 DTO，则新增内部方法：

```java
public KnowledgeDocumentResponse createManualDocument(Long projectId, Long knowledgeBaseId, String title, String content, String filePath)
```

要求：

- 不破坏现有上传 API。
- 不写本地文件。

---

## 10. 前端设计

### 10.1 ToolIncidentDetailDrawer 增强

修改或新增：

```text
frontend/src/modules/admin/components/ToolIncidentDetailDrawer.vue
```

在现有 Incident Detail / Timeline / Escalation 基础上增加：

```text
Root Cause
Impact
Resolution
Prevention
Follow-up Actions
Known Issue Template
Similar Incidents
Knowledge Links
Generate Knowledge Document
```

推荐 data-testid：

```text
incident-root-cause-section
incident-root-cause-editor
incident-root-cause-save-button
incident-root-cause-status
known-issue-template-select
known-issue-template-apply-button
similar-incident-list
similar-incident-item
incident-knowledge-links
incident-generate-knowledge-button
incident-root-cause-markdown-button
```

### 10.2 IncidentRootCauseEditor

新增：

```text
frontend/src/modules/admin/components/IncidentRootCauseEditor.vue
```

字段：

```text
Root Cause
Impact
Resolution
Prevention
Follow-up Actions
Tags
Confidence
Status
```

行为：

- 如果 note 不存在，展示“创建复盘记录”。
- 如果 note 存在，展示编辑表单。
- 支持保存为 DRAFT。
- 支持更新状态为 REVIEWED / PUBLISHED / ARCHIVED。
- PUBLISHED 后仍可查看，但如果后端允许编辑，需要清晰显示状态。

### 10.3 KnownIssueTemplatePanel

新增：

```text
frontend/src/modules/admin/components/KnownIssueTemplatePanel.vue
```

功能：

1. 展示项目模板。
2. 创建模板。
3. 启用 / 禁用模板。
4. 按 category 过滤。

可集成到 ObservabilityPage 或 Incident Detail Drawer 下方。

### 10.4 SimilarIncidentList

新增：

```text
frontend/src/modules/admin/components/SimilarIncidentList.vue
```

功能：

1. 展示相似 Incident。
2. 显示 score。
3. 显示 matchedField。
4. 点击打开该 Incident Detail。

### 10.5 Knowledge Link UI

在 Incident Detail Drawer 增加：

1. Knowledge Base 下拉。
2. “生成知识文档”按钮。
3. 已关联文档列表。
4. 点击文档可跳转 Knowledge 页面或显示文档 ID。

如果 Knowledge Base 列表为空：

- 显示 EmptyState：“当前项目暂无知识库，请先创建知识库。”

---

## 11. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
frontend/src/modules/knowledge/api.ts
```

新增：

```ts
export interface IncidentRootCauseNote {
  id: string
  projectId: string
  incidentId: string
  rootCause?: string
  impact?: string
  resolution?: string
  prevention?: string
  followUpActions?: string
  tags?: string
  confidence: string
  status: string
  authorId: string
  lastEditorId?: string
  publishedAt?: string
  createTime: string
  updateTime: string
}

export interface KnownIssueTemplate {
  id: string
  projectId?: string
  title: string
  category: string
  severity: string
  rootCauseTemplate?: string
  impactTemplate?: string
  resolutionTemplate?: string
  preventionTemplate?: string
  tags?: string
  enabled: boolean
}

export interface IncidentKnowledgeLink {
  id: string
  projectId: string
  incidentId: string
  rootCauseNoteId?: string
  knowledgeBaseId?: string
  knowledgeDocumentId?: string
  linkType: string
  title: string
  createTime: string
}

export interface SimilarIncident {
  incidentId: string
  title: string
  status: string
  severity: string
  score: number
  matchedField: string
  snippet?: string
  createTime?: string
}
```

新增 API 函数：

```ts
createIncidentRootCauseNote(incidentId: string, data)
updateIncidentRootCauseNote(noteId: string, data)
getIncidentRootCauseNote(incidentId: string)
listProjectIncidentRootCauseNotes(projectId: string, params)
exportIncidentRootCauseMarkdown(noteId: string)

createKnownIssueTemplate(projectId: string, data)
updateKnownIssueTemplate(templateId: string, data)
listProjectKnownIssueTemplates(projectId: string, params)
applyKnownIssueTemplate(incidentId: string, templateId: string)

generateIncidentKnowledgeDocument(incidentId: string, data)
listIncidentKnowledgeLinks(incidentId: string)
deleteIncidentKnowledgeLink(linkId: string)

searchSimilarIncidents(incidentId: string, query?: string, limit?: number)
```

---

## 12. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/IncidentRootCauseNoteIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/IncidentKnowledgeIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/SimilarIncidentSearchIntegrationTest.java
```

至少 44 个测试。

### 12.1 Root Cause Note

1. MAINTAINER 可创建 note。
2. VIEWER 不可创建 note。
3. 非项目成员不可创建 note。
4. 创建 note 后可查询。
5. 更新 rootCause / impact / resolution / prevention 成功。
6. tags 保存成功。
7. confidence 枚举校验。
8. status DRAFT -> REVIEWED 成功。
9. status REVIEWED -> PUBLISHED 成功并设置 publishedAt。
10. status PUBLISHED -> ARCHIVED 成功。
11. ARCHIVED 不可回退。
12. 同一 incident 重复创建 note 返回 CONFLICT 或已有 note。
13. markdown export 包含 Root Cause / Impact / Resolution。
14. markdown export 不泄露 secret。

### 12.2 Known Issue Template

15. MAINTAINER 可创建 template。
16. VIEWER 不可创建 template。
17. list templates 成功。
18. category filter 生效。
19. enabled filter 生效。
20. update template 成功。
21. disabled template 不可 apply 或 apply 返回 BAD_REQUEST。
22. apply template 填充 note 字段。
23. apply template 创建 knowledge link 或记录 template usage。

### 12.3 Knowledge Document

24. generate knowledge document 成功。
25. knowledgeBaseId 不属于项目返回 BAD_REQUEST。
26. incident 不存在返回 NOT_FOUND。
27. note 不存在时仍可生成文档，内容使用“未填写”。
28. 生成后创建 incident knowledge link。
29. list links 返回生成的 link。
30. delete link 不删除 knowledge document。
31. 生成文档类型为 MARKDOWN。
32. 生成文档包含 Timeline Summary。
33. 生成文档包含 Trace Summary。

### 12.4 Similar Incident Search

34. query 命中 title 返回 score 0.95。
35. query 命中 rootCause 返回 score 0.90。
36. query 命中 resolution 返回 score 0.85。
37. query 命中 summary 返回 score 0.75。
38. query 命中 tags 返回 score 0.70。
39. 不返回当前 incident 自身。
40. 限定同一 project。
41. limit 参数生效。
42. 空 query 使用 incident 自动 query。
43. 无结果返回空数组。

### 12.5 权限

44. 未登录查询 note 返回 UNAUTHORIZED。
45. 非项目成员查询 similar 返回 PROJECT_ACCESS_DENIED。
46. VIEWER 可查询 similar。
47. VIEWER 可查询 knowledge links。

可以超过 44 个。

---

## 13. 前端 E2E 要求

新增或修改：

```text
frontend/e2e/incident-sla-escalation.spec.ts
```

也可新增：

```text
frontend/e2e/incident-knowledge-root-cause.spec.ts
```

至少 8 个 E2E：

1. Incident Detail Drawer 显示 Root Cause section。
2. 创建 Root Cause Note 成功。
3. 编辑 Root Cause Note 成功。
4. 应用 Known Issue Template 成功。
5. Similar Incident list 可见。
6. Generate Knowledge Document 按钮可用。
7. Knowledge Links 展示生成结果。
8. Root Cause Markdown 导出 / 复制可用。
9. 页面无 JS error。

推荐 data-testid：

```text
incident-root-cause-section
incident-root-cause-editor
incident-root-cause-save-button
known-issue-template-select
known-issue-template-apply-button
similar-incident-list
incident-generate-knowledge-button
incident-knowledge-links
incident-root-cause-markdown-button
```

---

## 14. 文档与报告

完成后新增：

```text
docs/milestone-37j-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Root Cause Note 表说明
3. Known Issue Template 表说明
4. Incident Knowledge Link 表说明
5. Root Cause Service 设计说明
6. Known Issue Template Service 设计说明
7. Incident Knowledge Service 设计说明
8. Similar Incident Search 设计说明
9. API 清单
10. 前端 Root Cause / Similar / Knowledge Link UI 说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37K

---

## 15. 验收标准

完成后必须满足：

1. Incident 可创建 Root Cause Note。
2. Root Cause Note 可编辑、发布、归档。
3. Known Issue Template 可创建、应用。
4. Incident 可生成 Knowledge Document。
5. Knowledge Document 生成后有 link。
6. Similar Incident Search 可用。
7. Incident Detail Drawer 展示 Root Cause / Similar / Knowledge Links。
8. 所有 API 有权限校验。
9. 不调用真实 AI。
10. 不执行真实工具。
11. 不写本地文件。
12. 后端测试通过。
13. 前端 typecheck / build / E2E 通过。

---

## 16. 非目标

本阶段不做：

1. 不做真实 LLM 自动生成 RCA。
2. 不做真实向量相似搜索。
3. 不做复杂知识图谱。
4. 不做 Incident 评论线程。
5. 不做附件上传。
6. 不做 PDF 导出。
7. 不做跨项目相似搜索。
8. 不做多租户 Knowledge Governance。
9. 不做自动创建 RAG 文档审核流程。
10. 不做真实外部通知。

这些可以放到后续 Milestone。

---

## 17. 建议后续 Milestone

完成 37J 后，建议进入：

```text
Milestone 37K: Incident Retrospective Report & Knowledge Quality Review
```

候选能力：

- Incident retrospective report。
- RCA review checklist。
- Knowledge document quality status。
- Similar incident regression check。
- Root cause taxonomy dashboard。

也可以进入：

```text
Milestone 38A: Semantic Code Search / RAG Evaluation
```

---

## 18. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37J。

文档路径：
docs/milestone-37j-incident-knowledge-root-cause-notes.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37H/37I 的 Incident / SLA / Escalation / Timeline 基础上，新增 Incident Knowledge Base & Root Cause Notes。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 不调用真实模型生成 RCA。
9. Root Cause Note 由用户手动填写或模板填充，不自动编造根因。
10. Knowledge Document 草稿只通过现有 RAG service 落库，不写本地文件。
11. Similar Incident Search 使用 MySQL LIKE Mock 检索，不接真实向量库。
12. 不要破坏 36A-37I 已有 API。
13. 不要破坏 Knowledge Base / RAG 现有 API。
14. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
15. IDs 对外保持 String。
16. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V36__init_incident_knowledge_tables.sql。
2. 新增 tool_incident_root_cause_note / tool_known_issue_template / tool_incident_knowledge_link 三张表。
3. 新增 IncidentRootCauseNoteStatus / IncidentRootCauseConfidence / KnownIssueCategory / IncidentKnowledgeLinkType 枚举。
4. 新增 Entity / Mapper / DTO。
5. 新增 IncidentRootCauseService。
6. 新增 KnownIssueTemplateService。
7. 新增 IncidentKnowledgeService。
8. 新增 SimilarIncidentSearchService。
9. 新增 API：
   - POST /api/orchestration/incidents/{incidentId}/root-cause-note
   - PUT /api/orchestration/incident-root-cause-notes/{noteId}
   - GET /api/orchestration/incidents/{incidentId}/root-cause-note
   - GET /api/projects/{projectId}/incident-root-cause-notes
   - GET /api/orchestration/incident-root-cause-notes/{noteId}/markdown
   - POST /api/projects/{projectId}/known-issue-templates
   - PUT /api/orchestration/known-issue-templates/{templateId}
   - GET /api/projects/{projectId}/known-issue-templates
   - POST /api/orchestration/incidents/{incidentId}/apply-known-issue-template/{templateId}
   - POST /api/orchestration/incidents/{incidentId}/knowledge-document
   - GET /api/orchestration/incidents/{incidentId}/knowledge-links
   - DELETE /api/orchestration/incident-knowledge-links/{linkId}
   - GET /api/orchestration/incidents/{incidentId}/similar
10. 如需要，给 KnowledgeDocumentApplicationService 增加内部 createManualDocument 方法，但不要破坏现有上传 API。
11. 前端增强 Incident Detail Drawer，展示 Root Cause / Similar Incidents / Knowledge Links。
12. 新增 IncidentRootCauseEditor.vue。
13. 新增 KnownIssueTemplatePanel.vue。
14. 新增 SimilarIncidentList.vue。
15. 后端测试不少于 44 个。
16. 前端 E2E 不少于 8 个。
17. 新增 docs/milestone-37j-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Root Cause Note 表说明
3. Known Issue Template 表说明
4. Incident Knowledge Link 表说明
5. Root Cause Service 设计说明
6. Known Issue Template Service 设计说明
7. Incident Knowledge Service 设计说明
8. Similar Incident Search 设计说明
9. API 清单
10. 前端 Root Cause / Similar / Knowledge Link UI 说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37K

现在开始实现，不要只给计划。
```
