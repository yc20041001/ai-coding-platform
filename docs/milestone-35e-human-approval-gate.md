# Milestone 35E: Human Approval Gate

## 1. 背景

Milestone 35A-35D 已完成多智能体编排主链路：

- 35A: Multi-Agent Mock Orchestration
- 35B: Multi-Agent Message Passing
- 35C: Phase / Lane Parallel Mock Execution
- 35D: Workflow Strategy Template
- 35D-Fix: 全量质量门收口

当前多智能体流程可以根据 strategy 自动执行完整 Run：

```text
Task
  → Strategy Template
  → Phase / Lane
  → Step
  → Message Passing
  → Final Summary
```

但流程仍然是自动跑到底，没有人工确认环节。

后续如果要进入真实工具执行、代码变更提案、PR 创建等高风险能力，必须先建立人工审批闸门。

Milestone 35E 的目标是新增 **Human Approval Gate**：

```text
Phase 1 Planning
Phase 2 Implementation Plan
Approval Gate: 等待用户审批
Phase 3 Review
Phase 4 Summary
```

本阶段仍然保持 Mock，不执行真实代码，不做 Git 写操作，不调用 shell。

## 2. 总目标

实现多智能体编排中的人工审批节点：

1. Workflow Strategy 可以定义 approval gate。
2. Run 执行到 gate 时暂停，状态变为 `WAITING_APPROVAL`。
3. 前端显示审批卡片。
4. 用户可以 Approve 或 Reject。
5. Approve 后继续执行后续 Phase。
6. Reject 后 Run 进入 `CANCELED` 或 `FAILED`，Task 进入对应终态。
7. 审批动作写入 audit / logs / messages。
8. 全流程可测试、可回归、可演示。

完成后，平台从：

```text
自动 Mock 多智能体执行
```

升级为：

```text
带人工审批闸门的 Mock 多智能体工作流
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行 Git 写操作。
3. 不修改仓库文件。
4. 不生成真实代码文件。
5. 不调用外部工作流引擎。
6. 不引入消息队列 worker。
7. 不做多人会签。
8. 不做复杂审批流配置。
9. 不做审批超时自动处理。
10. 不做审批通知。
11. 不破坏 35A / 35B / 35C / 35D API。
12. 不破坏单 Agent 执行接口。
13. 不绕过 Task 状态机。
14. 前端保持当前中文暗色科技风 UI。

允许做：

- 新增 approval gate 表。
- 新增 Run / Phase 状态。
- 扩展 strategy template 标记 approval gate。
- 新增 approve / reject API。
- 扩展 MultiAgentRunPanel 显示审批卡片。
- 写 Task Log / Message / Audit。
- 将原本一次性执行到底的 Run 拆成两段执行：
  - start run → 执行到 gate → WAITING_APPROVAL
  - approve → 继续后续 phase → COMPLETED

## 4. 状态设计

### 4.1 Run 状态扩展

修改：

```text
MultiAgentRunStatus.java
```

新增：

```java
WAITING_APPROVAL
```

状态流转：

```text
PENDING → RUNNING → WAITING_APPROVAL → RUNNING → COMPLETED
PENDING → RUNNING → WAITING_APPROVAL → CANCELED
PENDING → RUNNING → FAILED
```

### 4.2 Phase 状态扩展

修改：

```text
MultiAgentPhaseStatus.java
```

新增：

```java
WAITING_APPROVAL
```

规则：

- 如果 approval gate 位于某个 phase 后：
  - 前置 phase 完成。
  - gate phase 或 virtual gate 状态为 `WAITING_APPROVAL`。
- Approve 后后续 phase 继续执行。
- Reject 后后续 phase 标记 `SKIPPED`。

### 4.3 Approval 状态

新增：

```text
MultiAgentApprovalStatus.java
```

枚举：

```java
public enum MultiAgentApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}
```

本阶段不实现 EXPIRED 自动过期，只预留。

## 5. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V18__init_multi_agent_approval_gates.sql
```

如果 V18 已存在，请顺延到下一个版本号。

### 5.1 multi_agent_approval_gate

```sql
CREATE TABLE IF NOT EXISTS multi_agent_approval_gate (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    phase_id BIGINT NULL,
    gate_key VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by BIGINT NULL,
    decided_by BIGINT NULL,
    decision_comment TEXT NULL,
    requested_at DATETIME NULL,
    decided_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_multi_agent_approval_run (run_id),
    INDEX idx_multi_agent_approval_task (task_id),
    INDEX idx_multi_agent_approval_project_status (project_id, status),
    INDEX idx_multi_agent_approval_decider (decided_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 人工审批闸门';
```

无物理外键，保持当前项目风格。

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
MultiAgentApprovalGateEntity.java
```

要求：

- `@TableName("multi_agent_approval_gate")`
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
private Long phaseId;
private String gateKey;
private String title;
private String description;
private String status;
private Long requestedBy;
private Long decidedBy;
private String decisionComment;
private LocalDateTime requestedAt;
private LocalDateTime decidedAt;
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

### 6.2 Mapper

新增：

```text
MultiAgentApprovalGateMapper.java
```

```java
public interface MultiAgentApprovalGateMapper extends BaseMapper<MultiAgentApprovalGateEntity> {
}
```

### 6.3 DTO

新增：

```text
MultiAgentApprovalGateResponse.java
MultiAgentApprovalDecisionRequest.java
```

`MultiAgentApprovalGateResponse` 字段：

```java
private String id;
private String runId;
private String phaseId;
private String gateKey;
private String title;
private String description;
private String status;
private String requestedBy;
private String decidedBy;
private String decisionComment;
private LocalDateTime requestedAt;
private LocalDateTime decidedAt;
```

`MultiAgentApprovalDecisionRequest` 字段：

```java
private String comment;
```

修改：

```text
MultiAgentRunResponse.java
```

增加：

```java
private List<MultiAgentApprovalGateResponse> approvalGates;
private MultiAgentApprovalGateResponse pendingApprovalGate;
```

## 7. Strategy Template 扩展

### 7.1 Approval Gate 定义

扩展 `WorkflowStrategyTemplate`：

```java
private List<WorkflowApprovalGateTemplate> approvalGates;
```

新增：

```java
public class WorkflowApprovalGateTemplate {
    private String gateKey;
    private String title;
    private String description;
    private Integer afterPhaseOrder;
}
```

或作为内部 record。

### 7.2 默认 Gate 规则

本阶段至少给这些 strategy 加 gate：

| Strategy | Gate |
|---|---|
| STANDARD_DELIVERY | Phase 2 后等待审批 |
| BACKEND_FOCUSED | Phase 2 后等待审批 |
| FRONTEND_FOCUSED | Phase 2 后等待审批 |
| REVIEW_ONLY | 不需要 gate，直接执行 |

Gate 示例：

```json
{
  "gateKey": "IMPLEMENTATION_PLAN_APPROVAL",
  "title": "实施方案审批",
  "description": "请确认多智能体生成的实施方案是否可以进入审查与总结阶段。",
  "afterPhaseOrder": 2
}
```

## 8. API 设计

### 8.1 查询 Run 审批 Gate

新增：

```http
GET /api/multi-agent-runs/{runId}/approval-gates
```

权限：

```text
VIEWER+
```

### 8.2 Approve

新增：

```http
POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/approve
```

权限：

```text
MAINTAINER+
```

请求：

```json
{
  "comment": "方案可进入下一阶段。"
}
```

行为：

1. 校验 run 属于项目。
2. 校验用户 MAINTAINER+。
3. 校验 run.status = `WAITING_APPROVAL`。
4. 校验 gate.status = `PENDING`。
5. gate.status = `APPROVED`。
6. 写 decision 信息。
7. run.status = `RUNNING`。
8. 继续执行后续 Phase。
9. 最终 run.status = `COMPLETED`。
10. task.status = `COMPLETED`。

### 8.3 Reject

新增：

```http
POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/reject
```

权限：

```text
MAINTAINER+
```

请求：

```json
{
  "comment": "方案需要重新调整，暂不进入后续阶段。"
}
```

行为：

1. 校验 run.status = `WAITING_APPROVAL`。
2. 校验 gate.status = `PENDING`。
3. gate.status = `REJECTED`。
4. run.status = `CANCELED`。
5. 后续 phase 标记 `SKIPPED`。
6. task.status = `CANCELED` 或 `FAILED`。

推荐本阶段：`task.status = CANCELED`。

## 9. 执行流程设计

### 9.1 Start Run

启动 run 后：

```text
Phase 1 执行
Phase 2 执行
创建 approval gate
Run WAITING_APPROVAL
Task RUNNING
返回 Run Response
```

注意：不继续执行 Phase 3 / Phase 4。

### 9.2 Approve

Approve 后：

```text
Gate APPROVED
Run RUNNING
Phase 3 执行
Phase 4 执行
Run COMPLETED
Task COMPLETED
生成 final artifact
```

### 9.3 Reject

Reject 后：

```text
Gate REJECTED
Run CANCELED
后续 Phase SKIPPED
Task CANCELED
写日志和消息
```

## 10. Logs / Messages / Audit

### 10.1 Task Logs

新增日志事件建议：

```text
MULTI_AGENT_APPROVAL_REQUESTED
MULTI_AGENT_APPROVAL_APPROVED
MULTI_AGENT_APPROVAL_REJECTED
MULTI_AGENT_RESUMED_AFTER_APPROVAL
```

如果 TaskLog enum 没有这些类型，可以使用现有 string eventType 风格或最接近类型。

### 10.2 Multi-Agent Messages

新增 message type 可选：

```text
APPROVAL_REQUEST
APPROVAL_DECISION
```

如果不想扩展 enum，也可用 `HANDOFF` / `FINAL_CONTEXT`，但推荐扩展：

```java
APPROVAL_REQUEST,
APPROVAL_DECISION
```

### 10.3 Audit

审批动作建议写入 AuditLog：

```text
MULTI_AGENT_APPROVE
MULTI_AGENT_REJECT
```

如果 AuditActionType 当前不含这些值，可以新增。

审计内容：

- runId
- gateId
- taskId
- projectId
- decision
- userId
- comment

## 11. 前端实现

### 11.1 MultiAgentRunPanel 增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

新增：

1. Pending Approval 卡片。
2. Gate 标题和说明。
3. 审批意见输入框。
4. Approve 按钮。
5. Reject 按钮。
6. 审批后自动刷新 run detail。

### 11.2 UI 状态

Run 状态 `WAITING_APPROVAL` 时：

```text
显示醒目的待审批状态
禁用再次启动 run
显示 Phase 1 / Phase 2 已完成
显示 Phase 3 / Phase 4 待执行
显示审批卡片
```

Approve 后：

```text
审批卡片显示 APPROVED
Run 继续执行并最终 COMPLETED
Phase 3 / Phase 4 显示 COMPLETED
Final Summary 可见
```

Reject 后：

```text
审批卡片显示 REJECTED
Run 显示 CANCELED
后续 Phase 显示 SKIPPED
Final Summary 不生成或显示审批拒绝说明
```

### 11.3 API Client

修改：

```text
frontend/src/modules/task/api.ts
```

新增类型：

```ts
export interface MultiAgentApprovalGateResponse { ... }
export interface MultiAgentApprovalDecisionRequest { comment?: string }
```

新增函数：

```ts
export function getMultiAgentApprovalGates(runId: string)
export function approveMultiAgentGate(runId: string, gateId: string, payload: MultiAgentApprovalDecisionRequest)
export function rejectMultiAgentGate(runId: string, gateId: string, payload: MultiAgentApprovalDecisionRequest)
```

Run response 类型增加：

```ts
approvalGates?: MultiAgentApprovalGateResponse[]
pendingApprovalGate?: MultiAgentApprovalGateResponse | null
```

## 12. 权限设计

| API | 权限 |
|---|---|
| GET approval gates | VIEWER+ |
| POST approve | MAINTAINER+ |
| POST reject | MAINTAINER+ |

权限路径：

```text
runId → run.projectId → ProjectPermissionService.checkProjectRole(...)
```

审批不允许普通 VIEWER / DEVELOPER 执行。

## 13. 后端测试要求

扩展：

```text
MultiAgentOrchestrationIntegrationTest.java
```

或新增：

```text
MultiAgentApprovalGateIntegrationTest.java
```

至少覆盖：

1. STANDARD_DELIVERY run 启动后进入 WAITING_APPROVAL。
2. run response 包含 pendingApprovalGate。
3. approval gate 状态为 PENDING。
4. Phase 1 / Phase 2 completed，Phase 3 / Phase 4 未执行或 pending/skipped。
5. VIEWER 可查询 approval gates。
6. DEVELOPER 不可 approve。
7. MAINTAINER 可 approve。
8. approve 后 run 继续到 COMPLETED。
9. approve 后 task 为 COMPLETED。
10. approve 后 gate 状态 APPROVED。
11. reject 后 run 为 CANCELED。
12. reject 后 task 为 CANCELED。
13. reject 后 gate 状态 REJECTED。
14. 重复 approve 返回 CONFLICT。
15. 非 WAITING_APPROVAL run approve 返回 CONFLICT。
16. REVIEW_ONLY strategy 不创建 approval gate，直接 COMPLETED。
17. approval decision 写入 task logs。
18. approval decision 写入 messages。
19. 非项目成员查询 gate 返回 PROJECT_ACCESS_DENIED。
20. 未登录 approve 返回 UNAUTHORIZED。

后端质量门：

```bash
cd backend
mvn test
```

## 14. 前端 E2E 要求

扩展：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
```

至少新增：

1. 启动 STANDARD_DELIVERY run 后显示待审批卡片。
2. 点击 Approve 后 run 最终 COMPLETED。
3. 点击 Reject 后 run CANCELED。
4. REVIEW_ONLY run 不显示审批卡片并直接完成。
5. 审批意见输入后显示在 gate 或 message 中。
6. 非 MAINTAINER 用户不显示或无法点击审批按钮。
7. 页面无 JS error。

前端质量门：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如需要 E2E 后端：

```bash
bash scripts/start-e2e-backend.sh
```

## 15. 手动验证清单

1. 登录 admin。
2. 打开项目。
3. 创建 Task。
4. 打开 Task Detail。
5. 进入「多智能体」Tab。
6. 选择 STANDARD_DELIVERY。
7. 启动 run。
8. 确认 Run 状态为 WAITING_APPROVAL。
9. 确认页面显示审批卡片。
10. 输入审批意见。
11. 点击 Approve。
12. 确认后续 Phase 执行。
13. 确认 Run COMPLETED。
14. 新建 Task。
15. 再次启动 STANDARD_DELIVERY。
16. 点击 Reject。
17. 确认 Run CANCELED。
18. 选择 REVIEW_ONLY。
19. 启动后确认无需审批直接 COMPLETED。

## 16. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35E 完成报告

1. 新增 / 修改文件清单
2. Approval Gate 数据库设计说明
3. Run / Phase / Approval 状态流转说明
4. Strategy Template 审批节点扩展说明
5. 后端 API 实现说明
6. Approve / Reject 执行流程说明
7. Logs / Messages / Audit 集成说明
8. 前端审批卡片实现说明
9. 权限控制说明
10. 后端测试覆盖说明
11. 前端 typecheck / build / E2E 结果
12. 手动验证结果
13. 已知限制
14. 是否可以进入 Milestone 35F
```

## 17. 已知限制

35E 完成后仍不包含：

- 多人会签。
- 审批超时。
- 审批通知。
- 审批规则配置。
- 工作流编辑器。
- 真实工具执行。
- 真实代码变更。

后续建议：

```text
35F: Persisted Workflow Template Management
36A: Safe Tool Execution Sandbox
36B: Code Change Proposal Flow
36C: Human Approved Git Patch Proposal
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35E。

文档路径：

```text
docs/milestone-35e-human-approval-gate.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在 Milestone 35A / 35B / 35C / 35D 基础上新增 Human Approval Gate。
3. 不要重写已有 Run / Step / Message / Phase / Lane / Strategy 能力。
4. 不要破坏已有 multi-agent run / detail / messages / phases / strategies API。
5. 不要破坏已有单 Agent `POST /api/tasks/{taskId}/execute`。
6. 不执行 shell。
7. 不执行 Git 写操作。
8. 不生成真实代码文件。
9. 不引入队列，不引入工作流引擎。
10. 不做多人会签。
11. 不做审批通知。
12. 不做审批超时自动处理。
13. 不更换技术栈，不更换 UI 框架。
14. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
15. 复用现有 ProjectPermissionService 权限模型。
16. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
17. 所有新增 API 的 ID 对外保持 String。
18. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 `V18__init_multi_agent_approval_gates.sql`，如果 V18 已存在则顺延版本号。
2. 新增 `multi_agent_approval_gate` 表。
3. 新增 `MultiAgentApprovalStatus` 枚举。
4. 修改 `MultiAgentRunStatus`，新增 `WAITING_APPROVAL`。
5. 修改 `MultiAgentPhaseStatus`，新增 `WAITING_APPROVAL`。
6. 新增 `MultiAgentApprovalGateEntity`。
7. 新增 `MultiAgentApprovalGateMapper`。
8. 新增 `MultiAgentApprovalGateResponse`。
9. 新增 `MultiAgentApprovalDecisionRequest`。
10. 修改 `MultiAgentRunResponse`，返回 approvalGates / pendingApprovalGate。
11. 扩展 Workflow Strategy Template，支持 approval gate 定义。
12. STANDARD_DELIVERY / BACKEND_FOCUSED / FRONTEND_FOCUSED 在 Phase 2 后创建 approval gate。
13. REVIEW_ONLY 不创建 approval gate，直接完成。
14. 新增 `GET /api/multi-agent-runs/{runId}/approval-gates`。
15. 新增 `POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/approve`。
16. 新增 `POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/reject`。
17. start run 时执行到 approval gate 后暂停，run.status = WAITING_APPROVAL。
18. approve 后继续执行后续 Phase，最终 COMPLETED。
19. reject 后 run CANCELED，task CANCELED，后续 Phase SKIPPED。
20. 写 Task Logs。
21. 写 Multi-Agent Messages。
22. 写 Audit Log。
23. 前端 task/api.ts 增加 approval gate 类型和 API 函数。
24. 前端 MultiAgentRunPanel.vue 增加审批卡片。
25. 前端支持 Approve / Reject 和审批意见输入。
26. 扩展后端集成测试。
27. 扩展前端 E2E 测试。

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
2. Approval Gate 数据库设计说明
3. Run / Phase / Approval 状态流转说明
4. Strategy Template 审批节点扩展说明
5. 后端 API 实现说明
6. Approve / Reject 执行流程说明
7. Logs / Messages / Audit 集成说明
8. 前端审批卡片实现说明
9. 权限控制说明
10. 后端测试覆盖说明
11. 前端 typecheck / build / E2E 结果
12. 手动验证结果
13. 已知限制
14. 是否可以进入 Milestone 35F

现在开始实现，不要只给计划。
