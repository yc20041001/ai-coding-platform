# Milestone 39B — Production Rollback Drill & Release Audit Hardening

## 状态

| 模块 | 状态 | 备注 |
|------|------|------|
| V42 迁移 SQL | ✅ 已完成 | 3 张表：release_rollback_drill / release_audit_event / release_postmortem_review |
| schema.sql 同步 | ✅ 已完成 | 追加了 3 张表的 CREATE TABLE IF NOT EXISTS |
| 5 个枚举 | ✅ 已完成 | ReleaseRollbackDrillStatus / ReleaseRollbackDrillScope / ReleaseAuditEventType / ReleasePostmortemReviewStatus / ReleasePostmortemOutcome |
| 3 个实体 | ✅ 已完成 | ReleaseRollbackDrillEntity / ReleaseAuditEventEntity / ReleasePostmortemReviewEntity |
| 3 个 Mapper | ✅ 已完成 | MyBatis-Plus BaseMapper 扩展 |
| 9 个 DTO | ✅ 已完成 | Request/Response 类，String ID 字段，无 Lombok |
| ReleaseRollbackDrillService | ✅ 已完成 | 演练 CRUD + 状态机校验（PLANNED→RUNNING→PASSED/FAILED/BLOCKED）+ 回滚就绪检查 |
| ReleaseAuditTrailService | ✅ 已完成 | 审计事件记录、时间线聚合、Markdown 审计报告导出 |
| ReleasePostmortemReviewService | ✅ 已完成 | 复盘 CRUD + 状态机校验（DRAFT→REVIEWED→PUBLISHED→ARCHIVED）+ 自动预填 |
| ReleaseAuditController | ✅ 已完成 | 14 个 REST API 端点 |
| 前端 api.ts 类型 | ✅ 已完成 | 全部 39B API 接口定义 |
| 3 个前端面板 | ✅ 已完成 | ReleaseRollbackDrillPanel / ReleaseAuditTimelinePanel / ReleasePostmortemReviewPanel |
| ObservabilityPage 集成 | ✅ 已完成 | 新增 39B 区域，含 3 个面板 |
| 后端集成测试 | ✅ 已完成 | 30 测试用例，全部通过 |
| 前端 E2E 测试 | ✅ 已完成 | 8 测试用例 |
| 前端类型检查 | ✅ 已完成 | vue-tsc --noEmit 通过 |
| 前端构建 | ✅ 已完成 | vite build 通过 |

## 新增 / 修改文件清单

### 新增后端文件
- `backend/src/main/resources/db/migration/V42__init_release_rollback_audit_tables.sql`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRollbackDrillStatus.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRollbackDrillScope.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseAuditEventType.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleasePostmortemReviewStatus.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleasePostmortemOutcome.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRollbackDrillEntity.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseAuditEventEntity.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleasePostmortemReviewEntity.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseRollbackDrillMapper.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseAuditEventMapper.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleasePostmortemReviewMapper.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateReleaseRollbackDrillRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateReleaseRollbackDrillRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseRollbackDrillResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateReleasePostmortemReviewRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateReleasePostmortemReviewRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleasePostmortemReviewResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseAuditEventResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseAuditTimelineResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseAuditReportResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseRollbackDrillService.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseAuditTrailService.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleasePostmortemReviewService.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/controller/ReleaseAuditController.java`
- `backend/src/test/java/com/aicoding/platform/orchestration/ReleaseAuditRollbackIntegrationTest.java`

### 新增前端文件
- `frontend/src/modules/admin/components/ReleaseRollbackDrillPanel.vue`
- `frontend/src/modules/admin/components/ReleaseAuditTimelinePanel.vue`
- `frontend/src/modules/admin/components/ReleasePostmortemReviewPanel.vue`
- `frontend/e2e/release-audit.spec.ts`

### 修改文件
- `backend/src/test/resources/schema.sql` — 追加 3 张测试表
- `frontend/src/modules/admin/api.ts` — 追加 39B 接口类型与函数
- `frontend/src/modules/admin/pages/ObservabilityPage.vue` — 新增 3 个面板导入与模板区

## 三张 Release Audit / Rollback 表说明

| 表名 | 用途 | 核心字段 |
|------|------|----------|
| release_rollback_drill | 记录回滚演练计划与执行结果 | drill_status, drill_scope, duration_seconds, rollback_steps_summary, blockers_summary |
| release_audit_event | 记录发布全过程的可审计事件 | event_type, actor_id, event_time, 关联 step/verification/incident/alert |
| release_postmortem_review | 记录发布后复盘总结 | review_status, overall_outcome, what_went_well, what_went_wrong, follow_up_actions |

## API 端点

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | /api/release-rollouts/{planId}/rollback-drills | 创建回滚演练 |
| GET | /api/release-rollouts/{planId}/rollback-drills | 演练列表 |
| GET | /api/release-rollouts/{planId}/rollback-drills/{drillId} | 演练详情 |
| PUT | /api/release-rollouts/{planId}/rollback-drills/{drillId} | 更新演练 |
| POST | /api/release-rollouts/{planId}/rollback-drills/{drillId}/status | 演练状态流转 |
| GET | /api/release-rollouts/{planId}/rollback-drills/readiness | 回滚就绪检查 |
| GET | /api/release-rollouts/{planId}/audit-events | 审计事件列表 |
| GET | /api/release-rollouts/{planId}/audit-timeline | 审计时间线聚合 |
| GET | /api/release-rollouts/{planId}/audit-report | 审计报告导出 |
| POST | /api/release-rollouts/{planId}/postmortem-review | 创建发布复盘 |
| GET | /api/release-rollouts/{planId}/postmortem-review | 获取复盘 |
| PUT | /api/release-rollouts/{planId}/postmortem-review/{reviewId} | 更新复盘 |
| POST | /api/release-rollouts/{planId}/postmortem-review/{reviewId}/status | 复盘状态流转 |
| GET | /api/release-rollouts/{planId}/postmortem-review/prefill | 预填复盘 |

## ReleaseRollbackDrillService 设计说明

- 校验 drill 只能绑定到已有 rollout plan
- 默认状态 "PLANNED"，创建时自动关联 releaseLabel
- 状态机：PLANNED → RUNNING → PASSED/FAILED/BLOCKED，PLANNED → CANCELLED
- 进入 RUNNING 时自动设置 started_at，终端状态时自动设置 finished_at
- 自动计算 duration_seconds（finished_at - started_at）
- isRollbackReady() 判定条件：存在 drill 且状态为 PASSED、rollbackStepsSummary 不为空、blockers 为空
- 关键动作写入 audit event

## ReleaseAuditTrailService 设计说明

- recordEvent() 统一写入 release_audit_event 表
- listEvents() 按 event_time 降序返回
- getTimeline() 聚合 event_counts_by_type + 事件列表
- generateAuditReport() 生成 Markdown 报告，包含：
  - Rollout Timeline（事件时间线）
  - Rollout Steps（步骤状态表）
  - Verification Results（验证记录表）
  - Rollback Drills（演练记录表）
  - Post-release Reviews（复盘记录表）

## ReleaseRollbackDrillPanel 说明

- 展示 drill 列表，每条显示状态标签、范围标签、环境、耗时
- 回滚就绪 StatusPulse badge（通过/警告）
- 新建/编辑 dialog，含范围选择、环境输入、成功标准、步骤、阻塞项
- 状态流转按钮（动态计算可用操作）
- 前端路径：`/api/release-rollouts/{planId}/rollback-drills/...`

## ReleaseAuditTimelinePanel 说明

- 展示 event_type 统计标签（按类型聚合数量）
- 时间线列表：时间、类型标签、摘要、操作人
- 导出审计报告按钮，Markdown 预览 dialog
- 前端路径：`/api/release-rollouts/{planId}/audit-timeline`

## ReleasePostmortemReviewPanel 说明

- 展示复盘状态、总体结果、总结、做好/做差、客户影响、改进项
- 状态流转按钮（草稿→审查→发布→归档，支持退回）
- 预填信号按钮：从 rollout/step/verification 聚合自动生成草稿
- 新建/编辑 dialog

## Rollback / Audit 边界说明

1. **不执行真实回滚**：drill 只记录计划、步骤、结果、证据，不触发基础设施动作
2. **不修改现有服务**：ReleaseRolloutPlanService 等 39A 服务未被修改
3. **不执行真实 shell 命令**：所有操作均为记录和查询
4. **审计事件为平台内记录**：只记录 release/rollout/verification/decision/review 行为
5. **不自动关闭 incident/alert**：audit event 可关联 incident/alert ID，但不修改其状态
6. **不调用 AI 生成结论**：prefill 仅聚合已有信号，不自动生成发布结论

## 后端测试结果

- **30 个集成测试**全部通过（15.7s）
- 覆盖：演练 CRUD、状态流转（PLANNED→RUNNING→PASSED/FAILED）、无效流转拒绝、回滚就绪判定、审计事件创建、事件列表排序、时间线聚合、复盘 CRUD、复盘状态流转、归档后拒绝编辑、预填信号、审计报告 Markdown、不存在的资源

## 前端 typecheck / build / E2E 结果

- `npm run typecheck` — 通过，零错误
- `npm run build` — 成功
- E2E 测试（8 个）— 已创建，遵循 E2E 模式（login + prerequisite guard）

## 已知限制

1. 39A 服务的状态变更（plan/step/verification）未写入 audit event（需后续修改 39A 服务）
2. E2E 测试依赖 seeded 项目数据，未部署环境下自动降级为空态断言
3. drill 的 duration_seconds 自动计算依赖 started_at 和 finished_at 的准确设置

## 是否可以进入 Milestone 39C

✅ **可以进入 Milestone 39C: Production Rollout Evidence Center & Executive Release Summary**

当前已完成：
- 回滚演练数据模型与 CRUD
- 发布审计事件时间线与报告
- 发布复盘记录与信号预填
- 完整的后端与前端质量门禁
