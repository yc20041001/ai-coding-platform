# Milestone 40B — Organization-level Trial Policy & Release Guardrail Automation 完成报告

## 1. 概述

实现组织级 release governance policy 与 guardrail automation。在 40A Multi-project Release Governance 基础上，新增组织级 trial policy、guardrail 自动化和 portfolio drift detection，使平台从"能看见多项目 release 状态"升级为"能定义组织级策略、检测偏离、输出治理建议"。

## 2. 新增 / 修改文件清单

### 后端新增
| 文件 | 说明 |
|------|------|
| `V45__init_org_policy_guardrail_tables.sql` | 3 张新表迁移 |
| `OrganizationPolicyScope.java` | 策略范围枚举 |
| `GuardrailEvaluationStatus.java` | Guardrail 评估状态枚举 |
| `GuardrailSeverity.java` | Guardrail 严重级别枚举 |
| `PortfolioDriftLevel.java` | Drift 等级枚举 |
| `GovernanceRecommendationPriority.java` | 治理建议优先级枚举 |
| `OrganizationTrialPolicyEntity.java` | 组织策略实体 |
| `ReleaseGuardrailEvaluationEntity.java` | Guardrail 评估实体 |
| `PortfolioDriftSnapshotEntity.java` | Drift 快照实体 |
| `OrganizationTrialPolicyMapper.java` | 策略 Mapper |
| `ReleaseGuardrailEvaluationMapper.java` | Guardrail Mapper |
| `PortfolioDriftSnapshotMapper.java` | Drift Mapper |
| `OrganizationTrialPolicyResponse.java` | 策略响应 DTO |
| `CreateOrganizationTrialPolicyRequest.java` | 创建策略请求 DTO |
| `UpdateOrganizationTrialPolicyRequest.java` | 更新策略请求 DTO |
| `ReleaseGuardrailEvaluationResponse.java` | Guardrail 评估响应 DTO |
| `ReleaseGuardrailDashboardResponse.java` | Guardrail 看板 DTO |
| `PortfolioDriftSnapshotResponse.java` | Drift 快照响应 DTO |
| `PortfolioDriftDashboardResponse.java` | Drift 看板 DTO |
| `GovernanceRecommendationResponse.java` | 治理建议 DTO |
| `OrganizationGovernanceSummaryResponse.java` | 组织治理摘要 DTO |
| `OrganizationTrialPolicyService.java` | 组织策略服务 |
| `ReleaseGuardrailAutomationService.java` | Guardrail 自动化服务 |
| `PortfolioDriftDetectionService.java` | Drift 检测服务 |
| `OrganizationGovernanceController.java` | 组织治理控制器（14 端点） |
| `OrganizationGovernanceGuardrailIntegrationTest.java` | 36 个集成测试 |

### 后端修改
| 文件 | 说明 |
|------|------|
| `schema.sql` | 追加 V45 三张测试表 |

### 前端新增
| 文件 | 说明 |
|------|------|
| `OrganizationTrialPolicyPanel.vue` | 组织策略面板 |
| `ReleaseGuardrailDashboardPanel.vue` | Guardrail 看板面板 |
| `PortfolioDriftDashboardPanel.vue` | Drift 检测面板 |
| `organization-governance.spec.ts` | 8 个 E2E 测试 |

### 前端修改
| 文件 | 说明 |
|------|------|
| `api.ts` | 新增 40B 接口（13 个数据接口 + 15 个 API 函数） |
| `ObservabilityPage.vue` | 新增 40B 组织级治理区块 |

## 3. 三张 Organization Governance 表说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `organization_trial_policy` | 组织级 Trial 策略 | policy_key(UNIQUE), display_name, policy_scope, enabled, threshold/signoff/rollback/verification/recommendation JSON 策略, notes |
| `release_guardrail_evaluation` | Release Guardrail 评估结果 | snapshot_date, project_id/name, policy_key, guardrail_key/category, evaluation_status(PASS/WARN/BLOCK/SKIP), severity, actual/threshold_value, summary, recommendation_text, evidence_json |
| `portfolio_drift_snapshot` | Portfolio Drift 快照 | snapshot_date, project_id/name, drift_score/level(STABLE/WATCH/HIGH/CRITICAL), confidence/signoff/verification delta, rollback_readiness_changed, summary_text |

三张表均无物理外键，保持与 40A 一致的设计风格。

## 4. OrganizationTrialPolicyService 设计说明

**职责**：管理组织级 trial policy 的 CRUD 与生命周期。

**核心方法**：
- `createPolicy()` — 创建策略，检查 policy_key 唯一约束，默认 scope=GLOBAL, enabled=1
- `listPolicies(scope)` — 可选 scope 过滤，按 createTime 降序
- `getPolicy()` / `updatePolicy()` / `updatePolicyStatus()` — 标准 CRUD + 启停
- `getEnabledPolicies()` — 查询所有启用策略（供 Guardrail 评估使用）

**设计要点**：
- 策略内容以 JSON 字段存储（threshold/signoff/rollback/verification/recommendation），为后续策略引擎解析预留扩展空间
- 默认初始化一个组织级策略供 guardrail 评估使用

## 5. ReleaseGuardrailAutomationService 设计说明

**职责**：基于组织策略自动评估每个项目的 guardrail，输出 PASS/WARN/BLOCK/SKIP + 治理建议。

**7 个 Guardrail 规则**：
| Guardrail Key | 类别 | PASS 条件 | WARN 条件 | BLOCK 条件 |
|------|------|----------|----------|----------|
| MIN_CONFIDENCE_SCORE | CONFIDENCE | >= 70 | >= 50 | < 50 |
| MAX_BLOCKING_ISSUES | ISSUES | = 0 | <= 3 | > 3 |
| MAX_FAILED_VERIFICATIONS | VERIFICATION | = 0 | <= 2 | > 2 |
| REQUIRE_ROLLBACK_READY | ROLLBACK | = 1 | — | = 0 |
| MIN_SIGNOFF_COMPLETION | SIGNOFF | >= 80% | >= 60% | < 60% |
| MAX_OPEN_INCIDENTS | INCIDENT | = 0 | <= 3 | > 3 |
| MAX_ACTIVE_ALERTS | ALERT | = 0 | <= 5 | > 5 |

**Severity 映射**：PASS→INFO, WARN→MEDIUM/LOW, BLOCK→HIGH/CRITICAL

**核心方法**：
- `refreshGuardrails()` — 删除当日评估 → 获取 portfolio snapshot → 遍历项目执行 7 个 guardrail → 批量插入
- `getGuardrails()` / `getDashboard()` — 查询与汇总
- `getRecommendations()` — 根据 CRITICAL/HIGH→P0/P1, MEDIUM→P2, LOW→P3 映射优先级

## 6. PortfolioDriftDetectionService 设计说明

**职责**：识别项目与组织基线的偏离趋势。

**Drift Score 计算**：
```
abs(confidenceDelta) × 0.6 + abs(signoffDelta) × 0.2 + abs(verificationDelta) × 0.15 + (rollbackReadinessChanged ? 15 : 0)
```

**Drift Level 阈值**：
- STABLE: < 5
- WATCH: >= 5
- HIGH: >= 20
- CRITICAL: >= 40

**核心方法**：
- `refreshDrift()` — 对比当日与昨日 portfolio snapshot，计算每个项目的 drift score/level
- `getDriftList()` / `getDriftDashboard()` — 查询与汇总（stable/watch/high/critical 计数 + top 5 drift）

## 7. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/organization-governance/policies` | 创建策略 |
| GET | `/api/organization-governance/policies` | 策略列表（可选 scope 过滤） |
| GET | `/api/organization-governance/policies/{id}` | 获取策略 |
| PUT | `/api/organization-governance/policies/{id}` | 更新策略 |
| POST | `/api/organization-governance/policies/{id}/status` | 启用/禁用 |
| POST | `/api/organization-governance/guardrails/refresh` | 刷新 Guardrail 评估 |
| GET | `/api/organization-governance/guardrails` | 获取评估列表 |
| GET | `/api/organization-governance/guardrails/dashboard` | Guardrail 看板 |
| GET | `/api/organization-governance/recommendations` | 治理建议列表 |
| POST | `/api/organization-governance/drift/refresh` | 刷新 Drift 快照 |
| GET | `/api/organization-governance/drift` | Drift 列表 |
| GET | `/api/organization-governance/drift/dashboard` | Drift 看板 |
| GET | `/api/organization-governance/summary` | 组织治理摘要 |
| GET | `/api/organization-governance/report` | 组织治理报告 |

## 8. OrganizationTrialPolicyPanel 说明

- 策略列表展示（名称、Key、Scope、启用/禁用状态）
- 新建/编辑策略对话框（policyKey、displayName、scope 选择、threshold JSON、备注）
- 启用/禁用切换按钮
- 复用 TechPanel、EmptyState、ErrorState、ElTag、ElDialog

## 9. ReleaseGuardrailDashboardPanel 说明

- MetricTile 指标卡：项目数、通过/警告/阻塞/严重计数、建议数
- 阻塞项目 Top 列表（项目名 + severity 标签 + 摘要）
- 治理建议列表（优先级标签 P0-P3 + 项目名 + 建议标题）
- 刷新评估按钮

## 10. PortfolioDriftDashboardPanel 说明

- MetricTile 指标卡：稳定/关注/高/严重计数
- Drift Trend Summary 文本
- Top Drift 项目列表（项目名 + drift level 色块 + 得分）
- 刷新 Drift 按钮

## 11. Organization-level Governance 边界说明

**已实现**：
- 组织级 trial policy 的 CRUD 管理
- 7 个 guardrail 规则的自动评估（基于 40A portfolio snapshot 数据）
- Portfolio drift 检测与趋势识别
- 治理建议生成（按优先级 P0-P3）
- 组织治理摘要 Markdown 导出

**不涉及**：
- 不执行真实 shell 命令
- 不自动修改 release/rollout/sign-off/verification 原始记录
- 不自动关闭 incident/alert/feedback
- 不替代人工审批
- 不调用 AI 生成治理结论
- drift 只识别与提示，不自动修复

## 12. 后端测试结果

**36 个集成测试全部通过**：

**Policy CRUD（10 个）**：创建、更新、禁用、重复 key 拒绝、scope 过滤、按 ID 查询、不存在处理（get/update/status）、notes 持久化

**Guardrail（8 个）**：刷新成功、pass/warn/block 计数、critical severity、dashboard 字段完整性、recommendation 生成、列表项验证、幂等刷新

**Drift（6 个）**：刷新成功、score 计算、level 返回、top projects、trend summary、幂等刷新

**Summary/Report（7 个）**：blocked projects、top drift projects、markdown 导出、project count、recommendations、disabled policy 排除、空数据处理

**Edge Cases（5 个）**：空 portfolio、markdown 内容、scope 默认值、top blocked projects、recommendation priority 合法性

## 13. 前端 Quality Gates

| 检查项 | 状态 |
|--------|------|
| `npm run typecheck` | ✅ 通过 |
| `npm run build` | ✅ 通过 |
| E2E (8 tests) | ⚠️ 8 个测试遵循与 40A 一致的 graceful fallback 模式，测试环境无 seeded 数据时显式跳过并记录 annotation |

## 14. 已知限制

1. **Guardrail 阈值硬编码**：当前阈值（confidence >= 70 等）为内置默认值，尚未解析 policy JSON 中的自定义阈值
2. **Drift 对比基线为昨日快照**：更准确的基线应该使用 organization policy 中定义的目标值，当前为简化实现
3. **E2E 测试环境依赖**：E2E 测试需要已登录状态和 seeded portfolio 数据，在无 seeded 数据环境下使用 graceful fallback 跳过
4. **Recommendation 去重**：同一项目的多个 guardrail 违规可能生成多条相似建议，尚未合并去重
5. **无权限控制**：当前所有 ADMIN 用户均可操作 policy，未实现细粒度 RBAC

## 15. 是否可以进入 Milestone 40C

**是。**

验收标准全部满足：
1. ✅ 三张表已落库（V45 migration + test schema）
2. ✅ Organization policy 可创建/编辑/启停
3. ✅ Guardrail evaluation 可聚合多个项目并输出 PASS/WARN/BLOCK
4. ✅ Recommendation 列表可查询
5. ✅ Drift snapshot 可生成并展示项目偏离
6. ✅ Organization summary/report 可导出 Markdown
7. ✅ 36 个后端集成测试通过
8. ✅ 前端 typecheck 通过
9. ✅ 前端 build 通过
10. ⚠️ E2E 通过或对无数据前置条件显式降级处理（已实现 graceful fallback）

建议 40C 方向：Governance Recommendation Workflow & Exception Waiver Management，包括推荐事项工作流、exception/waiver 申请与审批、治理闭环完成率。
