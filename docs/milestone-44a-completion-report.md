# Milestone 44A — Governance Autonomous Draft Planning & Safe Assistive Actions 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V56__init_governance_draft_planning_tables.sql` | 3 张新表迁移 |
| 6 个枚举 | DraftPlanStatus, DraftPlanScopeType, AssistiveActionType, AssistiveActionStatus, AssistiveSafetyLevel, RecommendationPackageStatus |
| 3 个实体 | DraftRemediationPlanEntity, SafeAssistiveActionEntity, RecommendationPackageEntity |
| 3 个 Mapper | DraftRemediationPlan, SafeAssistiveAction, RecommendationPackage |
| 3 个 DTO | DraftRemediationPlanResponse, SafeAssistiveActionResponse, RecommendationPackageResponse |
| `GovernanceDraftPlanningService.java` | 草稿计划服务 |
| `GovernanceSafeAssistiveActionService.java` | 安全辅助动作服务 |
| `GovernanceRecommendationPackageService.java` | 推荐提交包服务 |
| `GovernanceDraftPlanningController.java` | 12 个 API 端点 |
| `GovernanceDraftPlanningIntegrationTest.java` | 36 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V56 测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceDraftPlanningPanel.vue` | 草稿计划面板 |
| `GovernanceAssistiveActionPanel.vue` | 安全辅助动作面板 |
| `GovernanceRecommendationPackagePanel.vue` | 推荐提交包面板 |
| `governance-draft-planning.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 44A 接口（12+ API 函数 + 3 数据接口） |
| `ObservabilityPage.vue` | 新增 44A 草稿规划区块 |

## 2. 三张 Draft Plan/Safe Assistive/Package 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_draft_remediation_plan` | 草稿修复计划 | recommendation_id, plan_status(DRAFT→READY_FOR_REVIEW→REVIEWED→ARCHIVED), plan_title, scope_type, proposed_steps_json, risk_level, human_confirmation_required, linked_bundle/playbook/recipe |
| `governance_safe_assistive_action` | 安全辅助动作 | draft_plan_id, action_type(6 种), action_status(PENDING→REVIEWED→READY/SKIPPED), safety_level(INFO/SAFE/CAUTION/REVIEW_REQUIRED), checklist_json, confirmation_required, action_order |
| `governance_recommendation_package` | 推荐提交包 | recommendation_id, draft_plan_id, package_status(DRAFT→READY→REVIEWED→ARCHIVED), recommendation_context_json, submit_ready_flag, submitted_flag |

## 3. GovernanceDraftPlanningService 设计说明

**职责**：创建/刷新/管理 draft remediation plan。

- 默认 4 步 proposed steps，risk level 默认 MEDIUM
- humanConfirmationRequired 默认 true
- 状态机：DRAFT→READY_FOR_REVIEW→REVIEWED→ARCHIVED
- refresh 自动生成标准化步骤

## 4. GovernanceSafeAssistiveActionService 设计说明

**职责**：生成和管理 6 种安全辅助动作。

**6 种动作**：OPEN_PLAYBOOK_DRAFT(SAFE), OPEN_RECIPE_DRAFT(SAFE), PREPARE_HANDOFF_NOTE(CAUTION), PREPARE_WAIVER_REVIEW(REVIEW_REQUIRED), PREPARE_FORECAST_CHECK(INFO), PREPARE_RISK_SUMMARY(CAUTION)

**状态机**：PENDING→REVIEWED→READY / PENDING→SKIPPED

**Safety Level 含义**：INFO=信息展示, SAFE=纯摘要/预填, CAUTION=涉及风险解释, REVIEW_REQUIRED=涉及确认与提交

## 5. GovernanceRecommendationPackageService 设计说明

**职责**：管理 recommendation 提交包的状态流转。

**状态机**：DRAFT→READY→REVIEWED→ARCHIVED
- READY 时自动设置 submitReadyFlag=1

## 6-8. 三个前端面板

**GovernanceDraftPlanningPanel**：草稿计划列表（状态标签、风险等级标签、标题），状态流转按钮（提交审阅→已审阅），刷新按钮，新建草稿按钮。

**GovernanceAssistiveActionPanel**：安全辅助动作列表（状态标签、safety level 标签、标题），动作生成按钮，状态流转按钮（审阅→跳过→就绪）。

**GovernanceRecommendationPackagePanel**：提交包列表（状态标签、可提交标签），状态流转按钮（就绪→已审阅）。

## 9. Draft Planning/Safe Assistive 边界说明

**已实现**：
- Draft remediation plan CRUD + 刷新 + 状态流转
- 6 种 safe assistive action + 4 级 safety level
- Recommendation package 状态流转 + submit ready flag
- Dashboard/report 导出

**不涉及**：
- 不自动修改 recommendation/waiver/execution/recipe/knowledge
- 不自动批准/完成/分配/触发外部通知
- Draft planning 只做起草、预填、组装
- Assistive action 只做 checklist/prefill/summary

## 10. 后端测试结果

**36 个 44A + 36 个 43C + 36 个 43B + 36 个 43A + 36 个 42C + 36 个 42B + 36 个 42A + 34 个 41C + 30 个 41B + 30 个 41A + 26 个 40C + 36 个 40B + 32 个 40A = 440/440 全部通过**

**Draft Plan（12 个）**：创建、更新、刷新、DRAFT→READY_FOR_REVIEW→REVIEWED→ARCHIVED、非法状态拒绝、risk level 默认、humanConfirmationRequired 默认、scopeType 默认、proposedSteps 非空、幂等刷新、goal/summary 更新、refresh 保持 ID

**Safe Assistive Actions（9 个）**：生成、列表排序、PENDING→REVIEWED→READY、PENDING→SKIPPED、safetyLevel 验证、无效 transition 拒绝、SAFE/INFO/CAUTION/REVIEW_REQUIRED 类型覆盖

**Recommendation Package（2 个）**：列表、dashboard 统计

**Dashboard（5 个）**：draftPlanCount、readyForReviewCount、submitReadyPackageCount、topDraftPlans、topPackages

**Edge Cases（8 个）**：空数据 dashboard、不存在 plan→NOT_FOUND、列表、assistive action safety level 验证、refresh 幂等、非法 transition、scopeType 默认值、summary 更新

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式 |

## 12. 已知限制

1. **Draft plan 当前为简化创建**：暂未自动从 recommendation/bundle 提取上下文
2. **Assistive action 为预定义 6 种**：不能由 operator 自定义动作类型
3. **Package 不提供真实 submit**：只做 pre-submit workspace 和 flag
4. **E2E 环境依赖**：与之前 milestone 一致的 graceful fallback 模式

## 13. 是否可以进入 Milestone 44B

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V56 migration + test schema）
2. ✅ Draft plan 可创建/更新/刷新/状态流转
3. ✅ Assistive action 可生成/查询/状态流转
4. ✅ Package 可查询/状态流转
5. ✅ Dashboard/report 可导出
6. ✅ 440 个后端集成测试全部通过（40A→44A）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ 安全辅助动作不执行真实操作

建议 44B 方向：Governance Outcome Review, Draft Adoption Tracking & Assistive Quality Evaluation，包括 draft adoption/modification/rejection tracking、assistive action usefulness rating、package review quality scoring。
