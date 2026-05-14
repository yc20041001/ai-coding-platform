# AI Coding Platform 后端统一回归测试计划

## 1. 测试目标

对当前已完成的 Milestone 0-10 后端能力进行统一回归验证，确认主链路可用、关键数据落库、权限与异常处理正确、Mock/真实模型网关降级机制不破坏业务。

覆盖范围：

- Foundation 基础设施
- Auth 登录认证
- Project + Member
- Agent + Task
- Chat + SSE
- Repository 只读基础接口
- RAG Knowledge Base
- RAG 接入 Chat
- RAG 接入 Agent Orchestrator
- Model Gateway Provider + Mock fallback
- Chat SSE 模型网关流式输出

## 2. 测试边界

### 2.1 本次必须测试

- `mvn clean compile`
- `mvn test`
- Spring Boot health
- admin 登录
- 当前用户查询
- 项目创建与列表
- Agent 列表
- 知识库创建
- 文档上传与切片
- RAG 搜索
- Chat session 创建
- Chat sendMessage + RAG references
- Chat SSE token/done 流程
- Task 创建
- Agent Orchestrator executeTask + RAG context
- Task logs / artifacts / executions / model logs
- 无 token 返回 UNAUTHORIZED
- 已完成任务重复 execute 返回 CONFLICT

### 2.2 本次可选测试

- Repository bind / clone / pull / diff
- 真实 OpenAI / DeepSeek / Qwen 调用
- OpenAI-compatible provider 真实 token usage

可选原因：

- Git clone 依赖外网和目标仓库状态
- 真实模型依赖 API Key、账户余额、供应商可用性

### 2.3 本次不测试

- 真实代码生成落盘
- 真实 Git commit / push / PR
- 真实工具调用沙箱
- 真实向量数据库
- PDF / Word 深度解析

## 3. 环境要求

| 项目 | 要求 |
|---|---|
| JDK | Java 17 |
| Maven | 3.9+ |
| MySQL | 8.x |
| Backend | Spring Boot 3.x |
| 服务端口 | 8080 |

## 4. 环境变量

示例：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3307/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
export DB_USERNAME=root
export DB_PASSWORD=platform123
export JWT_SECRET="verification-test-secret-min-32bytes"
export MODEL_GATEWAY_PROVIDER=MOCK
```

如果使用默认 MySQL 3306：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
```

## 5. 编译与启动验证

### 5.1 编译

```bash
cd backend
mvn clean compile
```

期望：

```text
BUILD SUCCESS
```

### 5.2 测试

```bash
cd backend
mvn test
```

期望：

```text
BUILD SUCCESS
```

### 5.3 启动

```bash
cd backend
mvn spring-boot:run
```

期望：

- 应用启动成功
- Flyway 迁移全部完成
- Actuator health 为 UP

验证：

```bash
curl http://localhost:8080/actuator/health
```

期望：

```json
{"status":"UP"}
```

## 6. 主链路统一 Smoke 测试

可直接运行：

```bash
scripts/backend-unified-smoke-test.sh
```

或者指定服务地址：

```bash
BASE_URL=http://localhost:8080 scripts/backend-unified-smoke-test.sh
```

脚本会自动执行：

1. Health check
2. Login admin
3. `/api/auth/me`
4. Create project
5. List projects
6. List agents
7. Create knowledge base
8. Upload markdown document
9. RAG search
10. Create chat session
11. Chat sendMessage with RAG
12. Chat SSE stream
13. Get chat messages
14. Create task
15. Execute task with RAG
16. Get task detail
17. Get task logs
18. Get task artifacts
19. Get executions
20. Get model logs
21. No token negative test
22. Repeat execute negative test

## 7. 手动测试用例

## TC-001 Health Check

请求：

```bash
curl http://localhost:8080/actuator/health
```

期望：

- `status = UP`

## TC-002 Login

请求：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

期望：

- `code = OK`
- 返回 `accessToken`
- 用户 roles 包含 `ADMIN`

## TC-003 Current User

请求：

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <token>"
```

期望：

- 返回 admin 用户
- permissions 非空

## TC-004 Project

验证：

- 创建项目成功
- 项目列表 total 正确
- 项目详情 currentUserRole 正确

## TC-005 Agent

验证：

- `/api/agents` 返回内置 Agent
- 至少包含 Backend Agent

## TC-006 RAG Knowledge Base

验证：

- 创建知识库成功
- 上传 Markdown 文档成功
- document.status = COMPLETED
- chunkCount > 0
- RAG search 返回 references

## TC-007 Chat + RAG + SSE

验证：

- 创建 Chat Session 成功
- sendMessage 成功
- response.ragUsed = true
- response.references 非空
- SSE token event 正常
- SSE done event 正常
- done event 携带 references
- getMessages 返回 assistant references

## TC-008 Task + Agent Orchestrator + RAG

验证：

- 创建 PENDING task 成功
- executeTask 成功
- execution.status = COMPLETED
- task.status = COMPLETED
- task logs 包含 RAG_SEARCH / ORCHESTRATOR_START / MODEL_GATEWAY_REQUEST / ORCHESTRATOR_DONE
- artifacts 非空
- model logs 非空

## TC-009 Model Gateway

默认 Mock 模式：

- provider = MOCK
- outputContent 非空
- tokenUsage > 0

OPENAI enabled 但无 key：

- 不阻塞系统启动
- fallback 到 MOCK

Prompt Safety：

- 高危 prompt 不应泄露密钥
- Chat SSE 应返回 error 或降级策略符合实现

## TC-010 Negative Tests

| 场景 | 期望 |
|---|---|
| 无 token 访问 `/api/projects` | UNAUTHORIZED |
| invalid token | UNAUTHORIZED |
| COMPLETED task 重复 execute | CONFLICT |
| 不支持文档类型 PDF | BAD_REQUEST |
| RAG 无结果 | results = [] |

## 8. 数据库核验

可选执行：

```sql
SELECT COUNT(*) FROM project;
SELECT COUNT(*) FROM ai_task;
SELECT COUNT(*) FROM chat_message;
SELECT COUNT(*) FROM chat_message_reference;
SELECT COUNT(*) FROM knowledge_base;
SELECT COUNT(*) FROM knowledge_document;
SELECT COUNT(*) FROM document_chunk;
SELECT COUNT(*) FROM agent_execution;
SELECT COUNT(*) FROM model_request_log;
```

期望：

- 主链路执行后以上表均有相关数据

## 9. 通过标准

必须满足：

- `mvn clean compile` PASS
- `mvn test` PASS
- smoke 脚本 PASS
- 核心接口返回 `code=OK`
- Negative tests 返回预期错误码
- Chat SSE 有 done event
- Task execute 后状态为 COMPLETED
- RAG references 在 Chat 和 Agent 链路可见
- model_request_log 有记录

## 10. 测试报告模板

```markdown
# 统一回归测试报告

## 1. 测试环境

- JDK:
- Maven:
- MySQL:
- Backend:
- MODEL_GATEWAY_PROVIDER:

## 2. 编译与测试

| 检查项 | 结果 | 说明 |
|---|---|---|
| mvn clean compile | PASS/FAIL | |
| mvn test | PASS/FAIL | |
| Spring Boot 启动 | PASS/FAIL | |
| /actuator/health | PASS/FAIL | |

## 3. Smoke 测试结果

| 链路 | 结果 | 说明 |
|---|---|---|
| Auth | PASS/FAIL | |
| Project | PASS/FAIL | |
| Agent | PASS/FAIL | |
| RAG | PASS/FAIL | |
| Chat + SSE | PASS/FAIL | |
| Task + Orchestrator | PASS/FAIL | |
| Model Gateway | PASS/FAIL | |
| Negative Tests | PASS/FAIL | |

## 4. 发现的问题

...

## 5. 修复建议

...

## 6. 是否具备进入下一阶段条件

可以 / 不可以
```

