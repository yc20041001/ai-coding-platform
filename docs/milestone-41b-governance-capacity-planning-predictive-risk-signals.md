# Milestone 41B: Governance Capacity Planning & Predictive Risk Signals

## 1. 背景

截至 Milestone 41A，平台已经具备较完整的治理运营能力：

```text
40A
  Multi-project Release Governance

40B
  Organization Policy / Guardrail / Drift

40C
  Recommendation Workflow / Waiver Management

41A
  SLA / Escalation / Ownership Health
```

现在系统已经能回答：

```text
当前有哪些治理事项？
哪些已经逾期？
哪些需要 escalation？
哪个 owner 负载过高？
哪个 waiver 已经过期或即将到期？
```

但治理运营如果要再往前走一步，仍然缺“预测能力”：

```text
下周哪个 owner 最可能超载？
哪些项目的 recommendation backlog 正在恶化？
逾期趋势是否在上升？
哪些 waiver 会集中到期形成风险？
当前 throughput 是否足以消化新增事项？
```

换句话说，41A 让平台具备了：

```text
对当前治理状态的监控与升级
```

但还不具备：

```text
对治理容量和未来风险的前瞻判断
```

Milestone 41B 的目标就是新增：

```text
Governance Capacity Planning & Predictive Risk Signals
```

让平台从：

```text
能描述现在发生了什么
```

升级为：

```text
能预测接下来最可能出问题的 owner / 项目 / waiver / backlog
```

---

## 2. 总目标

实现治理容量规划与风险预测：

1. 新增 Governance Capacity Forecast 数据模型。
2. 新增 Predictive Risk Signal 数据模型。
3. 新增 Governance Backlog Snapshot 数据模型。
4. 支持按 owner 生成未来 7 天 / 14 天 capacity forecast。
5. 支持按项目生成 backlog health 与 overdue trend forecast。
6. 支持预测 waiver expiry risk。
7. 支持生成 predictive risk card（owner / project / waiver / backlog）。
8. 支持输出治理吞吐、积压变化和 forecast summary。
9. 支持导出 Governance Forecast Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有当前治理监控
```

升级为：

```text
有容量规划、风险前瞻和 backlog 演化预测
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改已有 recommendation / waiver / escalation 原始记录。
4. 不自动分配 owner。
5. 不自动关闭事项或批准 waiver。
6. 不自动触发外部通知。
7. 不调用真实 AI 自动生成预测结论。
8. predictive risk 只基于已有结构化数据和明确定义的规则 / 简单 forecast 公式。
9. forecast 只做提示，不自动调整 SLA、priority、owner 或 rollout policy。
10. 不破坏 1-41A 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 capacity forecast / predictive risk / backlog snapshot 表。
2. 聚合 40C workflow、41A SLA / escalation / ownership 数据。
3. 新增 dashboard、forecast、risk card、summary、export。
4. 使用规则法 / 简单线性趋势法计算预测值。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V48__init_governance_forecast_risk_tables.sql
```

### 4.1 governance_capacity_forecast

```sql
CREATE TABLE governance_capacity_forecast (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    forecast_horizon_days INT NOT NULL,
    owner_id BIGINT NOT NULL,
    owner_name VARCHAR(128) NOT NULL,
    current_open_count INT NOT NULL DEFAULT 0,
    current_overdue_count INT NOT NULL DEFAULT 0,
    avg_completed_per_day DECIMAL(8,2) NOT NULL DEFAULT 0,
    projected_new_items INT NOT NULL DEFAULT 0,
    projected_completed_items INT NOT NULL DEFAULT 0,
    projected_backlog_count INT NOT NULL DEFAULT 0,
    projected_overdue_count INT NOT NULL DEFAULT 0,
    capacity_risk_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_capacity_forecast_date(snapshot_date, forecast_horizon_days),
    KEY idx_governance_capacity_forecast_owner(owner_id, snapshot_date),
    KEY idx_governance_capacity_forecast_level(snapshot_date, capacity_risk_level)
);
```

### 4.2 predictive_risk_signal

```sql
CREATE TABLE predictive_risk_signal (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NULL,
    target_name VARCHAR(255) NOT NULL,
    signal_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    probability_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    time_horizon_days INT NOT NULL DEFAULT 7,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_predictive_risk_signal_date(snapshot_date, risk_level),
    KEY idx_predictive_risk_signal_target(target_type, target_id, snapshot_date),
    KEY idx_predictive_risk_signal_type(signal_type, snapshot_date)
);
```

### 4.3 governance_backlog_snapshot

```sql
CREATE TABLE governance_backlog_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    open_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    blocked_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0,
    waiver_active_count INT NOT NULL DEFAULT 0,
    incoming_7d_count INT NOT NULL DEFAULT 0,
    completed_7d_count INT NOT NULL DEFAULT 0,
    backlog_growth_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    backlog_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_backlog_snapshot_date(snapshot_date, backlog_growth_rate),
    KEY idx_governance_backlog_snapshot_project(project_id, snapshot_date),
    KEY idx_governance_backlog_snapshot_level(snapshot_date, backlog_health_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceCapacityRiskLevel.java
PredictiveRiskSignalType.java
PredictiveRiskTargetType.java
GovernanceBacklogHealthLevel.java
```

### 5.1 GovernanceCapacityRiskLevel

```text
LOW
WATCH
HIGH
CRITICAL
```

### 5.2 PredictiveRiskSignalType

```text
OWNER_OVERLOAD_FORECAST
OVERDUE_TREND_FORECAST
WAIVER_EXPIRY_CLUSTER
PROJECT_BACKLOG_GROWTH
THROUGHPUT_DEFICIT
```

### 5.3 PredictiveRiskTargetType

```text
OWNER
PROJECT
WAIVER_GROUP
PORTFOLIO
```

### 5.4 GovernanceBacklogHealthLevel

```text
HEALTHY
WATCH
RISK
CRITICAL
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
GovernanceCapacityForecastEntity.java
PredictiveRiskSignalEntity.java
GovernanceBacklogSnapshotEntity.java

GovernanceCapacityForecastMapper.java
PredictiveRiskSignalMapper.java
GovernanceBacklogSnapshotMapper.java
```

DTO 建议：

```text
GovernanceCapacityForecastResponse.java
GovernanceCapacityDashboardResponse.java

PredictiveRiskSignalResponse.java
PredictiveRiskDashboardResponse.java

GovernanceBacklogSnapshotResponse.java
GovernanceBacklogDashboardResponse.java

GovernanceForecastSummaryResponse.java
```

### 6.1 GovernanceCapacityDashboardResponse

建议字段：

```text
snapshotDate
ownerCount
lowRiskCount
watchCount
highCount
criticalCount
topRiskOwners
averageProjectedBacklog
averageProjectedOverdue
```

### 6.2 PredictiveRiskDashboardResponse

建议字段：

```text
snapshotDate
signalCount
highSignalCount
criticalSignalCount
ownerRiskSignals
projectRiskSignals
portfolioRiskSignals
topSignals
```

### 6.3 GovernanceBacklogDashboardResponse

建议字段：

```text
snapshotDate
projectCount
healthyCount
watchCount
riskCount
criticalCount
topGrowingBacklogs
topOverdueProjects
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceCapacityForecastService.java
PredictiveRiskSignalService.java
GovernanceBacklogHealthService.java
```

### 7.1 GovernanceCapacityForecastService

职责：

1. 按 owner 聚合当前 open / overdue / completed_7d。
2. 估算 avg_completed_per_day。
3. 估算 projected_new_items。
4. 计算 7 天 / 14 天 projected backlog 与 projected overdue。
5. 输出 capacity risk level。

建议简化公式：

```text
avgCompletedPerDay = completed7d / 7
projectedNewItems = max(1, recentIncomingRate * horizon)
projectedCompletedItems = avgCompletedPerDay * horizon
projectedBacklog = currentOpen + projectedNewItems - projectedCompletedItems
projectedOverdue = currentOverdue + max(0, projectedBacklog - currentOpen) * overdueFactor
```

### 7.2 PredictiveRiskSignalService

职责：

1. 基于 forecast / backlog / waiver 数据生成 signal。
2. 支持 owner overload forecast。
3. 支持 overdue trend forecast。
4. 支持 waiver expiry cluster risk。
5. 支持 throughput deficit 风险。
6. 输出 risk score / probability score / summary。

### 7.3 GovernanceBacklogHealthService

职责：

1. 按项目聚合 backlog snapshot。
2. 计算 backlog growth rate：

```text
(incoming_7d - completed_7d) / max(1, completed_7d)
```

3. 评估 backlog health level。
4. 输出 top growing backlog / top overdue project。
5. 生成 governance forecast summary / report。

---

## 8. API 设计

新增 Controller：

```text
GovernanceForecastController.java
```

建议端点：

### 8.1 Capacity Forecast

```text
POST   /api/governance-forecast/capacity/refresh
GET    /api/governance-forecast/capacity
GET    /api/governance-forecast/capacity/dashboard
```

### 8.2 Predictive Risk

```text
POST   /api/governance-forecast/risk-signals/refresh
GET    /api/governance-forecast/risk-signals
GET    /api/governance-forecast/risk-signals/dashboard
```

### 8.3 Backlog Health

```text
POST   /api/governance-forecast/backlog/refresh
GET    /api/governance-forecast/backlog
GET    /api/governance-forecast/backlog/dashboard
```

### 8.4 Summary / Report

```text
GET    /api/governance-forecast/summary
GET    /api/governance-forecast/report
```

权限建议：

```text
查看：ADMIN
refresh / recalculation：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Capacity Risk Level

可按 projected backlog / overdue 评估：

```text
projectedOverdue = 0 且 projectedBacklog 可控 -> LOW
projectedOverdue > 0 -> WATCH
projectedOverdue >= 3 或 projectedBacklog 明显增长 -> HIGH
projectedOverdue >= 8 或 owner 已高负载 -> CRITICAL
```

### 9.2 Backlog Health Level

```text
growthRate <= 0 且 overdue 少 -> HEALTHY
growthRate > 0 -> WATCH
growthRate > 0.5 或 overdue 明显上升 -> RISK
growthRate > 1.0 且 blocked/overdue 偏高 -> CRITICAL
```

### 9.3 Predictive Risk Score

示例：

```text
riskScore = backlogGrowthRate * 30
         + projectedOverdue * 8
         + expiringWaiverCluster * 10
         + throughputDeficit * 12
```

最终归一到：

```text
0 ~ 100
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceCapacityForecastPanel.vue
PredictiveRiskSignalPanel.vue
GovernanceBacklogHealthPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceCapacityForecastPanel

展示：

1. owner capacity 指标卡
2. top risk owners
3. projected backlog / overdue
4. forecast horizon 标记（7d / 14d）

### 10.2 PredictiveRiskSignalPanel

展示：

1. risk signal 指标卡
2. top signals 列表
3. risk level / probability / horizon
4. 按 targetType 分组

### 10.3 GovernanceBacklogHealthPanel

展示：

1. backlog health 指标卡
2. top growing backlog 项目
3. top overdue 项目
4. growth rate / health level tag

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. risk signal 要突出高/严重风险
4. forecast 面板要强调可扫描和可比较

---

## 11. 后端测试要求

新增：

```text
GovernanceForecastPredictiveRiskIntegrationTest.java
```

不少于 34 个集成测试，建议覆盖：

1. refresh capacity forecast success
2. refresh predictive risk success
3. refresh backlog snapshot success
4. capacity dashboard counts correct
5. projected backlog calculation correct
6. projected overdue calculation correct
7. capacity risk low
8. capacity risk watch
9. capacity risk high
10. capacity risk critical
11. backlog growth rate calculation correct
12. backlog health healthy
13. backlog health watch
14. backlog health risk
15. backlog health critical
16. owner overload forecast signal created
17. overdue trend forecast signal created
18. waiver expiry cluster signal created
19. throughput deficit signal created
20. predictive risk dashboard counts correct
21. top signals ordered by riskScore desc
22. summary response contains top risk owners
23. report export markdown success
24. unauthorized access reject
25. non-admin refresh reject
26. empty dataset returns empty dashboard
27. 14-day horizon returns larger forecast than 7-day where expected
28. repeated refresh idempotent by snapshot date
29. probability score range valid
30. risk score range valid
31. backlog dashboard top projects returned
32. forecast uses completed7d throughput
33. active waiver count influences predictive risk
34. blocked count influences backlog health

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-forecast.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 capacity forecast panel
2. predictive risk panel renders
3. backlog health panel renders
4. refresh buttons visible
5. top risk owners 列表可见
6. risk signal 列表可见
7. backlog growth / level 标签可见
8. no JS errors on page load

如果测试环境没有 seeded governance 数据：

1. 显式断言空态
2. 不把“无 forecast 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-41b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance forecast / risk / backlog 表说明
3. GovernanceCapacityForecastService 设计说明
4. PredictiveRiskSignalService 设计说明
5. GovernanceBacklogHealthService 设计说明
6. GovernanceCapacityForecastPanel 说明
7. PredictiveRiskSignalPanel 说明
8. GovernanceBacklogHealthPanel 说明
9. Forecast / Predictive Risk 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41C

---

## 14. 验收标准

必须全部满足：

1. governance_capacity_forecast / predictive_risk_signal / governance_backlog_snapshot 三张表已落库
2. capacity forecast 可刷新与查询
3. predictive risk signal 可生成与展示
4. backlog health 可刷新与展示
5. summary / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. forecast 与 risk signal 逻辑清晰可解释

---

## 15. 完成后的价值

完成 41B 后，平台将从：

```text
能监控治理运营当前状态
```

升级为：

```text
能预测治理容量、积压演化和未来风险信号
```

这一步会把治理能力从“运营控制台”推进到“轻量预测决策支持”阶段。

---

## 16. 后续建议

Milestone 41B 完成后，建议进入：

```text
Milestone 41C: Governance Simulation, What-if Planning & Policy Tuning
```

重点可包括：

1. what-if 情景模拟
2. SLA / policy 参数调优建议
3. owner rebalancing simulation
4. waiver risk reduction simulation
5. policy impact comparison

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 41B。

文档路径：
docs/milestone-41b-governance-capacity-planning-predictive-risk-signals.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 41A governance SLA / escalation / ownership health 基础上，新增 capacity planning 与 predictive risk signals。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改 recommendation / waiver / escalation 原始记录。
6. 不要自动分配 owner。
7. 不要自动关闭事项或批准 waiver。
8. 不要调用真实 AI 自动生成预测结论。
9. 预测只基于规则法 / 简单趋势法，不接入外部 ML 系统。
10. 不要破坏 1-41A 已有 API。
11. 前端保持中文暗色科技风 UI，复用现有组件。
12. IDs 对外保持 String。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V48__init_governance_forecast_risk_tables.sql。
2. 新增 governance_capacity_forecast / predictive_risk_signal / governance_backlog_snapshot 三张表。
3. 新增 4 个枚举：
   - GovernanceCapacityRiskLevel
   - PredictiveRiskSignalType
   - PredictiveRiskTargetType
   - GovernanceBacklogHealthLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceCapacityForecastService。
6. 新增 PredictiveRiskSignalService。
7. 新增 GovernanceBacklogHealthService。
8. 新增 API：
   - capacity refresh / list / dashboard
   - risk-signal refresh / list / dashboard
   - backlog refresh / list / dashboard
   - summary / report
9. 前端新增：
   - GovernanceCapacityForecastPanel.vue
   - PredictiveRiskSignalPanel.vue
   - GovernanceBacklogHealthPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 34 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-41b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance forecast / risk / backlog 表说明
3. GovernanceCapacityForecastService 设计说明
4. PredictiveRiskSignalService 设计说明
5. GovernanceBacklogHealthService 设计说明
6. GovernanceCapacityForecastPanel 说明
7. PredictiveRiskSignalPanel 说明
8. GovernanceBacklogHealthPanel 说明
9. Forecast / Predictive Risk 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41C

现在开始实现，不要只给计划。
```
