# Milestone 40C — Governance Recommendation Workflow & Exception Waiver Management 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V46__init_governance_workflow_waiver_tables.sql` | 3 张新表迁移 |
| `GovernanceWorkflowStatus.java` | 工作流状态枚举（OPEN/ACKNOWLEDGED/IN_PROGRESS/BLOCKED/COMPLETED/REJECTED） |
| `GovernanceWaiverStatus.java` | Waiver 状态枚举（REQUESTED/APPROVED/REJECTED/EXPIRED/REVOKED） |
| `GovernanceWaiverScope.java` | Waiver 范围枚举（PROJECT_RELEASE/POLICY_EXCEPTION/TEMPORARY_SIGNOFF_GAP/ROLLBACK_READINESS_EXCEPTION） |
| `GovernanceWorkflowPriority.java` | 工作流优先级枚举（P0/P1/P2/P3） |
| `GovernanceRecommendationItemEntity.java` | 推荐事项实体 |
| `GovernanceWaiverRequestEntity.java` | Waiver 申请实体 |
| `GovernanceWorkflowSnapshotEntity.java` | 工作流快照实体 |
| 3 个 Mapper | GovernanceRecommendationItemMapper, GovernanceWaiverRequestMapper, GovernanceWorkflowSnapshotMapper |
| 11 个 DTO | GovernanceRecommendationItemResponse, Create/UpdateRecommendationItemRequest, GovernanceWaiverRequestResponse, Create/UpdateWaiverRequestRequest, GovernanceWorkflowSnapshotResponse, GovernanceWorkflowDashboardResponse, GovernanceWorkflowSummaryResponse |
| `GovernanceRecommendationWorkflowService.java` | 推荐事项工作流服务 |
| `GovernanceWaiverManagementService.java` | Waiver 管理服务 |
| `GovernanceWorkflowSummaryService.java` | 工作流汇总服务 |
| `GovernanceWorkflowController.java` | 14 个 API 端点 |
| `GovernanceWorkflowWaiverIntegrationTest.java` | 26 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V46 三张测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceRecommendationWorkflowPanel.vue` | 推荐事项工作流面板 |
| `GovernanceWaiverPanel.vue` | Waiver 管理面板 |
| `GovernanceWorkflowSummaryPanel.vue` | 工作流概览面板 |
| `governance-workflow.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 40C 接口（20+ API 函数 + 数据接口） |
| `ObservabilityPage.vue` | 新增 40C 治理工作流区块 |

## 2. 三张 Governance Workflow/Waiver 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_recommendation_item` | 治理推荐事项 | project_id/name, source_snapshot_date, policy_key, guardrail_key, category, priority, workflow_status(OPEN/ACKNOWLEDGED/IN_PROGRESS/BLOCKED/COMPLETED/REJECTED), owner, due_at, resolved_at, resolution_note, UNIQUE KEY(project_id, snapshot_date, policy_key, guardrail_key) |
| `governance_waiver_request` | Waiver 申请 | recommendation_id, project_id, waiver_status(REQUESTED/APPROVED/REJECTED/EXPIRED/REVOKED), waiver_scope, requested_by/approved_by, reason_text, approval_note, expires_at, revoked_at |
| `governance_workflow_snapshot` | 工作流快照 | snapshot_date, total/open/in_progress/completed/blocked/overdue counts, active/expired waiver counts, completion_rate, overdue_rate |

## 3. GovernanceRecommendationWorkflowService 设计说明

**职责**：管理 governance recommendation 的完整工作流生命周期。

**核心方法**：
- `syncRecommendations()` — 从 40B guardrail 获取 recommendation，仅同步 P0/P1 优先级事项，按 UNIQUE KEY 去重
- `listItems()` / `getItem()` / `updateItem()` — 标准 CRUD，支持 status/priority 过滤
- `updateItemStatus()` — 状态流转引擎，验证合法转换

**状态机**：
```text
OPEN → ACKNOWLEDGED → IN_PROGRESS → COMPLETED
                  ↘→ BLOCKED
OPEN → REJECTED
BLOCKED → IN_PROGRESS
```

**设计要点**：同步时自动设置 dueAt=7 天后，状态变更时同步设置 resolvedAt，响应包含关联的 waiverStatus。

## 4. GovernanceWaiverManagementService 设计说明

**职责**：管理 waiver 的申请、审批、到期检测。

**核心方法**：
- `createWaiver()` — 创建 waiver（验证 recommendation 存在 + 无活跃 waiver，同时间只允许一个 active waiver）
- `listWaivers()` — 按 recommendation 查询历史 waiver
- `updateWaiver()` / `updateWaiverStatus()` — 更新/状态变更
- `scanExpiredWaivers()` — 批量扫描到期 waiver 并标记 EXPIRED

**Waiver 状态机**：
```text
REQUESTED → APPROVED
REQUESTED → REJECTED
APPROVED → EXPIRED
APPROVED → REVOKED
```

**边界**：waiver 只记录治理例外，不自动修改 guardrail 结果或 recommendation 状态。

## 5. GovernanceWorkflowSummaryService 设计说明

**职责**：生成工作流快照与治理统计。

**核心方法**：
- `refreshSnapshot()` — 删除当日快照 → 统计所有 recommendation 的状态分布 → 计算 completion/overdue rate → 插入快照
- `getDashboard()` — 最新快照数据 + 按优先级排序的 top items + 逾期 items
- `getSummary()` — 完整摘要 + Markdown 格式的治理报告

**计算公式**：
- Completion Rate = completedCount / totalCount × 100
- Overdue Rate = overdueCount / totalCount × 100
- Overdue 判定：status in (OPEN/ACKNOWLEDGED/IN_PROGRESS/BLOCKED) AND dueAt < now()

## 6-8. 三个前端面板

**GovernanceRecommendationWorkflowPanel**：推荐事项列表（优先级/状态标签、责任人、截止日、逾期高亮），状态流转按钮（确认→开始→完成/阻塞/拒绝），解决备注对话框。

**GovernanceWaiverPanel**：按 recommendation 展示 waiver 列表，申请 waiver 对话框（范围选择、原因、到期时间），审批/拒绝/撤销按钮。

**GovernanceWorkflowSummaryPanel**：MetricTile 指标卡（总事项/开放/处理中/已完成/阻塞/逾期/waiver/完成率/逾期率），高优先级事项列表，逾期事项列表（红色高亮），复制 Markdown 报告按钮。

## 9. Workflow/Waiver 边界说明

**已实现**：
- Recommendation 从 40B guardrail 同步为可跟踪的工作流事项
- 状态流转、责任人分配、截止日期、解决备注
- Waiver 申请/审批/拒绝/撤销/到期扫描
- 工作流快照、完成率/逾期率统计、Markdown 报告

**不涉及**：
- 不自动修改 release/rollout/sign-off/verification 原始记录
- 不自动关闭 recommendation（人工完成）
- 不自动批准 waiver（人工审批）
- Waiver 不自动将 guardrail 变为 PASS（只记录例外）
- 不调用 AI 生成治理结论

## 10. 后端测试结果

**26 个新测试 + 32 个 40A 测试 + 36 个 40B 测试 = 94/94 全部通过**

**推荐事项（9 个）**：同步成功、幂等去重、创建/更新/分配责任人、按 ID 查询、状态流转（OPEN→ACKNOWLEDGED→IN_PROGRESS→COMPLETED）、非法状态拒绝

**Waiver（5 个）**：创建、批准、拒绝、撤销、单活跃 waiver 约束

**快照/看板（6 个）**：刷新、计数、完成率、top priority、Markdown 导出、状态过滤

**边界（6 个）**：空数据看板、备注持久化、waiver 列表查询、逾期统计、到期扫描

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式，无 seeded 数据时跳过 |

## 12. 已知限制

1. **Recommendation 同步只含 P0/P1**：40B 的 P2/P3 建议不会进入工作流，后续可配置阈值
2. **Waiver 到期无自动提醒**：`scanExpiredWaivers()` 需手动调用或定时触发
3. **Owner 不校验存在性**：当前 ownerId 为自由输入，不关联用户系统
4. **E2E 环境依赖**：与 40A/40B 一致的 graceful fallback 模式
5. **Completion rate 含 REJECTED**：已拒绝事项不计入完成，可能影响完成率观感

## 13. 是否可以进入 Milestone 41A

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V46 migration + test schema）
2. ✅ Recommendation workflow 可同步/更新/状态流转
3. ✅ Waiver 可申请/审批/拒绝/过期/撤销
4. ✅ Workflow dashboard 展示 completion/overdue/waiver 统计
5. ✅ Summary/report 可导出 Markdown
6. ✅ 94 个后端集成测试全部通过（40A+40B+40C）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ Recommendation 与 waiver 关系清晰（一对多，同时间单 active waiver）

建议 41A 方向：Governance SLA, Escalation & Ownership Health，包括 recommendation SLA、overdue escalation、owner load/health、waiver expiry alert。
