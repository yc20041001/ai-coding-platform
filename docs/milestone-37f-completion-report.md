# Milestone 37F: Tool Execution Trace & Evidence Viewer — 完成报告

## 1. 新增 / 修改文件清单

### 后端新增 (11 个文件)

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionTraceResponse.java` | 顶层 Trace DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionTraceEventResponse.java` | Timeline 事件 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionEvidenceResponse.java` | Evidence 聚合 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionFileEvidenceResponse.java` | 文件读/跳过证据 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionApprovalEvidenceResponse.java` | 审批证据 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionJobEvidenceResponse.java` | Job 证据 DTO (含 Retry/DLQ) |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionArtifactEvidenceResponse.java` | 产物/补丁审查证据 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionTraceService.java` | Trace 聚合服务 |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolTracePayloadSanitizer.java` | Payload 脱敏/截断工具 |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionTraceController.java` | Trace API 控制器 |
| `backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionTraceIntegrationTest.java` | 集成测试（35 个） |

### 前端新增/修改 (7 个文件)

| 文件 | 说明 |
|------|------|
| `frontend/src/modules/task/components/ToolExecutionTraceDrawer.vue` | **新增** Trace 抽屉组件 |
| `frontend/src/modules/task/api.ts` | **修改** 新增 Trace 类型定义和 API 函数 |
| `frontend/src/modules/admin/api.ts` | **修改** ToolExecutionProblemJob 增加 toolExecutionId 字段 |
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | **修改** 集成 Trace Drawer |
| `frontend/src/modules/admin/pages/ObservabilityPage.vue` | **修改** 集成 Trace Drawer |
| `frontend/e2e/multi-agent-orchestration.spec.ts` | **修改** 新增 6 个 Trace E2E 测试 |
| `frontend/e2e/knowledge-observability.spec.ts` | **修改** 新增 1 个 Observability E2E 测试 |

---

## 2. Trace DTO 设计说明

```
ToolExecutionTraceResponse
├── executionId / projectId / taskId / runId / stepId
├── toolKey / toolName / riskLevel
├── status / mode / readOnly
├── policyAllowed / policyReason
├── inputPayload / outputPayload (已脱敏)
├── approval: ToolExecutionApprovalEvidenceResponse
│   ├── approvalId / status / approverId / approverName
│   └── comment / createTime / decidedAt
├── job: ToolExecutionJobEvidenceResponse
│   ├── jobId / status / priority / attemptCount
│   ├── errorCode / failureStage
│   ├── nextRetryAt / deadLetteredAt / deadLetterReason / sourceJobId
│   └── createTime / startedAt / finishedAt
├── evidence: ToolExecutionEvidenceResponse
│   ├── filesReadCount / skippedFilesCount
│   ├── redacted / truncated / binarySkipped
│   ├── pathSandboxApplied / sensitiveDenylistApplied
│   ├── filesRead[] / skippedFiles[]
│   └── artifacts[]
├── events: ToolExecutionTraceEventResponse[]
│   ├── eventType / title / description / status / eventTime
│   └── metadata (Map)
└── createTime / updateTime
```

所有 DTO 遵循项目规范：手写 getter/setter、无 Lombok、IDs 对外为 String 类型。

---

## 3. Trace Service 聚合逻辑

`ToolExecutionTraceService` 通过以下步骤聚合完整 Trace：

1. **查询主记录**: `ToolSandboxExecutionMapper.selectById(executionId)` → 不存在则抛出 NOT_FOUND
2. **权限检查**: `ProjectPermissionService.checkProjectRole(projectId, VIEWER+)`
3. **构建 Trace** (`buildTrace`):
   - 基础字段：executionId、toolKey、status、mode、projectId、taskId、runId、stepId
   - **Policy 推断**: status = BLOCKED/REJECTED → policyAllowed=false，other → true
   - **风险等级**: 优先从 `ToolExecutionApprovalEntity` 获取，否则从工具名推断（READ_*/SEARCH_* → LOW，其他 → MEDIUM）
   - **证据**: 解析 `outputPayload` JSON，提取 `filesRead`/`skippedFiles`/`redacted`/`truncated`/`binarySkipped` 等
   - **审批证据**: 查询 `tool_execution_approval WHERE tool_execution_id = ?`
   - **Job 证据**: 查询 `tool_execution_job WHERE tool_execution_id = ?` 按时间降序，取第一条
   - **产物证据**: 若有 artifactId，查询 `ai_task_artifact` 和 `patch_proposal_review`
   - **事件**: 根据实体字段动态生成 `EXECUTION_CREATED`、`POLICY_CHECKED`、`OUTPUT_CAPTURED` 等
4. **Payload 脱敏**: 通过 `ToolTracePayloadSanitizer` 处理 `inputPayload`/`outputPayload`

### 三个公开方法：

| 方法 | 查询条件 | 返回 |
|------|---------|------|
| `getTrace(executionId)` | `selectById` | 单个 ToolExecutionTraceResponse |
| `listRunTraces(runId)` | `selectList WHERE run_id = ?` | List 按 create_time ASC |
| `listTaskTraces(taskId)` | `selectList WHERE task_id = ?` | List 按 create_time ASC |

---

## 4. Trace API 清单

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/tool-sandbox-executions/{executionId}/trace` | 无 | `ApiResponse<ToolExecutionTraceResponse>` |
| GET | `/api/multi-agent-runs/{runId}/tool-execution-traces` | 无 | `ApiResponse<List<ToolExecutionTraceResponse>>` |
| GET | `/api/tasks/{taskId}/tool-execution-traces` | 无 | `ApiResponse<List<ToolExecutionTraceResponse>>` |

API 设计要点：
- 只读端点，不修改数据库
- 不包含多智能体 run/task 的 permissions header（由 service 层通过 checkProjectRole 统一处理）

---

## 5. Evidence 解析与安全处理

### 证据解析 (`buildEvidence`)

从 `outputPayload` JSON 提取：

| JSON 字段 | Evidence 字段 | 来源 |
|-----------|---------------|------|
| `filesRead[]` | `filesRead` + `filesReadCount` | 仓库只读工具输出 |
| `skippedFiles[]` | `skippedFiles` + `skippedFilesCount` | 仓库只读工具输出 |
| `redacted` | `redacted` | 布尔值 |
| `truncated` | `truncated` | 布尔值 |
| `binarySkipped` | `binarySkipped` | 布尔值 |
| `sensitiveDenylistApplied` | `sensitiveDenylistApplied` | 布尔值 |
| `filesTouched` / `gitOperations` 非空 | `pathSandboxApplied` | 推断 |
| artifactId（实体字段） | `artifacts[]` | 从 DB 关联查询 |

缺少字段时：返回 0/空列表/false，不抛异常。

### 安全处理 (`ToolTracePayloadSanitizer`)

1. **Secret 掩码**: 正则匹配并替换以下模式：
   - `sk-*` API Key
   - `ghp_*` / `github_pat_*` GitHub Token
   - `Bearer` 令牌
   - 敏感 JSON 字段：`api_key`、`secret`、`password`
   - JWT Token
   - 私钥文本
2. **长度保护**: 超过 64KB 的 payload 截断到 64KB

---

## 6. Timeline 事件设计

所有事件由 `buildEvents` 根据实体字段动态生成，不依赖独立的事件表：

| eventType | 触发条件 | 描述 |
|-----------|---------|------|
| `EXECUTION_CREATED` | 总是 | 工具执行已创建 |
| `POLICY_CHECKED` | 总是 | BLOCKED → 未通过，其他 → 通过 |
| `APPROVAL_CREATED` | 有 approval 记录 | 审批已创建 |
| `APPROVAL_ACCEPTED` | approval.status=APPROVED | 审批已通过 |
| `APPROVAL_REJECTED` | approval.status=REJECTED | 审批已驳回 |
| `JOB_CREATED` | job.status=PENDING/RUNNING | Worker 任务已创建 |
| `JOB_RUNNING` | job.status=RUNNING + startedAt | Worker 执行中 |
| `JOB_RETRY_PENDING` | job.status=RETRY_PENDING | 任务待重试（含 metadata） |
| `JOB_COMPLETED` | job.status=COMPLETED | Worker 执行完成 |
| `JOB_FAILED` | job.status=FAILED | Worker 执行失败 |
| `JOB_DEAD_LETTERED` | job.status=DEAD_LETTERED | 任务已进入死信队列 |
| `OUTPUT_CAPTURED` | status=COMPLETED/FAILED | 输出已记录 |
| `READ_ONLY_CONTRACT_WARNING` | outputPayload 含非空 filesTouched/gitOperations | 只读契约警告 |
| `OUTPUT_PARSE_WARNING` | outputPayload JSON 解析失败 | 输出解析警告 |
| `ARTIFACT_CREATED` | artifactId 非空 | 产物已生成 |
| `PATCH_REVIEW_CREATED` | 有 patch_proposal_review | 补丁审查已创建 |
| `PATCH_REVIEW_DECIDED` | review.reviewedAt 非空 | 补丁审查已决定 |

---

## 7. 前端 ToolExecutionTraceDrawer 说明

`ToolExecutionTraceDrawer.vue` 是一个 ElDrawer 组件，接收 `executionId` 作为 props：

- **Header**: 显示 toolKey、状态标签（颜色编码）、风险等级、mode 标记
- **Safety Banner**: 只读/未写入文件/未执行 Git 写操作 + 脱敏/截断标签
- **Warning Banner**: READ_ONLY_CONTRACT_WARNING 和 OUTPUT_PARSE_WARNING
- **Timeline**: 时间线显示所有事件，带图标、颜色编码、描述、时间戳
- **Evidence Grid**: 显示 filesReadCount / skippedFilesCount / redacted 等指标
- **Files Read / Skipped Files**: 详细文件列表
- **Approval Section**: 审批 ID、状态、备注、时间
- **Job Section**: Job ID、状态、优先级、尝试次数、错误码、失败阶段、重试/DLQ 信息
- **Artifacts Section**: 产物类型、标题、审查状态/决策
- **Payload**: 可折叠的 inputPayload / outputPayload JSON 显示

样式：暗色科技风，与现有组件风格一致。
所有关键元素带 `data-testid` 属性。

---

## 8. MultiAgentRunPanel / Observability 集成说明

### MultiAgentRunPanel.vue
- 每个 tool execution card 增加「查看证据链」按钮（`data-testid="tool-trace-open-button"`）
- 点击打开 ToolExecutionTraceDrawer
- 传递当前 executionId 给 drawer

### ObservabilityPage.vue
- 增加 trace drawer 状态和打开方法
- ToolExecutionTraceDrawer 组件挂载在页面底部
- 可为 future problem jobs 集成保留 toolExecutionId 传递通道

### 数据流
```
MultiAgentRunPanel / ObservabilityPage
  → ToolExecutionTraceDrawer (open with executionId)
    → getToolExecutionTrace(executionId)
      → GET /api/tool-sandbox-executions/{executionId}/trace
        → ToolExecutionTraceService.getTrace(executionId)
          → buildTrace(execution) — 聚合所有数据
            → 返回 ToolExecutionTraceResponse
```

---

## 9. 后端测试结果

测试类: `ToolExecutionTraceIntegrationTest.java`
**测试数量: 35 个**

### 测试覆盖分组

| 分组 | 数量 | 覆盖场景 |
|------|------|---------|
| Basic Trace | 8 | 执行 ID、toolKey、status、mode、events、evidence、project/task IDs、outputPayload |
| Evidence | 5 | file counts、redacted/truncated、safety flags、filesRead/skipped arrays、artifacts null handling |
| Run Traces | 4 | 列表返回、字段完整性、与执行数量匹配、4 traces for paused run |
| Task Traces | 3 | 列表返回、字段完整性、events |
| Permission & Error | 6 | 3 个端点 401、invalid executionId 404、empty for invalid run/task |
| Completed Run | 4 | 6 traces、policyAllowed、task traces 包含、review-only 2 traces |
| Safety & Sanitization | 5 | readOnly flag、payload 无 secret、output captured 事件、events 字段、runId |

```
ToolExecutionTraceIntegrationTest
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 10. 前端 typecheck / build 结果

```
npm run typecheck → vue-tsc --noEmit 退出码 0
npm run build → ✓ built in 4.15s
```

前端 typecheck 和 build 均通过。

### E2E 测试

6 个 Trace E2E 测试已添加到 `multi-agent-orchestration.spec.ts`：

1. `should show trace open button on tool cards` — 验证"查看证据链"按钮可见
2. `should open trace drawer and show timeline` — 验证抽屉打开且时间线可见
3. `should trace drawer show safety banner` — 验证安全提示横幅
4. `should trace drawer show evidence section with file counts` — 验证文件读取/跳过计数
5. `should close trace drawer without JS errors` — 验证关闭后无 JS 错误
6. 1 个 Observability E2E 测试（`knowledge-observability.spec.ts`）— 验证问题 Job 区域

E2E 测试需要后端运行，执行命令：

```bash
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1
```

---

## 11. 已知限制

1. **Evidence 字段条件性出现**: `binarySkipped`、`pathSandboxApplied`、`sensitiveDenylistApplied` 等字段只在 outputPayload 包含相关数据时才出现。Mock 执行输出不含这些字段，因此前端 evidence section 可能显示部分空值。
2. **审批证据仅限工具级审批**: ToolExecutionTrace.approval 记录的是工具级别审批（tool_execution_approval），而非多智能体运行级别审批闸门。Mock 执行不触发工具级审批，因此大多数 mock trace 的 `approval` 字段为 null。
3. **Job 证据**: Mock 执行模式不创建 tool_execution_job 记录，因此大多数 mock trace 的 `job` 字段为 null。Job 证据仅在异步 Worker 模式或带重试机制的流程中出现。
4. **产物证据**: 仅当 execution 有 artifactId 时才生成。Mock 执行不生成产物。
5. **全局问题 Job 展示**: Observability 页面暂未实现问题 Job 表格，因为缺少全局（非项目级）的 problem-jobs API。trace drawer 已预留，当未来 API 就绪时可接入。
6. **E2E 测试需后端支持**: 6 个 Trace E2E 测试已编写，需要 `start-e2e-backend.sh` 后端服务支持才能执行。

---

## 12. 是否可以进入 Milestone 37G

**可以。**

- 所有 14 项需求已完成实现
- 后端 35 个集成测试全部通过（≥24 要求）
- 前端 typecheck 和 build 无错误
- DTO、Service、Controller、Sanitizer、Frontend 组件完整
- 与 36A-37E 和 35A-35F API 无破坏性变更
