# Milestone 37C: Worker DLQ / Retry Backoff

## 1. 背景

Milestone 37A-37B 已经完成：

- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index

当前工具执行已经从同步 Mock drain 升级为可异步调度的 Worker Queue，并且具备只读代码索引与搜索能力。但异步执行体系还缺少生产级失败处理：

- 消费失败后的 Dead Letter Queue
- 重试退避策略
- 卡在 RUNNING 的 Job 恢复
- 失败原因结构化记录
- 管理员查看失败 / DLQ / 手动重试

Milestone 37C 的目标是新增 **Worker DLQ / Retry Backoff**：

```text
Tool Execution Job
  -> Worker process
  -> failure
  -> retry with backoff
  -> exhausted
  -> DLQ
  -> admin inspect / manual retry
```

本阶段仍不执行真实 shell，不做 Git 写操作，不写真实代码文件。失败恢复只服务于 Mock / Read-only 工具执行基础设施。

## 2. 总目标

实现异步工具执行的失败恢复闭环：

1. 新增 DLQ 配置与队列。
2. 新增 retry backoff 策略。
3. Job 增加 errorCode / failureStage / nextRetryAt / deadLetteredAt。
4. Worker 失败时按策略重试。
5. 超过重试次数后进入 DLQ。
6. 支持 RUNNING 超时恢复。
7. 提供 Admin API 查询失败 Job / DLQ / 手动重试。
8. 前端 Observability 或 Tool Job UI 展示失败与 DLQ 状态。
9. 补齐后端测试与前端 E2E。

完成后，异步工具执行从：

```text
PENDING -> RUNNING -> COMPLETED / FAILED
```

升级为：

```text
PENDING -> RUNNING -> RETRY_PENDING -> RUNNING -> FAILED / DEAD_LETTERED
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不执行 git checkout / git pull / git apply / git add / git commit / git push。
4. 不写真实代码文件。
5. 不应用 patch。
6. 不引入复杂分布式调度系统。
7. 不做 worker autoscaling。
8. 不做多租户 worker 隔离。
9. 不做 Prometheus/Grafana 强制集成。
10. 不破坏 37A queue 基础 API。
11. 不破坏 36A-37B 已有 API。
12. 不破坏 test profile 的稳定性。
13. 前端保持中文暗色科技风 UI。

允许做：

- RabbitMQ DLQ / DLX 配置。
- 数据库字段扩展。
- 简单 retry backoff。
- 手动 retry API。
- RUNNING 超时扫描 API 或服务方法。
- 前端失败状态展示。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V31__alter_tool_execution_job_dlq_retry_fields.sql
```

如果 V31 已存在，请顺延。

### 4.1 tool_execution_job 新增字段

```sql
ALTER TABLE tool_execution_job
    ADD COLUMN error_code VARCHAR(64) NULL AFTER last_error,
    ADD COLUMN failure_stage VARCHAR(64) NULL AFTER error_code,
    ADD COLUMN next_retry_at DATETIME NULL AFTER failure_stage,
    ADD COLUMN dead_lettered_at DATETIME NULL AFTER next_retry_at,
    ADD COLUMN dead_letter_reason TEXT NULL AFTER dead_lettered_at,
    ADD COLUMN source_job_id BIGINT NULL AFTER dead_letter_reason,
    ADD INDEX idx_tool_job_next_retry(next_retry_at),
    ADD INDEX idx_tool_job_dead_lettered(dead_lettered_at),
    ADD INDEX idx_tool_job_error_code(error_code);
```

字段说明：

| 字段 | 说明 |
|---|---|
| error_code | 结构化错误码 |
| failure_stage | 失败阶段，例如 PUBLISH / CONSUME / MOCK_EXECUTE / ARTIFACT |
| next_retry_at | 下一次重试时间 |
| dead_lettered_at | 进入 DLQ 时间 |
| dead_letter_reason | DLQ 原因 |
| source_job_id | 手动重试时指向原始失败 job |

## 5. 状态与枚举

修改：

```text
ToolExecutionJobStatus.java
```

新增：

```java
RETRY_PENDING,
DEAD_LETTERED
```

最终状态：

```java
PENDING,
RUNNING,
RETRY_PENDING,
COMPLETED,
FAILED,
CANCELED,
DEAD_LETTERED
```

新增：

```text
ToolExecutionErrorCode.java
ToolExecutionFailureStage.java
```

### 5.1 ToolExecutionErrorCode

```java
public enum ToolExecutionErrorCode {
    PUBLISH_FAILED,
    MESSAGE_INVALID,
    JOB_NOT_FOUND,
    JOB_CANCELED,
    MOCK_EXECUTION_FAILED,
    POLICY_BLOCKED,
    APPROVAL_REQUIRED,
    ARTIFACT_CREATE_FAILED,
    TIMEOUT,
    UNKNOWN
}
```

### 5.2 ToolExecutionFailureStage

```java
public enum ToolExecutionFailureStage {
    CREATE_JOB,
    PUBLISH,
    CONSUME,
    LOCK,
    POLICY_CHECK,
    MOCK_EXECUTE,
    ARTIFACT,
    COMPLETE
}
```

## 6. RabbitMQ DLQ 配置

修改：

```text
ToolExecutionQueueConfig.java
```

新增：

```text
tool.execution.dlx
tool.execution.dlq
tool.execution.dead
```

配置项：

```yaml
app:
  tool-worker:
    exchange: ${TOOL_WORKER_EXCHANGE:tool.execution.exchange}
    queue: ${TOOL_WORKER_QUEUE:tool.execution.queue}
    routing-key: ${TOOL_WORKER_ROUTING_KEY:tool.execution.run}
    dead-letter-exchange: ${TOOL_WORKER_DLX:tool.execution.dlx}
    dead-letter-queue: ${TOOL_WORKER_DLQ:tool.execution.dlq}
    dead-letter-routing-key: ${TOOL_WORKER_DLQ_ROUTING_KEY:tool.execution.dead}
```

主队列 arguments：

```java
x-dead-letter-exchange = tool.execution.dlx
x-dead-letter-routing-key = tool.execution.dead
```

注意：

- test profile queueEnabled=false，不强制连接 RabbitMQ。
- 生产配置可开启。

## 7. Retry Backoff 策略

新增：

```text
ToolExecutionRetryPolicy.java
```

默认策略：

| retryCount | delay |
|---|---|
| 0 -> 1 | 5 秒 |
| 1 -> 2 | 30 秒 |
| 2 -> 3 | 120 秒 |

配置：

```yaml
app:
  tool-worker:
    retry-delays-seconds: ${TOOL_WORKER_RETRY_DELAYS_SECONDS:5,30,120}
```

方法：

```java
public boolean canRetry(ToolExecutionJobEntity job)
public LocalDateTime nextRetryAt(ToolExecutionJobEntity job)
public long nextDelaySeconds(ToolExecutionJobEntity job)
```

规则：

- retryCount < maxRetryCount 才可重试。
- CANCELED 不自动重试。
- POLICY_BLOCKED 不自动重试。
- APPROVAL_REQUIRED 不自动重试。
- MESSAGE_INVALID 不自动重试。
- MOCK_EXECUTION_FAILED / TIMEOUT / UNKNOWN 可重试。

## 8. ToolExecutionJobService 改造

新增方法：

```java
public ToolExecutionJobResponse markFailedWithRetry(Long jobId, ToolExecutionErrorCode errorCode, ToolExecutionFailureStage stage, String message)
public ToolExecutionJobResponse moveToDeadLetter(Long jobId, String reason)
public List<ToolExecutionJobResponse> listFailedJobs(Long projectId, String status)
public ToolExecutionJobResponse manualRetry(Long jobId, String reason)
public int recoverTimedOutRunningJobs(Duration timeout)
```

### 8.1 markFailedWithRetry

行为：

1. 记录 errorCode / failureStage / lastError。
2. 如果 canRetry：
   - status = RETRY_PENDING
   - nextRetryAt = now + backoff
3. 否则：
   - status = DEAD_LETTERED
   - deadLetteredAt = now
   - deadLetterReason = message

### 8.2 manualRetry

行为：

1. 仅 MAINTAINER+。
2. 允许状态：
   - FAILED
   - DEAD_LETTERED
   - CANCELED
3. 创建新 job：
   - sourceJobId = old.id
   - retryCount = old.retryCount + 1
   - status = PENDING
4. publish 或 sync drain。
5. 保留旧 job 历史。

### 8.3 recoverTimedOutRunningJobs

行为：

1. 查找 RUNNING 且 lockedAt / startedAt 超过 timeout 的 job。
2. 调用 markFailedWithRetry(errorCode=TIMEOUT, stage=MOCK_EXECUTE)。
3. 返回恢复数量。

本阶段不强制定时执行，可通过 Admin API 手动触发。

## 9. Worker 消费失败处理

修改：

```text
ToolExecutionJobConsumer.java
ToolExecutionWorkerService.java
```

要求：

1. 消费消息时捕获异常。
2. 不向日志输出敏感 payload。
3. 对可重试异常调用 markFailedWithRetry。
4. 对不可重试异常 moveToDeadLetter。
5. Rabbit listener 不应无限重投同一消息。

建议：

- 应用层处理失败并 ack 消息。
- 数据库 job 状态作为事实来源。
- 不依赖 Rabbit 自动 requeue。

## 10. Retry Dispatcher

本阶段不做后台调度。

新增可选 API：

```text
POST /api/tool-execution-jobs/dispatch-retries
```

权限 ADMIN。

行为：

1. 查找 RETRY_PENDING 且 nextRetryAt <= now。
2. 将其重新 publish。
3. status 改回 PENDING。
4. 返回 dispatch count。

如果不想新增该 API，可在 manualRetry 覆盖手动恢复场景。但建议实现，便于测试和运维。

## 11. 后端 API

新增或扩展 Controller：

```text
ToolExecutionJobController.java
```

新增端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/tool-execution-jobs/failed?status=` | MAINTAINER+ | 查询失败 / DLQ / RETRY_PENDING |
| POST | `/api/tool-execution-jobs/{jobId}/manual-retry` | MAINTAINER+ | 手动重试 |
| POST | `/api/tool-execution-jobs/recover-timeouts` | ADMIN | 恢复超时 RUNNING job |
| POST | `/api/tool-execution-jobs/dispatch-retries` | ADMIN | 派发到期重试 |

响应示例：

```json
{
  "code": "OK",
  "data": {
    "id": "123",
    "status": "RETRY_PENDING",
    "errorCode": "TIMEOUT",
    "failureStage": "MOCK_EXECUTE",
    "nextRetryAt": "2026-05-21T12:00:00"
  }
}
```

## 12. DTO 增强

修改：

```text
ToolExecutionJobResponse.java
```

新增字段：

- errorCode
- failureStage
- nextRetryAt
- deadLetteredAt
- deadLetterReason
- sourceJobId

前端同步类型。

## 13. 前端 API

修改：

```text
frontend/src/modules/task/api.ts
```

或 `frontend/src/modules/tool/api.ts`。

新增字段：

```ts
errorCode?: string | null
failureStage?: string | null
nextRetryAt?: string | null
deadLetteredAt?: string | null
deadLetterReason?: string | null
sourceJobId?: string | null
```

新增函数：

```ts
listFailedToolExecutionJobs(projectId: string, status?: string)
manualRetryToolExecutionJob(jobId: string, reason?: string)
recoverTimedOutToolExecutionJobs()
dispatchRetryToolExecutionJobs()
```

## 14. 前端 UI

### 14.1 MultiAgentRunPanel

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

展示：

- RETRY_PENDING 状态
- DEAD_LETTERED 状态
- errorCode
- failureStage
- nextRetryAt
- deadLetterReason
- 手动重试按钮

data-testid：

- `tool-job-error-code`
- `tool-job-failure-stage`
- `tool-job-next-retry-at`
- `tool-job-dead-lettered`
- `btn-manual-retry-tool-job`

### 14.2 Observability 可选增强

可选修改：

```text
frontend/src/modules/admin/pages/ObservabilityPage.vue
```

新增 Tool Worker 小面板：

- RETRY_PENDING count
- DEAD_LETTERED count
- FAILED count

如果范围过大，可只在 MultiAgentRunPanel 展示。

## 15. 配置更新

修改：

```text
.env.example
deploy/docker-compose.app.yml
deploy/prod/docker-compose.prod.yml
docs/environment-variable-index.md
```

新增：

```dotenv
TOOL_WORKER_DLX=tool.execution.dlx
TOOL_WORKER_DLQ=tool.execution.dlq
TOOL_WORKER_DLQ_ROUTING_KEY=tool.execution.dead
TOOL_WORKER_RETRY_DELAYS_SECONDS=5,30,120
TOOL_WORKER_RUNNING_TIMEOUT_SECONDS=300
```

## 16. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionRetryDlqIntegrationTest.java
```

测试不少于 22 个：

### Retry policy

1. retryCount=0 next delay 5s。
2. retryCount=1 next delay 30s。
3. retryCount=2 next delay 120s。
4. retryCount >= maxRetryCount 不可重试。
5. POLICY_BLOCKED 不可自动重试。
6. TIMEOUT 可重试。

### Job state

7. markFailedWithRetry 可进入 RETRY_PENDING。
8. 超过次数进入 DEAD_LETTERED。
9. DEAD_LETTERED 填充 deadLetteredAt。
10. DEAD_LETTERED 填充 deadLetterReason。
11. recoverTimedOutRunningJobs 恢复 RUNNING 超时 job。
12. CANCELED job 不自动重试。

### API

13. list failed jobs 返回 RETRY_PENDING。
14. list failed jobs 返回 DEAD_LETTERED。
15. manual retry 创建新 job。
16. manual retry 设置 sourceJobId。
17. non-maintainer manual retry 返回 PROJECT_ACCESS_DENIED。
18. recover timeouts 需要 ADMIN。
19. dispatch retries 需要 ADMIN。
20. dispatch retries 将到期 RETRY_PENDING 改为 PENDING 或 publish。

### Safety

21. retry 后 outputPayload 仍包含 filesTouched=[]。
22. retry 后 outputPayload 仍包含 gitOperations=[]。
23. worker failure 不泄露 sensitive payload。

全量后端质量门：

```bash
cd backend
mvn test
```

## 17. 前端 E2E

新增或修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. Tool job 可显示 RETRY_PENDING 状态。
2. Tool job 可显示 DEAD_LETTERED 状态。
3. errorCode / failureStage 可见。
4. 手动重试按钮可见。
5. 页面无 JS error。

如果制造真实失败成本过高：

- 可通过后端测试覆盖 retry/dlq 逻辑。
- E2E 只验证 UI 对字段兼容。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 18. 文档与报告

完成后新增：

```text
docs/milestone-37c-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. DLQ / Retry Backoff 架构说明
3. 数据库字段说明
4. RabbitMQ DLQ 配置说明
5. RetryPolicy 说明
6. ToolExecutionJobService 改造说明
7. Worker 失败处理说明
8. 后端 API 清单
9. 前端失败状态展示说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37D

## 19. 验收标准

必须满足：

- tool_execution_job 包含 errorCode / failureStage / nextRetryAt / deadLetteredAt。
- ToolExecutionJobStatus 包含 RETRY_PENDING / DEAD_LETTERED。
- Retry backoff 策略可测试。
- 超过重试次数进入 DEAD_LETTERED。
- RUNNING 超时可恢复。
- manual retry 可创建新 job。
- non-maintainer 不能 manual retry。
- RabbitMQ DLQ 配置存在且默认不影响 test profile。
- 前端可展示 RETRY_PENDING / DEAD_LETTERED。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。
- 无真实 shell / Git / 文件写入。

## 20. 已知非目标

本阶段不做：

- RabbitMQ delayed exchange 插件
- 自动后台 retry dispatcher
- Prometheus metrics
- Grafana dashboard
- Worker autoscaling
- 分布式锁
- 多 worker 竞争控制
- Job dashboard 独立页面
- 真实工具执行

这些可进入后续：

- 37D: Tool Execution Metrics Dashboard
- 37E: Real Read-only Tool Adapter Hardening
- 38A: Code Search Semantic RAG
- 38B: Worker Autoscaling / DLQ Operations

## 21. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37C。

文档路径：
docs/milestone-37c-worker-dlq-retry-backoff.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 37A Async Worker Queue 基础上，新增 Worker DLQ / Retry Backoff。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要应用 patch。
8. Worker 仍然只执行 Mock / Read-only 工具。
9. test profile 必须保持稳定，不强制 RabbitMQ 连接。
10. 不要破坏 36A-37B 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V31 migration 扩展 tool_execution_job retry/dlq 字段。
2. ToolExecutionJobStatus 新增 RETRY_PENDING / DEAD_LETTERED。
3. 新增 ToolExecutionErrorCode / ToolExecutionFailureStage。
4. RabbitMQ config 增加 DLX / DLQ。
5. 新增 ToolExecutionRetryPolicy。
6. ToolExecutionJobService 增加 markFailedWithRetry / moveToDeadLetter / manualRetry / recoverTimedOutRunningJobs。
7. Worker failure 走 retry backoff 或 dead letter。
8. 新增 API：
   - GET /api/projects/{projectId}/tool-execution-jobs/failed?status=
   - POST /api/tool-execution-jobs/{jobId}/manual-retry
   - POST /api/tool-execution-jobs/recover-timeouts
   - POST /api/tool-execution-jobs/dispatch-retries
9. ToolExecutionJobResponse 增加 errorCode / failureStage / nextRetryAt / deadLetteredAt / deadLetterReason / sourceJobId。
10. 前端 MultiAgentRunPanel 展示 RETRY_PENDING / DEAD_LETTERED / errorCode / failureStage / manual retry。
11. 配置文件和 .env.example 增加 DLQ / retry backoff 参数。
12. 后端测试不少于 22 个。
13. 前端 E2E 覆盖失败状态展示。
14. 新增 docs/milestone-37c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. DLQ / Retry Backoff 架构说明
3. 数据库字段说明
4. RabbitMQ DLQ 配置说明
5. RetryPolicy 说明
6. ToolExecutionJobService 改造说明
7. Worker 失败处理说明
8. 后端 API 清单
9. 前端失败状态展示说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37D

现在开始实现，不要只给计划。
```
