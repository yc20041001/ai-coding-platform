# Milestone 36F: Sandbox Worker Queue

## 1. 背景

Milestone 36A-36E 已经完成安全工具能力：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema

当前工具执行链路已经具备：

```text
Tool Catalog -> Project Tool Config -> Parameters -> Policy -> Approval -> Mock Execute -> Artifact
```

但工具执行仍主要发生在请求线程中。即使当前只是 Mock，后续要支持更复杂的只读工具、仓库扫描、长耗时分析、可恢复任务，就需要一个执行队列模型。

Milestone 36F 的目标是新增 **Sandbox Worker Queue**：

```text
Tool Execution Requested
  -> create Tool Execution Job
  -> PENDING / RUNNING / COMPLETED / FAILED / CANCELED
  -> synchronous mock drain for now
  -> retry / cancel API
  -> frontend queue status display
```

本阶段仍不引入真实异步 Worker、不接 MQ、不执行真实 shell、不做 Git 写操作、不写真实代码文件。Queue 是数据库模型 + 同步 Mock drain，为后续异步 Worker 打基础。

## 2. 总目标

实现工具执行队列基础能力：

1. 新增 `tool_execution_job` 表。
2. 每次工具执行创建 Job 记录。
3. Job 与 `tool_sandbox_execution` 关联。
4. 支持 PENDING / RUNNING / COMPLETED / FAILED / CANCELED 状态。
5. 支持 retryCount / maxRetryCount / lastError。
6. 当前阶段通过同步 mock drain 完成 job。
7. 提供 Job 查询 / retry / cancel API。
8. ToolSandboxExecutionResponse 返回 job 信息。
9. 前端展示工具 Job 状态、重试次数、耗时。
10. 补齐后端测试和前端 E2E。

完成后，工具执行从：

```text
request thread directly marks execution completed
```

升级为：

```text
request thread creates job -> mock worker drains job -> execution completed
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不写真实代码文件。
4. 不应用 patch。
5. 不引入 RabbitMQ / Redis Queue / Kafka。
6. 不引入 Spring Scheduler 后台扫描。
7. 不引入真实异步线程池执行工具。
8. 不做分布式锁。
9. 不做 Worker 心跳。
10. 不做并发抢占。
11. 不破坏 36A-36E API。
12. 不破坏 Multi-Agent / Task 状态机。
13. 不绕过 Tool Policy / Approval。
14. 前端保持中文暗色科技风 UI。

允许做：

- 新增 Job 表。
- 同步创建和 drain Mock job。
- 提供 retry / cancel API。
- 记录 Job 状态、耗时、错误。
- 前端展示队列状态。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V25__init_tool_execution_job_tables.sql
```

如果 V25 已存在，请顺延。

### 4.1 tool_execution_job

```sql
CREATE TABLE IF NOT EXISTS tool_execution_job (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    step_id BIGINT NULL,
    tool_execution_id BIGINT NOT NULL,
    tool_key VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 2,
    request_payload JSON NULL,
    result_payload JSON NULL,
    last_error TEXT NULL,
    locked_by VARCHAR(128) NULL,
    locked_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_job_project_time(project_id, create_time),
    INDEX idx_tool_job_task(task_id),
    INDEX idx_tool_job_run(run_id),
    INDEX idx_tool_job_step(step_id),
    INDEX idx_tool_job_execution(tool_execution_id),
    INDEX idx_tool_job_status(status),
    INDEX idx_tool_job_tool(tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行队列作业表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| tool_execution_id | 关联 tool_sandbox_execution.id |
| status | PENDING / RUNNING / COMPLETED / FAILED / CANCELED |
| priority | LOW / NORMAL / HIGH，当前仅记录 |
| retry_count | 已重试次数 |
| max_retry_count | 最大重试次数 |
| request_payload | Job 输入快照 |
| result_payload | Job 输出快照 |
| locked_by / locked_at | 后续异步 Worker 预留 |
| duration_ms | 执行耗时 |

无物理外键，保持项目规范。

## 5. 状态与枚举设计

### 5.1 ToolExecutionJobStatus.java

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobStatus.java
```

```java
public enum ToolExecutionJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED
}
```

### 5.2 ToolExecutionJobPriority.java

新增：

```java
public enum ToolExecutionJobPriority {
    LOW,
    NORMAL,
    HIGH
}
```

当前阶段 priority 只记录，不做调度排序。

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobEntity.java
```

要求：

- `@TableName("tool_execution_job")`
- `@TableId(type = IdType.ASSIGN_ID)`
- createTime / updateTime 自动填充
- 不使用 Lombok
- 手写 getter/setter

### 6.2 Mapper

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolExecutionJobMapper.java
```

### 6.3 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionJobResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/RetryToolExecutionJobRequest.java
```

`ToolExecutionJobResponse` 字段：

- id
- projectId
- taskId
- runId
- stepId
- toolExecutionId
- toolKey
- status
- priority
- retryCount
- maxRetryCount
- requestPayload
- resultPayload
- lastError
- startedAt
- finishedAt
- durationMs
- createTime
- updateTime

`RetryToolExecutionJobRequest` 字段：

- reason String

## 7. Response 增强

修改：

```text
ToolSandboxExecutionResponse.java
```

新增：

- jobId String
- job ToolExecutionJobResponse

如果一个 execution 历史上有多次 job，response 默认返回最新 job。

可选：

- jobs List<ToolExecutionJobResponse>

本阶段建议只返回 latest job，避免响应过重。

## 8. 后端服务设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionJobService.java
```

### 8.1 创建 Job

```java
public ToolExecutionJobEntity createJob(ToolSandboxExecutionEntity execution, String requestPayload)
```

行为：

1. 插入 PENDING job。
2. priority 默认 NORMAL。
3. retryCount = 0。
4. maxRetryCount = 2。

### 8.2 同步 Mock Drain

```java
public ToolExecutionJobEntity drainMockJob(Long jobId)
```

行为：

1. 校验 job 存在。
2. 如果 job.status != PENDING → CONFLICT。
3. 标记 RUNNING。
4. 不执行真实 shell / Git / 文件写入。
5. 调用 ToolSandboxExecutionService 的内部 mock output 生成方法。
6. 标记 COMPLETED。
7. 写 resultPayload。
8. 更新 execution status / outputPayload / duration。
9. 返回 job。

注意：如果现有 ToolSandboxExecutionService 已负责生成 output，可将生成逻辑提取为 package-private helper，避免循环依赖。

### 8.3 retry

```java
public ToolExecutionJobResponse retryJob(Long jobId, RetryToolExecutionJobRequest request)
```

行为：

1. job 不存在 → NOT_FOUND。
2. 权限 MAINTAINER+。
3. 只有 FAILED / CANCELED job 可 retry。
4. retryCount >= maxRetryCount → CONFLICT。
5. 创建新的 PENDING job 或复用原 job？

推荐：创建新的 job，保留历史。

新 job：

- retryCount = old.retryCount + 1。
- requestPayload = old.requestPayload。
- toolExecutionId = old.toolExecutionId。
- status = PENDING → drainMockJob。

### 8.4 cancel

```java
public ToolExecutionJobResponse cancelJob(Long jobId)
```

行为：

1. job 不存在 → NOT_FOUND。
2. 权限 MAINTAINER+。
3. 只有 PENDING / RUNNING 可 cancel。
4. 当前无真实异步执行，所以 RUNNING 基本只在瞬时出现。
5. 标记 CANCELED。
6. 如果 execution 尚未完成，标记 CANCELED 或 BLOCKED（建议新增 ToolExecutionStatus.CANCELED）。

如果不想扩展 ToolExecutionStatus，可将 execution 标记 FAILED 并 errorMessage = canceled。推荐新增 CANCELED。

### 8.5 查询

```java
public ToolExecutionJobResponse getJob(Long jobId)
public List<ToolExecutionJobResponse> listByExecution(Long executionId)
public List<ToolExecutionJobResponse> listByRun(Long runId)
```

权限：

- 查询 VIEWER+。
- retry / cancel MAINTAINER+。

## 9. ToolSandboxExecutionService 改造

当前 36E 执行流程大致为：

```text
policy allowed -> generate mock output -> mark execution completed
requires approval -> waiting approval
approve -> generate mock output -> mark completed
```

36F 改为：

```text
policy allowed
  -> create execution PENDING
  -> create job PENDING
  -> drain mock job synchronously
  -> execution COMPLETED

approve
  -> approval APPROVED
  -> create job PENDING
  -> drain mock job synchronously
  -> execution COMPLETED
```

关键要求：

1. 对外行为保持基本兼容：请求结束时 Mock 仍已完成。
2. 数据库中能看到 job 从 PENDING/RUNNING 到 COMPLETED 的最终状态。
3. outputPayload / artifact 逻辑保持 36D/36E 结果不变。
4. rejected / blocked 不创建 completed job。

## 10. Job 与 Artifact 的关系

对于 `MOCK_PATCH_PROPOSAL`：

```text
approve -> create job -> drain job -> execution completed -> create PATCH_PROPOSAL artifact
```

`tool_execution_job.result_payload` 应包含：

```json
{
  "mock": true,
  "jobCompleted": true,
  "artifactId": "123",
  "filesTouched": [],
  "gitOperations": []
}
```

`tool_sandbox_execution.output_payload` 保持包含 artifactId。

## 11. 后端 API

新增 Controller：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionJobController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/tool-execution-jobs/{jobId}` | VIEWER+ | 查询 Job 详情 |
| GET | `/api/tool-sandbox-executions/{executionId}/jobs` | VIEWER+ | 查询 execution 下 Job 历史 |
| GET | `/api/multi-agent-runs/{runId}/tool-execution-jobs` | VIEWER+ | 查询 Run 下所有 Job |
| POST | `/api/tool-execution-jobs/{jobId}/retry` | MAINTAINER+ | 重试失败或取消的 Job |
| POST | `/api/tool-execution-jobs/{jobId}/cancel` | MAINTAINER+ | 取消 pending/running Job |

响应示例：

```json
{
  "code": "OK",
  "data": {
    "id": "123",
    "toolExecutionId": "456",
    "toolKey": "MOCK_PATCH_PROPOSAL",
    "status": "COMPLETED",
    "retryCount": 0,
    "maxRetryCount": 2,
    "durationMs": 12
  }
}
```

## 12. 任务日志

新增日志阶段：

| stage | 说明 |
|---|---|
| TOOL_JOB_CREATED | 工具执行 Job 已创建 |
| TOOL_JOB_RUNNING | 工具执行 Job 运行中 |
| TOOL_JOB_COMPLETED | 工具执行 Job 完成 |
| TOOL_JOB_FAILED | 工具执行 Job 失败 |
| TOOL_JOB_CANCELED | 工具执行 Job 取消 |
| TOOL_JOB_RETRIED | 工具执行 Job 重试 |

日志不得包含：

- API Key
- Token
- 密码
- 本地绝对路径
- 真实 patch 内容中的敏感数据

## 13. 前端 API

修改：

```text
frontend/src/modules/task/api.ts
```

或继续复用：

```text
frontend/src/modules/tool/api.ts
```

新增类型：

```ts
export interface ToolExecutionJob {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  stepId: string | null
  toolExecutionId: string
  toolKey: string
  status: string
  priority: string
  retryCount: number
  maxRetryCount: number
  requestPayload: string | null
  resultPayload: string | null
  lastError: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number
  createTime: string
  updateTime: string
}
```

更新 ToolSandboxExecutionResponse：

```ts
jobId?: string | null
job?: ToolExecutionJob | null
```

新增 API：

```ts
export function getToolExecutionJob(jobId: string)
export function getToolExecutionJobsByExecution(executionId: string)
export function getToolExecutionJobsByRun(runId: string)
export function retryToolExecutionJob(jobId: string, reason?: string)
export function cancelToolExecutionJob(jobId: string)
```

## 14. 前端 UI

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

### 14.1 工具卡片 Job 状态

每个 tool execution card 展示：

- Job 状态
- retryCount / maxRetryCount
- durationMs
- startedAt / finishedAt

状态标签：

| status | UI |
|---|---|
| PENDING | 灰色 |
| RUNNING | 黄色 |
| COMPLETED | 绿色 |
| FAILED | 红色 |
| CANCELED | 灰色 |

data-testid：

- `tool-job-status`
- `tool-job-retry-count`
- `tool-job-duration`

### 14.2 Job 详情折叠

展示：

- requestPayload
- resultPayload
- lastError

data-testid：

- `tool-job-detail`
- `tool-job-request-payload`
- `tool-job-result-payload`

### 14.3 Retry / Cancel 按钮

如果 job.status = FAILED / CANCELED：

- 显示「重试」

如果 job.status = PENDING / RUNNING：

- 显示「取消」

当前阶段同步 drain 很快，PENDING/RUNNING 按钮可能不常见，但 UI 结构要具备。

data-testid：

- `btn-retry-tool-job`
- `btn-cancel-tool-job`

## 15. 可选独立队列视图

本阶段不强制新增独立页面。

如果实现，建议：

```text
frontend/src/modules/tool/pages/ToolExecutionJobPage.vue
```

但为了控制范围，推荐只在 MultiAgentRunPanel 中展示。

## 16. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionJobIntegrationTest.java
```

测试不少于 18 个：

### Job Creation / Drain

1. allowed tool execution 创建 job。
2. job 最终状态 COMPLETED。
3. execution response 包含 jobId。
4. run tool executions 中每条 completed execution 有 job。
5. job requestPayload 包含 parameters。
6. job resultPayload 包含 mock=true / filesTouched=[] / gitOperations=[]。
7. task logs 包含 TOOL_JOB_CREATED / TOOL_JOB_COMPLETED。

### Approval + Job

8. HIGH 工具 WAITING_APPROVAL 时不创建 completed job。
9. approve 后创建 job。
10. approve 后 job COMPLETED。
11. patch proposal artifact 仍正常生成。

### API

12. GET job detail 成功。
13. GET execution jobs 成功。
14. GET run jobs 成功。
15. 未登录 GET job 返回 UNAUTHORIZED。
16. 无效 jobId 返回 NOT_FOUND。

### Retry / Cancel

17. FAILED job 可 retry。
18. retry 超过 maxRetryCount 返回 CONFLICT。
19. PENDING job 可 cancel。
20. COMPLETED job cancel 返回 CONFLICT。
21. 非 MAINTAINER retry 返回 PROJECT_ACCESS_DENIED。

说明：

- 如果很难自然制造 FAILED job，可在测试中直接插入 ToolExecutionJobEntity。
- 不要通过真实异常执行 shell 来制造失败。

全量后端质量门：

```bash
cd backend
mvn test
```

## 17. 前端 E2E

修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

新增测试：

1. 启动 Multi-Agent Run 后工具卡片显示 Job 状态。
2. Job 状态为 COMPLETED。
3. 显示 retryCount / duration。
4. 展开 Job detail 可看到 requestPayload / resultPayload。
5. 审批 MOCK_PATCH_PROPOSAL 后显示 Job COMPLETED。
6. 页面无 JS error。

如果实现 retry/cancel 可见场景困难，可通过 API 直接创建失败 job 不建议；E2E 以展示为主，retry/cancel 由后端测试覆盖。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 18. 文档与报告

完成后新增：

```text
docs/milestone-36f-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. tool_execution_job 表说明
3. ToolExecutionJobService 设计说明
4. 同步 Mock drain 说明
5. Retry / Cancel 规则说明
6. ToolSandboxExecutionService 集成说明
7. 后端 API 清单
8. 前端 Job 状态展示说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36G

## 19. 验收标准

必须满足：

- `tool_execution_job` 表存在。
- 每个 completed tool execution 有 job 记录。
- approval approve 后创建 job 并完成。
- job status 支持 PENDING / RUNNING / COMPLETED / FAILED / CANCELED。
- job response 包含 retryCount / maxRetryCount / durationMs。
- ToolSandboxExecutionResponse 包含 latest job。
- GET job detail 可用。
- GET execution jobs 可用。
- GET run jobs 可用。
- retry / cancel API 存在并有权限控制。
- 前端工具卡片展示 Job 状态。
- 前端可展开查看 requestPayload / resultPayload。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。
- 无真实 shell / Git / 文件写入。

## 20. 已知非目标

本阶段不做：

- RabbitMQ / Redis Queue
- 异步 Worker
- 后台调度
- 分布式锁
- Worker 心跳
- 并发抢占
- 真实工具执行
- 真实文件写入
- 真实 patch apply
- Job dashboard 独立页面

这些可进入后续：

- 36G: Read-only Repository Tooling
- 36H: Patch Review UI
- 36I: Tool Parameter Advanced Schema
- 37A: Async Worker Queue with Redis / RabbitMQ

## 21. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36F。

文档路径：
docs/milestone-36f-sandbox-worker-queue.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A-36E 的 Tool Sandbox / Policy / Approval / Patch Proposal / Parameters 基础上，新增 Sandbox Worker Queue。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要写真实代码文件。
6. 不要应用 patch。
7. 不要引入 RabbitMQ / Redis Queue / Kafka。
8. 不要引入真实异步 Worker 或后台调度。
9. 当前阶段使用数据库 Job + 同步 Mock drain。
10. 不要破坏 36A-36E 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V25 tool_execution_job migration。
2. 新增 ToolExecutionJobStatus / ToolExecutionJobPriority。
3. 新增 ToolExecutionJobEntity / Mapper / DTO。
4. 新增 ToolExecutionJobService。
5. ToolSandboxExecutionService 改为创建 job 并同步 mock drain。
6. approve MOCK_PATCH_PROPOSAL 后创建 job，再生成 PATCH_PROPOSAL artifact。
7. ToolSandboxExecutionResponse 增加 latest job 信息。
8. 新增 API：
   - GET /api/tool-execution-jobs/{jobId}
   - GET /api/tool-sandbox-executions/{executionId}/jobs
   - GET /api/multi-agent-runs/{runId}/tool-execution-jobs
   - POST /api/tool-execution-jobs/{jobId}/retry
   - POST /api/tool-execution-jobs/{jobId}/cancel
9. 写入 task logs：TOOL_JOB_CREATED / TOOL_JOB_RUNNING / TOOL_JOB_COMPLETED / TOOL_JOB_FAILED / TOOL_JOB_CANCELED / TOOL_JOB_RETRIED。
10. 前端 task/tool API 增加 ToolExecutionJob 类型和接口。
11. MultiAgentRunPanel 工具卡片显示 Job 状态、retryCount、duration、requestPayload、resultPayload。
12. 后端测试不少于 18 个。
13. 前端 E2E 覆盖 Job 状态展示。
14. 新增 docs/milestone-36f-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. tool_execution_job 表说明
3. ToolExecutionJobService 设计说明
4. 同步 Mock drain 说明
5. Retry / Cancel 规则说明
6. ToolSandboxExecutionService 集成说明
7. 后端 API 清单
8. 前端 Job 状态展示说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36G

现在开始实现，不要只给计划。
```
