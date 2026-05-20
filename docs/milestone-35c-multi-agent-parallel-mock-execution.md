# Milestone 35C: Multi-Agent Parallel Mock Execution

## 1. 背景

Milestone 35A 已完成 Multi-Agent Mock Orchestration：

- Multi-Agent Run / Step 表。
- 固定 Mock 编排流程。
- Task Logs / Events / Artifacts / AgentExecution 集成。
- 前端「多智能体」Tab 和 Timeline。

Milestone 35B 已完成 Multi-Agent Message Passing：

- `multi_agent_message` 表。
- `TASK_CONTEXT` / `STEP_OUTPUT` / `HANDOFF` / `REVIEW_FEEDBACK` / `FINAL_CONTEXT`。
- Step 的 `inputContext` 可消费前序 Step 输出。
- 前端可展示消息链路、输入上下文、输出内容。

当前编排仍然是纯串行：

```text
Architect → Backend → Test → Review → Final Summary
```

Milestone 35C 的目标是引入 **并行分组模型**：

```text
Phase 1: Architect
Phase 2: Backend + Frontend + Test
Phase 3: Review
Phase 4: Final Summary
```

注意：本阶段是 **Parallel Mock Execution**，不要求真正线程并发、不引入队列、不做异步 worker。后端可以同步执行，但数据结构、API 响应、前端 UI 必须体现 phase / group / lane 的并行语义。

## 2. 总目标

实现多 Agent Mock 编排的 Phase 化能力：

1. Multi-Agent Run 拥有多个 Phase。
2. 每个 Step 归属于一个 Phase。
3. 同一 Phase 内多个 Step 逻辑上并行。
4. Phase 之间顺序推进。
5. 前端 Timeline 从单线升级为 Phase / Lane 视图。
6. Message Passing 支持跨 Phase 聚合：
   - Phase 1 输出进入 Phase 2 所有 Step。
   - Phase 2 多个 Step 输出汇总后进入 Phase 3。
   - Phase 3 输出进入 Phase 4。
7. 保持 Mock，不执行真实代码，不写 Git。

完成后，平台从：

```text
串行多 Agent 协作
```

升级为：

```text
分阶段多 Agent 协作 + Mock 并行泳道展示
```

## 3. 严格边界

必须遵守：

1. 不做真实线程并发要求。
2. 不引入 RabbitMQ / Redis 队列。
3. 不做异步 worker。
4. 不做真实 LLM 多 Agent 推理。
5. 不执行 shell。
6. 不执行 Git 写操作。
7. 不修改仓库文件。
8. 不生成真实代码文件。
9. 不做工作流画布。
10. 不做拖拽编辑器。
11. 不破坏 35A / 35B API。
12. 不破坏单 Agent `POST /api/tasks/{taskId}/execute`。
13. 不绕过 Task 状态机。
14. 前端保持当前中文暗色科技风 UI。

允许做：

- 给 `multi_agent_step` 增加 phase 字段。
- 新增 `multi_agent_phase` 表。
- 新增 phase DTO / Entity / Mapper。
- 扩展 Run Response 返回 phases。
- 扩展前端 MultiAgentRunPanel 显示 phase lanes。
- 同步执行多个 step，但以相同 phase 编组展示。

推荐本阶段新增 `multi_agent_phase` 表，保持结构清晰。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V17__init_multi_agent_phases.sql
```

如果 V17 已存在，请顺延到下一个版本号。

### 4.1 multi_agent_phase

```sql
CREATE TABLE IF NOT EXISTS multi_agent_phase (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    phase_order INT NOT NULL,
    phase_key VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_summary TEXT NULL,
    output_summary TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_multi_agent_phase_run_order (run_id, phase_order),
    INDEX idx_multi_agent_phase_task (task_id),
    INDEX idx_multi_agent_phase_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 编排阶段';
```

### 4.2 multi_agent_step 增加 phase 字段

```sql
ALTER TABLE multi_agent_step
    ADD COLUMN phase_id BIGINT NULL AFTER run_id,
    ADD COLUMN phase_order INT NULL AFTER phase_id,
    ADD COLUMN lane_key VARCHAR(64) NULL AFTER phase_order,
    ADD INDEX idx_multi_agent_step_phase_order (phase_id, step_order);
```

无物理外键，保持当前项目风格。

## 5. Phase 设计

### 5.1 Phase 枚举

新增：

```text
MultiAgentPhaseStatus.java
```

```java
public enum MultiAgentPhaseStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}
```

可选新增：

```text
MultiAgentPhaseKey.java
```

```java
public enum MultiAgentPhaseKey {
    PLANNING,
    IMPLEMENTATION,
    REVIEW,
    SUMMARY
}
```

### 5.2 默认 Phase 模板

| Phase Order | Phase Key | Title | Steps |
|---:|---|---|---|
| 1 | PLANNING | 架构规划 | ARCHITECTURE_ANALYSIS |
| 2 | IMPLEMENTATION | 实现方案并行分析 | BACKEND_IMPLEMENTATION_PLAN, FRONTEND_IMPLEMENTATION_PLAN, TEST_PLAN |
| 3 | REVIEW | 综合审查 | CODE_REVIEW |
| 4 | SUMMARY | 最终总结 | FINAL_SUMMARY |

### 5.3 Lane 设计

每个 Step 增加 `laneKey`：

| Step Type | Lane Key |
|---|---|
| ARCHITECTURE_ANALYSIS | architect |
| BACKEND_IMPLEMENTATION_PLAN | backend |
| FRONTEND_IMPLEMENTATION_PLAN | frontend |
| TEST_PLAN | test |
| CODE_REVIEW | review |
| FINAL_SUMMARY | summary |

前端用 laneKey 渲染泳道。

## 6. Entity / Mapper / DTO

### 6.1 新增 Entity

```text
MultiAgentPhaseEntity.java
```

要求：

- `@TableName("multi_agent_phase")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` createTime
- `@TableField(fill = FieldFill.INSERT_UPDATE)` updateTime
- 不继承 BaseEntity
- 手写 getter/setter

字段：

```java
private Long id;
private Long runId;
private Long projectId;
private Long taskId;
private Integer phaseOrder;
private String phaseKey;
private String title;
private String status;
private String inputSummary;
private String outputSummary;
private LocalDateTime startedAt;
private LocalDateTime finishedAt;
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

### 6.2 新增 Mapper

```text
MultiAgentPhaseMapper.java
```

```java
public interface MultiAgentPhaseMapper extends BaseMapper<MultiAgentPhaseEntity> {
}
```

### 6.3 新增 DTO

```text
MultiAgentPhaseResponse.java
```

字段：

```java
private String id;
private String runId;
private Integer phaseOrder;
private String phaseKey;
private String title;
private String status;
private String inputSummary;
private String outputSummary;
private List<MultiAgentStepResponse> steps;
private LocalDateTime startedAt;
private LocalDateTime finishedAt;
```

### 6.4 修改 Step DTO

`MultiAgentStepResponse` 增加：

```java
private String phaseId;
private Integer phaseOrder;
private String laneKey;
```

### 6.5 修改 Run DTO

`MultiAgentRunResponse` 增加：

```java
private List<MultiAgentPhaseResponse> phases;
```

保持已有 `steps` 和 `messages` 字段，不能删除。

## 7. API 设计

### 7.1 Run Detail 增强

已有：

```http
GET /api/multi-agent-runs/{runId}
```

增强响应：

```json
{
  "id": "206...",
  "status": "COMPLETED",
  "phases": [
    {
      "id": "207...",
      "phaseOrder": 1,
      "phaseKey": "PLANNING",
      "title": "架构规划",
      "status": "COMPLETED",
      "steps": []
    }
  ],
  "steps": [],
  "messages": []
}
```

### 7.2 查询 Run Phases

新增：

```http
GET /api/multi-agent-runs/{runId}/phases
```

权限：

```text
VIEWER+
```

响应：

```json
[
  {
    "id": "...",
    "phaseOrder": 2,
    "phaseKey": "IMPLEMENTATION",
    "title": "实现方案并行分析",
    "status": "COMPLETED",
    "steps": [
      {
        "stepType": "BACKEND_IMPLEMENTATION_PLAN",
        "laneKey": "backend",
        "status": "COMPLETED"
      }
    ]
  }
]
```

### 7.3 启动 Run 请求兼容

已有：

```http
POST /api/tasks/{taskId}/multi-agent-runs
```

请求不强制改动。

可选新增：

```json
{
  "strategy": "PHASED_PARALLEL_MOCK"
}
```

默认策略建议从 `DEFAULT_MOCK` 升级为：

```text
PHASED_PARALLEL_MOCK
```

但要兼容旧值 `DEFAULT_MOCK`。

## 8. Phase 执行规则

### 8.1 总流程

```text
Run RUNNING
  Phase 1 RUNNING
    Architect Step
  Phase 1 COMPLETED

  Phase 2 RUNNING
    Backend Step
    Frontend Step
    Test Step
  Phase 2 COMPLETED

  Phase 3 RUNNING
    Review Step
  Phase 3 COMPLETED

  Phase 4 RUNNING
    Final Summary Step
  Phase 4 COMPLETED
Run COMPLETED
```

### 8.2 Mock 并行语义

本阶段后端可以同步执行 Phase 2 的 Backend / Frontend / Test：

```java
for (Step step : phase.steps) {
    executeStepSynchronously(step);
}
```

但数据上必须体现：

- 相同 `phaseId`
- 相同 `phaseOrder`
- 不同 `laneKey`
- Phase status 覆盖该组步骤

前端以并行泳道展示，用户看到的是“同阶段多 Agent 分工”。

### 8.3 Phase 状态计算

| Step 状态组合 | Phase 状态 |
|---|---|
| 至少一个 FAILED | FAILED |
| 所有 step SKIPPED | SKIPPED |
| 所有非 skipped step COMPLETED | COMPLETED |
| 执行中 | RUNNING |

如果 Phase FAILED：

- Run FAILED
- Task FAILED
- 后续 Phase 不执行，标记 SKIPPED 或不创建。

推荐本阶段：后续 Phase 已创建但标记 `SKIPPED`。

## 9. Message Passing 与 Phase 聚合

35C 必须复用 35B 的 message passing。

新增要求：

### 9.1 Phase 输入

每个 Phase 开始时生成或更新 `phase.inputSummary`：

```text
Phase 2 input = Phase 1 completed step summaries
Phase 3 input = Phase 2 backend/frontend/test outputs
Phase 4 input = Phase 1 + Phase 2 + Phase 3 summaries
```

### 9.2 Phase 输出

每个 Phase 完成后生成 `phase.outputSummary`：

```text
Phase 2 output = Backend + Frontend + Test step summaries
```

### 9.3 Handoff 粒度

35B 的 HANDOFF 是 step → step。

35C 增加 phase-aware handoff：

```text
Phase 1 → Phase 2 all steps
Phase 2 all steps → Phase 3
Phase 3 → Phase 4
```

可以继续写入 `multi_agent_message`，message content 中注明 phase。

## 10. Mock 输出增强

新增 Frontend Step Mock 输出：

```markdown
## 前端实现计划

基于 Architect Agent 的架构规划，本步骤建议：

1. 页面入口与路由
2. API client 类型定义
3. 状态展示与交互反馈
4. E2E 覆盖点

已消费上游上下文：
- PLANNING / ARCHITECTURE_ANALYSIS
```

Phase 2 的 Backend / Frontend / Test 都应显示：

```markdown
同阶段并行角色：Backend / Frontend / Test
```

Review Step 应显示：

```markdown
已聚合 Phase 2 输出：
- Backend
- Frontend
- Test
```

Final Summary 应显示：

```markdown
已聚合所有 Phase：
- PLANNING
- IMPLEMENTATION
- REVIEW
```

## 11. 前端实现

### 11.1 MultiAgentRunPanel 增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

新增 Phase / Lane 视图。

推荐布局：

```text
Phase 1: 架构规划
  [Architect lane]

Phase 2: 实现方案并行分析
  [Backend lane] [Frontend lane] [Test lane]

Phase 3: 综合审查
  [Review lane]

Phase 4: 最终总结
  [Summary lane]
```

每个 Lane 卡片展示：

- Agent Name
- Step Type
- StatusPulse
- Input Context toggle
- Output Content toggle
- Message count

### 11.2 Run Summary 增强

顶部 summary cards：

| 指标 | 说明 |
|---|---|
| Phases | phase 数量 |
| Parallel Steps | phase 内 step 数 > 1 的 step 总数 |
| Messages | message 总数 |
| Completed | completed step 数 |
| Skipped | skipped step 数 |

### 11.3 API client

修改：

```text
frontend/src/modules/task/api.ts
```

新增：

```ts
export interface MultiAgentPhaseResponse { ... }
export function getMultiAgentRunPhases(runId: string) { ... }
```

Run detail 类型增加：

```ts
phases?: MultiAgentPhaseResponse[]
```

### 11.4 UI 要求

- 中文 UI。
- 暗色科技风。
- 不用新 UI 框架。
- 不引入图形画布库。
- 不做拖拽。
- 移动端基础可读即可。

## 12. 后端测试要求

扩展：

```text
MultiAgentOrchestrationIntegrationTest.java
```

至少新增测试：

1. start run 后创建 4 个 phase。
2. phases 按 phaseOrder ASC 返回。
3. Phase 2 包含 Backend / Frontend / Test 三个 step。
4. Phase 2 三个 step 拥有相同 phaseId / phaseOrder。
5. 每个 step 返回 laneKey。
6. Run detail 返回 phases。
7. `GET /api/multi-agent-runs/{runId}/phases` 返回 phases。
8. Phase 1 outputSummary 被 Phase 2 inputSummary 引用。
9. Phase 2 outputSummary 聚合 Backend / Frontend / Test。
10. Review step inputContext 包含 Phase 2 聚合摘要。
11. Final Summary 包含所有 Phase 摘要。
12. 未启用 Frontend Agent 时，Frontend Step SKIPPED，Phase 2 仍可 COMPLETED 或 PARTIAL_COMPLETED。
13. 非项目成员查询 phases 返回 PROJECT_ACCESS_DENIED。
14. 未登录查询 phases 返回 UNAUTHORIZED。

如需要 `PARTIAL_COMPLETED`，可以加入枚举；如果不想新增状态，则规则为：

```text
Phase 中至少一个 completed 且没有 failed → COMPLETED
```

## 13. 前端 E2E 要求

扩展：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

至少新增测试：

1. 启动 run 后显示 Phase 视图。
2. Phase 2 显示 Backend / Frontend / Test 三个 lane。
3. 每个 lane 可展开输入上下文。
4. Review lane 可看到 Phase 2 聚合提示。
5. Final Summary 显示 phase 数和 message 数。
6. 页面无 JS error。

前端质量门：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果 E2E 需要后端测试环境：

```bash
bash scripts/start-e2e-backend.sh
```

## 14. 已知 flaky 收口要求

35B 报告中仍有一个已知旧 flaky：

```text
agent-version dialog
```

35C 执行时需要处理其中一种：

1. 修复该 flaky，使全量 E2E 通过。
2. 或明确隔离该 flaky，并在报告中说明不影响 35C 质量门。

推荐：优先修复，目标：

```text
npm run test:e2e -- --workers=1 全量通过
```

## 15. 手动验证清单

1. 登录成功。
2. 打开项目。
3. 启用 Architect / Backend / Frontend / Test / Review Agent。
4. 创建 Task。
5. 打开 Task Detail。
6. 进入「多智能体」Tab。
7. 启动多智能体编排。
8. 页面显示 4 个 Phase。
9. Phase 2 显示 Backend / Frontend / Test 三个并行 lane。
10. 展开 Backend 输入上下文，可看到 Architect 摘要。
11. 展开 Test 输入上下文，可看到 Architect 摘要。
12. Review 输入上下文可看到 Phase 2 聚合摘要。
13. Final Summary 可看到所有 Phase 摘要。
14. Artifacts 中有 Multi-Agent Summary。
15. Executions 中有多个 AgentExecution。

## 16. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35C 完成报告

1. 新增 / 修改文件清单
2. 数据库 Migration 说明
3. Multi-Agent Phase 设计说明
4. Phase / Lane 执行规则说明
5. Message Passing 与 Phase 聚合说明
6. Mock 输出增强说明
7. 后端 API 实现说明
8. 前端 Phase / Lane 视图实现说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. Flaky 测试处理说明
12. 手动验证结果
13. 已知限制
14. 是否可以进入 Milestone 35D
```

## 17. 已知限制

35C 完成后仍然不包含：

- 真实并发执行。
- 异步队列。
- Worker 调度。
- 工作流编辑器。
- Agent 自由对话。
- 工具调用。
- 真实代码修改。

后续建议：

```text
35D: Workflow Strategy Template
35E: Human Approval Gate
36A: Safe Tool Execution Sandbox
36B: Code Change Proposal Flow
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35C。

文档路径：

```text
docs/milestone-35c-multi-agent-parallel-mock-execution.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在 Milestone 35A / 35B 基础上新增 Phase / Lane 的 Parallel Mock Execution。
3. 不要重写 35A / 35B 已完成的 Run / Step / Message / Timeline 能力。
4. 不要破坏已有 `POST /api/tasks/{taskId}/multi-agent-runs`。
5. 不要破坏已有 `GET /api/multi-agent-runs/{runId}` 和 messages API。
6. 不要破坏已有单 Agent `POST /api/tasks/{taskId}/execute`。
7. 不接真实多 Agent 推理。
8. 不要求真实线程并发。
9. 不执行 shell。
10. 不执行 Git 写操作。
11. 不生成真实代码文件。
12. 不引入队列，不引入工作流引擎。
13. 不更换技术栈，不更换 UI 框架。
14. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
15. 复用现有 ProjectPermissionService 权限模型。
16. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
17. 所有新增 API 的 ID 对外保持 String。
18. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 `V17__init_multi_agent_phases.sql`，如果 V17 已存在则顺延版本号。
2. 新增 `multi_agent_phase` 表。
3. 修改 `multi_agent_step`，增加 `phase_id`、`phase_order`、`lane_key`。
4. 新增 `MultiAgentPhaseEntity`。
5. 新增 `MultiAgentPhaseMapper`。
6. 新增 `MultiAgentPhaseStatus` 枚举。
7. 新增 `MultiAgentPhaseResponse` DTO。
8. 修改 `MultiAgentStepResponse`，返回 phaseId / phaseOrder / laneKey。
9. 修改 `MultiAgentRunResponse`，返回 phases。
10. 新增 `GET /api/multi-agent-runs/{runId}/phases`。
11. 增强 `GET /api/multi-agent-runs/{runId}`，返回 phases。
12. 实现默认 Phase 模板：
    - PLANNING: Architect
    - IMPLEMENTATION: Backend + Frontend + Test
    - REVIEW: Review
    - SUMMARY: Final Summary
13. Phase 2 的 Backend / Frontend / Test 必须拥有相同 phaseOrder，不同 laneKey。
14. 后端可同步执行，但数据和响应必须体现 Mock parallel semantics。
15. Phase inputSummary / outputSummary 必须聚合上下游消息。
16. Review Step inputContext 必须包含 Phase 2 聚合摘要。
17. Final Summary 必须包含所有 Phase 摘要。
18. 前端 MultiAgentRunPanel.vue 增加 Phase / Lane 视图。
19. 前端 summary 显示 Phases / Parallel Steps / Messages / Completed / Skipped。
20. 修复或隔离 35B 报告中的 agent-version dialog flaky。
21. 扩展后端集成测试。
22. 扩展前端 E2E 测试。

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
3. Multi-Agent Phase 设计说明
4. Phase / Lane 执行规则说明
5. Message Passing 与 Phase 聚合说明
6. Mock 输出增强说明
7. 后端 API 实现说明
8. 前端 Phase / Lane 视图实现说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. Flaky 测试处理说明
12. 手动验证结果
13. 已知限制
14. 是否可以进入 Milestone 35D

现在开始实现，不要只给计划。
