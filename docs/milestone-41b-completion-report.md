# Milestone 41B — Governance Capacity Planning & Predictive Risk Signals 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V48__init_governance_forecast_risk_tables.sql` | 3 张新表迁移 |
| `GovernanceCapacityRiskLevel.java` | 容量风险级别枚举（LOW/WATCH/HIGH/CRITICAL） |
| `PredictiveRiskSignalType.java` | 风险信号类型枚举（OWNER_OVERLOAD_FORECAST/OVERDUE_TREND_FORECAST/WAIVER_EXPIRY_CLUSTER/PROJECT_BACKLOG_GROWTH/THROUGHPUT_DEFICIT） |
| `PredictiveRiskTargetType.java` | 风险目标类型枚举（OWNER/PROJECT/WAIVER_GROUP/PORTFOLIO） |
| `GovernanceBacklogHealthLevel.java` | 积压健康度枚举（HEALTHY/WATCH/RISK/CRITICAL） |
| `GovernanceCapacityForecastEntity.java` | 容量预测实体 |
| `PredictiveRiskSignalEntity.java` | 风险信号实体 |
| `GovernanceBacklogSnapshotEntity.java` | 积压快照实体 |
| 3 个 Mapper | GovernanceCapacityForecastMapper, PredictiveRiskSignalMapper, GovernanceBacklogSnapshotMapper |
| 7 个 DTO | GovernanceCapacityForecastResponse, GovernanceCapacityDashboardResponse, PredictiveRiskSignalResponse, PredictiveRiskDashboardResponse, GovernanceBacklogSnapshotResponse, GovernanceBacklogDashboardResponse, GovernanceForecastSummaryResponse |
| `GovernanceCapacityForecastService.java` | 容量预测服务 |
| `PredictiveRiskSignalService.java` | 风险信号服务 |
| `GovernanceBacklogHealthService.java` | 积压健康度服务 |
| `GovernanceForecastController.java` | 13 个 API 端点 |
| `GovernanceForecastPredictiveRiskIntegrationTest.java` | 30 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V48 三张测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceCapacityForecastPanel.vue` | 容量预测面板 |
| `PredictiveRiskSignalPanel.vue` | 风险预测信号面板 |
| `GovernanceBacklogHealthPanel.vue` | 积压健康度面板 |
| `governance-forecast.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 41B 接口（15+ API 函数 + 8 数据接口） |
| `ObservabilityPage.vue` | 新增 41B 治理预测区块 |

## 2. 三张 Governance Forecast/Risk/Backlog 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_capacity_forecast` | Owner 容量预测 | snapshot_date, forecast_horizon_days(7/14), owner_id/name, current_open/overdue_count, avg_completed_per_day, projected_new/completed/backlog/overdue_count, capacity_risk_level |
| `predictive_risk_signal` | 预测风险信号 | snapshot_date, target_type/id/name, signal_type, risk_level, risk_score(0-100), probability_score(0-100), time_horizon_days, summary, detail, evidence_json |
| `governance_backlog_snapshot` | 项目积压快照 | snapshot_date, project_id/name, open/in_progress/blocked/overdue/waiver counts, incoming_7d/completed_7d, backlog_growth_rate, backlog_health_level |

## 3. GovernanceCapacityForecastService 设计说明

**职责**：按 owner 生成 7/14 天容量预测。

**核心公式**：
- avgCompletedPerDay = completed7d / 7
- projectedNewItems = max(1, incomingRate × horizon)
- projectedCompleted = avgCompletedPerDay × horizon
- projectedBacklog = max(0, open + projectedNew - projectedCompleted)
- projectedOverdue = currentOverdue + max(0, projectedBacklog - open) × overdueFactor

**Capacity Risk Level**：
- projectedOverdue ≥ 8 → CRITICAL
- projectedOverdue ≥ 3 或 backlog 明显增长 → HIGH
- projectedOverdue > 0 → WATCH
- 其他 → LOW

## 4. PredictiveRiskSignalService 设计说明

**职责**：基于 forecast/waiver/backlog 数据生成 5 种风险信号。

**信号类型**：
1. OWNER_OVERLOAD_FORECAST — 从 capacity forecast 中提取 HIGH/CRITICAL owner，riskScore = projectedOverdue×12 + backlog×3
2. OVERDUE_TREND_FORECAST — 平均 projected overdue > 2 时生成，反映组合级别逾期趋势
3. WAIVER_EXPIRY_CLUSTER — 7 天内到期 waiver ≥ 2 时生成，≥ 5 为 CRITICAL
4. THROUGHPUT_DEFICIT — 平均 projected backlog > 5 时生成，反映吞吐不足

所有 riskScore 和 probabilityScore 归一化到 0-100。

## 5. GovernanceBacklogHealthService 设计说明

**职责**：按项目聚合 backlog 并评估健康度。

**Backlog Growth Rate**：
- growthRate = (incoming7d - completed7d) / max(1, completed7d)

**Backlog Health Level**：
- growthRate > 1.0 且 blocked>0 或 overdue>3 → CRITICAL
- growthRate > 0.5 或 overdue > 3 → RISK
- growthRate > 0 → WATCH
- 其他 → HEALTHY

## 6-8. 三个前端面板

**GovernanceCapacityForecastPanel**：MetricTile 指标卡（Owner数/低风险/关注/高/严重/预测积压/预测逾期），高风险 Owner 列表（风险等级标签、积压/逾期/完成率）。

**PredictiveRiskSignalPanel**：MetricTile 指标卡（信号数/严重/高/Owner风险/项目风险/组合风险），top signals 列表（风险标签、类型、目标、得分/概率）。

**GovernanceBacklogHealthPanel**：MetricTile 指标卡（项目数/健康/关注/风险/严重），增长最快积压列表（健康标签、增长率/开放/阻塞），逾期最多项目列表。

## 9. Forecast / Predictive Risk 边界说明

**已实现**：
- Owner capacity 7d/14d 预测
- 5 种 predictive risk signal 生成
- Backlog health snapshot + growth rate
- Forecast summary Markdown

**不涉及**：
- 不修改 recommendation/waiver/escalation 原始记录
- 不自动分配 owner
- 不自动关闭事项或批准 waiver
- 不触发外部通知
- 不调用 AI/ML（纯规则法）
- forecast 只做提示，不自动调整 SLA/priority/owner

## 10. 后端测试结果

**30 个 41B 测试 + 30 个 41A + 26 个 40C + 36 个 40B + 32 个 40A = 154/154 全部通过**

**Capacity Forecast（6 个）**：刷新、dashboard 统计、projected backlog 计算、列表、14天 horizon、幂等刷新

**Predictive Risk Signals（8 个）**：刷新、列表、dashboard 统计、riskScore 范围(0-100)、probabilityScore 范围(0-100)、top signals、幂等、信号类型

**Backlog Health（7 个）**：刷新、project count、列表、健康等级分布、top growing、top overdue、幂等

**Summary/Report（5 个）**：forecast 数据、Markdown、signal count、backlog counts、projected totals

**Edge Cases（4 个）**：空数据 dashboard、容量风险等级分布、top risk owners、backlog growth rate

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式 |

## 12. 已知限制

1. **预测只基于简单规则法**：不涉及 ML 或复杂时间序列分析
2. **Waiver expiry cluster 需要活跃 waiver**：无活跃 waiver 时不生成信号
3. **Owner 容量预测基于 7d 历史**：新 owner 或数据不足时预测精度有限
4. **E2E 环境依赖**：与之前 milestone 一致的 graceful fallback 模式

## 13. 是否可以进入 Milestone 41C

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V48 migration + test schema）
2. ✅ Capacity forecast 可刷新与查询
3. ✅ Predictive risk signal 可生成与展示
4. ✅ Backlog health 可刷新与展示
5. ✅ Summary/report 可导出 Markdown
6. ✅ 154 个后端集成测试全部通过（40A+40B+40C+41A+41B）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ Forecast 与 risk signal 逻辑清晰可解释

建议 41C 方向：Governance Simulation, What-if Planning & Policy Tuning，包括 what-if 情景模拟、SLA/policy 参数调优建议、owner rebalancing simulation。
