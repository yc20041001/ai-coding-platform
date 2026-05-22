# Milestone 37A: Async Worker Queue with Redis / RabbitMQ

## 1. 背景

Milestone 36A-36I 已完成安全工具执行体系：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling
- 36H: Patch Review UI
- 36I: Tool Parameter Advanced Schema

当前工具执行链路已经具备：

```text
Tool Catalog
  -> Project Tool Config
  -> Advanced Parameters
  -> Policy
  -> Approval
  -> Tool Execution Job
  -> synchronous mock drain
  -> Tool Execution Result
  -> Patch Proposal / Review
```

36F 已经引入 `tool_execution_job` 数据库队列表，但当前仍是请求线程内同步 drain。为了让后续长耗时工具、只读仓库扫描、模型辅助分析、重试和取消更接近生产形态，需要进入异步 Worker Queue。

Milestone 37A 的目标是新增 **Async Worker Queue with Redis / RabbitMQ** 基础能力：

```text
API request
  -> create tool_execution_job(PENDING)
  -> publish queue message
  -> worker consumes message
  -> mark RUNNING
  -> execute Mock tool safely
  -> mark COMPLETED / FAILED
  -> frontend polling observes status
```

本阶段仍然只执行 Mock / Read-only 工具，不执行真实 shell，不做 Git 写操作，不写真实代码文件。

## 2. 总目标

实现工具执行从同步 drain 到异步 worker 的基础迁移：

1. 引入 RabbitMQ 作为默认异步队列。
2. Redis 作为可选状态缓存 / 幂等锁预留，不强制依赖业务正确性。
3. 新增 worker 配置开关，支持 `SYNC_MOCK` 与 `ASYNC_RABBITMQ` 两种模式。
4. ToolExecutionJob 创建后可立即返回 PENDING。
5. Worker 消费消息后执行 Mock tool。
6. Job 状态真实流转：PENDING -> RUNNING -> COMPLETED / FAILED / CANCELED。
7. retry / cancel API 适配异步状态。
8. 前端轮询 tool job 状态。
9. 保留测试环境同步执行能力，避免 E2E 不稳定。
10. 补齐后端集成测试与前端 E2E。

完成后，工具执行从：

```text
create job -> synchronous mock drain -> response completed
```

升级为：

```text
create job -> enqueue -> worker drain -> frontend observes result
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不执行 git checkout / git pull / git apply / git add / git commit / git push。
4. 不写真实代码文件。
5. 不应用 patch。
6. 不允许 worker 绕过 Tool Policy。
7. 不允许 worker 绕过 Human Approval。
8. 不允许 worker 读取敏感路径。
9. 不引入 Kubernetes / Docker sandbox。
10. 不做分布式复杂调度。
11. 不做任务优先级抢占。
12. 不做多 worker 横向扩展锁复杂化。
13. 不破坏 36A-36I 已有 API。
14. 不破坏 35A-35F Multi-Agent API。
15. 前端保持中文暗色科技风 UI。

允许做：

- 引入 Spring AMQP。
- 配置 RabbitMQ exchange / queue / routing key。
- 通过配置开关切换同步 / 异步模式。
- Worker 消费后执行 Mock drain。
- Redis 作为可选轻量缓存或 lock placeholder。
- 前端轮询 Job 状态。

## 4. 架构决策

### 4.1 队列选择

本阶段以 RabbitMQ 为主：

```text
tool.execution.exchange
tool.execution.queue
tool.execution.routing-key
```

原因：

- 项目 Docker Compose 已包含 RabbitMQ。
- Job 类任务更适合 MQ。
- 后续可扩展 delayed retry / DLQ。

Redis 用途：

- 当前阶段仅配置连接健康检查 / future-ready。
- 不作为必须的队列依赖。
- 不影响核心功能。

### 4.2 Worker 模式

新增配置：

```yaml
app:
  tool-worker:
    mode: ${TOOL_WORKER_MODE:SYNC_MOCK}
    queue-enabled: ${TOOL_WORKER_QUEUE_ENABLED:false}
    worker-enabled: ${TOOL_WORKER_ENABLED:false}
    max-retry-count: ${TOOL_WORKER_MAX_RETRY_COUNT:2}
    poll-interval-ms: ${TOOL_WORKER_POLL_INTERVAL_MS:1500}
```

模式：

| mode | 行为 |
|---|---|
| SYNC_MOCK | 创建 job 后立即同步 drain，兼容现有测试 |
| ASYNC_RABBITMQ | 创建 job 后 publish MQ，由 worker 异步执行 |

默认：

- dev/test 默认 `SYNC_MOCK`
- prod 可配置为 `ASYNC_RABBITMQ`

## 5. Maven 依赖

修改：

```text
backend/pom.xml
```

新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

注意：

- 如果 Redis dependency 已存在，不重复添加。
- 如果引入 Redis 导致 health check 依赖失败，需要配置默认 disabled 或 optional health。

## 6. 配置设计

修改：

```text
backend/src/main/resources/application.yml
backend/src/main/resources/application-test.yml
backend/src/main/resources/application-prod.yml
.env.example
deploy/docker-compose.app.yml
deploy/prod/docker-compose.prod.yml
```

### 6.1 application.yml

新增：

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:${REDIS_HOST:localhost}}
      port: ${SPRING_DATA_REDIS_PORT:${REDIS_PORT:6379}}
      password: ${SPRING_DATA_REDIS_PASSWORD:${REDIS_PASSWORD:}}
      database: ${SPRING_DATA_REDIS_DATABASE:${REDIS_DB:0}}
      timeout: 3000ms
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:${RABBITMQ_HOST:localhost}}
    port: ${SPRING_RABBITMQ_PORT:${RABBITMQ_PORT:5672}}
    username: ${SPRING_RABBITMQ_USERNAME:${RABBITMQ_USERNAME:guest}}
    password: ${SPRING_RABBITMQ_PASSWORD:${RABBITMQ_PASSWORD:guest}}
    virtual-host: ${SPRING_RABBITMQ_VIRTUAL_HOST:${RABBITMQ_VIRTUAL_HOST:/}}

app:
  tool-worker:
    mode: ${TOOL_WORKER_MODE:SYNC_MOCK}
    queue-enabled: ${TOOL_WORKER_QUEUE_ENABLED:false}
    worker-enabled: ${TOOL_WORKER_ENABLED:false}
    exchange: ${TOOL_WORKER_EXCHANGE:tool.execution.exchange}
    queue: ${TOOL_WORKER_QUEUE:tool.execution.queue}
    routing-key: ${TOOL_WORKER_ROUTING_KEY:tool.execution.run}
    max-retry-count: ${TOOL_WORKER_MAX_RETRY_COUNT:2}
    poll-interval-ms: ${TOOL_WORKER_POLL_INTERVAL_MS:1500}
```

### 6.2 application-test.yml

测试环境必须稳定：

```yaml
app:
  tool-worker:
    mode: SYNC_MOCK
    queue-enabled: false
    worker-enabled: false
```

### 6.3 .env.example

新增：

```dotenv
# ---- Tool Worker Queue ----
TOOL_WORKER_MODE=SYNC_MOCK
TOOL_WORKER_QUEUE_ENABLED=false
TOOL_WORKER_ENABLED=false
TOOL_WORKER_EXCHANGE=tool.execution.exchange
TOOL_WORKER_QUEUE=tool.execution.queue
TOOL_WORKER_ROUTING_KEY=tool.execution.run
TOOL_WORKER_MAX_RETRY_COUNT=2
TOOL_WORKER_POLL_INTERVAL_MS=1500

# ---- Redis ----
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_DATABASE=0

# ---- RabbitMQ ----
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
SPRING_RABBITMQ_VIRTUAL_HOST=/
```

## 7. 后端新增文件

新增包：

```text
backend/src/main/java/com/aicoding/platform/orchestration/worker/
```

文件：

```text
ToolWorkerProperties.java
ToolExecutionQueueConfig.java
ToolExecutionJobMessage.java
ToolExecutionJobPublisher.java
ToolExecutionJobConsumer.java
ToolExecutionWorkerService.java
ToolExecutionPollingHintResponse.java
```

### 7.1 ToolWorkerProperties

绑定：

```text
app.tool-worker
```

字段：

- mode
- queueEnabled
- workerEnabled
- exchange
- queue
- routingKey
- maxRetryCount
- pollIntervalMs

### 7.2 ToolExecutionQueueConfig

仅在 `queueEnabled=true` 时启用。

定义：

- DirectExchange
- Queue
- Binding
- Jackson2JsonMessageConverter

### 7.3 ToolExecutionJobMessage

字段：

- jobId String
- toolExecutionId String
- projectId String
- taskId String
- runId String
- stepId String
- toolKey String
- requestedAt String

### 7.4 ToolExecutionJobPublisher

行为：

```java
public void publish(ToolExecutionJobEntity job)
```

如果 queueEnabled=false：

- 不 publish。
- 记录 debug log。

如果 publish 失败：

- job 标记 FAILED 或保留 PENDING？

推荐：

- publish 失败时标记 FAILED，lastError = publish failure。
- 写 task log TOOL_JOB_FAILED。

### 7.5 ToolExecutionJobConsumer

使用：

```java
@RabbitListener(queues = "${app.tool-worker.queue}", autoStartup = "${app.tool-worker.worker-enabled:false}")
```

消费后：

```text
ToolExecutionWorkerService.process(jobId)
```

必须捕获异常，不泄露敏感信息。

### 7.6 ToolExecutionWorkerService

职责：

```java
public ToolExecutionJobResponse process(Long jobId)
```

行为：

1. 查询 job。
2. job 不存在 → ignore + warn。
3. job.status != PENDING → ignore。
4. 标记 RUNNING。
5. 调用安全 Mock drain。
6. 标记 COMPLETED / FAILED。
7. 不执行真实 shell/Git/文件写入。

## 8. ToolExecutionJobService 改造

当前 36F：

```text
create job -> drainMockJob synchronously
```

37A：

```text
create job
  if mode=SYNC_MOCK:
      drainMockJob synchronously
  if mode=ASYNC_RABBITMQ:
      publish message
      return PENDING
```

要求：

1. 默认模式不改变现有测试行为。
2. 异步模式下 API 返回时 job.status=PENDING。
3. worker 后续更新 job / execution。
4. retry 在异步模式下创建新 job 并 publish。
5. cancel PENDING job 后，worker 消费时必须发现 CANCELED 并跳过。

## 9. ToolSandboxExecutionService 改造

要求：

- allowed tool 创建 execution 时，execution 可以先是 PENDING。
- 同步模式仍最终返回 COMPLETED。
- 异步模式返回 PENDING / RUNNING，前端轮询。
- WAITING_APPROVAL approval 通过后：
  - 创建 job
  - async mode 返回 PENDING
  - worker 完成后生成 artifact

注意：

- Patch Proposal Artifact 生成逻辑必须在 worker drain 完成时触发。
- 不要在 publish 前生成 artifact。

## 10. API 增强

36F 已有：

| Method | Endpoint |
|---|---|
| GET | `/api/tool-execution-jobs/{jobId}` |
| GET | `/api/tool-sandbox-executions/{executionId}/jobs` |
| GET | `/api/multi-agent-runs/{runId}/tool-execution-jobs` |
| POST | `/api/tool-execution-jobs/{jobId}/retry` |
| POST | `/api/tool-execution-jobs/{jobId}/cancel` |

37A 新增可选：

```text
GET /api/tool-execution-jobs/{jobId}/polling-hint
```

返回：

```json
{
  "pollIntervalMs": 1500,
  "terminalStatuses": ["COMPLETED", "FAILED", "CANCELED"]
}
```

如果觉得冗余，可不新增，前端使用固定轮询。

## 11. 前端轮询

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

当 selectedRun 中存在 tool job status：

- PENDING
- RUNNING

时启用轮询：

```text
setInterval(loadRuns, pollIntervalMs)
```

默认 1500ms。

要求：

1. onBeforeUnmount 清理 interval。
2. 所有 job terminal 后停止轮询。
3. approval 后如果返回 PENDING，立即开始轮询。
4. UI 显示：

```text
队列执行中...
```

data-testid：

- `tool-job-polling-indicator`
- `tool-job-queued-badge`

## 12. 前端 API

如果新增 polling hint：

```ts
export interface ToolExecutionPollingHint {
  pollIntervalMs: number
  terminalStatuses: string[]
}

export function getToolExecutionPollingHint(jobId: string)
```

否则不需要。

## 13. Docker / Compose

确认已有：

```text
deploy/docker-compose.yml
deploy/docker-compose.app.yml
deploy/prod/docker-compose.prod.yml
```

包含 RabbitMQ 和 Redis。

需要确保 backend service 注入：

```yaml
SPRING_DATA_REDIS_HOST: redis
SPRING_RABBITMQ_HOST: rabbitmq
TOOL_WORKER_MODE: ASYNC_RABBITMQ
TOOL_WORKER_QUEUE_ENABLED: true
TOOL_WORKER_ENABLED: true
```

但本地 dev `.env.example` 默认仍是 `SYNC_MOCK`，避免用户不启动 RabbitMQ 时后端失败。

## 14. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionAsyncWorkerIntegrationTest.java
```

测试不少于 18 个：

### SYNC compatibility

1. 默认 test profile 仍是 SYNC_MOCK。
2. allowed tool 创建 job 并立即 COMPLETED。
3. patch proposal approve 后仍立即生成 artifact。

### Async behavior without real RabbitMQ

可通过 mock publisher 或直接调用 WorkerService。

4. ASYNC_RABBITMQ mode 下 create job 后保持 PENDING。
5. publisher 被调用。
6. WorkerService.process 将 PENDING job 改为 RUNNING -> COMPLETED。
7. WorkerService.process 遇到 CANCELED job 跳过。
8. WorkerService.process 遇到 COMPLETED job 幂等跳过。
9. WorkerService.process 不存在 job 不抛系统异常。
10. Worker execution outputPayload 仍包含 mock=true。
11. Worker execution outputPayload 仍包含 filesTouched=[]。
12. Worker execution outputPayload 仍包含 gitOperations=[]。

### Retry / Cancel

13. retry 在 async mode 下创建 PENDING job。
14. retry publish failure 标记 FAILED。
15. cancel PENDING job 成功。
16. cancel COMPLETED job 返回 CONFLICT。
17. non-maintainer retry 返回 PROJECT_ACCESS_DENIED。

### Config

18. ToolWorkerProperties 正确绑定默认值。
19. Rabbit queue config 在 queueEnabled=false 时不强制连接。

全量后端质量门：

```bash
cd backend
mvn test
```

## 15. 前端 E2E

新增或修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. 工具 job 显示队列状态。
2. 如果 job PENDING/RUNNING，显示 polling indicator。
3. job COMPLETED 后 polling indicator 消失。
4. retry/cancel 按钮仍可见于对应状态。
5. 页面无 JS error。

说明：

- E2E 环境可继续使用 SYNC_MOCK。
- 异步轮询可通过后端 test fixture 或 API 状态制造；如果成本过高，E2E 只验证 UI 兼容，异步行为由后端测试覆盖。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 16. 文档与报告

完成后新增：

```text
docs/milestone-37a-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Worker Queue 架构说明
3. RabbitMQ / Redis 配置说明
4. ToolExecutionJobService 改造说明
5. ToolExecutionWorkerService 说明
6. 同步 / 异步模式说明
7. Retry / Cancel 异步规则
8. 前端轮询说明
9. Docker / Compose 配置说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37B

## 17. 验收标准

必须满足：

- 引入 Spring AMQP 配置，但默认不强制 RabbitMQ 启动。
- test profile 保持 SYNC_MOCK 稳定。
- ASYNC_RABBITMQ 模式可创建 PENDING job。
- publisher 可发布 ToolExecutionJobMessage。
- worker 可消费并完成 Mock job。
- worker 不执行真实 shell / Git / 文件写入。
- retry / cancel 在 async 模式下行为正确。
- 前端可显示 queued/running 状态。
- 前端轮询可停止。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 18. 已知非目标

本阶段不做：

- 真实工具执行
- Kubernetes worker
- 多实例分布式锁
- RabbitMQ DLQ
- delayed retry
- Prometheus worker metrics
- Job dashboard 独立页面
- Redis stream queue
- Worker autoscaling

这些可进入后续：

- 37B: Read-only Code Search Index
- 37C: Worker DLQ / Retry Backoff
- 37D: Tool Execution Metrics Dashboard
- 37E: Real Read-only Tool Adapter Hardening

## 19. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37A。

文档路径：
docs/milestone-37a-async-worker-queue-redis-rabbitmq.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36F 的数据库 Job Queue 基础上，新增 RabbitMQ 异步 worker 能力。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要应用 patch。
8. 异步 worker 仍然只执行 Mock / Read-only 工具。
9. 默认 dev/test 必须保持 SYNC_MOCK，避免 RabbitMQ 未启动导致测试失败。
10. 不要破坏 36A-36I 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. backend/pom.xml 引入 spring-boot-starter-amqp，如 Redis starter 已缺失也补齐。
2. application.yml / application-test.yml / .env.example 增加 app.tool-worker、RabbitMQ、Redis 配置。
3. 新增 ToolWorkerProperties。
4. 新增 ToolExecutionQueueConfig。
5. 新增 ToolExecutionJobMessage。
6. 新增 ToolExecutionJobPublisher。
7. 新增 ToolExecutionJobConsumer。
8. 新增 ToolExecutionWorkerService。
9. ToolExecutionJobService 支持 SYNC_MOCK / ASYNC_RABBITMQ 两种模式。
10. ToolSandboxExecutionService approve / allowed execution 使用 job queue 模式。
11. retry / cancel 适配 async job。
12. 前端 MultiAgentRunPanel 增加 polling，显示 queued/running 状态。
13. Docker / Compose 注入 RabbitMQ / Redis / Tool Worker 环境变量，但默认本地仍可 SYNC_MOCK。
14. 后端测试不少于 18 个。
15. 前端 E2E 覆盖 job queue 状态显示。
16. 新增 docs/milestone-37a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Worker Queue 架构说明
3. RabbitMQ / Redis 配置说明
4. ToolExecutionJobService 改造说明
5. ToolExecutionWorkerService 说明
6. 同步 / 异步模式说明
7. Retry / Cancel 异步规则
8. 前端轮询说明
9. Docker / Compose 配置说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37B

现在开始实现，不要只给计划。
```
