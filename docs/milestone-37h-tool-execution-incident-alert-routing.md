# Milestone 37H: Tool Execution Incident Workflow & Alert Routing

## 1. 背景

Milestone 37D-37G 已经完成工具执行的指标、证据链、审计导出和人工审查：

- 37D: Tool Execution Metrics Dashboard
- 37E: Real Read-only Tool Adapter Hardening
- 37F: Tool Execution Trace & Evidence Viewer
- 37G: Tool Execution Audit Export & Operator Review

当前系统已经具备：

```text
Tool Execution
  -> Metrics
  -> Trace
  -> Audit Export
  -> Operator Review
```

但运维和试用阶段还缺少一个更高层的闭环：

```text
什么时候需要提醒人？
哪些失败应该升级为 Incident？
谁来处理？
是否已确认？
是否已解决？
是否需要关联 trace / review / export？
```

Milestone 37H 的目标是新增 **Tool Execution Incident Workflow & Alert Routing**：

```text
把 problem jobs、DLQ、retry 风暴、read-only warning、高风险审查等信号自动或手动升级为 Incident，并提供简单的告警路由和处理状态。
```

本阶段仍然不接真实 Slack / PagerDuty / Email，只做内部 Incident 数据模型、Mock Alert Route、前端运维面板和测试。

---

## 2. 总目标

实现工具执行 Incident 与告警路由基础能力：

1. 新增 Tool Incident 数据模型。
2. 新增 Incident Severity / Status / Source / Alert Channel 枚举。
3. 支持从 Tool Execution / Tool Job / Operator Review / Multi-Agent Run 创建 Incident。
4. 支持自动从 problem jobs 生成 Incident。
5. 支持 Incident 状态流转：OPEN → ACKNOWLEDGED → RESOLVED / WONT_FIX / FALSE_POSITIVE。
6. 支持简单 Alert Routing Rule。
7. 支持 Mock Alert Delivery 记录，不发送真实外部通知。
8. Observability 增加 Incident 面板。
9. Trace Drawer / Operator Review 中展示关联 Incident。
10. 后端测试与前端 E2E 覆盖。

完成后，从：

```text
问题可审查
```

升级为：

```text
问题可升级为 Incident、可分派、可确认、可解决、可审计
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
8. 不创建真实 patch 文件。
9. 不发送真实 Slack / PagerDuty / Email。
10. Alert Delivery 只写数据库 Mock 记录。
11. Incident 不自动 retry / cancel / approve 工具。
12. Incident 不改变 Tool Execution / Job 原始结果。
13. 不破坏 36A-37G 已有 API。
14. 不破坏 Tool Operator Review API。
15. 前端保持中文暗色科技风 UI。

允许做：

1. 新增 Incident 表。
2. 新增 Alert Rule / Delivery 表。
3. 从已有 problem jobs / trace / review 创建 Incident。
4. 手动触发 Mock Alert。
5. 前端展示 Incident 面板。
6. 前端展示 Mock Alert Delivery 历史。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V34__init_tool_incident_alert_tables.sql
```

### 4.1 tool_incident

```sql
CREATE TABLE tool_incident (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    tool_execution_id BIGINT NULL,
    tool_job_id BIGINT NULL,
    operator_review_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    resolution TEXT NULL,
    assignee_id BIGINT NULL,
    created_by BIGINT NULL,
    acknowledged_by BIGINT NULL,
    resolved_by BIGINT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_incident_project_time(project_id, create_time),
    KEY idx_tool_incident_status(status),
    KEY idx_tool_incident_severity(severity),
    KEY idx_tool_incident_execution(tool_execution_id),
    KEY idx_tool_incident_job(tool_job_id),
    KEY idx_tool_incident_review(operator_review_id),
    KEY idx_tool_incident_source(source_type, source_id),
    KEY idx_tool_incident_assignee(assignee_id)
);
```

### 4.2 tool_alert_rule

```sql
CREATE TABLE tool_alert_rule (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    source_type VARCHAR(32) NOT NULL,
    min_severity VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    config_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_alert_rule_project(project_id),
    KEY idx_tool_alert_rule_enabled(enabled),
    KEY idx_tool_alert_rule_source(source_type)
);
```

### 4.3 tool_alert_delivery

```sql
CREATE TABLE tool_alert_delivery (
    id BIGINT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    rule_id BIGINT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    payload TEXT NULL,
    error_message TEXT NULL,
    delivered_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_alert_delivery_incident(incident_id),
    KEY idx_tool_alert_delivery_project_time(project_id, create_time),
    KEY idx_tool_alert_delivery_status(status)
);
```

无物理外键，保持项目风格。

---

## 5. 枚举设计

新增：

```text
ToolIncidentStatus.java
ToolIncidentSeverity.java
ToolIncidentSourceType.java
ToolAlertChannel.java
ToolAlertDeliveryStatus.java
```

### 5.1 ToolIncidentStatus

```text
OPEN
ACKNOWLEDGED
RESOLVED
WONT_FIX
FALSE_POSITIVE
```

### 5.2 ToolIncidentSeverity

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

### 5.3 ToolIncidentSourceType

```text
TOOL_EXECUTION_FAILED
TOOL_JOB_FAILED
TOOL_JOB_RETRY_PENDING
TOOL_JOB_DEAD_LETTERED
READ_ONLY_CONTRACT_WARNING
TRACE_OUTPUT_PARSE_WARNING
HIGH_RISK_REVIEW
OPERATOR_REVIEW
MANUAL
```

### 5.4 ToolAlertChannel

```text
IN_APP
MOCK_WEBHOOK
MOCK_SLACK
MOCK_EMAIL
```

### 5.5 ToolAlertDeliveryStatus

```text
PENDING
DELIVERED
FAILED
SKIPPED
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ToolIncidentEntity.java
ToolAlertRuleEntity.java
ToolAlertDeliveryEntity.java

ToolIncidentMapper.java
ToolAlertRuleMapper.java
ToolAlertDeliveryMapper.java

CreateToolIncidentRequest.java
UpdateToolIncidentRequest.java
ToolIncidentResponse.java
CreateToolAlertRuleRequest.java
UpdateToolAlertRuleRequest.java
ToolAlertRuleResponse.java
ToolAlertDeliveryResponse.java
ToolIncidentSummaryResponse.java
```

### 6.1 CreateToolIncidentRequest

```java
public class CreateToolIncidentRequest {
    private String projectId;
    private String sourceType;
    private String sourceId;
    private String severity;
    private String title;
    private String summary;
    private String assigneeId;
    private String toolExecutionId;
    private String toolJobId;
    private String operatorReviewId;
}
```

校验：

- `projectId` 必填。
- `sourceType` 必填。
- `severity` 必填。
- `title` 必填，最长 255。
- `summary` 最长 4000。
- 如果提供 toolExecutionId / toolJobId / operatorReviewId，必须存在并属于 project。

### 6.2 UpdateToolIncidentRequest

```java
public class UpdateToolIncidentRequest {
    private String status;
    private String severity;
    private String title;
    private String summary;
    private String resolution;
    private String assigneeId;
}
```

状态规则：

- `OPEN -> ACKNOWLEDGED`
- `OPEN -> RESOLVED / WONT_FIX / FALSE_POSITIVE`
- `ACKNOWLEDGED -> RESOLVED / WONT_FIX / FALSE_POSITIVE`
- 终态可重新打开为 `OPEN`，但必须清空 resolvedBy / resolvedAt。
- status 改为 `ACKNOWLEDGED` 时设置 acknowledgedBy / acknowledgedAt。
- status 改为终态时设置 resolvedBy / resolvedAt。

### 6.3 ToolIncidentSummaryResponse

```java
public class ToolIncidentSummaryResponse {
    private Long openCount;
    private Long acknowledgedCount;
    private Long resolvedCount;
    private Long criticalCount;
    private Long highCount;
    private Long deadLetteredCount;
    private Long retryPendingCount;
}
```

---

## 7. 后端服务设计

### 7.1 ToolIncidentService

新增：

```text
ToolIncidentService.java
```

职责：

1. 创建 incident。
2. 更新 incident。
3. 查询 incident。
4. 查询项目 incident 列表。
5. 查询 incident summary。
6. 从 problem job 生成 incident。
7. 从 operator review 生成 incident。
8. 创建后触发 Mock Alert Routing。

建议方法：

```java
public ToolIncidentResponse createIncident(CreateToolIncidentRequest request)

public ToolIncidentResponse updateIncident(Long incidentId, UpdateToolIncidentRequest request)

public ToolIncidentResponse getIncident(Long incidentId)

public PageResult<ToolIncidentResponse> listProjectIncidents(Long projectId, String status, String severity, PageQuery pageQuery)

public ToolIncidentSummaryResponse getProjectIncidentSummary(Long projectId)

public ToolIncidentResponse createFromProblemJob(Long projectId, Long jobId)

public ToolIncidentResponse createFromOperatorReview(Long reviewId)
```

权限：

- 创建 incident：项目 MAINTAINER+。
- 更新 incident：项目 MAINTAINER+。
- 查询 incident：项目 VIEWER+。
- summary：项目 VIEWER+。

### 7.2 ToolAlertRuleService

新增：

```text
ToolAlertRuleService.java
```

职责：

1. 创建 alert rule。
2. 更新 alert rule。
3. 启用 / 禁用 alert rule。
4. 查询项目 alert rules。
5. 查找适用于 incident 的规则。

权限：

- 创建 / 更新 / 启禁用：项目 OWNER 或 ADMIN。
- 查询：项目 VIEWER+。

默认规则：

如果项目没有任何规则，系统可使用内置默认路由：

```text
HIGH/CRITICAL -> IN_APP
DEAD_LETTERED -> IN_APP
```

不强制写入数据库。

### 7.3 ToolAlertDeliveryService

新增：

```text
ToolAlertDeliveryService.java
```

职责：

1. 根据 incident 匹配 alert rule。
2. 生成 delivery payload。
3. 写入 tool_alert_delivery。
4. 对 `IN_APP` / `MOCK_*` channel 标记为 DELIVERED。
5. 不发送真实外部请求。

建议方法：

```java
public List<ToolAlertDeliveryResponse> routeIncident(ToolIncidentEntity incident)

public List<ToolAlertDeliveryResponse> listIncidentDeliveries(Long incidentId)

public ToolAlertDeliveryResponse retryDelivery(Long deliveryId)
```

`retryDelivery` 仍然只是 Mock delivery，不发送真实外部请求。

---

## 8. 自动 Incident 生成规则

新增服务方法可由 controller 手动触发：

```text
POST /api/projects/{projectId}/tool-incidents/sync-problem-jobs
```

扫描：

- FAILED jobs
- RETRY_PENDING jobs
- DEAD_LETTERED jobs

生成规则：

| Job Status | Incident Source | Severity |
|---|---|---|
| FAILED | TOOL_JOB_FAILED | MEDIUM |
| RETRY_PENDING | TOOL_JOB_RETRY_PENDING | LOW |
| DEAD_LETTERED | TOOL_JOB_DEAD_LETTERED | HIGH |

幂等规则：

同一个 `tool_job_id + source_type` 已有非终态 incident 时，不重复创建，只更新 `lastSeenAt`。

终态 incident 不重新打开，除非用户手动创建新 incident。

---

## 9. 后端 API

### 9.1 创建 Incident

```http
POST /api/projects/{projectId}/tool-incidents
```

权限：

```text
项目 MAINTAINER+
```

请求：

```json
{
  "sourceType": "TOOL_JOB_DEAD_LETTERED",
  "sourceId": "501",
  "severity": "HIGH",
  "title": "工具 Job 进入 DLQ",
  "summary": "READ_FILE_SNIPPET 多次重试失败后进入 DLQ。",
  "toolJobId": "501"
}
```

### 9.2 更新 Incident

```http
PUT /api/tool-incidents/{incidentId}
```

权限：

```text
项目 MAINTAINER+
```

请求：

```json
{
  "status": "ACKNOWLEDGED",
  "summary": "已确认，正在排查。"
}
```

### 9.3 查询 Incident

```http
GET /api/tool-incidents/{incidentId}
```

权限：

```text
项目 VIEWER+
```

### 9.4 查询项目 Incidents

```http
GET /api/projects/{projectId}/tool-incidents?status=&severity=&page=&size=
```

权限：

```text
项目 VIEWER+
```

### 9.5 查询项目 Incident Summary

```http
GET /api/projects/{projectId}/tool-incidents/summary
```

权限：

```text
项目 VIEWER+
```

### 9.6 同步 Problem Jobs 为 Incidents

```http
POST /api/projects/{projectId}/tool-incidents/sync-problem-jobs
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
    "created": 3,
    "updated": 2,
    "skipped": 5
  }
}
```

### 9.7 Alert Rules

```http
POST /api/projects/{projectId}/tool-alert-rules
PUT /api/tool-alert-rules/{ruleId}
GET /api/projects/{projectId}/tool-alert-rules
```

权限：

```text
创建/更新：OWNER+
查询：VIEWER+
```

### 9.8 Alert Deliveries

```http
GET /api/tool-incidents/{incidentId}/alert-deliveries
POST /api/tool-alert-deliveries/{deliveryId}/retry
```

权限：

```text
项目 MAINTAINER+
```

---

## 10. 前端设计

### 10.1 新增 ToolIncidentPanel

新增：

```text
frontend/src/modules/admin/components/ToolIncidentPanel.vue
```

职责：

1. 展示 incident summary。
2. 展示 incident 列表。
3. 支持 status / severity 筛选。
4. 支持同步 problem jobs。
5. 支持创建 incident。
6. 支持 ACK / RESOLVE。
7. 支持查看关联 Trace / Review。
8. 支持查看 Alert Deliveries。

推荐 data-testid：

```text
tool-incident-panel
tool-incident-summary
tool-incident-list
tool-incident-row
tool-incident-sync-button
tool-incident-create-button
tool-incident-ack-button
tool-incident-resolve-button
tool-incident-status
tool-incident-severity
tool-incident-alert-deliveries
```

### 10.2 ToolIncidentDialog

新增：

```text
frontend/src/modules/admin/components/ToolIncidentDialog.vue
```

字段：

```text
Source Type
Source ID
Severity
Title
Summary
Assignee
Tool Execution ID
Tool Job ID
Operator Review ID
```

### 10.3 ToolAlertRulePanel

新增：

```text
frontend/src/modules/admin/components/ToolAlertRulePanel.vue
```

功能：

1. 展示项目 alert rules。
2. 创建 IN_APP / MOCK_SLACK / MOCK_WEBHOOK / MOCK_EMAIL 规则。
3. 启用 / 禁用规则。
4. 显示 channel / minSeverity / sourceType。

推荐 data-testid：

```text
tool-alert-rule-panel
tool-alert-rule-list
tool-alert-rule-create-button
tool-alert-rule-enabled-switch
```

### 10.4 ObservabilityPage 集成

修改：

```text
frontend/src/modules/admin/pages/ObservabilityPage.vue
```

在已有 ToolExecutionMetricsPanel 附近加入：

```text
ToolIncidentPanel
ToolAlertRulePanel
```

如果当前 ObservabilityPage 没有 projectId：

- 全局 Overview 可以只展示 Metrics。
- 项目级页面或选中项目后展示 Incident。

如果当前页面已有 project filter，则复用。

最低要求：

- 在有 projectId 的上下文展示 incident panel。
- 如果没有 projectId，显示 EmptyState：请选择项目查看工具事件。

### 10.5 Trace Drawer / Operator Review 集成

修改：

```text
ToolExecutionTraceDrawer.vue
ToolOperatorReviewDialog.vue
```

新增：

- “创建 Incident”按钮。
- 已有关联 Incident 列表。
- 点击 Incident 可打开详情或跳到 Observability。

---

## 11. 前端 API 类型

修改：

```text
frontend/src/modules/admin/api.ts
frontend/src/modules/task/api.ts
```

建议放在 admin/api.ts：

```ts
export interface ToolIncident {
  id: string
  projectId: string
  taskId?: string
  runId?: string
  toolExecutionId?: string
  toolJobId?: string
  operatorReviewId?: string
  sourceType: string
  sourceId?: string
  severity: string
  status: string
  title: string
  summary?: string
  resolution?: string
  assigneeId?: string
  firstSeenAt?: string
  lastSeenAt?: string
  acknowledgedAt?: string
  resolvedAt?: string
  createTime: string
  updateTime: string
}

export interface ToolIncidentSummary {
  openCount: number
  acknowledgedCount: number
  resolvedCount: number
  criticalCount: number
  highCount: number
  deadLetteredCount: number
  retryPendingCount: number
}

export interface ToolAlertRule {
  id: string
  projectId?: string
  name: string
  enabled: boolean
  sourceType: string
  minSeverity: string
  channel: string
  routeTarget?: string
  createTime: string
  updateTime: string
}

export interface ToolAlertDelivery {
  id: string
  incidentId: string
  projectId: string
  ruleId?: string
  channel: string
  routeTarget?: string
  status: string
  errorMessage?: string
  deliveredAt?: string
  createTime: string
}
```

新增函数：

```ts
createToolIncident(projectId: string, data: CreateToolIncidentRequest)
updateToolIncident(incidentId: string, data: UpdateToolIncidentRequest)
getToolIncident(incidentId: string)
listProjectToolIncidents(projectId: string, params?: ...)
getProjectToolIncidentSummary(projectId: string)
syncProblemJobsToIncidents(projectId: string)

createToolAlertRule(projectId: string, data: CreateToolAlertRuleRequest)
updateToolAlertRule(ruleId: string, data: UpdateToolAlertRuleRequest)
listProjectToolAlertRules(projectId: string)
listIncidentAlertDeliveries(incidentId: string)
retryToolAlertDelivery(deliveryId: string)
```

---

## 12. 后端测试要求

新增测试：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolIncidentWorkflowIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/ToolAlertRoutingIntegrationTest.java
```

至少 36 个测试。

### 12.1 Incident CRUD

1. MAINTAINER 可创建 incident。
2. VIEWER 不可创建 incident。
3. 非项目成员不可创建 incident。
4. 创建 incident 时 invalid sourceType 返回 BAD_REQUEST。
5. 创建 incident 时 invalid severity 返回 BAD_REQUEST。
6. 创建 incident 时 toolJobId 不存在返回 NOT_FOUND。
7. 创建 incident 时 toolJobId 不属于项目返回 PROJECT_ACCESS_DENIED 或 BAD_REQUEST。
8. 查询 incident 成功。
9. list project incidents 成功。
10. status filter 生效。
11. severity filter 生效。

### 12.2 状态流转

12. OPEN -> ACKNOWLEDGED 成功并设置 acknowledgedBy / acknowledgedAt。
13. ACKNOWLEDGED -> RESOLVED 成功并设置 resolvedBy / resolvedAt。
14. OPEN -> WONT_FIX 成功。
15. OPEN -> FALSE_POSITIVE 成功。
16. RESOLVED -> OPEN 清空 resolvedBy / resolvedAt。
17. invalid status 返回 BAD_REQUEST。

### 12.3 Problem Jobs 同步

18. FAILED job 同步为 MEDIUM incident。
19. RETRY_PENDING job 同步为 LOW incident。
20. DEAD_LETTERED job 同步为 HIGH incident。
21. 同一 job 重复同步不重复创建。
22. 重复同步更新 lastSeenAt。
23. 终态 incident 不被重新打开。
24. sync 返回 created / updated / skipped 计数。

### 12.4 Alert Rules / Delivery

25. OWNER 可创建 alert rule。
26. VIEWER 不可创建 alert rule。
27. list alert rules 成功。
28. update alert rule enabled=false 成功。
29. incident 创建后生成 IN_APP delivery。
30. MOCK_SLACK delivery 标记 DELIVERED。
31. MOCK_WEBHOOK delivery 标记 DELIVERED。
32. retry delivery 成功。
33. disabled rule 不生成 delivery。
34. minSeverity 低于 incident severity 时匹配成功。
35. minSeverity 高于 incident severity 时不匹配。
36. list incident deliveries 成功。

### 12.5 权限

37. 未登录查询 incident 返回 UNAUTHORIZED。
38. 非项目成员查询 incident 返回 PROJECT_ACCESS_DENIED。
39. VIEWER 可查询 incident summary。
40. VIEWER 不可 sync problem jobs。

可以超过 36 个。

---

## 13. 前端 E2E 要求

新增或修改：

```text
frontend/e2e/knowledge-observability.spec.ts
frontend/e2e/multi-agent-orchestration.spec.ts
```

也可新增：

```text
frontend/e2e/tool-incident-alert.spec.ts
```

至少 8 个 E2E：

1. Observability 中显示 Tool Incident Panel。
2. 点击 sync problem jobs 后显示结果。
3. 创建 manual incident 成功。
4. ACK incident 成功。
5. RESOLVE incident 成功。
6. Alert Rule panel 可创建 mock rule。
7. Incident 显示 alert delivery。
8. Trace Drawer 中可创建 incident。
9. 页面无 JS error。

推荐 data-testid：

```text
tool-incident-panel
tool-incident-sync-button
tool-incident-create-button
tool-incident-dialog
tool-incident-submit-button
tool-incident-ack-button
tool-incident-resolve-button
tool-alert-rule-panel
tool-alert-rule-create-button
tool-incident-alert-deliveries
```

---

## 14. 文档与报告

完成后新增：

```text
docs/milestone-37h-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. tool_incident / tool_alert_rule / tool_alert_delivery 表说明
3. Incident Service 设计说明
4. Alert Rule / Delivery Service 设计说明
5. Problem Jobs 同步规则
6. API 清单
7. 前端 ToolIncidentPanel / ToolAlertRulePanel 说明
8. Trace Drawer / Operator Review 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37I

---

## 15. 验收标准

完成后必须满足：

1. Incident 可创建、查询、更新。
2. Incident 支持 ACK / RESOLVE / WONT_FIX / FALSE_POSITIVE。
3. Problem Jobs 可同步为 Incidents。
4. 同步逻辑幂等。
5. Alert Rule 可创建、更新、查询。
6. Incident 创建后可产生 Mock Alert Delivery。
7. Delivery 不发送真实外部请求。
8. Observability 可展示 Incident 面板。
9. Trace Drawer 可创建 Incident。
10. 权限校验正确。
11. 后端测试通过。
12. 前端 typecheck / build / E2E 通过。

---

## 16. 非目标

本阶段不做：

1. 不做真实 Slack / Email / PagerDuty 集成。
2. 不做 webhook 真实 HTTP 请求。
3. 不做复杂 On-call 排班。
4. 不做 SLA 计时器。
5. 不做 Incident 评论线程。
6. 不做 Incident 附件上传。
7. 不做 PDF 导出。
8. 不做 Prometheus Alertmanager 集成。
9. 不做 OpenTelemetry。
10. 不做自动 retry / cancel / approve。

这些可以放到后续 Milestone。

---

## 17. 建议后续 Milestone

完成 37H 后，建议进入：

```text
Milestone 37I: Incident SLA / Notification Rules / Escalation Mock
```

候选能力：

- SLA dueAt / breachedAt。
- Escalation policy。
- Mock notification schedule。
- Review / Incident 双向关联。
- Incident timeline。

也可以进入：

```text
Milestone 38A: Semantic Code Search / RAG Evaluation
```

---

## 18. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37H。

文档路径：
docs/milestone-37h-tool-execution-incident-alert-routing.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37D Metrics、37F Trace Evidence、37G Audit Export / Operator Review 基础上，新增 Tool Execution Incident Workflow & Alert Routing。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 不发送真实 Slack / PagerDuty / Email。
9. Alert Delivery 只写数据库 Mock 记录，不发送真实外部请求。
10. Incident 不自动 retry / cancel / approve 工具。
11. Incident 不改变 Tool Execution / Job 原始结果。
12. 不要破坏 36A-37G 已有 API。
13. 不要破坏 Tool Operator Review API。
14. 遵循现有项目规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
15. IDs 对外保持 String。
16. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V34__init_tool_incident_alert_tables.sql。
2. 新增 ToolIncidentStatus / ToolIncidentSeverity / ToolIncidentSourceType / ToolAlertChannel / ToolAlertDeliveryStatus 枚举。
3. 新增 ToolIncidentEntity / ToolAlertRuleEntity / ToolAlertDeliveryEntity。
4. 新增对应 Mapper 和 DTO。
5. 新增 ToolIncidentService，支持 Incident 创建、更新、查询、列表、summary、sync problem jobs。
6. 新增 ToolAlertRuleService，支持规则创建、更新、查询。
7. 新增 ToolAlertDeliveryService，支持 Mock delivery 记录、列表、retry。
8. 新增 API：
   - POST /api/projects/{projectId}/tool-incidents
   - PUT /api/tool-incidents/{incidentId}
   - GET /api/tool-incidents/{incidentId}
   - GET /api/projects/{projectId}/tool-incidents
   - GET /api/projects/{projectId}/tool-incidents/summary
   - POST /api/projects/{projectId}/tool-incidents/sync-problem-jobs
   - POST /api/projects/{projectId}/tool-alert-rules
   - PUT /api/tool-alert-rules/{ruleId}
   - GET /api/projects/{projectId}/tool-alert-rules
   - GET /api/tool-incidents/{incidentId}/alert-deliveries
   - POST /api/tool-alert-deliveries/{deliveryId}/retry
9. 前端新增 ToolIncidentPanel.vue。
10. 前端新增 ToolIncidentDialog.vue。
11. 前端新增 ToolAlertRulePanel.vue。
12. ObservabilityPage 集成 Incident / Alert Rule 面板。
13. Trace Drawer / Operator Review 中增加创建 Incident 入口。
14. 后端测试不少于 36 个。
15. 前端 E2E 不少于 8 个。
16. 新增 docs/milestone-37h-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. tool_incident / tool_alert_rule / tool_alert_delivery 表说明
3. Incident Service 设计说明
4. Alert Rule / Delivery Service 设计说明
5. Problem Jobs 同步规则
6. API 清单
7. 前端 ToolIncidentPanel / ToolAlertRulePanel 说明
8. Trace Drawer / Operator Review 集成说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 37I

现在开始实现，不要只给计划。
```
