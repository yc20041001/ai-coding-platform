# Milestone 35F: Persisted Workflow Template Management — 完成报告

## 1. 新增 / 修改文件清单

### 新增文件 (9 files)

**Backend — Domain / DTO / Mapper / Service / Controller (6 files)**

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/WorkflowTemplateStatus.java` | 工作流模板状态枚举 (ENABLED / DISABLED) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/WorkflowTemplateEntity.java` | 工作流模板实体，映射 workflow_template 表 |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/WorkflowTemplateMapper.java` | MyBatis-Plus Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/WorkflowTemplateResponse.java` | 模板详情响应 DTO（含 strategy / phaseCount / stepCount） |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateWorkflowTemplateStatusRequest.java` | 状态更新请求 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/WorkflowTemplateApplicationService.java` | 模板管理服务（list / detail / updateStatus，ADMIN only） |

**Backend — Migration (1 file)**

| `backend/src/main/resources/db/migration/V19__init_workflow_template_tables.sql` | Flyway 迁移：建表 + Seed 4 个内置模板 |

**Backend — Controller (1 file)**

| `backend/src/main/java/com/aicoding/platform/orchestration/controller/WorkflowTemplateController.java` | 模板管理 REST API 控制器 |

**Frontend (2 files)**

| `frontend/src/modules/workflow/api.ts` | 模板 API 类型与函数 |
| `frontend/src/modules/workflow/pages/WorkflowTemplatePage.vue` | 工作流模板管理页面（列表、筛选、详情抽屉、启用/停用） |

**Backend — Test (1 file)**

| `backend/src/test/java/com/aicoding/platform/orchestration/WorkflowTemplateIntegrationTest.java` | 集成测试（12 tests） |

**Frontend — E2E (1 file)**

| `frontend/e2e/workflow-template.spec.ts` | E2E 测试（8 tests） |

### 修改文件 (4 files)

| 文件 | 变更说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/application/WorkflowStrategyCatalogService.java` | 重构：listStrategies / resolveTemplate 改为 DB-first + built-in fallback；新增 toResponse(entity) 完整解析 phases/steps |
| `backend/src/test/resources/schema.sql` | 新增 V19 workflow_template 建表 + Seed 数据（H2 兼容 LONGTEXT） |
| `frontend/src/app/router/index.ts` | 新增 `/workflow-templates` 路由 |
| `frontend/src/shared/components/FloatingDock.vue` | 新增 "工作流" 导航入口（ADMIN only，showObservability 下显示） |

## 2. Workflow Template 数据库设计说明

- 表名：`workflow_template`，引擎 InnoDB，字符集 utf8mb4
- 主键：`id BIGINT`，使用 `IdType.ASSIGN_ID`（雪花算法）
- 唯一索引：`uk_workflow_template_key` (template_key)
- 普通索引：`idx_workflow_template_status`、`idx_workflow_template_category`、`idx_workflow_template_builtin`
- `built_in TINYINT`：1=内置模板，0=自定义模板（自定义模板暂未开放创建）
- `template_json JSON`：存储完整的策略定义（phases / steps / approvalGates），与 StrategyTemplate 结构一致
- `create_time` / `update_time`：自动填充时间戳

## 3. Seed 内置模板说明

| ID | templateKey | 名称 | 阶段数 | 步骤数 | 特点 |
|---|---|---|---|---|---|
| 900001 | STANDARD_DELIVERY | 标准交付流程 | 4 | 6 | 架构→后端/前端/测试并行→审查→总结 |
| 900002 | BACKEND_FOCUSED | 后端优先流程 | 4 | 5 | 架构→后端/测试并行→审查→总结 |
| 900003 | FRONTEND_FOCUSED | 前端优先流程 | 4 | 5 | 架构→前端/测试并行→审查→总结 |
| 900004 | REVIEW_ONLY | 审查流程 | 2 | 2 | 审查→总结（无审批闸门） |

所有内置模板初始状态均为 ENABLED，`built_in=1`。

## 4. WorkflowStrategyCatalogService 改造说明

**改造前：** 纯静态代码构建，`listStrategies()` / `resolveTemplate()` 均从 `BUILTIN_STRATEGIES` 静态列表读取。

**改造后：**
- `listStrategies()`：优先查询 DB 中 `status='ENABLED'` 的模板，通过 `toResponse(WorkflowTemplateEntity)` 解析为 `WorkflowStrategyResponse`（完整包含 phases/steps 数据）；若 DB 无数据则 fallback 到 `BUILTIN_STRATEGIES`
- `resolveTemplate(strategyKey)`：标准化 key（兼容 legacy "DEFAULT_MOCK" → "STANDARD_DELIVERY"），优先查 DB enabled 模板，fallback 到 built-in，都不存在则抛 BAD_REQUEST
- `isValidStrategy(strategyKey)`：优先查 DB enabled 模板 key 集合，fallback 到静态 VALID_KEYS
- 新增 `toResponse(WorkflowTemplateEntity)`：解析 template_json → phases/steps 完整 DTO（修复了之前只计数不填充 phases 数组的问题）
- 新增 `toStrategyTemplate(WorkflowTemplateEntity)`：解析 template_json → StrategyTemplate 领域对象（供 resolveTemplate 使用）

## 5. 后端模板管理 API 实现说明

| Method | Path | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/workflow-templates?status=` | 列表（可选按 status 过滤） | ADMIN |
| GET | `/api/workflow-templates/{templateId}` | 详情（含 strategy 解析） | ADMIN |
| PUT | `/api/workflow-templates/{templateId}/status` | 启用/停用（body: `{"status":"ENABLED\|DISABLED"}`） | ADMIN |

**权限控制：** 所有模板管理 API 通过 `requireAdmin()` 校验 ADMIN 角色，非 ADMIN 返回 FORBIDDEN。

**状态校验：** 仅接受 "ENABLED" / "DISABLED"，无效值返回 BAD_REQUEST。

**404 处理：** 不存在的 templateId 返回 NOT_FOUND。

**未认证访问：** 返回 UNAUTHORIZED。

## 6. 前端工作流模板页面实现说明

- **路由：** `/workflow-templates`，组件 `WorkflowTemplatePage.vue`
- **导航入口：** FloatingDock 中 "⧩ 工作流" 按钮，仅 ADMIN（`showObservability` 条件下）可见
- **列表功能：**
  - 表格列：模板名称、Key、状态（StatusPulse）、内置/自定义、阶段数、步骤数、更新时间、操作
  - 状态筛选 chips：全部 / 启用 / 停用
- **详情抽屉（el-drawer）：**
  - 基本信息（名称、Key、描述、状态、类型、分类、阶段/步骤数、时间）
  - 策略概览（strategyKey、name、description、phaseCount/stepCount）
  - 阶段与步骤明细（Phase 卡片 + Step 列表）
  - 审批闸门（gateKey、title、description、afterPhaseOrder）
  - Raw JSON（格式化显示 templateJson）
- **启用/停用操作：** GlowButton toggle，loading 状态，成功/失败 ElMessage 提示
- **空状态：** EmptyState 组件展示

## 7. MultiAgentRunPanel 策略下拉兼容说明

- `MultiAgentRunPanel.vue` 通过 `getMultiAgentStrategies()` 获取策略列表
- 策略列表由 `WorkflowStrategyCatalogService.listStrategies()` 提供（DB-first）
- 策略选项使用 `data-testid="strategy-option-{strategyKey}"` 标识
- 停用模板不会出现在策略下拉中
- Legacy "DEFAULT_MOCK" key 自动映射为 "STANDARD_DELIVERY"

## 8. 权限控制说明

- 模板管理 API（list / detail / updateStatus）：`requireAdmin()` → 非 ADMIN 返回 FORBIDDEN
- 策略列表 API（`/api/multi-agent-strategies`）：无需 ADMIN，所有已认证用户可访问（用于 MultiAgentRunPanel 下拉）
- 前端导航入口：`v-if="showObservability"` 控制显示（仅 ADMIN 看到）
- 未认证访问策略 API：返回 UNAUTHORIZED

## 9. 后端测试覆盖说明

### WorkflowTemplateIntegrationTest（12 tests，全部通过）

| # | 测试 | 说明 |
|---|---|---|
| 1 | shouldHaveFourBuiltInTemplatesAfterSeed | Seed 验证：4 个内置模板，状态 ENABLED，builtIn=true |
| 2 | shouldListMultiAgentStrategiesFromDatabase | 策略列表从 DB 读取：4 个策略含 STANDARD_DELIVERY |
| 3 | shouldFilterTemplatesByStatus | 按状态筛选：ENABLED=4，DISABLED=0 |
| 4 | shouldGetTemplateDetail | 模板详情：含 strategy、phases/steps 计数 |
| 5 | shouldRejectUnauthenticatedAccess | 未认证访问返回 UNAUTHORIZED |
| 6 | shouldRejectUnauthenticatedDetailAccess | 未认证详情访问返回 UNAUTHORIZED |
| 7 | shouldResolveLegacyDefaultMockToEnabledTemplate | Legacy DEFAULT_MOCK 映射为 STANDARD_DELIVERY |
| 8 | shouldReturnValidPhasesAndStepCountsInStrategies | 策略 phases 数据完整性 |
| 9 | shouldRejectInvalidStatusValue | 无效状态值 ARCHIVED → BAD_REQUEST |
| 10 | shouldReturnNotFoundForMissingTemplate | 不存在模板 → NOT_FOUND |
| 11 | shouldReturnNotFoundForTemplateStatusUpdateWithBadId | 不存在模板状态更新 → NOT_FOUND |
| 12 | shouldDisableTemplateAndHideFromStrategies | 停用→策略列表隐藏→启用→恢复（含 DISABLED 策略启动校验失败） |

### MultiAgentOrchestrationIntegrationTest（62 tests，全部通过）

### 全量后端测试：292 tests，1 预存失败（TaskStateMachineTest.shouldRejectTransitionToNull，与本次变更无关）

## 10. 前端 typecheck / build / E2E 结果

| 项 | 结果 |
|---|---|
| vue-tsc --noEmit | ✅ 通过（零错误） |
| vite build | ✅ 通过（WorkflowTemplatePage 成功打包） |
| E2E (8 tests) | ✅ 全部通过 |

### E2E 测试明细

| # | 测试 | 结果 |
|---|---|---|
| 1 | should display workflow templates nav entry for admin | ✅ |
| 2 | should open workflow templates page | ✅ |
| 3 | should show 4 built-in templates | ✅ |
| 4 | should open template detail drawer | ✅ |
| 5 | should disable BACKEND_FOCUSED and hide from strategy dropdown | ✅ |
| 6 | should re-enable BACKEND_FOCUSED and show in strategy dropdown | ✅ |
| 7 | should filter templates by status | ✅ |
| 8 | should not have JS errors on workflow template page | ✅ |

## 11. 手动验证结果

- [x] `GET /api/workflow-templates` 返回 4 个模板，全部 ENABLED
- [x] `GET /api/workflow-templates?status=DISABLED` 返回空数组
- [x] `GET /api/workflow-templates/900001` 返回 STANDARD_DELIVERY 详情含 strategy
- [x] `PUT /api/workflow-templates/900002/status` DISABLED → 策略列表变为 3 个
- [x] `PUT /api/workflow-templates/900002/status` ENABLED → 策略列表恢复 4 个
- [x] DISABLED 策略启动 Multi-Agent Run 返回 BAD_REQUEST
- [x] Legacy "DEFAULT_MOCK" 正常映射为 STANDARD_DELIVERY
- [x] 无效状态值 ARCHIVED 返回 BAD_REQUEST
- [x] 不存在的模板 999999 返回 NOT_FOUND
- [x] 未认证请求返回 UNAUTHORIZED
- [x] 前端模板管理页面正常渲染、筛选、详情抽屉、启用/停用
- [x] 前端策略下拉与模板状态同步

## 12. 已知限制

与设计文档一致，35F 完成后仍不包含：

- 自定义创建模板
- 编辑模板 JSON
- 模板版本管理
- 模板发布审批
- 拖拽工作流编辑器
- 条件分支 / 工具节点
- 团队级模板权限

## 13. 是否可以进入 Milestone 36A

✅ **可以。** Milestone 35F 全部质量门通过（后端 292 tests - 1 预存失败，typecheck 零错误，build 成功，E2E 8/8 通过）。建议下一步进入 36A（Safe Tool Execution Sandbox）或 35G（如有）。
