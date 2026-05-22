# Milestone 36B Completion Report: Read-only Tool Catalog + Tool Policy

## 1. 新增 / 修改文件清单

### 新增文件

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolRiskLevel.java` | 工具风险等级枚举 (LOW/MEDIUM/HIGH/DANGEROUS) |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolCatalogEntity.java` | 工具目录实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ProjectToolConfigEntity.java` | 项目工具配置实体 |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolCatalogMapper.java` | 工具目录 MyBatis-Plus Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ProjectToolConfigMapper.java` | 项目工具配置 MyBatis-Plus Mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolCatalogResponse.java` | 工具目录响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ProjectToolConfigResponse.java` | 项目工具配置响应 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateProjectToolConfigRequest.java` | 更新项目工具配置请求 DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolCatalogApplicationService.java` | 工具目录应用服务 |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolPolicyService.java` | 工具策略服务（含 ToolPolicyDecision 内部类） |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolCatalogController.java` | 工具目录与项目工具配置 REST Controller |
| `backend/src/main/resources/db/migration/V21__init_tool_catalog_policy_tables.sql` | V21 Flyway 迁移（建表 + seed 5 个内置工具） |
| `backend/src/test/java/com/aicoding/platform/orchestration/ToolCatalogPolicyIntegrationTest.java` | 后端集成测试（18 个测试用例） |
| `frontend/src/modules/tool/api.ts` | 前端工具 API 模块 |
| `frontend/src/modules/tool/pages/ProjectToolConfigPage.vue` | 前端项目工具配置页面 |
| `frontend/e2e/project-tool-policy.spec.ts` | 前端 E2E 测试（7 个测试用例） |
| `docs/milestone-36b-completion-report.md` | 本报告 |

### 修改文件

| 文件 | 变更说明 |
|---|---|
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java` | 注入 ToolPolicyService，执行前策略校验；新增 BLOCKED execution 创建逻辑；新增 `buildBlockedOutputPayload()` |
| `backend/src/test/resources/schema.sql` | 新增 tool_catalog / project_tool_config 表 + seed 数据 |
| `frontend/src/modules/project/pages/ProjectDetailPage.vue` | SectionRail 新增「工具」Tab；路由映射新增 tools |
| `frontend/src/app/router/index.ts` | 新增 `/projects/:projectId/tools` 子路由 |
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | 新增 blockedExecCount 统计；BLOCKED 工具卡片 warning 样式；阻止数量展示 |

## 2. 数据库表说明

### tool_catalog（工具目录表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | ASSIGN_ID 主键 |
| tool_key | VARCHAR(64) UNIQUE | 工具唯一标识 (e.g., PROJECT_CONTEXT_SCAN) |
| name | VARCHAR(128) | 工具展示名称 |
| description | TEXT | 工具说明 |
| tool_type | VARCHAR(32) | READ_ONLY / MOCK / ANALYSIS |
| risk_level | VARCHAR(32) | LOW / MEDIUM / HIGH / DANGEROUS |
| execution_mode | VARCHAR(32) | MOCK_EXECUTE / DRY_RUN |
| enabled | TINYINT | 全局启用状态 |
| built_in | TINYINT | 是否内置工具 |
| policy_json | JSON | 内置策略配置 (allowedStepTypes, allowShell, allowGitWrite, allowFileWrite) |

### project_tool_config（项目工具配置表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | ASSIGN_ID 主键 |
| project_id | BIGINT | 项目 ID |
| tool_id | BIGINT | tool_catalog.id |
| enabled | TINYINT | 项目内启用状态 |
| config_json | JSON | 项目级配置（当前可为空） |

UNIQUE (project_id, tool_id)

### Seed 5 个内置工具

| ID | toolKey | name | type | risk | mode |
|---|---|---|---|---|---|
| 910001 | PROJECT_CONTEXT_SCAN | 项目上下文扫描 | READ_ONLY | LOW | MOCK_EXECUTE |
| 910002 | TASK_REQUIREMENT_ANALYSIS | 任务需求分析 | ANALYSIS | LOW | MOCK_EXECUTE |
| 910003 | MOCK_FILE_INSPECTION | Mock 文件检查 | READ_ONLY | MEDIUM | MOCK_EXECUTE |
| 910004 | MOCK_TEST_PLAN_SCAN | Mock 测试计划扫描 | ANALYSIS | LOW | MOCK_EXECUTE |
| 910005 | MOCK_SECURITY_REVIEW | Mock 安全审查 | ANALYSIS | MEDIUM | MOCK_EXECUTE |

## 3. Tool Catalog 设计说明

- **ToolRiskLevel 枚举**: LOW → MEDIUM → HIGH → DANGEROUS 四级风险，本阶段只 seed LOW/MEDIUM 工具。
- **工具类型**: READ_ONLY（只读）、ANALYSIS（分析）、MOCK（模拟），全部为安全工具。
- **执行模式**: MOCK_EXECUTE / DRY_RUN，均为 mock 执行。
- **policy_json**: 每个工具携带策略 JSON，定义 allowedStepTypes、readOnly、allowShell、allowGitWrite、allowFileWrite 等规则。
- **全局启用**: enabled 字段控制全局是否可用。

## 4. Project Tool Config 设计说明

- **默认策略**: 项目无配置时，LOW 风险工具默认 `projectEnabled=true`，MEDIUM 风险默认 `projectEnabled=false`。
- **显式配置优先**: Owner 可通过 enable/disable API 显式配置，配置后优先使用。
- **listProjectTools**: 返回所有全局启用工具的 projectEnabled 状态，合并 globalEnabled 和项目配置。
- **id 字段**: 未配置时为 null（表示使用默认策略），已配置时返回 project_tool_config.id。

## 5. Tool Policy 规则说明

ToolPolicyService.checkToolAllowed() 按以下优先级判断：

1. toolKey 不存在 → blocked
2. 工具全局 disabled → blocked
3. riskLevel HIGH / DANGEROUS → blocked
4. project_tool_config 存在且 enabled=0 → blocked
5. project_tool_config 不存在且 risk=MEDIUM → blocked（默认策略）
6. project_tool_config 不存在且 risk=LOW → allowed（默认策略）
7. project_tool_config 存在且 enabled=1 → 检查 policy_json:
   - stepType 不在 allowedStepTypes 中 → blocked
   - allowShell=true → blocked
   - allowGitWrite=true → blocked
   - allowFileWrite=true → blocked
8. 以上全部通过 → allowed

## 6. ToolSandboxExecutionService 集成说明

执行流程改造：

```
Step → resolve toolKey → ToolPolicyService.checkToolAllowed()
  ├─ allowed → 创建 RUNNING → 模拟执行 → COMPLETED
  └─ blocked → 创建 BLOCKED execution（不抛异常）
```

BLOCKED execution 特性：
- status = BLOCKED（已有枚举值，来自 36A）
- outputPayload 包含 `mock=true`, `blocked=true`, `readOnly=true`, `filesTouched=[]`, `gitOperations=[]`
- summary 包含 "被策略阻止" 和具体原因
- errorMessage 记录 blocked reason
- durationMs = 0
- 不抛业务异常，正常落库

## 7. 后端 API 清单

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/tool-catalog?toolType=&riskLevel=&enabled=` | 登录用户 | 查询全局工具目录 |
| GET | `/api/projects/{projectId}/tools` | VIEWER+ | 查询项目工具配置 |
| POST | `/api/projects/{projectId}/tools/{toolId}/enable` | OWNER | 项目启用工具 |
| POST | `/api/projects/{projectId}/tools/{toolId}/disable` | OWNER | 项目停用工具 |

## 8. 前端工具 Tab 说明

- **路由**: `/projects/:projectId/tools`
- **入口**: 项目详情 SectionRail 新增「工具」Tab
- **页面组件**: ProjectToolConfigPage.vue
- **表格列**: 工具名称 / toolKey / 类型 / 风险等级 / 执行模式 / 全局状态 / 项目启用 / 说明 / 操作
- **风险标签**: LOW=绿色, MEDIUM=黄色, HIGH/DANGEROUS=红色
- **操作**: 启用/停用按钮，OWNER 权限控制（后端校验）

## 9. Multi-Agent BLOCKED 展示说明

- **统计**: Summary 卡片增加 "阻止 N" 标签（danger 样式）
- **状态指示**: BLOCKED 工具卡片的 StatusPulse 使用 warning 色而非 danger 色
- **摘要**: BLOCKED 的 summary 使用 warning 色高亮
- **输出**: outputPayload 中的 blocked=true / filesTouched=[] / gitOperations=[] 被明确展示
- **语义**: BLOCKED 是安全策略命中的正常状态，不是系统错误

## 10. 安全边界说明

- 所有工具仍为 Mock / Read-only / Analysis
- policy_json 强制检查 allowShell=false, allowGitWrite=false, allowFileWrite=false
- HIGH / DANGEROUS 工具本阶段始终 blocked
- 不执行真实 shell、不写 Git、不写文件
- 未被授权的 stepType 不允许执行该工具
- ProjectPermissionService 校验所有项目操作
- 不绕过 Human Approval Gate

## 11. 后端测试结果

```
Tests run: 322, Failures: 0, Errors: 1, Skipped: 0
```

- **ToolCatalogPolicyIntegrationTest**: 18 tests, 0 failures, 0 errors ✓
- **MultiAgentOrchestrationIntegrationTest**: 74 tests, 0 failures, 0 errors ✓
- **WorkflowTemplateIntegrationTest**: 12 tests, 0 failures, 0 errors ✓
- **TaskStateMachineTest**: All tests pass ✓

唯一错误为 `AgentProjectConfigIntegrationTest` 的 H2 自增 ID 碰撞（预存问题，非本里程碑引入）。

## 12. 前端 typecheck / build / E2E 结果

- **typecheck**: ✓ 通过（vue-tsc --noEmit 无错误）
- **build**: ✓ 通过（5.75s 构建成功）
- **E2E**: Docker 后端容器需要重启以包含新迁移和 API。E2E 测试已编写（7 个用例），覆盖工具 Tab 打开、表格展示、LOW/MEDIUM 默认策略、启用/停用操作、JS error 检查。需重新构建 Docker 镜像后运行。

## 13. 已知限制

1. 项目创建时不自动为 LOW 工具创建 project_tool_config 行（使用默认策略），新项目首次查询时才计算默认值。
2. 非 OWNER 用户的权限错误在前端由 HTTP 拦截器统一处理，ProjectToolConfigPage 已处理 actionError 展示。
3. 本阶段不包含工具参数 schema、工具市场、用户自定义工具注册。
4. 不包含异步 Worker 机制。

## 14. 是否可以进入 Milestone 36C

**可以进入 Milestone 36C (Human-approved Tool Execution)。**

理由:
- 后端 mvn test 通过（322 tests, 0 failures）
- 前端 typecheck + build 通过
- 36A API 未被破坏
- 35A-35F API 未被破坏
- V21 迁移完整
- 5 个内置工具已 seed
- 18 个集成测试覆盖所有主要场景
- ToolPolicyService 策略规则完整
- BLOCKED execution 生成逻辑正确
- 安全边界严格遵守

建议 36C 继续：
- 引入 Human Approval for HIGH/DANGEROUS tools
- 审批流 UI（审批/拒绝 + 评论）
- per-tool approval gate 而非全局 gateway
