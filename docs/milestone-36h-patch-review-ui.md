# Milestone 36H: Patch Review UI

## 1. 背景

Milestone 36A-36G 已完成安全工具与只读仓库上下文能力：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling

当前系统已经可以：

```text
MOCK_PATCH_PROPOSAL
  -> approval
  -> job queue
  -> mock patch proposal artifact
  -> repository context reference
```

但 `PATCH_PROPOSAL` 仍主要以 Markdown 产物形式展示。为了让用户更清晰地审阅补丁提案，需要提供专门的 Patch Review UI 和人工审阅状态。

Milestone 36H 的目标是新增 **Patch Review UI**：

```text
PATCH_PROPOSAL artifact
  -> Patch Review view
  -> file sections / mock diff / safety banner / checklist
  -> accept as plan / reject / mark reviewed
  -> no patch apply
  -> no file write
```

本阶段仍不应用补丁，不写文件，不执行 Git 操作。所有审阅操作只保存决策记录。

## 2. 总目标

实现补丁提案审阅体验：

1. 新增 Patch Proposal Review 数据模型。
2. 为 `PATCH_PROPOSAL` artifact 建立 review 状态。
3. 提供 review 查询 / 接受 / 拒绝 / 标记已审阅 API。
4. 前端 Task Artifact 中为 `PATCH_PROPOSAL` 提供专门 Review UI。
5. UI 展示文件分组、Mock diff、安全提示、检查清单。
6. 决策仅作为计划记录，不应用 patch。
7. 补齐后端测试与前端 E2E。

完成后，Patch Proposal 从：

```text
Markdown artifact
```

升级为：

```text
reviewable proposal object
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不执行 `git apply`。
4. 不执行 `git add` / `git commit` / `git push`。
5. 不写真实代码文件。
6. 不修改 workspace。
7. 不创建真实 patch 文件。
8. 不做 PR comment。
9. 不做自动代码修改。
10. 不做多人审批流。
11. 不破坏 Task Artifact 现有接口。
12. 不破坏 36A-36G API。
13. 不绕过 Patch Proposal Artifact 的安全提示。
14. 前端保持中文暗色科技风 UI。

允许做：

- 新增 review 表。
- 新增 review API。
- 保存人工决策。
- 前端解析/展示 mock diff。
- 前端显示 checklist。
- 将决策写入 audit/task log。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V27__init_patch_proposal_review_tables.sql
```

如果 V27 已存在，请顺延。

### 4.1 patch_proposal_review

```sql
CREATE TABLE IF NOT EXISTS patch_proposal_review (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    artifact_id BIGINT NOT NULL,
    tool_execution_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    decision VARCHAR(32) NULL,
    reviewer_id BIGINT NULL,
    review_comment TEXT NULL,
    reviewed_at DATETIME NULL,
    safety_confirmed TINYINT NOT NULL DEFAULT 0,
    checklist_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_patch_review_artifact(artifact_id),
    INDEX idx_patch_review_project_status(project_id, status),
    INDEX idx_patch_review_task(task_id),
    INDEX idx_patch_review_decision(decision),
    INDEX idx_patch_review_reviewer(reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补丁提案审阅表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| artifact_id | PATCH_PROPOSAL artifact ID |
| tool_execution_id | 来源 tool execution ID |
| status | PENDING / REVIEWED |
| decision | ACCEPTED_AS_PLAN / REJECTED / NEEDS_CHANGES / MARKED_REVIEWED |
| safety_confirmed | 审阅者确认未应用补丁 |
| checklist_json | 审阅检查项 |

无物理外键，保持项目规范。

## 5. 状态与枚举

### 5.1 PatchProposalReviewStatus.java

```java
public enum PatchProposalReviewStatus {
    PENDING,
    REVIEWED
}
```

### 5.2 PatchProposalDecision.java

```java
public enum PatchProposalDecision {
    ACCEPTED_AS_PLAN,
    REJECTED,
    NEEDS_CHANGES,
    MARKED_REVIEWED
}
```

语义：

| decision | 说明 |
|---|---|
| ACCEPTED_AS_PLAN | 接受为后续实现计划，不应用补丁 |
| REJECTED | 拒绝该提案 |
| NEEDS_CHANGES | 需要修改提案 |
| MARKED_REVIEWED | 仅标记已审阅 |

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/PatchProposalReviewEntity.java
```

要求：

- `@TableName("patch_proposal_review")`
- `@TableId(type = IdType.ASSIGN_ID)`
- createTime / updateTime 自动填充
- 不使用 Lombok
- 手写 getter/setter

### 6.2 Mapper

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/PatchProposalReviewMapper.java
```

### 6.3 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/PatchProposalReviewResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/PatchProposalReviewDecisionRequest.java
```

`PatchProposalReviewResponse` 字段：

- id
- projectId
- taskId
- artifactId
- toolExecutionId
- status
- decision
- reviewerId
- reviewComment
- reviewedAt
- safetyConfirmed
- checklistJson
- createTime
- updateTime

`PatchProposalReviewDecisionRequest` 字段：

- decision String
- comment String
- safetyConfirmed Boolean
- checklist Map<String, Object>

## 7. 后端服务设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/PatchProposalReviewService.java
```

### 7.1 ensureReviewForArtifact

```java
public PatchProposalReviewResponse ensureReviewForArtifact(Long artifactId)
```

行为：

1. 查询 artifact。
2. artifact 不存在 → NOT_FOUND。
3. artifactType != PATCH_PROPOSAL → BAD_REQUEST。
4. 校验项目权限 VIEWER+。
5. 如果 review 已存在，返回。
6. 如果不存在，创建 PENDING review。

### 7.2 decide

```java
public PatchProposalReviewResponse decide(Long artifactId, PatchProposalReviewDecisionRequest request)
```

行为：

1. 校验 artifact 是 PATCH_PROPOSAL。
2. 校验权限 MAINTAINER+。
3. decision 必须合法。
4. safetyConfirmed 必须为 true，否则 BAD_REQUEST。
5. 更新：
   - status = REVIEWED
   - decision = request.decision
   - reviewerId = currentUser
   - reviewComment
   - reviewedAt
   - checklistJson
6. 写 task log：

```text
stage: PATCH_PROPOSAL_REVIEWED
message: Patch Proposal 已审阅，决策：ACCEPTED_AS_PLAN
```

7. 记录 audit log（如果当前模块容易接入）：

```text
AuditActionType.PATCH_PROPOSAL_REVIEW
```

如果新增 audit enum 成本较低，则新增；否则只写 task log。

### 7.3 幂等规则

- 已 REVIEWED 再次提交 decision：允许覆盖还是 CONFLICT？

建议：允许覆盖，但写 task log：

```text
PATCH_PROPOSAL_REVIEW_UPDATED
```

这样方便用户修改决策。

## 8. PatchProposalArtifactService 集成

修改：

```text
PatchProposalArtifactService
```

生成 PATCH_PROPOSAL artifact 后：

1. 自动创建 PENDING review。
2. checklistJson 初始化：

```json
{
  "matchesRequirement": false,
  "noSensitiveData": false,
  "noFileWritten": true,
  "noGitOperation": true,
  "readyForManualImplementation": false
}
```

3. task log：

```text
stage: PATCH_PROPOSAL_REVIEW_CREATED
message: Patch Proposal 审阅记录已创建，等待人工审阅。
```

## 9. 后端 API

新增 Controller：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/PatchProposalReviewController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/task-artifacts/{artifactId}/patch-review` | VIEWER+ | 查询或创建 review |
| POST | `/api/task-artifacts/{artifactId}/patch-review/decision` | MAINTAINER+ | 提交审阅决策 |
| GET | `/api/tasks/{taskId}/patch-reviews` | VIEWER+ | 查询任务下所有 patch reviews |

请求示例：

```json
{
  "decision": "ACCEPTED_AS_PLAN",
  "comment": "可以作为后续手工实现计划。",
  "safetyConfirmed": true,
  "checklist": {
    "matchesRequirement": true,
    "noSensitiveData": true,
    "noFileWritten": true,
    "noGitOperation": true,
    "readyForManualImplementation": true
  }
}
```

响应示例：

```json
{
  "code": "OK",
  "data": {
    "artifactId": "123",
    "status": "REVIEWED",
    "decision": "ACCEPTED_AS_PLAN",
    "safetyConfirmed": true
  }
}
```

## 10. Artifact Response 增强

可选修改：

```text
TaskArtifactResponse.java
```

新增：

- patchReviewStatus
- patchReviewDecision

如果不想改现有 Artifact API，可由前端在打开 PATCH_PROPOSAL 时单独调用 review API。

推荐：先不改 Artifact Response，降低回归风险。

## 11. 前端 API

新增：

```text
frontend/src/modules/task/patchReviewApi.ts
```

或放入 `frontend/src/modules/task/api.ts`。

类型：

```ts
export interface PatchProposalReview {
  id: string
  projectId: string
  taskId: string
  artifactId: string
  toolExecutionId: string | null
  status: string
  decision: string | null
  reviewerId: string | null
  reviewComment: string | null
  reviewedAt: string | null
  safetyConfirmed: boolean
  checklistJson: string | null
  createTime: string
  updateTime: string
}

export interface PatchProposalReviewDecisionRequest {
  decision: string
  comment?: string
  safetyConfirmed: boolean
  checklist?: Record<string, unknown>
}
```

函数：

```ts
export function getPatchProposalReview(artifactId: string)
export function submitPatchProposalReviewDecision(artifactId: string, data: PatchProposalReviewDecisionRequest)
export function listTaskPatchReviews(taskId: string)
```

## 12. 前端组件设计

新增：

```text
frontend/src/modules/task/components/PatchProposalReviewPanel.vue
```

Props：

```ts
artifact: TaskArtifact
```

功能：

1. 解析 artifact.content。
2. 显示安全 banner。
3. 显示文件列表。
4. 显示 diff-like 内容。
5. 显示 checklist。
6. 提交审阅决策。

### 12.1 安全 Banner

必须显示：

```text
安全提示：该补丁提案仅用于审阅。系统未写入文件，未执行 git apply，未提交代码。
```

data-testid:

- `patch-review-safety-banner`

### 12.2 文件列表

从 artifact markdown 中解析：

- diff --git a/... b/...

如果解析失败，显示：

```text
未解析到文件列表
```

data-testid:

- `patch-review-file-list`
- `patch-review-file-item`

### 12.3 Diff-like 展示

使用现有 MarkdownRenderer 或简单 `<pre>`。

要求：

- 不引入 Monaco。
- 不做复杂 diff viewer。
- 保持暗色风格。

data-testid:

- `patch-review-diff`

### 12.4 Checklist

字段：

- 是否符合任务需求
- 是否无敏感信息
- 是否确认未写入文件
- 是否确认未执行 Git 操作
- 是否可作为后续手工实现计划

data-testid:

- `patch-review-checklist`
- `patch-review-safety-confirmed`

### 12.5 决策按钮

按钮：

- 接受为计划
- 需要修改
- 拒绝
- 标记已审阅

data-testid:

- `btn-accept-patch-plan`
- `btn-needs-patch-changes`
- `btn-reject-patch-proposal`
- `btn-mark-patch-reviewed`

提交前必须确认：

- safetyConfirmed = true
- noFileWritten = true
- noGitOperation = true

## 13. TaskDetailPage 集成

修改：

```text
frontend/src/modules/task/pages/TaskDetailPage.vue
```

在 Artifacts Tab：

1. 如果 artifactType = PATCH_PROPOSAL：
   - 使用 `PatchProposalReviewPanel`
2. 否则保持原有 MarkdownRenderer。

要求：

- 不破坏 REPORT / MARKDOWN / 其他 artifact 展示。
- PATCH_PROPOSAL 显示 review 状态。

## 14. MultiAgentRunPanel 集成

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

当工具卡片关联 artifactId：

1. 显示：

```text
Patch Review: Pending / Reviewed
```

2. 提示用户：

```text
请在任务产物中完成补丁提案审阅。
```

不要求在 MultiAgentRunPanel 内直接做 review 决策，避免 UI 复杂。

## 15. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/PatchProposalReviewIntegrationTest.java
```

测试不少于 18 个：

1. PATCH_PROPOSAL artifact 生成后自动创建 PENDING review。
2. GET patch-review 返回 PENDING。
3. 非 PATCH_PROPOSAL artifact 查询 review 返回 BAD_REQUEST。
4. 不存在 artifact 返回 NOT_FOUND。
5. MAINTAINER 可提交 ACCEPTED_AS_PLAN。
6. MAINTAINER 可提交 REJECTED。
7. MAINTAINER 可提交 NEEDS_CHANGES。
8. MAINTAINER 可提交 MARKED_REVIEWED。
9. safetyConfirmed=false 返回 BAD_REQUEST。
10. 无效 decision 返回 BAD_REQUEST。
11. VIEWER 提交 decision 返回 PROJECT_ACCESS_DENIED。
12. 未登录查询 review 返回 UNAUTHORIZED。
13. 重复提交 decision 可更新 review。
14. task logs 包含 PATCH_PROPOSAL_REVIEWED。
15. listTaskPatchReviews 返回任务下 review。
16. checklistJson 正确保存。
17. review 不触发任何 tool execution。
18. review 不改变 artifact content。
19. review output 不包含真实 Git 操作。

全量后端质量门：

```bash
cd backend
mvn test
```

## 16. 前端 E2E

新增或修改：

```text
frontend/e2e/multi-agent-orchestration.spec.ts
frontend/e2e/patch-proposal-review.spec.ts
```

测试：

1. 生成 PATCH_PROPOSAL artifact。
2. 打开 Task Artifacts Tab。
3. PATCH_PROPOSAL 使用 Review Panel 展示。
4. 安全 banner 可见。
5. 文件列表可见。
6. diff-like 内容可见。
7. checklist 可勾选。
8. 点击“接受为计划”成功。
9. review 状态变为 REVIEWED / ACCEPTED_AS_PLAN。
10. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 17. 文档与报告

完成后新增：

```text
docs/milestone-36h-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. patch_proposal_review 表说明
3. PatchProposalReviewService 设计说明
4. Patch Review API 清单
5. PatchProposalReviewPanel 说明
6. TaskDetailPage 集成说明
7. MultiAgentRunPanel 集成说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36I

## 18. 验收标准

必须满足：

- `patch_proposal_review` 表存在。
- PATCH_PROPOSAL artifact 自动创建 PENDING review。
- 可以查询 patch review。
- 可以提交 ACCEPTED_AS_PLAN / REJECTED / NEEDS_CHANGES / MARKED_REVIEWED。
- safetyConfirmed=false 时拒绝提交。
- VIEWER 不可提交 decision。
- review 不应用 patch。
- review 不写文件。
- review 不执行 Git。
- 前端 PATCH_PROPOSAL 使用专门 Review Panel 展示。
- 前端显示安全 banner。
- 前端可提交审阅决策。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 19. 已知非目标

本阶段不做：

- 真实 patch apply
- 真实文件写入
- Git commit
- PR comment
- 多人审批
- 代码编辑器
- Monaco diff viewer
- 下载 patch 文件
- 审阅通知
- 审阅过期机制

这些可进入后续：

- 36I: Tool Parameter Advanced Schema
- 36J: Patch Review History / Comments
- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index

## 20. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36H。

文档路径：
docs/milestone-36h-patch-review-ui.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A-36G 的 Tool Sandbox / Policy / Approval / Patch Proposal / Parameters / Job Queue / Repository Read Tools 基础上，新增 Patch Review UI。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. Patch Review 只记录人工审阅决策，不应用补丁。
9. 不要破坏 Task Artifact 原有接口。
10. 不要破坏 36A-36G 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V27 patch_proposal_review migration。
2. 新增 PatchProposalReviewStatus / PatchProposalDecision 枚举。
3. 新增 PatchProposalReviewEntity / Mapper / DTO。
4. 新增 PatchProposalReviewService。
5. PatchProposalArtifactService 生成 PATCH_PROPOSAL 后自动创建 PENDING review。
6. 新增 API：
   - GET /api/task-artifacts/{artifactId}/patch-review
   - POST /api/task-artifacts/{artifactId}/patch-review/decision
   - GET /api/tasks/{taskId}/patch-reviews
7. 前端新增 PatchProposalReviewPanel.vue。
8. TaskDetailPage Artifacts Tab 对 PATCH_PROPOSAL 使用 Review Panel。
9. MultiAgentRunPanel 对关联 artifact 显示 Patch Review 状态提示。
10. 后端测试不少于 18 个。
11. 前端 E2E 覆盖 PATCH_PROPOSAL Review Panel 和提交决策。
12. 新增 docs/milestone-36h-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. patch_proposal_review 表说明
3. PatchProposalReviewService 设计说明
4. Patch Review API 清单
5. PatchProposalReviewPanel 说明
6. TaskDetailPage 集成说明
7. MultiAgentRunPanel 集成说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36I

现在开始实现，不要只给计划。
```
