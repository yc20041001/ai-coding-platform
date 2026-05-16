# Milestone 29: 后端测试覆盖与质量门增强

## 1. 背景

当前项目已经完成：

- Milestone 27：E2E 稳定性修复与发布质量收口。
- Milestone 28：前端性能优化与包体治理。

现在前端质量门已经稳定：

- TypeScript typecheck 通过。
- Vite build 通过。
- Playwright E2E 13/13 连续通过。
- Bundle chunk 已完成治理。

后端目前已经有 70 个测试通过，但系统规模已经扩展到多个关键模块：

- Auth / JWT / Security
- Project / Member / Repository
- Task / Agent / Orchestrator
- Chat SSE
- RAG / Knowledge Base
- Model Gateway / Prompt Safety / Provider Fallback
- GitHub OAuth / PR Review
- Audit / Observability
- Production scripts / Docker / Deployment

Milestone 29 的目标是补齐后端测试覆盖矩阵和质量门，让后端在继续演进时有更可靠的回归保障。

> 验收目标：后端关键服务、异常路径、安全边界和外部集成降级逻辑都有明确测试覆盖；`mvn test` 稳定通过；新增后端测试策略与覆盖报告模板。

## 2. 严格边界

执行本阶段必须遵守：

1. 不新增业务功能。
2. 不重写后端架构。
3. 不更换测试框架。
4. 不引入真实外部 API 调用。
5. 不要求真实 OpenAI / Claude / DeepSeek / GitHub token。
6. 不改生产数据库数据。
7. 不提交真实密钥。
8. 不让测试依赖不可控网络。
9. 不让测试依赖本机已有脏数据。
10. 不通过删除断言来通过测试。
11. 不把关键失败测试标记为 disabled。
12. 不绕过 Spring Security。
13. 不破坏现有 70 个测试。

允许做：

- 新增单元测试。
- 新增 Spring Boot 集成测试。
- 新增测试 helper / fixture。
- 新增 test profile 配置。
- 新增 Mock provider / fake GitHub client 测试替身。
- 小范围修复测试暴露出的真实 bug。
- 增强 Maven Surefire / test 脚本。
- 新增测试文档和 coverage report 模板。

## 3. 总目标

实现 7 个能力：

1. Backend Test Matrix
   - 明确模块、风险、测试类型、覆盖状态。

2. Service Unit Tests
   - 对纯业务服务和边界逻辑补测试。

3. Integration Tests
   - 对核心 API 链路补充认证、权限、异常路径测试。

4. Security Tests
   - JWT 类型校验。
   - 无 token / 错 token / refresh token 访问保护资源。
   - 权限不足路径。

5. Model Gateway Tests
   - Prompt safety。
   - Provider fallback。
   - Error code mapping。
   - Secret masking。
   - Cost estimation。

6. RAG / Chat / Task Edge Tests
   - Chunking 边界。
   - Empty result。
   - SSE message completion。
   - Task illegal transition。

7. Quality Gate
   - `mvn test` 稳定通过。
   - 测试报告文档化。
   - release-check 里明确后端测试 gate。

## 4. 执行前必须阅读

执行前先阅读：

```text
backend/pom.xml
backend/src/test/resources/application-test.yml
backend/src/test/java/com/aicoding/platform/support/IntegrationTestBase.java
backend/src/test/java/com/aicoding/platform/support/TestJsonHelper.java
backend/src/test/java/com/aicoding/platform/auth/AuthIntegrationTest.java
backend/src/test/java/com/aicoding/platform/project/ProjectIntegrationTest.java
backend/src/test/java/com/aicoding/platform/task/TaskOrchestratorIntegrationTest.java
backend/src/test/java/com/aicoding/platform/chat/ChatIntegrationTest.java
backend/src/test/java/com/aicoding/platform/rag/RagIntegrationTest.java
backend/src/test/java/com/aicoding/platform/github/application/PrReviewApplicationServiceTest.java
backend/src/test/java/com/aicoding/platform/github/application/GithubPropertiesTest.java
backend/src/main/java/com/aicoding/platform/security/JwtTokenProvider.java
backend/src/main/java/com/aicoding/platform/security/filter/JwtAuthenticationFilter.java
backend/src/main/java/com/aicoding/platform/auth/application/AuthApplicationService.java
backend/src/main/java/com/aicoding/platform/task/application/TaskApplicationService.java
backend/src/main/java/com/aicoding/platform/orchestrator/application/AgentOrchestratorService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/DefaultModelGateway.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/PromptSafetyService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelSecretMaskingService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelPricingService.java
backend/src/main/java/com/aicoding/platform/rag/application/DocumentChunkService.java
backend/src/main/java/com/aicoding/platform/rag/application/RagSearchApplicationService.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatApplicationService.java
backend/src/main/java/com/aicoding/platform/chat/application/ChatStreamService.java
backend/src/main/java/com/aicoding/platform/github/application/PrReviewApplicationService.java
docs/testing-strategy.md
scripts/run-backend-checks.sh
scripts/release-checklist.sh
```

如果某些文件不存在，先说明实际情况，再选择最小可行替代方案。

## 5. 建议新增 / 修改文件

### 5.1 测试文件

建议新增：

```text
backend/src/test/java/com/aicoding/platform/security/JwtTokenProviderTest.java
backend/src/test/java/com/aicoding/platform/security/JwtAuthenticationIntegrationTest.java
backend/src/test/java/com/aicoding/platform/task/TaskStateMachineTest.java
backend/src/test/java/com/aicoding/platform/rag/DocumentChunkServiceTest.java
backend/src/test/java/com/aicoding/platform/rag/RagSearchApplicationServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/PromptSafetyServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/ModelSecretMaskingServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/ModelPricingServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/DefaultModelGatewayTest.java
backend/src/test/java/com/aicoding/platform/chat/ChatApplicationServiceTest.java
backend/src/test/java/com/aicoding/platform/orchestrator/AgentOrchestratorServiceTest.java
backend/src/test/java/com/aicoding/platform/common/GlobalExceptionHandlerIntegrationTest.java
```

不要求一次性全部实现。优先覆盖高风险模块：

1. Security / JWT。
2. Model Gateway。
3. Task state machine。
4. RAG chunking/search。
5. Chat / SSE。

### 5.2 Test Support

可新增：

```text
backend/src/test/java/com/aicoding/platform/support/TestDataFactory.java
backend/src/test/java/com/aicoding/platform/support/MockModelProviderFactory.java
backend/src/test/java/com/aicoding/platform/support/TestAuthHelper.java
```

要求：

- 减少重复 JSON 拼接。
- 生成唯一测试数据。
- 不隐藏断言。
- 不让测试 helper 变成复杂框架。

### 5.3 文档

新增：

```text
docs/backend-test-matrix.md
docs/backend-testing-guide.md
docs/backend-coverage-report-template.md
docs/milestone-29-validation-report-template.md
```

修改：

```text
docs/testing-strategy.md
README.md
scripts/run-backend-checks.sh
scripts/release-checklist.sh
```

## 6. 后端测试矩阵要求

新增：

```text
docs/backend-test-matrix.md
```

必须包含：

| Module | Risk | Current Tests | Required Tests | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| Auth / JWT | Token misuse | Integration | access/refresh/type/expired | P0 | ... |
| Project / Member | Permission errors | Integration | owner/viewer/maintainer | P1 | ... |
| Task / Agent | State corruption | Integration | state machine illegal transitions | P0 | ... |
| Chat SSE | Stream lifecycle | Integration | streaming/completed/error | P1 | ... |
| RAG | Bad chunks/search | Integration | chunk overlap/search empty | P1 | ... |
| Model Gateway | Cost/security/fallback | Unit | safety/masking/fallback/error | P0 | ... |
| GitHub PR Review | External API / parsing | Unit | JSON parse/patch limit/error | P1 | ... |
| Observability / Audit | Missing logs | Integration | audit not blocking flow | P2 | ... |

## 7. 重点测试要求

### 7.1 Security / JWT

必须测试：

1. access token 包含 `type=access`。
2. refresh token 包含 `type=refresh`。
3. refresh token 不能访问受保护 API。
4. access token 不能调用 refresh 接口。
5. 无 token 返回 `UNAUTHORIZED`。
6. 篡改 token 返回 `UNAUTHORIZED`。

优先文件：

```text
backend/src/test/java/com/aicoding/platform/security/JwtTokenProviderTest.java
backend/src/test/java/com/aicoding/platform/security/JwtAuthenticationIntegrationTest.java
```

### 7.2 Task State Machine

必须测试：

1. PENDING → RUNNING 合法。
2. RUNNING → COMPLETED 合法。
3. RUNNING → FAILED 合法。
4. RUNNING → CANCELED 合法。
5. CANCELED → RUNNING 非法。
6. COMPLETED → RUNNING 非法。
7. FAILED → PENDING retry 合法，且清理 errorMessage/startTime/endTime。
8. retryCount >= maxRetryCount 时 retry 非法。

优先文件：

```text
backend/src/test/java/com/aicoding/platform/task/TaskStateMachineTest.java
```

### 7.3 Model Gateway

必须测试：

1. PromptSafetyService 能阻止高危 prompt。
2. Warning prompt 不阻塞但可记录。
3. API key 被 mask。
4. Bearer token 被 mask。
5. Cost estimation 对常见模型返回非负数。
6. Provider timeout / network / rate limit 能映射 error code。
7. fallback enabled 时真实 provider 失败后使用 mock。
8. fallback disabled 时返回失败。

优先文件：

```text
backend/src/test/java/com/aicoding/platform/modelgateway/PromptSafetyServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/ModelSecretMaskingServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/ModelPricingServiceTest.java
backend/src/test/java/com/aicoding/platform/modelgateway/DefaultModelGatewayTest.java
```

### 7.4 RAG

必须测试：

1. 空文本不会崩溃。
2. 短文本产生 1 个 chunk。
3. 长文本按 chunkSize 分片。
4. overlap 生效。
5. token count 不为 0。
6. content hash 稳定。
7. search 无结果返回空。
8. search limit 生效。

优先文件：

```text
backend/src/test/java/com/aicoding/platform/rag/DocumentChunkServiceTest.java
backend/src/test/java/com/aicoding/platform/rag/RagSearchApplicationServiceTest.java
```

### 7.5 Chat

必须测试：

1. 创建 session 权限。
2. sendMessage 生成 user message + assistant message。
3. assistant 初始状态为 STREAMING。
4. getMessages 包含 references。
5. 非项目成员不能访问。
6. stream completed 后状态变为 COMPLETED。

如果 SSE 实时测试复杂，可以先覆盖服务层状态变化与 controller 基础接口。

### 7.6 GitHub PR Review

已有 `PrReviewApplicationServiceTest`，需要检查是否覆盖：

1. JSON code block parse。
2. 非 JSON 输出 fallback。
3. risk level validation。
4. patch 为空。
5. patch 超长截断 / 限制。
6. prompt 中不包含 token。

缺失则补充。

## 8. 测试数据要求

所有新增集成测试必须：

- 使用唯一名称。
- 不依赖 Demo 数据。
- 不依赖测试顺序。
- 不依赖真实外部服务。
- 不污染真实生产数据。

建议：

```java
String suffix = String.valueOf(System.currentTimeMillis());
String projectName = "IT Project " + suffix;
```

如果使用共享 dev/test database，必须：

- 避免固定唯一键冲突。
- 不清空整库。
- 不删除非测试数据。

## 9. Test Profile 要求

检查：

```text
backend/src/test/resources/application-test.yml
```

必须确保：

- Provider 使用 MOCK。
- GitHub OAuth 可未配置。
- Prompt safety 开启。
- Flyway / schema 初始化策略明确。
- 不需要真实 Redis / RabbitMQ。
- 不读取 `.env.production`。

如果需要新增 test fixture，必须说明。

## 10. Quality Gate 要求

后端发布前必须通过：

```bash
cd backend
mvn test
```

建议 `scripts/run-backend-checks.sh` 包含：

```bash
mvn clean compile
mvn test
mvn package -DskipTests
```

`scripts/release-checklist.sh` 应检查：

- Backend tests pass。
- Frontend typecheck / build pass。
- E2E pass。
- Bundle check no FAIL。
- Secret scan pass。

## 11. 验证要求

完成后必须执行：

```bash
cd backend
mvn test
```

如果修改了 Maven / test config：

```bash
mvn clean test
```

如果改了脚本：

```bash
bash -n scripts/run-backend-checks.sh
bash -n scripts/release-checklist.sh
```

必须执行文档检查：

```bash
test -f docs/backend-test-matrix.md
test -f docs/backend-testing-guide.md
test -f docs/backend-coverage-report-template.md
test -f docs/milestone-29-validation-report-template.md
```

如果只新增后端测试和文档，不需要运行前端 E2E，但必须说明原因。

## 12. 完成后输出格式

完成后必须按以下格式输出：

```text
Milestone 29 完成报告

1. 新增/修改文件清单
2. Backend Test Matrix 说明
3. 新增测试覆盖说明
4. Security / JWT 测试说明
5. Model Gateway 测试说明
6. RAG / Chat / Task 测试说明
7. Quality Gate 更新说明
8. 自动化验证结果
9. 已知限制
10. 是否可以进入 Milestone 30
```

## 13. 不做事项

本阶段明确不做：

- 不追求 100% coverage。
- 不引入 Jacoco 强制阈值，除非已有配置。
- 不接真实外部 API。
- 不做性能压测。
- 不做数据库大规模压测。
- 不重构业务代码。
- 不修复所有历史技术债。
- 不为了测试方便暴露生产接口。

## 14. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 29。

文档路径：
docs/milestone-29-backend-test-coverage-quality-gates.md

执行要求：
1. 先完整阅读该文档，再检查 backend 现有测试、test profile、support helpers、关键 service 和 release scripts。
2. 本阶段目标是后端测试覆盖与质量门增强，不是增加业务功能。
3. 不要改后端核心业务逻辑，除非测试暴露真实 bug，且修复前要说明原因。
4. 不要接真实 OpenAI/Claude/DeepSeek/GitHub API，不需要真实 token。
5. 不要删除已有测试断言，不要把关键失败测试 disabled。
6. 新增测试必须使用 MOCK provider、唯一数据名、可重复运行，不依赖 Demo 数据或外部网络。
7. 优先覆盖 Security/JWT、Model Gateway、Task State Machine、RAG chunk/search、Chat 状态、GitHub PR Review parsing。
8. 如果只改后端测试和文档，不需要跑前端 E2E，但要说明原因。

需要实现：
1. 新增 docs/backend-test-matrix.md。
2. 新增 docs/backend-testing-guide.md。
3. 新增 docs/backend-coverage-report-template.md。
4. 新增 docs/milestone-29-validation-report-template.md。
5. 检查并增强 Security/JWT 测试：access/refresh token type、refresh token 不能访问 protected API、access token 不能 refresh。
6. 检查并增强 Task 状态机测试：合法/非法流转、retry 边界。
7. 检查并增强 Model Gateway 测试：prompt safety、secret masking、pricing、fallback、error code。
8. 检查并增强 RAG 测试：chunk split、overlap、hash、empty/search limit。
9. 检查并增强 Chat 测试：sendMessage 状态、references、权限边界。
10. 检查并增强 GitHub PR Review 测试：JSON parse、bad output fallback、prompt 不含 token。
11. 如有必要，新增 TestDataFactory / TestAuthHelper 等轻量 helper，避免重复。
12. 更新 docs/testing-strategy.md 和 README.md 的后端测试说明。
13. 如有必要，更新 scripts/run-backend-checks.sh 和 scripts/release-checklist.sh，确保 mvn test 是后端发布质量门。

完成后必须执行：
1. cd backend && mvn test
2. 如果改了 Maven/test config：cd backend && mvn clean test
3. test -f docs/backend-test-matrix.md
4. test -f docs/backend-testing-guide.md
5. test -f docs/backend-coverage-report-template.md
6. test -f docs/milestone-29-validation-report-template.md
7. 如果改了脚本：bash -n scripts/run-backend-checks.sh && bash -n scripts/release-checklist.sh

完成后按以下格式输出：
1. 新增/修改文件清单
2. Backend Test Matrix 说明
3. 新增测试覆盖说明
4. Security / JWT 测试说明
5. Model Gateway 测试说明
6. RAG / Chat / Task 测试说明
7. Quality Gate 更新说明
8. 自动化验证结果
9. 已知限制
10. 是否可以进入 Milestone 30

现在开始实现，不要只给计划。
```
