# Milestone 40C: Governance Recommendation Workflow & Exception Waiver Management

## 1. 背景

截至 Milestone 40B，平台已经具备组织级治理能力：

```text
40A
  Multi-project Release Governance
  -> Portfolio Snapshot
  -> Governance Baseline Template
  -> Risk Heatmap

40B
  Organization Trial Policy
  -> Guardrail Evaluation
  -> Portfolio Drift Detection
  -> Governance Recommendation
```

现在系统已经能回答：

```text
哪些项目违反了组织级 guardrail？
哪些项目需要优先治理？
哪些项目 drift 风险最高？
哪些推荐事项优先级最高？
```

但治理要真正落地，还缺最关键的一层“执行闭环”：

```text
推荐事项被谁接收？
是否有人确认、处理中、已完成、已拒绝？
如果项目因为业务原因需要临时例外，怎么申请 waiver？
waiver 谁批准？多久到期？过期后是否重新进入治理队列？
治理完成率、逾期率、waiver 覆盖率怎么衡量？
```

换句话说，40B 让平台具备了：

```text
识别风险与给出建议
```

但还没有：

```text
recommendation workflow + exception / waiver management
```

Milestone 40C 的目标就是新增：

```text
Governance Recommendation Workflow & Exception Waiver Management
```

让平台从：

```text
能发现问题、提示问题
```

升级为：

```text
能追踪问题怎么被处理、哪些是批准的例外、哪些已经逾期或失效
```

---

## 2. 总目标

实现治理建议闭环与例外管理：

1. 新增 Governance Recommendation Item 数据模型。
2. 新增 Governance Waiver Request 数据模型。
3. 新增 Governance Workflow Snapshot 数据模型。
4. 支持把 40B recommendation 落地为可跟踪的 workflow item。
5. 支持 recommendation 的状态流转与责任人记录。
6. 支持 waiver 的申请、审批、拒绝、到期、失效。
7. 支持 recommendation 与 waiver 的关联。
8. 支持统计治理完成率、逾期率、waiver 活跃量。
9. 支持导出 Governance Workflow Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
recommendation 只是看板上的一条建议
```

升级为：

```text
recommendation 是一个有状态、有责任人、有例外、有到期治理的闭环事项
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改已有 release / rollout / sign-off / verification 原始记录。
4. 不自动关闭 incident / alert / feedback / recommendation。
5. 不自动批准 waiver。
6. 不自动替代人工审批或签字。
7. 不调用真实 AI 自动生成治理结论。
8. waiver 只作为治理例外记录，不自动更改 guardrail evaluation 原始结果。
9. recommendation workflow 只管理状态、责任人与说明，不执行基础设施动作。
10. 不破坏 1-40B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 recommendation item / waiver request / workflow snapshot 表。
2. 从 40B guardrail evaluation 生成或同步 workflow item。
3. 新增 recommendation 状态流转、waiver 审批与到期状态。
4. 新增 dashboard、列表、详情、summary、export。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V46__init_governance_workflow_waiver_tables.sql
```

### 4.1 governance_recommendation_item

```sql
CREATE TABLE governance_recommendation_item (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    source_snapshot_date DATE NOT NULL,
    policy_key VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    workflow_status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL,
    due_at DATETIME NULL,
    resolved_at DATETIME NULL,
    resolution_note TEXT NULL,
    source_evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_recommendation_project(project_id, workflow_status),
    KEY idx_governance_recommendation_priority(priority, workflow_status),
    KEY idx_governance_recommendation_due(due_at, workflow_status),
    UNIQUE KEY uk_governance_recommendation_source(project_id, source_snapshot_date, policy_key, guardrail_key)
);
```

### 4.2 governance_waiver_request

```sql
CREATE TABLE governance_waiver_request (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    waiver_status VARCHAR(32) NOT NULL,
    waiver_scope VARCHAR(64) NOT NULL,
    requested_by BIGINT NULL,
    requested_by_name VARCHAR(128) NULL,
    approved_by BIGINT NULL,
    approved_by_name VARCHAR(128) NULL,
    reason_text TEXT NOT NULL,
    approval_note TEXT NULL,
    expires_at DATETIME NULL,
    revoked_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_waiver_project(project_id, waiver_status),
    KEY idx_governance_waiver_recommendation(recommendation_id),
    KEY idx_governance_waiver_expiry(expires_at, waiver_status)
);
```

### 4.3 governance_workflow_snapshot

```sql
CREATE TABLE governance_workflow_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    total_recommendation_count INT NOT NULL DEFAULT 0,
    open_recommendation_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    blocked_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0,
    active_waiver_count INT NOT NULL DEFAULT 0,
    expired_waiver_count INT NOT NULL DEFAULT 0,
    completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    overdue_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_workflow_snapshot_date(snapshot_date)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceWorkflowStatus.java
GovernanceWaiverStatus.java
GovernanceWaiverScope.java
GovernanceWorkflowPriority.java
```

### 5.1 GovernanceWorkflowStatus

```text
OPEN
ACKNOWLEDGED
IN_PROGRESS
BLOCKED
COMPLETED
REJECTED
```

### 5.2 GovernanceWaiverStatus

```text
REQUESTED
APPROVED
REJECTED
EXPIRED
REVOKED
```

### 5.3 GovernanceWaiverScope

```text
PROJECT_RELEASE
POLICY_EXCEPTION
TEMPORARY_SIGNOFF_GAP
ROLLBACK_READINESS_EXCEPTION
```

### 5.4 GovernanceWorkflowPriority

```text
P0
P1
P2
P3
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceRecommendationItemEntity.java
GovernanceWaiverRequestEntity.java
GovernanceWorkflowSnapshotEntity.java

GovernanceRecommendationItemMapper.java
GovernanceWaiverRequestMapper.java
GovernanceWorkflowSnapshotMapper.java
```

DTO 建议：

```text
GovernanceRecommendationItemResponse.java
CreateGovernanceRecommendationItemRequest.java
UpdateGovernanceRecommendationItemRequest.java

GovernanceWaiverRequestResponse.java
CreateGovernanceWaiverRequestRequest.java
UpdateGovernanceWaiverRequestRequest.java

GovernanceWorkflowSnapshotResponse.java
GovernanceWorkflowDashboardResponse.java
GovernanceWorkflowSummaryResponse.java
```

### 6.1 GovernanceWorkflowDashboardResponse

建议字段：

```text
snapshotDate
totalRecommendationCount
openRecommendationCount
inProgressCount
completedCount
blockedCount
overdueCount
activeWaiverCount
expiredWaiverCount
completionRate
overdueRate
topPriorityItems
topOverdueItems
```

### 6.2 GovernanceRecommendationItemResponse

建议字段：

```text
id
projectId
projectName
sourceSnapshotDate
policyKey
guardrailKey
category
priority
workflowStatus
title
summary
ownerId
ownerName
dueAt
resolvedAt
resolutionNote
waiverStatus
createTime
updateTime
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceRecommendationWorkflowService.java
GovernanceWaiverManagementService.java
GovernanceWorkflowSummaryService.java
```

### 7.1 GovernanceRecommendationWorkflowService

职责：

1. 从 40B recommendation 生成或同步 recommendation item。
2. 管理 item 的 CRUD。
3. 支持状态流转：

```text
OPEN -> ACKNOWLEDGED -> IN_PROGRESS -> COMPLETED
                    \-> BLOCKED
OPEN -> REJECTED
BLOCKED -> IN_PROGRESS
```

4. 支持分配 owner。
5. 支持记录 dueAt / resolutionNote。
6. 自动判定 overdue。

### 7.2 GovernanceWaiverManagementService

职责：

1. 创建 waiver request。
2. 审批 / 拒绝 / 撤销 waiver。
3. 检查 waiver 到期并标记 `EXPIRED`。
4. 将 waiver 状态回填到 recommendation item 视图。
5. 支持一个 recommendation 对应多个 waiver 记录，但同一时间只允许一个 active waiver。

waiver 状态机：

```text
REQUESTED -> APPROVED
REQUESTED -> REJECTED
APPROVED -> EXPIRED
APPROVED -> REVOKED
```

### 7.3 GovernanceWorkflowSummaryService

职责：

1. 生成 workflow snapshot。
2. 统计 completion rate / overdue rate。
3. 输出 top priority / overdue items。
4. 导出 Markdown summary：

```text
开放事项总数
已完成率
逾期率
活动 waiver 数
最高优先级事项
最需要关注的阻塞项
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceWorkflowController.java
```

建议端点：

### 8.1 Recommendation Workflow

```text
POST   /api/governance-workflow/recommendations/sync
GET    /api/governance-workflow/recommendations
GET    /api/governance-workflow/recommendations/{itemId}
PUT    /api/governance-workflow/recommendations/{itemId}
POST   /api/governance-workflow/recommendations/{itemId}/status
```

### 8.2 Waiver

```text
POST   /api/governance-workflow/recommendations/{itemId}/waivers
GET    /api/governance-workflow/recommendations/{itemId}/waivers
PUT    /api/governance-workflow/waivers/{waiverId}
POST   /api/governance-workflow/waivers/{waiverId}/status
POST   /api/governance-workflow/waivers/scan-expiry
```

### 8.3 Summary / Snapshot

```text
POST   /api/governance-workflow/snapshots/refresh
GET    /api/governance-workflow/dashboard
GET    /api/governance-workflow/summary
GET    /api/governance-workflow/report
```

权限建议：

```text
查看：ADMIN
编辑 workflow：ADMIN
审批 waiver：ADMIN
refresh snapshot：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Recommendation Sync

建议将 40B 中以下 recommendation 同步为 workflow item：

```text
severity = HIGH / CRITICAL
priority = P0 / P1
evaluationStatus = BLOCK 或持续 WARN
```

### 9.2 Overdue 判定

当满足以下条件时视为 overdue：

```text
workflowStatus in (OPEN, ACKNOWLEDGED, IN_PROGRESS, BLOCKED)
and dueAt < now()
```

### 9.3 Completion Rate

```text
completed_count / total_recommendation_count * 100
```

### 9.4 Waiver 生效说明

waiver 只表示：

```text
该 recommendation 在一定时间内被批准为例外
```

不表示：

```text
guardrail 自动 PASS
或 recommendation 自动 CLOSED
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceRecommendationWorkflowPanel.vue
GovernanceWaiverPanel.vue
GovernanceWorkflowSummaryPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceRecommendationWorkflowPanel

展示：

1. recommendation item 列表
2. priority / status / owner / dueAt
3. create / edit dialog
4. status 流转按钮
5. overdue 高亮

### 10.2 GovernanceWaiverPanel

展示：

1. 当前 item 的 waiver 列表
2. create waiver dialog
3. approve / reject / revoke 按钮
4. expiresAt / status / reason / approval note

### 10.3 GovernanceWorkflowSummaryPanel

展示：

1. workflow 概览指标卡
2. completion rate / overdue rate
3. top priority items
4. top overdue items
5. summary text / export 按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 推荐事项列表要突出优先级与逾期状态
4. waiver 面板要清楚区分已批准、已拒绝、已过期

---

## 11. 后端测试要求

新增：

```text
GovernanceWorkflowWaiverIntegrationTest.java
```

不少于 32 个集成测试，建议覆盖：

1. sync recommendations success
2. duplicate sync idempotent
3. create recommendation item success
4. update recommendation item success
5. assign owner success
6. status open -> acknowledged
7. status acknowledged -> in_progress
8. status in_progress -> completed
9. status blocked -> in_progress
10. invalid status transition reject
11. dueAt overdue detection
12. create waiver request success
13. approve waiver success
14. reject waiver success
15. revoke waiver success
16. waiver expires after scan
17. only one active waiver allowed
18. recommendation item includes waiver status
19. dashboard counts correct
20. completion rate correct
21. overdue rate correct
22. top priority items returned
23. top overdue items returned
24. markdown report export success
25. unauthorized access reject
26. non-admin update reject
27. sync only high-priority recommendations
28. blocked item remains open with waiver approved
29. waiver expiry does not auto-complete item
30. recommendation resolution note persisted
31. empty dataset returns empty dashboard
32. snapshot refresh success

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-workflow.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 workflow summary panel
2. recommendation workflow 列表渲染
3. waiver panel 渲染
4. create waiver dialog works
5. recommendation 状态按钮可见
6. overdue / priority 标签可见
7. refresh / export 按钮可见
8. no JS errors on page load

如果测试环境没有 seeded workflow 数据：

1. 显式断言空态
2. 不把“无 workflow 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-40c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance workflow / waiver 表说明
3. GovernanceRecommendationWorkflowService 设计说明
4. GovernanceWaiverManagementService 设计说明
5. GovernanceWorkflowSummaryService 设计说明
6. GovernanceRecommendationWorkflowPanel 说明
7. GovernanceWaiverPanel 说明
8. GovernanceWorkflowSummaryPanel 说明
9. Workflow / Waiver 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41A

---

## 14. 验收标准

必须全部满足：

1. governance_recommendation_item / governance_waiver_request / governance_workflow_snapshot 三张表已落库
2. recommendation workflow 可同步 / 更新 / 状态流转
3. waiver 可申请 / 审批 / 拒绝 / 过期 / 撤销
4. workflow dashboard 可展示 completion / overdue / waiver 统计
5. summary / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. recommendation 与 waiver 的关系清晰可追踪

---

## 15. 完成后的价值

完成 40C 后，平台将从：

```text
能发现问题、给出建议
```

升级为：

```text
能追踪建议如何被处理、哪些是正式批准的例外、哪些已经逾期失效
```

这一步会把组织级治理从“看板”推进成“工作流系统”，让治理真正形成闭环。

---

## 16. 后续建议

Milestone 40C 完成后，建议进入：

```text
Milestone 41A: Governance SLA, Escalation & Ownership Health
```

重点可包括：

1. recommendation SLA
2. overdue escalation
3. owner load / owner health
4. waiver expiry alert
5. governance throughput metrics

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 40C。

文档路径：
docs/milestone-40c-governance-recommendation-workflow-exception-waiver-management.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 40B 组织级 policy / guardrail / drift 基础上，新增 recommendation workflow 与 exception waiver management。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改已有 release / rollout / sign-off / verification 原始记录。
6. 不要自动关闭 recommendation / incident / alert / feedback。
7. 不要自动批准 waiver。
8. 不要调用真实 AI 自动生成治理结论。
9. waiver 只表示治理例外，不自动把 guardrail 变成 PASS。
10. 不要破坏 1-40B 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V46__init_governance_workflow_waiver_tables.sql。
2. 新增 governance_recommendation_item / governance_waiver_request / governance_workflow_snapshot 三张表。
3. 新增 4 个枚举：
   - GovernanceWorkflowStatus
   - GovernanceWaiverStatus
   - GovernanceWaiverScope
   - GovernanceWorkflowPriority
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceRecommendationWorkflowService。
6. 新增 GovernanceWaiverManagementService。
7. 新增 GovernanceWorkflowSummaryService。
8. 新增 API：
   - recommendation sync / list / get / update / status
   - waiver create / list / update / status / scan-expiry
   - workflow snapshot refresh / dashboard / summary / report
9. 前端新增：
   - GovernanceRecommendationWorkflowPanel.vue
   - GovernanceWaiverPanel.vue
   - GovernanceWorkflowSummaryPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 32 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-40c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance workflow / waiver 表说明
3. GovernanceRecommendationWorkflowService 设计说明
4. GovernanceWaiverManagementService 设计说明
5. GovernanceWorkflowSummaryService 设计说明
6. GovernanceRecommendationWorkflowPanel 说明
7. GovernanceWaiverPanel 说明
8. GovernanceWorkflowSummaryPanel 说明
9. Workflow / Waiver 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41A

现在开始实现，不要只给计划。
```
