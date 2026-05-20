# Milestone 35B: Multi-Agent Message Passing

## 1. 背景

Milestone 35A 已完成 Multi-Agent Mock Orchestration：

- `multi_agent_run` / `multi_agent_step` 表。
- 固定 5 步 Mock 编排流程。
- 每个已启用 Agent Step 创建 `AgentExecution`。
- Task Logs / Events / Artifacts 已接入。
- Task Detail 已有「多智能体」Tab 和 Timeline 展示。

当前 35A 的多 Agent 编排仍偏“串行展示”：

```text
Architect Step → Backend Step → Test Step → Review Step → Final Summary
```

但每个 Step 的上下文还不够明确，后续 Agent 没有显式消费前序 Agent 的输出。

Milestone 35B 的目标是补齐 **Agent Message Passing**：

```text
Architect 输出 → Backend 输入上下文
Backend 输出 → Test 输入上下文
Test 输出 → Review 输入上下文
所有输出 → Final Summary 输入上下文
```

本阶段仍然保持 Mock，不做真实多模型推理，不执行 shell，不写 Git，不生成真实代码文件。

## 2. 总目标

实现多 Agent Step 间的上下文传递闭环：

1. 每个 Step 的 `inputContext` 必须包含任务摘要和前序 Step 输出摘要。
2. 每个 Step 的 `outputContent` 必须能被后续 Step 引用。
3. 编排 Run 中可查看完整消息链路。
4. 前端 Timeline 支持查看「输入上下文」和「输出内容」。
5. Final Summary 必须基于所有已完成 Step 输出生成。
6. Logs / Artifacts 中体现消息传递过程。
7. 保持 35A API 兼容，不破坏已有 E2E。

完成后，35B 应从：

```text
多 Agent 顺序执行
```

升级为：

```text
多 Agent 顺序协作 + 上下文传递
```

## 3. 严格边界

必须遵守：

1. 不做真实 LLM 多轮协作。
2. 不并行执行 Agent。
3. 不引入 RabbitMQ / Redis 队列调度。
4. 不执行 shell。
5. 不执行 Git 写操作。
6. 不修改仓库文件。
7. 不做工作流编辑器。
8. 不做 Agent 之间自由聊天。
9. 不破坏 35A 的 API。
10. 不破坏单 Agent `POST /api/tasks/{taskId}/execute`。
11. 不绕过 Task 状态机。
12. 前端保持当前中文暗色科技风 UI。

允许做：

- 修改 `MultiAgentStepEntity` / DTO 增加 message 字段。
- 新增 `multi_agent_message` 表，记录 Step 间消息。
- 或者不新增表，使用 `inputContext` / `outputContent` 实现轻量传递。
- 新增 message DTO / response。
- 增强 `MultiAgentOrchestrationService` 的上下文构造逻辑。
- 增强前端 MultiAgentRunPanel 展示输入 / 输出。

推荐本阶段采用 **新增 message 表**，这样后续 35C / 35D 可以继续扩展。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V16__init_multi_agent_messages.sql
```

如果 V16 已存在，请顺延到下一个版本号。

### 4.1 multi_agent_message

记录 Agent Step 间的消息传递。

```sql
CREATE TABLE IF NOT EXISTS multi_agent_message (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    from_step_id BIGINT NULL,
    to_step_id BIGINT NULL,
    from_agent_id BIGINT NULL,
    to_agent_id BIGINT NULL,
    message_type VARCHAR(64) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    summary TEXT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_multi_agent_message_run_time (run_id, create_time),
    INDEX idx_multi_agent_message_task (task_id),
    INDEX idx_multi_agent_message_from_step (from_step_id),
    INDEX idx_multi_agent_message_to_step (to_step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 消息传递记录';
```

无物理外键，保持当前项目风格。

## 5. 枚举设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentMessageType.java
```

枚举值：

```java
public enum MultiAgentMessageType {
    TASK_CONTEXT,
    STEP_OUTPUT,
    HANDOFF,
    REVIEW_FEEDBACK,
    FINAL_CONTEXT
}
```

说明：

| 类型 | 说明 |
|---|---|
| TASK_CONTEXT | Task 原始上下文传入第一步 |
| STEP_OUTPUT | Step 完成后沉淀的输出消息 |
| HANDOFF | 前一步传给下一步的交接消息 |
| REVIEW_FEEDBACK | Review Agent 产生的反馈 |
| FINAL_CONTEXT | 汇总给 Final Summary 的上下文 |

## 6. 实体 / Mapper / DTO

### 6.1 Entity

新增：

```text
MultiAgentMessageEntity.java
```

要求：

- `@TableName("multi_agent_message")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` createTime
- 不继承 BaseEntity
- 手写 getter/setter

字段：

```java
private Long id;
private Long runId;
private Long projectId;
private Long taskId;
private Long fromStepId;
private Long toStepId;
private Long fromAgentId;
private Long toAgentId;
private String messageType;
private String content;
private String summary;
private LocalDateTime createTime;
```

### 6.2 Mapper

新增：

```text
MultiAgentMessageMapper.java
```

```java
public interface MultiAgentMessageMapper extends BaseMapper<MultiAgentMessageEntity> {
}
```

### 6.3 DTO

新增：

```text
MultiAgentMessageResponse.java
```

字段：

```java
private String id;
private String runId;
private String fromStepId;
private String toStepId;
private String fromAgentId;
private String toAgentId;
private String messageType;
private String content;
private String summary;
private LocalDateTime createTime;
```

修改：

```text
MultiAgentStepResponse.java
MultiAgentRunResponse.java
```

建议增加：

```java
private List<MultiAgentMessageResponse> inputMessages;
private List<MultiAgentMessageResponse> outputMessages;
```

或在 Run Response 顶层增加：

```java
private List<MultiAgentMessageResponse> messages;
```

推荐：**Run 顶层返回 messages，Step 仍保留 inputContext/outputContent**。前端可以按 stepId 归类展示。

## 7. API 设计

### 7.1 查询 Run 消息

新增：

```http
GET /api/multi-agent-runs/{runId}/messages
```

权限：

```text
VIEWER+
```

响应：

```json
[
  {
    "id": "2060000000000000101",
    "runId": "2060000000000000001",
    "fromStepId": null,
    "toStepId": "2060000000000000002",
    "messageType": "TASK_CONTEXT",
    "summary": "任务上下文已传入架构分析步骤",
    "content": "Task title: ...\nTask description: ...",
    "createTime": "2026-05-19T10:00:00.000-07:00"
  }
]
```

### 7.2 查询 Run 详情

已有：

```http
GET /api/multi-agent-runs/{runId}
```

增强响应：

```json
{
  "id": "...",
  "steps": [],
  "messages": []
}
```

保持兼容：已有字段不能删除。

## 8. Message Passing 规则

### 8.1 初始上下文

启动 Run 时创建一条 `TASK_CONTEXT` 消息：

```text
Task → Architect Step
```

内容包含：

- task.title
- task.description
- task.type
- task.priority
- request.instruction
- RAG context summary（如已有）

### 8.2 Step 输出消息

每个已完成 Step 创建一条 `STEP_OUTPUT` 消息：

```text
Step → Run Memory
```

内容为 step.outputContent。

summary 为简短摘要，例如：

```text
架构分析完成：识别了后端接口、权限、测试风险。
```

### 8.3 Handoff 消息

每个 Step 完成后，如果存在下一个可执行 Step，则创建 `HANDOFF` 消息：

```text
Current Step → Next Step
```

内容包含：

```markdown
## 上一步输出摘要

...

## 对下一步的建议

...
```

### 8.4 Review Feedback

Review Step 完成后创建 `REVIEW_FEEDBACK` 消息：

```text
Review Step → Final Summary Step
```

### 8.5 Final Context

Final Summary Step 执行前创建 `FINAL_CONTEXT` 消息：

```text
All Completed Steps → Final Summary Step
```

内容包含所有 completed step 的摘要列表。

## 9. Step inputContext 构造规则

35A 中每个 Step 已有 `inputContext` 字段。

35B 要求 `inputContext` 不再只是静态模板，而是由消息链路构造：

```text
inputContext =
  Task Context
  + Relevant Handoff Messages
  + Previous Step Output Summaries
  + Current Step Instruction
```

建议格式：

```markdown
# 当前步骤上下文

## 任务上下文
...

## 前序 Agent 输出摘要
1. Architect: ...
2. Backend: ...

## 接收到的交接消息
...

## 当前步骤要求
...
```

## 10. Mock 输出增强

Mock 输出应体现“已读取前序上下文”。

示例：

### Backend Step

```markdown
## 后端实现计划

基于 Architect Agent 的架构分析，本步骤建议：

1. ...
2. ...

已消费上游上下文：
- ARCHITECTURE_ANALYSIS
```

### Test Step

```markdown
## 测试计划

基于 Architect 和 Backend 两个步骤的输出，建议覆盖：

...

已消费上游上下文：
- ARCHITECTURE_ANALYSIS
- BACKEND_IMPLEMENTATION_PLAN
```

### Review Step

```markdown
## 审查反馈

基于前序方案，发现以下风险：

...
```

## 11. 前端实现

### 11.1 MultiAgentRunPanel 增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

新增展示：

1. 消息链路视图：
   - Task Context
   - Handoff
   - Step Output
   - Review Feedback
   - Final Context
2. Step 卡片增加：
   - 「输入上下文」折叠区
   - 「输出内容」折叠区
   - 「消息」数量 badge
3. Timeline 上显示箭头 / 连接线：

```text
Architect → Backend → Test → Review → Summary
```

4. Final Summary 区域显示：
   - 已聚合 Step 数量
   - 消息数量
   - skipped step 数量

### 11.2 UI 要求

- 使用当前中文暗色科技风。
- 复用 `StatusPulse`、`GlowButton`、`MarkdownRenderer`。
- 不引入图形画布库。
- 不做拖拽。
- 不做复杂节点编辑。

## 12. 后端测试要求

新增或扩展：

```text
MultiAgentOrchestrationIntegrationTest.java
```

至少新增测试：

1. start run 后生成 `TASK_CONTEXT` 消息。
2. 每个 completed step 生成 `STEP_OUTPUT` 消息。
3. completed step 之间生成 `HANDOFF` 消息。
4. final step 前生成 `FINAL_CONTEXT` 消息。
5. run detail 返回 messages。
6. `GET /api/multi-agent-runs/{runId}/messages` 返回按 createTime ASC 排序。
7. skipped step 不生成 `STEP_OUTPUT`。
8. 后续 step 的 `inputContext` 包含前序 step summary。
9. final summary 包含所有 completed step summary。
10. 非项目成员查询 messages 返回 PROJECT_ACCESS_DENIED。
11. 未登录查询 messages 返回 UNAUTHORIZED。

后端质量门：

```bash
cd backend
mvn test
```

## 13. 前端 E2E 要求

扩展：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

至少覆盖：

1. 启动 multi-agent run。
2. Timeline 显示多个 Step。
3. 消息链路视图可见。
4. 点击 Step 可展开输入上下文。
5. 点击 Step 可展开输出内容。
6. Final Summary 显示聚合信息。
7. 页面无 JS error。

前端质量门：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果需要 E2E 后端：

```bash
bash scripts/start-e2e-backend.sh
```

## 14. 手动验证清单

1. 登录成功。
2. 打开项目。
3. 确认至少启用 Architect / Backend / Test / Review Agent。
4. 创建 Task。
5. 打开 Task Detail。
6. 进入「多智能体」Tab。
7. 启动多智能体编排。
8. Timeline 显示步骤。
9. 打开 Architect Step，能看到 Task Context。
10. 打开 Backend Step，能看到 Architect 输出摘要。
11. 打开 Test Step，能看到 Architect + Backend 输出摘要。
12. 打开 Review Step，能看到前序方案摘要。
13. Final Summary 显示聚合内容。
14. 消息链路视图显示多条消息。
15. Artifacts 中包含更新后的 Summary。

## 15. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35B 完成报告

1. 新增 / 修改文件清单
2. 数据库 Migration 说明
3. Multi-Agent Message 设计说明
4. Message Passing 规则实现说明
5. Step inputContext 构造说明
6. Mock 输出增强说明
7. 后端 API 实现说明
8. 前端消息链路 / 输入输出展示说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. 手动验证结果
12. 已知限制
13. 是否可以进入 Milestone 35C
```

## 16. 已知限制

35B 完成后仍然不包含：

- 真正的 Agent 自由对话。
- 并行消息传递。
- 真实 LLM memory。
- 自动任务拆分。
- 工作流图编辑器。
- 人工审批节点。
- 工具调用。

后续建议：

```text
35C: Multi-Agent Parallel Mock Execution
35D: Workflow Strategy Template
36A: Safe Tool Execution Sandbox
36B: Code Change Proposal Flow
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35B。

文档路径：

```text
docs/milestone-35b-multi-agent-message-passing.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在 Milestone 35A Multi-Agent Mock Orchestration 基础上新增 Agent Message Passing。
3. 不要重写 35A 已完成的 Run / Step / Timeline 能力。
4. 不要破坏已有 `POST /api/tasks/{taskId}/multi-agent-runs`。
5. 不要破坏已有单 Agent `POST /api/tasks/{taskId}/execute`。
6. 不接真实多 Agent 推理。
7. 不执行 shell。
8. 不执行 Git 写操作。
9. 不生成真实代码文件。
10. 不引入队列，不引入工作流引擎。
11. 不更换技术栈，不更换 UI 框架。
12. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
13. 复用现有 ProjectPermissionService 权限模型。
14. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
15. 所有新增 API 的 ID 对外保持 String。
16. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 `V16__init_multi_agent_messages.sql`，如果 V16 已存在则顺延版本号。
2. 新增 `multi_agent_message` 表。
3. 新增 `MultiAgentMessageEntity`。
4. 新增 `MultiAgentMessageMapper`。
5. 新增 `MultiAgentMessageType` 枚举。
6. 新增 `MultiAgentMessageResponse` DTO。
7. 增强 `MultiAgentRunResponse`，返回 messages。
8. 新增 `GET /api/multi-agent-runs/{runId}/messages`。
9. 增强 `GET /api/multi-agent-runs/{runId}`，返回 messages。
10. 启动 run 时生成 `TASK_CONTEXT` 消息。
11. 每个 completed step 生成 `STEP_OUTPUT` 消息。
12. Step 之间生成 `HANDOFF` 消息。
13. Review Step 生成 `REVIEW_FEEDBACK` 消息。
14. Final Summary Step 前生成 `FINAL_CONTEXT` 消息。
15. Step 的 `inputContext` 必须由 Task Context + 前序消息 + 当前步骤要求组合而成。
16. Mock 输出必须体现已消费前序上下文。
17. 前端 `MultiAgentRunPanel.vue` 增加消息链路视图。
18. 前端 Step 卡片增加「输入上下文」和「输出内容」折叠展示。
19. 前端 Final Summary 显示消息数量 / completed step 数 / skipped step 数。
20. 新增或扩展后端集成测试。
21. 扩展前端 E2E 测试。

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
3. Multi-Agent Message 设计说明
4. Message Passing 规则实现说明
5. Step inputContext 构造说明
6. Mock 输出增强说明
7. 后端 API 实现说明
8. 前端消息链路 / 输入输出展示说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. 手动验证结果
12. 已知限制
13. 是否可以进入 Milestone 35C

现在开始实现，不要只给计划。
