# Milestone 41A: Governance SLA, Escalation & Ownership Health

## 1. 背景

截至 Milestone 40C，平台已经具备组织级治理闭环：

```text
40A
  Multi-project Release Governance

40B
  Organization Policy / Guardrail / Drift

40C
  Recommendation Workflow / Waiver Management / Workflow Snapshot
```

现在系统已经能回答：

```text
有哪些治理建议？
谁在处理？
哪些已经完成？
哪些有 waiver？
整体完成率和逾期率如何？
```

但治理系统真正进入“可运营”阶段，还缺三块关键能力：

```text
1. recommendation 有没有明确 SLA？
2. 逾期项有没有升级（escalation）机制？
3. owner 本身的负载和健康度是否可见？
```

当前 40C 的已知限制已经直接指出了这个缺口：

```text
P0/P1 才同步
waiver 无自动提醒
owner 不校验存在性
```

所以 Milestone 41A 的目标是新增：

```text
Governance SLA, Escalation & Ownership Health
```

让平台从：

```text
有 recommendation 工作流
```

升级为：

```text
有治理 SLA、逾期升级、waiver 到期提醒，以及 owner 负载/健康度度量
```

---

## 2. 总目标

实现治理运营层能力：

1. 新增 Governance SLA Policy 数据模型。
2. 新增 Governance Escalation Event 数据模型。
3. 新增 Governance Ownership Snapshot 数据模型。
4. 支持为 recommendation priority / category 定义默认 SLA。
5. 支持 overdue recommendation 自动生成 escalation event。
6. 支持 waiver 即将到期与已到期提醒。
7. 支持 owner existence 校验与 owner 健康度统计。
8. 支持 owner load / overdue load / completed throughput 指标。
9. 支持导出 Governance Operations Summary Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
recommendation 只是可跟踪工作项
```

升级为：

```text
recommendation 是有 SLA、有升级路径、有 owner 健康管理的治理运营事项
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动修改已有 release / rollout / sign-off / verification 原始记录。
4. 不自动完成 recommendation。
5. 不自动批准 waiver。
6. 不自动指派 owner 到真实用户，除非显式请求。
7. 不自动关闭 incident / alert / feedback / recommendation。
8. 不调用真实 AI 自动生成治理结论。
9. escalation 只记录事件与提示，不触发外部通知系统。
10. ownership health 只基于平台内结构化数据计算。
11. 不破坏 1-40C 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 SLA policy / escalation event / ownership snapshot 表。
2. 新增 SLA 刷新、overdue 扫描、waiver expiry 扫描。
3. 新增 owner 负载 / 健康度 / throughput dashboard。
4. 新增 Markdown summary / report 导出。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V47__init_governance_sla_escalation_ownership_tables.sql
```

### 4.1 governance_sla_policy

```sql
CREATE TABLE governance_sla_policy (
    id BIGINT PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    category VARCHAR(64) NULL,
    sla_hours INT NOT NULL,
    warning_hours INT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_sla_policy(policy_key),
    KEY idx_governance_sla_policy_priority(priority, enabled)
);
```

### 4.2 governance_escalation_event

```sql
CREATE TABLE governance_escalation_event (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    escalation_type VARCHAR(64) NOT NULL,
    escalation_level VARCHAR(32) NOT NULL,
    event_status VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL,
    triggered_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_escalation_recommendation(recommendation_id, triggered_at),
    KEY idx_governance_escalation_project(project_id, triggered_at),
    KEY idx_governance_escalation_status(event_status, escalation_level)
);
```

### 4.3 governance_ownership_snapshot

```sql
CREATE TABLE governance_ownership_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    owner_id BIGINT NOT NULL,
    owner_name VARCHAR(128) NOT NULL,
    total_assigned_count INT NOT NULL DEFAULT 0,
    open_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0,
    completed_7d_count INT NOT NULL DEFAULT 0,
    active_waiver_count INT NOT NULL DEFAULT 0,
    owner_health_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    owner_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_ownership_snapshot_date(snapshot_date, owner_health_score),
    KEY idx_governance_ownership_snapshot_owner(owner_id, snapshot_date),
    KEY idx_governance_ownership_snapshot_level(snapshot_date, owner_health_level)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
GovernanceEscalationType.java
GovernanceEscalationLevel.java
GovernanceEscalationStatus.java
OwnerHealthLevel.java
```

### 5.1 GovernanceEscalationType

```text
OVERDUE_RECOMMENDATION
WAIVER_EXPIRING_SOON
WAIVER_EXPIRED
OWNER_OVERLOADED
OWNER_MISSING
```

### 5.2 GovernanceEscalationLevel

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.3 GovernanceEscalationStatus

```text
OPEN
ACKNOWLEDGED
RESOLVED
IGNORED
```

### 5.4 OwnerHealthLevel

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
GovernanceSlaPolicyEntity.java
GovernanceEscalationEventEntity.java
GovernanceOwnershipSnapshotEntity.java

GovernanceSlaPolicyMapper.java
GovernanceEscalationEventMapper.java
GovernanceOwnershipSnapshotMapper.java
```

DTO 建议：

```text
GovernanceSlaPolicyResponse.java
CreateGovernanceSlaPolicyRequest.java
UpdateGovernanceSlaPolicyRequest.java

GovernanceEscalationEventResponse.java
GovernanceEscalationDashboardResponse.java

GovernanceOwnershipSnapshotResponse.java
GovernanceOwnershipDashboardResponse.java

GovernanceOperationsSummaryResponse.java
```

### 6.1 GovernanceEscalationDashboardResponse

建议字段：

```text
snapshotDate
openEscalationCount
highEscalationCount
criticalEscalationCount
waiverExpiringSoonCount
waiverExpiredCount
ownerMissingCount
topEscalations
```

### 6.2 GovernanceOwnershipDashboardResponse

建议字段：

```text
snapshotDate
ownerCount
healthyCount
watchCount
riskCount
criticalCount
topOverloadedOwners
topHealthyOwners
overallThroughput7d
```

---

## 7. 服务设计

新增应用服务：

```text
GovernanceSlaPolicyService.java
GovernanceEscalationService.java
GovernanceOwnershipHealthService.java
```

### 7.1 GovernanceSlaPolicyService

职责：

1. 管理 SLA policy 的 CRUD。
2. 提供默认 policy 初始化。
3. 根据 recommendation priority / category 解析匹配的 SLA。
4. 供 overdue 与 warning 判定使用。

默认建议：

```text
P0 -> 24h
P1 -> 72h
P2 -> 168h
P3 -> 336h
```

### 7.2 GovernanceEscalationService

职责：

1. 扫描 overdue recommendation。
2. 扫描 waiver 即将到期（如 24h 内）与已到期。
3. 检测 owner 缺失。
4. 检测 owner overload（例如 open / overdue 超阈值）。
5. 生成 escalation event。
6. 支持 event 状态流转：

```text
OPEN -> ACKNOWLEDGED -> RESOLVED
OPEN -> IGNORED
ACKNOWLEDGED -> RESOLVED
```

注意：

1. escalation 事件需要幂等，避免同一 recommendation 在同一周期重复写入相同事件。
2. 不接入外部消息通知，本阶段只记录与展示。

### 7.3 GovernanceOwnershipHealthService

职责：

1. 校验 owner 是否存在于用户表。
2. 聚合每个 owner 的 open / in-progress / overdue / completed_7d / waiver 数据。
3. 计算 owner health score 与 level。
4. 输出 ownership dashboard。
5. 导出 governance operations summary。

建议 owner health score 示例：

```text
基础分 100
- overdueCount * 12
- openCount * 3
- activeWaiverCount * 4
+ completed_7d_count * 2
```

level 示例：

```text
>= 85 HEALTHY
>= 60 WATCH
>= 35 RISK
< 35 CRITICAL
```

---

## 8. API 设计

新增 Controller：

```text
GovernanceOperationsController.java
```

建议端点：

### 8.1 SLA Policy

```text
POST   /api/governance-operations/sla-policies
GET    /api/governance-operations/sla-policies
GET    /api/governance-operations/sla-policies/{policyId}
PUT    /api/governance-operations/sla-policies/{policyId}
POST   /api/governance-operations/sla-policies/{policyId}/status
```

### 8.2 Escalation

```text
POST   /api/governance-operations/escalations/scan
GET    /api/governance-operations/escalations
GET    /api/governance-operations/escalations/dashboard
POST   /api/governance-operations/escalations/{eventId}/status
```

### 8.3 Ownership Health

```text
POST   /api/governance-operations/ownership/refresh
GET    /api/governance-operations/ownership
GET    /api/governance-operations/ownership/dashboard
```

### 8.4 Summary / Report

```text
GET    /api/governance-operations/summary
GET    /api/governance-operations/report
```

权限建议：

```text
查看：ADMIN
编辑 SLA policy：ADMIN
scan / refresh / status update：ADMIN
```

---

## 9. 聚合规则建议

### 9.1 Overdue 与 Warning

对 recommendation：

```text
elapsed >= warning_hours -> warning
elapsed >= sla_hours -> overdue + escalation candidate
```

### 9.2 Waiver Expiry Alert

建议：

```text
expiresAt 在未来 24h 内 -> WAIVER_EXPIRING_SOON
expiresAt < now -> WAIVER_EXPIRED
```

### 9.3 Owner Overload

示例规则：

```text
open_count >= 10 -> WATCH
overdue_count >= 5 -> RISK
overdue_count >= 10 或 owner 缺失 -> CRITICAL
```

### 9.4 Governance Operations Summary

建议至少包含：

```text
SLA policy 数
开放 escalation 数
高/严重 escalation 数
owner 健康度分布
overdue recommendation 数
即将到期 waiver 数
近 7 天完成 throughput
```

---

## 10. 前端设计

新增组件建议：

```text
GovernanceSlaPolicyPanel.vue
GovernanceEscalationPanel.vue
GovernanceOwnershipHealthPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 GovernanceSlaPolicyPanel

展示：

1. SLA policy 列表
2. create / edit dialog
3. priority / category / slaHours / warningHours / enabled

### 10.2 GovernanceEscalationPanel

展示：

1. escalation 指标卡
2. event 列表
3. type / level / status / owner / triggeredAt
4. acknowledge / resolve / ignore 按钮

### 10.3 GovernanceOwnershipHealthPanel

展示：

1. ownership health 指标卡
2. owner 排名表
3. 健康等级 tag
4. top overloaded / top healthy owner
5. throughput 指标

UI 要求：

1. 保持中文暗色科技风
2. 复用 `MetricTile`、`StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. escalation 要高亮严重等级
4. ownership 排名要便于扫描与比较

---

## 11. 后端测试要求

新增：

```text
GovernanceOperationsSlaEscalationIntegrationTest.java
```

不少于 34 个集成测试，建议覆盖：

1. create SLA policy success
2. update SLA policy success
3. disable SLA policy success
4. duplicate policyKey reject
5. list SLA policy success
6. overdue recommendation creates escalation
7. warning recommendation not escalated
8. waiver expiring soon creates escalation
9. waiver expired creates escalation
10. owner missing creates escalation
11. owner overload creates escalation
12. escalation scan idempotent
13. escalation status open -> acknowledged
14. escalation status acknowledged -> resolved
15. escalation status open -> ignored
16. invalid escalation transition reject
17. ownership refresh success
18. ownership snapshot owner count correct
19. owner health level healthy
20. owner health level watch
21. owner health level risk
22. owner health level critical
23. top overloaded owner returned
24. top healthy owner returned
25. throughput_7d calculation correct
26. summary response correct
27. report export markdown success
28. unauthorized access reject
29. non-admin edit reject
30. empty dataset returns empty dashboard
31. SLA policy matching by priority works
32. category-specific SLA overrides default
33. overdue rate reflected in summary
34. active waiver count reflected in owner snapshot

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/governance-operations.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 SLA policy panel
2. escalation panel renders
3. ownership health panel renders
4. create SLA dialog works
5. escalation action buttons visible
6. owner health tags visible
7. refresh / scan buttons visible
8. no JS errors on page load

如果测试环境没有 seeded governance workflow 数据：

1. 显式断言空态
2. 不把“无 operations 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-41a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. 三张 governance operations 表说明
3. GovernanceSlaPolicyService 设计说明
4. GovernanceEscalationService 设计说明
5. GovernanceOwnershipHealthService 设计说明
6. GovernanceSlaPolicyPanel 说明
7. GovernanceEscalationPanel 说明
8. GovernanceOwnershipHealthPanel 说明
9. SLA / Escalation / Ownership 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41B

---

## 14. 验收标准

必须全部满足：

1. governance_sla_policy / governance_escalation_event / governance_ownership_snapshot 三张表已落库
2. SLA policy 可创建 / 编辑 / 启停
3. escalation scan 可生成 overdue / waiver / owner 事件
4. ownership health 可生成 snapshot 与排名
5. operations summary / report 可导出
6. 后端集成测试通过
7. 前端 `npm run typecheck` 通过
8. 前端 `npm run build` 通过
9. 前端 E2E 通过或对无数据前置条件显式降级处理
10. owner 健康度与 escalation 行为清晰可追踪

---

## 15. 完成后的价值

完成 41A 后，平台将从：

```text
有治理工作流
```

升级为：

```text
有治理运营体系：SLA、升级、owner 健康、throughput
```

这一步会让治理体系从“可管理”进入“可持续运营”的阶段。

---

## 16. 后续建议

Milestone 41A 完成后，建议进入：

```text
Milestone 41B: Governance Capacity Planning & Predictive Risk Signals
```

重点可包括：

1. owner capacity forecast
2. overdue trend forecast
3. waiver expiry risk forecast
4. recommendation backlog health
5. predictive governance risk cards

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 41A。

文档路径：
docs/milestone-41a-governance-sla-escalation-ownership-health.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 40C recommendation workflow / waiver 基础上，新增 governance SLA、escalation 和 ownership health。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动修改已有 recommendation / waiver 原始记录，除非明确的状态流转或扫描逻辑要求。
6. 不要自动完成 recommendation。
7. 不要自动批准 waiver。
8. 不要自动关闭 incident / alert / feedback。
9. 不要调用真实 AI 自动生成治理结论。
10. escalation 只记录与展示，不接外部通知。
11. 不要破坏 1-40C 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V47__init_governance_sla_escalation_ownership_tables.sql。
2. 新增 governance_sla_policy / governance_escalation_event / governance_ownership_snapshot 三张表。
3. 新增 4 个枚举：
   - GovernanceEscalationType
   - GovernanceEscalationLevel
   - GovernanceEscalationStatus
   - OwnerHealthLevel
4. 新增实体、Mapper、DTO。
5. 新增 GovernanceSlaPolicyService。
6. 新增 GovernanceEscalationService。
7. 新增 GovernanceOwnershipHealthService。
8. 新增 API：
   - SLA policy CRUD / status
   - escalation scan / list / dashboard / status
   - ownership refresh / list / dashboard
   - operations summary / report
9. 前端新增：
   - GovernanceSlaPolicyPanel.vue
   - GovernanceEscalationPanel.vue
   - GovernanceOwnershipHealthPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 34 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-41a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 governance operations 表说明
3. GovernanceSlaPolicyService 设计说明
4. GovernanceEscalationService 设计说明
5. GovernanceOwnershipHealthService 设计说明
6. GovernanceSlaPolicyPanel 说明
7. GovernanceEscalationPanel 说明
8. GovernanceOwnershipHealthPanel 说明
9. SLA / Escalation / Ownership 边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 41B

现在开始实现，不要只给计划。
```
