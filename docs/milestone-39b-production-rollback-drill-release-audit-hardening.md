# Milestone 39B: Production Rollback Drill & Release Audit Hardening

## 1. 背景

Milestone 39A 已经完成 Production Readiness 与 Controlled Rollout：

```text
38C
  Beta Release Gate / Go-No-Go Decision

39A
  Release Rollout Plan
  -> Rollout Step
  -> Verification Record
  -> Observation Window
```

现在系统已经能回答：

```text
这次版本能不能上线？
上线分几步做？
观察窗口内看哪些信号？
哪些条件触发回滚建议？
```

但进入真正的 Production 管理层面，还缺少两块很关键的能力：

```text
1. 回滚演练（rollback drill）是否准备充分？
2. 发布全过程有没有形成可审计、可追责、可复盘的 release audit trail？
```

当前平台虽然已有：

```text
Incident / Alert / RCA / Retrospective / Release Rollout
```

但仍然缺失：

```text
回滚预案模板化
回滚演练记录
发布审计证据导出
事后发布复盘与生产事件回链
```

Milestone 39B 的目标就是新增：

```text
Production Rollback Drill & Release Audit Hardening
```

让平台从：

```text
能管理 rollout
```

升级为：

```text
能管理 rollback drill、release audit、post-release review 与生产事件追溯
```

---

## 2. 总目标

实现 Production Rollback 与 Release Audit 加固能力：

1. 新增 Release Rollback Drill 数据模型。
2. 新增 Release Audit Event 数据模型。
3. 新增 Release Postmortem Review 数据模型。
4. 支持为 rollout plan 绑定 rollback drill。
5. 支持记录 drill 的准备项、执行项、耗时、结果、阻塞点。
6. 支持记录 release audit trail（状态推进、步骤更新、验证结果、人工决策）。
7. 支持记录 post-release review（上线后总结、问题、改进项）。
8. 支持关联 production incident / alert / verification 记录。
9. 支持导出 Release Audit Report Markdown。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有 rollout plan，但 rollback 与 release 审计仍依赖人工文档
```

升级为：

```text
有结构化 rollback drill、发布审计链路和上线后复盘记录
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动执行真实 rollback。
4. 不自动操作 Docker / Kubernetes / 云服务。
5. 不自动回滚数据库、镜像、配置、环境变量。
6. 不自动关闭 incident / alert / feedback。
7. 不自动把 rollback recommended 变成 rolled back。
8. 不调用真实 AI 自动生成发布结论。
9. audit event 只记录平台内发生的 release / rollout / verification / decision / review 行为。
10. rollback drill 只记录计划、步骤、结果、证据，不执行基础设施动作。
11. 不破坏 1-39A 已有 API 与页面。
12. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 rollback drill / release audit / post-release review 表。
2. 绑定 39A rollout plan、38C decision、37H-37K incident。
3. 新增 timeline、report、dialog、drawer、export 能力。
4. 新增 post-release review 和 action item 记录。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V42__init_release_rollback_audit_tables.sql
```

### 4.1 release_rollback_drill

```sql
CREATE TABLE release_rollback_drill (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    drill_status VARCHAR(32) NOT NULL,
    drill_scope VARCHAR(64) NOT NULL,
    environment_name VARCHAR(64) NOT NULL,
    owner_id BIGINT NULL,
    executor_id BIGINT NULL,
    planned_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_seconds BIGINT NULL,
    success_criteria TEXT NULL,
    rollback_steps_summary TEXT NULL,
    blockers_summary TEXT NULL,
    result_summary TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_rollback_drill_plan(plan_id),
    KEY idx_release_rollback_drill_project(project_id, create_time),
    KEY idx_release_rollback_drill_status(drill_status)
);
```

### 4.2 release_audit_event

```sql
CREATE TABLE release_audit_event (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(128) NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    related_step_id BIGINT NULL,
    related_verification_id BIGINT NULL,
    related_incident_id BIGINT NULL,
    related_alert_id BIGINT NULL,
    evidence_json JSON NULL,
    event_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_audit_event_plan(plan_id, event_time),
    KEY idx_release_audit_event_project(project_id, event_time),
    KEY idx_release_audit_event_type(event_type)
);
```

### 4.3 release_postmortem_review

```sql
CREATE TABLE release_postmortem_review (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    overall_outcome VARCHAR(32) NOT NULL,
    summary TEXT NULL,
    what_went_well TEXT NULL,
    what_went_wrong TEXT NULL,
    customer_impact TEXT NULL,
    follow_up_actions TEXT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_postmortem_plan(plan_id),
    KEY idx_release_postmortem_project(project_id, create_time),
    KEY idx_release_postmortem_status(review_status)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
ReleaseRollbackDrillStatus.java
ReleaseRollbackDrillScope.java
ReleaseAuditEventType.java
ReleasePostmortemReviewStatus.java
ReleasePostmortemOutcome.java
```

### 5.1 ReleaseRollbackDrillStatus

```text
PLANNED
RUNNING
PASSED
FAILED
BLOCKED
CANCELLED
```

### 5.2 ReleaseRollbackDrillScope

```text
CONFIG_ONLY
APP_VERSION
DB_AND_APP
FULL_ENVIRONMENT
```

### 5.3 ReleaseAuditEventType

```text
PLAN_CREATED
PLAN_STATUS_CHANGED
STEP_STATUS_CHANGED
VERIFICATION_RECORDED
ROLLBACK_DRILL_UPDATED
DECISION_LINKED
INCIDENT_LINKED
POSTMORTEM_UPDATED
REPORT_EXPORTED
```

### 5.4 ReleasePostmortemReviewStatus

```text
DRAFT
REVIEWED
PUBLISHED
ARCHIVED
```

### 5.5 ReleasePostmortemOutcome

```text
SUCCESS
SUCCESS_WITH_ISSUES
ROLLBACK_NEEDED
FAILED_RELEASE
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ReleaseRollbackDrillEntity.java
ReleaseAuditEventEntity.java
ReleasePostmortemReviewEntity.java

ReleaseRollbackDrillMapper.java
ReleaseAuditEventMapper.java
ReleasePostmortemReviewMapper.java
```

DTO 建议：

```text
CreateReleaseRollbackDrillRequest.java
UpdateReleaseRollbackDrillRequest.java
ReleaseRollbackDrillResponse.java

CreateReleasePostmortemReviewRequest.java
UpdateReleasePostmortemReviewRequest.java
ReleasePostmortemReviewResponse.java

ReleaseAuditEventResponse.java
ReleaseAuditTimelineResponse.java
ReleaseAuditReportResponse.java
```

### 6.1 ReleaseRollbackDrillResponse

建议字段：

```text
id
planId
projectId
releaseLabel
drillStatus
drillScope
environmentName
ownerId
executorId
plannedAt
startedAt
finishedAt
durationSeconds
successCriteria
rollbackStepsSummary
blockersSummary
resultSummary
evidenceJson
createTime
updateTime
```

### 6.2 ReleaseAuditTimelineResponse

建议字段：

```text
planId
releaseLabel
totalEvents
latestEventTime
eventCountsByType
events
```

---

## 7. 服务设计

新增应用服务：

```text
ReleaseRollbackDrillService.java
ReleaseAuditTrailService.java
ReleasePostmortemReviewService.java
```

### 7.1 ReleaseRollbackDrillService

职责：

1. 创建与更新 rollback drill。
2. 校验 drill 只能绑定到已有 rollout plan。
3. 支持状态流转：

```text
PLANNED -> RUNNING -> PASSED
                  \-> FAILED
                  \-> BLOCKED
PLANNED -> CANCELLED
```

4. 计算耗时。
5. 输出是否具备 rollback readiness。
6. 将关键动作写入 audit event。

### 7.2 ReleaseAuditTrailService

职责：

1. 统一记录 release audit event。
2. 捕获来源：
   - rollout plan status change
   - step status change
   - verification record create/update
   - rollback drill create/update/status
   - postmortem review create/update/publish
   - report export
3. 按时间线聚合 audit trail。
4. 导出 Release Audit Report Markdown。

### 7.3 ReleasePostmortemReviewService

职责：

1. 创建和更新 postmortem review。
2. 聚合 production incident / alert / verification / rollout signals。
3. 支持 review 状态流转：

```text
DRAFT -> REVIEWED -> PUBLISHED -> ARCHIVED
          \-> DRAFT
```

4. 形成结构化总结：
   - what went well
   - what went wrong
   - customer impact
   - follow-up actions
5. 将 review 行为写入 audit event。

---

## 8. API 设计

新增 Controller：

```text
ReleaseAuditController.java
```

建议端点：

### 8.1 Rollback Drill

```text
POST   /api/release-rollouts/{planId}/rollback-drills
GET    /api/release-rollouts/{planId}/rollback-drills
GET    /api/release-rollouts/{planId}/rollback-drills/{drillId}
PUT    /api/release-rollouts/{planId}/rollback-drills/{drillId}
POST   /api/release-rollouts/{planId}/rollback-drills/{drillId}/status
```

### 8.2 Audit Trail

```text
GET    /api/release-rollouts/{planId}/audit-events
GET    /api/release-rollouts/{planId}/audit-timeline
GET    /api/release-rollouts/{planId}/audit-report
```

### 8.3 Postmortem Review

```text
POST   /api/release-rollouts/{planId}/postmortem-review
GET    /api/release-rollouts/{planId}/postmortem-review
PUT    /api/release-rollouts/{planId}/postmortem-review/{reviewId}
POST   /api/release-rollouts/{planId}/postmortem-review/{reviewId}/status
```

权限建议：

```text
查看：ADMIN / OWNER / MAINTAINER
编辑：ADMIN / OWNER
发布 / 归档：ADMIN / OWNER
```

---

## 9. 聚合规则建议

### 9.1 Rollback Readiness

若存在以下任一情况，则 `rollbackReady=false`：

1. 没有 rollback drill
2. 最新 rollback drill 状态不是 `PASSED`
3. rollback step summary 为空
4. blockers summary 非空且未清除

### 9.2 Post-release Review 补全建议

若 rollout plan 已 `COMPLETED` 或 `ROLLED_BACK`，系统应允许自动预填 review 草稿，来源包括：

1. 39A verification records
2. 37H-37K incidents / RCA / retrospective
3. 38A beta blocker feedback
4. 38B model cost alert / PR review quality issue

缺失字段可统一填：

```text
待补充。
```

### 9.3 Release Audit Report 建议内容

```text
Release Label
Go / No-Go Decision
Rollout Timeline
Verification Summary
Rollback Drill Result
Observation Window Signals
Incidents / Alerts Linked
Post-release Review
Final Outcome
```

---

## 10. 前端设计

新增组件建议：

```text
ReleaseRollbackDrillPanel.vue
ReleaseAuditTimelinePanel.vue
ReleasePostmortemReviewPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 ReleaseRollbackDrillPanel

展示：

1. drill 列表
2. 当前 rollback readiness badge
3. create / edit dialog
4. status 按钮
5. duration / blockers / result summary
6. evidence tags

### 10.2 ReleaseAuditTimelinePanel

展示：

1. release audit event timeline
2. event type 统计
3. step / verification / incident / review 回链
4. export report 按钮

### 10.3 ReleasePostmortemReviewPanel

展示：

1. review 状态
2. overall outcome
3. what went well / wrong
4. customer impact
5. follow-up actions
6. status 按钮（reviewed / published / archived）

UI 要求：

1. 保持中文暗色科技风
2. 复用 `StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 控制台风格为主：时间线、表格、抽屉、状态标签、表单
4. 不做营销风页面

---

## 11. 后端测试要求

新增：

```text
ReleaseAuditRollbackIntegrationTest.java
```

不少于 26 个集成测试，建议覆盖：

1. create rollback drill success
2. update rollback drill success
3. rollback drill status planned -> running
4. rollback drill status running -> passed
5. rollback drill status running -> failed
6. invalid rollback drill transition reject
7. rollback readiness false when no drill
8. rollback readiness true when latest drill passed
9. audit event created on rollout status change
10. audit event created on step status change
11. audit event created on verification record create
12. audit event created on rollback drill update
13. list audit events ordered by eventTime desc
14. audit timeline aggregates counts
15. create postmortem review success
16. update postmortem review success
17. postmortem status draft -> reviewed
18. postmortem status reviewed -> published
19. reviewed -> draft allowed
20. archived blocks update
21. postmortem auto-prefill from rollout / incident signals
22. audit report markdown export success
23. unauthorized access reject
24. non-owner update reject
25. project scoped listing works
26. incident / alert linkage appears in audit report

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/release-audit.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 rollback drill panel
2. create rollback drill dialog works
3. rollback drill list renders
4. status update works
5. audit timeline renders
6. postmortem review create/edit works
7. export report button visible
8. no JS errors on page load

如果测试环境没有 seeded release rollout：

1. 显式断言空态
2. 不把“无 rollout 数据”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-39b-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. rollback drill / audit event / postmortem review 表说明
3. ReleaseRollbackDrillService 设计说明
4. ReleaseAuditTrailService 设计说明
5. ReleaseRollbackDrillPanel 说明
6. ReleaseAuditTimelinePanel 说明
7. ReleasePostmortemReviewPanel 说明
8. Rollback / Audit 边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 39C

---

## 14. 验收标准

必须全部满足：

1. rollback drill / audit event / postmortem review 三张表已落库
2. rollback drill 状态流转完整
3. release audit event 时间线可查询
4. postmortem review 可创建 / 更新 / 状态流转
5. audit report markdown 可导出
6. rollout / verification / incident / alert 可在 audit trail 中回链
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无 rollout 前置条件显式降级处理

---

## 15. 完成后的价值

完成 39B 后，平台将从：

```text
能管理 rollout
```

升级为：

```text
能管理 rollback drill、release audit 和 post-release review
```

这会让平台更接近真正可运营、可审计、可复盘的 production release 管理系统。

---

## 16. 后续建议

Milestone 39B 完成后，建议进入：

```text
Milestone 39C: Production Rollout Evidence Center & Executive Release Summary
```

重点可包括：

1. executive summary card
2. release evidence bundle export
3. stakeholder sign-off summary
4. cross-release comparison
5. release confidence trend

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 39B。

文档路径：
docs/milestone-39b-production-rollback-drill-release-audit-hardening.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 39A Controlled Rollout、38C Beta Release Gate、38A/38B 质量治理、37H-37K Incident / RCA / Retrospective 基础上，新增 Production Rollback Drill 与 Release Audit Hardening。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动执行真实 rollback。
6. 不要自动操作 Docker / Kubernetes / 云服务。
7. 不要自动修改环境变量、数据库、域名、代理配置。
8. 不要自动关闭 incident / alert / feedback。
9. rollback drill 只记录计划、步骤、结果、证据，不执行基础设施动作。
10. audit event 只记录 release / rollout / verification / review 行为。
11. 不要破坏 1-39A 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V42__init_release_rollback_audit_tables.sql。
2. 新增 release_rollback_drill / release_audit_event / release_postmortem_review 三张表。
3. 新增 5 个枚举：
   - ReleaseRollbackDrillStatus
   - ReleaseRollbackDrillScope
   - ReleaseAuditEventType
   - ReleasePostmortemReviewStatus
   - ReleasePostmortemOutcome
4. 新增实体、Mapper、DTO。
5. 新增 ReleaseRollbackDrillService。
6. 新增 ReleaseAuditTrailService。
7. 新增 ReleasePostmortemReviewService。
8. 新增 API：
   - rollback drill CRUD / status
   - release audit events / timeline / report
   - postmortem review create / get / update / status
9. 前端新增：
   - ReleaseRollbackDrillPanel.vue
   - ReleaseAuditTimelinePanel.vue
   - ReleasePostmortemReviewPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 26 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-39b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 release audit / rollback 表说明
3. ReleaseRollbackDrillService 设计说明
4. ReleaseAuditTrailService 设计说明
5. ReleaseRollbackDrillPanel 说明
6. ReleaseAuditTimelinePanel 说明
7. ReleasePostmortemReviewPanel 说明
8. Rollback / Audit 边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 39C

现在开始实现，不要只给计划。
```
