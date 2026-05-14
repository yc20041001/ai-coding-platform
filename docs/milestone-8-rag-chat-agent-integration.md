# Milestone 8: RAG 接入 Chat + Agent Orchestrator 实施文档

## 1. 背景与目标

当前项目已经完成以下 P0 后端能力：

- Auth 登录认证与 JWT
- Project + Member 项目与成员权限
- Repository 仓库绑定与只读 Git 操作
- Task + Agent 任务与 Agent 管理
- Chat + SSE 会话、消息与 Mock 流式输出
- Agent Orchestrator + Mock Model Gateway 执行闭环
- RAG Knowledge Base 基础模块

Milestone 7 已经实现：

- 项目级 Knowledge Base
- Knowledge Document
- Document Chunk
- Mock Embedding
- MySQL LIKE Mock RAG Search
- RAG 结果到 Chat Reference 的转换预留

Milestone 8 的目标是把 RAG 能力真正接入现有业务流：

> Chat 发送消息时自动检索项目知识库并写入 message references；Agent Orchestrator 执行任务时自动检索项目知识库，将 RAG context 写入 AgentExecution.inputPrompt 和 ModelRequest.context，并在执行结果中保留引用来源。

本阶段仍然不接真实大模型、不接真实向量数据库、不做真实代码修改、不做 Git 写操作。

## 2. 实施边界

### 2.1 本阶段要做

- Chat sendMessage 自动执行 RAG Search
- ChatMessageReference 自动落库
- Chat message 查询返回 references
- Chat SSE done 事件可包含 references 信息
- Agent Orchestrator executeTask 自动执行 RAG Search
- AgentExecution.inputPrompt 保存 RAG context
- ModelRequest.context 填入 RAG 检索结果
- Task Log 记录 RAG 检索行为
- Task Artifact 或 Execution Response 可追踪引用来源
- 统一 RAG 上下文拼接格式
- 增加 RAG 开关与默认参数

### 2.2 本阶段不做

- 不接真实 Embedding 模型
- 不接真实向量数据库
- 不接真实大模型
- 不执行真实 shell
- 不做真实 Git 写操作
- 不生成真实代码文件
- 不改造复杂多轮 memory
- 不做 RAG 重排序 rerank
- 不做跨项目知识检索
- 不做权限绕过式引用查询

## 3. 约束要求

- 不破坏 Milestone 1-7 已验证通过的核心逻辑
- 优先采用最小侵入式集成
- 不改变现有 API 的必填字段
- 新增字段必须保持向后兼容
- IDs 对外仍保持 String
- 权限校验继续复用 `ProjectPermissionService`
- 仍然使用 Mock Model Gateway
- 仍然使用 MySQL LIKE Mock RAG Search
- 不使用 Lombok
- 构造器注入
- 手写 getter/setter
- 返回值继续使用 `ApiResponse`
- 异常继续使用 `BizException` + `ErrorCode`

## 4. 模块目标

实现 3 个集成能力。

### 4.1 Chat + RAG

- 用户发送消息时，根据消息内容执行 RAG Search
- 将检索结果转换为 ChatMessageReference
- 将 references 绑定到 assistant message 或 user message
- 消息列表查询时返回 references
- SSE 流式结束后 references 仍然可查询

### 4.2 Agent Orchestrator + RAG

- 执行任务时，根据 task title / description / instruction 执行 RAG Search
- 将 RAG context 拼接进 AgentExecution.inputPrompt
- 将 RAG context 写入 ModelRequest.context
- 任务日志记录 RAG 检索数量
- 执行结果可看到使用过的上下文引用

### 4.3 RAG Context Formatter

- 统一 RAG context 拼接格式
- 控制最大引用数量
- 控制单个 chunk 内容长度
- 输出结构可读、可审计、可追踪来源

## 5. 新增与修改文件总览

Milestone 8 以修改集成为主，新增少量 DTO / Service。

### 5.1 建议新增文件

```text
backend/src/main/java/com/aicoding/platform/rag/application/RagContextService.java
backend/src/main/java/com/aicoding/platform/rag/dto/RagContext.java
backend/src/main/java/com/aicoding/platform/rag/dto/RagReference.java
backend/src/main/java/com/aicoding/platform/rag/config/RagProperties.java
```

### 5.2 建议修改文件

```text
backend/src/main/java/com/aicoding/platform/chat/dto/SendChatMessageRequest.java
backend/src/main/java/com/aicoding/platform/chat/dto/SendChatMessageResponse.java
backend/src/main/java/com/aicoding/platform/chat/dto/ChatStreamEvent.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatApplicationService.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatStreamService.java

backend/src/main/java/com/aicoding/platform/orchestrator/dto/ExecuteTaskRequest.java
backend/src/main/java/com/aicoding/platform/orchestrator/dto/AgentExecutionResponse.java
backend/src/main/java/com/aicoding/platform/orchestrator/application/AgentOrchestratorService.java

backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelRequest.java

backend/src/main/resources/application.yml
```

### 5.3 不建议修改

除非编译必须，不建议修改：

```text
auth/
project/
member/
repository/
agent/
task/domain/
task/controller/
rag/domain/
rag/infrastructure/
```

## 6. 配置设计

新增配置：

```yaml
app:
  rag:
    enabled: true
    chat-enabled: true
    agent-enabled: true
    default-limit: 5
    max-context-chars: 4000
    max-chunk-chars: 800
```

### 6.1 RagProperties

新增：

```text
rag/config/RagProperties.java
```

字段：

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| enabled | boolean | true | RAG 总开关 |
| chatEnabled | boolean | true | Chat RAG 开关 |
| agentEnabled | boolean | true | Agent RAG 开关 |
| defaultLimit | int | 5 | 默认检索数量 |
| maxContextChars | int | 4000 | 最大上下文长度 |
| maxChunkChars | int | 800 | 单个 chunk 最大内容长度 |

要求：

- 使用 `@ConfigurationProperties(prefix = "app.rag")`
- 在已有配置类或新配置中启用 `@EnableConfigurationProperties(RagProperties.class)`
- 不影响应用启动

## 7. DTO 设计

## 7.1 RagReference

新增：

```text
rag/dto/RagReference.java
```

字段：

| 字段 | 类型 |
|---|---|
| chunkId | String |
| documentId | String |
| knowledgeBaseId | String |
| title | String |
| filePath | String |
| score | BigDecimal |
| snippet | String |
| referenceType | String |
| startLine | Integer |
| endLine | Integer |

## 7.2 RagContext

新增：

```text
rag/dto/RagContext.java
```

字段：

| 字段 | 类型 |
|---|---|
| query | String |
| contextText | String |
| references | List<RagReference> |
| total | Long |
| elapsedMs | Long |

## 7.3 SendChatMessageRequest 增强

修改：

```text
chat/dto/SendChatMessageRequest.java
```

新增字段：

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| useRag | Boolean | true | 是否启用 RAG |
| knowledgeBaseId | String | null | 指定知识库 |
| ragLimit | Integer | null | 指定检索数量 |

要求：

- 不影响旧请求
- 旧请求不传时默认启用 RAG

示例：

```json
{
  "content": "Agent Orchestrator 是怎么执行任务的？",
  "agentIds": ["300002"],
  "stream": true,
  "useRag": true,
  "knowledgeBaseId": "205xxx",
  "ragLimit": 5
}
```

## 7.4 SendChatMessageResponse 增强

修改：

```text
chat/dto/SendChatMessageResponse.java
```

新增字段：

| 字段 | 类型 |
|---|---|
| references | List<ChatMessageReferenceResponse> |
| ragUsed | Boolean |

## 7.5 ChatStreamEvent 增强

修改：

```text
chat/dto/ChatStreamEvent.java
```

新增字段：

| 字段 | 类型 |
|---|---|
| references | List<ChatMessageReferenceResponse> |
| ragUsed | Boolean |

done 事件中建议携带 references。

## 7.6 ExecuteTaskRequest 增强

修改：

```text
orchestrator/dto/ExecuteTaskRequest.java
```

新增字段：

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| useRag | Boolean | true | 是否启用 RAG |
| knowledgeBaseId | String | null | 指定知识库 |
| ragLimit | Integer | null | 指定检索数量 |

旧请求不传时默认启用 RAG。

## 7.7 AgentExecutionResponse 增强

修改：

```text
orchestrator/dto/AgentExecutionResponse.java
```

新增字段：

| 字段 | 类型 |
|---|---|
| ragUsed | Boolean |
| references | List<RagReference> |

如果不希望修改数据库表，`ragUsed` 和 `references` 可在响应时根据 prompt/context 临时组装；但建议 references 来自本次 RAG 检索结果。

## 7.8 ModelRequest 增强

确认或新增字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| context | String | RAG context |

如果已有 `context` 字段，不需要重复添加。

## 8. RagContextService 设计

新增：

```text
rag/application/RagContextService.java
```

职责：

- 封装 RAG Search 调用
- 控制 RAG 开关
- 控制 limit
- 控制 context 长度
- 将 RagSearchResultResponse 转为 RagReference
- 生成统一 contextText

依赖：

- `RagSearchApplicationService`
- `RagProperties`

## 8.1 buildContextForChat

```java
RagContext buildContextForChat(Long projectId, String query, String knowledgeBaseId, Integer limit, Boolean useRag);
```

逻辑：

1. 如果全局 RAG disabled，返回空 RagContext
2. 如果 chatEnabled false，返回空 RagContext
3. 如果 useRag 显式 false，返回空 RagContext
4. query 为空返回空 RagContext
5. 调用 `RagSearchApplicationService.search(...)`
6. 将结果转换为 references
7. 拼接 contextText
8. 返回 RagContext

## 8.2 buildContextForTask

```java
RagContext buildContextForTask(Long projectId, String query, String knowledgeBaseId, Integer limit, Boolean useRag);
```

逻辑：

1. 如果全局 RAG disabled，返回空 RagContext
2. 如果 agentEnabled false，返回空 RagContext
3. 如果 useRag 显式 false，返回空 RagContext
4. query 为空返回空 RagContext
5. 调用 `RagSearchApplicationService.search(...)`
6. 转 references
7. 拼接 contextText
8. 返回 RagContext

## 8.3 contextText 格式

统一格式：

```text
以下是从项目知识库检索到的相关上下文，仅供参考：

[Reference 1]
Title: {title}
File: {filePath}
Score: {score}
Content:
{snippet}

[Reference 2]
Title: {title}
File: {filePath}
Score: {score}
Content:
{snippet}
```

限制：

- 单个 snippet 最长 `maxChunkChars`
- 总 contextText 最长 `maxContextChars`
- 超出时截断并追加：

```text
... [context truncated]
```

## 8.4 Empty RagContext

提供静态方法：

```java
RagContext empty(String query);
```

返回：

- query = 原始 query
- contextText = ""
- references = empty list
- total = 0
- elapsedMs = 0

## 9. Chat 集成设计

## 9.1 sendMessage 流程调整

修改：

```text
chat/application/ChatApplicationService.java
```

当前流程：

```text
保存 user message
创建 assistant STREAMING message
更新 session lastMessageTime
返回 streamUrl
```

调整为：

```text
查询 session
校验 DEVELOPER+
保存 user message
执行 RAG Search
创建 assistant STREAMING message
将 RAG references 绑定到 assistant message
更新 session lastMessageTime
返回 streamUrl + references + ragUsed
```

### 9.2 Reference 落库策略

推荐：

- references 绑定到 assistant message
- 因为 assistant 回复使用了这些上下文

写入表：

```text
chat_message_reference
```

字段映射：

| ChatMessageReferenceEntity | RagReference |
|---|---|
| messageId | assistantMessage.id |
| projectId | session.projectId |
| referenceType | DOCUMENT |
| referenceId | chunkId |
| title | title |
| filePath | filePath |
| score | score |
| snippet | snippet |
| startLine | startLine |
| endLine | endLine |

### 9.3 SendChatMessageResponse

发送消息后返回：

```json
{
  "userMessageId": "205xxx",
  "assistantMessageId": "205yyy",
  "streamUrl": "/api/chat/sessions/205aaa/stream?messageId=205yyy",
  "ragUsed": true,
  "references": [
    {
      "referenceType": "DOCUMENT",
      "referenceId": "205chunk",
      "title": "API Design",
      "filePath": "docs/api-design.md",
      "score": 0.95,
      "snippet": "Agent Orchestrator..."
    }
  ]
}
```

### 9.4 getMessages

当前已返回 references。

需要确认：

- assistant message 的 references 可正确返回
- 无 RAG 结果时 references = []

## 10. Chat SSE 集成设计

修改：

```text
chat/application/ChatStreamService.java
```

### 10.1 done 事件增强

done event 增加：

```json
{
  "messageId": "205xxx",
  "status": "COMPLETED",
  "tokenUsage": 31,
  "ragUsed": true,
  "references": [
    {
      "referenceType": "DOCUMENT",
      "referenceId": "205chunk",
      "title": "API Design",
      "filePath": "docs/api-design.md",
      "score": 0.95,
      "snippet": "Agent Orchestrator..."
    }
  ]
}
```

### 10.2 token 事件不携带 references

token event 仍保持轻量：

```json
{
  "messageId": "205xxx",
  "content": "已收"
}
```

### 10.3 已完成消息重复 stream

如果 message 已经 COMPLETED：

- 直接返回 done
- done 中也应携带 references

## 11. Agent Orchestrator 集成设计

修改：

```text
orchestrator/application/AgentOrchestratorService.java
```

## 11.1 executeTask 流程调整

当前流程：

```text
查询 task
校验权限
校验状态
解析 agent
创建 execution
构建 prompt
调用 ModelGateway
写 model log
写 task log/event/artifact
更新 task/execution 状态
```

调整为：

```text
查询 task
校验权限
校验状态
解析 agent
构建 RAG query
执行 RAG Search
创建 execution
构建包含 RAG context 的 prompt
ModelRequest.context = ragContext.contextText
调用 ModelGateway
写 model log
写 task log/event/artifact
更新 task/execution 状态
返回 execution + references + ragUsed
```

## 11.2 RAG query 构建

建议：

```text
{task.title}

{task.description}

{request.instruction}
```

空值跳过。

## 11.3 Prompt 增强

原 Prompt 中增加：

```text
Project Knowledge Context:
{ragContext.contextText}
```

完整结构建议：

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

Project Knowledge Context:
{ragContext.contextText}

Constraints:
- 当前阶段使用 Mock Model Gateway
- 不调用真实大模型
- 不修改真实代码
- 不执行 Git 写操作
- 如果使用了上下文，请在输出中简要说明参考了哪些项目文档
```

## 11.4 ModelRequest.context

调用模型网关时：

```java
modelRequest.setContext(ragContext.getContextText());
```

## 11.5 Task Log 增强

新增日志：

```text
RAG_SEARCH
```

内容示例：

```text
RAG search completed, query="...", total=3, elapsedMs=8
```

如果 RAG disabled：

```text
RAG search skipped
```

如果无结果：

```text
RAG search completed, no references found
```

## 11.6 AgentExecutionResponse

返回：

```json
{
  "id": "205xxx",
  "status": "COMPLETED",
  "ragUsed": true,
  "references": [
    {
      "chunkId": "205chunk",
      "documentId": "205doc",
      "title": "API Design",
      "filePath": "docs/api-design.md",
      "score": 0.95,
      "snippet": "Agent Orchestrator..."
    }
  ]
}
```

## 12. Model Gateway 集成设计

修改：

```text
modelgateway/dto/ModelRequest.java
```

确认字段：

```java
private String context;
```

MockModelGateway 可根据 context 调整输出：

- 如果 context 非空，输出中增加：

```text
本次 Mock 执行已接收项目知识库上下文。
```

- 如果 context 为空，保持原输出。

不要改变真实模型接口设计方向。

## 13. 数据库设计

Milestone 8 原则上不新增表。

复用：

- `chat_message_reference`
- `agent_execution.input_prompt`
- `model_request_log`
- `ai_task_log`
- `ai_task_artifact`
- `document_chunk`

### 13.1 是否修改 agent_execution 表

默认不修改。

RAG references 不单独落表到 agent_execution。

原因：

- 当前可通过 `inputPrompt` 审计使用的上下文
- Chat references 已有专表
- Task artifact 可保存输出结果
- 后续如需 Agent references 表，可在新里程碑扩展

## 14. API 行为变化

## 14.1 POST /api/chat/sessions/{sessionId}/messages

新增可选请求字段：

```json
{
  "content": "Agent Orchestrator 是怎么执行任务的？",
  "agentIds": ["300002"],
  "stream": true,
  "useRag": true,
  "knowledgeBaseId": "205xxx",
  "ragLimit": 5
}
```

响应新增：

```json
{
  "ragUsed": true,
  "references": []
}
```

## 14.2 GET /api/chat/sessions/{sessionId}/stream

done event 新增：

```json
{
  "ragUsed": true,
  "references": []
}
```

## 14.3 POST /api/tasks/{taskId}/execute

新增可选请求字段：

```json
{
  "instruction": "请结合项目知识库执行任务",
  "useRag": true,
  "knowledgeBaseId": "205xxx",
  "ragLimit": 5
}
```

响应新增：

```json
{
  "ragUsed": true,
  "references": []
}
```

## 15. 权限设计

不新增权限模型。

权限沿用现有业务入口：

| 入口 | 权限 |
|---|---|
| Chat sendMessage | session.projectId DEVELOPER+ |
| Chat stream | session.projectId VIEWER+ |
| Task execute | task.projectId DEVELOPER+ |
| RAG search | projectId VIEWER+ |

RAG 集成内部调用时仍必须确保：

- knowledgeBaseId 属于当前 project
- 不允许跨项目检索
- references 只能引用当前项目的文档 chunk

## 16. 错误处理

### 16.1 Chat RAG 错误策略

Chat 发送消息时：

- 如果 RAG Search 失败，不应导致消息发送失败
- 应记录日志
- references 返回空
- ragUsed = false
- assistant mock 回复继续进行

原因：

- Chat 可用性优先
- RAG 是增强能力，不应阻塞基础聊天

### 16.2 Agent RAG 错误策略

Task execute 时：

- 如果 RAG Search 失败，不应导致任务执行失败
- 写 task log：`RAG_SEARCH_FAILED`
- ModelRequest.context 使用空字符串
- 继续调用 Mock Model Gateway

原因：

- 当前阶段 RAG 是上下文增强，不是强依赖

### 16.3 明确错误

以下仍应正常返回错误：

| 场景 | 错误 |
|---|---|
| 未登录 | UNAUTHORIZED |
| 无项目权限 | PROJECT_ACCESS_DENIED |
| task 不存在 | NOT_FOUND |
| session 不存在 | NOT_FOUND |
| 指定 knowledgeBaseId 不属于项目 | PROJECT_ACCESS_DENIED 或 BAD_REQUEST |
| task 状态不能执行 | CONFLICT |

## 17. 回归风险控制

必须保证：

- 不传 `useRag` 的旧 Chat 请求仍然成功
- 不传 `knowledgeBaseId` 的旧 Chat 请求仍然成功
- 不传 `useRag` 的旧 Task execute 请求仍然成功
- 项目没有知识库时 Chat / Task 仍然成功
- RAG 无结果时 Chat / Task 仍然成功
- SSE token 流不受 references 影响
- 已完成 message 重复 stream 仍然直接 done
- Task 状态机不被绕过

## 18. 验收标准

### 18.1 编译测试

必须通过：

```bash
cd backend
mvn compile
mvn test
```

### 18.2 手动验证前置

1. 启动 MySQL
2. 启动后端
3. 登录 admin
4. 创建 project
5. 创建 knowledge base
6. 上传包含 `Agent Orchestrator` 关键词的 Markdown 文档
7. 创建 chat session
8. 创建 PENDING task

## 19. Chat + RAG 验证

### 19.1 发送 Chat 消息并启用 RAG

请求：

```http
POST /api/chat/sessions/{sessionId}/messages
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "content": "Agent Orchestrator 是怎么执行任务的？",
  "agentIds": ["300002"],
  "stream": true,
  "useRag": true,
  "knowledgeBaseId": "{knowledgeBaseId}",
  "ragLimit": 5
}
```

期望：

- 返回 userMessageId
- 返回 assistantMessageId
- 返回 streamUrl
- ragUsed = true
- references 非空
- referenceType = DOCUMENT
- snippet/content 包含相关内容

### 19.2 查询消息列表

请求：

```http
GET /api/chat/sessions/{sessionId}/messages
Authorization: Bearer <token>
```

期望：

- user message 存在
- assistant message 存在
- assistant message references 非空

### 19.3 SSE stream done 携带 references

请求：

```http
GET /api/chat/sessions/{sessionId}/stream?messageId={assistantMessageId}
Authorization: Bearer <token>
```

期望：

- token event 正常
- done event status = COMPLETED
- done event ragUsed = true
- done event references 非空

### 19.4 Chat 禁用 RAG

请求体：

```json
{
  "content": "Agent Orchestrator 是怎么执行任务的？",
  "agentIds": ["300002"],
  "stream": true,
  "useRag": false
}
```

期望：

- 消息发送成功
- ragUsed = false
- references = []

## 20. Agent Orchestrator + RAG 验证

### 20.1 执行任务并启用 RAG

请求：

```http
POST /api/tasks/{taskId}/execute
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "instruction": "请结合项目知识库执行这个任务",
  "useRag": true,
  "knowledgeBaseId": "{knowledgeBaseId}",
  "ragLimit": 5
}
```

期望：

- execution.status = COMPLETED
- ragUsed = true
- references 非空
- outputContent 非空
- tokenUsage > 0

### 20.2 查询 execution 详情

请求：

```http
GET /api/agent-executions/{executionId}
Authorization: Bearer <token>
```

期望：

- inputPrompt 包含 `Project Knowledge Context`
- inputPrompt 包含文档 title 或 snippet
- references 非空

### 20.3 查询任务日志

请求：

```http
GET /api/tasks/{taskId}/logs
Authorization: Bearer <token>
```

期望至少包含：

- RAG_SEARCH
- ORCHESTRATOR_START
- MODEL_GATEWAY_REQUEST
- ORCHESTRATOR_DONE

### 20.4 查询模型日志

请求：

```http
GET /api/agent-executions/{executionId}/model-logs
Authorization: Bearer <token>
```

期望：

- provider = MOCK
- success = true
- totalTokens > 0

### 20.5 Agent 禁用 RAG

请求体：

```json
{
  "instruction": "不使用知识库执行任务",
  "useRag": false
}
```

期望：

- execution.status = COMPLETED
- ragUsed = false
- references = []
- inputPrompt 不包含知识库 chunk 内容

## 21. 异常场景验证

| 场景 | 期望 |
|---|---|
| 无 token 调 Chat sendMessage | UNAUTHORIZED |
| 无 token 调 Task execute | UNAUTHORIZED |
| knowledgeBaseId 不属于当前 project | BAD_REQUEST 或 PROJECT_ACCESS_DENIED |
| 项目无知识库且 useRag=true | Chat/Task 仍成功，references=[] |
| RAG 搜索无结果 | Chat/Task 仍成功，references=[] |
| COMPLETED task 重复 execute | CONFLICT |
| stream 已完成消息 | 直接 done，携带 references |

## 22. 完成报告模板

完成后请按以下格式输出：

```markdown
# Milestone 8 完成报告

## 1. 新增/修改文件清单

...

## 2. 新增配置项

...

## 3. 新增/变更 API 行为

...

## 4. Chat + RAG 集成流程

...

## 5. Agent Orchestrator + RAG 集成流程

...

## 6. RagContextService 设计与实现

...

## 7. References 落库与返回结果

...

## 8. 错误处理与降级策略

...

## 9. mvn compile / mvn test 结果

...

## 10. 手动接口验证结果

...

## 11. 回归验证结果

...

## 12. 是否可以进入 Milestone 9：真实模型网关接入

...
```

## 23. Milestone 9 预告

如果 Milestone 8 验证通过，下一阶段建议进入：

```text
Milestone 9: 真实模型网关接入
```

建议范围：

- OpenAI / Claude / DeepSeek / Qwen Provider 抽象
- ModelConfig 管理接入真实配置
- API Key 加密存储
- 模型调用超时与重试
- Token usage 精确统计
- 流式模型输出
- 模型错误降级到 Mock Provider
- 成本统计
- Prompt 注入防御基础策略

