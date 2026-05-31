# Milestone 43C — Governance Adaptive Guidance, Operator Feedback & Copilot Tuning Loop 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V55__init_governance_copilot_tuning_tables.sql` | 3 张新表迁移 |
| `GovernanceFeedbackTargetType.java` | 反馈目标类型枚举（NEXT_STEP/GUIDED_TASK/REUSE_BUNDLE/WORKSPACE_SESSION） |
| `GovernanceFeedbackReasonCode.java` | 反馈原因枚举（8 种：HELPFUL/TOO_GENERIC/NOT_RELEVANT 等） |
| `GovernanceAdaptiveSignalType.java` | 自适应信号类型枚举（5 种：SUGGESTION_TYPE_WEIGHT/FOCUS_MODE_WEIGHT 等） |
| `GovernanceAdaptiveSignalLevel.java` | 信号等级枚举（BOOST/KEEP/WATCH/DOWNRANK） |
| `GovernanceCopilotTuningWindow.java` | 调优窗口枚举（DAY_7/14/30） |
| 3 个实体 | OperatorFeedbackEntity, AdaptiveGuidanceSignalEntity, CopilotTuningSnapshotEntity |
| 3 个 Mapper | Feedback, AdaptiveSignal, TuningSnapshot |
| 3 个 DTO | FeedbackResponse, AdaptiveSignalResponse, TuningSnapshotResponse |
| `GovernanceOperatorFeedbackService.java` | Operator 反馈服务 |
| `GovernanceAdaptiveGuidanceService.java` | 自适应引导服务 |
| `GovernanceCopilotTuningService.java` | Copilot 调优服务 |
| `GovernanceCopilotTuningController.java` | 11 个 API 端点 |
| `GovernanceCopilotTuningIntegrationTest.java` | 36 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V55 测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceOperatorFeedbackPanel.vue` | Operator 反馈面板 |
| `GovernanceAdaptiveGuidancePanel.vue` | 自适应引导面板 |
| `GovernanceCopilotTuningPanel.vue` | Copilot 调优面板 |
| `governance-copilot-tuning.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 43C 接口（12+ API 函数 + 3 数据接口） |
| `ObservabilityPage.vue` | 新增 43C Copilot 调优区块 |

## 2. 三张 Feedback/Adaptive Guidance/Tuning Snapshot 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_operator_feedback` | Operator 显式反馈 | session_id, feedback_target_type, feedback_rating(1-5), helpful_flag, accepted_flag, reason_code(8 种), note_text |
| `governance_adaptive_guidance_signal` | 自适应引导信号 | signal_type(5 种), focus_mode, suggestion_type, acceptance_rate, completion_rate, avg_feedback_rating, weight_score, signal_level(BOOST/KEEP/WATCH/DOWNRANK), rationale_text |
| `governance_copilot_tuning_snapshot` | Copilot 调优快照 | snapshot_window(DAY_14), total_feedback_count, acceptance/dismissal_rate, avg_feedback_rating, top/weakest_suggestion_type, tuning_confidence_score |

## 3. GovernanceOperatorFeedbackService 设计说明

**职责**：记录 operator 对 next-step/bundle/task/session 的显式反馈。

- 采集 rating(1-5)、helpfulFlag、acceptedFlag、reasonCode(8 种)
- 支持按 session 查询、全量列表

## 4. GovernanceAdaptiveGuidanceService 设计说明

**职责**：聚合 feedback 数据，生成 5 种 adaptive signal。

**公式**：
```
weightScore = acceptanceRate×0.4 + completionRate×0.3 + avgRating×12 - dismissalRate×0.25
```

**分级**：≥80 BOOST, ≥55 KEEP, ≥30 WATCH, <30 DOWNRANK

**Signal 类型**：SUGGESTION_TYPE_WEIGHT（按建议类型统计）、FOCUS_MODE_WEIGHT（按聚焦模式）、DISMISSAL_RISK_SIGNAL（忽略风险检测）

**Dashboard**：signalCount, boostCount, downrankCount

## 5. GovernanceCopilotTuningService 设计说明

**职责**：生成 tuning snapshot 与 report。

**调优置信度公式**：
```
tuningConfidenceScore = min(feedbackCount, 50)×1.2 + acceptanceRate×0.25 + avgRating×8
```

计算 top/weakest suggestion type，输出 Markdown report。

## 6-8. 三个前端面板

**GovernanceOperatorFeedbackPanel**：反馈列表（目标类型标签、有帮助/无帮助标签、评分、原因码）。

**GovernanceAdaptiveGuidancePanel**：信号列表（BOOST/DOWNRANK/KEEP/WATCH 标签、类型标签、权重分、rationale 解释）。

**GovernanceCopilotTuningPanel**：MetricTile（反馈数/接受率/忽略率/评分/置信度），最佳/最弱类型展示，快照计数。

## 9. Copilot Tuning/Adaptive Guidance 边界说明

**已实现**：
- Feedback 记录（rating/helpful/accepted/reasonCode）
- 5 种 adaptive signal（BOOST/DOWNRANK 含 rationale 解释）
- Tuning snapshot（confidence score、top/weakest type）
- Dashboard/report 导出

**不涉及**：
- 不自动修改 recommendation/waiver/execution/recipe/knowledge
- 不自动批准/完成/分配
- 不调用 AI 理解 free-text 反馈
- Adaptive guidance 只做排序倾向与解释

## 10. 后端测试结果

**36 个 43C + 36 个 43B + 36 个 43A + 36 个 42C + 36 个 42B + 36 个 42A + 34 个 41C + 30 个 41B + 30 个 41A + 26 个 40C + 36 个 40B + 32 个 40A = 404/404 全部通过**

**Feedback（8 个）**：记录、按 session 查询、rating、reasonCode、helpfulFlag、按目标类型、suggestionType、全量列表

**Adaptive Signals（9 个）**：刷新、幂等、高接受率→BOOST、rationale、weight score、focus mode signal、dashboard 计数、signalLevel 标签、NOT_RELEVANT→DOWNRANK

**Tuning Snapshot（10 个）**：刷新、totalFeedbackCount、acceptanceRate、avgRating、confidenceScore、dismissalRate、topSuggestionType、summaryMarkdown、weakestSuggestionType、幂等

**Dashboard/Report（5 个）**：latestSnapshot 降级处理、空数据降级、report Markdown、signal dashboard 空数据

**Edge Cases（4 个）**：重复 feedback 不报错、多反馈聚合、helpful→avg rating 提升、dismissal rate 计算

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式 |

## 12. 已知限制

1. **Feedback 不支持 free-text 语义理解**：reasonCode 为枚举值，不调用 AI
2. **Adaptive signal 基于简单规则聚合**：不基于 ML 模型
3. **Tuning snapshot 窗口固定为 14 天**：未支持动态窗口切换
4. **E2E 环境依赖**：与之前 milestone 一致的 graceful fallback 模式

## 13. 是否可以进入 Milestone 44A

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V55 migration + test schema）
2. ✅ Operator feedback 可记录/查询
3. ✅ Adaptive signals 可刷新/查询/dashboard
4. ✅ Tuning snapshot/report 可导出
5. ✅ BOOST/KEEP/WATCH/DOWNRANK 可计算和解释
6. ✅ 404 个后端集成测试全部通过（40A→43C）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ 调优逻辑清晰、可解释

建议 44A 方向：Governance Autonomous Draft Planning & Safe Assistive Actions，包括自动生成 draft remediation plan、safe assistive action checklist、recommendation package assembly。
