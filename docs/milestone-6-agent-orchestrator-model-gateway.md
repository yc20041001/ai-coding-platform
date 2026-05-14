# Milestone 6: Agent Orchestrator + Model Gateway 基础模块实施文档

## 1. 背景与目标

当前项目已经完成以下 P0 后端主链路：

- Foundation 基础设施
- Auth 登录认证与 JWT
- Project + Member 项目与成员权限
- Repository 仓库绑定与只读 Git 操作
- Task + Agent 任务与 Agent 基础管理
- Chat + SSE 会话、消息与 Mock 流式输出

Milestone 6 的目标是补齐 AI Coding 平台的核心执行闭环：

> 任务创建后，可以由 Agent Orchestrator 调度 Agent，通过 Model Gateway 获取 Mock 模型输出，并将执行记录、任务日志、任务事件、任务产物、模型调用日志全部持久化。

本阶段只做基础编排能力，不接真实大模型，不执行真实工具，不修改真实代码，不做 Git 写操作。

## 2. 实施边界

### 2.1 本阶段要做

- Agent Orchestrator 基础调度
- Model Gateway 抽象接口
- Mock Model Gateway 实现
- Agent 执行记录持久化
- 模型请求日志持久化
- 手动触发 Task 执行
- Task 状态推进
- Task 日志写入
- Task 事件写入
- Task 产物生成
- 执行记录查询
- 模型调用日志查询

### 2.2 本阶段不做

- 不接 OpenAI / Claude / DeepSeek / Gemini / Qwen 等真实模型
- 不做真实代码生成落盘
- 不执行 shell 命令
- 不执行 Git commit / push / PR
- 不做 Agent 自动并发调度
- 不做工作流 DAG
- 不做工具调用沙箱
- 不做真实 RAG 检索
- 不修改已验证通过模块的核心逻辑

## 3. 依赖模块

Milestone 6 依赖以下已完成模块：

| 模块 | 依赖点 |
|---|---|
| Auth | 当前登录用户、JWT 权限链路 |
| Project + Member | 项目访问权限、项目角色校验 |
| Agent | Agent 基础信息、Agent 类型、状态 |
| Task | 任务主表、任务日志、任务事件、任务产物、状态流转 |
| Chat | 预留 Chat Agent 执行入口 |

## 4. 总体架构

```text
Client
  |
  | POST /api/tasks/{taskId}/execute
  v
AgentOrchestratorController
  |
  v
AgentOrchestratorService
  |
  |-- ProjectPermissionService      权限校验
  |-- AiTaskMapper                  查询/更新任务
  |-- AiAgentMapper                 查询 Agent
  |-- AgentExecutionMapper          写执行记录
  |-- ModelGateway                  调用模型网关
  |-- ModelRequestLogService        写模型请求日志
  |-- AiTaskLogMapper               写任务日志
  |-- AiTaskEventMapper             写任务事件
  |-- AiTaskArtifactMapper          写任务产物
  |
  v
MockModelGateway
  |
  v
Mock ModelResponse
```

## 5. 新增目录结构

```text
backend/src/main/java/com/aicoding/platform/
├── orchestrator/
│   ├── application/
│   │   └── AgentOrchestratorService.java
│   ├── controller/
│   │   └── AgentOrchestratorController.java
│   ├── domain/
│   │   ├── AgentExecutionEntity.java
│   │   ├── ModelRequestLogEntity.java
│   │   ├── AgentExecutionType.java
│   │   ├── AgentExecutionStatus.java
│   │   └── ModelRequestType.java
│   ├── dto/
│   │   ├── ExecuteTaskRequest.java
│   │   ├── AgentExecutionResponse.java
│   │   └── ModelRequestLogResponse.java
│   └── infrastructure/
│       ├── AgentExecutionMapper.java
│       └── ModelRequestLogMapper.java
│
├── modelgateway/
│   ├── application/
│   │   ├── ModelGateway.java
│   │   ├── MockModelGateway.java
│   │   └── ModelRequestLogService.java
│   └── dto/
│       ├── ModelRequest.java
│       └── ModelResponse.java
│
└── resources/db/migration/
    └── V7__init_orchestrator_and_model_gateway_tables.sql
```

## 6. 数据库设计

新增迁移文件：

```text
backend/src/main/resources/db/migration/V7__init_orchestrator_and_model_gateway_tables.sql
```

### 6.1 agent_execution

Agent 执行记录表。

```sql
CREATE TABLE agent_execution (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    task_id BIGINT NULL COMMENT '任务 ID',
    chat_session_id BIGINT NULL COMMENT '聊天会话 ID',
    chat_message_id BIGINT NULL COMMENT '聊天消息 ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    execution_type VARCHAR(32) NOT NULL COMMENT '执行类型: TASK/CHAT/REVIEW/MANUAL',
    status VARCHAR(32) NOT NULL COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED/CANCELED',
    input_prompt MEDIUMTEXT NULL COMMENT '输入 Prompt',
    output_content MEDIUMTEXT NULL COMMENT '输出内容',
    error_message TEXT NULL COMMENT '错误信息',
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    token_usage BIGINT NOT NULL DEFAULT 0 COMMENT 'Token 使用量',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_agent_execution_project_time (project_id, create_time),
    KEY idx_agent_execution_task (task_id),
    KEY idx_agent_execution_chat (chat_session_id, chat_message_id),
    KEY idx_agent_execution_agent (agent_id),
    KEY idx_agent_execution_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 执行记录表';
```

### 6.2 model_request_log

模型请求日志表。

```sql
CREATE TABLE model_request_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    execution_id BIGINT NULL COMMENT 'Agent 执行记录 ID',
    provider VARCHAR(32) NOT NULL COMMENT '模型供应商',
    model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
    request_type VARCHAR(32) NOT NULL COMMENT '请求类型',
    prompt_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'Prompt Token 数',
    completion_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'Completion Token 数',
    total_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '总 Token 数',
    latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_message TEXT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_model_request_project_time (project_id, create_time),
    KEY idx_model_request_execution (execution_id),
    KEY idx_model_request_provider (provider),
    KEY idx_model_request_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型请求日志表';
```

### 6.3 数据库约束原则

- 不使用物理外键
- 通过业务层保证引用一致性
- 所有 ID 使用 MyBatis-Plus 雪花 ID
- 时间字段使用 `MetaObjectHandler` 自动填充
- 执行记录与模型日志不可物理删除

## 7. Domain 设计

### 7.1 AgentExecutionType

```java
public enum AgentExecutionType {
    TASK,
    CHAT,
    REVIEW,
    MANUAL
}
```

### 7.2 AgentExecutionStatus

```java
public enum AgentExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED
}
```

### 7.3 ModelRequestType

```java
public enum ModelRequestType {
    CHAT,
    TASK_EXECUTION,
    CODE_REVIEW,
    SUMMARY,
    MOCK
}
```

### 7.4 AgentExecutionEntity

要求：

- `@TableName("agent_execution")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- `@TableField(fill = FieldFill.INSERT_UPDATE)` 标注 `updateTime`
- 不继承 `BaseEntity`
- 不使用 Lombok
- 手写 getter/setter

字段：

| 字段 | 类型 |
|---|---|
| id | Long |
| projectId | Long |
| taskId | Long |
| chatSessionId | Long |
| chatMessageId | Long |
| agentId | Long |
| executionType | String |
| status | String |
| inputPrompt | String |
| outputContent | String |
| errorMessage | String |
| startedAt | LocalDateTime |
| finishedAt | LocalDateTime |
| tokenUsage | Long |
| createTime | LocalDateTime |
| updateTime | LocalDateTime |

### 7.5 ModelRequestLogEntity

要求：

- `@TableName("model_request_log")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- 不继承 `BaseEntity`
- 不使用 Lombok
- 手写 getter/setter

字段：

| 字段 | 类型 |
|---|---|
| id | Long |
| projectId | Long |
| executionId | Long |
| provider | String |
| modelName | String |
| requestType | String |
| promptTokens | Long |
| completionTokens | Long |
| totalTokens | Long |
| latencyMs | Long |
| success | Boolean |
| errorMessage | String |
| createTime | LocalDateTime |

## 8. Mapper 设计

### 8.1 AgentExecutionMapper

```java
package com.aicoding.platform.orchestrator.infrastructure;

import com.aicoding.platform.orchestrator.domain.AgentExecutionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentExecutionMapper extends BaseMapper<AgentExecutionEntity> {
}
```

### 8.2 ModelRequestLogMapper

```java
package com.aicoding.platform.orchestrator.infrastructure;

import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelRequestLogMapper extends BaseMapper<ModelRequestLogEntity> {
}
```

## 9. Model Gateway 设计

### 9.1 ModelRequest

字段：

| 字段 | 类型 |
|---|---|
| projectId | Long |
| executionId | Long |
| provider | String |
| modelName | String |
| requestType | String |
| systemPrompt | String |
| userPrompt | String |
| context | String |
| temperature | BigDecimal |
| maxTokens | Integer |

### 9.2 ModelResponse

字段：

| 字段 | 类型 |
|---|---|
| content | String |
| promptTokens | Long |
| completionTokens | Long |
| totalTokens | Long |
| latencyMs | Long |
| success | Boolean |
| errorMessage | String |

### 9.3 ModelGateway

```java
package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;

public interface ModelGateway {
    ModelResponse generate(ModelRequest request);
}
```

### 9.4 MockModelGateway

职责：

- 实现 `ModelGateway`
- 使用 `@Service`
- 不调用真实模型
- 根据 `requestType` 返回固定 Mock 内容
- 模拟 token 使用量
- 模拟延迟耗时

Mock 返回策略：

| requestType | content |
|---|---|
| TASK_EXECUTION | 任务已由 Mock Agent 执行完成。当前阶段未调用真实大模型，也未修改代码。系统已完成任务分析、执行日志记录与产物生成。 |
| CHAT | 我是 Mock Agent，已收到你的消息。真实模型网关将在后续里程碑接入。 |
| CODE_REVIEW | Mock Review 完成：当前未发现阻塞性问题。后续接入真实模型后将输出详细代码审查意见。 |
| SUMMARY | Mock Summary 完成：当前内容已被概括，后续将接入真实摘要模型。 |
| 默认 | Mock Model Gateway 已处理请求。 |

### 9.5 ModelRequestLogService

职责：

- 统一保存模型调用日志
- Orchestrator 不直接操作 `ModelRequestLogMapper`
- 成功与失败统一记录

方法：

```java
void record(Long projectId, Long executionId, ModelRequest request, ModelResponse response);
```

写入字段：

- projectId
- executionId
- provider
- modelName
- requestType
- promptTokens
- completionTokens
- totalTokens
- latencyMs
- success
- errorMessage

## 10. DTO 设计

### 10.1 ExecuteTaskRequest

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| agentId | String | 否 | 指定执行 Agent，不传则使用任务绑定的 agentId |
| instruction | String | 否 | 额外执行指令 |

### 10.2 AgentExecutionResponse

字段：

| 字段 | 类型 |
|---|---|
| id | String |
| projectId | String |
| taskId | String |
| chatSessionId | String |
| chatMessageId | String |
| agentId | String |
| agentName | String |
| executionType | String |
| status | String |
| inputPrompt | String |
| outputContent | String |
| errorMessage | String |
| tokenUsage | Long |
| startedAt | LocalDateTime |
| finishedAt | LocalDateTime |
| createTime | LocalDateTime |

### 10.3 ModelRequestLogResponse

字段：

| 字段 | 类型 |
|---|---|
| id | String |
| projectId | String |
| executionId | String |
| provider | String |
| modelName | String |
| requestType | String |
| promptTokens | Long |
| completionTokens | Long |
| totalTokens | Long |
| latencyMs | Long |
| success | Boolean |
| errorMessage | String |
| createTime | LocalDateTime |

## 11. Agent Orchestrator 设计

### 11.1 AgentOrchestratorService

核心职责：

- 校验项目权限
- 校验任务状态
- 选择 Agent
- 创建 Agent Execution
- 构建 Prompt
- 调用 Model Gateway
- 写模型请求日志
- 推进 Task 状态
- 写 Task 日志
- 写 Task 事件
- 生成 Task Artifact
- 返回执行结果

依赖：

- `AgentExecutionMapper`
- `ModelGateway`
- `ModelRequestLogService`
- `AiTaskMapper`
- `AiAgentMapper`
- `AiTaskLogMapper`
- `AiTaskArtifactMapper`
- `AiTaskEventMapper`
- `ProjectPermissionService`
- `ChatMessageMapper`
- `ChatSessionMapper`

### 11.2 executeTask 流程

方法：

```java
AgentExecutionResponse executeTask(Long taskId, ExecuteTaskRequest request);
```

流程：

1. 查询 `AiTaskEntity`
2. 任务不存在时返回 `NOT_FOUND`
3. 通过 `task.projectId` 校验当前用户具备 `DEVELOPER+`
4. 校验任务状态必须为 `PENDING`
5. 解析 Agent：
   - 优先使用 `request.agentId`
   - 其次使用 `task.agentId`
   - 都不存在则返回 `BAD_REQUEST`
6. 查询 `AiAgentEntity`
7. Agent 不存在时返回 `NOT_FOUND`
8. 创建 `AgentExecutionEntity`
   - status = `RUNNING`
   - executionType = `TASK`
   - startedAt = 当前时间
9. 写任务事件：`PENDING -> RUNNING`
10. 写任务日志：`ORCHESTRATOR_START`
11. 更新任务状态为 `RUNNING`
12. 构建 Prompt
13. 调用 `ModelGateway.generate()`
14. 调用 `ModelRequestLogService.record()`
15. 如果模型响应成功：
   - 更新 execution 为 `COMPLETED`
   - 保存 outputContent
   - 保存 tokenUsage
   - finishedAt = 当前时间
   - 写任务日志：`MODEL_GATEWAY_REQUEST`
   - 写任务日志：`ORCHESTRATOR_DONE`
   - 写任务产物：`Mock Agent Execution Result`
   - 写任务事件：`RUNNING -> COMPLETED`
   - 更新任务状态为 `COMPLETED`
16. 如果模型响应失败或抛出异常：
   - 更新 execution 为 `FAILED`
   - 保存 errorMessage
   - finishedAt = 当前时间
   - 写任务日志：`ORCHESTRATOR_FAILED`
   - 写任务事件：`RUNNING -> FAILED`
   - 更新任务状态为 `FAILED`
17. 返回 `AgentExecutionResponse`

### 11.3 Prompt 构建规则

Prompt 至少包含：

```text
你是 AI Coding Platform 的 Agent。

Agent:
- Name: {agent.name}
- Type: {agent.type}

Task:
- Title: {task.title}
- Description: {task.description}
- Type: {task.taskType}
- Priority: {task.priority}

Instruction:
{request.instruction}

Constraints:
- 当前阶段使用 Mock Model Gateway
- 不调用真实大模型
- 不修改真实代码
- 不执行 Git 写操作
- 输出应该适合作为任务执行结果保存
```

## 12. Controller 设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestrator/controller/AgentOrchestratorController.java
```

### 12.1 API 清单

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/tasks/{taskId}/execute` | DEVELOPER+ | 手动触发 Agent 执行任务 |
| GET | `/api/tasks/{taskId}/executions` | VIEWER+ | 查询任务执行记录 |
| GET | `/api/agent-executions/{executionId}` | VIEWER+ | 查询执行详情 |
| GET | `/api/agent-executions/{executionId}/model-logs` | VIEWER+ | 查询模型调用日志 |

### 12.2 POST /api/tasks/{taskId}/execute

请求：

```json
{
  "agentId": "300002",
  "instruction": "请用 Mock Agent 执行这个任务"
}
```

响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "2055000000000000001",
    "projectId": "2054000000000000001",
    "taskId": "2054999999999999999",
    "agentId": "300002",
    "agentName": "Backend Agent",
    "executionType": "TASK",
    "status": "COMPLETED",
    "inputPrompt": "...",
    "outputContent": "任务已由 Mock Agent 执行完成...",
    "tokenUsage": 42,
    "startedAt": "2026-05-13T10:00:00",
    "finishedAt": "2026-05-13T10:00:01",
    "createTime": "2026-05-13T10:00:00"
  },
  "traceId": "xxxx",
  "timestamp": "2026-05-13T10:00:01"
}
```

## 13. Task 模块联动要求

执行任务后必须写入以下数据。

### 13.1 ai_task_event

至少写入：

- `PENDING -> RUNNING`
- `RUNNING -> COMPLETED`

失败时：

- `RUNNING -> FAILED`

### 13.2 ai_task_log

至少写入：

- `ORCHESTRATOR_START`
- `MODEL_GATEWAY_REQUEST`
- `ORCHESTRATOR_DONE`

失败时：

- `ORCHESTRATOR_FAILED`

日志级别建议：

| 场景 | level |
|---|---|
| 启动编排 | INFO |
| 调用模型网关 | INFO |
| 执行完成 | INFO |
| 执行失败 | ERROR |

### 13.3 ai_task_artifact

成功执行后生成一条产物：

| 字段 | 值 |
|---|---|
| artifactType | MARKDOWN 或 DOCUMENT |
| title | Mock Agent Execution Result |
| content | ModelResponse.content |
| projectId | task.projectId |
| taskId | task.id |

### 13.4 ai_task

成功执行：

- status = `COMPLETED`
- startTime 非空
- endTime 非空
- errorMessage = null

失败执行：

- status = `FAILED`
- startTime 非空
- endTime 非空
- errorMessage 非空

## 14. 状态流转规则

Milestone 6 只允许执行 `PENDING` 任务。

| 当前状态 | execute 行为 |
|---|---|
| PENDING | 允许执行 |
| RUNNING | 返回 CONFLICT |
| REVIEWING | 返回 CONFLICT |
| COMPLETED | 返回 CONFLICT |
| FAILED | 返回 CONFLICT，需先走 retry |
| CANCELED | 返回 CONFLICT |

执行过程内部状态：

```text
PENDING -> RUNNING -> COMPLETED
PENDING -> RUNNING -> FAILED
```

## 15. 权限设计

| 操作 | 权限 |
|---|---|
| 执行任务 | 项目 DEVELOPER+ |
| 查询任务执行记录 | 项目 VIEWER+ |
| 查询执行详情 | 项目 VIEWER+ |
| 查询模型日志 | 项目 VIEWER+ |

权限校验方式：

- task 相关接口先通过 `taskId` 查询 `projectId`
- execution 相关接口先通过 `executionId` 查询 `projectId`
- 然后调用 `ProjectPermissionService.checkProjectRole(...)`

## 16. 错误处理

| 场景 | 错误码 |
|---|---|
| 未登录 | UNAUTHORIZED |
| 无项目权限 | PROJECT_ACCESS_DENIED |
| task 不存在 | NOT_FOUND |
| agent 不存在 | NOT_FOUND |
| task 状态不能执行 | CONFLICT |
| task 无 agentId 且 request 未传 agentId | BAD_REQUEST |
| 模型网关异常 | INTERNAL_ERROR 或 execution.status=FAILED |

如果 `ErrorCode` 暂时缺少更细业务码，优先复用已有通用错误码，不要为了本阶段大范围扩展错误码。

## 17. Chat Agent 预留能力

可在 `AgentOrchestratorService` 中预留内部方法：

```java
AgentExecutionResponse executeChatAgent(Long sessionId, Long messageId);
```

基础流程：

1. 查询 chat session
2. 通过 session.projectId 校验 `DEVELOPER+`
3. 查询用户消息
4. 默认选择 Backend Agent
5. 创建 AgentExecutionEntity
6. 调用 MockModelGateway，requestType = `CHAT`
7. 创建或更新 assistant message
8. 返回 AgentExecutionResponse

当前阶段 Controller 可以不暴露该接口，或作为 P0 测试接口暴露。

## 18. 验收标准

### 18.1 编译测试

必须通过：

```bash
cd backend
mvn compile
mvn test
```

### 18.2 启动前置

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3307/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
export DB_USERNAME=root
export DB_PASSWORD=platform123
export JWT_SECRET="verification-test-secret-min-32bytes"

cd backend
mvn spring-boot:run
```

按本地实际 MySQL 密码调整 `DB_PASSWORD`。

### 18.3 手动验证流程

#### 1. 登录 admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

期望：

- 返回 accessToken
- 返回 roles 包含 `ADMIN`

#### 2. 创建项目

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Milestone 6 Verification Project",
    "description": "Project for Agent Orchestrator verification",
    "techStack": ["Java", "Spring Boot"]
  }'
```

期望：

- 返回 projectId
- 当前用户是 OWNER

#### 3. 创建任务

```bash
curl -X POST http://localhost:8080/api/projects/<projectId>/tasks \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "实现用户列表接口",
    "description": "请模拟完成用户列表接口的设计与实现说明",
    "taskType": "FEATURE",
    "priority": "MEDIUM",
    "agentId": "300002"
  }'
```

期望：

- 返回 taskId
- status = `PENDING`

#### 4. 执行任务

```bash
curl -X POST http://localhost:8080/api/tasks/<taskId>/execute \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "请用 Mock Agent 执行这个任务"
  }'
```

期望：

- execution.status = `COMPLETED`
- outputContent 非空
- tokenUsage > 0

#### 5. 查询任务详情

```bash
curl http://localhost:8080/api/tasks/<taskId> \
  -H "Authorization: Bearer <accessToken>"
```

期望：

- task.status = `COMPLETED`
- startTime 非空
- endTime 非空

#### 6. 查询任务日志

```bash
curl http://localhost:8080/api/tasks/<taskId>/logs \
  -H "Authorization: Bearer <accessToken>"
```

期望至少包含：

- `ORCHESTRATOR_START`
- `MODEL_GATEWAY_REQUEST`
- `ORCHESTRATOR_DONE`

#### 7. 查询任务产物

```bash
curl http://localhost:8080/api/tasks/<taskId>/artifacts \
  -H "Authorization: Bearer <accessToken>"
```

期望：

- 至少 1 条 artifact
- title = `Mock Agent Execution Result`
- content 非空

#### 8. 查询执行记录

```bash
curl http://localhost:8080/api/tasks/<taskId>/executions \
  -H "Authorization: Bearer <accessToken>"
```

期望：

- 至少 1 条 execution
- status = `COMPLETED`

#### 9. 查询执行详情

```bash
curl http://localhost:8080/api/agent-executions/<executionId> \
  -H "Authorization: Bearer <accessToken>"
```

期望：

- outputContent 非空
- agentName 非空

#### 10. 查询模型调用日志

```bash
curl http://localhost:8080/api/agent-executions/<executionId>/model-logs \
  -H "Authorization: Bearer <accessToken>"
```

期望：

- provider = `MOCK`
- modelName = `mock-agent-model`
- success = true
- totalTokens > 0

#### 11. 重复执行已完成任务

```bash
curl -X POST http://localhost:8080/api/tasks/<taskId>/execute \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{}'
```

期望：

- 返回 `CONFLICT`
- 不创建新的成功执行记录

#### 12. 无 token 验证

```bash
curl -X POST http://localhost:8080/api/tasks/<taskId>/execute \
  -H "Content-Type: application/json" \
  -d '{}'
```

期望：

- 返回 `UNAUTHORIZED`

## 19. 完成报告模板

Milestone 6 完成后，请按以下格式输出：

```markdown
# Milestone 6 完成报告

## 1. 新增/修改文件清单

...

## 2. 数据库表和索引清单

...

## 3. 新增 API 清单

...

## 4. Agent Orchestrator 执行流程

...

## 5. Model Gateway Mock 策略

...

## 6. Task 日志、事件、产物写入结果

...

## 7. mvn compile / mvn test 结果

...

## 8. 手动接口验证结果

...

## 9. 是否可以进入 Milestone 7：RAG Knowledge Base 基础模块

...
```

## 20. Milestone 7 预告

如果 Milestone 6 验证通过，下一阶段进入：

```text
Milestone 7: RAG Knowledge Base 基础模块
```

建议范围：

- knowledge_base
- knowledge_document
- document_chunk
- embedding mock
- 文档上传元数据
- Markdown/代码文本切分
- Mock 向量检索
- Chat/Agent 引用 reference 写入

Milestone 7 才开始接入 RAG 上下文，不在 Milestone 6 中提前实现。

