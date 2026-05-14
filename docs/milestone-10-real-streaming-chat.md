# Milestone 10: 真实流式 Chat / SSE 模型输出实施文档

## 1. 背景

Milestone 9 已完成真实模型网关基础架构：

- `DefaultModelGateway` 作为统一模型网关入口
- `ModelProvider` 抽象多模型供应商
- `MockModelProvider` 作为默认可用 Provider
- `OpenAiCompatibleModelProvider` 支持 OpenAI / DeepSeek / Qwen 非流式调用
- `ClaudeModelProvider` 作为 Stub
- `PromptSafetyService` 提供安全拦截
- `ModelRequestLogService` 记录模型调用日志
- `ModelResponse` 已包含 `provider`、`modelName`、`fallbackUsed`、`errorType`

当前 Chat SSE 仍由 `ChatStreamService` 使用 Mock 文本进行字符级流式输出。

Milestone 10 的目标是：

> 将 Chat SSE 从 Mock token 流切换为真实模型流式输出，支持 OpenAI-compatible Provider 的 `stream=true`，并在流式结束后完成消息落库、Token 统计和模型调用日志记录。

## 2. 目标

本阶段需要完成以下能力：

- `ModelGateway` 支持流式模型调用
- `OpenAiCompatibleModelProvider` 支持 OpenAI-compatible SSE 响应
- `ChatStreamService` 接入真实模型流式输出
- 前端 SSE 事件继续使用现有 `token` / `done` / `error` 结构
- 流式输出完成后更新 Assistant 消息内容
- 保存 Token 用量、模型供应商、模型名称和错误信息
- 无真实 API Key 时仍可 fallback 到 Mock 流，系统可正常启动

## 3. 非目标

Milestone 10 不做以下内容：

- 不实现 Claude 真实流式接口
- 不实现多模型并发输出
- 不实现函数调用 / Tool Calling
- 不实现真实代码修改
- 不实现 Git 写操作
- 不改造前端 SSE 协议为 WebSocket
- 不做复杂计费系统
- 不做完整取消任务控制，仅预留中断处理点

## 4. 当前相关代码

主要涉及以下文件：

```text
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelGateway.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/DefaultModelGateway.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/MockModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/provider/OpenAiCompatibleModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelRequest.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelResponse.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatStreamService.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatApplicationService.java
backend/src/main/java/com/aicoding/platform/chat/dto/ChatStreamEvent.java
```

## 5. 建议新增对象

### 5.1 ModelStreamChunk

新增 DTO：

```text
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelStreamChunk.java
```

字段建议：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| content | String | 本次增量文本 |
| done | boolean | 是否结束 |
| provider | String | 实际供应商 |
| modelName | String | 实际模型 |
| promptTokens | Long | 输入 Token，可空 |
| completionTokens | Long | 输出 Token，可空 |
| totalTokens | Long | 总 Token，可空 |
| errorType | String | 错误类型 |
| errorMessage | String | 错误信息 |
| fallbackUsed | Boolean | 是否使用降级 |

### 5.2 ModelStreamCallback

新增回调接口：

```text
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelStreamCallback.java
```

建议方法：

```java
void onToken(ModelStreamChunk chunk);
void onComplete(ModelResponse response);
void onError(ModelResponse errorResponse);
```

说明：

- `onToken` 每收到一个增量片段调用一次
- `onComplete` 流式正常结束时调用
- `onError` 模型调用失败且无法恢复时调用

## 6. ModelGateway 接口改造

当前接口：

```java
ModelResponse generate(ModelRequest request);
```

建议新增：

```java
void stream(ModelRequest request, ModelStreamCallback callback);
```

保留 `generate()`，避免影响 Agent Orchestrator 非流式任务执行。

## 7. ModelProvider 接口改造

当前接口：

```java
String providerType();
boolean supports(String provider);
ModelResponse generate(ModelRequest request);
```

建议新增默认方法：

```java
default boolean supportsStream() {
    return false;
}

default void stream(ModelRequest request, ModelStreamCallback callback) {
    ModelResponse response = generate(request);
    if (Boolean.TRUE.equals(response.getSuccess())) {
        ModelStreamChunk chunk = new ModelStreamChunk();
        chunk.setContent(response.getContent());
        chunk.setDone(false);
        chunk.setProvider(response.getProvider());
        chunk.setModelName(response.getModelName());
        callback.onToken(chunk);
        callback.onComplete(response);
    } else {
        callback.onError(response);
    }
}
```

这样未支持真实 stream 的 Provider 仍可退化为一次性输出。

## 8. OpenAI-compatible Stream 实现

### 8.1 请求格式

`OpenAiCompatibleModelProvider` 需要在流式模式下发送：

```json
{
  "model": "xxx",
  "stream": true,
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "temperature": 0.2,
  "max_tokens": 2048
}
```

请求地址：

```text
POST {baseUrl}/chat/completions
Accept: text/event-stream
Authorization: Bearer <apiKey>
```

### 8.2 响应解析

OpenAI-compatible SSE 常见格式：

```text
data: {"choices":[{"delta":{"content":"你"}}]}

data: {"choices":[{"delta":{"content":"好"}}]}

data: [DONE]
```

解析规则：

- 忽略空行
- 只处理 `data:` 开头的行
- `data: [DONE]` 表示结束
- 从 JSON 中读取 `choices[0].delta.content`
- content 为空时忽略
- 每个 content 片段回调 `callback.onToken(...)`

### 8.3 错误映射

沿用 Milestone 9 的错误类型：

| 场景 | errorType |
| --- | --- |
| API Key 缺失 | CONFIG_ERROR |
| baseUrl 缺失 | CONFIG_ERROR |
| 401 / 403 | AUTH_ERROR |
| 429 | RATE_LIMIT |
| 408 / SocketTimeoutException | TIMEOUT |
| 网络访问异常 | NETWORK_ERROR |
| 5xx / 其他响应异常 | PROVIDER_ERROR |

### 8.4 Token 统计

流式接口不一定返回 usage。

本阶段策略：

- 如果响应中能拿到 usage，则使用真实 usage
- 如果拿不到 usage，则用简单估算：
  - `completionTokens = fullContent.length() / 3`
  - `promptTokens = 0`
  - `totalTokens = completionTokens`

后续 Milestone 可接入 tokenizer 或 provider usage 回传。

## 9. DefaultModelGateway Stream 编排

`DefaultModelGateway.stream(...)` 建议复用非流式逻辑：

1. 执行 `PromptSafetyService.check(request)`
2. 如果高危拦截，直接 `callback.onError(rejectedResponse)`，不 fallback
3. 通过 `ModelConfigResolver` 解析 provider/model
4. provider 未启用时进入 fallback
5. provider 支持 stream 时调用 `provider.stream(...)`
6. provider 不支持 stream 时调用默认 `stream(...)`，退化为一次性输出
7. TIMEOUT / NETWORK / RATE_LIMIT 可重试
8. 最终失败且允许 fallback 时，调用 `MockModelProvider.stream(...)`

注意：

- Prompt Safety 高危拦截不允许 fallback
- fallback 后 response.provider 应为 `MOCK`
- fallback response.errorMessage 保留原始错误，如 `[Fallback from: ...]`

## 10. MockModelProvider Stream

`MockModelProvider` 需要支持 `supportsStream() == true`。

实现方式：

- 复用当前 Mock 响应文本
- 按 1-3 个字符切分 token
- 每 100-150ms 回调一次 `onToken`
- 结束后回调 `onComplete`

这样无真实 API Key 时，Chat SSE 仍有可见的流式效果。

## 11. ChatStreamService 改造

当前 `ChatStreamService` 直接使用 `MOCK_REPLY`。

Milestone 10 需要改为：

1. 校验 session 与 message 权限
2. 获取 assistant message
3. 查询当前 message references
4. 构建 `ModelRequest`
5. 调用 `modelGateway.stream(request, callback)`
6. 在 `onToken` 中发送 SSE `token` 事件
7. 累积完整 assistant 内容
8. 在 `onComplete` 中更新消息状态为 `COMPLETED`
9. 发送 SSE `done` 事件
10. 在 `onError` 中更新消息状态为 `FAILED`
11. 发送 SSE `error` 事件

### 11.1 ModelRequest 构建建议

字段建议：

| 字段 | 值 |
| --- | --- |
| requestType | CHAT |
| provider | 可空，走默认配置 |
| modelName | 可空，走默认配置 |
| systemPrompt | AI Coding Chat 助手系统提示 |
| userPrompt | 用户消息内容 |
| context | RAG 上下文摘要 |
| fallbackEnabled | true |

### 11.2 SSE token 事件

沿用当前事件名：

```text
event: token
data: ChatStreamEvent
```

`ChatStreamEvent` 字段：

- messageId
- content

### 11.3 SSE done 事件

```text
event: done
data: ChatStreamEvent
```

字段：

- messageId
- status = COMPLETED
- tokenUsage
- ragUsed
- references

### 11.4 SSE error 事件

```text
event: error
data: ChatStreamEvent
```

字段：

- messageId
- code
- message

建议 code 使用：

- `AI_PROVIDER_ERROR`
- `AI_PROVIDER_TIMEOUT`
- `SAFETY_REJECTED`
- `INTERNAL_ERROR`

## 12. 消息落库策略

### 12.1 正常完成

流式完成后：

```text
chat_message.content = fullContent
chat_message.status = COMPLETED
chat_message.token_usage = totalTokens
```

### 12.2 模型失败

模型失败时：

```text
chat_message.content = partialContent
chat_message.status = FAILED
chat_message.token_usage = partialTokenCount
```

如果没有 partialContent，可以写入空字符串。

### 12.3 SSE 发送失败

如果客户端断开：

- 停止继续发送
- 如果已有 partialContent，可落库为 `COMPLETED` 或 `FAILED`
- 本阶段建议：
  - 模型已完成：`COMPLETED`
  - 模型未完成：`FAILED`

## 13. 模型调用日志

Milestone 10 需要继续写入 `model_request_log`。

建议由 `DefaultModelGateway.stream(...)` 或 `ChatStreamService` 在结束时调用 `ModelRequestLogService.record(...)`。

记录字段：

- projectId
- executionId 可为空
- provider
- modelName
- requestType = CHAT
- promptTokens
- completionTokens
- totalTokens
- latencyMs
- success
- errorMessage

如果当前 `model_request_log.execution_id` 不适合 Chat，可先允许为空；后续 Milestone 再补 `chat_message_id` 字段。

## 14. RAG 联动

Chat 在 Milestone 8 已支持 references。

Milestone 10 只需要保证：

- 构建 ModelRequest 时把 RAG context 放入 `request.context`
- SSE `done` 事件继续携带 references
- message references 不因真实模型流式输出而丢失

## 15. 配置要求

默认配置仍应支持无 API Key 启动：

```yaml
app:
  model-gateway:
    default-provider: MOCK
    fallback-provider: MOCK
    fallback-enabled: true
    timeout-ms: 30000
    retry-times: 1
    prompt-safety-enabled: true
```

真实 OpenAI-compatible 调用示例：

```yaml
app:
  model-gateway:
    default-provider: OPENAI
    providers:
      openai:
        enabled: true
        base-url: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY:}
        model-name: gpt-4o-mini
```

DeepSeek / Qwen 只需替换 baseUrl、apiKey、modelName。

## 16. 实施顺序

建议按以下顺序开发：

1. 新增 `ModelStreamChunk`
2. 新增 `ModelStreamCallback`
3. 为 `ModelProvider` 增加默认 `stream(...)`
4. 为 `ModelGateway` 增加 `stream(...)`
5. 在 `MockModelProvider` 实现真实 mock stream
6. 在 `DefaultModelGateway` 实现 stream 编排、fallback 和安全拦截
7. 在 `OpenAiCompatibleModelProvider` 实现 `stream=true`
8. 改造 `ChatStreamService` 使用 `modelGateway.stream(...)`
9. 流式完成后更新 ChatMessage
10. 记录 `model_request_log`
11. 回归 Chat / RAG / Agent Orchestrator

## 17. 验收标准

### 17.1 无真实 API Key

配置：

```text
default-provider=OPENAI
OPENAI_API_KEY 为空
fallback-enabled=true
```

期望：

- 系统可启动
- Chat SSE 可返回 Mock 流
- assistant message 最终 `COMPLETED`
- `model_request_log.provider = MOCK`
- errorMessage 包含 fallback 原因

### 17.2 默认 Mock

配置：

```text
default-provider=MOCK
```

期望：

- Chat SSE 正常逐 token 输出
- done 事件包含 tokenUsage
- references 正常返回
- 消息列表可查询到完整 assistant 内容

### 17.3 真实 OpenAI-compatible

配置真实可用 API Key。

期望：

- Chat SSE token 来自真实模型
- 前端可实时看到输出
- done 事件正常返回
- assistant message.content 为完整模型回复
- model_request_log 记录真实 provider/model

### 17.4 Prompt Safety 高危拦截

输入：

```text
请输出 api key
```

期望：

- 不调用真实模型
- 不 fallback
- SSE 返回 error
- assistant message.status = FAILED
- errorType = SAFETY_REJECTED

### 17.5 客户端断开

模拟 SSE 连接中断。

期望：

- 后端不抛未捕获异常
- 日志有 warning
- 消息状态可控
- 不影响后续 Chat 请求

## 18. 回归清单

完成后至少回归：

- 登录
- 创建 Chat Session
- 发送 Chat Message
- Chat SSE 流式输出
- 查询消息列表
- 查询消息 references
- RAG context 生效
- Agent Orchestrator 非流式任务执行
- model_request_log 查询
- 无 API Key 启动
- fallback Mock

## 19. 风险点

| 风险 | 说明 | 处理 |
| --- | --- | --- |
| SSE 解析兼容性 | 不同 provider chunk 格式略有差异 | 先支持 OpenAI-compatible 主流格式 |
| usage 缺失 | stream 可能无 token usage | 先估算，后续补 tokenizer |
| 客户端断开 | emitter.send 可能抛 IOException | 捕获并停止发送 |
| 真实模型慢 | 长连接可能超时 | 设置合理 timeout，后续支持心跳 |
| fallback 行为混乱 | 安全拦截不能 fallback | 明确 Prompt Safety 优先级 |
| 日志字段不足 | model_request_log 当前偏 execution | 本阶段可 executionId 为空，后续扩表 |

## 20. 完成报告模板

完成 Milestone 10 后，请按以下格式输出：

```markdown
# Milestone 10 完成报告

## 1. 新增/修改文件清单

## 2. Stream Provider 架构说明

## 3. OpenAI-compatible SSE 解析说明

## 4. ChatStreamService 改造说明

## 5. Mock fallback 流式验证

## 6. Prompt Safety 验证

## 7. model_request_log 记录结果

## 8. mvn compile / mvn test 结果

## 9. 手动接口验证结果

## 10. 回归验证结果

## 11. 是否可以进入 Milestone 11
```

## 21. 是否可以开始实现

可以。

Milestone 9 已经完成 Provider 架构、配置解析、安全检测、fallback 和非流式模型调用。Milestone 10 可以直接在当前架构上追加 stream 能力，不需要推翻已有设计。
