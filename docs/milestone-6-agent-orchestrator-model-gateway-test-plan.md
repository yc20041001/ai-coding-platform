# Milestone 6: Agent Orchestrator + Model Gateway 测试计划

## 1. 测试目标

对 Milestone 6 的 Agent Orchestrator + Model Gateway 基础模块进行完整验证。

本测试计划覆盖：

- 数据库迁移是否成功
- Spring Boot 应用是否正常启动
- Auth 登录链路是否可用
- Project / Task 前置数据是否可创建
- Agent Orchestrator 是否能执行 PENDING 任务
- Mock Model Gateway 是否返回标准响应
- Agent Execution 是否落库
- Model Request Log 是否落库
- Task 状态是否正确流转
- Task 日志、事件、产物是否正确生成
- 权限与异常场景是否按预期返回

本阶段不验证：

- 真实大模型调用
- 真实代码生成
- 真实 Shell 执行
- 真实 Git 写操作
- Agent 多轮自动协作
- RAG 检索增强

## 2. 测试范围

### 2.1 被测模块

```text
orchestrator/
  controller/AgentOrchestratorController.java
  application/AgentOrchestratorService.java
  domain/*
  dto/*
  infrastructure/*

modelgateway/
  application/ModelGateway.java
  application/MockModelGateway.java
  application/ModelRequestLogService.java
  dto/*

db/migration/V7__init_orchestrator_and_model_gateway_tables.sql
```

### 2.2 关联模块

```text
auth/
project/
member/
agent/
task/
chat/
common/
security/
```

关联模块只做回归验证，不主动改动核心逻辑。

## 3. 测试环境

### 3.1 基础环境

| 项目 | 要求 |
|---|---|
| JDK | Java 17 |
| Maven | 3.9+ |
| MySQL | 8.x |
| Backend | Spring Boot 3.x |
| Database | ai_coding_platform |

### 3.2 环境变量

按本地 MySQL 配置调整：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3307/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
export DB_USERNAME=root
export DB_PASSWORD=platform123
export JWT_SECRET="verification-test-secret-min-32bytes"
```

如果你的 MySQL 是默认 3306：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
```

### 3.3 创建数据库

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p -e \
"CREATE DATABASE IF NOT EXISTS ai_coding_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

## 4. 编译与基础验证

### 4.1 编译

```bash
cd backend
mvn compile
```

期望：

```text
BUILD SUCCESS
```

### 4.2 单元测试

```bash
cd backend
mvn test
```

期望：

```text
BUILD SUCCESS
```

### 4.3 启动应用

```bash
cd backend
mvn spring-boot:run
```

期望：

- 应用启动成功
- Tomcat 监听 `8080`
- Flyway 执行到 `V7`
- `/actuator/health` 返回 `UP`

验证：

```bash
curl http://localhost:8080/actuator/health
```

期望：

```json
{"status":"UP"}
```

## 5. 数据库迁移验证

### 5.1 Flyway 版本验证

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

期望：

- `V7__init_orchestrator_and_model_gateway_tables.sql` 已执行
- `success = 1`

### 5.2 表存在验证

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SHOW TABLES LIKE 'agent_execution'; SHOW TABLES LIKE 'model_request_log';"
```

期望：

```text
agent_execution
model_request_log
```

### 5.3 索引验证

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SHOW INDEX FROM agent_execution; SHOW INDEX FROM model_request_log;"
```

期望包含：

```text
idx_agent_execution_project_time
idx_agent_execution_task
idx_agent_execution_chat
idx_agent_execution_agent
idx_agent_execution_status
idx_model_request_project_time
idx_model_request_execution
idx_model_request_provider
idx_model_request_success
```

## 6. 测试数据准备

### 6.1 登录 admin

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo $TOKEN
```

期望：

- 输出非空 JWT
- admin 用户具备 `ADMIN` 角色

### 6.2 创建项目

```bash
PROJECT_ID=$(curl -s -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Milestone 6 Test Project",
    "description":"Project for Agent Orchestrator test",
    "techStack":["Java","Spring Boot"]
  }' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo $PROJECT_ID
```

期望：

- 输出非空 projectId
- 当前用户自动成为 OWNER

### 6.3 创建任务

```bash
TASK_ID=$(curl -s -X POST "http://localhost:8080/api/projects/$PROJECT_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Mock Agent 执行任务",
    "description":"验证 Agent Orchestrator 调用 Mock Model Gateway 并生成任务产物",
    "taskType":"FEATURE",
    "priority":"MEDIUM",
    "agentId":"300002"
  }' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo $TASK_ID
```

期望：

- 输出非空 taskId
- task.status = `PENDING`
- agentId = `300002`

## 7. API 测试用例

## TC-M6-001 执行 PENDING 任务

### 优先级

P0

### 前置条件

- 已登录 admin
- 已创建 project
- 已创建 PENDING task

### 请求

```bash
EXECUTION_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/tasks/$TASK_ID/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction":"请用 Mock Agent 执行这个任务"
  }')

echo $EXECUTION_RESPONSE | python3 -m json.tool
```

### 期望结果

- HTTP 200
- `code = OK`
- `data.status = COMPLETED`
- `data.outputContent` 非空
- `data.tokenUsage > 0`
- `data.agentId = 300002`
- `data.executionType = TASK`
- `data.startedAt` 非空
- `data.finishedAt` 非空

### 提取 executionId

```bash
EXECUTION_ID=$(echo $EXECUTION_RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo $EXECUTION_ID
```

## TC-M6-002 查询任务详情，验证状态完成

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

- `data.status = COMPLETED`
- `data.startTime` 非空
- `data.endTime` 非空
- `data.errorMessage` 为空或 null

## TC-M6-003 查询任务日志

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/tasks/$TASK_ID/logs" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

至少包含以下日志：

- `ORCHESTRATOR_START`
- `MODEL_GATEWAY_REQUEST`
- `ORCHESTRATOR_DONE`

日志级别期望：

- `ORCHESTRATOR_START`: `INFO`
- `MODEL_GATEWAY_REQUEST`: `INFO`
- `ORCHESTRATOR_DONE`: `INFO`

## TC-M6-004 查询任务产物

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/tasks/$TASK_ID/artifacts" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

- 至少 1 条产物
- `title = Mock Agent Execution Result`
- `artifactType = MARKDOWN` 或 `DOCUMENT`
- `content` 非空

## TC-M6-005 查询任务执行记录列表

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/tasks/$TASK_ID/executions" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

- 至少 1 条 execution
- `status = COMPLETED`
- `outputContent` 非空
- `agentName` 非空

## TC-M6-006 查询执行详情

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/agent-executions/$EXECUTION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

- `data.id = $EXECUTION_ID`
- `data.taskId = $TASK_ID`
- `data.projectId = $PROJECT_ID`
- `data.status = COMPLETED`
- `data.outputContent` 非空
- `data.tokenUsage > 0`

## TC-M6-007 查询模型调用日志

### 优先级

P0

### 请求

```bash
curl -s "http://localhost:8080/api/agent-executions/$EXECUTION_ID/model-logs" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

### 期望结果

- 至少 1 条 model log
- `provider = MOCK`
- `modelName = mock-agent-model`
- `requestType = TASK_EXECUTION`
- `success = true`
- `totalTokens > 0`
- `latencyMs >= 0`

## TC-M6-008 重复执行 COMPLETED 任务

### 优先级

P0

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/$TASK_ID/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' \
  | python3 -m json.tool
```

### 期望结果

- 返回 `CONFLICT`
- message 包含“不允许”或“状态”
- 不创建新的成功 execution

### 数据库辅助验证

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SELECT COUNT(*) FROM agent_execution WHERE task_id = $TASK_ID AND status = 'COMPLETED';"
```

期望：

- 数量仍为 1

## TC-M6-009 无 token 执行任务

### 优先级

P0

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/$TASK_ID/execute" \
  -H "Content-Type: application/json" \
  -d '{}' \
  | python3 -m json.tool
```

### 期望结果

- `code = UNAUTHORIZED`
- message = `未登录或 Token 无效`

## TC-M6-010 非法 token 执行任务

### 优先级

P1

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/$TASK_ID/execute" \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -d '{}' \
  | python3 -m json.tool
```

### 期望结果

- `code = UNAUTHORIZED`

## TC-M6-011 task 不存在

### 优先级

P1

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/999999999999999999/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' \
  | python3 -m json.tool
```

### 期望结果

- `code = NOT_FOUND`

## TC-M6-012 agent 不存在

### 优先级

P1

### 前置条件

创建一个新的 PENDING 任务。

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/<newTaskId>/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId":"999999999999999999",
    "instruction":"使用不存在的 Agent 执行"
  }' \
  | python3 -m json.tool
```

### 期望结果

- `code = NOT_FOUND`
- 不推进任务状态
- 不生成成功产物

## TC-M6-013 task 无 agentId 且请求未传 agentId

### 优先级

P1

### 前置条件

创建一个没有绑定 agentId 的 PENDING 任务。如果当前接口强制需要 agentId，可跳过该用例并记录为“不适用”。

### 请求

```bash
curl -s -X POST "http://localhost:8080/api/tasks/<taskWithoutAgentId>/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' \
  | python3 -m json.tool
```

### 期望结果

- `code = BAD_REQUEST`
- message 说明缺少 agentId

## TC-M6-014 模型日志数据库核验

### 优先级

P1

### SQL

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SELECT provider, model_name, request_type, total_tokens, success FROM model_request_log WHERE execution_id = $EXECUTION_ID;"
```

### 期望结果

- provider = `MOCK`
- model_name = `mock-agent-model`
- request_type = `TASK_EXECUTION`
- total_tokens > 0
- success = 1

## TC-M6-015 Agent Execution 数据库核验

### 优先级

P1

### SQL

```bash
mysql -h 127.0.0.1 -P 3307 -u root -p ai_coding_platform \
  -e "SELECT project_id, task_id, agent_id, execution_type, status, token_usage FROM agent_execution WHERE id = $EXECUTION_ID;"
```

### 期望结果

- project_id = `$PROJECT_ID`
- task_id = `$TASK_ID`
- agent_id = `300002`
- execution_type = `TASK`
- status = `COMPLETED`
- token_usage > 0

## 8. 一键手动验证脚本

以下脚本适合快速验证主链路。

请先确认服务已启动，并根据实际端口、密码调整变量。

```bash
BASE_URL="http://localhost:8080"

TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo "TOKEN OK"

PROJECT_ID=$(curl -s -X POST "$BASE_URL/api/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Milestone 6 Smoke Project",
    "description":"Smoke test project",
    "techStack":["Java","Spring Boot"]
  }' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo "PROJECT_ID=$PROJECT_ID"

TASK_ID=$(curl -s -X POST "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Milestone 6 Smoke Task",
    "description":"Smoke test for Agent Orchestrator",
    "taskType":"FEATURE",
    "priority":"MEDIUM",
    "agentId":"300002"
  }' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo "TASK_ID=$TASK_ID"

EXECUTION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/tasks/$TASK_ID/execute" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"执行 Milestone 6 smoke test"}')

echo "$EXECUTION_RESPONSE" | python3 -m json.tool

EXECUTION_ID=$(echo "$EXECUTION_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo "EXECUTION_ID=$EXECUTION_ID"

echo "=== Task Detail ==="
curl -s "$BASE_URL/api/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool

echo "=== Task Logs ==="
curl -s "$BASE_URL/api/tasks/$TASK_ID/logs" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool

echo "=== Task Artifacts ==="
curl -s "$BASE_URL/api/tasks/$TASK_ID/artifacts" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool

echo "=== Executions ==="
curl -s "$BASE_URL/api/tasks/$TASK_ID/executions" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool

echo "=== Model Logs ==="
curl -s "$BASE_URL/api/agent-executions/$EXECUTION_ID/model-logs" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

## 9. 回归测试清单

Milestone 6 完成后，至少回归以下接口：

| 模块 | 接口 | 期望 |
|---|---|---|
| Auth | POST /api/auth/login | 正常登录 |
| Auth | GET /api/auth/me | 返回当前用户 |
| Project | POST /api/projects | 正常创建项目 |
| Project | GET /api/projects | 分页 total 正确 |
| Agent | GET /api/agents | 返回内置 Agent |
| Task | POST /api/projects/{projectId}/tasks | 正常创建任务 |
| Task | GET /api/tasks/{taskId} | 正常查询任务 |
| Task | GET /api/tasks/{taskId}/logs | 正常查询日志 |
| Task | GET /api/tasks/{taskId}/artifacts | 正常查询产物 |
| Orchestrator | POST /api/tasks/{taskId}/execute | 正常执行 PENDING 任务 |

## 10. 风险点与重点观察

### 10.1 状态流转风险

重点确认：

- 只能执行 `PENDING` 任务
- `COMPLETED` 任务重复执行必须返回 `CONFLICT`
- 失败时任务状态应为 `FAILED`

### 10.2 日志与事件一致性风险

重点确认：

- `ai_task_log` 与 `ai_task_event` 均有记录
- 顺序符合执行流程
- 失败场景不会只更新 task 而不写日志

### 10.3 产物生成风险

重点确认：

- 成功执行后必须生成 artifact
- artifact content 不能空
- artifact taskId/projectId 正确

### 10.4 权限风险

重点确认：

- 无 token 返回 `UNAUTHORIZED`
- 无项目权限返回 `PROJECT_ACCESS_DENIED`
- 查询 execution/model logs 时不能绕过 project 权限

### 10.5 Mock Gateway 边界

重点确认：

- 不调用真实模型
- 不依赖外网
- 不执行 shell
- 不写真实代码文件
- 不做 Git 写操作

## 11. 测试完成报告模板

完成测试后按以下格式输出：

```markdown
# Milestone 6 测试报告

## 1. 测试环境

- JDK:
- Maven:
- MySQL:
- Backend:
- Database:

## 2. 编译与启动

| 检查项 | 结果 | 说明 |
|---|---|---|
| mvn compile | PASS/FAIL | |
| mvn test | PASS/FAIL | |
| Spring Boot 启动 | PASS/FAIL | |
| Flyway V7 | PASS/FAIL | |
| /actuator/health | PASS/FAIL | |

## 3. 数据库验证

| 检查项 | 结果 | 说明 |
|---|---|---|
| agent_execution 表 | PASS/FAIL | |
| model_request_log 表 | PASS/FAIL | |
| 索引 | PASS/FAIL | |

## 4. API 用例结果

| 用例 | 结果 | 说明 |
|---|---|---|
| TC-M6-001 执行 PENDING 任务 | PASS/FAIL | |
| TC-M6-002 查询任务详情 | PASS/FAIL | |
| TC-M6-003 查询任务日志 | PASS/FAIL | |
| TC-M6-004 查询任务产物 | PASS/FAIL | |
| TC-M6-005 查询执行记录列表 | PASS/FAIL | |
| TC-M6-006 查询执行详情 | PASS/FAIL | |
| TC-M6-007 查询模型调用日志 | PASS/FAIL | |
| TC-M6-008 重复执行 COMPLETED 任务 | PASS/FAIL | |
| TC-M6-009 无 token 执行任务 | PASS/FAIL | |
| TC-M6-010 非法 token 执行任务 | PASS/FAIL | |
| TC-M6-011 task 不存在 | PASS/FAIL | |
| TC-M6-012 agent 不存在 | PASS/FAIL | |
| TC-M6-013 task 无 agentId | PASS/FAIL/NA | |
| TC-M6-014 模型日志数据库核验 | PASS/FAIL | |
| TC-M6-015 Agent Execution 数据库核验 | PASS/FAIL | |

## 5. 发现的问题

...

## 6. 修复情况

...

## 7. 残余风险

...

## 8. 是否可以进入 Milestone 7

可以 / 不可以

原因：
...
```

