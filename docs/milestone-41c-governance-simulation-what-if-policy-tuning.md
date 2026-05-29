# Milestone 41C: Governance Simulation, What-if Planning & Policy Tuning

## 1. 背景

截至 Milestone 41B，平台已经具备组织级治理预测能力：

```text
40A
  Multi-project Release Governance

40B
  Organization Policy / Guardrail / Drift

40C
  Recommendation Workflow / Waiver Management

41A
  SLA / Escalation / Ownership Health

41B
  Capacity Forecast / Predictive Risk / Backlog Health
```

现在系统已经能回答：

```text
哪些 owner 即将超载？
哪些项目 backlog 正在恶化？
哪些 waiver 到期会形成风险？
未来 7 天 / 14 天可能出现哪些治理压力？
```

但如果治理系统想真正帮助团队“提前做决策”，还缺少一层模拟能力：

```text
如果把 P1 SLA 从 72h 调成 96h，会怎样？
如果把部分事项从 Owner A 调给 Owner B，风险会不会下降？
如果提前处理将到期 waiver，未来风险信号会改善多少？
如果换一个 guardrail policy 组合，portfolio 的 risk 和 confidence 会发生什么变化？
```

换句话说，41B 让平台具备了：

```text
对未来风险的规则化预测
```

但还不具备：

```text
对治理策略改动进行 what-if 模拟和影响对比
```

Milestone 41C 的目标就是新增：

```text
Governance Simulation, What-if Planning & Policy Tuning
```

让平台从：

```text
能预测接下来可能发生什么
```

升级为：

```text
能模拟“如果我们这样做，会发生什么”
```

---

## 2. 总目标

实现治理模拟与策略调优能力：

1. 新增 Governance Simulation Scenario 数据模型。
2. 新增 Governance Simulation Result 数据模型。
3. 新增 Policy Tuning Suggestion 数据模型。
4. 支持 what-if 模拟 SLA / owner rebalancing / waiver reduction / policy threshold 变化。
5. 支持比较模拟前后 forecast / risk / backlog 变化。
6. 支持输出 owner rebalancing recommendation。
7. 支持输出 waiver risk reduction simulation。
8. 支持生成 policy tuning suggestion。
9. 支持导出 Simulation Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
知道未来风险会升高
```

升级为：

```text
知道哪种治理动作最可能把风险压下去
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改生产中的 SLA / owner / waiver / policy 原始记录。
4. 不自动重新分配 owner。
5. 不自动批准、撤销、关闭 waiver。
6. 不自动应用 simulation 结果到真实系统。
7. 不调用真实 AI 自动生成策略结论。
8. simulation 只基于当前数据库快照和规则法计算。
9. what-if 结果只作为建议，不作为真实执行。
10. 不破坏 1-41B 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 simulation scenario / result / tuning suggestion 表。
2. 基于 41A / 41B 的数据进行规则化模拟。
3. 新增 scenario、comparison、suggestion、report 面板。
4. 输出结构化 Markdown 模拟报告。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V49__init_governance_simulation_tables.sql
```

### 4.1 governance_simulation_scenario

```sql
CREATE TABLE governance_simulation_scenario (
    id BIGINT PRIMARY KEY,
    scenario_name VARCHAR(255) NOT NULL,
    scenario_type VARCHAR(64) NOT NULL,
    baseline_snapshot_date DATE NULL,
    scenario_status VARCHAR(32) NOT NULL,
    input_json JSON NOT NULL,
    notes TEXT NULL,
    created_by BIGINT NULL,
    created_by_name VARCHAR(128) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_simulation_scenario_type(scenario_type, scenario_status),
    KEY idx_governance_simulation_scenario_date(baseline_snapshot_date, create_time)
);
```

### 4.2 governance_simulation_result

```sql
CREATE TABLE governance_simulation_result (
    id BIGINT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    impacted_owner_count INT NOT NULL DEFAULT 0,
    impacted_project_count INT NOT NULL DEFAULT 0,
    projected_backlog_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_overdue_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_risk_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_capacity_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    detail_json JSON NULL,
    report_markdown MEDIUMTEXT NULL,
    calculated_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_simulation_result_scenario(scenario_id),
    KEY idx_governance_simulation_result_status(result_status, calculated_at)
);
```

### 4.3 policy_tuning_suggestion

```sql
CREATE TABLE policy_tuning_suggestion (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    target_scope VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NULL,
    current_value VARCHAR(255) NULL,
    suggested_value VARCHAR(255) NULL,
    expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_policy_tuning_suggestion_date(snapshot_date, priority),
    KEY idx_policy_tuning_suggestion_type(suggestion_type, target_scope)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceSimulationScenarioType.java
GovernanceSimulationScenarioStatus.java
GovernanceSimulationResultStatus.java
PolicyTuningSuggestionType.java
```

### 5.1 GovernanceSimulationScenarioType

```text
SLA_TUNING
OWNER_REBALANCING
WAIVER_REDUCTION
POLICY_THRESHOLD_TUNING
```

### 5.2 GovernanceSimulationScenarioStatus

```text
DRAFT
READY
SIMULATED
ARCHIVED
```

### 5.3 GovernanceSimulationResultStatus

```text
SUCCESS
WARNING
NO_IMPROVEMENT
INVALID
```

### 5.4 PolicyTuningSuggestionType

```text
ADJUST_SLA
REBALANCE_OWNER_LOAD
REDUCE_WAIVER_CLUSTER
ADJUST_GUARDRAIL_THRESHOLD
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceSimulationScenarioEntity.java
GovernanceSimulationResultEntity.java
PolicyTuningSuggestionEntity.java

GovernanceSimulationScenarioMapper.java
GovernanceSimulationResultMapper.java
PolicyTuningSuggestionMapper.java
```

DTO 建议：

```text
CreateGovernanceSimulationScenarioRequest.java
UpdateGovernanceSimulationScenarioRequest.java
GovernanceSimulationScenarioResponse.java

GovernanceSimulationResultResponse.java
GovernanceSimulationComparisonResponse.java

PolicyTuningSuggestionResponse.java
GovernanceSimulationDashboardResponse.java
```

### 6.1 GovernanceSimulationDashboardResponse

建议字段：

```text
snapshotDate
scenarioCount
successfulScenarioCount
warningScenarioCount
noImprovementCount
topScenarios
topSuggestions
```

### 6.2 GovernanceSimulationComparisonResponse

建议字段：

```text
scenarioId
scenarioName
scenarioType
baselineProjectedBacklog
simulatedProjectedBacklog
baselineProjectedOverdue
simulatedProjectedOverdue
baselineRiskScore
simulatedRiskScore
deltaSummary
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceSimulationService.java
GovernanceWhatIfPlannerService.java
PolicyTuningSuggestionService.java
```

### 7.1 GovernanceSimulationService

职责：

1. 管理 scenario 的 CRUD。
2. 执行 simulation 计算。
3. 生成 simulation result 与 markdown report。
4. 支持状态流转：

```text
DRAFT -> READY -> SIMULATED -> ARCHIVED
```

### 7.2 GovernanceWhatIfPlannerService

职责：

1. 基于 41A/41B 数据计算 baseline。
2. 根据 scenario type 应用规则化变更：
   - `SLA_TUNING`：放宽/收紧 SLA 小时数
   - `OWNER_REBALANCING`：把部分 open item 从高负载 owner 转移到低负载 owner（仅模拟）
   - `WAIVER_REDUCTION`：模拟在未来 7 天处理一部分 waiver
   - `POLICY_THRESHOLD_TUNING`：调整关键 guardrail threshold
3. 计算前后 delta。

### 7.3 PolicyTuningSuggestionService

职责：

1. 基于 simulation / forecast / drift 自动生成 tuning suggestion。
2. 输出 suggestion priority、当前值、建议值、预期影响。
3. 支持按 snapshot date / target scope 查询。

---

## 8. API 设计

新增 Controller：

```text
GovernanceSimulationController.java
```

建议端点：

### 8.1 Scenario

```text
POST   /api/governance-simulation/scenarios
GET    /api/governance-simulation/scenarios
GET    /api/governance-simulation/scenarios/{scenarioId}
PUT    /api/governance-simulation/scenarios/{scenarioId}
POST   /api/governance-simulation/scenarios/{scenarioId}/status
POST   /api/governance-simulation/scenarios/{scenarioId}/run
```

### 8.2 Result / Comparison

```text
GET    /api/governance-simulation/scenarios/{scenarioId}/result
GET    /api/governance-simulation/scenarios/{scenarioId}/comparison
GET    /api/governance-simulation/report
```

### 8.3 Suggestions / Dashboard

```text
POST   /api/governance-simulation/suggestions/refresh
GET    /api/governance-simulation/suggestions
GET    /api/governance-simulation/dashboard
```

权限建议：

```text
查看：ADMIN
创建 / 运行 scenario：ADMIN
刷新 suggestion：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Simulation Result 判定

```text
如果 projected overdue、risk、backlog 同时下降 -> SUCCESS
如果部分指标改善但部分恶化 -> WARNING
如果整体无改善或更差 -> NO_IMPROVEMENT
输入不合法 -> INVALID
```

### 9.2 Owner Rebalancing 模拟

示例输入：

```json
{
  "sourceOwnerId": 1001,
  "targetOwnerId": 1002,
  "moveItemCount": 3
}
```

效果：

```text
source backlog 减少
target backlog 增加
重新计算两位 owner 的 projected overdue / risk
```

### 9.3 Policy Tuning Suggestion 触发示例

```text
连续 3 天 P1 overdue 高 -> 建议延长 P1 SLA 或增加 owner capacity
rollback readiness 长期不足 -> 建议收紧 guardrail threshold
waiver cluster 重复发生 -> 建议减少 waiver 有效期或强化 owner 处理要求
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceSimulationScenarioPanel.vue
GovernanceSimulationComparisonPanel.vue
PolicyTuningSuggestionPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceSimulationScenarioPanel

展示：

1. scenario 列表
2. create / edit dialog
3. 运行按钮
4. status tag
5. scenario type 与输入摘要

### 10.2 GovernanceSimulationComparisonPanel

展示：

1. baseline vs simulated 指标对比
2. delta tag（改善 / 恶化）
3. result 状态
4. report 下载/查看

### 10.3 PolicyTuningSuggestionPanel

展示：

1. suggestion 列表
2. priority / type / scope / current vs suggested
3. expected impact
4. rationale 摘要

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. comparison 要清楚展示 before/after
4. tuning suggestion 要突出优先级与预期收益

---

## 11. 后端测试要求

新增：

```text
GovernanceSimulationPolicyTuningIntegrationTest.java
```

不少于 34 个集成测试，建议覆盖：

1. create scenario success
2. update scenario success
3. status draft -> ready
4. ready -> simulated
5. simulated -> archived
6. invalid status transition reject
7. run SLA tuning scenario success
8. run owner rebalancing scenario success
9. run waiver reduction scenario success
10. run policy threshold tuning scenario success
11. invalid scenario input returns INVALID
12. comparison returns baseline and simulated values
13. simulation result SUCCESS
14. simulation result WARNING
15. simulation result NO_IMPROVEMENT
16. markdown report export success
17. refresh suggestions success
18. suggestion list returns items
19. adjust SLA suggestion generated
20. rebalancing suggestion generated
21. waiver reduction suggestion generated
22. guardrail threshold suggestion generated
23. dashboard counts correct
24. unauthorized access reject
25. non-admin create reject
26. empty baseline returns empty dashboard
27. owner rebalancing affects source and target forecast
28. waiver reduction lowers expiry-related risk
29. threshold tuning changes simulated guardrail result
30. repeated run updates latest result
31. archived scenario blocks rerun
32. suggestion priority populated
33. suggestion current/suggested value persisted
34. scenario report contains delta summary

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-simulation.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 scenario panel
2. comparison panel renders
3. suggestion panel renders
4. create scenario dialog works
5. run scenario button visible
6. comparison before/after 区域可见
7. suggestion priority / impact 可见
8. no JS errors on page load

如果测试环境没有 seeded governance forecast 数据：

1. 显式断言空态
2. 不把“无 simulation 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-41c-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance simulation / tuning 表说明
3. GovernanceSimulationService 设计说明
4. GovernanceWhatIfPlannerService 设计说明
5. PolicyTuningSuggestionService 设计说明
6. GovernanceSimulationScenarioPanel 说明
7. GovernanceSimulationComparisonPanel 说明
8. PolicyTuningSuggestionPanel 说明
9. Simulation / Tuning 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42A

---

## 14. 验收标准

必须全部满足：

1. governance_simulation_scenario / governance_simulation_result / policy_tuning_suggestion 三张表已落库
2. scenario 可创建 / 编辑 / 运行 / 状态流转
3. comparison 可展示 baseline 与 simulated 差异
4. tuning suggestion 可刷新与展示
5. report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. simulation 与 suggestion 逻辑清晰、可解释

---

## 15. 完成后的价值

完成 41C 后，平台将从：

```text
能预测治理风险
```

升级为：

```text
能模拟治理动作、比较不同方案、输出可执行的调优建议
```

这一步会让治理系统从“预测看板”迈向“决策支持工作台”。

---

## 16. 后续建议

Milestone 41C 完成后，建议进入：

```text
Milestone 42A: Governance Execution Automation & Recommendation Playbooks
```

重点可包括：

1. recommendation playbook template
2. workflow automation suggestion
3. owner handoff assistant
4. waiver mitigation playbook
5. guided remediation checklists

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 41C。

文档路径：
docs/milestone-41c-governance-simulation-what-if-policy-tuning.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 41B governance capacity forecast / predictive risk 基础上，新增 simulation、what-if planning 和 policy tuning。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / escalation / SLA / policy 原始记录。
6. 不要自动分配 owner。
7. 不要自动关闭事项或批准 waiver。
8. 不要调用真实 AI 自动生成模拟结论。
9. simulation 只做规则法对比，不做真实执行。
10. 不要破坏 1-41B 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V49__init_governance_simulation_tables.sql。
2. 新增 governance_simulation_scenario / governance_simulation_result / policy_tuning_suggestion 三张表。
3. 新增 4 个枚举：
   - GovernanceSimulationScenarioType
   - GovernanceSimulationScenarioStatus
   - GovernanceSimulationResultStatus
   - PolicyTuningSuggestionType
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceSimulationService。
6. 新增 GovernanceWhatIfPlannerService。
7. 新增 PolicyTuningSuggestionService。
8. 新增 API：
   - scenario CRUD / status / run
   - result / comparison / report
   - tuning suggestion refresh / list / dashboard
9. 前端新增：
   - GovernanceSimulationScenarioPanel.vue
   - GovernanceSimulationComparisonPanel.vue
   - PolicyTuningSuggestionPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 34 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-41c-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance simulation / tuning 表说明
3. GovernanceSimulationService 设计说明
4. GovernanceWhatIfPlannerService 设计说明
5. PolicyTuningSuggestionService 设计说明
6. GovernanceSimulationScenarioPanel 说明
7. GovernanceSimulationComparisonPanel 说明
8. PolicyTuningSuggestionPanel 说明
9. Simulation / Tuning 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 42A

现在开始实现，不要只给计划。
```
