# Milestone 37F: Tool Execution Trace & Evidence Viewer

## 1. 背景

Milestone 36A-36I 已经完成 Tool Sandbox / Tool Catalog / Human Approval / Patch Proposal / Parameter Schema / Patch Review 等基础能力。

Milestone 37A-37E 又继续补齐了异步 Worker、DLQ/Retry、Metrics Dashboard、真实只读仓库适配器加固：

- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index
- 37C: Worker DLQ / Retry Backoff
- 37D: Tool Execution Metrics Dashboard
- 37E: Real Read-only Tool Adapter Hardening

现在工具链已经可以做到：

```text
Policy Check
  -> Human Approval
  -> Job Queue
  -> Worker Execution
  -> Read-only Repository Adapter
  -> Output / Artifact / Metrics
```

但对最终用户、审计人员和开发者来说，还缺少一个关键能力：

```text
一次工具执行到底经历了什么？
读了哪些文件？
跳过了哪些文件？
为什么允许执行？
谁审批了？
是否触发 retry / DLQ？
是否产生 artifact / patch review？
输出有没有 redacted / truncated？
```

Milestone 37F 的目标是新增 **Tool Execution Trace & Evidence Viewer**：

```text
把一次 tool execution 的策略判定、审批、job、worker、输入、输出、文件证据、artifact、review、metrics 串成完整证据链。
```

本阶段只做可审计展示与后端聚合，不新增真实执行能力。

---

## 2. 总目标

实现工具执行证据链能力：

1. 新增 Tool Execution Trace 聚合服务。
2. 新增 Trace / Evidence DTO。
3. 新增工具执行 trace 查询 API。
4. 聚合 tool sandbox execution、approval、job、artifact、patch review、read evidence、retry / DLQ 信息。
5. 前端新增 ToolExecutionTraceDrawer。
6. MultiAgentRunPanel 中每个 tool job / execution 可打开 Trace。
7. Observability problem jobs 可跳转或打开 Trace。
8. Trace 视图展示完整时间线。
9. Trace 视图展示安全证据：policy result、approval、filesRead、skippedFiles、redacted、truncated、readOnly。
10. 补齐后端集成测试与前端 E2E。

完成后，从：

```text
工具执行结果可见
```

升级为：

```text
工具执行过程可解释、可审计、可回放查看
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
9. Trace 只读取数据库中已有记录和 outputPayload。
10. Trace 不重新执行工具。
11. Trace 不重新读取仓库文件。
12. Trace 不返回 secret 原文。
13. Trace 不绕过项目权限。
14. 不破坏 36A-37E 已有 API。
15. 不破坏 Multi-Agent Run / Task Detail 现有页面。
16. 前端保持中文暗色科技风 UI。

允许做：

1. 新增只读查询 API。
2. 新增 trace 聚合 DTO。
3. 从已保存的 inputPayload / outputPayload 中解析 filesRead、skippedFiles、redacted、truncated。
4. 从 existing entities 聚合 approval、job、artifact、patch review。
5. 在前端展示 trace timeline 和 evidence cards。

---

## 4. 后端设计

### 4.1 新增包位置

建议沿用 orchestration 模块：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/
backend/src/main/java/com/aicoding/platform/orchestration/application/
backend/src/main/java/com/aicoding/platform/orchestration/controller/
```

新增：

```text
ToolExecutionTraceResponse.java
ToolExecutionTraceEventResponse.java
ToolExecutionEvidenceResponse.java
ToolExecutionFileEvidenceResponse.java
ToolExecutionArtifactEvidenceResponse.java
ToolExecutionApprovalEvidenceResponse.java
ToolExecutionJobEvidenceResponse.java
ToolExecutionTraceService.java
```

如果现有 controller 已有 ToolExecutionJobController / ToolSandboxExecutionController，可直接扩展，不强制新增 Controller。

---

## 5. Trace 聚合对象设计

### 5.1 ToolExecutionTraceResponse

```java
public class ToolExecutionTraceResponse {
    private String executionId;
    private String projectId;
    private String taskId;
    private String runId;
    private String stepId;
    private String toolKey;
    private String toolName;
    private String riskLevel;
    private String status;
    private String mode;
    private Boolean readOnly;
    private Boolean policyAllowed;
    private String policyReason;
    private String inputPayload;
    private String outputPayload;
    private ToolExecutionApprovalEvidenceResponse approval;
    private ToolExecutionJobEvidenceResponse job;
    private ToolExecutionEvidenceResponse evidence;
    private List<ToolExecutionTraceEventResponse> events;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

要求：

- 所有 ID 对外保持 String。
- `inputPayload` / `outputPayload` 是已保存 JSON 的安全版本。
- 如果 outputPayload 中存在疑似 secret，必须由 37E 的 redaction 结果或本阶段二次防御处理。
- `events` 按时间升序。

### 5.2 ToolExecutionTraceEventResponse

```java
public class ToolExecutionTraceEventResponse {
    private String eventType;
    private String title;
    private String description;
    private String status;
    private LocalDateTime eventTime;
    private Map<String, Object> metadata;
}
```

建议事件类型：

```text
EXECUTION_CREATED
POLICY_CHECKED
APPROVAL_CREATED
APPROVAL_ACCEPTED
APPROVAL_REJECTED
JOB_CREATED
JOB_QUEUED
JOB_RUNNING
JOB_RETRY_PENDING
JOB_COMPLETED
JOB_FAILED
JOB_DEAD_LETTERED
OUTPUT_CAPTURED
ARTIFACT_CREATED
PATCH_REVIEW_CREATED
PATCH_REVIEW_DECIDED
```

不要求新增事件表。本阶段可以从现有实体字段推导事件。

### 5.3 ToolExecutionEvidenceResponse

```java
public class ToolExecutionEvidenceResponse {
    private Integer filesReadCount;
    private Integer skippedFilesCount;
    private Boolean redacted;
    private Boolean truncated;
    private Boolean binarySkipped;
    private Boolean pathSandboxApplied;
    private Boolean sensitiveDenylistApplied;
    private List<ToolExecutionFileEvidenceResponse> filesRead;
    private List<ToolExecutionFileEvidenceResponse> skippedFiles;
    private List<ToolExecutionArtifactEvidenceResponse> artifacts;
}
```

从 outputPayload 解析：

```json
{
  "readOnly": true,
  "filesRead": [],
  "skippedFiles": [],
  "redacted": true,
  "truncated": false,
  "filesTouched": [],
  "gitOperations": []
}
```

如果 outputPayload 缺少字段：

- `filesReadCount = 0`
- `skippedFilesCount = 0`
- `redacted = false`
- `truncated = false`
- 不抛异常

### 5.4 ToolExecutionFileEvidenceResponse

```java
public class ToolExecutionFileEvidenceResponse {
    private String path;
    private String reason;
    private Long sizeBytes;
    private Integer lineStart;
    private Integer lineEnd;
    private Boolean redacted;
    private Boolean truncated;
}
```

### 5.5 ToolExecutionApprovalEvidenceResponse

```java
public class ToolExecutionApprovalEvidenceResponse {
    private String approvalId;
    private String status;
    private String approverId;
    private String approverName;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime decidedAt;
}
```

### 5.6 ToolExecutionJobEvidenceResponse

```java
public class ToolExecutionJobEvidenceResponse {
    private String jobId;
    private String status;
    private String priority;
    private Integer attemptCount;
    private String errorCode;
    private String failureStage;
    private LocalDateTime nextRetryAt;
    private LocalDateTime deadLetteredAt;
    private String deadLetterReason;
    private String sourceJobId;
    private LocalDateTime createTime;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

### 5.7 ToolExecutionArtifactEvidenceResponse

```java
public class ToolExecutionArtifactEvidenceResponse {
    private String artifactId;
    private String artifactType;
    private String title;
    private String patchReviewStatus;
    private String patchReviewDecision;
    private LocalDateTime createTime;
}
```

---

## 6. Trace Service 设计

新增：

```text
ToolExecutionTraceService.java
```

职责：

1. 根据 executionId 查询 tool sandbox execution。
2. 校验用户对 projectId 的 VIEWER+ 权限。
3. 查询 approval。
4. 查询 job。
5. 查询关联 artifact。
6. 查询 patch review。
7. 解析 inputPayload / outputPayload。
8. 生成 evidence。
9. 生成 timeline events。
10. 返回 ToolExecutionTraceResponse。

建议方法：

```java
public ToolExecutionTraceResponse getTrace(Long executionId)

public List<ToolExecutionTraceResponse> listRunTraces(Long runId)

public List<ToolExecutionTraceResponse> listTaskTraces(Long taskId)
```

权限规则：

- `getTrace(executionId)`：通过 execution.projectId 校验 VIEWER+。
- `listRunTraces(runId)`：通过 run.projectId 校验 VIEWER+。
- `listTaskTraces(taskId)`：通过 task.projectId 校验 VIEWER+。

异常规则：

- execution 不存在：`NOT_FOUND`
- run 不存在：`NOT_FOUND`
- task 不存在：`NOT_FOUND`
- 无项目权限：`PROJECT_ACCESS_DENIED`
- JSON 解析失败：不要让 API 500，返回空 evidence，并在 events 中增加 `OUTPUT_PARSE_WARNING`

---

## 7. 后端 API

### 7.1 查询单次 Tool Execution Trace

```http
GET /api/tool-sandbox-executions/{executionId}/trace
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
    "executionId": "205...",
    "projectId": "101",
    "taskId": "201",
    "runId": "301",
    "stepId": "401",
    "toolKey": "READ_FILE_SNIPPET",
    "toolName": "Read File Snippet",
    "riskLevel": "MEDIUM",
    "status": "COMPLETED",
    "readOnly": true,
    "policyAllowed": true,
    "policyReason": "Project tool policy allows this tool",
    "approval": null,
    "job": {
      "jobId": "501",
      "status": "COMPLETED",
      "attemptCount": 1
    },
    "evidence": {
      "filesReadCount": 1,
      "skippedFilesCount": 0,
      "redacted": false,
      "truncated": false,
      "filesRead": [
        {
          "path": "backend/src/main/java/Example.java",
          "lineStart": 1,
          "lineEnd": 80
        }
      ]
    },
    "events": [
      {
        "eventType": "EXECUTION_CREATED",
        "title": "工具执行已创建"
      },
      {
        "eventType": "JOB_COMPLETED",
        "title": "Worker 执行完成"
      }
    ]
  }
}
```

### 7.2 查询 Run 下全部 Tool Traces

```http
GET /api/multi-agent-runs/{runId}/tool-execution-traces
```

权限：

```text
项目 VIEWER+
```

用途：

- MultiAgentRunPanel 批量展示 evidence。
- 避免前端逐个 execution 发太多请求。

### 7.3 查询 Task 下全部 Tool Traces

```http
GET /api/tasks/{taskId}/tool-execution-traces
```

权限：

```text
项目 VIEWER+
```

用途：

- Task Detail 的工具证据总览。
- 后续可挂到 Observability problem job 跳转。

---

## 8. 数据库变更

本阶段默认不强制新增表。

原因：

- 36A-37E 已经把 execution / approval / job / artifact / review / metrics 存起来。
- Trace 可由现有实体聚合生成。
- 不新增物理外键，保持既有风格。

允许新增轻量 migration 的情况：

如果现有 `tool_sandbox_execution` 缺少 `policy_reason`、`risk_level` 或类似字段，允许新增字段：

```text
V32__alter_tool_sandbox_execution_trace_fields.sql
```

可选字段：

```sql
ALTER TABLE tool_sandbox_execution
  ADD COLUMN policy_allowed TINYINT NULL,
  ADD COLUMN policy_reason VARCHAR(512) NULL,
  ADD COLUMN trace_summary TEXT NULL;
```

注意：

- 只有确实缺字段才新增。
- 若现有字段已满足，不要新增 migration。
- 测试 schema.sql 需要同步。

---

## 9. 安全处理

### 9.1 outputPayload 二次防御

Trace API 返回 outputPayload 时必须二次保护：

1. 对疑似 secret pattern 做 mask。
2. 单字段最大长度建议 64KB。
3. 总响应最大 evidence payload 建议 256KB。
4. 如果被截断，设置 `truncated=true`。

可以复用 37E 的 RepositoryContentSafetyService，或新增：

```text
ToolTracePayloadSanitizer.java
```

要求：

- 不返回 API Key 原文。
- 不返回 Bearer token 原文。
- 不返回 GitHub token 原文。
- 不返回 private key 原文。

### 9.2 filesTouched / gitOperations 验证

对于只读工具，Trace 应当明确显示：

```json
{
  "readOnly": true,
  "filesTouched": [],
  "gitOperations": []
}
```

如果 outputPayload 中出现非空 `filesTouched` 或 `gitOperations`：

- 不执行任何修复动作。
- Trace 中增加 WARNING 事件：

```text
READ_ONLY_CONTRACT_WARNING
```

前端显示黄色告警。

### 9.3 权限

Trace API 不允许通过 ID 越权读取其他项目执行记录。

必须测试：

- 未登录：UNAUTHORIZED
- 非项目成员：PROJECT_ACCESS_DENIED
- VIEWER：可读取
- DEVELOPER / MAINTAINER / OWNER：可读取

---

## 10. 前端设计

### 10.1 新增组件

新增：

```text
frontend/src/modules/task/components/ToolExecutionTraceDrawer.vue
```

职责：

1. 接收 `executionId` 或 `trace`。
2. 打开抽屉时加载 trace。
3. 展示 timeline。
4. 展示 policy / approval / job / evidence / artifact / payload。
5. 展示安全提示。

Props 建议：

```ts
interface Props {
  modelValue: boolean
  executionId?: string
}
```

事件：

```ts
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()
```

### 10.2 Trace Drawer 结构

UI 结构：

```text
Tool Execution Trace
  ├─ Header: toolKey / status / riskLevel / readOnly
  ├─ Safety Banner
  │   ├─ 只读执行
  │   ├─ 未写入文件
  │   ├─ 未执行 Git 写操作
  │   └─ Redacted / Truncated badges
  ├─ Timeline
  │   ├─ Policy Checked
  │   ├─ Approval
  │   ├─ Job Queue
  │   ├─ Worker Running
  │   ├─ Output Captured
  │   └─ Artifact / Review
  ├─ Evidence
  │   ├─ Files Read
  │   ├─ Skipped Files
  │   ├─ Denylist / Path Sandbox
  │   └─ Redaction / Truncation
  ├─ Approval
  ├─ Job
  ├─ Artifacts / Patch Review
  └─ Payload JSON
```

必须保持中文暗色科技风 UI。

### 10.3 推荐 data-testid

```text
tool-trace-drawer
tool-trace-open-button
tool-trace-status
tool-trace-risk
tool-trace-readonly-badge
tool-trace-safety-banner
tool-trace-timeline
tool-trace-event
tool-trace-files-read
tool-trace-skipped-files
tool-trace-redacted-badge
tool-trace-truncated-badge
tool-trace-approval
tool-trace-job
tool-trace-artifact
tool-trace-payload
tool-trace-warning
```

### 10.4 MultiAgentRunPanel 集成

在现有 tool execution / job 卡片中增加：

```text
查看证据链
```

点击打开 ToolExecutionTraceDrawer。

要求：

- 不影响现有 approval / retry / cancel / manual retry 按钮。
- 如果 executionId 缺失，按钮隐藏。
- 对 PATCH_PROPOSAL 展示 artifact / review status。
- 对 READ_* 工具展示 filesRead / skippedFiles / redacted / truncated 摘要。

### 10.5 Observability 集成

在 ToolExecutionMetricsPanel 的 problem jobs 中增加：

```text
查看证据链
```

如果 problem job 有 toolExecutionId，点击打开 Trace Drawer。

如果 problem job 缺少 executionId，只展示 job 信息，不显示 Trace 按钮。

---

## 11. 前端 API 类型

修改：

```text
frontend/src/modules/task/api.ts
```

新增：

```ts
export interface ToolExecutionTraceEvent {
  eventType: string
  title: string
  description?: string
  status?: string
  eventTime?: string
  metadata?: Record<string, unknown>
}

export interface ToolExecutionFileEvidence {
  path: string
  reason?: string
  sizeBytes?: number
  lineStart?: number
  lineEnd?: number
  redacted?: boolean
  truncated?: boolean
}

export interface ToolExecutionTrace {
  executionId: string
  projectId: string
  taskId?: string
  runId?: string
  stepId?: string
  toolKey: string
  toolName?: string
  riskLevel?: string
  status: string
  mode?: string
  readOnly?: boolean
  policyAllowed?: boolean
  policyReason?: string
  inputPayload?: string
  outputPayload?: string
  approval?: unknown
  job?: unknown
  evidence?: {
    filesReadCount: number
    skippedFilesCount: number
    redacted: boolean
    truncated: boolean
    filesRead: ToolExecutionFileEvidence[]
    skippedFiles: ToolExecutionFileEvidence[]
  }
  events: ToolExecutionTraceEvent[]
}

export function getToolExecutionTrace(executionId: string)
export function getMultiAgentRunToolExecutionTraces(runId: string)
export function getTaskToolExecutionTraces(taskId: string)
```

如果 admin API 也需要使用 Trace Drawer，可将类型放到 shared 或 task/api 复用，不要重复定义不一致类型。

---

## 12. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionTraceIntegrationTest.java
```

至少 24 个测试。

### 12.1 Trace 基础测试

1. completed execution 可返回 trace。
2. trace 包含 executionId / toolKey / status。
3. trace 包含 job evidence。
4. trace 包含 timeline events。
5. run trace list 返回该 run 下所有 traces。
6. task trace list 返回该 task 下所有 traces。

### 12.2 Evidence 测试

7. READ_FILE_SNIPPET trace 解析 filesRead。
8. READ_REPOSITORY_TREE trace 解析 skippedFiles。
9. redacted=true 正确返回。
10. truncated=true 正确返回。
11. outputPayload 缺字段时不报错。
12. outputPayload 非法 JSON 时返回 warning event。

### 12.3 Approval / Artifact / Review 测试

13. HIGH risk approval pending trace 包含 approval。
14. approval accepted trace 包含 decidedAt。
15. approval rejected trace 包含 rejected status。
16. PATCH_PROPOSAL trace 包含 artifact evidence。
17. PATCH_PROPOSAL trace 包含 patchReviewStatus。

### 12.4 Retry / DLQ 测试

18. retry pending job trace 包含 nextRetryAt。
19. dead lettered job trace 包含 deadLetteredAt / reason。
20. manual retry job trace 包含 sourceJobId。

### 12.5 权限测试

21. 未登录 get trace → UNAUTHORIZED。
22. 非项目成员 get trace → PROJECT_ACCESS_DENIED。
23. VIEWER 可 get trace。
24. 不存在 executionId → NOT_FOUND。

### 12.6 安全测试

25. outputPayload 中 secret 被 mask。
26. readOnly 工具出现 filesTouched 非空时生成 warning event。
27. gitOperations 非空时生成 warning event。
28. 超长 outputPayload 被截断。

可以超过 24 个，推荐覆盖 28 个。

---

## 13. 前端 E2E 要求

修改或新增：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
frontend/e2e/knowledge-observability.spec.ts
```

也可新增：

```text
frontend/e2e/tool-execution-trace.spec.ts
```

至少 6 个 E2E：

1. Multi-Agent run 中点击“查看证据链”打开 Drawer。
2. Drawer 显示 timeline。
3. Drawer 显示只读安全提示。
4. Drawer 显示 filesRead / skippedFiles。
5. Patch Proposal trace 显示 artifact / review 状态。
6. Problem jobs 中可打开 Trace（如果有 executionId）。
7. Drawer 关闭后页面不报 JS error。

建议 data-testid 断言：

```ts
await expect(page.getByTestId('tool-trace-drawer')).toBeVisible()
await expect(page.getByTestId('tool-trace-timeline')).toBeVisible()
await expect(page.getByTestId('tool-trace-safety-banner')).toBeVisible()
```

---

## 14. 文档与报告

完成后新增：

```text
docs/milestone-37f-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Trace DTO 设计说明
3. Trace Service 聚合逻辑
4. Trace API 清单
5. Evidence 解析与安全处理
6. Timeline 事件设计
7. 前端 ToolExecutionTraceDrawer 说明
8. MultiAgentRunPanel / Observability 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37G

---

## 15. 验收标准

完成后必须满足：

1. `GET /api/tool-sandbox-executions/{executionId}/trace` 可用。
2. `GET /api/multi-agent-runs/{runId}/tool-execution-traces` 可用。
3. `GET /api/tasks/{taskId}/tool-execution-traces` 可用。
4. Trace 返回 policy / approval / job / evidence / events。
5. 只读工具 trace 展示 filesRead / skippedFiles。
6. redacted / truncated 可见。
7. PATCH_PROPOSAL trace 关联 artifact / patch review。
8. Retry / DLQ 信息可见。
9. 前端 MultiAgentRunPanel 可打开 Trace Drawer。
10. Observability problem jobs 可查看 Trace。
11. 权限校验正确。
12. 不新增真实写操作能力。
13. 不泄露 secret。
14. 后端测试通过。
15. 前端 typecheck / build / E2E 通过。

---

## 16. 非目标

本阶段不做：

1. 不做 OpenTelemetry tracing 接入。
2. 不做 Jaeger / Tempo 集成。
3. 不做 Trace 持久化新表，除非确实缺字段。
4. 不做实时 WebSocket trace。
5. 不做工具执行回放。
6. 不做重新执行工具。
7. 不做真实 patch apply。
8. 不做真实 Git 写操作。
9. 不做复杂 graph visualization。
10. 不做导出 PDF / CSV。

这些可以放到后续 Milestone。

---

## 17. 建议后续 Milestone

完成 37F 后，建议进入：

```text
Milestone 37G: Tool Execution Audit Export & Operator Review
```

候选能力：

- 导出单次 tool trace 为 Markdown。
- 导出 run-level evidence report。
- Operator 审核 problem jobs。
- Security review checklist。
- Evidence bundle download。

也可以进入：

```text
Milestone 38A: Semantic Code Search / RAG Evaluation
```

---

## 18. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37F。

文档路径：
docs/milestone-37f-tool-execution-trace-evidence-viewer.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 36A-36I 和 37A-37E 的 Tool Sandbox / Approval / Job Queue / DLQ / Metrics / Read-only Adapter 基础上，新增 Tool Execution Trace & Evidence Viewer。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. Trace 只读取数据库已有记录和 outputPayload，不重新执行工具。
9. Trace 不重新读取仓库文件。
10. 不要破坏 36A-37E 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 ToolExecutionTraceResponse / ToolExecutionTraceEventResponse / ToolExecutionEvidenceResponse 等 DTO。
2. 新增 ToolExecutionTraceService，聚合 execution、approval、job、artifact、patch review、outputPayload evidence、timeline events。
3. 新增或扩展 Controller，提供：
   - GET /api/tool-sandbox-executions/{executionId}/trace
   - GET /api/multi-agent-runs/{runId}/tool-execution-traces
   - GET /api/tasks/{taskId}/tool-execution-traces
4. Trace 必须包含 policy / approval / job / evidence / events。
5. Evidence 必须解析 filesRead / skippedFiles / redacted / truncated / readOnly。
6. PATCH_PROPOSAL trace 必须关联 artifact 和 patch review 状态。
7. Retry / DLQ job trace 必须展示 nextRetryAt / deadLetteredAt / deadLetterReason / sourceJobId。
8. outputPayload 返回前必须做 secret mask 和长度保护。
9. 前端新增 ToolExecutionTraceDrawer.vue。
10. MultiAgentRunPanel 中每个 tool execution / job 可打开 Trace Drawer。
11. Observability problem jobs 中如有 toolExecutionId，可打开 Trace Drawer。
12. 新增后端测试不少于 24 个，建议覆盖 28 个。
13. 新增前端 E2E 不少于 6 个。
14. 新增 docs/milestone-37f-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Trace DTO 设计说明
3. Trace Service 聚合逻辑
4. Trace API 清单
5. Evidence 解析与安全处理
6. Timeline 事件设计
7. 前端 ToolExecutionTraceDrawer 说明
8. MultiAgentRunPanel / Observability 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37G

现在开始实现，不要只给计划。
```
