# Milestone 35D: Workflow Strategy Template

## 1. 背景

Milestone 35A 已完成 Multi-Agent Mock Orchestration：

- Multi-Agent Run / Step。
- 固定 Mock 编排流程。
- Task Logs / Events / Artifacts / AgentExecution 集成。
- 前端「多智能体」Tab。

Milestone 35B 已完成 Multi-Agent Message Passing：

- `multi_agent_message`。
- Step 间上下文传递。
- Run Detail 返回 messages。
- 前端消息链路展示。

Milestone 35C 已完成 Phase / Lane Parallel Mock Execution：

- `multi_agent_phase`。
- Phase / Lane 分组。
- `PLANNING → IMPLEMENTATION → REVIEW → SUMMARY`。
- Phase 2 支持 Backend / Frontend / Test 并行 Mock 语义。
- 前端 Phase / Lane 视图。

当前 35C 的 Phase 模板仍是硬编码：

```text
PLANNING: Architect
IMPLEMENTATION: Backend + Frontend + Test
REVIEW: Review
SUMMARY: Final Summary
```

Milestone 35D 的目标是把硬编码编排升级为 **Workflow Strategy Template**：

```text
用户启动 Multi-Agent Run 时选择一个 Strategy
后端根据 Strategy 生成不同 Phase / Step / Lane
前端展示 Strategy 信息和模板详情
```

本阶段仍然保持 Mock，不做真实工作流编辑器、不做拖拽画布、不做工具执行。

## 2. 总目标

实现可选择的多智能体工作流策略模板：

1. 后端内置多个 Workflow Strategy。
2. 启动 Multi-Agent Run 时可选择 strategy。
3. 后端根据 strategy 生成不同 Phase / Step / Lane。
4. Run Response 返回 strategyName / strategyDescription / template metadata。
5. 前端启动面板可选择 strategy。
6. 前端 Run Detail 显示当前使用的策略。
7. 测试覆盖不同策略的 Phase / Step 结构。

完成后，平台从：

```text
固定 Phase/Lane 编排
```

升级为：

```text
可选择的 Workflow Strategy 模板编排
```

## 3. 严格边界

必须遵守：

1. 不做工作流拖拽编辑器。
2. 不做自定义节点编辑。
3. 不做人工审批节点。
4. 不做真实工具调用。
5. 不执行 shell。
6. 不执行 Git 写操作。
7. 不修改仓库文件。
8. 不生成真实代码文件。
9. 不引入队列 / worker / 工作流引擎。
10. 不破坏 35A / 35B / 35C API。
11. 不破坏单 Agent `POST /api/tasks/{taskId}/execute`。
12. 不绕过 Task 状态机。
13. 前端保持当前中文暗色科技风 UI。

允许做：

- 新增 strategy enum / DTO。
- 新增 strategy catalog service。
- 新增 strategy list API。
- 扩展 `StartMultiAgentRunRequest`。
- 扩展 `MultiAgentRunResponse`。
- 修改 MultiAgentOrchestrationService 根据 strategy 生成 phases。
- 前端启动 Multi-Agent Run 时提供 strategy 下拉。

本阶段推荐 **代码内置模板**，不新增数据库表。  
原因：35D 目标是模板化，不是后台动态配置。数据库持久化模板留到后续 35F。

## 4. Strategy 设计

### 4.1 内置策略列表

至少实现 4 个策略：

| Strategy Key | 名称 | 说明 |
|---|---|---|
| STANDARD_DELIVERY | 标准交付流程 | 架构 → 后端/前端/测试并行 → 审查 → 总结 |
| BACKEND_FOCUSED | 后端优先流程 | 架构 → 后端/测试并行 → 审查 → 总结 |
| FRONTEND_FOCUSED | 前端优先流程 | 架构 → 前端/测试并行 → 审查 → 总结 |
| REVIEW_ONLY | 审查流程 | 审查 → 总结 |

保留兼容：

| Legacy Strategy | 映射 |
|---|---|
| DEFAULT_MOCK | STANDARD_DELIVERY |
| PHASED_PARALLEL_MOCK | STANDARD_DELIVERY |

### 4.2 Strategy 结构

建议新增内部 record / DTO：

```java
public class WorkflowStrategyTemplate {
    private String strategyKey;
    private String name;
    private String description;
    private List<WorkflowPhaseTemplate> phases;
}
```

```java
public class WorkflowPhaseTemplate {
    private Integer phaseOrder;
    private String phaseKey;
    private String title;
    private List<WorkflowStepTemplate> steps;
}
```

```java
public class WorkflowStepTemplate {
    private Integer stepOrder;
    private String stepType;
    private String agentCode;
    private String laneKey;
    private String title;
}
```

这些类可以是 DTO，也可以是 application 内部类。  
如果需要前端展示，建议 DTO 化。

## 5. 默认 Strategy 模板

### 5.1 STANDARD_DELIVERY

```text
Phase 1 PLANNING
  - ARCHITECTURE_ANALYSIS / architect-agent / architect

Phase 2 IMPLEMENTATION
  - BACKEND_IMPLEMENTATION_PLAN / backend-agent / backend
  - FRONTEND_IMPLEMENTATION_PLAN / frontend-agent / frontend
  - TEST_PLAN / test-agent / test

Phase 3 REVIEW
  - CODE_REVIEW / review-agent / review

Phase 4 SUMMARY
  - FINAL_SUMMARY / architect-agent / summary
```

### 5.2 BACKEND_FOCUSED

```text
Phase 1 PLANNING
  - ARCHITECTURE_ANALYSIS / architect-agent / architect

Phase 2 BACKEND_IMPLEMENTATION
  - BACKEND_IMPLEMENTATION_PLAN / backend-agent / backend
  - TEST_PLAN / test-agent / test

Phase 3 REVIEW
  - CODE_REVIEW / review-agent / review

Phase 4 SUMMARY
  - FINAL_SUMMARY / architect-agent / summary
```

### 5.3 FRONTEND_FOCUSED

```text
Phase 1 PLANNING
  - ARCHITECTURE_ANALYSIS / architect-agent / architect

Phase 2 FRONTEND_IMPLEMENTATION
  - FRONTEND_IMPLEMENTATION_PLAN / frontend-agent / frontend
  - TEST_PLAN / test-agent / test

Phase 3 REVIEW
  - CODE_REVIEW / review-agent / review

Phase 4 SUMMARY
  - FINAL_SUMMARY / architect-agent / summary
```

### 5.4 REVIEW_ONLY

```text
Phase 1 REVIEW
  - CODE_REVIEW / review-agent / review

Phase 2 SUMMARY
  - FINAL_SUMMARY / architect-agent / summary
```

## 6. 后端 API 设计

### 6.1 查询 Strategy 列表

新增：

```http
GET /api/multi-agent-strategies
```

权限：

```text
登录用户
```

响应：

```json
[
  {
    "strategyKey": "STANDARD_DELIVERY",
    "name": "标准交付流程",
    "description": "架构 → 后端/前端/测试并行 → 审查 → 总结",
    "phaseCount": 4,
    "stepCount": 6,
    "phases": [
      {
        "phaseOrder": 1,
        "phaseKey": "PLANNING",
        "title": "架构规划",
        "steps": [
          {
            "stepOrder": 1,
            "stepType": "ARCHITECTURE_ANALYSIS",
            "agentCode": "architect-agent",
            "laneKey": "architect",
            "title": "架构分析"
          }
        ]
      }
    ]
  }
]
```

### 6.2 启动 Multi-Agent Run

已有：

```http
POST /api/tasks/{taskId}/multi-agent-runs
```

请求增强：

```json
{
  "strategy": "BACKEND_FOCUSED",
  "instruction": "重点分析后端接口和测试风险",
  "useRag": true,
  "knowledgeBaseId": "2054487957508165634"
}
```

规则：

- strategy 为空：默认 `STANDARD_DELIVERY`。
- strategy = `DEFAULT_MOCK`：映射到 `STANDARD_DELIVERY`。
- strategy = `PHASED_PARALLEL_MOCK`：映射到 `STANDARD_DELIVERY`。
- strategy 不存在：返回 `BAD_REQUEST`。

### 6.3 Run Detail 响应增强

`MultiAgentRunResponse` 增加：

```java
private String strategyKey;
private String strategyName;
private String strategyDescription;
```

如果当前已有 `strategy` 字段，保留它，并保证：

```text
strategy = strategyKey
```

## 7. 后端实现建议

### 7.1 新增 DTO

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowStrategyResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowPhaseTemplateResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowStepTemplateResponse.java
```

### 7.2 新增 Service

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/WorkflowStrategyCatalogService.java
```

职责：

- 返回全部内置 strategy。
- 根据 strategyKey 解析模板。
- 兼容 legacy strategy。
- 计算 phaseCount / stepCount。

建议方法：

```java
List<WorkflowStrategyResponse> listStrategies();
WorkflowStrategyTemplate resolveTemplate(String strategyKey);
String normalizeStrategyKey(String strategyKey);
```

### 7.3 修改 MultiAgentOrchestrationService

当前 35C 可能有硬编码 PhaseTemplate。

35D 要求：

```text
hardcoded phase template
  → WorkflowStrategyCatalogService.resolveTemplate(request.strategy)
```

然后根据模板生成：

- phase
- step
- lane
- message passing
- phase inputSummary / outputSummary

### 7.4 Controller

新增 endpoint 可以放在：

```text
MultiAgentOrchestrationController.java
```

或新增：

```text
WorkflowStrategyController.java
```

推荐新增 `WorkflowStrategyController.java`，职责更清晰。

## 8. 前端实现

### 8.1 API client

修改：

```text
frontend/src/modules/task/api.ts
```

新增类型：

```ts
export interface WorkflowStepTemplateResponse {
  stepOrder: number
  stepType: string
  agentCode: string
  laneKey: string
  title: string
}

export interface WorkflowPhaseTemplateResponse {
  phaseOrder: number
  phaseKey: string
  title: string
  steps: WorkflowStepTemplateResponse[]
}

export interface WorkflowStrategyResponse {
  strategyKey: string
  name: string
  description: string
  phaseCount: number
  stepCount: number
  phases: WorkflowPhaseTemplateResponse[]
}
```

新增函数：

```ts
export function getMultiAgentStrategies()
```

Start request 支持：

```ts
strategy?: string
```

### 8.2 MultiAgentRunPanel 增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

新增：

1. Strategy 选择器：
   - 标准交付流程
   - 后端优先流程
   - 前端优先流程
   - 审查流程
2. Strategy 预览：
   - phase count
   - step count
   - phase / lane mini preview
3. 启动时传递 selectedStrategy。
4. Run 卡片显示 strategyName。
5. Run Detail 顶部显示 strategyDescription。

### 8.3 UI 要求

- 中文 UI。
- 暗色科技风。
- 复用 StatusPulse / GlowButton / MarkdownRenderer / EmptyState。
- 不做拖拽。
- 不做画布。
- 不做复杂编辑器。

## 9. 策略选择行为

启动前：

```text
用户选择 strategy → 前端展示模板预览 → 点击启动
```

启动后：

```text
Run 使用固定 strategy，不随前端切换而变化
```

如果用户切换 Strategy，只影响下一次启动。

## 10. 错误处理

| 场景 | 行为 |
|---|---|
| strategy 为空 | 使用 STANDARD_DELIVERY |
| strategy = DEFAULT_MOCK | 映射 STANDARD_DELIVERY |
| strategy = PHASED_PARALLEL_MOCK | 映射 STANDARD_DELIVERY |
| strategy 不存在 | BAD_REQUEST |
| strategy 中某 agent 未启用 | 对应 step SKIPPED |
| strategy 所有 step skipped | run 可 FAILED 或 COMPLETED_WITH_SKIPS，本阶段建议 FAILED |

如果不引入 `COMPLETED_WITH_SKIPS` 状态，则使用已有状态：

```text
至少一个 step completed 且无 failed → COMPLETED
全部 skipped → FAILED
```

## 11. 后端测试要求

扩展：

```text
MultiAgentOrchestrationIntegrationTest.java
```

或新增：

```text
WorkflowStrategyIntegrationTest.java
```

至少覆盖：

1. `GET /api/multi-agent-strategies` 返回 4 个 strategy。
2. strategy 列表包含 phaseCount / stepCount。
3. STANDARD_DELIVERY 生成 4 个 phase。
4. BACKEND_FOCUSED 的 IMPLEMENTATION phase 只包含 backend/test。
5. FRONTEND_FOCUSED 的 IMPLEMENTATION phase 只包含 frontend/test。
6. REVIEW_ONLY 只生成 REVIEW / SUMMARY 两个 phase。
7. 空 strategy 默认 STANDARD_DELIVERY。
8. DEFAULT_MOCK 映射 STANDARD_DELIVERY。
9. PHASED_PARALLEL_MOCK 映射 STANDARD_DELIVERY。
10. 无效 strategy 返回 BAD_REQUEST。
11. Run Detail 返回 strategyName / strategyDescription。
12. 不同 strategy 的 phase / step / lane 结构正确。
13. 未登录查询 strategy 返回 UNAUTHORIZED 或按项目现有约定允许登录用户，需与 SecurityConfig 一致。

后端质量门：

```bash
cd backend
mvn test
```

## 12. 前端 E2E 要求

扩展：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

至少覆盖：

1. Multi-Agent Panel 加载 strategy 下拉。
2. 选择 BACKEND_FOCUSED 后展示模板预览。
3. 启动 BACKEND_FOCUSED run 后 Phase 2 只显示 backend/test lane。
4. 选择 REVIEW_ONLY 后启动 run，页面只显示 review/summary phase。
5. Run 卡片显示 strategy name。
6. 页面无 JS error。

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

## 13. 手动验证清单

1. 登录成功。
2. 打开项目。
3. 创建 Task。
4. 打开 Task Detail。
5. 进入「多智能体」Tab。
6. Strategy 下拉显示 4 个策略。
7. 选择「标准交付流程」，预览显示 4 phase / 6 step。
8. 启动后页面显示 Backend / Frontend / Test 三 lane。
9. 创建新 Task。
10. 选择「后端优先流程」，预览显示 backend/test。
11. 启动后 Phase 2 不显示 frontend lane。
12. 创建新 Task。
13. 选择「审查流程」，只显示 Review / Summary。
14. Logs / Artifacts 正常生成。
15. Run Detail 显示 strategy name / description。

## 14. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35D 完成报告

1. 新增 / 修改文件清单
2. Workflow Strategy 设计说明
3. 内置 Strategy 模板说明
4. 后端 API 实现说明
5. Strategy 到 Phase / Step / Lane 的生成规则说明
6. 与 Message Passing / Phase 聚合的集成说明
7. 前端 Strategy 选择器 / 模板预览实现说明
8. 后端测试覆盖说明
9. 前端 typecheck / build / E2E 结果
10. 手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 35E
```

## 15. 已知限制

35D 完成后仍然不包含：

- 自定义工作流编辑。
- 数据库存储工作流模板。
- 拖拽节点。
- 条件分支。
- 人工审批节点。
- 并发 worker。
- 工具调用。
- 真实代码修改。

后续建议：

```text
35E: Human Approval Gate
35F: Persisted Workflow Template Management
36A: Safe Tool Execution Sandbox
36B: Code Change Proposal Flow
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35D。

文档路径：

```text
docs/milestone-35d-workflow-strategy-template.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在 Milestone 35A / 35B / 35C 基础上新增 Workflow Strategy Template。
3. 不要重写 35A / 35B / 35C 已完成的 Run / Step / Message / Phase / Lane 能力。
4. 不要破坏已有 `POST /api/tasks/{taskId}/multi-agent-runs`。
5. 不要破坏已有 run detail / messages / phases API。
6. 不要破坏已有单 Agent `POST /api/tasks/{taskId}/execute`。
7. 不做工作流拖拽编辑器。
8. 不做数据库持久化 workflow template。
9. 不做人工审批节点。
10. 不执行 shell。
11. 不执行 Git 写操作。
12. 不生成真实代码文件。
13. 不引入队列，不引入工作流引擎。
14. 不更换技术栈，不更换 UI 框架。
15. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
16. 复用现有 ProjectPermissionService 权限模型。
17. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
18. 所有新增 API 的 ID 对外保持 String。
19. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 Workflow Strategy DTO：
   - WorkflowStrategyResponse
   - WorkflowPhaseTemplateResponse
   - WorkflowStepTemplateResponse
2. 新增 WorkflowStrategyCatalogService。
3. 内置 4 个 strategy：
   - STANDARD_DELIVERY
   - BACKEND_FOCUSED
   - FRONTEND_FOCUSED
   - REVIEW_ONLY
4. 兼容 legacy strategy：
   - DEFAULT_MOCK → STANDARD_DELIVERY
   - PHASED_PARALLEL_MOCK → STANDARD_DELIVERY
5. 新增 `GET /api/multi-agent-strategies`。
6. 修改 `StartMultiAgentRunRequest`，支持 strategy。
7. 修改 `MultiAgentRunResponse`，返回 strategyKey / strategyName / strategyDescription。
8. 修改 MultiAgentOrchestrationService，根据 strategy template 生成 phase / step / lane。
9. 无效 strategy 返回 BAD_REQUEST。
10. 前端 task/api.ts 增加 strategy 类型和 getMultiAgentStrategies()。
11. 前端 MultiAgentRunPanel.vue 增加 strategy 下拉。
12. 前端显示 strategy 模板预览。
13. 启动 run 时传递 selectedStrategy。
14. Run 卡片 / detail 显示 strategy name / description。
15. 扩展后端集成测试。
16. 扩展前端 E2E 测试。

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
2. Workflow Strategy 设计说明
3. 内置 Strategy 模板说明
4. 后端 API 实现说明
5. Strategy 到 Phase / Step / Lane 的生成规则说明
6. 与 Message Passing / Phase 聚合的集成说明
7. 前端 Strategy 选择器 / 模板预览实现说明
8. 后端测试覆盖说明
9. 前端 typecheck / build / E2E 结果
10. 手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 35E

现在开始实现，不要只给计划。
