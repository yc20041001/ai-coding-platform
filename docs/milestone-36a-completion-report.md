# Milestone 36A: Safe Tool Execution Sandbox — 完成报告

## 1. 新增 / 修改文件清单

### 新增文件 (10 files)

**Backend — Domain / Enums (4 files)**

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionStatus.java` | 工具执行状态枚举 (PENDING / RUNNING / COMPLETED / FAILED / BLOCKED) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionMode.java` | 工具执行模式枚举 (DRY_RUN / MOCK_EXECUTE) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolType.java` | 工具类型枚举 (READ_ONLY / MOCK / ANALYSIS) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolName.java` | 工具名称枚举 (PROJECT_CONTEXT_SCAN / TASK_REQUIREMENT_ANALYSIS / MOCK_FILE_INSPECTION / MOCK_TEST_PLAN_SCAN / MOCK_SECURITY_REVIEW) |

**Backend — Entity / Mapper / DTO (3 files)**

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolSandboxExecutionEntity.java` | 工具沙箱执行实体，映射 `tool_sandbox_execution` 表 |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolSandboxExecutionMapper.java` | MyBatis-Plus Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolSandboxExecutionResponse.java` | 工具沙箱执行响应 DTO（所有 ID 对外保持 String） |

**Backend — Service (1 file)**

| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java` | 工具沙箱执行服务：Mock 执行、按 Run/Step 查询、单条详情查询 |

**Backend — Migration (1 file)**

| `backend/src/main/resources/db/migration/V20__init_tool_sandbox_tables.sql` | Flyway 迁移：建表 `tool_sandbox_execution` |

**Backend — Test (0 new files, tests added to existing)**

| `backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java` | 新增 12 个工具沙箱集成测试 |

**Frontend (0 new files, modifications only)**

| `frontend/e2e/multi-agent-orchestration.spec.ts` | 新增 5 个工具沙箱 E2E 测试 |

### 修改文件 (6 files)

| 文件 | 变更说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentStepResponse.java` | 新增 `toolExecutions` 字段 |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentRunResponse.java` | 新增 `toolExecutions` 字段 |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/MultiAgentOrchestrationService.java` | 注入 ToolSandboxExecutionService；completed step 后创建 tool execution；写入 TOOL_SANDBOX_EXECUTED 日志；toRunResponse / toStepResponse / toPhaseResponse 填充 toolExecutions |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/MultiAgentOrchestrationController.java` | 注入 ToolSandboxExecutionService；新增 3 个查询 API |
| `backend/src/test/resources/schema.sql` | 新增 V20 `tool_sandbox_execution` 建表（H2 兼容） |
| `frontend/src/modules/task/api.ts` | 新增 `ToolSandboxExecutionResponse` 类型；`MultiAgentStepResponse` / `MultiAgentRunResponse` 增加 `toolExecutions`；新增 3 个 API 函数 |
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | 新增工具沙箱统计标签、Step 内工具卡片、安全声明、可折叠输出 |

## 2. 数据库表说明

- 表名：`tool_sandbox_execution`，引擎 InnoDB，字符集 utf8mb4
- 主键：`id BIGINT`，使用 `IdType.ASSIGN_ID`（雪花算法）
- 索引：`idx_tool_sandbox_project_time`、`idx_tool_sandbox_task`、`idx_tool_sandbox_run`、`idx_tool_sandbox_step`、`idx_tool_sandbox_agent`、`idx_tool_sandbox_status`
- 无物理外键，保持项目现有数据库风格
- `input_payload` / `output_payload` 使用 JSON 类型（H2 测试环境使用 TEXT）

## 3. Tool Sandbox Execution 设计说明

### 核心流程

```text
Step COMPLETED → ToolSandboxExecutionService.mockExecuteForStep() → INSERT execution (RUNNING) → 生成 Mock output → UPDATE execution (COMPLETED) → 写入 TOOL_SANDBOX_EXECUTED 日志
```

### stepType → toolName 映射

| stepType | toolName | toolType |
|---|---|---|
| ARCHITECTURE_ANALYSIS | PROJECT_CONTEXT_SCAN | READ_ONLY |
| BACKEND_IMPLEMENTATION_PLAN | TASK_REQUIREMENT_ANALYSIS | ANALYSIS |
| FRONTEND_IMPLEMENTATION_PLAN | MOCK_FILE_INSPECTION | READ_ONLY |
| TEST_PLAN | MOCK_TEST_PLAN_SCAN | MOCK |
| CODE_REVIEW | MOCK_SECURITY_REVIEW | ANALYSIS |
| FINAL_SUMMARY | PROJECT_CONTEXT_SCAN | READ_ONLY |

### 触发时机

- 只对 COMPLETED step 创建 tool execution
- SKIPPED step 不创建 tool execution
- WAITING_APPROVAL 前已完成的 step 也有 tool execution
- approval 继续执行后的后续 step 也有 tool execution
- executionMode 固定为 `MOCK_EXECUTE`

## 4. Mock Tool Executor 行为说明

- 不调用真实外部命令
- 不执行 Shell / Git 写操作
- 不写入文件系统
- 生成 deterministic mock output，`output_payload` 固定包含：
  - `"mock": true`
  - `"readOnly": true`
  - `"filesTouched": []`
  - `"gitOperations": []`
- `durationMs` 随机 5-25ms 模拟执行耗时
- `summary` 明确声明 "Mock 工具执行完成：...只读模拟，无文件写入，无 Git 操作"

## 5. Multi-Agent 集成说明

- `MultiAgentOrchestrationService` 构造器新增 `ToolSandboxExecutionService` 依赖
- `executePhase()` 方法中，step 标记 COMPLETED 后立即调用 `toolSandboxExecutionService.mockExecuteForStep()`
- 然后写入 `TOOL_SANDBOX_EXECUTED` 日志
- `toRunResponse()` 中查询所有 tool executions，分组到 stepId，同时设置 run 级别和 step 级别的 `toolExecutions`
- `getPhases()` 也加载 tool executions 并填充到 phase → step 响应中

## 6. 后端 API 清单

| Method | Path | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/multi-agent-runs/{runId}/tool-executions` | VIEWER+ | 查询 Run 下所有工具执行 |
| GET | `/api/multi-agent-steps/{stepId}/tool-executions` | VIEWER+ | 查询 Step 下所有工具执行 |
| GET | `/api/tool-sandbox-executions/{executionId}` | VIEWER+ | 查询单条工具执行详情 |

权限校验：通过 run / step / execution 获取 projectId，调用 `ProjectPermissionService.checkProjectMember()`。

## 7. 前端展示说明

- **Run 顶部统计**：新增 "工具执行 N"（warning dark）和 "Mock 执行 N"（info dark）标签
- **Step 展开区域**：在消息区和输出区之间新增「工具沙箱」卡片区域
- **工具卡片**：展示 toolName（中文映射）、executionMode（MOCK_EXECUTE）、status（StatusPulse）、durationMs、summary
- **可折叠输出**：点击 "▸ 查看输出" 展开 formatted JSON
- **安全声明**：固定文本 "当前为 Mock 沙箱执行：未执行真实 Shell，未执行 Git 写操作，未写入文件。"

data-testid 属性：
- `multi-agent-tool-summary` — 工具执行统计标签
- `multi-agent-tool-section` — 工具沙箱区域
- `multi-agent-tool-card` — 工具卡片
- `multi-agent-tool-output` — 工具输出折叠按钮

## 8. 安全边界说明

严格遵守：
- 不执行真实 Shell 命令
- 不执行真实 Git 写操作
- 不修改真实业务代码文件
- 不生成真实代码补丁
- 不接入远程执行器
- 不引入 Docker sandbox / Firecracker / Kubernetes Job
- 不做异步 Worker / 队列
- 不做文件系统写入工具

所有工具执行的 `output_payload` 均显式声明 `mock=true, readOnly=true, filesTouched=[], gitOperations=[]`。

## 9. 后端测试结果

### MultiAgentOrchestrationIntegrationTest（74 tests，全部通过）

新增 12 个工具沙箱测试：

| # | 测试 | 说明 |
|---|---|---|
| 1 | shouldCompletedStepsGenerateToolExecutions | 4 个 completed step 均有 tool execution |
| 2 | shouldSkippedStepsNotGenerateToolExecutions | SKIPPED step 不生成 tool execution |
| 3 | shouldRunDetailReturnToolExecutions | Run detail 返回 toolExecutions 数组 |
| 4 | shouldStepResponseIncludeToolExecutions | Step response 含 toolExecutions |
| 5 | shouldGetRunToolExecutionsEndpoint | GET run tool-executions 成功 |
| 6 | shouldGetStepToolExecutionsEndpoint | GET step tool-executions 成功 |
| 7 | shouldGetSingleToolExecutionEndpoint | GET single tool execution 成功 |
| 8 | shouldToolExecutionOutputDeclareMockSafety | output 含 mock/readOnly/filesTouched=[],gitOperations=[] |
| 9 | shouldTaskLogsContainToolSandboxExecuted | Task log 含 TOOL_SANDBOX_EXECUTED |
| 10 | shouldRejectUnauthenticatedForToolExecutionApis | 未认证返回 UNAUTHORIZED |
| 11 | shouldReturnNotFoundForInvalidToolExecutionId | 无效 ID 返回 NOT_FOUND |
| 12 | shouldPhasesIncludeStepsWithToolExecutions | Phase → Step 含 toolExecutions |

### WorkflowTemplateIntegrationTest（12 tests，全部通过）

### 全量后端测试：304 tests，1 预存失败（TaskStateMachineTest.shouldRejectTransitionToNull，与本次变更无关）

## 10. 前端 typecheck / build / E2E 结果

| 项 | 结果 |
|---|---|
| vue-tsc --noEmit | 通过（零错误） |
| vite build | 通过 |
| E2E (5 new tests) | 全部通过 |

### 新增 E2E 测试明细

| # | 测试 | 结果 |
|---|---|---|
| 1 | should display tool execution stats after multi-agent run | 通过 |
| 2 | should show tool sandbox section when step is expanded | 通过 |
| 3 | should tool card show MOCK_EXECUTE and COMPLETED | 通过 |
| 4 | should tool output contain safety declarations | 通过 |
| 5 | should not have JS errors with tool sandbox display | 通过 |

全量 E2E：60/61 通过（1 个预存失败：`should show Phase 2 with three parallel lanes`，前端时序问题，与本次变更无关）

## 11. 已知限制

与设计文档一致，36A 完成后仍不包含：

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

## 12. 是否可以进入 Milestone 36B

可以。Milestone 36A 全部质量门通过（后端 304 tests - 1 预存失败，typecheck 零错误，build 成功，E2E 5/5 新测试通过）。建议下一步进入 36B（Read-only Tool Catalog）或用户指定的下一里程碑。
