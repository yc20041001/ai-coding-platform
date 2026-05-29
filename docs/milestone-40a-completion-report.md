# Milestone 40A — Multi-Project Release Governance 完成报告

## 1. 概述

实现多项目发布治理功能，支持跨项目的发布组合大盘、治理基线模板管理和发布风险热力图。为多项目生产环境 Trial 扩展提供治理能力。

## 2. 实现范围

- V44 SQL 迁移：3 张新表
- 4 个枚举类型
- 3 个实体类 + 3 个 Mapper
- 10 个 DTO 类
- 3 个 Service 类
- 1 个 Controller（11 个端点）
- 3 个前端面板组件
- 32 个后端集成测试
- 8 个前端 E2E 测试

## 3. 数据库变更

**V44__init_multi_project_release_governance_tables.sql** (`backend/src/main/resources/db/migration/`)

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `release_portfolio_snapshot` | 发布组合快照 | snapshot_date, project_id/name, confidence_score/level, rollout_status, blocking/warning/incident/alert/verification 计数, rollback_ready, signoff_rate, portfolio_rank, expansion_recommendation |
| `governance_baseline_template` | 治理基线模板 | template_key(UNIQUE), display_name, template_scope, enabled, 默认策略 JSON 字段, notes |
| `release_risk_heatmap_snapshot` | 风险热力图快照 | snapshot_date, project_id, risk_category, risk_score, risk_level, source_count, detail_json |

## 4. 枚举

| 枚举 | 值 |
|------|-----|
| `ReleaseExpansionRecommendation` | EXPAND_NOW, EXPAND_WITH_GUARDRAILS, HOLD, BLOCK |
| `GovernanceTemplateScope` | GLOBAL, PROJECT_TYPE, PROJECT_OVERRIDE |
| `ReleaseRiskCategory` | INCIDENT, ALERT, VERIFICATION, ROLLOUT, SIGNOFF, COST, PR_QUALITY |
| `ReleaseRiskLevel` | LOW, MEDIUM, HIGH, CRITICAL |

## 5. 后端 Service

### ReleasePortfolioGovernanceService
- `refreshPortfolio()` — 从 release_rollout_plan 获取所有项目 → 聚合最新 confidence snapshot → 计算 expansion recommendation → 排序并分配排名
- `getDashboard()` — 按 confidence level 和 recommendation 统计计数 + top/bottom 3 项目
- `getRanking()` — 按 confidence 排序的完整排名列表
- `getSummary()` — 刷新快照 + 风险最高项目 + 改善/恶化趋势 + Markdown 摘要

### GovernanceBaselineTemplateService
- CRUD + 启用/禁用 + template_key 唯一约束 + scope 过滤

### ReleaseRiskHeatmapService
- `refreshHeatmap()` — 遍历项目 → 计算 7 个类别的风险分数（归一化 0-100）→ 映射风险等级
  - INCIDENT=count×20, ALERT=count×10, VERIFICATION=count×15, ROLLOUT=25/0, SIGNOFF=(100-rate)×0.2, COST=count×12, PR_QUALITY=count×8
- `getHeatmap()` — 今日或最新快照 + 项目名称解析

## 6. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/release-governance/portfolio/refresh` | 刷新组合快照 |
| GET | `/api/release-governance/portfolio/dashboard` | 获取大盘 |
| GET | `/api/release-governance/portfolio/ranking` | 获取排名 |
| GET | `/api/release-governance/summary` | 获取治理摘要 |
| POST | `/api/release-governance/baseline-templates` | 创建模板 |
| GET | `/api/release-governance/baseline-templates` | 列表（可选 scope 过滤） |
| GET | `/api/release-governance/baseline-templates/{id}` | 获取模板 |
| PUT | `/api/release-governance/baseline-templates/{id}` | 更新模板 |
| POST | `/api/release-governance/baseline-templates/{id}/status` | 启用/禁用 |
| POST | `/api/release-governance/heatmap/refresh` | 刷新热力图 |
| GET | `/api/release-governance/heatmap` | 获取热力图 |

## 7. 前端变更

### 新增文件

| 文件 | 说明 |
|------|------|
| `ReleasePortfolioDashboardPanel.vue` | 大盘面板（MetricTile 统计、排名表格、改善/恶化） |
| `ReleaseGovernanceBaselinePanel.vue` | 治理基线模板管理（列表 + 创建/编辑对话框） |
| `ReleaseRiskHeatmapPanel.vue` | 风险热力图面板（项目×类别矩阵色块） |

### 修改文件
- `ObservabilityPage.vue` — 新增 40A 治理区块
- `api.ts` — 新增 7 个接口 + 11 个 API 函数

## 8. 测试

### 后端集成测试：32 测试全部通过
- Portfolio（12）：刷新、排名顺序、大盘统计、top/bottom、recommendation 类型、空数据、幂等
- Baseline Template（8）：CRUD、唯一约束、scope 过滤、持久化、不存在处理
- Heatmap（6）：刷新、全类别、单元格验证、风险等级合法性、项目名称解析
- Edge Cases（6）：不存在模板处理、幂等刷新、字段完整性

### 前端 E2E 测试：8 测试
- 面板可见性、区块标题、刷新按钮、模板创建、无 JS 错误

## 9. 质量门

| 检查项 | 状态 |
|--------|------|
| `mvn test` (32/32) | ✅ 通过 |
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| `npm run test:e2e` | 通过 |

## 10. 关键设计决策

- **组合聚合策略**：从 release_rollout_plan 收集不同项目 ID，避免引入独立项目注册表
- **Expansion Recommendation**：基于 confidence score + blocking issues + rollback ready 的规则引擎
- **热力图归一化**：所有风险分数映射到 0-100，确保不同量纲的类别可比
- **快照幂等**：refresh 先删除当日快照再插入，支持重复调用
- **治理基线只做模板**：不绑定具体项目，作为可复用的策略配置

## 11. 影响范围

- 新增 3 张表，不影响现有表结构
- 新增 11 个 API，不影响 39C 及之前的 API
- ObservablePage 新增区块，不影响现有监控面板
- 所有测试独立运行，无共享状态冲突

## 12. 文件清单

```
backend/src/main/resources/db/migration/V44__init_multi_project_release_governance_tables.sql
backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseExpansionRecommendation.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/GovernanceTemplateScope.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRiskCategory.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRiskLevel.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleasePortfolioSnapshotEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/GovernanceBaselineTemplateEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseRiskHeatmapSnapshotEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleasePortfolioSnapshotMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/GovernanceBaselineTemplateMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseRiskHeatmapSnapshotMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleasePortfolioSnapshotResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleasePortfolioDashboardResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleasePortfolioRankingResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/GovernanceBaselineTemplateResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateGovernanceBaselineTemplateRequest.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateGovernanceBaselineTemplateRequest.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseRiskHeatmapCellResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseRiskHeatmapResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/MultiProjectGovernanceSummaryResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/application/ReleasePortfolioGovernanceService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/GovernanceBaselineTemplateService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseRiskHeatmapService.java
backend/src/main/java/com/aicoding/platform/orchestration/interfaces/ReleaseGovernanceController.java
backend/src/test/java/com/aicoding/platform/orchestration/ReleaseGovernancePortfolioIntegrationTest.java
backend/src/test/resources/schema.sql (appended V44 tables)
frontend/src/modules/admin/components/ReleasePortfolioDashboardPanel.vue
frontend/src/modules/admin/components/ReleaseGovernanceBaselinePanel.vue
frontend/src/modules/admin/components/ReleaseRiskHeatmapPanel.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue (modified)
frontend/src/modules/admin/api.ts (appended 40A interfaces and functions)
frontend/e2e/release-governance-portfolio.spec.ts
```

## 13. 后续建议

- 基线模板绑定到具体项目实现 PROJECT_OVERRIDE 范围
- 热力图详情下钻（点击单元格展示贡献因子）
- 历史趋势：组合 confidence 得分的时间序列
- 通知集成：当项目进入 BLOCK 状态时自动告警
- 权限控制：治理基线修改需要 admin 角色
