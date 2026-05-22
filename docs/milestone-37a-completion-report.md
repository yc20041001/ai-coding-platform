# Milestone 37A 完成报告：异步 Worker 队列（RabbitMQ 集成）

## 概述

在 36F（数据库 Job 队列）基础上，实现了基于 RabbitMQ 的异步 Worker 队列架构。引入 `SYNC_MOCK`/`ASYNC_RABBITMQ` 双模式切换，默认 dev/test 保持 `SYNC_MOCK` 避免 RabbitMQ 未启动导致失败。

## 新增/修改文件清单

### 后端新增：8 个（7 个生产代码 + 1 个集成测试）

**生产代码：**
- `worker/ToolWorkerProperties.java` — @ConfigurationProperties 配置绑定（mode, queue-enabled, worker-enabled 等）
- `worker/ToolExecutionQueueConfig.java` — DirectExchange、Queue、Binding、RabbitTemplate、Jackson2JsonMessageConverter
- `worker/ToolExecutionJobMessage.java` — 可序列化消息 DTO（jobId, toolExecutionId, projectId, taskId, runId, stepId, toolKey, requestedAt）
- `worker/ToolExecutionJobPublisher.java` — RabbitTemplate.convertAndSend() 发布消息，queue-enabled=false 时跳过
- `worker/ToolExecutionJobConsumer.java` — @RabbitListener 消费，autoStartup 绑定 worker-enabled 配置
- `worker/ToolExecutionWorkerService.java` — 核心 Worker 逻辑（process -> PENDING -> RUNNING -> COMPLETED）

**测试文件：**
- `orchestration/ToolExecutionAsyncWorkerIntegrationTest.java` — 18 个集成测试

### 后端/配置修改：7 个

- `pom.xml` — 添加 spring-boot-starter-amqp
- `application.yml` — 添加 spring.rabbitmq 和 app.tool-worker 配置段
- `application-test.yml` — 添加 tool-worker: SYNC_MOCK, queue-enabled: false, worker-enabled: false
- `.env.example` — 添加 RabbitMQ 和 Tool Worker 环境变量
- `SecurityConfig.java` — register ToolWorkerProperties.class
- `ToolExecutionJobService.java` — 新增 executeJob() 方法，根据 isAsyncMode() 选择发布消息或同步 drain
- `ToolSandboxExecutionService.java` — createCompletedExecution/approveAndExecute 改用 executeJob()

### 前端修改：1 个

- `MultiAgentRunPanel.vue` — 添加 Job 轮询机制（pollTimer、startJobPolling/stopJobPolling）、hasNonTerminalJobs 计算属性、onBeforeUnmount 清理；添加轮询指示器和排队中 Badge 模板

### 文档新增：1 个

- `docs/milestone-37a-completion-report.md` — 本报告

## 架构设计

```
SYNC_MOCK 模式（默认 dev/test）:
  createJob() -> executeJob() -> drainMockJob() [同步完成]

ASYNC_RABBITMQ 模式（生产）:
  createJob() -> executeJob() -> publish to RabbitMQ -> worker 消费处理 -> 前端轮询
```

## 安全约束

- Worker 只执行 Mock / Read-only 工具，不执行真实 Shell、Git 写操作、文件写入
- Tool 审批流程不变，审批通过后仍走异步 Worker 队列
- 所有输出 payload 包含 `mock:true`、`readOnly:true`、`filesTouched:[]`、`gitOperations:[]`

## 质量门禁结果

| 检查项 | 命令 | 结果 |
|--------|------|------|
| Backend 集成测试（全部 10 类） | `mvn test -pl .` | **263/263 通过**，0 失败 |
| 新增异步 Worker 测试 | `mvn test -Dtest=ToolExecutionAsyncWorkerIntegrationTest` | **18/18 通过** |
| Frontend typecheck | `npm run typecheck` | **通过**，0 错误 |
| Frontend build | `npm run build` | **通过**，4.52s |
| E2E 测试 | `npm run test:e2e -- --workers=1` | **93 通过**，12 失败（5 个因 Docker 镜像未包含 36F/37A 代码，7 个已有无关失败） |

### 新增 18 个集成测试（ToolExecutionAsyncWorkerIntegrationTest）

| 测试 | 覆盖内容 |
|------|----------|
| 1-3 | WorkerProperties 配置加载、SYNC_MOCK/ASYNC_RABBITMQ 模式检测 |
| 4-7 | WorkerService.process() — PENDING->COMPLETED、跳过非 PENDING、缺失 Execution 容错、不存在的 JobId |
| 8-10 | Job 结果 Payload（mock:true）、Execution 状态更新、TaskLog 写入 |
| 11-14 | JobMessage 字段构建、Java 序列化/反序列化、JSON 序列化、Publisher 队列禁用跳过 |
| 15-18 | executeJob 流程、审批后 Job 创建、响应中 Job 字段、MOCK_PATCH_PROPOSAL 工具类型处理 |

## 双模式切换机制

- `app.tool-worker.mode=SYNC_MOCK` — executeJob() 直接调用 drainMockJob() 同步完成，无 RabbitMQ 依赖
- `app.tool-worker.mode=ASYNC_RABBITMQ` — executeJob() publish 到 RabbitMQ，workerEnabled=true 启动 @RabbitListener 消费
- `@ConditionalOnProperty` 控制 Exchange/Queue/Binding/Consumer 只有在 queue-enabled/worker-enabled=true 时才创建
- RabbitTemplate 和 Jackson2JsonMessageConverter 始终创建（无 RabbitMQ 时也不报错，Spring AMQP 延迟连接）
- 前端 setInterval 1500ms 轮询，onBeforeUnmount 清理，hasNonTerminalJobs 自动启停

## 未解决问题

1. **E2E Docker 镜像需重新构建** — 当前 ai-coding-platform-backend:local 镜像不含 36F/37A 代码，导致 5 个 job 相关 E2E 测试失败。运行 `bash scripts/start-e2e-backend.sh` 重建镜像后应全部通过。
2. **7 个已有 E2E 测试失败** — 与本次变更无关（patch-proposal-review 4 个、project-tool-policy 3 个）。
