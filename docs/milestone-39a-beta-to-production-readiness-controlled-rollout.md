# Milestone 39A: Beta-to-Production Readiness & Controlled Rollout

## 1. 背景

截至 Milestone 38C，平台已经具备完整的 Beta 质量与放行判断能力：

```text
37H-37K
  Incident / RCA / Retrospective / Knowledge Quality

38A
  Beta Trial Session / Feedback / Readiness

38B
  Real Model Cost / PR Review Quality

38C
  Beta Release Gate / Go-No-Go Decision Center
```

现在系统已经能回答：

```text
这次 Beta 能不能放？
有哪些阻塞项？
为什么是 Go / Conditional Go / No-Go？
```

但从产品试用阶段走向更正式的“生产前受控发布”，还缺一层关键能力：

```text
谁来执行 rollout？
分几步放量？
每一步要验证什么？
如果出问题，怎么回滚？
观察窗口内需要盯哪些信号？
最终如何形成一份 release readiness / rollout report？
```

Milestone 39A 的目标就是新增：

```text
Beta-to-Production Readiness & Controlled Rollout
```

让平台从：

```text
能做 Beta 决策
```

升级为：

```text
能管理受控发布计划、步骤验证、观察窗口、回滚判断与发布审计
```

---

## 2. 总目标

实现 Production Readiness 与 Controlled Rollout 管理中心：

1. 新增 Release Rollout Plan 数据模型。
2. 新增 Release Rollout Step 数据模型。
3. 新增 Release Verification Record 数据模型。
4. 支持引用 38C 的 Go / No-Go 决策作为前置条件。
5. 支持为每个 release label 制定 rollout / rollback / observation plan。
6. 支持记录每个 rollout step 的状态、结果、证据与阻塞说明。
7. 支持记录发布前 / 发布中 / 发布后的验证项结果。
8. 支持标记 observation window 内是否出现 incident / alert / cost spike / PR quality regression。
9. 支持生成 Markdown 版 Release Readiness Report 与 Rollout Summary。
10. 补齐后端测试与前端 E2E。

完成后，从：

```text
有 Go / No-Go 判断，但发布过程仍靠线下约定
```

升级为：

```text
有结构化 rollout plan、验证记录、观察窗口与回滚依据
```

---

## 3. 严格边界

必须遵守：

1. 不执行真实 shell 命令作为业务能力。
2. 不执行真实 Git 写操作。
3. 不自动发布到生产环境。
4. 不自动操作 Docker / Kubernetes / 云服务。
5. 不自动修改环境变量、密钥、域名、代理配置。
6. 不自动关闭 incident / alert / feedback。
7. 不自动把 Go 决策转为已上线状态。
8. 不调用真实 AI 自动生成发布结论。
9. Rollout 只记录“计划、步骤、验证、结果、证据”，不执行基础设施操作。
10. 不破坏 1-38C 已有 API 与页面。
11. 前端保持中文暗色科技风 UI，复用现有组件。

允许做：

1. 新增 rollout plan / step / verification record 表。
2. 聚合 38C 决策、37H-37K 事故信号、38A 反馈、38B 成本与质量信号。
3. 新增 dashboard、列表、详情抽屉、时间线、导出 Markdown。
4. 新增受控发布状态流转与人工确认。

---

## 4. 数据库设计

新增 migration：

```text
backend/src/main/resources/db/migration/V41__init_release_rollout_tables.sql
```

### 4.1 release_rollout_plan

```sql
CREATE TABLE release_rollout_plan (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    source_decision_id BIGINT NULL,
    rollout_status VARCHAR(32) NOT NULL,
    rollout_strategy VARCHAR(32) NOT NULL,
    target_environment VARCHAR(64) NOT NULL,
    owner_id BIGINT NULL,
    approver_id BIGINT NULL,
    planned_start_at DATETIME NULL,
    planned_end_at DATETIME NULL,
    observation_window_minutes INT NOT NULL DEFAULT 60,
    rollback_trigger_summary TEXT NULL,
    success_criteria_summary TEXT NULL,
    readiness_summary TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_rollout_plan_project(project_id, create_time),
    KEY idx_release_rollout_plan_status(rollout_status),
    UNIQUE KEY uk_release_rollout_label(project_id, release_label)
);
```

### 4.2 release_rollout_step

```sql
CREATE TABLE release_rollout_step (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    step_order INT NOT NULL,
    step_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    step_status VARCHAR(32) NOT NULL,
    verification_scope VARCHAR(32) NOT NULL,
    required TINYINT NOT NULL DEFAULT 1,
    blocking TINYINT NOT NULL DEFAULT 1,
    instructions TEXT NULL,
    expected_result TEXT NULL,
    actual_result TEXT NULL,
    evidence_json JSON NULL,
    operator_id BIGINT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_rollout_step_plan(plan_id, step_order),
    KEY idx_release_rollout_step_status(step_status),
    UNIQUE KEY uk_release_rollout_step(plan_id, step_key)
);
```

### 4.3 release_verification_record

```sql
CREATE TABLE release_verification_record (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    verification_phase VARCHAR(32) NOT NULL,
    verification_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    verification_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    evidence_json JSON NULL,
    related_incident_id BIGINT NULL,
    related_alert_id BIGINT NULL,
    recorded_by BIGINT NULL,
    recorded_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_verification_plan(plan_id, verification_phase, recorded_at),
    KEY idx_release_verification_status(verification_status, severity),
    KEY idx_release_verification_project(project_id, recorded_at)
);
```

无物理外键，保持当前项目风格一致。

---

## 5. 枚举设计

新增：

```text
ReleaseRolloutStatus.java
ReleaseRolloutStrategy.java
ReleaseRolloutStepStatus.java
ReleaseVerificationPhase.java
ReleaseVerificationStatus.java
ReleaseVerificationSeverity.java
```

### 5.1 ReleaseRolloutStatus

```text
DRAFT
READY
IN_PROGRESS
OBSERVING
COMPLETED
ROLLED_BACK
CANCELLED
```

### 5.2 ReleaseRolloutStrategy

```text
MANUAL_FULL
PHASED_PERCENTAGE
INTERNAL_ONLY
PROJECT_WHITELIST
```

### 5.3 ReleaseRolloutStepStatus

```text
PENDING
RUNNING
PASSED
FAILED
SKIPPED
BLOCKED
```

### 5.4 ReleaseVerificationPhase

```text
PRE_RELEASE
ROLLOUT
OBSERVATION
POST_RELEASE
```

### 5.5 ReleaseVerificationStatus

```text
PASS
WARN
FAIL
SKIP
```

### 5.6 ReleaseVerificationSeverity

```text
INFO
LOW
MEDIUM
HIGH
CRITICAL
```

---

## 6. 实体 / Mapper / DTO

新增：

```text
ReleaseRolloutPlanEntity.java
ReleaseRolloutStepEntity.java
ReleaseVerificationRecordEntity.java

ReleaseRolloutPlanMapper.java
ReleaseRolloutStepMapper.java
ReleaseVerificationRecordMapper.java
```

DTO 建议：

```text
CreateReleaseRolloutPlanRequest.java
UpdateReleaseRolloutPlanRequest.java
ReleaseRolloutPlanResponse.java

CreateReleaseRolloutStepRequest.java
UpdateReleaseRolloutStepRequest.java
ReleaseRolloutStepResponse.java

CreateReleaseVerificationRecordRequest.java
UpdateReleaseVerificationRecordRequest.java
ReleaseVerificationRecordResponse.java

ReleaseReadinessDashboardResponse.java
ReleaseRolloutSummaryResponse.java
ReleaseReadinessReportResponse.java
```

### 6.1 ReleaseRolloutPlanResponse

建议字段：

```text
id
projectId
releaseLabel
sourceDecisionId
rolloutStatus
rolloutStrategy
targetEnvironment
ownerId
approverId
plannedStartAt
plannedEndAt
observationWindowMinutes
rollbackTriggerSummary
successCriteriaSummary
readinessSummary
stepCount
passedStepCount
failedStepCount
verificationCount
blockingVerificationCount
createTime
updateTime
```

### 6.2 ReleaseReadinessDashboardResponse

建议字段：

```text
projectId
releaseLabel
decisionStatus
rolloutStatus
overallReadinessStatus
blockingIssueCount
warningIssueCount
openIncidentCount
activeAlertCount
highRiskFeedbackCount
costAlertCount
prQualityWarnCount
preReleasePassRate
observationVerificationCount
rollbackRecommended
lastEvaluatedAt
```

---

## 7. 服务设计

新增应用服务：

```text
ReleaseRolloutPlanService.java
ReleaseVerificationService.java
ReleaseReadinessReportService.java
```

### 7.1 ReleaseRolloutPlanService

职责：

1. 创建 rollout plan。
2. 校验 release label 与 38C decision 的一致性。
3. 初始化默认 rollout steps。
4. 支持 plan 状态流转：

```text
DRAFT -> READY -> IN_PROGRESS -> OBSERVING -> COMPLETED
                        \-> ROLLED_BACK
                        \-> CANCELLED
```

5. 支持 step 状态更新。
6. 校验 blocking step 未通过时不得进入下一阶段。

默认建议 steps：

```text
1. PRECHECK_DECISION
2. VERIFY_ENVIRONMENT
3. START_ROLLOUT
4. VERIFY_CORE_FLOW
5. OBSERVE_SIGNALS
6. FINAL_CONFIRMATION
```

### 7.2 ReleaseVerificationService

职责：

1. 管理 verification records。
2. 支持按 phase 记录验证项。
3. 聚合已有信号，生成建议 verification：
   - 38C Gate BLOCK / WARN
   - 38B Cost Alert / PR Quality
   - 37H-37K Incident / RCA / Retrospective / Knowledge Quality
   - 38A High-risk feedback / readiness issues
4. 支持标记 rollback recommended。
5. 支持输出 blocking verification 列表。

### 7.3 ReleaseReadinessReportService

职责：

1. 生成 readiness dashboard。
2. 生成 rollout summary。
3. 导出 Markdown：

```text
Release Label
Go/No-Go Decision
Rollout Strategy
Pre-release Checks
Rollout Steps
Observation Window
Warnings / Blocking Issues
Rollback Triggers
Final Outcome
```

---

## 8. API 设计

新增 Controller：

```text
ReleaseRolloutController.java
```

建议端点：

### 8.1 Plan

```text
POST   /api/release-rollouts
GET    /api/release-rollouts
GET    /api/release-rollouts/{planId}
PUT    /api/release-rollouts/{planId}
POST   /api/release-rollouts/{planId}/status
```

### 8.2 Step

```text
GET    /api/release-rollouts/{planId}/steps
POST   /api/release-rollouts/{planId}/steps
PUT    /api/release-rollouts/{planId}/steps/{stepId}
POST   /api/release-rollouts/{planId}/steps/{stepId}/status
```

### 8.3 Verification

```text
GET    /api/release-rollouts/{planId}/verifications
POST   /api/release-rollouts/{planId}/verifications
PUT    /api/release-rollouts/{planId}/verifications/{recordId}
```

### 8.4 Dashboard / Report

```text
GET    /api/release-readiness/dashboard
GET    /api/release-rollouts/{planId}/summary
GET    /api/release-rollouts/{planId}/report
```

权限建议：

```text
查看：ADMIN / OWNER / MAINTAINER
编辑：ADMIN / OWNER
状态推进：ADMIN / OWNER
```

---

## 9. 聚合规则建议

### 9.1 Rollout 前置条件

若存在以下任一情况，plan 不得从 `DRAFT/READY` 进入 `IN_PROGRESS`：

1. 38C 最新 decision 为 `NO_GO`
2. 存在 blocking gate evaluation = `BLOCK`
3. 存在未解决 CRITICAL incident
4. 存在未确认 HIGH/CRITICAL cost alert
5. 存在 high-risk feedback 且未处理

### 9.2 Observation Window 风险信号

若 observation window 内出现以下任一情况，可建议 `rollbackRecommended=true`：

1. 新增 CRITICAL incident
2. 新增 HIGH/CRITICAL alert 且未 ack
3. 成本异常告警新增
4. PR Review 质量显著下降
5. 新增 Beta blocker feedback

### 9.3 Overall Readiness Status

建议聚合：

```text
所有 blocking verification PASS -> READY
存在 WARN 但无 FAIL -> READY_WITH_RISK
存在 FAIL 或 rollbackRecommended -> NOT_READY
```

---

## 10. 前端设计

新增组件建议：

```text
ReleaseReadinessDashboardPanel.vue
ReleaseRolloutPlanPanel.vue
ReleaseVerificationPanel.vue
```

集成位置：

```text
ObservabilityPage.vue
```

### 10.1 ReleaseReadinessDashboardPanel

展示：

1. 当前 release label
2. 38C decision status
3. rollout status
4. blocking / warning 计数
5. open incidents / active alerts / cost alerts / high-risk feedback
6. overall readiness badge
7. rollback recommended badge

### 10.2 ReleaseRolloutPlanPanel

展示：

1. rollout plan 列表
2. create / edit dialog
3. step timeline
4. step 状态推进按钮
5. observation window 时长
6. rollback trigger / success criteria / readiness summary

### 10.3 ReleaseVerificationPanel

展示：

1. 按 phase 分组的 verification 列表
2. PASS / WARN / FAIL tag
3. severity tag
4. 证据摘要
5. create / edit dialog
6. report export 按钮

UI 要求：

1. 保持中文暗色科技风
2. 复用 `StatusPulse`、`GlowButton`、`NeonDivider`、`EmptyState`、`ErrorState`
3. 不做营销风页面
4. 以运维控制台风格为主：表格、时间线、抽屉、对话框、状态标签

---

## 11. 后端测试要求

新增：

```text
ReleaseRolloutIntegrationTest.java
```

不少于 24 个集成测试，建议覆盖：

1. create rollout plan success
2. duplicate release label reject
3. link 38C decision success
4. NO_GO decision blocks rollout start
5. blocking evaluation blocks rollout start
6. unresolved critical incident blocks rollout start
7. create default steps success
8. list steps ordered by stepOrder
9. update step status passed
10. update step status failed
11. failed blocking step prevents next phase transition
12. create verification record success
13. verification grouped by phase
14. readiness dashboard aggregates counts
15. rollback recommended when critical signal exists
16. report export returns markdown
17. unauthorized access reject
18. non-owner update reject
19. project scoped listing works
20. observation phase data included in summary
21. rollout completed after all required steps pass
22. cancelled rollout cannot move to completed
23. rolled back status recorded correctly
24. empty project returns empty dashboard

---

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/release-rollout.spec.ts
```

不少于 8 个用例，建议覆盖：

1. observability 页面显示 release readiness dashboard
2. create rollout plan dialog works
3. rollout plan list renders
4. step timeline renders
5. update step status works
6. verification record create works
7. report export button visible
8. no JS errors on page load

如果测试环境没有 seeded project：

1. 显式断言空态
2. 不把“无项目”误判为功能失败

---

## 13. 完成报告要求

完成后新增：

```text
docs/milestone-39a-completion-report.md
```

报告至少包含：

1. 新增 / 修改文件清单
2. release_rollout_plan / step / verification_record 表说明
3. ReleaseRolloutPlanService 设计说明
4. ReleaseVerificationService 设计说明
5. Release Readiness Dashboard 说明
6. Release Rollout Plan Panel 说明
7. Verification Panel 说明
8. Rollout / Rollback 边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 39B

---

## 14. 验收标准

必须全部满足：

1. rollout plan / step / verification record 三张表已落库
2. rollout plan 状态流转完整
3. step timeline 可视化
4. verification 记录可创建 / 编辑 / 列表
5. readiness dashboard 可聚合 38C / 38B / 38A / 37H-37K 信号
6. Markdown report 可导出
7. 后端集成测试通过
8. 前端 `npm run typecheck` 通过
9. 前端 `npm run build` 通过
10. 前端 E2E 通过或对无项目前置条件显式降级处理

---

## 15. 完成后的价值

完成 39A 后，平台将从：

```text
能做 Beta 放行判断
```

升级为：

```text
能管理受控 rollout、验证记录、观察窗口与回滚依据
```

这意味着 v1.1 的交付形态会更接近真实 production readiness，而不是单纯“功能已开发完毕”。

---

## 16. 后续建议

Milestone 39A 完成后，建议进入：

```text
Milestone 39B: Production Rollback Drill & Release Audit Hardening
```

重点可包括：

1. rollback drill 模板
2. release audit trail
3. post-release review checklist
4. production incident back-reference
5. release evidence export

---

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 39A。

文档路径：
docs/milestone-39a-beta-to-production-readiness-controlled-rollout.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 37H-37K Incident / RCA / Retrospective / Knowledge Quality、38A Beta Feedback、38B Real Model Cost & PR Review Quality、38C Beta Release Gate 基础上，新增 Production Readiness 与 Controlled Rollout 管理能力。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要自动发布到生产环境。
6. 不要自动操作 Docker / Kubernetes / 云服务。
7. 不要自动修改环境变量、密钥、域名、代理配置。
8. 不要自动关闭 incident / alert / feedback。
9. 不要自动批准 Go 决策。
10. Rollout 只记录计划、步骤、验证、证据与结果，不执行基础设施动作。
11. 不要破坏 1-38C 已有 API。
12. 前端保持中文暗色科技风 UI，复用现有组件。
13. IDs 对外保持 String。
14. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。

需要实现：
1. 新增 V41__init_release_rollout_tables.sql。
2. 新增 release_rollout_plan / release_rollout_step / release_verification_record 三张表。
3. 新增 6 个枚举：
   - ReleaseRolloutStatus
   - ReleaseRolloutStrategy
   - ReleaseRolloutStepStatus
   - ReleaseVerificationPhase
   - ReleaseVerificationStatus
   - ReleaseVerificationSeverity
4. 新增实体、Mapper、DTO。
5. 新增 ReleaseRolloutPlanService。
6. 新增 ReleaseVerificationService。
7. 新增 ReleaseReadinessReportService。
8. 新增 API：
   - rollout plan CRUD / status
   - rollout step list / create / update / status
   - verification list / create / update
   - readiness dashboard / summary / report
9. 前端新增：
   - ReleaseReadinessDashboardPanel.vue
   - ReleaseRolloutPlanPanel.vue
   - ReleaseVerificationPanel.vue
10. 集成到 ObservabilityPage.vue。
11. 后端测试不少于 24 个。
12. 前端 E2E 不少于 8 个。
13. 新增 docs/milestone-39a-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 三张 release rollout 表说明
3. ReleaseRolloutPlanService 设计说明
4. ReleaseVerificationService 设计说明
5. Release Readiness Dashboard 说明
6. Release Rollout Plan Panel 说明
7. Verification Panel 说明
8. Rollout / Rollback 边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 39B

现在开始实现，不要只给计划。
```
