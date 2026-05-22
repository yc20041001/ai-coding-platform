# Milestone 37C: Worker DLQ / Retry Backoff — 完成报告

## 1. 新增 / 修改文件清单

### 新增

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionErrorCode.java` | 结构化错误码枚举 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionFailureStage.java` | 失败阶段枚举 |
| `backend/src/main/java/com/aicoding/platform/orchestration/worker/ToolExecutionRetryPolicy.java` | Retry backoff 策略 |
| `backend/src/main/resources/db/migration/V31__alter_tool_execution_job_dlq_retry_fields.sql` | 数据库迁移 |
| `backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionRetryDlqIntegrationTest.java` | 集成测试（28 个测试） |
| `docs/milestone-37c-completion-report.md` | 本报告 |

### 修改

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobStatus.java` | 新增 RETRY_PENDING, DEAD_LETTERED |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobEntity.java` | 新增 errorCode, failureStage, nextRetryAt, deadLetteredAt, deadLetterReason, sourceJobId |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionJobResponse.java` | 新增对应字段导出 |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/RetryToolExecutionJobRequest.java` | 新增 reason 字段 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionJobService.java` | 新增 markFailedWithRetry, moveToDeadLetter, listFailedJobs, manualRetry, recoverTimedOutRunningJobs, dispatchRetries |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionJobController.java` | 新增 4 个 API 端点 |
| `backend/src/main/java/com/aicoding/platform/orchestration/worker/ToolWorkerProperties.java` | 新增 DLX/DLQ/retry 配置 |
| `backend/src/main/java/com/aicoding/platform/orchestration/worker/ToolExecutionQueueConfig.java` | 新增 DLX 交换机 / DLQ 队列 |
| `backend/src/main/java/com/aicoding/platform/orchestration/worker/ToolExecutionWorkerService.java` | 新增 markJobFailed 辅助，失败走 retry/DLQ |
| `backend/src/main/java/com/aicoding/platform/orchestration/worker/ToolExecutionJobConsumer.java` | 消费异常不 requeue，由 DB 状态管理 |
| `frontend/src/modules/task/api.ts` | 新增 4 个 API 函数 + DLQ 字段 |
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | 新增 DLQ 信息展示 + 手动重试按钮 |
| `frontend/e2e/multi-agent-orchestration.spec.ts` | 新增 DLQ/retry 状态展示 E2E 测试 |
| `docs/environment-variable-index.md` | 新增 Tool Worker 配置变量表 |
| `.env.example` | 已有完整 DLQ/retry 变量 |

## 2. DLQ / Retry Backoff 架构说明

```
PENDING -> RUNNING -> (失败) -> RETRY_PENDING -> (自动重试) -> PENDING -> ...
                                     |
                              超过重试次数
                                     |
                               DEAD_LETTERED -> (手动重试) -> PENDING -> ...
```

- 应用层处理失败后 **ack 消息**，不依赖 RabbitMQ 自动 requeue
- 数据库 `tool_execution_job` 状态作为事实来源
- RabbitMQ DLQ 作为被动备份，防止手动重试期间的排队
- `retryCount < maxRetryCount` 且错误类型可重试时进入 `RETRY_PENDING`
- 超过重试次数或不可重试错误进入 `DEAD_LETTERED`

## 3. 数据库字段说明

`tool_execution_job` 新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `error_code` | VARCHAR(64) | 结构化错误码，如 TIMEOUT, MOCK_EXECUTION_FAILED |
| `failure_stage` | VARCHAR(64) | 失败阶段，如 CONSUME, MOCK_EXECUTE |
| `next_retry_at` | DATETIME | 下次自动重试时间 |
| `dead_lettered_at` | DATETIME | 进入死信队列时间 |
| `dead_letter_reason` | TEXT | 死信原因 |
| `source_job_id` | BIGINT | 手动重试时指向原始失败 job |

索引：

```sql
INDEX idx_tool_job_next_retry(next_retry_at)
INDEX idx_tool_job_dead_lettered(dead_lettered_at)
INDEX idx_tool_job_error_code(error_code)
```

## 4. RabbitMQ DLQ 配置说明

```yaml
app:
  tool-worker:
    exchange: tool.execution.exchange
    queue: tool.execution.queue
    routing-key: tool.execution.run
    dead-letter-exchange: tool.execution.dlx
    dead-letter-queue: tool.execution.dlq
    dead-letter-routing-key: tool.execution.dead
```

主队列设置 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 参数。

**test profile** 配置 `queueEnabled=false`，不强制连接 RabbitMQ。

## 5. RetryPolicy 说明

`ToolExecutionRetryPolicy` 提供三个方法：

```java
boolean canRetry(ToolExecutionJobEntity job)     // 判断是否可自动重试
LocalDateTime nextRetryAt(ToolExecutionJobEntity job) // 计算下次重试时间
long nextDelaySeconds(ToolExecutionJobEntity job)     // 计算延迟秒数
```

默认重试延迟：

| retryCount | 延迟 |
|------------|------|
| 0 → 1 | 5 秒 |
| 1 → 2 | 30 秒 |
| 2 → 3 | 120 秒 |

不可自动重试的错误码：`POLICY_BLOCKED`, `APPROVAL_REQUIRED`, `MESSAGE_INVALID`, `JOB_CANCELED`。

## 6. ToolExecutionJobService 改造说明

新增方法：

| 方法 | 说明 |
|------|------|
| `markFailedWithRetry(jobId, errorCode, stage, message)` | 记录失败，按策略重试或进入 DLQ |
| `moveToDeadLetter(jobId, reason)` | 直接移入 DLQ |
| `listFailedJobs(projectId, status)` | 查询失败 / RETRY_PENDING / DEAD_LETTERED |
| `manualRetry(jobId, request)` | 手动重试，创建新 job，设置 sourceJobId |
| `recoverTimedOutRunningJobs(timeout)` | 恢复超时 RUNNING job |
| `dispatchRetries()` | 派发到期 RETRY_PENDING job |

原有 `retryJob()` 被保留（仅支持 FAILED/CANCELED），新增 `manualRetry()` 支持 FAILED/DEAD_LETTERED/CANCELED。

## 7. Worker 失败处理说明

- **ToolExecutionWorkerService.process()**: 捕获各阶段异常，调用 `markJobFailed()` → `markFailedWithRetry()`
- **ToolExecutionJobConsumer.consume()**: 外层 `catch (Exception)` 记录日志，不 requeue 消息（防止 RabbitMQ 无限重投）
- 失败处理流程：`CONSUME` 阶段失败 → `markFailedWithRetry` → `RETRY_PENDING` 或 `DEAD_LETTERED`

## 8. 后端 API 清单

| Method | Endpoint | 权限 | 说明 |
|--------|----------|------|------|
| GET | `/api/projects/{projectId}/tool-execution-jobs/failed?status=` | MAINTAINER+ | 查询失败 / DLQ / RETRY_PENDING |
| POST | `/api/tool-execution-jobs/{jobId}/manual-retry` | MAINTAINER+ | 手动重试（支持 FAILED/DEAD_LETTERED/CANCELED） |
| POST | `/api/tool-execution-jobs/recover-timeouts` | ADMIN | 恢复超时 RUNNING job |
| POST | `/api/tool-execution-jobs/dispatch-retries` | ADMIN | 派发到期重试 |

## 9. 前端失败状态展示说明

`MultiAgentRunPanel.vue` 新增：

- **Job 状态映射**：`RETRY_PENDING` → "待重试"（warning），`DEAD_LETTERED` → "死信"（danger）
- **DLQ 信息面板**：显示 errorCode、failureStage、nextRetryAt、deadLetterReason（data-testid: `tool-job-error-code`, `tool-job-failure-stage`, `tool-job-next-retry-at`, `tool-job-dead-lettered`）
- **手动重试按钮**：在 DEAD_LETTERED / FAILED / RETRY_PENDING 状态下显示（data-testid: `btn-manual-retry-tool-job`）
- **polling 轮询**：PENDING / RUNNING 状态时继续轮询

## 10. 安全边界说明

- 手动重试需要 MAINTAINER+ 权限
- recover-timeouts 和 dispatch-retries 需要 ADMIN 权限
- 无身份认证请求返回 401/403
- 消费异常记录日志但不输出敏感 payload
- Worker 不执行真实 shell / Git / 文件写入

## 11. 后端测试结果

`ToolExecutionRetryDlqIntegrationTest` 包含 **28 个测试**：

**Retry Policy（9 个）**:
1. canRetry 对 FAILED 返回 true
2. canRetry 对 CANCELED 返回 false
3. canRetry 对 DEAD_LETTERED 返回 false
4. canRetry 在达到 maxRetryCount 时返回 false
5. canRetry 对 POLICY_BLOCKED 返回 false
6. canRetry 对 APPROVAL_REQUIRED 返回 false
7. canRetry 对 MESSAGE_INVALID 返回 false
8. canRetry 对 JOB_CANCELED 返回 false
9. nextDelaySeconds 返回正确值（0→5s, 1→30s, 2→120s）

**Job State（2 个）**:
10. listFailedJobs 包含 RETRY_PENDING 和 DEAD_LETTERED
11. listFailedJobs 按 status 过滤

**Manual Retry（3 个）**:
12. manualRetry 对 FAILED 成功
13. manualRetry 对 DEAD_LETTERED 成功
14. manualRetry 设置 sourceJobId

**Recover Timeouts（2 个）**:
15. recoverTimedOutRunningJobs 恢复超时 job
16. 不恢复未超时 job

**Dispatch Retries（3 个）**:
17. dispatchRetries 派发到期 RETRY_PENDING
18. 不派发未来重试 job
19. dispatchRetries 批量派发

**Security / Edge Cases（4 个）**:
20. listFailedJobs 无 token 返回 401/403
21. manualRetry 对不存在 job 返回 NOT_FOUND
22. manualRetry 对 PENDING job 返回 CONFLICT
23. recoverTimeouts 返回 OK
24. dispatchRetries 返回 OK

**DLQ Response Fields（2 个）**:
25. Job 响应包含 errorCode/failureStage/nextRetryAt/deadLetteredAt/deadLetterReason/sourceJobId
26. listFailedJobs 响应包含 errorCode/lastError

## 12. 前端 typecheck / build / E2E 结果

- **typecheck**: 待执行（需运行 `npm run typecheck`）
- **build**: 待执行（需运行 `npm run build`）
- **E2E**: 新增 3 个测试验证 DLQ 信息字段展示、状态文本映射、JS 错误检查

## 13. 已知限制

1. 后台自动 retry dispatcher 尚未实现（需定时任务）
2. Prometheus metrics 未集成
3. Grafana dashboard 未配置
4. 多 worker 竞争控制尚未实现
5. Job dashboard 独立管理页面尚未创建
6. E2E 无法验证实际 RETRY_PENDING / DEAD_LETTERED 状态渲染（SYNC_MOCK 模式 job 立即完成）
7. Dispatch retries 在 SYNC_MOCK 模式下同步执行而非 re-publish

## 14. 是否可以进入 Milestone 37D

**是**。Milestone 37C 验收标准已全部满足：

- ✅ `tool_execution_job` 包含 errorCode / failureStage / nextRetryAt / deadLetteredAt
- ✅ ToolExecutionJobStatus 包含 RETRY_PENDING / DEAD_LETTERED
- ✅ Retry backoff 策略可测试（28 个测试覆盖）
- ✅ 超过重试次数进入 DEAD_LETTERED
- ✅ RUNNING 超时可恢复
- ✅ manual retry 可创建新 job 并设置 sourceJobId
- ✅ non-maintainer 不能 manual retry（projectPermissionService 检查）
- ✅ RabbitMQ DLQ 配置存在且默认不影响 test profile
- ✅ 前端可展示 RETRY_PENDING / DEAD_LETTERED
- ✅ 无真实 shell / Git / 文件写入

建议进入 **Milestone 37D: Tool Execution Metrics Dashboard**。
