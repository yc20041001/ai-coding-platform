# Milestone 36C: Human-approved Tool Execution

## 1. 背景

Milestone 36A-36B 已经完成工具执行安全底座：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy

当前工具策略为：

```text
LOW / allowed -> Mock execution COMPLETED
disabled / disallowed -> BLOCKED
HIGH / DANGEROUS -> BLOCKED
```

这足够安全，但缺少“人审后允许执行”的能力。后续如果要引入更接近真实执行的工具，例如 patch proposal、只读仓库 diff、受限文件读取，就需要一个人工审批闸门。

Milestone 36C 的目标是新增 **Human-approved Tool Execution**：

```text
ToolPolicy = REQUIRES_APPROVAL
  -> create Tool Approval Gate
  -> tool execution WAITING_APPROVAL
  -> OWNER / MAINTAINER approve or reject
  -> approved: run Mock execution
  -> rejected: mark REJECTED / BLOCKED
```

本阶段仍不执行真实 shell，不做 Git 写操作，不写真实代码文件。审批通过后也只执行 Mock / Dry-run。

## 2. 总目标

实现工具执行的人审闸门基础闭环：

1. 新增 Tool Approval 数据表。
2. ToolPolicyService 支持 `REQUIRES_APPROVAL` 决策。
3. ToolSandboxExecution 支持 `WAITING_APPROVAL` 状态。
4. 高风险但可审批工具生成 approval gate，而不是直接 BLOCKED。
5. OWNER / MAINTAINER 可 approve / reject。
6. approve 后执行 Mock 工具并标记 COMPLETED。
7. reject 后标记 REJECTED 或 BLOCKED。
8. 前端 MultiAgentRunPanel 展示工具审批卡片。
9. 补齐后端集成测试和前端 E2E。

完成后，工具策略从：

```text
allowed / blocked
```

升级为：

```text
allowed / blocked / requires approval
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不写真实代码文件。
4. 不做 patch apply。
5. 不做真实文件读取。
6. 不引入 Docker sandbox / Firecracker / Kubernetes Job。
7. 不引入异步队列 / Worker。
8. 不做审批通知系统。
9. 不做审批超时后台任务。
10. 不做多级审批。
11. 不破坏 36A tool_sandbox_execution API。
12. 不破坏 36B Tool Catalog / Project Tool Config API。
13. 不破坏 35A-35F Multi-Agent API。
14. 不绕过 ProjectPermissionService。
15. 前端保持中文暗色科技风 UI。

允许做：

- 新增审批表。
- 新增审批状态。
- 新增审批 API。
- ToolPolicyService 返回 REQUIRES_APPROVAL。
- ToolSandboxExecutionService 创建 WAITING_APPROVAL 记录。
- 审批通过后执行 Mock。
- 审批拒绝后更新状态。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V22__init_tool_execution_approval_tables.sql
```

如果 V22 已存在，请顺延到下一个版本号。

### 4.1 tool_execution_approval

```sql
CREATE TABLE IF NOT EXISTS tool_execution_approval (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    step_id BIGINT NULL,
    tool_execution_id BIGINT NOT NULL,
    tool_id BIGINT NULL,
    tool_key VARCHAR(64) NOT NULL,
    approval_key VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by BIGINT NULL,
    decided_by BIGINT NULL,
    decision_comment TEXT NULL,
    requested_at DATETIME NULL,
    decided_at DATETIME NULL,
    expires_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_approval_project_status(project_id, status),
    INDEX idx_tool_approval_task(task_id),
    INDEX idx_tool_approval_run(run_id),
    INDEX idx_tool_approval_step(step_id),
    INDEX idx_tool_approval_execution(tool_execution_id),
    INDEX idx_tool_approval_tool(tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行审批表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| tool_execution_id | 对应 tool_sandbox_execution.id |
| tool_key | 工具 key |
| approval_key | 审批 key，例如 TOOL_EXECUTION_APPROVAL |
| risk_level | 风险等级 |
| status | PENDING / APPROVED / REJECTED / EXPIRED |
| requested_by | 请求人 |
| decided_by | 审批人 |
| decision_comment | 审批意见 |
| expires_at | 过期时间，当前仅记录不自动处理 |

无物理外键，保持项目规范。

## 5. 状态设计

### 5.1 ToolApprovalStatus.java

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolApprovalStatus.java
```

```java
public enum ToolApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}
```

### 5.2 ToolPolicyDecisionType.java

如果 36B 尚未定义，可新增内部枚举或独立枚举：

```java
public enum ToolPolicyDecisionType {
    ALLOWED,
    BLOCKED,
    REQUIRES_APPROVAL
}
```

### 5.3 ToolExecutionStatus 扩展

修改 36A 的 `ToolExecutionStatus`：

```java
public enum ToolExecutionStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    BLOCKED,
    REJECTED
}
```

说明：

| 状态 | 说明 |
|---|---|
| WAITING_APPROVAL | 工具调用已创建，但等待人工审批 |
| REJECTED | 人工拒绝后终态 |
| BLOCKED | 策略直接阻止，无审批机会 |

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionApprovalEntity.java
```

要求：

- `@TableName("tool_execution_approval")`
- `@TableId(type = IdType.ASSIGN_ID)`
- createTime / updateTime 自动填充
- 不使用 Lombok
- 手写 getter/setter

### 6.2 Mapper

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolExecutionApprovalMapper.java
```

### 6.3 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionApprovalResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolApprovalDecisionRequest.java
```

`ToolExecutionApprovalResponse` 字段：

- id
- projectId
- taskId
- runId
- stepId
- toolExecutionId
- toolId
- toolKey
- approvalKey
- title
- description
- riskLevel
- status
- requestedBy
- decidedBy
- decisionComment
- requestedAt
- decidedAt
- expiresAt
- createTime

`ToolApprovalDecisionRequest` 字段：

- comment String

## 7. ToolPolicyService 改造

36B 中 `ToolPolicyService.checkToolAllowed(...)` 需要升级为三态决策。

建议返回：

```java
public static class ToolPolicyDecision {
    private ToolPolicyDecisionType decisionType;
    private boolean allowed;
    private boolean requiresApproval;
    private String blockedReason;
    private ToolCatalogEntity toolCatalog;
    private ProjectToolConfigEntity projectConfig;
}
```

### 7.1 决策规则

| 条件 | decision |
|---|---|
| tool 不存在 | BLOCKED |
| tool 全局 disabled | BLOCKED |
| project config disabled | BLOCKED |
| policy_json.allowShell=true | BLOCKED |
| policy_json.allowGitWrite=true | BLOCKED |
| policy_json.allowFileWrite=true | BLOCKED |
| policy_json.allowedStepTypes 不包含 stepType | BLOCKED |
| riskLevel LOW | ALLOWED |
| riskLevel MEDIUM 且项目 enabled | ALLOWED |
| riskLevel HIGH 且项目 enabled | REQUIRES_APPROVAL |
| riskLevel DANGEROUS | BLOCKED |

注意：

- DANGEROUS 本阶段仍然不可审批，直接 BLOCKED。
- HIGH 可以审批，但审批通过后仍只 Mock。
- MEDIUM 保持 36B 行为：项目启用即可执行 Mock。

### 7.2 默认项目策略

延续 36B：

- project_tool_config 不存在时：
  - LOW 默认 allowed
  - MEDIUM 默认 blocked
  - HIGH 默认 blocked
  - DANGEROUS blocked
- project_tool_config 存在且 enabled=1 时：
  - LOW / MEDIUM allowed
  - HIGH requires approval
  - DANGEROUS blocked

## 8. Seed 一个 HIGH 工具

为了验证审批流，36C 需要新增一个 HIGH 风险 Mock 工具。

在 V22 或新 migration 中新增：

| id | toolKey | name | type | risk | mode |
|---|---|---|---|---|---|
| 910006 | MOCK_PATCH_PROPOSAL | Mock 补丁方案生成 | ANALYSIS | HIGH | MOCK_EXECUTE |

`policy_json`：

```json
{
  "allowedStepTypes": ["BACKEND_IMPLEMENTATION_PLAN", "FRONTEND_IMPLEMENTATION_PLAN", "CODE_REVIEW"],
  "readOnly": true,
  "allowShell": false,
  "allowGitWrite": false,
  "allowFileWrite": false,
  "requiresApproval": true
}
```

注意：

- 这个工具不生成真实 patch 文件。
- 只生成 outputPayload 中的 mock patch summary。
- `filesTouched=[]`。
- `gitOperations=[]`。

## 9. ToolSandboxExecutionService 改造

修改：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java
```

执行流程：

```text
resolve toolKey
  -> ToolPolicyService.check(...)
  -> ALLOWED: create RUNNING -> COMPLETED mock execution
  -> BLOCKED: create BLOCKED execution
  -> REQUIRES_APPROVAL: create WAITING_APPROVAL execution + ToolExecutionApproval
```

### 9.1 WAITING_APPROVAL execution

字段：

- status = WAITING_APPROVAL
- summary = `工具 MOCK_PATCH_PROPOSAL 等待人工审批`
- outputPayload：

```json
{
  "mock": true,
  "waitingApproval": true,
  "filesTouched": [],
  "gitOperations": []
}
```

任务日志：

```text
stage: TOOL_SANDBOX_WAITING_APPROVAL
message: 工具 MOCK_PATCH_PROPOSAL 需要人工审批后执行。
```

### 9.2 approve 后执行 Mock

新增方法：

```java
public ToolSandboxExecutionResponse approveAndExecute(Long executionId, String comment)
```

行为：

1. 校验 execution 存在。
2. 校验 execution.status = WAITING_APPROVAL。
3. 校验 approval.status = PENDING。
4. 校验权限 OWNER / MAINTAINER。
5. 更新 approval = APPROVED。
6. 执行 Mock output。
7. 更新 execution = COMPLETED。
8. 写 task log `TOOL_SANDBOX_APPROVED_EXECUTED`。

### 9.3 reject

新增方法：

```java
public ToolSandboxExecutionResponse rejectExecution(Long executionId, String comment)
```

行为：

1. 校验 execution 存在。
2. 校验 execution.status = WAITING_APPROVAL。
3. 校验 approval.status = PENDING。
4. 校验权限 OWNER / MAINTAINER。
5. 更新 approval = REJECTED。
6. 更新 execution = REJECTED。
7. outputPayload 包含 rejected=true。
8. 写 task log `TOOL_SANDBOX_REJECTED`。

### 9.4 幂等与非法状态

- 已 APPROVED 再 approve → CONFLICT。
- 已 REJECTED 再 reject → CONFLICT。
- 非 WAITING_APPROVAL execution 审批 → CONFLICT。
- 无权限审批 → PROJECT_ACCESS_DENIED。

## 10. Multi-Agent 集成策略

36C 不要求所有 step 都使用 HIGH 工具。

推荐：

- 默认 stepType 映射仍保持 36B 的 LOW / MEDIUM 工具。
- 为 CODE_REVIEW 或 BACKEND_IMPLEMENTATION_PLAN 增加可选 HIGH 工具 `MOCK_PATCH_PROPOSAL`。

简单策略：

```text
if stepType == CODE_REVIEW and project tool MOCK_PATCH_PROPOSAL enabled:
    create waiting approval tool execution
else:
    use existing 36B mapping
```

这样：

- 默认旧流程不受影响。
- Owner 开启 HIGH 工具后，Review step 会产生 WAITING_APPROVAL。
- 审批通过后才生成 mock patch proposal。

## 11. 后端 API

新增 Controller 或扩展现有 Controller：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionApprovalController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/tool-sandbox-executions/{executionId}/approval` | VIEWER+ | 查询工具审批 |
| POST | `/api/tool-sandbox-executions/{executionId}/approve` | MAINTAINER+ | 批准并执行 Mock |
| POST | `/api/tool-sandbox-executions/{executionId}/reject` | MAINTAINER+ | 拒绝执行 |
| GET | `/api/projects/{projectId}/tool-approvals?status=` | VIEWER+ | 查询项目审批列表 |

审批请求：

```json
{
  "comment": "确认允许生成 Mock 补丁方案"
}
```

审批响应：

```json
{
  "code": "OK",
  "data": {
    "id": "123",
    "toolExecutionId": "456",
    "status": "APPROVED",
    "decisionComment": "确认允许生成 Mock 补丁方案"
  }
}
```

## 12. Response 增强

修改：

```text
ToolSandboxExecutionResponse.java
```

新增：

- approval ToolExecutionApprovalResponse
- requiresApproval Boolean

前端类型同步。

## 13. 前端 API

修改：

```text
frontend/src/modules/task/api.ts
```

或新增：

```text
frontend/src/modules/tool/api.ts
```

如果 36B 已有 `frontend/src/modules/tool/api.ts`，继续复用。

新增类型：

```ts
export interface ToolExecutionApproval {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  stepId: string | null
  toolExecutionId: string
  toolId: string | null
  toolKey: string
  approvalKey: string
  title: string
  description: string | null
  riskLevel: string
  status: string
  requestedBy: string | null
  decidedBy: string | null
  decisionComment: string | null
  requestedAt: string | null
  decidedAt: string | null
  expiresAt: string | null
}
```

新增函数：

```ts
export function getToolExecutionApproval(executionId: string)
export function approveToolExecution(executionId: string, comment?: string)
export function rejectToolExecution(executionId: string, comment?: string)
export function listProjectToolApprovals(projectId: string, status?: string)
```

## 14. 前端 UI

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

### 14.1 工具审批卡片

当 tool execution status = WAITING_APPROVAL：

显示：

- 工具名称
- 风险等级
- WAITING_APPROVAL 状态
- 审批说明
- 审批意见输入框
- 批准按钮
- 驳回按钮

文案：

```text
该工具需要人工审批。审批通过后仍只执行 Mock，不会执行真实 Shell、Git 或文件写入。
```

按钮：

- `批准并执行 Mock`
- `驳回`

data-testid：

- `tool-approval-card`
- `tool-approval-comment`
- `btn-approve-tool`
- `btn-reject-tool`

### 14.2 审批历史

如果 status = APPROVED / REJECTED：

显示：

- 审批人
- 审批意见
- 决策时间

### 14.3 工具统计

Run summary 增加：

```text
待审批工具 N
已批准 N
已拒绝 N
```

## 15. Project Tool 页面增强

修改 36B 页面：

```text
frontend/src/modules/tool/pages/ProjectToolConfigPage.vue
```

要求：

1. 显示 HIGH 工具。
2. HIGH 工具可由 OWNER 启用。
3. HIGH 工具旁显示：

```text
需要审批
```

4. DANGEROUS 工具如果未来出现，按钮禁用并显示：

```text
当前阶段不允许启用
```

## 16. 后端测试

新增或修改：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionApprovalIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/ToolCatalogPolicyIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java
```

测试不少于 18 个：

### Policy / Approval Creation

1. HIGH 工具默认 project config 不存在时 BLOCKED。
2. OWNER 启用 HIGH 工具成功。
3. HIGH 工具启用后 policy 返回 REQUIRES_APPROVAL。
4. CODE_REVIEW step 创建 WAITING_APPROVAL execution。
5. WAITING_APPROVAL execution 创建 approval record。
6. task logs 包含 TOOL_SANDBOX_WAITING_APPROVAL。

### Approval API

7. VIEWER 可查询 approval。
8. 未登录查询 approval 返回 UNAUTHORIZED。
9. MAINTAINER / OWNER 可 approve。
10. approve 后 execution = COMPLETED。
11. approve 后 approval = APPROVED。
12. approve 后 outputPayload 仍包含 mock=true / filesTouched=[] / gitOperations=[]。
13. MAINTAINER / OWNER 可 reject。
14. reject 后 execution = REJECTED。
15. reject 后 approval = REJECTED。
16. 非 MAINTAINER approve 返回 PROJECT_ACCESS_DENIED。
17. 重复 approve 返回 CONFLICT。
18. 非 WAITING_APPROVAL execution approve 返回 CONFLICT。
19. DANGEROUS 工具仍然 BLOCKED。

全量后端质量门：

```bash
cd backend
mvn test
```

## 17. 前端 E2E

新增或修改：

```text
frontend/e2e/project-tool-policy.spec.ts
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. 项目工具页可启用 HIGH 工具，并显示「需要审批」。
2. 启用 HIGH 工具后启动 Multi-Agent Run。
3. MultiAgentRunPanel 显示 tool approval card。
4. 点击批准并执行 Mock 后，工具卡片变为 COMPLETED。
5. 点击驳回后，工具卡片变为 REJECTED。
6. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 18. 文档与报告

完成后新增：

```text
docs/milestone-36c-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Approval 设计说明
4. ToolPolicyService 三态决策说明
5. ToolSandboxExecutionService 审批流说明
6. 后端 API 清单
7. 前端审批卡片说明
8. Project Tool 页面增强说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36D

## 19. 验收标准

必须满足：

- tool_execution_approval 表存在。
- ToolPolicyService 支持 ALLOWED / BLOCKED / REQUIRES_APPROVAL。
- HIGH 工具启用后不直接执行，而是 WAITING_APPROVAL。
- approve 后 execution = COMPLETED。
- reject 后 execution = REJECTED。
- approve 后仍只 Mock，不执行真实 shell / Git / file write。
- outputPayload 始终包含：
  - `mock=true`
  - `filesTouched=[]`
  - `gitOperations=[]`
- 无权限用户不可审批。
- 重复审批返回 CONFLICT。
- 前端可以展示审批卡片并完成 approve / reject。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 20. 已知非目标

本阶段不做：

- 真实 shell executor
- 真实 Git executor
- 文件写入工具
- Patch apply
- 多级审批
- 审批通知
- 审批超时自动任务
- 异步 Worker
- 工具参数 schema 编辑器
- 用户自定义工具

这些可进入后续：

- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling

## 21. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36C。

文档路径：
docs/milestone-36c-human-approved-tool-execution.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A/36B 的 Tool Sandbox + Tool Policy 基础上，新增 Human-approved Tool Execution。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要写真实代码文件。
6. 审批通过后也只能执行 Mock / Dry-run，不允许真实执行。
7. 不要破坏 36A tool_sandbox_execution API。
8. 不要破坏 36B Tool Catalog / Project Tool Config API。
9. 不要破坏 35A-35F Multi-Agent Run / Phase / Step / Message / Approval Gate / Workflow Template API。
10. 不要绕过 ProjectPermissionService。
11. 不要改 Auth、Project、Member、Repository、Chat、RAG、Model Gateway 已验证逻辑，除非本模块必须依赖。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V22 tool_execution_approval migration。
2. 新增 ToolApprovalStatus / ToolPolicyDecisionType。
3. 扩展 ToolExecutionStatus：WAITING_APPROVAL / REJECTED。
4. 新增 ToolExecutionApprovalEntity / Mapper / DTO。
5. ToolPolicyService 从 allowed/blocked 升级为 ALLOWED / BLOCKED / REQUIRES_APPROVAL。
6. Seed 一个 HIGH 风险工具 MOCK_PATCH_PROPOSAL。
7. ToolSandboxExecutionService 对 HIGH 工具创建 WAITING_APPROVAL execution + approval record。
8. approve 后执行 Mock，并将 execution 标记 COMPLETED。
9. reject 后将 execution 标记 REJECTED。
10. 新增审批 API：
   - GET /api/tool-sandbox-executions/{executionId}/approval
   - POST /api/tool-sandbox-executions/{executionId}/approve
   - POST /api/tool-sandbox-executions/{executionId}/reject
   - GET /api/projects/{projectId}/tool-approvals?status=
11. ToolSandboxExecutionResponse 增加 approval / requiresApproval 字段。
12. 前端 tool/api.ts 增加审批类型和 API。
13. MultiAgentRunPanel 增加工具审批卡片。
14. ProjectToolConfigPage 显示 HIGH 工具“需要审批”。
15. 后端测试不少于 18 个。
16. 前端 E2E 覆盖 approve / reject。
17. 新增 docs/milestone-36c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Approval 设计说明
4. ToolPolicyService 三态决策说明
5. ToolSandboxExecutionService 审批流说明
6. 后端 API 清单
7. 前端审批卡片说明
8. Project Tool 页面增强说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36D

现在开始实现，不要只给计划。
```
