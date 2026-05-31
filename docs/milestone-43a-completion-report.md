# Milestone 43A — Governance Copilot Workspace & Guided Operations Console 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V53__init_governance_copilot_workspace_tables.sql` | 3 张新表迁移 |
| `GovernanceWorkspaceSessionStatus.java` | 会话状态枚举（ACTIVE/PAUSED/COMPLETED/ARCHIVED） |
| `GovernanceWorkspaceFocusMode.java` | 聚焦模式枚举（PRIORITY_FIRST/OWNER_CENTRIC/PROJECT_CENTRIC/WAIVER_REDUCTION/BACKLOG_REDUCTION） |
| `GovernanceGuidedTaskType.java` | 引导任务类型枚举（TRIAGE_RECOMMENDATION/RUN_PLAYBOOK/APPLY_RECIPE_GUIDANCE/PREPARE_HANDOFF/REVIEW_WAIVER/REDUCE_BACKLOG） |
| `GovernanceGuidedTaskStatus.java` | 任务状态枚举（OPEN/IN_PROGRESS/DONE/SKIPPED/BLOCKED） |
| `GovernanceNextStepSuggestionType.java` | 下一步建议类型枚举（OPEN_PLAYBOOK/OPEN_RECIPE/OPEN_KNOWLEDGE/START_HANDOFF/REVIEW_WAIVER/REVIEW_FORECAST） |
| 3 个实体 | GovernanceWorkspaceSessionEntity, GovernanceGuidedTaskEntity, GovernanceNextStepRecommendationEntity |
| 3 个 Mapper | WorkspaceSession, GuidedTask, NextStepRecommendation |
| 3 个 DTO | WorkspaceSessionResponse, GuidedTaskResponse, NextStepRecommendationResponse |
| `GovernanceWorkspaceService.java` | 工作台会话服务 |
| `GovernanceGuidedOperationsService.java` | 引导任务服务 |
| `GovernanceNextStepRecommendationService.java` | 下一步建议服务 |
| `GovernanceWorkspaceController.java` | 12 个 API 端点 |
| `GovernanceWorkspaceCopilotIntegrationTest.java` | 36 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V53 测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceWorkspaceConsole.vue` | Copilot 工作台面板 |
| `GovernanceGuidedTaskPanel.vue` | 引导任务面板 |
| `GovernanceNextStepPanel.vue` | 下一步建议面板 |
| `governance-workspace.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 43A 接口（12+ API 函数 + 3 数据接口） |
| `ObservabilityPage.vue` | 新增 43A Copilot 工作台区块 |

## 2. 三张 Workspace/GuidedTask/NextStep 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_workspace_session` | 工作台会话 | operator_id/name, session_status, focus_mode, selected_project/recommendation/owner_id, started_at, ended_at |
| `governance_guided_task` | 引导任务 | session_id, recommendation_id, task_type, priority, task_status, title, linked_playbook_key/recipe_key/knowledge_entry_id, due_at |
| `governance_next_step_recommendation` | 下一步建议 | session_id, guided_task_id, recommendation_id, suggestion_rank, suggestion_type, title, rationale, expected_outcome, action_payload_json |

## 3. GovernanceWorkspaceService 设计说明

**职责**：管理 workspace 会话的 CRUD 与状态流转。

**状态机**：
```text
ACTIVE → PAUSED → ACTIVE
ACTIVE → COMPLETED → ARCHIVED
```

支持 focus mode 切换（PRIORITY_FIRST/OWNER_CENTRIC/PROJECT_CENTRIC/WAIVER_REDUCTION/BACKLOG_REDUCTION），自动创建默认 ACTIVE 会话。

## 4. GovernanceGuidedOperationsService 设计说明

**职责**：基于 recommendation 数据生成引导式任务列表。

- 从 open items 生成 guided tasks（分类/执行 playbook/应用 recipe）
- 按 priority + overdue 排序
- 自动链接 playbook/recipe（blocked→playbook, guardrail→recipe）

**任务状态机**：
```text
OPEN → IN_PROGRESS → DONE
OPEN → SKIPPED / BLOCKED
BLOCKED → IN_PROGRESS
```

## 5. GovernanceNextStepRecommendationService 设计说明

**职责**：生成 3-5 条 context-aware 下一步建议。

**生成规则**：
| 条件 | 建议类型 | 优先级 |
|------|----------|--------|
| P0/P1 recommendation | OPEN_PLAYBOOK | 1 |
| 有活跃 waiver | REVIEW_WAIVER | 2 |
| blocked > 0 | START_HANDOFF | 3 |
| overdue > 0 | REVIEW_FORECAST | 4 |
| 兜底 | OPEN_KNOWLEDGE | 5 |

## 6-8. 三个前端面板

**GovernanceWorkspaceConsole**：MetricTile（开放任务/进行中/阻塞），活跃会话信息（ID/聚焦模式/状态），Focus Mode 选择器，会话控制按钮（暂停/恢复），下一步建议卡片列表（标题 + rationale）。

**GovernanceGuidedTaskPanel**：引导任务列表（状态标签、任务类型标签、优先级标签），Playbook/Recipe 链接信息，状态流转按钮（开始→完成/阻塞/跳过→继续）。

**GovernanceNextStepPanel**：下一步建议卡片（类型标签、标题、rationale、预期效果），绿色预期效果文本。

## 9. Workspace/Copilot 边界说明

**已实现**：
- Workspace 会话 CRUD + 状态流转 + Focus Mode
- Guided task 生成 + 状态流转 + playbook/recipe 链接
- Context-aware next-step recommendation（3-5 条）
- Dashboard/report 导出

**不涉及**：
- 不自动修改 recommendation/waiver/execution/recipe/knowledge 原始记录
- 不自动批准 waiver/完成 recommendation/分配 owner
- 不调用 AI 执行治理动作
- Next-step 只做导航建议，不做真实操作

## 10. 后端测试结果

**36 个 43A 测试 + 36 个 42C + 36 个 42B + 36 个 42A + 34 个 41C + 30 个 41B + 30 个 41A + 26 个 40C + 36 个 40B + 32 个 40A = 332/332 全部通过**

**Workspace Session（10 个）**：创建、更新、状态流转（ACTIVE→PAUSED→ACTIVE→COMPLETED→ARCHIVED）、非法状态拒绝、刷新、列表、按 ID 查询

**Guided Tasks（9 个）**：生成、状态流转（OPEN→IN_PROGRESS→DONE/BLOCKED→IN_PROGRESS、OPEN→SKIPPED）、非法状态拒绝、linked 字段

**Next-Step Recommendations（7 个）**：生成、计数 0-5、suggestionType、OPEN_PLAYBOOK、REVIEW_FORECAST、rationale、expectedOutcome

**Dashboard（6 个）**：active session、open/inProgress/blocked 计数、next-steps、focus mode、topGuidedTasks

**Edge Cases（4 个）**：空数据 dashboard、幂等刷新、session 列表、contextSummary

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式 |

## 12. 已知限制

1. **Guided task 基于推荐事项生成**：不在 workspace session 中保存历史 task 状态变更记录
2. **Next-step recommendation 为规则法**：不基于 operator 历史行为学习
3. **Focus mode 尚未深度影响 task 排序**：当前 focus mode 更多作为元数据标记
4. **E2E 环境依赖**：与之前 milestone 一致的 graceful fallback 模式

## 13. 是否可以进入 Milestone 43B

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V53 migration + test schema）
2. ✅ Workspace session 可创建/更新/状态流转
3. ✅ Guided task 可生成/更新/状态流转
4. ✅ Next-step recommendation 可生成/查询
5. ✅ Dashboard/report 可导出
6. ✅ 332 个后端集成测试全部通过（40A→43A）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ 工作台导航和建议逻辑清晰、可解释、可追踪

建议 43B 方向：Governance Operator Productivity, Session Insights & Action Reuse，包括 operator session productivity metrics、common action sequence mining、next-step success feedback、reusable action bundles、workspace session insights。
