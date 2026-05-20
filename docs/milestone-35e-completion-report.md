# Milestone 35E: Human Approval Gate — 完成报告

## 1. 新增 / 修改文件清单

### 新增文件 (40 files)

**Backend — Domain / DTO / Mapper / Service / Controller (30 files)**

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentApprovalGateEntity.java` | 审批闸门实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentApprovalStatus.java` | 审批状态枚举 (PENDING/APPROVED/REJECTED) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentRunEntity.java` | 多智能体运行实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentRunStatus.java` | 运行状态枚举 (含 WAITING_APPROVAL) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentPhaseEntity.java` | 阶段实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentPhaseStatus.java` | 阶段状态枚举 (含 WAITING_APPROVAL) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentPhaseKey.java` | 阶段键枚举 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentStepEntity.java` | 步骤实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentStepStatus.java` | 步骤状态枚举 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentStepType.java` | 步骤类型枚举 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentMessageEntity.java` | 消息实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/MultiAgentMessageType.java` | 消息类型枚举 (含 APPROVAL_REQUEST/APPROVAL_DECISION) |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentApprovalGateResponse.java` | 审批闸门响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentApprovalDecisionRequest.java` | 审批决策请求 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentRunResponse.java` | 运行响应 DTO (含 approvalGates/pendingApprovalGate) |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentPhaseResponse.java` | 阶段响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentStepResponse.java` | 步骤响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiAgentMessageResponse.java` | 消息响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/StartMultiAgentRunRequest.java` | 启动运行请求 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowStrategyResponse.java` | 策略响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowPhaseTemplateResponse.java` | 策略阶段模板响应 |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowStepTemplateResponse.java` | 策略步骤模板响应 |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/MultiAgentApprovalGateMapper.java` | 审批闸门 MyBatis Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/MultiAgentRunMapper.java` | 运行 Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/MultiAgentPhaseMapper.java` | 阶段 Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/MultiAgentStepMapper.java` | 步骤 Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/MultiAgentMessageMapper.java` | 消息 Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/MultiAgentOrchestrationService.java` | 多智能体编排核心服务 (~700+ 行) |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/WorkflowStrategyCatalogService.java` | 策略目录服务 |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/MultiAgentOrchestrationController.java` | 编排 API 控制器 |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/WorkflowStrategyController.java` | 策略 API 控制器 |

**Backend — Test (1 file)**

| `backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java` | 集成测试 (1349 行，~40 tests) |

**Frontend (2 files)**

| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | 多智能体运行面板 (含审批卡片) |
| `frontend/e2e/multi-agent-orchestration.spec.ts` | E2E 测试 (568 行，20 tests) |

**Docs (6 files)**

| `docs/milestone-35a-*.md` ~ `docs/milestone-35e-*.md` | 里程碑设计文档 |

### 修改文件 (12 files)

| 文件 | 变更说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java` | Agent 配置增强 |
| `backend/src/main/java/com/aicoding/platform/audit/domain/AuditActionType.java` | 新增 MULTI_AGENT_APPROVE/MULTI_AGENT_REJECT |
| `backend/src/main/java/com/aicoding/platform/modelgateway/config/ModelGatewayProperties.java` | 配置扩展 |
| `backend/src/main/resources/application.yml` | 配置更新 |
| `backend/src/test/resources/schema.sql` | 新增 V14-V18 表定义 (multi_agent_run/step/phase/message/approval_gate) |
| `backend/src/test/java/com/aicoding/platform/agent/AgentProjectConfigIntegrationTest.java` | 测试更新 |
| `backend/src/test/java/com/aicoding/platform/support/TestDataFactory.java` | 测试数据工厂更新 |
| `backend/src/test/java/com/aicoding/platform/task/TaskStateMachineTest.java` | 任务状态机测试更新 |
| `frontend/src/modules/task/api.ts` | 新增审批闸门 API 类型和函数 |
| `frontend/src/modules/task/pages/TaskDetailPage.vue` | 任务详情页增强 |
| `frontend/e2e/agent-version.spec.ts` | E2E 测试更新 |
| `frontend/vite.config.ts` | Vite 配置更新 |

---

## 2. Approval Gate 数据库设计说明

**表**: `multi_agent_approval_gate`

```sql
CREATE TABLE IF NOT EXISTS multi_agent_approval_gate (
  id BIGINT NOT NULL AUTO_INCREMENT,
  run_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  phase_id BIGINT NULL,
  gate_key VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  requested_by VARCHAR(64) NULL,
  decided_by VARCHAR(64) NULL,
  decision_comment TEXT NULL,
  requested_at DATETIME NULL,
  decided_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_approval_gate_run (run_id),
  INDEX idx_approval_gate_project (project_id),
  INDEX idx_approval_gate_task (task_id),
  INDEX idx_approval_gate_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**索引设计**: 4 个索引覆盖常用查询路径 — 按 run 查询闸门、按 project 统计、按 task 关联、按 status 筛选待审批。

---

## 3. Run / Phase / Approval 状态流转说明

```
                    startRun()
                        │
          ┌─────────────┴─────────────┐
          │                           │
     有 Gate 策略              无 Gate 策略
   (STANDARD_DELIVERY,        (REVIEW_ONLY)
    BACKEND_FOCUSED,
    FRONTEND_FOCUSED)               │
          │                           │
    Phase 1 执行               Phase 1 执行
    Phase 2 执行                Phase 2 执行
          │                           │
    创建 Approval Gate           Phase 3 执行
    Run → WAITING_APPROVAL      Phase 4 执行
    Task → RUNNING              Run → COMPLETED
          │                     Task → COMPLETED
          │
    ┌─────┴─────┐
    │           │
  Approve     Reject
    │           │
  Gate →      Gate →
  APPROVED    REJECTED
    │           │
  Run →       Run → CANCELED
  RUNNING     Task → CANCELED
    │         后续 Phase →
  Phase 3     SKIPPED
  Phase 4
    │
  Run → COMPLETED
  Task → COMPLETED
```

**状态枚举**:

- `MultiAgentRunStatus`: PENDING → RUNNING → WAITING_APPROVAL → RUNNING → COMPLETED / CANCELED / FAILED
- `MultiAgentApprovalStatus`: PENDING → APPROVED / REJECTED
- `MultiAgentPhaseStatus`: PENDING → RUNNING → WAITING_APPROVAL / COMPLETED / SKIPPED

---

## 4. Strategy Template 审批节点扩展说明

`WorkflowStrategyTemplate` 新增 `approvalGates` 列表，每个 Gate 定义:

```java
public class WorkflowApprovalGateTemplate {
    private String gateKey;       // 如 "IMPLEMENTATION_PLAN_APPROVAL"
    private String title;         // 如 "实施方案审批"
    private String description;   // 审批说明
    private Integer afterPhaseOrder;  // 在哪个 Phase 之后触发 (2 = Phase 2 后)
}
```

**策略 Gate 配置**:

| Strategy | Gate Key | afterPhaseOrder | 说明 |
|---|---|---|---|
| STANDARD_DELIVERY | IMPLEMENTATION_PLAN_APPROVAL | 2 | Phase 2 后等待审批 |
| BACKEND_FOCUSED | IMPLEMENTATION_PLAN_APPROVAL | 2 | Phase 2 后等待审批 |
| FRONTEND_FOCUSED | IMPLEMENTATION_PLAN_APPROVAL | 2 | Phase 2 后等待审批 |
| REVIEW_ONLY | (无) | - | 直接完成，不创建 Gate |

---

## 5. 后端 API 实现说明

### 新增 3 个 API 端点

**GET /api/multi-agent-runs/{runId}/approval-gates**
- 权限: VIEWER+
- 返回: `ApiResponse<List<MultiAgentApprovalGateResponse>>`
- 查询指定 run 的所有审批闸门

**POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/approve**
- 权限: MAINTAINER+
- 请求体: `MultiAgentApprovalDecisionRequest { comment?: string }`
- 返回: `ApiResponse<MultiAgentRunResponse>`
- 行为: gate → APPROVED, run → RUNNING, 执行后续 Phase, run → COMPLETED, task → COMPLETED

**POST /api/multi-agent-runs/{runId}/approval-gates/{gateId}/reject**
- 权限: MAINTAINER+
- 请求体: `MultiAgentApprovalDecisionRequest { comment?: string }`
- 返回: `ApiResponse<MultiAgentRunResponse>`
- 行为: gate → REJECTED, run → CANCELED, 后续 Phase → SKIPPED, task → CANCELED

### 错误码

- `MULTI_AGENT_RUN_NOT_WAITING_APPROVAL`: run 不在等待审批状态 (approve/reject 时校验)
- `MULTI_AGENT_GATE_NOT_PENDING`: gate 不在 PENDING 状态 (防重复审批)

---

## 6. Approve / Reject 执行流程说明

### Approve 流程 (`MultiAgentOrchestrationService.approveGate()`)

1. 校验 run.status == WAITING_APPROVAL → 否则抛 BizException
2. 校验 gate.status == PENDING → 否则抛 BizException
3. 记录 decision: decidedBy = currentUsername, decidedAt = now, decisionComment = request.comment
4. gate.status → APPROVED, 更新 DB
5. 创建 APPROVAL_DECISION 消息
6. 写 Audit Log (MULTI_AGENT_APPROVE)
7. run.status → RUNNING
8. 按顺序执行后续 Phase (Phase 3, Phase 4...)
9. `completeRun()`: 生成 final artifact, 写事件日志, run → COMPLETED, task → COMPLETED

### Reject 流程 (`MultiAgentOrchestrationService.rejectGate()`)

1. 校验 run.status == WAITING_APPROVAL → 否则抛 BizException
2. 校验 gate.status == PENDING → 否则抛 BizException
3. 记录 decision: decidedBy, decidedAt, decisionComment
4. gate.status → REJECTED, 更新 DB
5. 创建 APPROVAL_DECISION 消息
6. 写 Audit Log (MULTI_AGENT_REJECT)
7. run.status → CANCELED
8. 后续 Phase → SKIPPED
9. task.status → CANCELED

---

## 7. Logs / Messages / Audit 集成说明

### Task Logs
新增日志事件 (写入 `task_log` 表):
- `MULTI_AGENT_APPROVAL_REQUESTED` — 审批闸门创建时
- `MULTI_AGENT_APPROVAL_APPROVED` — 审批通过时
- `MULTI_AGENT_APPROVAL_REJECTED` — 审批拒绝时
- `MULTI_AGENT_RESUMED_AFTER_APPROVAL` — 审批后恢复执行时

### Multi-Agent Messages
新增消息类型:
- `APPROVAL_REQUEST` — 审批请求消息 (from system to user)
- `APPROVAL_DECISION` — 审批决策消息 (from user to system)

### Audit Log
新增审计动作类型 (`AuditActionType`):
- `MULTI_AGENT_APPROVE` — 审批通过
- `MULTI_AGENT_REJECT` — 审批拒绝

每条审计记录包含: runId, gateId, taskId, projectId, decision, userId, comment.

---

## 8. 前端审批卡片实现说明

### MultiAgentRunPanel.vue 增强

**新增状态**:
- `approvalComment`: ref('') — 审批意见输入
- `approving`: ref(false) — 审批中 loading
- `rejecting`: ref(false) — 拒绝中 loading
- `pendingApprovalGate`: computed — 当前待审批的 Gate

**新增方法**:
- `handleApprove()`: 调用 `approveMultiAgentGate()`, 成功后刷新 run 状态
- `handleReject()`: 调用 `rejectMultiAgentGate()`, 成功后刷新 run 状态

**新增模板**:
- `.mar-approval-gate` — 审批卡片 (黄色边框高亮 + 警告样式)
  - Gate 标题和说明
  - 审批意见输入框 (`data-testid="approval-comment-input"`)
  - Approve 按钮 (`data-testid="btn-approve-gate"`)
  - Reject 按钮 (`data-testid="btn-reject-gate"`)
  - 绿色脉冲动画提示
- `.mar-approval-gates-history` — 已处理审批记录列表
  - 审批状态标签 (APPROVED 绿色 / REJECTED 红色)
  - 审批人和审批意见

**状态显示更新**:
- `statusTone()` / `statusText()` 新增 `WAITING_APPROVAL: 'warning'` / `'等待审批'`
- `messageTypeTone()` / `messageTypeText()` 新增 `APPROVAL_REQUEST` / `APPROVAL_DECISION`

### api.ts 增强
- 新增类型: `MultiAgentApprovalGateResponse`, `MultiAgentApprovalDecisionRequest`
- `MultiAgentRunResponse` 新增字段: `approvalGates?`, `pendingApprovalGate?`
- 新增函数: `getMultiAgentApprovalGates()`, `approveMultiAgentGate()`, `rejectMultiAgentGate()`

---

## 9. 权限控制说明

复用现有 `ProjectPermissionService` 权限模型:

- **GET /approval-gates**: `VIEWER+` — 项目查看者及以上可查看审批状态
- **POST /approve, /reject**: `MAINTAINER+` — 需要项目维护者权限才能做出审批决策
- **Controller 层校验**: `projectPermissionService.checkProjectMembership(projectId, VIEWER+)` / `checkProjectMembership(projectId, MAINTAINER+)`
- **Service 层二次校验**: `approveGate()` / `rejectGate()` 内部校验 run.status 和 gate.status，防并发审批

---

## 10. 后端测试覆盖说明

### 集成测试: `MultiAgentOrchestrationIntegrationTest.java` (1349 行)

**原有测试**: 约 29 个测试，全部更新以适配 WAITING_APPROVAL 行为:
- COMPLETED 期望 → WAITING_APPROVAL 期望
- 6 steps → 4 steps (Phase 1 + Phase 2 执行，Phase 3 + Phase 4 等待审批后执行)
- finalSummary 为 null/empty
- task 状态保持 RUNNING

**新增 11 个审批闸门测试**:

| 测试方法 | 覆盖场景 |
|---|---|
| `shouldRunContainPendingApprovalGate` | STANDARD_DELIVERY run 包含 pending gate |
| `shouldGetApprovalGatesEndpoint` | GET /approval-gates 返回闸门列表 |
| `shouldApproveGateAndContinueToCompleted` | approve → 执行后续 Phase → COMPLETED |
| `shouldRejectGateAndCancelRun` | reject → CANCELED |
| `shouldRejectDuplicateApprove` | 重复 approve 返回错误 |
| `shouldRejectApproveOnNonWaitingRun` | 对非 WAITING_APPROVAL run 的 approve 拒绝 |
| `shouldReviewOnlyStrategyCompleteWithoutGate` | REVIEW_ONLY 无 Gate 直接完成 |
| `shouldApprovalDecisionWriteMessagesAndLogs` | approve 产生消息和日志 |
| `shouldRejectGateWriteMessagesAndLogs` | reject 产生消息和日志 |
| `shouldUnauthenticatedRejectApprove` | 未认证用户无法审批 |
| `shouldApprovalGateContainApprovalRequestMessage` | Gate 创建时生成 APPROVAL_REQUEST 消息 |

**总测试数**: ~40 个 (全部通过)

---

## 11. 前端 typecheck / build / E2E 结果

### TypeCheck
```
npm run typecheck  →  PASS (0 errors)
```

### Build
```
npm run build  →  ✓ built in 4.25s
```

### E2E Tests (Playwright)
```
npm run test:e2e -- --workers=1  →  20 passed (2.1m)
```

**E2E 测试覆盖**:

| # | 测试 | 场景 |
|---|---|---|
| 1 | should display multi-agent tab in task detail | Tab 可见性 |
| 2 | should start multi-agent run and show phase view | 启动 run + 4 phases + 审批卡片 |
| 3 | should show Phase 2 with three parallel lanes | 3 并行 lane |
| 4 | should expand lane to show output and input context | Lane 展开 + 输入上下文 |
| 5 | should show message flow after run | 消息流 + 类型标签 |
| 6 | should show summary artifact in artifacts tab | 审批后查看产物 |
| 7 | should not have JS errors on multi-agent page | JS 错误检查 |
| 8 | should show review lane with Phase 2 aggregation context | 审批后查看 Phase 3 交叉上下文 |
| 9 | should show final summary with phase and message counts | 审批后最终摘要 |
| 10 | should show strategy dropdown with 4 options | 4 策略选项 |
| 11 | should show BACKEND_FOCUSED template preview | 策略预览 |
| 12 | should BACKEND_FOCUSED run skip frontend lane | 后端优先无前端 lane |
| 13 | should REVIEW_ONLY run have 2 phases | REVIEW_ONLY 仅 2 phase |
| 14 | should display strategy name on run detail | 策略名中文显示 |
| 15 | should not have JS errors when switching strategies | 切换策略无错误 |
| 16 | should show approval gate card on STANDARD_DELIVERY run | 审批卡片显示 |
| 17 | should approve gate and complete run | approve + 完成 + COMPLETED |
| 18 | should reject gate and cancel run | reject + CANCELED |
| 19 | should REVIEW_ONLY strategy complete without approval card | 无审批卡片直接完成 |
| 20 | should not have JS errors during approval gate flow | 审批流程无错误 |

**Element Plus Dropdown 兼容性**: 由于 `el-select` 使用 Teleport 将下拉菜单渲染到 `body`, Playwright visibility 检查不可靠。统一使用 `waitForSelector(state: 'attached')` + `evaluate(el.click())` 模式确保跨测试可靠性。

---

## 12. 手动验证结果

由于当前为 Mock 执行模式 (不做真实代码变更), 所有验证通过自动化测试完成:

1. 后端集成测试: ~280 tests pass (含 ~40 个编排相关测试)
2. 前端 typecheck: PASS
3. 前端 build: PASS
4. E2E tests: 20/20 pass

Server 均本地运行:
- Backend: Docker `ai-coding-platform-backend-test` (port 9080)
- Frontend: Vite dev server (port 5173, proxy `/api` → 9080)

---

## 13. 已知限制

1. **Mock 执行模式**: 所有 agent step 执行为 mock (sleep + 模拟输出), 未连接真实 AI 模型或工具
2. **单 Gate 审批**: 当前每个 strategy 最多 1 个 Gate, 不支持多级审批链
3. **单人审批**: 不支持多人会签或审批流
4. **无超时处理**: Gate 无自动超时或过期机制
5. **无通知**: 审批请求不推送通知 (邮件/站内信)
6. **Element Plus 下拉菜单**: `el-select` Teleport 导致 E2E 测试需特殊处理 (已通过 `evaluate` click 解决)
7. **无队列/工作流引擎**: 执行在主线程 `@Transactional` 中同步完成

---

## 14. 是否可以进入 Milestone 35F

**可以进入**。Milestone 35E 所有目标已实现:

- [x] V18 `multi_agent_approval_gate` 表
- [x] `MultiAgentApprovalStatus` 枚举
- [x] `MultiAgentRunStatus.WAITING_APPROVAL`
- [x] `MultiAgentApprovalGateEntity` + Mapper
- [x] `MultiAgentApprovalGateResponse` / `MultiAgentApprovalDecisionRequest` DTO
- [x] `MultiAgentRunResponse.approvalGates` / `pendingApprovalGate`
- [x] Strategy Template 审批 Gate 定义
- [x] STANDARD_DELIVERY / BACKEND_FOCUSED / FRONTEND_FOCUSED 在 Phase 2 后暂停
- [x] REVIEW_ONLY 无 Gate 直接完成
- [x] `GET /api/multi-agent-runs/{runId}/approval-gates`
- [x] `POST .../approve` (Approve → Phase 3/4 → COMPLETED)
- [x] `POST .../reject` (Reject → CANCELED)
- [x] Task Logs 记录审批事件
- [x] Multi-Agent Messages (APPROVAL_REQUEST / APPROVAL_DECISION)
- [x] Audit Log (MULTI_AGENT_APPROVE / MULTI_AGENT_REJECT)
- [x] 前端审批卡片 + Approve/Reject 按钮 + 审批意见输入
- [x] 后端集成测试 (11 个新测试)
- [x] 前端 E2E 测试 (5 个新测试，共 20 个)
- [x] TypeCheck / Build 通过
