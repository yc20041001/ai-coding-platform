# Milestone 36A: Safe Tool Execution Sandbox

## 1. 背景

Milestone 35A-35F 已经完成多智能体编排能力：

- 35A: Multi-Agent Mock Orchestration
- 35B: Message Passing
- 35C: Phase / Lane Parallel Mock Execution
- 35D: Workflow Strategy Template
- 35E: Human Approval Gate
- 35F: Persisted Workflow Template Management

当前系统已经可以：

```text
Task -> Multi-Agent Run -> Phase / Lane -> Step -> Message -> Approval Gate -> Final Summary
```

但 Agent 仍停留在 Mock 文本输出阶段，没有统一的工具调用记录、执行边界、审计数据和后续扩展接口。

Milestone 36A 的目标是新增 **Safe Tool Execution Sandbox** 基础模块：

```text
Agent Step -> Tool Sandbox Plan -> Dry Run / Mock Execute -> Tool Log -> Artifact / Response
```

本阶段不接真实 shell，不做 Git 写操作，不改真实代码文件，只建立安全工具执行的最小闭环。

## 2. 总目标

实现一个可审计、默认安全、仅 Mock / Dry-run 的工具执行沙箱：

1. 定义 Tool Sandbox Execution 数据模型。
2. 支持多智能体 Step 关联工具执行记录。
3. 提供后端 API 查询工具执行记录。
4. 提供 Mock Tool Executor，模拟只读工具结果。
5. 严格禁止真实 shell / Git 写操作。
6. 将工具执行结果写入任务日志和响应。
7. 前端 Multi-Agent Run Panel 展示 Tool Executions。
8. 补齐后端集成测试和前端 E2E。

完成后，系统从：

```text
Agent 只输出 Markdown
```

升级为：

```text
Agent 输出 Markdown + 安全工具执行记录 + 可审计工具结果
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令。
2. 不执行真实 Git 写操作。
3. 不执行 `git add` / `git commit` / `git push` / `git reset` / `git checkout`。
4. 不修改真实业务代码文件。
5. 不生成真实代码补丁。
6. 不接入真实远程执行器。
7. 不引入 Docker sandbox / Firecracker / Kubernetes Job。
8. 不做异步 Worker / 队列。
9. 不做文件系统写入工具。
10. 不绕过 Human Approval Gate。
11. 不破坏 35A-35F 现有 API。
12. 不破坏 Task 状态机。
13. 不破坏 Chat / RAG / Model Gateway 现有能力。

允许做：

- 新增工具执行记录表。
- 新增 tool execution entity / mapper / DTO / service / controller。
- 新增 Mock Tool Executor。
- 在 Multi-Agent Step 完成时创建模拟工具执行记录。
- 将工具结果作为 Step 附属数据返回。
- 前端展示工具执行记录。
- 后端和前端测试覆盖。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V20__init_tool_sandbox_tables.sql
```

如果 V20 已存在，请顺延到下一个版本号。

### 4.1 tool_sandbox_execution

```sql
CREATE TABLE IF NOT EXISTS tool_sandbox_execution (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    phase_id BIGINT NULL,
    step_id BIGINT NULL,
    agent_id BIGINT NULL,
    tool_name VARCHAR(64) NOT NULL,
    tool_type VARCHAR(32) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_payload JSON NULL,
    output_payload JSON NULL,
    summary TEXT NULL,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_sandbox_project_time(project_id, create_time),
    INDEX idx_tool_sandbox_task(task_id),
    INDEX idx_tool_sandbox_run(run_id),
    INDEX idx_tool_sandbox_step(step_id),
    INDEX idx_tool_sandbox_agent(agent_id),
    INDEX idx_tool_sandbox_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全工具沙箱执行记录';
```

字段说明：

| 字段 | 说明 |
|---|---|
| project_id | 项目 ID |
| task_id | 关联任务 ID，可为空 |
| run_id | 关联 Multi-Agent Run ID，可为空 |
| phase_id | 关联 Phase ID，可为空 |
| step_id | 关联 Step ID，可为空 |
| agent_id | 关联 Agent ID，可为空 |
| tool_name | 工具名称，例如 PROJECT_CONTEXT_SCAN |
| tool_type | 工具类型，例如 READ_ONLY / MOCK / ANALYSIS |
| execution_mode | DRY_RUN / MOCK_EXECUTE |
| status | PENDING / RUNNING / COMPLETED / FAILED / BLOCKED |
| input_payload | 工具输入 JSON |
| output_payload | 工具输出 JSON |
| summary | 简要结果 |
| error_message | 错误信息 |
| duration_ms | 模拟执行耗时 |

无物理外键，保持项目现有数据库风格。

## 5. 枚举设计

新增目录：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/
```

新增枚举：

### 5.1 ToolExecutionStatus.java

```java
public enum ToolExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    BLOCKED
}
```

### 5.2 ToolExecutionMode.java

```java
public enum ToolExecutionMode {
    DRY_RUN,
    MOCK_EXECUTE
}
```

### 5.3 ToolType.java

```java
public enum ToolType {
    READ_ONLY,
    MOCK,
    ANALYSIS
}
```

### 5.4 ToolName.java

```java
public enum ToolName {
    PROJECT_CONTEXT_SCAN,
    TASK_REQUIREMENT_ANALYSIS,
    MOCK_FILE_INSPECTION,
    MOCK_TEST_PLAN_SCAN,
    MOCK_SECURITY_REVIEW
}
```

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolSandboxExecutionEntity.java
```

要求：

- `@TableName("tool_sandbox_execution")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` createTime
- `@TableField(fill = FieldFill.INSERT_UPDATE)` updateTime
- 不继承 BaseEntity
- 手写 getter/setter

### 6.2 Mapper

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolSandboxExecutionMapper.java
```

```java
public interface ToolSandboxExecutionMapper extends BaseMapper<ToolSandboxExecutionEntity> {
}
```

### 6.3 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolSandboxExecutionResponse.java
```

字段：

- id
- projectId
- taskId
- runId
- phaseId
- stepId
- agentId
- toolName
- toolType
- executionMode
- status
- inputPayload
- outputPayload
- summary
- errorMessage
- startedAt
- finishedAt
- durationMs
- createTime

所有 ID 对外保持 String。

## 7. 后端服务设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java
```

### 7.1 核心方法

```java
public ToolSandboxExecutionEntity mockExecuteForStep(
    Long projectId,
    Long taskId,
    Long runId,
    Long phaseId,
    Long stepId,
    Long agentId,
    String stepType,
    String inputContext
)
```

行为：

1. 根据 stepType 选择 ToolName。
2. 创建 execution，状态 RUNNING。
3. 不调用真实外部命令。
4. 生成 deterministic mock output。
5. 更新状态 COMPLETED。
6. 返回 entity。

### 7.2 stepType 到 toolName 映射

| stepType | toolName |
|---|---|
| ARCHITECTURE_ANALYSIS | PROJECT_CONTEXT_SCAN |
| BACKEND_IMPLEMENTATION_PLAN | TASK_REQUIREMENT_ANALYSIS |
| FRONTEND_IMPLEMENTATION_PLAN | MOCK_FILE_INSPECTION |
| TEST_PLAN | MOCK_TEST_PLAN_SCAN |
| CODE_REVIEW | MOCK_SECURITY_REVIEW |
| FINAL_SUMMARY | PROJECT_CONTEXT_SCAN |

### 7.3 Mock 输出规则

`output_payload` 示例：

```json
{
  "mock": true,
  "readOnly": true,
  "filesTouched": [],
  "gitOperations": [],
  "findings": [
    "Mock sandbox scanned task context.",
    "No real filesystem or git operation was executed."
  ]
}
```

`summary` 示例：

```text
Mock 工具执行完成：PROJECT_CONTEXT_SCAN，只读模拟，无文件写入，无 Git 操作。
```

### 7.4 查询方法

```java
public List<ToolSandboxExecutionResponse> listByRun(Long runId)
public List<ToolSandboxExecutionResponse> listByStep(Long stepId)
public ToolSandboxExecutionResponse getExecution(Long executionId)
```

权限：

- 先通过 run / step / execution 获取 projectId。
- 再调用 `ProjectPermissionService.checkProjectRole(projectId, VIEWER+)`。

## 8. Multi-Agent 集成点

修改：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/MultiAgentOrchestrationService.java
```

在 Step 成功生成 Mock 输出后，创建工具沙箱记录：

```text
Step output generated
  -> ToolSandboxExecutionService.mockExecuteForStep(...)
  -> write task log TOOL_SANDBOX_EXECUTED
  -> response includes toolExecutions
```

要求：

1. 只对 COMPLETED step 创建 tool execution。
2. SKIPPED step 不创建 tool execution。
3. WAITING_APPROVAL 前已完成的 step 也要有 tool execution。
4. approval 继续执行后，后续 step 也要有 tool execution。
5. tool execution 失败不应导致真实 shell / Git fallback。

## 9. Response 增强

修改：

```text
MultiAgentStepResponse.java
```

新增：

```java
private List<ToolSandboxExecutionResponse> toolExecutions;
```

修改：

```text
MultiAgentRunResponse.java
```

新增：

```java
private List<ToolSandboxExecutionResponse> toolExecutions;
```

映射规则：

- Run response 中包含 run 下所有 tool executions。
- Step response 中包含该 step 下的 tool executions。
- Phase response 中的 steps 也要带 tool executions。

## 10. 后端 API

修改：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/MultiAgentOrchestrationController.java
```

新增接口：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/multi-agent-runs/{runId}/tool-executions` | VIEWER+ | 查询 Run 下所有工具执行 |
| GET | `/api/multi-agent-steps/{stepId}/tool-executions` | VIEWER+ | 查询 Step 下所有工具执行 |
| GET | `/api/tool-sandbox-executions/{executionId}` | VIEWER+ | 查询单条工具执行详情 |

返回：

```json
{
  "code": "OK",
  "data": [
    {
      "id": "123",
      "toolName": "PROJECT_CONTEXT_SCAN",
      "toolType": "READ_ONLY",
      "executionMode": "MOCK_EXECUTE",
      "status": "COMPLETED",
      "summary": "Mock 工具执行完成...",
      "durationMs": 12
    }
  ]
}
```

## 11. 任务日志

每次工具模拟执行完成后写入 task log：

```text
level: INFO
stage: TOOL_SANDBOX_EXECUTED
message: 工具 PROJECT_CONTEXT_SCAN 已在 MOCK_EXECUTE 模式完成，只读模拟，无文件写入。
```

禁止写入 API Key / Token / 本地绝对路径 / 密码。

## 12. 前端 API

修改：

```text
frontend/src/modules/task/api.ts
```

新增类型：

```ts
export interface ToolSandboxExecutionResponse {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  phaseId: string | null
  stepId: string | null
  agentId: string | null
  toolName: string
  toolType: string
  executionMode: string
  status: string
  inputPayload: string | null
  outputPayload: string | null
  summary: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number
  createTime: string
}
```

增强：

- `MultiAgentStepResponse.toolExecutions?: ToolSandboxExecutionResponse[]`
- `MultiAgentRunResponse.toolExecutions?: ToolSandboxExecutionResponse[]`

新增 API：

```ts
export function getMultiAgentRunToolExecutions(runId: string)
export function getMultiAgentStepToolExecutions(stepId: string)
export function getToolSandboxExecution(executionId: string)
```

## 13. 前端 UI

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

新增展示：

1. Run 顶部统计增加：
   - 工具执行 N
   - Mock 执行 N
2. 每个 Step 展开后显示「工具沙箱」区域。
3. 工具卡片展示：
   - toolName
   - executionMode
   - status
   - durationMs
   - summary
4. outputPayload 可折叠查看。
5. 明确显示安全提示：

```text
当前为 Mock 沙箱执行：未执行真实 Shell，未执行 Git 写操作，未写入文件。
```

建议 data-testid：

- `multi-agent-tool-summary`
- `multi-agent-tool-section`
- `multi-agent-tool-card`
- `multi-agent-tool-output`

## 14. 后端测试

修改：

```text
backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java
```

新增测试不少于 10 个：

1. start run 后 completed step 生成 tool execution。
2. waiting approval 前已有 steps 生成 tool execution。
3. skipped step 不生成 tool execution。
4. run detail 返回 toolExecutions。
5. step response 返回 toolExecutions。
6. GET run tool executions 成功。
7. GET step tool executions 成功。
8. GET single tool execution 成功。
9. 未登录访问 tool execution API 返回 UNAUTHORIZED。
10. 无效 executionId 返回 NOT_FOUND。
11. tool execution output 声明 mock/readOnly/filesTouched=[]/gitOperations=[]。
12. task logs 包含 TOOL_SANDBOX_EXECUTED。

全量后端质量门：

```bash
cd backend
mvn test
```

## 15. 前端 E2E

修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

新增测试：

1. 启动 Multi-Agent Run 后显示工具执行统计。
2. 展开 Step 后显示工具沙箱区域。
3. 工具卡片显示 MOCK_EXECUTE / COMPLETED。
4. 工具输出包含 no real shell / no git write 语义。
5. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 16. 文档与报告

完成后新增：

```text
docs/milestone-36a-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Sandbox Execution 设计说明
4. Mock Tool Executor 行为说明
5. Multi-Agent 集成说明
6. 后端 API 清单
7. 前端展示说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36B

## 17. 验收标准

必须满足：

- `tool_sandbox_execution` 表存在。
- Multi-Agent completed step 自动生成 tool execution。
- Run detail 可看到 toolExecutions。
- Step detail 可看到 toolExecutions。
- 前端可展示工具沙箱执行记录。
- Mock output 明确包含：
  - `mock=true`
  - `readOnly=true`
  - `filesTouched=[]`
  - `gitOperations=[]`
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。
- 不出现真实 shell / Git 写操作。

## 18. 已知非目标

本阶段不做：

- 真实 shell executor
- 真实 Git executor
- 文件写入工具
- patch apply
- Docker isolated sandbox
- Kubernetes Job runner
- 异步队列
- 工具 marketplace
- 工作流节点级工具配置编辑器
- 真实代码修改

这些可以放到后续：

- 36B: Read-only Tool Catalog
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Sandbox Worker / Queue

## 19. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36A。

文档路径：
docs/milestone-36a-safe-tool-execution-sandbox.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 35A-35F 的多智能体编排基础上，新增 Safe Tool Execution Sandbox。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要修改真实业务代码文件作为 Agent 输出。
6. 所有工具执行必须是 Mock / Dry-run / Read-only 记录。
7. 不要破坏已有 Multi-Agent Run / Phase / Step / Message / Approval Gate / Workflow Template API。
8. 不要绕过 Task 状态机。
9. 不要改 Auth、Project、Member、Repository、Chat、RAG、Model Gateway 已验证逻辑，除非本模块必须依赖。
10. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
11. IDs 对外保持 String。
12. 权限校验复用 ProjectPermissionService。
13. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V20 tool_sandbox_execution migration。
2. 新增 ToolExecutionStatus / ToolExecutionMode / ToolType / ToolName 枚举。
3. 新增 ToolSandboxExecutionEntity / Mapper / DTO。
4. 新增 ToolSandboxExecutionService，提供 Mock / Dry-run 工具执行。
5. MultiAgentOrchestrationService 在 completed step 后创建 tool execution。
6. Run / Step response 增加 toolExecutions。
7. 新增查询 API：
   - GET /api/multi-agent-runs/{runId}/tool-executions
   - GET /api/multi-agent-steps/{stepId}/tool-executions
   - GET /api/tool-sandbox-executions/{executionId}
8. 每次工具执行写入 Task Log：TOOL_SANDBOX_EXECUTED。
9. 前端 task/api.ts 新增类型和 API。
10. MultiAgentRunPanel 增加工具沙箱统计和 step 内工具卡片。
11. 后端集成测试不少于 10 个。
12. 前端 E2E 覆盖工具沙箱显示。
13. 新增 docs/milestone-36a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Sandbox Execution 设计说明
4. Mock Tool Executor 行为说明
5. Multi-Agent 集成说明
6. 后端 API 清单
7. 前端展示说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36B

现在开始实现，不要只给计划。
```
