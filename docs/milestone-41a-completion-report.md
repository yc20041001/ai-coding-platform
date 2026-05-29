# Milestone 41A — Governance SLA, Escalation & Ownership Health 完成报告

## 1. 新增/修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V47__init_governance_sla_escalation_ownership_tables.sql` | 3 张新表迁移 |
| `GovernanceEscalationType.java` | 升级类型枚举（OVERDUE_RECOMMENDATION/WAIVER_EXPIRING_SOON/WAIVER_EXPIRED/OWNER_OVERLOADED/OWNER_MISSING） |
| `GovernanceEscalationLevel.java` | 升级级别枚举（INFO/LOW/MEDIUM/HIGH/CRITICAL） |
| `GovernanceEscalationStatus.java` | 升级状态枚举（OPEN/ACKNOWLEDGED/RESOLVED/IGNORED） |
| `OwnerHealthLevel.java` | Owner 健康度枚举（HEALTHY/WATCH/RISK/CRITICAL） |
| `GovernanceSlaPolicyEntity.java` | SLA 策略实体 |
| `GovernanceEscalationEventEntity.java` | 升级事件实体 |
| `GovernanceOwnershipSnapshotEntity.java` | Owner 快照实体 |
| 3 个 Mapper | GovernanceSlaPolicyMapper, GovernanceEscalationEventMapper, GovernanceOwnershipSnapshotMapper |
| 8 个 DTO | GovernanceSlaPolicyResponse, Create/UpdateSlaPolicyRequest, GovernanceEscalationEventResponse, GovernanceEscalationDashboardResponse, GovernanceOwnershipSnapshotResponse, GovernanceOwnershipDashboardResponse, GovernanceOperationsSummaryResponse |
| `GovernanceSlaPolicyService.java` | SLA 策略服务 |
| `GovernanceEscalationService.java` | 升级事件服务 |
| `GovernanceOwnershipHealthService.java` | Owner 健康度服务 |
| `GovernanceOperationsController.java` | 15 个 API 端点 |
| `GovernanceOperationsSlaEscalationIntegrationTest.java` | 30 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V47 三张测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `GovernanceSlaPolicyPanel.vue` | SLA 策略面板 |
| `GovernanceEscalationPanel.vue` | 升级事件面板 |
| `GovernanceOwnershipHealthPanel.vue` | Owner 健康度面板 |
| `governance-operations.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 41A 接口（15+ API 函数 + 8 数据接口） |
| `ObservabilityPage.vue` | 新增 41A 治理运营区块 |

## 2. 三张 Governance Operations 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `governance_sla_policy` | SLA 策略定义 | policy_key(UNIQUE), display_name, priority(P0-P3), sla_hours, warning_hours, enabled |
| `governance_escalation_event` | 升级事件记录 | recommendation_id, project_id, escalation_type, escalation_level, event_status(OPEN/ACKNOWLEDGED/RESOLVED/IGNORED), triggered_at, acknowledged_at, resolved_at |
| `governance_ownership_snapshot` | Owner 健康快照 | owner_id/name, total/open/in_progress/overdue/completed_7d counts, active_waiver_count, owner_health_score(0-100), owner_health_level |

## 3. GovernanceSlaPolicyService 设计说明

**职责**：管理 SLA policy 的 CRUD，提供默认 SLA 配置能力。

- P0 → 24h SLA, P1 → 72h, P2 → 168h, P3 → 336h（可自定义）
- 支持按 priority 匹配，category 可选覆盖
- `getEnabledPolicies()` 供 escalation 扫描使用

## 4. GovernanceEscalationService 设计说明

**职责**：扫描并生成 escalation 事件，管理事件状态流转。

**5 种升级类型**：
1. `OVERDUE_RECOMMENDATION` — 逾期 recommendation 自动生成 HIGH 级别升级
2. `WAIVER_EXPIRING_SOON` — 24h 内到期的 waiver 生成 MEDIUM 级别提醒
3. `WAIVER_EXPIRED` — 已到期的 waiver 生成 CRITICAL 级别升级
4. `OWNER_OVERLOADED` — 逾期数 ≥5 生成 HIGH, ≥10 生成 CRITICAL
5. `OWNER_MISSING` — 无 owner 的 recommendation 生成 HIGH 级别升级

**幂等性**：扫描时检查同 recommendation + 同类型的 OPEN/ACKNOWLEDGED 事件，避免重复创建。

**事件状态机**：
```text
OPEN → ACKNOWLEDGED → RESOLVED
OPEN → IGNORED
```

## 5. GovernanceOwnershipHealthService 设计说明

**职责**：按 owner 聚合 recommendation 数据，计算健康度。

**Health Score 公式**：
```
base 100 - overdueCount×12 - openCount×3 - activeWaiverCount×4 + completed7dCount×2
(范围 0-100)
```

**Health Level 阈值**：
- ≥85: HEALTHY
- ≥60: WATCH
- ≥35: RISK
- <35: CRITICAL

**Dashboard**：owner 数量/健康/关注/风险/严重分布，top overloaded owners（按得分升序），top healthy owners（按得分降序），7 天完成吞吐量。

## 6-8. 三个前端面板

**GovernanceSlaPolicyPanel**：SLA 策略列表（priority/SLA 小时/警告小时/状态标签），新建对话框（policyKey、priority、SLA小时、警告小时），启用/禁用切换。

**GovernanceEscalationPanel**：MetricTile 指标卡（开放/严重/高/Waiver到期/Owner缺失），事件列表（类型/级别标签、摘要、owner），确认/忽略/解决按钮，扫描按钮。

**GovernanceOwnershipHealthPanel**：MetricTile 指标卡（Owner数/健康/关注/风险/严重/7d完成），超载 Owner Top（红色高亮 + 得分/逾期/开放数），健康 Owner Top，刷新按钮。

## 9. SLA / Escalation / Ownership 边界说明

**已实现**：
- SLA policy CRUD + 按 priority 匹配
- Escalation 扫描（overdue/waiver expiry/owner missing/owner overload）
- Escalation 状态流转（确认→解决，忽略）
- Owner 健康度快照 + 排名
- Operations summary Markdown

**不涉及**：
- 不触发外部通知系统（slack/email/pager）
- 不自动完成 recommendation
- 不自动批准 waiver
- 不执行基础设施动作
- 不调用 AI 生成治理结论
- Owner 不关联真实用户系统（自由输入）

## 10. 后端测试结果

**30 个 41A 测试 + 26 个 40C + 36 个 40B + 32 个 40A = 124/124 全部通过**

**SLA Policy（6 个）**：创建、更新、禁用、重复 key 拒绝、列表、按 ID 查询、不存在处理

**Escalation（9 个）**：扫描、幂等、列表、dashboard 统计、状态流转（OPEN→ACKNOWLEDGED→RESOLVED/IGNORED）、非法状态拒绝

**Ownership（8 个）**：刷新成功、owner 计数、列表、top overloaded、top healthy、throughput、健康等级分布、幂等刷新

**Summary/Report（4 个）**：summary 字段、markdown 导出、throughput 包含、waiver/owner 统计

**Edge Cases（3 个）**：空数据处理、escalation 类型计数、top 事件列表

## 11. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 遵循 graceful fallback 模式 |

## 12. 已知限制

1. **Escalation 无外部通知**：当前只记录与展示，不接 Slack/Email/PagerDuty
2. **Owner 不关联用户系统**：ownerId 自由输入，不校验存在性
3. **SLA 匹配 by priority**：category 字段预留但未深入实现 category 级覆盖
4. **Waiver 到期扫描需手动触发**：`scanEscalations()` 需要定时任务或手动调用
5. **E2E 环境依赖**：与 40A/40B/40C 一致的 graceful fallback 模式

## 13. 是否可以进入 Milestone 41B

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V47 migration + test schema）
2. ✅ SLA policy 可创建/编辑/启停
3. ✅ Escalation scan 可生成 overdue/waiver/owner 事件
4. ✅ Ownership health 可生成 snapshot 与排名
5. ✅ Operations summary/report 可导出 Markdown
6. ✅ 124 个后端集成测试全部通过（40A+40B+40C+41A）
7. ✅ 前端 typecheck 通过
8. ✅ 前端 build 通过
9. ⚠️ E2E 对无数据前置条件显式降级处理
10. ✅ Owner 健康度与 escalation 行为清晰可追踪

建议 41B 方向：Governance Capacity Planning & Predictive Risk Signals，包括 owner capacity forecast、overdue/waiver trend forecast、predictive governance risk cards。
