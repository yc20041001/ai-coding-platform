# Milestone 37I: Incident SLA / Escalation Mock / Review Timeline

## 1. 背景

Milestone 37H 已经完成 **Tool Execution Incident Workflow & Alert Routing**：

- `tool_incident`
- `tool_alert_rule`
- `tool_alert_delivery`
- Incident CRUD
- Problem Jobs 同步
- Mock Alert Routing
- Observability Incident 面板
- Trace Drawer / Operator Review 入口

当前系统已经具备：

```text
Problem Job / Trace / Review
  -> Incident
  -> Mock Alert Delivery
  -> ACK / RESOLVE
```

但 Incident 处理还缺少三个生产化关键点：

1. **SLA**：不同严重级别应该有不同处理时限，并能标识是否超时。
2. **Escalation**：超过 SLA 后应该生成 Mock 升级记录和告警投递。
3. **Timeline**：Incident 的创建、确认、解决、重开、告警投递、关联审查都应该形成完整时间线。

Milestone 37I 的目标是新增：

```text
Incident SLA / Escalation Mock / Review Timeline
```

让事件处理从：

```text
有状态
```

升级为：

```text
有时限、有升级、有完整处理轨迹
```

---

## 2. 总目标

实现 Incident SLA、Mock Escalation 和 Timeline：

1. 为 Incident 增加 SLA 字段：`dueAt`、`breachedAt`、`slaStatus`。
2. 根据 severity 自动计算 dueAt。
3. 支持手动刷新 SLA 状态。
4. 支持扫描已超时 Incident 并生成 Mock Escalation。
5. 新增 Escalation Policy / Escalation Event。
6. 新增 Incident Timeline 聚合 API。
7. Timeline 聚合 incident 状态变更、alert delivery、operator review、trace evidence、escalation。
8. 前端 Incident Detail Drawer 展示 SLA countdown / breached 状态。
9. 前端展示 Timeline。
10. 前端支持手动触发 SLA Scan / Escalation Mock。
11. 补齐后端测试与前端 E2E。

完成后，从：

```text
Incident 可处理
```

升级为：

```text
Incident 可按 SLA 管理、可升级、可审计处理过程
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不执行 `git checkout` / `git pull` / `git fetch`。
4. 不执行 `git reset` / `git apply`。
5. 不执行 `git add` / `git commit` / `git push`。
6. 不写真实代码文件。
7. 不修改 workspace 文件。
8. 不发送真实 Slack / PagerDuty / Email。
9. Escalation 只写数据库 Mock 记录。
10. Escalation 不自动 retry / cancel / approve 工具。
11. Escalation 不改变 Tool Execution / Job 原始结果。
12. SLA 扫描只更新 Incident SLA 字段和 Escalation 记录。
13. Timeline 只聚合已有数据，不重新执行工具。
14. 不破坏 36A-37H 已有 API。
15. 前端保持中文暗色科技风 UI。

允许做：

1. 新增 SLA 字段 migration。
2. 新增 escalation 表。
3. 新增 timeline DTO 和 API。
4. 手动触发 SLA scan。
5. 手动触发 mock escalation。
6. 前端展示 SLA / Timeline / Escalation。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V35__alter_tool_incident_sla_escalation.sql
```

### 4.1 tool_incident 增加 SLA 字段

```sql
ALTER TABLE tool_incident
  ADD COLUMN sla_minutes INT NULL,
  ADD COLUMN due_at DATETIME NULL,
  ADD COLUMN breached_at DATETIME NULL,
  ADD COLUMN sla_status VARCHAR(32) NULL,
  ADD COLUMN escalation_level INT NOT NULL DEFAULT 0;

CREATE INDEX idx_tool_incident_sla_status ON tool_incident(sla_status);
CREATE INDEX idx_tool_incident_due_at ON tool_incident(due_at);
```

如果 MySQL 不支持某些 `CREATE INDEX` 重复执行防御语法，不要使用 `IF NOT EXISTS`，Flyway 保证只执行一次。

### 4.2 tool_escalation_policy

```sql
CREATE TABLE tool_escalation_policy (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    severity VARCHAR(32) NOT NULL,
    sla_minutes INT NOT NULL,
    escalation_after_minutes INT NOT NULL,
    max_escalation_level INT NOT NULL DEFAULT 2,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_escalation_policy_project(project_id),
    KEY idx_tool_escalation_policy_enabled(enabled),
    KEY idx_tool_escalation_policy_severity(severity)
);
```

### 4.3 tool_escalation_event

```sql
CREATE TABLE tool_escalation_event (
    id BIGINT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    policy_id BIGINT NULL,
    escalation_level INT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    reason TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_escalation_event_incident(incident_id),
    KEY idx_tool_escalation_event_project_time(project_id, create_time),
    KEY idx_tool_escalation_event_status(status)
);
```

无物理外键，保持项目风格。

---

## 5. 枚举设计

新增：

```text
ToolIncidentSlaStatus.java
ToolEscalationEventStatus.java
```

### 5.1 ToolIncidentSlaStatus

```text
NOT_STARTED
WITHIN_SLA
AT_RISK
BREACHED
RESOLVED
WAIVED
```

建议规则：

- OPEN / ACKNOWLEDGED 且未超时：`WITHIN_SLA`
- 距 dueAt 剩余不足 25%：`AT_RISK`
- 当前时间超过 dueAt：`BREACHED`
- RESOLVED / WONT_FIX / FALSE_POSITIVE：`RESOLVED`
- 如果 severity=INFO 且无 dueAt：`WAIVED`

### 5.2 ToolEscalationEventStatus

```text
CREATED
DELIVERED
SKIPPED
FAILED
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ToolEscalationPolicyEntity.java
ToolEscalationEventEntity.java
ToolEscalationPolicyMapper.java
ToolEscalationEventMapper.java
```

新增 DTO：

```text
ToolEscalationPolicyResponse.java
CreateToolEscalationPolicyRequest.java
UpdateToolEscalationPolicyRequest.java
ToolEscalationEventResponse.java
ToolIncidentTimelineEventResponse.java
ToolIncidentTimelineResponse.java
ToolIncidentSlaScanResponse.java
ToolIncidentEscalationScanResponse.java
```

修改：

```text
ToolIncidentResponse.java
```

新增字段：

```java
private Integer slaMinutes;
private LocalDateTime dueAt;
private LocalDateTime breachedAt;
private String slaStatus;
private Integer escalationLevel;
```

---

## 7. SLA 规则设计

### 7.1 默认 SLA

如果没有项目自定义 escalation policy，使用内置默认 SLA：

| Severity | SLA |
|---|---:|
| CRITICAL | 30 分钟 |
| HIGH | 120 分钟 |
| MEDIUM | 480 分钟 |
| LOW | 1440 分钟 |
| INFO | 无 SLA / WAIVED |

Incident 创建时：

```text
slaMinutes = severity 对应默认值
dueAt = firstSeenAt + slaMinutes
slaStatus = WITHIN_SLA 或 WAIVED
escalationLevel = 0
```

### 7.2 自定义 Policy

项目可以创建 escalation policy：

```json
{
  "name": "High severity escalation",
  "severity": "HIGH",
  "slaMinutes": 60,
  "escalationAfterMinutes": 90,
  "maxEscalationLevel": 2,
  "channel": "MOCK_SLACK",
  "routeTarget": "#ai-platform-alerts"
}
```

匹配规则：

- 优先匹配 project-specific policy。
- 若无项目规则，使用默认规则。
- severity 精确匹配。
- INFO 默认不升级。

---

## 8. 后端服务设计

### 8.1 ToolIncidentSlaService

新增：

```text
ToolIncidentSlaService.java
```

职责：

1. Incident 创建时初始化 SLA。
2. Incident 更新 severity 时重新计算 SLA。
3. Incident 状态变为终态时设置 slaStatus=RESOLVED。
4. 手动 scan 时刷新 OPEN / ACKNOWLEDGED incidents 的 SLA 状态。
5. 返回扫描统计。

建议方法：

```java
public void initializeSla(ToolIncidentEntity incident)

public void refreshSla(ToolIncidentEntity incident)

public ToolIncidentSlaScanResponse scanProjectSla(Long projectId)

public ToolIncidentSlaScanResponse scanAllSla()
```

扫描响应：

```java
public class ToolIncidentSlaScanResponse {
    private Integer scanned;
    private Integer withinSla;
    private Integer atRisk;
    private Integer breached;
    private Integer resolved;
}
```

### 8.2 ToolEscalationPolicyService

新增：

```text
ToolEscalationPolicyService.java
```

职责：

1. 创建 policy。
2. 更新 policy。
3. 查询项目 policies。
4. 根据 incident severity 查找 policy。
5. 提供默认 policy fallback。

权限：

- 创建 / 更新：项目 OWNER+。
- 查询：项目 VIEWER+。

### 8.3 ToolEscalationService

新增：

```text
ToolEscalationService.java
```

职责：

1. 扫描 breached incidents。
2. 根据 policy 判断是否应升级。
3. 创建 escalation event。
4. 调用 ToolAlertDeliveryService 写入 Mock delivery。
5. 更新 incident.escalationLevel。

建议方法：

```java
public ToolIncidentEscalationScanResponse scanProjectEscalations(Long projectId)

public ToolEscalationEventResponse escalateIncident(Long incidentId, String reason)

public List<ToolEscalationEventResponse> listIncidentEscalations(Long incidentId)
```

扫描响应：

```java
public class ToolIncidentEscalationScanResponse {
    private Integer scanned;
    private Integer escalated;
    private Integer skipped;
    private Integer maxLevelReached;
}
```

幂等规则：

- 同一个 incident + escalationLevel 只能创建一个 event。
- 如果 incident 已终态，不升级。
- 如果 escalationLevel >= maxEscalationLevel，不升级。

### 8.4 ToolIncidentTimelineService

新增：

```text
ToolIncidentTimelineService.java
```

职责：

1. 聚合 Incident 状态时间。
2. 聚合 Alert Delivery。
3. 聚合 Escalation Event。
4. 聚合 Operator Review。
5. 聚合 Tool Trace 关键事件。
6. 返回按时间排序的 timeline。

Timeline event 类型建议：

```text
INCIDENT_CREATED
INCIDENT_ACKNOWLEDGED
INCIDENT_RESOLVED
INCIDENT_REOPENED
SLA_AT_RISK
SLA_BREACHED
ESCALATION_CREATED
ALERT_DELIVERED
ALERT_FAILED
OPERATOR_REVIEW_CREATED
OPERATOR_REVIEW_RESOLVED
TRACE_EVENT
```

---

## 9. 现有服务改造

### 9.1 ToolIncidentService

修改：

1. 创建 incident 后调用 `ToolIncidentSlaService.initializeSla()`。
2. 更新 severity 后重新计算 SLA。
3. 状态终态后设置 SLA resolved。
4. 重新打开后重新计算 SLA。
5. Response 包含 SLA 字段。

### 9.2 ToolAlertDeliveryService

修改：

1. 支持 escalation event 触发的 delivery。
2. delivery payload 中包含 incidentId、escalationLevel、slaStatus、dueAt。
3. 仍然不发送真实外部请求。

### 9.3 ToolIncidentAlertController

可以扩展原 controller，或新增：

```text
ToolIncidentSlaController.java
```

保持路径清晰即可。

---

## 10. 后端 API

### 10.1 SLA Scan

```http
POST /api/projects/{projectId}/incidents/scan-sla
```

权限：

```text
项目 MAINTAINER+
```

响应：

```json
{
  "code": "OK",
  "data": {
    "scanned": 10,
    "withinSla": 5,
    "atRisk": 2,
    "breached": 3,
    "resolved": 0
  }
}
```

### 10.2 Escalation Scan

```http
POST /api/projects/{projectId}/incidents/scan-escalations
```

权限：

```text
项目 MAINTAINER+
```

响应：

```json
{
  "code": "OK",
  "data": {
    "scanned": 3,
    "escalated": 2,
    "skipped": 1,
    "maxLevelReached": 0
  }
}
```

### 10.3 Manual Escalate Incident

```http
POST /api/orchestration/incidents/{incidentId}/escalate
```

权限：

```text
项目 MAINTAINER+
```

请求：

```json
{
  "reason": "超过 SLA 后手动升级。"
}
```

### 10.4 Incident Timeline

```http
GET /api/orchestration/incidents/{incidentId}/timeline
```

权限：

```text
项目 VIEWER+
```

### 10.5 Escalation Policies

```http
POST /api/projects/{projectId}/escalation-policies
PUT /api/orchestration/escalation-policies/{policyId}
GET /api/projects/{projectId}/escalation-policies
```

权限：

```text
创建 / 更新：OWNER+
查询：VIEWER+
```

### 10.6 Escalation Events

```http
GET /api/orchestration/incidents/{incidentId}/escalations
```

权限：

```text
项目 VIEWER+
```

---

## 11. 前端设计

### 11.1 ToolIncidentPanel 增强

修改：

```text
frontend/src/modules/admin/components/ToolIncidentPanel.vue
```

新增：

1. SLA 状态列。
2. Due At 列。
3. Breached At 列。
4. Escalation Level 列。
5. “扫描 SLA”按钮。
6. “扫描升级”按钮。
7. “手动升级”按钮。
8. SLA 状态筛选。

推荐 data-testid：

```text
tool-incident-sla-status
tool-incident-due-at
tool-incident-breached-at
tool-incident-escalation-level
tool-incident-scan-sla-button
tool-incident-scan-escalation-button
tool-incident-escalate-button
```

### 11.2 ToolIncidentDetailDrawer

如果当前 ObservabilityPage 已有 incident detail drawer，可增强；否则新增：

```text
frontend/src/modules/admin/components/ToolIncidentDetailDrawer.vue
```

展示：

```text
Incident Summary
SLA
Escalation
Timeline
Alert Deliveries
Related Trace
Related Operator Review
```

推荐 data-testid：

```text
tool-incident-detail-drawer
tool-incident-sla-card
tool-incident-timeline
tool-incident-timeline-event
tool-incident-escalation-list
tool-incident-delivery-list
```

### 11.3 ToolEscalationPolicyPanel

新增：

```text
frontend/src/modules/admin/components/ToolEscalationPolicyPanel.vue
```

功能：

1. 展示 escalation policies。
2. 创建 policy。
3. 更新启用状态。
4. 展示 severity、slaMinutes、escalationAfterMinutes、maxEscalationLevel、channel。

推荐 data-testid：

```text
tool-escalation-policy-panel
tool-escalation-policy-create-button
tool-escalation-policy-list
tool-escalation-policy-enabled-switch
```

### 11.4 ObservabilityPage 集成

修改：

```text
frontend/src/modules/admin/pages/ObservabilityPage.vue
```

在 Incident / Alert Rule 旁边加入：

```text
ToolEscalationPolicyPanel
ToolIncidentDetailDrawer
```

---

## 12. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
```

新增：

```ts
export interface ToolIncidentTimelineEvent {
  eventType: string
  title: string
  description?: string
  status?: string
  eventTime?: string
  metadata?: Record<string, unknown>
}

export interface ToolIncidentTimeline {
  incidentId: string
  events: ToolIncidentTimelineEvent[]
}

export interface ToolIncidentSlaScanResult {
  scanned: number
  withinSla: number
  atRisk: number
  breached: number
  resolved: number
}

export interface ToolIncidentEscalationScanResult {
  scanned: number
  escalated: number
  skipped: number
  maxLevelReached: number
}

export interface ToolEscalationPolicy {
  id: string
  projectId?: string
  name: string
  enabled: boolean
  severity: string
  slaMinutes: number
  escalationAfterMinutes: number
  maxEscalationLevel: number
  channel: string
  routeTarget?: string
}

export interface ToolEscalationEvent {
  id: string
  incidentId: string
  projectId: string
  policyId?: string
  escalationLevel: number
  severity: string
  channel: string
  routeTarget?: string
  status: string
  reason?: string
  createTime: string
}
```

新增函数：

```ts
scanIncidentSla(projectId: string)
scanIncidentEscalations(projectId: string)
escalateIncident(incidentId: string, reason?: string)
getIncidentTimeline(incidentId: string)
listIncidentEscalations(incidentId: string)
createEscalationPolicy(projectId: string, data: CreateToolEscalationPolicyRequest)
updateEscalationPolicy(policyId: string, data: UpdateToolEscalationPolicyRequest)
listProjectEscalationPolicies(projectId: string)
```

---

## 13. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolIncidentSlaEscalationIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/ToolIncidentTimelineIntegrationTest.java
```

至少 40 个测试。

### 13.1 SLA 初始化

1. CRITICAL incident 默认 SLA 为 30 分钟。
2. HIGH incident 默认 SLA 为 120 分钟。
3. MEDIUM incident 默认 SLA 为 480 分钟。
4. LOW incident 默认 SLA 为 1440 分钟。
5. INFO incident slaStatus=WAIVED。
6. dueAt = firstSeenAt + slaMinutes。

### 13.2 SLA 刷新 / 状态

7. 未超时为 WITHIN_SLA。
8. 接近 dueAt 为 AT_RISK。
9. 超过 dueAt 为 BREACHED。
10. RESOLVED incident slaStatus=RESOLVED。
11. WONT_FIX incident slaStatus=RESOLVED。
12. FALSE_POSITIVE incident slaStatus=RESOLVED。
13. reopen incident 后重新计算 SLA。
14. severity 更新后重新计算 SLA。
15. scanProjectSla 返回统计。

### 13.3 Escalation Policy

16. OWNER 可创建 policy。
17. VIEWER 不可创建 policy。
18. list policies 成功。
19. update policy enabled=false 成功。
20. invalid severity 返回 BAD_REQUEST。
21. invalid channel 返回 BAD_REQUEST。
22. maxEscalationLevel 小于 1 返回 BAD_REQUEST。

### 13.4 Escalation Event

23. breached incident 可手动 escalate。
24. OPEN 但未 breached incident 可手动 escalate（允许人工升级）。
25. resolved incident 不可自动 escalate。
26. scan escalations 创建 event。
27. scan escalations 幂等。
28. 同 level 不重复创建 event。
29. max level reached 后不再升级。
30. escalation 创建 alert delivery。
31. disabled policy 不创建 escalation。
32. list incident escalations 成功。

### 13.5 Timeline

33. timeline 包含 INCIDENT_CREATED。
34. ACK 后 timeline 包含 INCIDENT_ACKNOWLEDGED。
35. RESOLVE 后 timeline 包含 INCIDENT_RESOLVED。
36. escalation 后 timeline 包含 ESCALATION_CREATED。
37. alert delivery 后 timeline 包含 ALERT_DELIVERED。
38. operator review 关联后 timeline 包含 OPERATOR_REVIEW_CREATED。
39. trace event 可进入 timeline。
40. timeline 按时间升序。

### 13.6 权限

41. 未登录 timeline 返回 UNAUTHORIZED。
42. 非项目成员 timeline 返回 PROJECT_ACCESS_DENIED。
43. VIEWER 可查询 timeline。
44. VIEWER 不可 scan SLA。
45. MAINTAINER 可 scan SLA / escalation。

可以超过 40 个。

---

## 14. 前端 E2E 要求

新增或修改：

```text
frontend/e2e/incident-alert-routing.spec.ts
```

也可新增：

```text
frontend/e2e/incident-sla-escalation.spec.ts
```

至少 8 个 E2E：

1. Incident Panel 显示 SLA 状态列。
2. 点击扫描 SLA 显示结果。
3. 点击扫描升级显示结果。
4. Incident Detail Drawer 显示 SLA 卡片。
5. Incident Detail Drawer 显示 Timeline。
6. 手动升级 Incident 成功。
7. Escalation Policy Panel 可创建 policy。
8. Escalation list / alert delivery 可见。
9. 页面无 JS error。

推荐 data-testid：

```text
tool-incident-sla-status
tool-incident-scan-sla-button
tool-incident-scan-escalation-button
tool-incident-detail-drawer
tool-incident-sla-card
tool-incident-timeline
tool-incident-escalate-button
tool-escalation-policy-panel
tool-escalation-policy-create-button
```

---

## 15. 文档与报告

完成后新增：

```text
docs/milestone-37i-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. SLA 字段与默认规则说明
3. Escalation Policy / Event 表说明
4. SLA Service 设计说明
5. Escalation Service 设计说明
6. Timeline Service 设计说明
7. API 清单
8. 前端 Incident Detail / Timeline / Policy Panel 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37J

---

## 16. 验收标准

完成后必须满足：

1. Incident 创建时自动计算 SLA。
2. Incident 可显示 dueAt / slaStatus / escalationLevel。
3. SLA scan 可更新 AT_RISK / BREACHED。
4. Escalation policy 可创建、更新、查询。
5. Breached incident 可生成 mock escalation event。
6. Escalation event 可触发 mock alert delivery。
7. Incident timeline 可返回创建、确认、解决、升级、投递等事件。
8. 前端可展示 SLA、Timeline、Escalation。
9. 权限校验正确。
10. 不发送真实外部通知。
11. 后端测试通过。
12. 前端 typecheck / build / E2E 通过。

---

## 17. 非目标

本阶段不做：

1. 不做真实 Slack / Email / PagerDuty。
2. 不做真实 webhook HTTP 请求。
3. 不做 on-call 排班。
4. 不做复杂 escalation tree。
5. 不做工作日/节假日日历。
6. 不做 SLA 暂停 / 恢复。
7. 不做多租户 SLA。
8. 不做 Prometheus Alertmanager。
9. 不做 PDF 导出。
10. 不做自动 retry / cancel / approve。

这些可以放到后续 Milestone。

---

## 18. 建议后续 Milestone

完成 37I 后，建议进入：

```text
Milestone 37J: Incident Knowledge Base & Root Cause Notes
```

候选能力：

- Incident root cause notes。
- Known issue 模板。
- Incident 与 RAG 文档关联。
- 复盘记录。
- 相似 Incident 搜索。

也可以进入：

```text
Milestone 38A: Semantic Code Search / RAG Evaluation
```

---

## 19. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37I。

文档路径：
docs/milestone-37i-incident-sla-escalation-timeline.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37H Tool Execution Incident Workflow & Alert Routing 基础上，新增 Incident SLA / Escalation Mock / Review Timeline。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 不发送真实 Slack / PagerDuty / Email。
9. Escalation 只写数据库 Mock 记录，不发送真实外部请求。
10. Escalation 不自动 retry / cancel / approve 工具。
11. Incident / Timeline 不改变 Tool Execution / Job 原始结果。
12. 不要破坏 36A-37H 已有 API。
13. 不要破坏 Tool Operator Review API。
14. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
15. IDs 对外保持 String。
16. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V35__alter_tool_incident_sla_escalation.sql。
2. tool_incident 增加 slaMinutes / dueAt / breachedAt / slaStatus / escalationLevel。
3. 新增 tool_escalation_policy / tool_escalation_event 表。
4. 新增 ToolIncidentSlaStatus / ToolEscalationEventStatus 枚举。
5. 新增 ToolEscalationPolicyEntity / ToolEscalationEventEntity 和 Mapper。
6. 新增 SLA / Escalation / Timeline 相关 DTO。
7. 新增 ToolIncidentSlaService。
8. 新增 ToolEscalationPolicyService。
9. 新增 ToolEscalationService。
10. 新增 ToolIncidentTimelineService。
11. 改造 ToolIncidentService：创建/更新/重开/终态时维护 SLA 字段。
12. 新增 API：
   - POST /api/projects/{projectId}/incidents/scan-sla
   - POST /api/projects/{projectId}/incidents/scan-escalations
   - POST /api/orchestration/incidents/{incidentId}/escalate
   - GET /api/orchestration/incidents/{incidentId}/timeline
   - POST /api/projects/{projectId}/escalation-policies
   - PUT /api/orchestration/escalation-policies/{policyId}
   - GET /api/projects/{projectId}/escalation-policies
   - GET /api/orchestration/incidents/{incidentId}/escalations
13. 前端增强 ToolIncidentPanel，显示 SLA / dueAt / escalationLevel，并支持 scan SLA / scan escalation。
14. 新增或增强 ToolIncidentDetailDrawer，展示 SLA card、Timeline、Escalation、Alert Deliveries。
15. 新增 ToolEscalationPolicyPanel。
16. ObservabilityPage 集成新面板。
17. 后端测试不少于 40 个。
18. 前端 E2E 不少于 8 个。
19. 新增 docs/milestone-37i-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. SLA 字段与默认规则说明
3. Escalation Policy / Event 表说明
4. SLA Service 设计说明
5. Escalation Service 设计说明
6. Timeline Service 设计说明
7. API 清单
8. 前端 Incident Detail / Timeline / Policy Panel 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37J

现在开始实现，不要只给计划。
```
