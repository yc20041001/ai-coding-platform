# Milestone 37G: Tool Execution Audit Export & Operator Review

## 1. 背景

Milestone 37F 已经完成 **Tool Execution Trace & Evidence Viewer**，可以把一次工具执行的策略判定、审批、Job、Worker、读取文件、跳过原因、脱敏/截断、Artifact、Patch Review 串成完整证据链。

当前能力已经足够支持页面内查看：

```text
Tool Execution
  -> Trace Drawer
  -> Timeline
  -> Evidence
  -> Payload
  -> Artifact / Patch Review
```

但进入真实试用、内部安全审查和运维排障时，还需要两个能力：

1. **导出证据**：把一次 execution 或一个 run 的证据链导出成 Markdown 文档，方便复制到 Issue、PR、审计记录或事故报告。
2. **操作员审查**：对 failed / retry / dead-letter / suspicious trace 进行人工处理，记录处理状态、备注、负责人和结论。

Milestone 37G 的目标是新增 **Tool Execution Audit Export & Operator Review**：

```text
Trace Viewer 负责看清发生了什么；
Audit Export 负责带走证据；
Operator Review 负责记录人如何处理问题。
```

---

## 2. 总目标

实现工具执行审计导出与操作员审查闭环：

1. 新增 ToolExecutionAuditExportService。
2. 支持导出单次 Tool Execution Trace 为 Markdown。
3. 支持导出 Multi-Agent Run 级 evidence report 为 Markdown。
4. 新增 Operator Review 数据模型。
5. 支持对 Tool Execution / Job / Run 创建审查记录。
6. 支持更新审查状态、备注、结论。
7. Observability Problem Jobs 增加“创建审查 / 查看审查”入口。
8. Trace Drawer 增加“导出 Markdown / 创建审查”操作。
9. 所有导出内容必须脱敏，不允许泄露 API Key / Token / Secret。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
证据链可查看
```

升级为：

```text
证据链可导出、问题可审查、处理过程可追踪
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
8. 不创建真实 patch 文件。
9. 导出只读取数据库已有 trace 数据，不重新执行工具。
10. 导出不重新读取仓库文件。
11. 导出内容必须复用 37F 的 payload sanitizer。
12. Operator Review 只记录人工处理信息，不改变工具执行结果。
13. Operator Review 不自动 retry / cancel / approve。
14. 不破坏 36A-37F 已有 API。
15. 前端保持中文暗色科技风 UI。

允许做：

1. 新增审查表。
2. 新增只读导出 API。
3. 新增审查 CRUD API。
4. 前端下载 Markdown 文件。
5. 前端复制 Markdown 到剪贴板。
6. 前端显示审查状态。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V33__init_tool_operator_review.sql
```

新增表：

```sql
CREATE TABLE tool_operator_review (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    tool_execution_id BIGINT NULL,
    tool_job_id BIGINT NULL,
    review_target_type VARCHAR(32) NOT NULL,
    review_target_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    resolution TEXT NULL,
    assignee_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    resolved_by BIGINT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    KEY idx_tool_operator_review_project_time(project_id, create_time),
    KEY idx_tool_operator_review_target(review_target_type, review_target_id),
    KEY idx_tool_operator_review_execution(tool_execution_id),
    KEY idx_tool_operator_review_job(tool_job_id),
    KEY idx_tool_operator_review_status(status),
    KEY idx_tool_operator_review_assignee(assignee_id)
);
```

无物理外键，保持项目现有风格。

---

## 5. 枚举设计

新增：

```text
ToolOperatorReviewStatus.java
ToolOperatorReviewSeverity.java
ToolOperatorReviewTargetType.java
```

### 5.1 ToolOperatorReviewStatus

```text
OPEN
IN_PROGRESS
RESOLVED
WONT_FIX
FALSE_POSITIVE
```

### 5.2 ToolOperatorReviewSeverity

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.3 ToolOperatorReviewTargetType

```text
TOOL_EXECUTION
TOOL_JOB
MULTI_AGENT_RUN
TASK
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolOperatorReviewEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolOperatorReviewMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateToolOperatorReviewRequest.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateToolOperatorReviewRequest.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolOperatorReviewResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolAuditExportResponse.java
```

### 6.1 CreateToolOperatorReviewRequest

```java
public class CreateToolOperatorReviewRequest {
    private String reviewTargetType;
    private String reviewTargetId;
    private String severity;
    private String title;
    private String summary;
    private String assigneeId;
}
```

校验：

- `reviewTargetType` 必填。
- `reviewTargetId` 必填。
- `severity` 必填。
- `title` 必填，最长 255。
- `summary` 最长 4000。
- target 必须存在，且必须能解析 projectId。

### 6.2 UpdateToolOperatorReviewRequest

```java
public class UpdateToolOperatorReviewRequest {
    private String status;
    private String severity;
    private String title;
    private String summary;
    private String resolution;
    private String assigneeId;
}
```

校验：

- `status` 可选，但必须是枚举值。
- `resolution` 最长 4000。
- 当 status 变为 `RESOLVED` / `WONT_FIX` / `FALSE_POSITIVE` 时，自动设置 `resolvedBy` / `resolvedAt`。
- 当 status 从终态改回 `OPEN` / `IN_PROGRESS` 时，清空 `resolvedBy` / `resolvedAt`。

### 6.3 ToolAuditExportResponse

```java
public class ToolAuditExportResponse {
    private String targetType;
    private String targetId;
    private String fileName;
    private String contentType;
    private String markdown;
    private Integer traceCount;
    private Boolean redacted;
    private Boolean truncated;
    private LocalDateTime generatedAt;
}
```

---

## 7. 后端服务设计

### 7.1 ToolExecutionAuditExportService

新增：

```text
ToolExecutionAuditExportService.java
```

职责：

1. 复用 `ToolExecutionTraceService` 获取 trace。
2. 生成单 execution Markdown。
3. 生成 run-level Markdown。
4. 生成 task-level Markdown。
5. 对 Markdown 做最终 secret mask。
6. 返回 ToolAuditExportResponse。

建议方法：

```java
public ToolAuditExportResponse exportExecutionTrace(Long executionId)

public ToolAuditExportResponse exportRunEvidence(Long runId)

public ToolAuditExportResponse exportTaskEvidence(Long taskId)
```

Markdown 必须包含：

```text
# Tool Execution Audit Report

## Summary
- Target:
- Project:
- Task:
- Run:
- Generated At:
- Trace Count:

## Tool Execution
- Tool:
- Status:
- Risk:
- Read-only:
- Policy:

## Timeline
| Time | Event | Status | Description |

## Evidence
### Files Read
### Skipped Files
### Redaction / Truncation
### Read-only Contract

## Approval

## Job

## Artifact / Patch Review

## Sanitized Payload

## Notes
- This report is generated from stored trace data.
- No tool was re-executed.
- No repository files were re-read.
```

Run-level report 必须按 trace 分组：

```text
# Multi-Agent Run Tool Evidence Report

## Run Summary

## Trace 1: READ_FILE_SNIPPET

## Trace 2: MOCK_PATCH_PROPOSAL
```

### 7.2 ToolOperatorReviewService

新增：

```text
ToolOperatorReviewService.java
```

职责：

1. 创建 review。
2. 更新 review。
3. 查询单条 review。
4. 按 project 查询 review 列表。
5. 按 target 查询 review 列表。
6. 解析 target projectId 并做权限校验。

建议方法：

```java
public ToolOperatorReviewResponse createReview(CreateToolOperatorReviewRequest request)

public ToolOperatorReviewResponse updateReview(Long reviewId, UpdateToolOperatorReviewRequest request)

public ToolOperatorReviewResponse getReview(Long reviewId)

public PageResult<ToolOperatorReviewResponse> listProjectReviews(Long projectId, String status, String severity, PageQuery pageQuery)

public List<ToolOperatorReviewResponse> listTargetReviews(String targetType, Long targetId)
```

权限：

- 创建 review：项目 MAINTAINER+。
- 更新 review：项目 MAINTAINER+。
- 查询 review：项目 VIEWER+。
- 导出 Markdown：项目 VIEWER+。

---

## 8. 后端 API

### 8.1 导出单次 Execution Trace

```http
GET /api/tool-sandbox-executions/{executionId}/audit-export
```

权限：

```text
项目 VIEWER+
```

响应：

```json
{
  "code": "OK",
  "data": {
    "targetType": "TOOL_EXECUTION",
    "targetId": "205...",
    "fileName": "tool-execution-205-audit.md",
    "contentType": "text/markdown",
    "markdown": "# Tool Execution Audit Report\n...",
    "traceCount": 1,
    "redacted": true,
    "truncated": false,
    "generatedAt": "2026-05-22T..."
  }
}
```

### 8.2 导出 Run Evidence Report

```http
GET /api/multi-agent-runs/{runId}/audit-export
```

权限：

```text
项目 VIEWER+
```

### 8.3 导出 Task Evidence Report

```http
GET /api/tasks/{taskId}/tool-audit-export
```

权限：

```text
项目 VIEWER+
```

### 8.4 创建 Operator Review

```http
POST /api/tool-operator-reviews
```

权限：

```text
target 所属项目 MAINTAINER+
```

请求：

```json
{
  "reviewTargetType": "TOOL_EXECUTION",
  "reviewTargetId": "205...",
  "severity": "MEDIUM",
  "title": "检查一次失败的只读文件读取",
  "summary": "该工具读取失败，需要确认是否为路径策略导致。"
}
```

### 8.5 更新 Operator Review

```http
PUT /api/tool-operator-reviews/{reviewId}
```

权限：

```text
项目 MAINTAINER+
```

请求：

```json
{
  "status": "RESOLVED",
  "resolution": "确认是 denylist 命中，行为符合预期。"
}
```

### 8.6 查询 Operator Review

```http
GET /api/tool-operator-reviews/{reviewId}
```

权限：

```text
项目 VIEWER+
```

### 8.7 查询项目 Reviews

```http
GET /api/projects/{projectId}/tool-operator-reviews?status=&severity=&page=&size=
```

权限：

```text
项目 VIEWER+
```

### 8.8 查询 Target Reviews

```http
GET /api/tool-operator-reviews/by-target?targetType=TOOL_EXECUTION&targetId=205...
```

权限：

```text
target 所属项目 VIEWER+
```

---

## 9. Markdown 导出安全要求

导出 Markdown 必须：

1. 使用 37F `ToolTracePayloadSanitizer`。
2. 对最终 Markdown 再做一次全局 secret mask。
3. 不包含原始 API Key。
4. 不包含 Bearer token。
5. 不包含 GitHub token。
6. 不包含 private key。
7. 不包含 `.env` 文件内容。
8. 如果 payload 被截断，报告中显示：

```text
> Payload was truncated for safety.
```

9. 如果发生 redaction，报告中显示：

```text
> Sensitive values were redacted.
```

10. 导出内容只来自存储 trace，不重新读取文件。

---

## 10. 前端设计

### 10.1 ToolExecutionTraceDrawer 增强

修改：

```text
frontend/src/modules/task/components/ToolExecutionTraceDrawer.vue
```

新增操作：

```text
导出 Markdown
复制 Markdown
创建审查
查看审查
```

推荐 data-testid：

```text
tool-audit-export-button
tool-audit-copy-button
tool-operator-review-create-button
tool-operator-review-list
tool-operator-review-status
```

行为：

- 点击“导出 Markdown”：调用 audit-export API，浏览器下载 `.md` 文件。
- 点击“复制 Markdown”：调用 audit-export API，将 markdown 写入 clipboard。
- 点击“创建审查”：打开创建审查弹窗。
- 如果目标已有审查记录，在 Drawer 中显示审查列表。

### 10.2 新增 Operator Review Dialog

新增：

```text
frontend/src/modules/task/components/ToolOperatorReviewDialog.vue
```

字段：

```text
目标类型
目标 ID
严重级别
标题
摘要
负责人（可选，本阶段可以只填 ID 或不做选择器）
```

状态更新：

```text
OPEN -> IN_PROGRESS -> RESOLVED / WONT_FIX / FALSE_POSITIVE
```

### 10.3 Observability Problem Jobs 集成

修改：

```text
frontend/src/modules/admin/components/ToolExecutionMetricsPanel.vue
```

在 problem jobs 表格中增加：

```text
证据链
导出
创建审查
审查状态
```

如果没有 `toolExecutionId`：

- 禁用“证据链 / 导出”按钮。
- 仍可针对 `TOOL_JOB` 创建审查。

### 10.4 Project Review List

本阶段可以先不新增独立页面，但建议在 ObservabilityPage 增加一个折叠区：

```text
工具审查记录
```

调用：

```text
GET /api/projects/{projectId}/tool-operator-reviews
```

如果当前页面没有 projectId，可暂不做全局列表，仅实现 problem jobs 内联创建与 target reviews 查询。

---

## 11. 前端 API 类型

修改：

```text
frontend/src/modules/task/api.ts
frontend/src/modules/admin/api.ts
```

新增类型：

```ts
export interface ToolAuditExportResponse {
  targetType: string
  targetId: string
  fileName: string
  contentType: string
  markdown: string
  traceCount: number
  redacted: boolean
  truncated: boolean
  generatedAt: string
}

export interface ToolOperatorReview {
  id: string
  projectId: string
  taskId?: string
  runId?: string
  toolExecutionId?: string
  toolJobId?: string
  reviewTargetType: string
  reviewTargetId: string
  status: string
  severity: string
  title: string
  summary?: string
  resolution?: string
  assigneeId?: string
  createdBy: string
  resolvedBy?: string
  createTime: string
  updateTime: string
  resolvedAt?: string
}
```

新增函数：

```ts
export function exportToolExecutionAudit(executionId: string)
export function exportMultiAgentRunAudit(runId: string)
export function exportTaskToolAudit(taskId: string)
export function createToolOperatorReview(data: CreateToolOperatorReviewRequest)
export function updateToolOperatorReview(reviewId: string, data: UpdateToolOperatorReviewRequest)
export function getToolOperatorReview(reviewId: string)
export function listProjectToolOperatorReviews(projectId: string, params?: ...)
export function listTargetToolOperatorReviews(targetType: string, targetId: string)
```

---

## 12. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolAuditExportIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/ToolOperatorReviewIntegrationTest.java
```

至少 34 个测试。

### 12.1 Audit Export 测试

1. export single execution 返回 markdown。
2. markdown 包含 Tool Execution Audit Report。
3. markdown 包含 timeline。
4. markdown 包含 evidence。
5. markdown 包含 filesRead。
6. markdown 包含 skippedFiles。
7. markdown 包含 approval。
8. markdown 包含 job。
9. markdown 包含 artifact / patch review。
10. run export 包含多个 trace。
11. task export 包含多个 trace。
12. secret 被 mask。
13. 超长 payload 被截断。
14. 不存在 execution 返回 NOT_FOUND。
15. 未登录 export 返回 UNAUTHORIZED。
16. 非项目成员 export 返回 PROJECT_ACCESS_DENIED。
17. VIEWER 可 export。

### 12.2 Operator Review 测试

18. MAINTAINER 可创建 execution review。
19. MAINTAINER 可创建 job review。
20. MAINTAINER 可创建 run review。
21. VIEWER 不可创建 review。
22. 非项目成员不可创建 review。
23. 创建 review 时 target 不存在返回 NOT_FOUND。
24. 创建 review 时 invalid targetType 返回 BAD_REQUEST。
25. list target reviews 返回创建记录。
26. list project reviews 支持 status 过滤。
27. list project reviews 支持 severity 过滤。
28. update review status 到 IN_PROGRESS。
29. update review status 到 RESOLVED 自动设置 resolvedBy / resolvedAt。
30. update review resolution 保存成功。
31. VIEWER 不可 update。
32. 查询单条 review 权限正确。
33. 终态改回 OPEN 清空 resolvedAt。
34. title 为空返回 VALIDATION_ERROR 或 BAD_REQUEST。

可以超过 34 个。

---

## 13. 前端 E2E 要求

新增或修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
frontend/e2e/knowledge-observability.spec.ts
```

也可新增：

```text
frontend/e2e/tool-audit-export-review.spec.ts
```

至少 8 个 E2E：

1. Trace Drawer 中点击“导出 Markdown”成功。
2. Trace Drawer 中点击“复制 Markdown”成功或显示成功提示。
3. Trace Drawer 中点击“创建审查”打开 Dialog。
4. 创建审查成功后显示 review 状态。
5. 更新审查状态为 RESOLVED。
6. Problem Jobs 中可创建 job review。
7. Run audit export 按钮可用。
8. 页面无 JS error。

推荐 data-testid：

```text
tool-audit-export-button
tool-audit-copy-button
tool-operator-review-dialog
tool-operator-review-create-button
tool-operator-review-submit-button
tool-operator-review-status
tool-operator-review-resolution
problem-job-create-review-button
```

---

## 14. 文档与报告

完成后新增：

```text
docs/milestone-37g-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. tool_operator_review 表说明
3. Audit Export Service 设计说明
4. Operator Review Service 设计说明
5. API 清单
6. Markdown 导出格式与安全处理
7. 前端 Trace Drawer 增强说明
8. Observability Problem Jobs 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37H

---

## 15. 验收标准

完成后必须满足：

1. 单次 tool execution 可导出 Markdown。
2. multi-agent run 可导出 evidence report。
3. task 可导出 tool audit report。
4. 导出内容包含 timeline / evidence / approval / job / artifact。
5. 导出内容不泄露 secret。
6. 导出不会重新执行工具。
7. 导出不会重新读取文件。
8. 操作员可创建 review。
9. 操作员可更新 review 状态。
10. problem jobs 可创建 review。
11. Trace Drawer 可显示 review 状态。
12. 权限校验正确。
13. 后端测试通过。
14. 前端 typecheck / build / E2E 通过。

---

## 16. 非目标

本阶段不做：

1. 不做 PDF 导出。
2. 不做 CSV 导出。
3. 不做 email 发送。
4. 不做 Slack / PagerDuty 通知。
5. 不做多级审批工作流。
6. 不做审查 SLA。
7. 不做审查评论 threaded conversation。
8. 不做 OpenTelemetry / Jaeger。
9. 不做真实工具执行回放。
10. 不做真实 Git 写操作。

这些可以放到后续 Milestone。

---

## 17. 建议后续 Milestone

完成 37G 后，建议进入：

```text
Milestone 37H: Tool Execution Incident Workflow & Alert Routing
```

候选能力：

- Problem job 自动创建 incident。
- Severity 到 alert rule 映射。
- Operator review SLA。
- Alert routing webhook mock。
- Incident dashboard。

也可以进入：

```text
Milestone 38A: Semantic Code Search / RAG Evaluation
```

---

## 18. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37G。

文档路径：
docs/milestone-37g-tool-execution-audit-export-operator-review.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37F Tool Execution Trace & Evidence Viewer 基础上，新增 Audit Export 与 Operator Review。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 导出只读取数据库已有 trace 数据，不重新执行工具。
9. 导出不重新读取仓库文件。
10. Operator Review 只记录人工处理信息，不改变工具执行结果。
11. 不要破坏 36A-37F 已有 API。
12. 不要破坏 35A-35F Multi-Agent API。
13. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
14. IDs 对外保持 String。
15. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V33__init_tool_operator_review.sql。
2. 新增 ToolOperatorReviewStatus / ToolOperatorReviewSeverity / ToolOperatorReviewTargetType 枚举。
3. 新增 ToolOperatorReviewEntity / Mapper / DTO。
4. 新增 ToolExecutionAuditExportService，支持导出：
   - 单次 tool execution markdown
   - multi-agent run evidence markdown
   - task tool audit markdown
5. 新增 ToolOperatorReviewService，支持创建、更新、查询、列表。
6. 新增 API：
   - GET /api/tool-sandbox-executions/{executionId}/audit-export
   - GET /api/multi-agent-runs/{runId}/audit-export
   - GET /api/tasks/{taskId}/tool-audit-export
   - POST /api/tool-operator-reviews
   - PUT /api/tool-operator-reviews/{reviewId}
   - GET /api/tool-operator-reviews/{reviewId}
   - GET /api/projects/{projectId}/tool-operator-reviews
   - GET /api/tool-operator-reviews/by-target
7. Markdown 导出必须复用 sanitizer，最终内容不得泄露 API Key / Token / Secret。
8. ToolExecutionTraceDrawer 增加“导出 Markdown / 复制 Markdown / 创建审查 / 查看审查”。
9. 新增 ToolOperatorReviewDialog.vue。
10. Observability problem jobs 增加“创建审查 / 审查状态”。
11. 后端测试不少于 34 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-37g-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. tool_operator_review 表说明
3. Audit Export Service 设计说明
4. Operator Review Service 设计说明
5. API 清单
6. Markdown 导出格式与安全处理
7. 前端 Trace Drawer 增强说明
8. Observability Problem Jobs 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37H

现在开始实现，不要只给计划。
```
