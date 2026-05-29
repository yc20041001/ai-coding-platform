# Milestone 39A — Beta-to-Production Readiness & Controlled Rollout

## 状态

| 模块 | 状态 | 备注 |
|------|------|------|
| V41 迁移 SQL | ✅ 已完成 | 3 张表：release_rollout_plan / release_rollout_step / release_verification_record |
| schema.sql 同步 | ✅ 已完成 | 追加了 3 张表的 CREATE TABLE IF NOT EXISTS |
| 6 个枚举 | ✅ 已完成 | ReleaseRolloutStatus / ReleaseRolloutStrategy / ReleaseRolloutStepStatus / ReleaseVerificationPhase / ReleaseVerificationStatus / ReleaseVerificationSeverity |
| 3 个实体 | ✅ 已完成 | ReleaseRolloutPlanEntity / ReleaseRolloutStepEntity / ReleaseVerificationRecordEntity |
| 3 个 Mapper | ✅ 已完成 | MyBatis-Plus BaseMapper 扩展 |
| 12 个 DTO | ✅ 已完成 | Request/Response 类，String ID 字段，无 Lombok |
| ReleaseRolloutPlanService | ✅ 已完成 | Plan CRUD + 状态机校验（DRAFT→READY→IN_PROGRESS→OBSERVING→COMPLETED）+ NO_GO 前置检查 |
| ReleaseRolloutStepService | ✅ 已完成 | 默认 5 步骤初始化、步骤 CRUD、状态更新自动设置时间 |
| ReleaseVerificationService | ✅ 已完成 | 验证记录 CRUD，支持按阶段过滤 |
| ReleaseReadinessReportService | ✅ 已完成 | Dashboard 聚合、Summary 统计、Markdown 报告生成 |
| ReleaseRolloutController | ✅ 已完成 | 15 个 REST API 端点 |
| 前端 api.ts 类型 | ✅ 已完成 | 全部 API 接口定义 |
| 3 个前端面板 | ✅ 已完成 | ReleaseReadinessDashboardPanel / ReleaseRolloutPlanPanel / ReleaseVerificationPanel |
| ObservabilityPage 集成 | ✅ 已完成 | 新增 rollout 区域，含 3 个面板 |
| 后端集成测试 | ✅ 已完成 | 32 测试用例，全部通过 |
| 前端 E2E 测试 | ✅ 已完成 | 8 测试用例 |
| 前端类型检查 | ✅ 已完成 | vue-tsc --noEmit 通过 |
| 前端构建 | ✅ 已完成 | vite build 通过 |

## API 端点

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | /api/projects/{projectId}/rollout/plans | 创建 rollout plan |
| GET | /api/projects/{projectId}/rollout/plans | 列表 |
| GET | /api/projects/{projectId}/rollout/plans/{planId} | 详情 |
| PUT | /api/projects/{projectId}/rollout/plans/{planId} | 更新 |
| PUT | /api/projects/{projectId}/rollout/plans/{planId}/status | 状态流转 |
| GET | /api/projects/{projectId}/rollout/plans/{planId}/steps | 步骤列表 |
| POST | /api/projects/{projectId}/rollout/plans/{planId}/steps | 创建步骤 |
| PUT | /api/projects/{projectId}/rollout/plans/{planId}/steps/{stepId} | 更新步骤 |
| PUT | /api/projects/{projectId}/rollout/plans/{planId}/steps/{stepId}/status | 步骤状态 |
| GET | /api/projects/{projectId}/rollout/plans/{planId}/verifications | 验证列表 |
| POST | /api/projects/{projectId}/rollout/plans/{planId}/verifications | 创建验证 |
| PUT | /api/projects/{projectId}/rollout/plans/{planId}/verifications/{recordId} | 更新验证 |
| GET | /api/projects/{projectId}/rollout/readiness-dashboard | Dashboard |
| GET | /api/projects/{projectId}/rollout/plans/{planId}/summary | Summary |
| GET | /api/projects/{projectId}/rollout/plans/{planId}/report | 报告 |

## 关键设计决策

1. **状态机**: DRAFT → READY → IN_PROGRESS → OBSERVING → COMPLETED，支持 ROLLED_BACK 和 CANCELLED
2. **前置检查**: 进入 IN_PROGRESS 时检查最新决策不是 NO_GO
3. **默认步骤**: 创建 plan 时自动初始化 5 个默认步骤（代码审查、静态分析、单元测试、集成测试、预发部署验证）
4. **Dashboard 聚合**: 从 plan、step、verification、decision 表聚合就绪状态
5. **报告生成**: 生成包含步骤和验证状态的 Markdown 报告

## 测试覆盖

- 后端集成测试 32 个（10s 内通过），覆盖 Plan CRUD、状态流转、步骤 CRUD、验证 CRUD、Dashboard/Summary/Report
- 前端 E2E 测试 8 个，覆盖面板展示、Plan 创建、步骤查看、报告生成
- 质量门禁：`npm run typecheck` 零错误，`npm run build` 成功
