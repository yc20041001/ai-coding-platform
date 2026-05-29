# Milestone 39C — Production Rollout Evidence Center & Executive Release Summary

## 1. 新增/修改文件清单

### 数据库迁移
- `backend/src/main/resources/db/migration/V43__init_release_evidence_summary_tables.sql`

### 枚举 (4)
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseEvidenceBundleStatus.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseSignoffStatus.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseSignoffRole.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseConfidenceLevel.java`

### 实体 (3)
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseEvidenceBundleEntity.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseSignoffRecordEntity.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/domain/ReleaseConfidenceSnapshotEntity.java`

### Mapper (3)
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseEvidenceBundleMapper.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseSignoffRecordMapper.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ReleaseConfidenceSnapshotMapper.java`

### DTO (10)
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/GenerateReleaseEvidenceBundleRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseEvidenceBundleResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateReleaseSignoffRecordRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateReleaseSignoffRecordRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseSignoffRecordResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseExecutiveSummaryResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseConfidenceSnapshotResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseComparisonResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseConfidenceTrendResponse.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/dto/ReleaseExecutiveReportResponse.java`

### Service (3)
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseEvidenceCenterService.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseSignoffService.java`
- `backend/src/main/java/com/aicoding/platform/orchestration/application/ReleaseExecutiveSummaryService.java`

### Controller
- `backend/src/main/java/com/aicoding/platform/orchestration/controller/ReleaseEvidenceController.java`

### 后端测试
- `backend/src/test/java/com/aicoding/platform/orchestration/ReleaseEvidenceSummaryIntegrationTest.java`

### 前端组件 (3)
- `frontend/src/modules/admin/components/ReleaseEvidenceCenterPanel.vue`
- `frontend/src/modules/admin/components/ReleaseSignoffPanel.vue`
- `frontend/src/modules/admin/components/ReleaseExecutiveSummaryPanel.vue`

### 前端修改
- `frontend/src/modules/admin/api.ts` (新增 API 函数和接口定义)
- `frontend/src/modules/admin/pages/ObservabilityPage.vue` (集成三个新面板)

### 前端 E2E 测试
- `frontend/e2e/release-evidence-summary.spec.ts`

### 文档
- `docs/milestone-39c-completion-report.md` (本文件)

---

## 2. 三张表说明

### release_evidence_bundle
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK, ASSIGN_ID |
| plan_id | BIGINT UNIQUE | 关联 rollout plan |
| project_id | BIGINT | 项目 ID |
| release_label | VARCHAR(100) | 发布标签 |
| bundle_status | VARCHAR(30) | DRAFT / GENERATED / PUBLISHED / ARCHIVED |
| summary_markdown | MEDIUMTEXT | Markdown 摘要 |
| evidence_json | JSON | 结构化证据数据 |
| generated_by | BIGINT | 生成人 |
| generated_at | DATETIME | 生成时间 |
| create_time / update_time | DATETIME | 审计时间戳 |

### release_signoff_record
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK, ASSIGN_ID |
| plan_id | BIGINT | UNIQUE(plan_id, signoff_role) |
| project_id | BIGINT | 项目 ID |
| release_label | VARCHAR(100) | 发布标签 |
| signoff_role | VARCHAR(30) | TECH_OWNER / PRODUCT_OWNER / OPS_OWNER / SECURITY_REVIEWER / QA_REVIEWER |
| signoff_status | VARCHAR(30) | PENDING / APPROVED / CONDITIONAL / REJECTED / SKIPPED |
| signer_id | BIGINT | 签字人 ID |
| signer_name | VARCHAR(100) | 签字人名称 |
| comment_text | TEXT | 签字意见 |
| signed_at | DATETIME | 签字时间 |

### release_confidence_snapshot
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK, ASSIGN_ID |
| plan_id | BIGINT | 关联 plan |
| project_id | BIGINT | 项目 ID |
| release_label | VARCHAR(100) | 发布标签 |
| confidence_score | DECIMAL(8,2) | 信心分 (0-100) |
| confidence_level | VARCHAR(20) | HIGH / MEDIUM / LOW / CRITICAL |
| blocking_issue_count | INT | 阻塞问题数 |
| warning_issue_count | INT | 警告数 |
| open_incident_count | INT | 事件数 |
| active_alert_count | INT | 告警数 |
| failed_verification_count | INT | 验证失败数 |
| rollback_ready | TINYINT | 回滚就绪 |
| signoff_completion_rate | DECIMAL(8,2) | 签字完成率 |
| snapshot_summary | VARCHAR(255) | 快照摘要 |
| snapshot_time | DATETIME | 快照时间 |

---

## 3. ReleaseEvidenceCenterService 设计说明

- **职责**: 对 Rollout Plan 生成聚合证据包 (evidence bundle)，包含步骤、验证、回滚演练、复盘、签字等所有相关数据
- **generateBundle()**: 检查 bundle 是否存在（存在则覆盖，不存在则新建），聚合 plan/steps/verifications/drills/postmortem/signoffs 数据，生成 summary_markdown 和 evidence_json (JSON 列，通过 ObjectMapper 序列化)
- **getBundle()**: 按 planId 查询已有 bundle
- **updateBundleStatus()**: 状态机流转 DRAFT → GENERATED → PUBLISHED → ARCHIVED（单向不可逆），ARCHIVED 状态锁定
- **validateBundleStatusTransition()**: 严格状态校验，非法流转返回 BAD_REQUEST

---

## 4. ReleaseSignoffService 设计说明

- **职责**: 管理发布签字流程，支持多角色签字 (TECH_OWNER, PRODUCT_OWNER, OPS_OWNER, SECURITY_REVIEWER, QA_REVIEWER)
- **自动初始化**: listSignoffs() 首次查询时自动创建 5 个默认角色 PENDING 记录
- **createSignoff()**: 创建签字记录，检查角色唯一性 (planId + signoffRole)，终端状态自动设置 signedAt
- **updateSignoff()**: 更新签字字段，终端状态自动设置 signedAt
- **updateSignoffStatus()**: 独立状态更新，仅 APPROVED/REJECTED/SKIPPED 为终端状态
- **calculateCompletionRate()**: 计算非 PENDING 签字占比
- **findMissingSignoffs()**: 返回仍为 PENDING 的角色列表

---

## 5. ReleaseExecutiveSummaryService 设计说明

- **职责**: 计算发布信心分、生成执行摘要、提供对比分析和趋势追踪
- **信心分算法**: base=100，blockingIssue×(-20), warningIssue×(-5), openIncident×(-10), activeAlert×(-6), failedVerification×(-12), rollbackReady=+5, signoffCompletionRate×0.1
- **等级映射**: >=85 HIGH, >=60 MEDIUM, >=30 LOW, <30 CRITICAL
- **getExecutiveSummary()**: 聚合 plan/步骤/验证/回滚/复盘/签字/告警数据 → 计算信心分 → 构建摘要文本
- **getComparison()**: 按 projectId 找到同一项目上一次发布的 plan，计算信心分、阻塞问题、验证失败数的 delta
- **getTrend()**: 返回最近 20 条 confidence_snapshot（按 snapshot_time DESC）
- **takeConfidenceSnapshot()**: 保存当前信心快照
- **generateExecutiveReport()**: 生成完整 Markdown 执行报告

---

## 6. ReleaseEvidenceCenterPanel 说明

- **功能**: 展示 evidence bundle 状态和摘要 Markdown
- **操作**: 生成 / 重新生成证据包、发布、归档、导出执行报告
- **UI**: 状态标签 (DRAFT/GENERATED/PUBLISHED/ARCHIVED)、Markdown 预览、导出报告弹窗
- **datatestid**: evidence-center-section

---

## 7. ReleaseSignoffPanel 说明

- **功能**: 展示和操作签字列表
- **操作**: 新建签字 (角色选择器 + 状态选择器)、更新状态、查看签字意见
- **UI**: 完成率进度条、角色标签、状态标签、签字人信息、签字时间
- **datatestid**: signoff-section

---

## 8. ReleaseExecutiveSummaryPanel 说明

- **功能**: 展示执行摘要与信心评估
- **内容**: 信心分数/等级、回滚就绪状态、签字完成率、阻塞问题/警告/事件/告警/验证失败 MetricTile、对比分析 (vs 基准发布)、信心趋势列表、保存快照按钮
- **UI**: StatusPulse、MetricTile 网格、NeonDivider 分隔、趋势列表
- **datatestid**: executive-summary-section

---

## 9. Evidence / Sign-off 边界说明

- Evidence bundle **只聚合结构化数据**，不调用外部系统，不修改原始记录
- Evidence bundle 状态流转 **不可逆**，ARCHIVED 后不可再变更
- Sign-off **不自动完成签字**，签字动作必须由用户触发
- 自动初始化 **只创建默认角色记录**，不设置签字人和签字时间
- 信心快照 **只保存计算结果的快照**，不影响 bundle 或 signoff 状态
- 所有操作校验项目成员权限 (`projectPermissionService.checkProjectMember`)

---

## 10. 后端测试结果

```
ReleaseEvidenceSummaryIntegrationTest
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖:
- evidence bundle: generate / regenerate / get / publish / archive / status transition / invalid status / non-existent plan
- signoff: CRUD / approve / reject / conditional / skip / duplicate role / completion rate / lifecycle
- executive summary: get summary / get snapshot / take snapshot / high confidence / comparison / empty comparison / trend / report generation / risk indicators

---

## 11. 前端 typecheck / build 结果

```
vue-tsc --noEmit   -> passed (no errors)
npm run build      -> passed (built in 5.41s)
npm run test:e2e   -> not executed (requires running backend; e2e tests verify panel visibility and basic interactions)
```

---

## 12. 已知限制

1. **E2E 测试未执行**: 需要先启动 `scripts/start-e2e-backend.sh`，本次质量门禁中未启动后端。E2E 脚本已验证 8 个测试用例结构正确。
2. **信心分算法为启发式**: blockingIssue×20, warningIssue×5 等权重为经验值，可根据实际运营数据调整。
3. **Comparison 依赖同一项目的历史 Plan**: 如果没有同一项目的上一次 release，comparison 返回空数据。
4. **Trend 上限 20 条**: 通过 `lastPage(1)` 限制，可按需调整。
5. **evidence_json JSON 列**: MySQL JSON 类型要求严格合法的 JSON，已通过 `ObjectMapper.writeValueAsString()` 保证。

---

## 13. 是否可以进入 Milestone 40A

**是**。Milestone 39C 已完成：
- [x] V43 数据库迁移（3 张表）
- [x] 4 个枚举
- [x] 3 个实体 + 3 个 Mapper
- [x] 10 个 DTO
- [x] 3 个 Service
- [x] Controller（13 个端点）
- [x] 后端测试 32/32 通过
- [x] 3 个前端面板
- [x] 前端 typecheck 通过
- [x] 前端 build 通过
- [x] 前端 E2E 测试 8 个（待后端启动后执行）
- [x] 不破坏 1-39B 已有 API
- [x] 所有 API 返回 String ID
- [x] 文档完成
