# Milestone 35A: Multi-Agent Mock Orchestration

## 1. 背景

当前项目已经完成：

- 单 Agent 执行闭环：Task → AgentExecution → ModelGateway → Logs / Events / Artifacts。
- Project Agent 配置闭环：
  - Agent Definition
  - Agent Version
  - Model Config
  - Runtime Config
- Project Agent 页面可启用 / 停用 / 选择版本 / 选择模型配置 / 编辑运行配置。
- Chat、RAG、Task、Model Gateway、Audit、Observability 已具备基础能力。

但当前 Orchestrator 仍以单 Agent 为主：

```text
Task → 1 Agent → 1 Execution → 1 Output
```

Milestone 35A 的目标是进入「多 Agent 协作」的第一步：实现 **Mock Multi-Agent Orchestration**。

本阶段不要求真实多模型推理、不要求真实代码生成、不要求 Git 写操作，只做可观测、可回归、可演示的多 Agent 编排闭环。

## 2. 总目标

实现一个基础的多 Agent Mock 编排能力：

```text
Task
  → Orchestration Plan
  → Architect Agent Step
  → Backend Agent Step
  → Test Agent Step
  → Review Agent Step
  → Final Summary Artifact
```

完成后平台应具备：

1. 用户可以对一个 Task 触发多 Agent 编排。
2. 后端按固定 Mock 流程顺序执行多个 Agent Step。
3. 每个 Agent Step 都有独立执行记录、日志、状态、输出。
4. 总编排有自己的 orchestration run 记录。
5. Task Logs / Artifacts / Executions 能看到多 Agent 协作结果。
6. 前端 Task Detail 能展示 Multi-Agent Timeline。
7. 全流程不执行真实 shell、不写 Git、不生成真实代码文件。

## 3. 严格边界

必须遵守：

1. 不接真实多 Agent 自动推理。
2. 不自动拆真实子任务。
3. 不执行 shell 命令。
4. 不执行 Git 写操作。
5. 不修改仓库文件内容。
6. 不绕过 Task 状态机。
7. 不破坏已有 `POST /api/tasks/{taskId}/execute` 单 Agent 执行。
8. 不破坏已有 Project Agent 配置页面。
9. 不新增复杂工作流编辑器。
10. 不引入消息队列异步调度。
11. 不要求 Redis/RabbitMQ 参与本阶段。
12. 前端保持当前中文科技风 UI。

允许做：

- 新增多 Agent 编排表。
- 新增 orchestration / step DTO、Mapper、Service、Controller。
- 复用现有 `AgentExecutionEntity` 记录每个 Agent Step。
- 复用现有 `AiTaskLogEntity` / `AiTaskArtifactEntity` / `AiTaskEventEntity`。
- 复用现有 `ProjectPermissionService` 权限。
- 复用 Project Agent Config 读取 Agent Version / Model Config / Runtime Config。
- 使用 Mock 输出模拟不同 Agent 的职责。

## 4. 推荐模块命名

后端建议放在：

```text
backend/src/main/java/com/aicoding/platform/orchestration/
```

结构：

```text
orchestration/
  controller/
    MultiAgentOrchestrationController.java
  application/
    MultiAgentOrchestrationService.java
  domain/
    MultiAgentRunEntity.java
    MultiAgentStepEntity.java
    MultiAgentRunStatus.java
    MultiAgentStepStatus.java
    MultiAgentStepType.java
  infrastructure/
    MultiAgentRunMapper.java
    MultiAgentStepMapper.java
  dto/
    StartMultiAgentRunRequest.java
    MultiAgentRunResponse.java
    MultiAgentStepResponse.java
```

也可以放入现有 `orchestrator/` 包下，但建议用 `orchestration/` 区分「单 Agent 执行」与「多 Agent 编排」。

## 5. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V14__init_multi_agent_orchestration_tables.sql
```

如果当前仓库已有 V14，请顺延为下一个版本号。

### 5.1 multi_agent_run

多 Agent 编排主表。

```sql
CREATE TABLE IF NOT EXISTS multi_agent_run (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    strategy VARCHAR(64) NOT NULL,
    title VARCHAR(255) NULL,
    input_summary TEXT NULL,
    final_summary MEDIUMTEXT NULL,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_multi_agent_run_project_time (project_id, create_time),
    INDEX idx_multi_agent_run_task (task_id),
    INDEX idx_multi_agent_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 编排运行记录';
```

### 5.2 multi_agent_step

多 Agent 编排步骤表。

```sql
CREATE TABLE IF NOT EXISTS multi_agent_step (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    agent_execution_id BIGINT NULL,
    step_order INT NOT NULL,
    step_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_context MEDIUMTEXT NULL,
    output_content MEDIUMTEXT NULL,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_multi_agent_step_run_order (run_id, step_order),
    INDEX idx_multi_agent_step_task (task_id),
    INDEX idx_multi_agent_step_agent (agent_id),
    INDEX idx_multi_agent_step_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 编排步骤';
```

无物理外键，保持当前项目迁移风格。

## 6. 枚举设计

### 6.1 MultiAgentRunStatus

```java
public enum MultiAgentRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED
}
```

### 6.2 MultiAgentStepStatus

```java
public enum MultiAgentStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}
```

### 6.3 MultiAgentStepType

```java
public enum MultiAgentStepType {
    ARCHITECTURE_ANALYSIS,
    BACKEND_IMPLEMENTATION_PLAN,
    FRONTEND_IMPLEMENTATION_PLAN,
    TEST_PLAN,
    CODE_REVIEW,
    FINAL_SUMMARY
}
```

本阶段建议固定默认流程：

| 顺序 | Step Type | Agent Code | 说明 |
|---:|---|---|---|
| 1 | ARCHITECTURE_ANALYSIS | architect-agent | 架构分析 |
| 2 | BACKEND_IMPLEMENTATION_PLAN | backend-agent | 后端方案 |
| 3 | TEST_PLAN | test-agent | 测试方案 |
| 4 | CODE_REVIEW | review-agent | 风险审查 |
| 5 | FINAL_SUMMARY | architect-agent | 总结归档 |

如果项目没有启用某个 Agent：

- 本阶段可以自动跳过该 step，状态为 `SKIPPED`。
- 或使用全局 Agent fallback。

推荐：**优先读取项目启用配置，未启用则 SKIPPED**，更符合 Project Agent 配置闭环。

## 7. API 设计

### 7.1 启动多 Agent 编排

```http
POST /api/tasks/{taskId}/multi-agent-runs
```

权限：

```text
DEVELOPER+
```

请求：

```json
{
  "strategy": "DEFAULT_MOCK",
  "instruction": "请多智能体协作分析这个任务",
  "useRag": true,
  "knowledgeBaseId": "2054487957508165634"
}
```

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| strategy | string | 否 | 默认 `DEFAULT_MOCK` |
| instruction | string | 否 | 用户额外说明 |
| useRag | boolean | 否 | 是否允许 RAG，上限遵循 runtime config |
| knowledgeBaseId | string | 否 | 指定知识库 |

响应：

```json
{
  "id": "2060000000000000001",
  "projectId": "2054487957508165634",
  "taskId": "2054487957508169999",
  "status": "COMPLETED",
  "strategy": "DEFAULT_MOCK",
  "title": "Multi-Agent Mock Run",
  "finalSummary": "...",
  "steps": [
    {
      "id": "2060000000000000002",
      "stepOrder": 1,
      "stepType": "ARCHITECTURE_ANALYSIS",
      "status": "COMPLETED",
      "agentId": "300001",
      "agentName": "Architect Agent",
      "agentExecutionId": "2060000000000000011",
      "outputContent": "..."
    }
  ]
}
```

### 7.2 查询任务的多 Agent 编排记录

```http
GET /api/tasks/{taskId}/multi-agent-runs?page=1&pageSize=10
```

权限：

```text
VIEWER+
```

### 7.3 查询多 Agent 编排详情

```http
GET /api/multi-agent-runs/{runId}
```

权限：

```text
VIEWER+
```

## 8. 状态流转

### 8.1 Run 状态

```text
PENDING → RUNNING → COMPLETED
PENDING → RUNNING → FAILED
PENDING → RUNNING → CANCELED
```

本阶段不做异步取消接口，`CANCELED` 预留。

### 8.2 Step 状态

```text
PENDING → RUNNING → COMPLETED
PENDING → SKIPPED
PENDING → RUNNING → FAILED
```

如果某个 step FAILED：

- run.status = FAILED
- task.status = FAILED
- 后续 step 不执行或标记 SKIPPED

推荐本阶段：失败后停止后续 step。

## 9. 编排服务行为

### 9.1 MultiAgentOrchestrationService.startRun()

核心流程：

1. 查询 Task。
2. 从 Task 解析 projectId。
3. 校验项目权限 `DEVELOPER+`。
4. 校验 Task 状态：
   - 仅允许 `PENDING` 或 `FAILED` 启动多 Agent mock run。
   - 若 `COMPLETED` 返回 `CONFLICT`。
5. 创建 `multi_agent_run`，状态 `RUNNING`。
6. 将 Task 状态推进到 `RUNNING`，复用 Task 状态机或现有受控方法。
7. 构造固定编排计划。
8. 逐个执行 step：
   - 查询对应 Agent。
   - 查询 Project Agent Config。
   - 未启用则写 `SKIPPED`。
   - 已启用则创建 `AgentExecutionEntity`。
   - 写 `AiTaskLogEntity`。
   - 生成 Mock output。
   - step.status = COMPLETED。
9. 汇总 finalSummary。
10. 写 task artifact：
    - title: `Multi-Agent Mock Orchestration Summary`
    - type: `MARKDOWN`
11. 写 task event：
    - `RUNNING → COMPLETED`
12. 更新 Task 为 `COMPLETED`。
13. run.status = `COMPLETED`。
14. 返回 run response。

### 9.2 Mock 输出模板

不同 step 输出不同风格，方便演示：

#### ARCHITECTURE_ANALYSIS

```markdown
## 架构分析

- 识别任务目标
- 判断影响模块
- 给出推荐分层方案
- 标记主要风险
```

#### BACKEND_IMPLEMENTATION_PLAN

```markdown
## 后端实现计划

- 需要新增 / 修改的 Service
- API 行为
- 数据一致性
- 测试建议
```

#### TEST_PLAN

```markdown
## 测试计划

- 单元测试
- 集成测试
- E2E 验证
- 回归风险
```

#### CODE_REVIEW

```markdown
## 代码审查清单

- 权限是否正确
- 状态机是否被绕过
- 是否有敏感信息泄露
- 是否破坏已有接口
```

#### FINAL_SUMMARY

```markdown
## 多智能体协作总结

本次任务已完成 Mock 多智能体协作分析，输出包含架构、后端、测试和审查建议。
```

## 10. 与现有 Task / AgentExecution 的关系

本阶段建议：

- `multi_agent_run` 表示一次多 Agent 编排。
- `multi_agent_step` 表示编排中的每一步。
- 每个真正执行的 step 创建一个 `AgentExecutionEntity`。
- `multi_agent_step.agent_execution_id` 指向对应 execution。

这样可以复用已有：

- `GET /api/tasks/{taskId}/executions`
- `GET /api/agent-executions/{executionId}`
- `GET /api/agent-executions/{executionId}/model-logs`

但本阶段 Mock Step 不必调用真实 ModelGateway。可以：

- 不写 `model_request_log`。
- 或写 `provider=MOCK` 的日志。

推荐：**写 MOCK model request log**，方便 Observability 继续有数据。

## 11. 前端实现

### 11.1 Task Detail 新增 Multi-Agent Tab

在 Task Detail 页面增加一个 Tab：

```text
多智能体
```

如果已有 SectionRail，则新增一项：

```text
Overview / Logs / Artifacts / Executions / Multi-Agent
```

### 11.2 MultiAgentRunPanel.vue

新增组件：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

页面内容：

1. 顶部操作区：
   - 「启动多智能体编排」按钮
   - strategy 显示 `DEFAULT_MOCK`
   - 说明：当前阶段为 Mock 编排，不执行真实代码修改。
2. Run 列表：
   - run id
   - status
   - strategy
   - startedAt / finishedAt
3. Timeline：
   - Architect
   - Backend
   - Test
   - Review
   - Summary
4. Step 详情：
   - agent name
   - step type
   - status
   - output markdown
5. Final Summary：
   - markdown 渲染。

### 11.3 UI 风格

要求：

- 保持当前暗色科技风。
- 使用现有 `StatusPulse` / `GlowButton` / `MarkdownRenderer`。
- 不引入新 UI 框架。
- 不做复杂拖拽编排器。
- 不做工作流画布。

## 12. 权限设计

| API | 权限 |
|---|---|
| POST `/api/tasks/{taskId}/multi-agent-runs` | DEVELOPER+ |
| GET `/api/tasks/{taskId}/multi-agent-runs` | VIEWER+ |
| GET `/api/multi-agent-runs/{runId}` | VIEWER+ |

权限路径：

```text
taskId → task.projectId → ProjectPermissionService.checkProjectRole(...)
```

必须与 Task 模块既有模式一致。

## 13. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java
```

至少覆盖：

1. DEVELOPER 可以启动 multi-agent run。
2. 未登录启动返回 UNAUTHORIZED。
3. 非项目成员启动返回 PROJECT_ACCESS_DENIED。
4. VIEWER 不可启动。
5. VIEWER 可查询 run 列表。
6. run 创建后状态为 COMPLETED。
7. steps 按 stepOrder 返回。
8. 未启用 Agent 的 step 标记 SKIPPED。
9. 已启用 Agent 的 step 生成 AgentExecution。
10. Task 最终变为 COMPLETED。
11. Task Logs 包含 MULTI_AGENT_START / STEP_DONE / MULTI_AGENT_DONE。
12. Task Artifacts 包含 `Multi-Agent Mock Orchestration Summary`。
13. COMPLETED task 重复启动返回 CONFLICT。
14. 无效 taskId 返回 NOT_FOUND。

后端质量门：

```bash
cd backend
mvn test
```

## 14. 前端测试要求

新增 E2E：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

至少覆盖：

1. 登录后创建项目和任务。
2. 打开 Task Detail。
3. 进入「多智能体」Tab。
4. 点击启动多智能体编排。
5. 页面显示 Timeline。
6. 至少一个 step 显示 COMPLETED 或 SKIPPED。
7. Final Summary 可见。
8. 返回 Artifacts Tab 能看到总结产物。
9. 页面无 JS error。

前端质量门：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果验证码影响 E2E，继续使用：

```bash
bash scripts/start-e2e-backend.sh
```

## 15. 手动验证清单

1. 登录成功。
2. 创建或选择 Demo Project。
3. 启用至少 Architect / Backend / Test / Review Agent。
4. 创建一个 Task。
5. 打开 Task Detail。
6. 进入「多智能体」Tab。
7. 点击「启动多智能体编排」。
8. Timeline 显示多个步骤。
9. Step output 可展开查看。
10. Task 状态变为 COMPLETED。
11. Logs 显示多 Agent 编排日志。
12. Artifacts 显示 Multi-Agent Summary。
13. Executions 显示多个 Agent Execution。
14. Observability 中可看到相关审计或模型日志。

## 16. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35A 完成报告

1. 新增 / 修改文件清单
2. 数据库 Migration 说明
3. Multi-Agent Run / Step 设计说明
4. 后端 API 实现说明
5. Mock 编排流程说明
6. 与 Task / AgentExecution / Logs / Artifacts 的集成说明
7. 前端 Multi-Agent Tab / Timeline 实现说明
8. 后端测试覆盖说明
9. 前端 typecheck / build / E2E 结果
10. 手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 35B
```

## 17. 已知限制

本阶段完成后仍然不会具备：

- 真正的多模型协同推理。
- 自动任务拆解。
- Agent 之间的真实消息传递。
- 工作流可视化编辑。
- 并行执行。
- 异步队列调度。
- Git / shell / 文件写操作。

这些留到后续：

```text
35B: Multi-Agent Message Passing
35C: Multi-Agent Parallel Mock Execution
35D: Workflow Template / Strategy Config
36A: Safe Tool Execution Sandbox
36B: Real Code Generation Proposal Flow
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35A。

文档路径：

```text
docs/milestone-35a-multi-agent-mock-orchestration.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在已有单 Agent Orchestrator、Project Agent Config、Task Detail 的基础上新增 Multi-Agent Mock Orchestration。
3. 不要重写已有 Orchestrator。
4. 不要破坏 `POST /api/tasks/{taskId}/execute` 单 Agent 执行接口。
5. 不接真实多 Agent 推理。
6. 不执行 shell。
7. 不执行 Git 写操作。
8. 不生成真实代码文件。
9. 不绕过 Task 状态机。
10. 不更换技术栈，不更换 UI 框架。
11. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
12. 复用现有 ProjectPermissionService 权限模型。
13. 复用现有 Task Logs / Events / Artifacts / AgentExecution / ModelRequestLog 能力。
14. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
15. 所有新 API 的 ID 对外保持 String。
16. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 `V14__init_multi_agent_orchestration_tables.sql`，如果 V14 已存在则顺延版本号。
2. 新增 `multi_agent_run` 和 `multi_agent_step` 两张表。
3. 新增 MultiAgentRun / MultiAgentStep Entity、Mapper、Enum、DTO。
4. 新增 `MultiAgentOrchestrationService`。
5. 新增 `MultiAgentOrchestrationController`。
6. 实现 `POST /api/tasks/{taskId}/multi-agent-runs`。
7. 实现 `GET /api/tasks/{taskId}/multi-agent-runs`。
8. 实现 `GET /api/multi-agent-runs/{runId}`。
9. 实现固定 Mock 编排流程：
   - ARCHITECTURE_ANALYSIS
   - BACKEND_IMPLEMENTATION_PLAN
   - TEST_PLAN
   - CODE_REVIEW
   - FINAL_SUMMARY
10. 对每个已启用 Agent Step 创建 AgentExecution。
11. 对未启用 Agent Step 标记 SKIPPED。
12. 写 Task Logs、Task Events、Task Artifact。
13. Task 最终推进到 COMPLETED。
14. 前端 Task Detail 新增「多智能体」Tab。
15. 新增 `MultiAgentRunPanel.vue`，展示启动按钮、run 列表、timeline、step output、final summary。
16. 新增后端集成测试。
17. 新增前端 E2E 测试。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果 E2E 需要后端测试环境，请先执行：

```bash
bash scripts/start-e2e-backend.sh
```

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. 数据库 Migration 说明
3. Multi-Agent Run / Step 设计说明
4. 后端 API 实现说明
5. Mock 编排流程说明
6. 与 Task / AgentExecution / Logs / Artifacts 的集成说明
7. 前端 Multi-Agent Tab / Timeline 实现说明
8. 后端测试覆盖说明
9. 前端 typecheck / build / E2E 结果
10. 手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 35B

现在开始实现，不要只给计划。
