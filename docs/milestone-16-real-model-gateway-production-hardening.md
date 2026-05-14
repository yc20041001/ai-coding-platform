# Milestone 16: 真实模型网关接入与生产级加固实施文档

## 1. 背景与目标

Milestone 15 已完成自动化测试与演示数据固化，当前项目已经具备稳定本地演示条件：

- 后端集成测试通过
- 前端 E2E 测试通过
- 后端 compile / test / package 通过
- 前端 typecheck / build / e2e 通过
- 演示数据脚本和一键检查脚本就绪

当前 AI 能力仍以 Mock Model Gateway 为主，适合本地演示和回归测试，但距离真实使用还缺少：

- 真实模型 Provider 接入验证
- 模型配置管理
- API Key 安全处理
- 调用超时、重试、fallback 策略
- 统一错误码与错误降级
- Token / 成本统计
- Prompt Safety 策略强化
- 前端模型配置与用量展示

Milestone 16 的目标是：

> 将 Model Gateway 从 Mock 演示能力升级为可配置、可观测、可降级、可审计的真实模型调用基础设施，并保持 Mock 模式可用于测试和演示。

## 2. 实施边界

### 2.1 本阶段要做

- 完善 OpenAI Compatible Provider
- 完善 Claude Provider
- 增加 DeepSeek / Qwen / Gemini 配置模板
- 统一模型配置解析与校验
- 强化 API Key 安全处理
- 增强超时、重试、fallback 策略
- 标准化模型调用错误码
- 完善 token usage 与成本统计
- 增强 Prompt Safety
- 增加模型连通性测试接口
- 前端增加模型配置页面
- 前端增加模型用量与调用日志增强展示
- 保持所有自动化测试可在 MOCK 模式下稳定通过

### 2.2 本阶段不做

- 不做模型微调
- 不做私有模型部署
- 不做复杂多租户计费系统
- 不做企业密钥托管服务集成
- 不做完整 Prompt 管理平台
- 不做复杂 Agent 自动工具执行
- 不做真实代码写入或 Git 写操作
- 不把真实 API Key 写入仓库

## 3. 核心设计原则

- Mock 永远保留，用于测试、演示和 fallback
- 真实 Provider 可插拔
- API Key 不返回前端明文
- 日志不输出完整 prompt 中的敏感字段和 API Key
- 失败可观测，失败不吞错
- Chat SSE 与 Task Execute 都走统一 Model Gateway
- Model Request Log 记录足够排查问题的信息
- 成本统计先做基础估算，后续可替换为精确计费

## 4. 建议新增/修改文件

### 4.1 后端新增

```text
backend/src/main/java/com/aicoding/platform/modelgateway/domain/ModelGatewayErrorCode.java
backend/src/main/java/com/aicoding/platform/modelgateway/domain/ModelProviderCapability.java
backend/src/main/java/com/aicoding/platform/modelgateway/domain/ModelPricingRule.java

backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelConnectionTestRequest.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelConnectionTestResponse.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelProviderOptionResponse.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelConfigRequest.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelConfigResponse.java
backend/src/main/java/com/aicoding/platform/modelgateway/dto/ModelUsageCostResponse.java

backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelConfigApplicationService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelConnectionTestService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelPricingService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelSecretMaskingService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/PromptSafetyPolicyService.java

backend/src/main/java/com/aicoding/platform/modelgateway/controller/ModelConfigController.java
```

### 4.2 后端修改

```text
backend/src/main/java/com/aicoding/platform/modelgateway/application/DefaultModelGateway.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/OpenAiCompatibleModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ClaudeModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/MockModelProvider.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelRequestLogService.java
backend/src/main/java/com/aicoding/platform/orchestrator/application/AgentOrchestratorService.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatStreamService.java
backend/src/main/java/com/aicoding/platform/observability/application/ModelUsageApplicationService.java
```

### 4.3 数据库迁移

如现有 `model_config` 表字段足够，可不新增迁移。

如需要增强，新增：

```text
backend/src/main/resources/db/migration/V10__alter_model_config_production_fields.sql
```

建议字段：

```sql
ALTER TABLE model_config
  ADD COLUMN base_url VARCHAR(512) NULL,
  ADD COLUMN api_key_masked VARCHAR(128) NULL,
  ADD COLUMN timeout_ms BIGINT NOT NULL DEFAULT 30000,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 2,
  ADD COLUMN fallback_enabled TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN stream_enabled TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN input_price_per_1k DECIMAL(12, 6) NULL,
  ADD COLUMN output_price_per_1k DECIMAL(12, 6) NULL,
  ADD COLUMN last_test_time DATETIME NULL,
  ADD COLUMN last_test_success TINYINT NULL,
  ADD COLUMN last_test_error TEXT NULL;
```

注意：

- 不存储明文 API Key，优先从环境变量读取
- 若必须支持数据库存储密钥，本阶段只做接口预留，不落真实密钥

### 4.4 前端新增/修改

```text
frontend/src/modules/model/api.ts
frontend/src/modules/model/pages/ModelConfigPage.vue
frontend/src/modules/model/components/ModelProviderCard.vue
frontend/src/modules/model/components/ModelConnectionTestDialog.vue
frontend/src/modules/model/components/ModelUsageCostPanel.vue
frontend/src/app/router/index.ts
frontend/src/app/layouts/BasicLayout.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue
```

## 5. Provider 接入要求

### 5.1 OpenAI Compatible Provider

支持：

- `baseUrl`
- `apiKey`
- `modelName`
- 非流式 `chat/completions`
- 流式 SSE `stream=true`
- timeout
- HTTP error 解析
- rate limit error 解析

兼容目标：

```text
OpenAI
DeepSeek
Qwen OpenAI-Compatible
本地 OpenAI-Compatible 网关
```

请求示例：

```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "You are a coding assistant."},
    {"role": "user", "content": "请总结这个任务。"}
  ],
  "temperature": 0.2,
  "stream": false
}
```

### 5.2 Claude Provider

支持：

- `baseUrl`
- `apiKey`
- `modelName`
- Messages API
- stream event 解析
- timeout
- HTTP error 解析

请求示例：

```json
{
  "model": "claude-3-5-sonnet-latest",
  "max_tokens": 1024,
  "messages": [
    {"role": "user", "content": "请总结这个任务。"}
  ]
}
```

### 5.3 Mock Provider

要求：

- 保持默认可用
- 保持测试稳定
- 支持非流式和流式
- 不依赖外部网络
- 可通过环境变量强制启用

## 6. 模型配置策略

### 6.1 配置来源优先级

建议优先级：

1. 项目级模型配置
2. 系统默认模型配置
3. 环境变量
4. Mock fallback

### 6.2 环境变量模板

`.env.example` 建议新增：

```bash
# Model Gateway
MODEL_GATEWAY_PROVIDER=MOCK
MODEL_GATEWAY_DEFAULT_MODEL=mock-agent-model

# OpenAI Compatible
OPENAI_COMPATIBLE_BASE_URL=https://api.openai.com/v1
OPENAI_COMPATIBLE_API_KEY=
OPENAI_COMPATIBLE_MODEL=gpt-4.1-mini

# Claude
CLAUDE_BASE_URL=https://api.anthropic.com
CLAUDE_API_KEY=
CLAUDE_MODEL=claude-3-5-sonnet-latest

# DeepSeek
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_API_KEY=
DEEPSEEK_MODEL=deepseek-chat

# Qwen
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_API_KEY=
QWEN_MODEL=qwen-plus
```

## 7. API Key 安全要求

必须做到：

- 不在日志输出 API Key
- 不在接口响应返回 API Key 明文
- 前端只展示 masked key，例如 `sk-****abcd`
- `.env` 不提交 Git
- `.env.example` 只放空值或示例占位
- 异常信息不要包含 Authorization header

建议新增：

```text
ModelSecretMaskingService.mask(String value)
```

规则：

- 长度小于等于 8：全部显示为 `****`
- 长度大于 8：保留前 3 位和后 4 位，中间 `****`

## 8. 错误码标准化

新增枚举：

```text
ModelGatewayErrorCode
```

建议值：

```text
CONFIG_MISSING
API_KEY_MISSING
PROVIDER_NOT_FOUND
MODEL_NOT_FOUND
PROMPT_BLOCKED
RATE_LIMITED
TIMEOUT
NETWORK_ERROR
AUTH_FAILED
BAD_RESPONSE
STREAM_INTERRUPTED
FALLBACK_USED
UNKNOWN
```

错误映射：

| 场景 | 错误码 |
|---|---|
| 没配置 Provider | CONFIG_MISSING |
| API Key 为空 | API_KEY_MISSING |
| HTTP 401 / 403 | AUTH_FAILED |
| HTTP 429 | RATE_LIMITED |
| Socket timeout | TIMEOUT |
| IOException | NETWORK_ERROR |
| 响应 JSON 解析失败 | BAD_RESPONSE |
| Prompt Safety 拦截 | PROMPT_BLOCKED |

## 9. 超时、重试与 Fallback

### 9.1 超时

默认：

```text
connectTimeout: 10s
readTimeout: 60s
streamTimeout: 120s
```

### 9.2 重试

只允许对以下错误重试：

- TIMEOUT
- NETWORK_ERROR
- RATE_LIMITED

不允许重试：

- AUTH_FAILED
- API_KEY_MISSING
- CONFIG_MISSING
- PROMPT_BLOCKED

### 9.3 Fallback

策略：

- 默认 fallback 到 Mock Provider
- fallback 必须写入 model_request_log
- fallback 结果要标识 `fallbackUsed=true`
- Prompt Safety 拦截不允许 fallback

## 10. Token 与成本统计

### 10.1 Token Usage

优先使用 Provider 返回值：

```text
promptTokens
completionTokens
totalTokens
```

如果 Provider 没返回：

```text
promptTokens = prompt.length / 3
completionTokens = output.length / 3
totalTokens = promptTokens + completionTokens
```

### 10.2 成本估算

新增 `ModelPricingService`：

```text
inputCost = promptTokens / 1000 * inputPricePer1k
outputCost = completionTokens / 1000 * outputPricePer1k
totalCost = inputCost + outputCost
```

前端 Observability 可展示：

- requestCount
- successCount
- failureCount
- totalTokens
- estimatedCost
- provider breakdown
- model breakdown

## 11. Prompt Safety 强化

当前阶段做基础策略：

- 检测 prompt injection 常见表达
- 检测泄露密钥类请求
- 检测要求绕过系统规则类请求
- 检测危险命令类请求

建议规则：

```text
ignore previous instructions
reveal your system prompt
print api key
show secret
delete database
drop table
rm -rf
curl ... | sh
```

行为：

- 命中高危规则：阻断，返回 PROMPT_BLOCKED
- 命中中危规则：记录 warning，可继续
- 所有拦截写入 audit log / model request log

## 12. 后端 API 设计

### 12.1 查询 Provider 选项

```http
GET /api/model-gateway/providers
```

权限：

```text
ADMIN
```

响应：

```json
{
  "code": "OK",
  "data": [
    {
      "provider": "MOCK",
      "displayName": "Mock Provider",
      "supportsStream": true,
      "requiresApiKey": false
    }
  ]
}
```

### 12.2 查询模型配置

```http
GET /api/model-gateway/configs
```

权限：

```text
ADMIN
```

### 12.3 创建或更新模型配置

```http
POST /api/model-gateway/configs
PUT /api/model-gateway/configs/{configId}
```

权限：

```text
ADMIN
```

注意：

- 请求可以接收 apiKey，但响应不能返回 apiKey 明文
- 如 apiKey 为空，表示不更新密钥

### 12.4 测试模型连接

```http
POST /api/model-gateway/test-connection
```

权限：

```text
ADMIN
```

请求：

```json
{
  "provider": "OPENAI_COMPATIBLE",
  "baseUrl": "https://api.openai.com/v1",
  "modelName": "gpt-4.1-mini",
  "apiKey": "sk-xxx"
}
```

响应：

```json
{
  "success": true,
  "latencyMs": 820,
  "message": "Connection test passed",
  "maskedApiKey": "sk-****abcd"
}
```

### 12.5 查询模型成本汇总

```http
GET /api/observability/model-usage/cost-summary
GET /api/projects/{projectId}/observability/model-usage/cost-summary
```

权限：

```text
ADMIN
```

## 13. 前端页面设计

### 13.1 Model Config 页面

新增路由：

```text
/model-gateway
```

仅 ADMIN 可见。

页面结构：

- Provider Cards
- Model Config Table
- Create / Edit Dialog
- Test Connection Dialog
- Usage Cost Panel

字段：

- provider
- modelName
- baseUrl
- status
- streamEnabled
- fallbackEnabled
- timeoutMs
- maxRetries
- maskedApiKey
- lastTestSuccess
- lastTestTime
- lastTestError

### 13.2 Observability 增强

在 Observability 页面增加：

- provider 维度统计
- model 维度统计
- estimatedCost
- failure rate
- fallback count

## 14. 测试要求

### 14.1 后端测试

必须保证：

```bash
cd backend
mvn clean compile
mvn test
```

新增测试建议：

```text
ModelSecretMaskingServiceTest
ModelPricingServiceTest
PromptSafetyPolicyServiceTest
MockModelProviderTest
DefaultModelGatewayFallbackTest
ModelConnectionTestServiceTest
```

必须覆盖：

- Mock 调用成功
- API Key masking
- Provider 不存在
- API Key 缺失
- Prompt Safety 阻断
- timeout/network/rate limit 可重试
- auth failed 不重试
- fallback 使用 Mock
- fallback 记录 model request log

### 14.2 前端测试

必须保证：

```bash
cd frontend
npm run typecheck
npm run build
```

如已有 Playwright：

```bash
npm run test:e2e
```

建议新增 E2E：

- ADMIN 可进入 Model Gateway 页面
- 非 ADMIN 不显示 Model Gateway 菜单
- Mock Provider 配置可展示
- Test Connection 成功/失败状态可展示

## 15. 验收标准

### 15.1 必须通过

- Mock 模式仍然稳定可用
- Chat SSE 可继续流式输出
- Task Execute 可继续走 Orchestrator
- OpenAI Compatible Provider 可完成连接测试
- Claude Provider 可完成连接测试，若无 key 应清晰提示
- API Key 不出现在接口响应和日志
- Model Request Log 记录 provider/model/token/latency/success/error
- fallback 逻辑可验证
- Prompt Safety 高危请求被阻断
- 后端 compile/test 通过
- 前端 typecheck/build 通过

### 15.2 不允许出现

- 真实 API Key 写入仓库
- 前端返回 API Key 明文
- 日志打印 Authorization header
- Prompt Safety 拦截后 fallback 到 Mock
- 真实 Provider 失败导致 Chat/Task 整体不可用且无错误信息
- Mock 模式测试被真实 Provider 配置影响

## 16. 完成报告模板

```markdown
# Milestone 16 完成报告

## 1. 新增/修改文件清单

| 文件 | 说明 |
|---|---|
|  |  |

## 2. Provider 接入结果

| Provider | 非流式 | 流式 | 连接测试 | 说明 |
|---|---|---|---|---|
| MOCK |  |  |  |  |
| OPENAI_COMPATIBLE |  |  |  |  |
| CLAUDE |  |  |  |  |

## 3. 安全加固

| 项目 | 结果 | 说明 |
|---|---|---|
| API Key masking |  |  |
| 日志脱敏 |  |  |
| Prompt Safety |  |  |
| 响应不返回明文 Key |  |  |

## 4. 错误处理 / 重试 / Fallback

| 场景 | 结果 |
|---|---|
| TIMEOUT retry |  |
| RATE_LIMIT retry |  |
| AUTH_FAILED no retry |  |
| Mock fallback |  |
| Prompt blocked no fallback |  |

## 5. Token / 成本统计

| 项目 | 结果 |
|---|---|
| promptTokens |  |
| completionTokens |  |
| totalTokens |  |
| estimatedCost |  |

## 6. 前端页面

| 页面/组件 | 结果 |
|---|---|
| Model Gateway 菜单 |  |
| Model Config 页面 |  |
| Test Connection Dialog |  |
| Usage Cost Panel |  |
| Observability 增强 |  |

## 7. 构建与测试

| 命令 | 结果 |
|---|---|
| backend mvn clean compile |  |
| backend mvn test |  |
| frontend npm run typecheck |  |
| frontend npm run build |  |
| frontend npm run test:e2e |  |

## 8. 已知限制

- 

## 9. 结论

是否可以进入 Milestone 17：GitHub OAuth + PR Review：

- [ ] 是
- [ ] 否
```

## 17. 给 Claude 的执行提示词

可以直接发送以下内容给 Claude：

```text
请根据项目中的文档执行 Milestone 16：真实模型网关接入与生产级加固。

文档路径：
docs/milestone-16-real-model-gateway-production-hardening.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend、frontend、docs、scripts 目录结构。
2. 本阶段是在 Milestone 15 已完成自动化测试与演示数据固化基础上，增强真实模型网关能力。
3. 不新增大业务模块。
4. 不重构已验证通过的 Auth、Project、Task、Chat、RAG、Observability 核心逻辑。
5. 不破坏 Mock Provider，Mock 必须继续作为默认测试和演示模式。
6. 不把真实 API Key 写入仓库、日志、测试、README 或 .env.example。
7. 不执行真实代码写入或 Git 写操作。
8. 不做模型微调、私有模型部署、CI/CD 或生产 Kubernetes。
9. 可以修复实现过程中发现的明确 bug，但必须说明原因和影响范围。
10. 所有真实 Provider 配置必须通过环境变量或后台配置读取，接口响应不能返回 API Key 明文。
11. Chat SSE 和 Task Execute 必须继续走统一 Model Gateway。
12. Prompt Safety 高危拦截后不得 fallback 到 Mock。
13. 真实 Provider 失败时要有明确错误码、日志和可观测记录。

需要实现：
1. 完善 OpenAI Compatible Provider 的非流式与流式调用。
2. 完善 Claude Provider 的非流式与流式调用。
3. 增加 DeepSeek / Qwen / Gemini 配置模板。
4. 增加 ModelGatewayErrorCode。
5. 增加模型配置查询、创建/更新、连接测试接口。
6. 增加 API Key masking 和日志脱敏。
7. 增强 timeout / retry / fallback 策略。
8. 增强 Prompt Safety 策略。
9. 增加 token usage 与 estimatedCost 统计。
10. 增强 model_request_log 记录 provider/model/token/latency/success/error/fallback。
11. 前端新增 Model Gateway 配置页面，仅 ADMIN 可见。
12. 前端新增 Test Connection Dialog。
13. 前端增强 Observability 的模型用量/成本展示。
14. 更新 .env.example、frontend/.env.example、README.md。
15. 增加必要的后端单元测试或集成测试。
16. 如已有 Playwright，补充最小前端 E2E。

完成后必须执行：
后端：
cd backend
mvn clean compile
mvn test

前端：
cd frontend
npm run typecheck
npm run build

如已有 E2E：
cd frontend
npm run test:e2e

手动验证：
1. Mock 模式 Chat SSE 正常。
2. Mock 模式 Task Execute 正常。
3. 无真实 API Key 时真实 Provider 连接测试返回清晰错误。
4. 有 API Key 环境变量时 OpenAI Compatible 连接测试可通过。
5. API Key 不在前端响应和后端日志中明文出现。
6. Prompt Safety 高危输入被阻断。
7. fallback 使用 Mock 且写入 model_request_log。
8. Observability 可看到模型调用统计。

完成后按以下格式输出：
1. 新增/修改文件清单
2. Provider 接入结果
3. 安全加固结果
4. 错误处理 / 重试 / Fallback 验证
5. Token / 成本统计实现
6. 前端 Model Gateway 页面实现
7. 构建与测试结果
8. 手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 17：GitHub OAuth + PR Review

现在开始执行，不要只给计划。
```
