# Milestone 36B: Read-only Tool Catalog + Tool Policy

## 1. 背景

Milestone 36A 已完成 Safe Tool Execution Sandbox：

```text
Multi-Agent Step -> Mock Tool Execution -> Tool Sandbox Record -> Task Log -> Frontend Display
```

当前工具执行仍是代码内部直接根据 stepType 选择 mock tool。系统还没有：

- 可查询的工具目录
- 工具风险等级
- 项目级工具启用 / 禁用
- Agent / Step 级工具策略
- 执行前策略校验

Milestone 36B 的目标是新增 **Read-only Tool Catalog + Tool Policy**：

```text
Tool Catalog -> Project Tool Config -> Policy Check -> Sandbox Execution / BLOCKED
```

本阶段仍不执行真实 shell，不做 Git 写操作，不写真实代码文件。所有工具仍为 READ_ONLY / MOCK / ANALYSIS。

## 2. 总目标

实现可管理、可审计、默认安全的工具目录与策略控制：

1. 新增 Tool Catalog 表，保存内置工具定义。
2. 新增 Project Tool Config 表，保存项目级工具启用状态。
3. 新增 Tool Policy Service，在工具执行前做策略校验。
4. 36A 的 ToolSandboxExecutionService 执行前检查策略。
5. 未启用 / 高风险 / 不允许的工具记录为 BLOCKED，不执行 mock output。
6. 提供工具目录与项目工具配置 API。
7. 前端项目详情新增「工具」Tab。
8. 工具卡片显示类型、风险、启用状态、安全说明。
9. 补齐后端集成测试与前端 E2E。

完成后，系统从：

```text
Step 直接创建 Mock Tool Execution
```

升级为：

```text
Step -> Tool Catalog -> Project Policy -> Sandbox Execution or BLOCKED
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不写真实代码文件。
4. 不做 patch apply。
5. 不引入 Docker sandbox / Firecracker / Kubernetes Job。
6. 不接真实本地文件扫描器。
7. 不做工作流节点级工具拖拽配置。
8. 不做工具 marketplace。
9. 不做用户自定义工具注册。
10. 不破坏 36A tool_sandbox_execution API。
11. 不破坏 35A-35F Multi-Agent API。
12. 不绕过 Human Approval Gate。
13. 不绕过 ProjectPermissionService。
14. 前端保持中文暗色科技风 UI。

允许做：

- 内置工具 seed 到数据库。
- 项目级启用 / 禁用工具。
- 工具执行前做 policy check。
- BLOCKED 工具执行记录落库。
- 前端展示工具目录和项目策略。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V21__init_tool_catalog_policy_tables.sql
```

如果 V21 已存在，请顺延到下一个版本号。

### 4.1 tool_catalog

工具目录表。

```sql
CREATE TABLE IF NOT EXISTS tool_catalog (
    id BIGINT PRIMARY KEY,
    tool_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT NULL,
    tool_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    built_in TINYINT NOT NULL DEFAULT 1,
    policy_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_tool_catalog_key(tool_key),
    INDEX idx_tool_catalog_type(tool_type),
    INDEX idx_tool_catalog_risk(risk_level),
    INDEX idx_tool_catalog_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具目录表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| tool_key | 工具唯一 key，例如 PROJECT_CONTEXT_SCAN |
| name | 工具展示名称 |
| description | 工具说明 |
| tool_type | READ_ONLY / MOCK / ANALYSIS |
| risk_level | LOW / MEDIUM / HIGH / DANGEROUS |
| execution_mode | DRY_RUN / MOCK_EXECUTE |
| enabled | 全局是否启用 |
| built_in | 是否内置 |
| policy_json | 内置策略，例如允许 stepTypes |

### 4.2 project_tool_config

项目级工具配置表。

```sql
CREATE TABLE IF NOT EXISTS project_tool_config (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    tool_id BIGINT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    config_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_project_tool(project_id, tool_id),
    INDEX idx_project_tool_project(project_id),
    INDEX idx_project_tool_tool(tool_id),
    INDEX idx_project_tool_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工具配置表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| project_id | 项目 ID |
| tool_id | tool_catalog.id |
| enabled | 项目内是否启用 |
| config_json | 项目级配置，当前可为空 |

无物理外键，保持项目规范。

## 5. Seed 内置工具

迁移中 seed 5 个工具：

| id | toolKey | name | type | risk | mode | default |
|---|---|---|---|---|---|---|
| 910001 | PROJECT_CONTEXT_SCAN | 项目上下文扫描 | READ_ONLY | LOW | MOCK_EXECUTE | enabled |
| 910002 | TASK_REQUIREMENT_ANALYSIS | 任务需求分析 | ANALYSIS | LOW | MOCK_EXECUTE | enabled |
| 910003 | MOCK_FILE_INSPECTION | Mock 文件检查 | READ_ONLY | MEDIUM | MOCK_EXECUTE | enabled |
| 910004 | MOCK_TEST_PLAN_SCAN | Mock 测试计划扫描 | ANALYSIS | LOW | MOCK_EXECUTE | enabled |
| 910005 | MOCK_SECURITY_REVIEW | Mock 安全审查 | ANALYSIS | MEDIUM | MOCK_EXECUTE | enabled |

`policy_json` 示例：

```json
{
  "allowedStepTypes": ["ARCHITECTURE_ANALYSIS", "FINAL_SUMMARY"],
  "readOnly": true,
  "allowShell": false,
  "allowGitWrite": false,
  "allowFileWrite": false
}
```

注意：

- 不 seed DANGEROUS 工具。
- 不 seed WRITE 工具。
- 不 seed Git 写工具。

## 6. 枚举设计

新增或复用 36A 枚举：

### 6.1 ToolRiskLevel.java

```java
public enum ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    DANGEROUS
}
```

36B 必须拒绝：

- `HIGH`
- `DANGEROUS`

除非后续 36C 引入人工审批，本阶段不允许执行。

### 6.2 ProjectToolStatus

不需要新增枚举，可使用 enabled TINYINT。

## 7. Entity / Mapper / DTO

### 7.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolCatalogEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/ProjectToolConfigEntity.java
```

要求：

- MyBatis-Plus 注解
- `@TableId(type = IdType.ASSIGN_ID)`
- createTime / updateTime 自动填充
- 不使用 Lombok
- 手写 getter/setter

### 7.2 Mapper

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolCatalogMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ProjectToolConfigMapper.java
```

### 7.3 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolCatalogResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/ProjectToolConfigResponse.java
backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateProjectToolConfigRequest.java
```

`ToolCatalogResponse` 字段：

- id
- toolKey
- name
- description
- toolType
- riskLevel
- executionMode
- enabled
- builtIn
- policyJson
- createTime
- updateTime

`ProjectToolConfigResponse` 字段：

- id
- projectId
- toolId
- toolKey
- name
- description
- toolType
- riskLevel
- executionMode
- globalEnabled
- projectEnabled
- configJson
- createTime
- updateTime

`UpdateProjectToolConfigRequest` 字段：

- enabled Boolean
- config Map<String, Object>

## 8. 后端服务设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolCatalogApplicationService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolPolicyService.java
```

### 8.1 ToolCatalogApplicationService

方法：

```java
public List<ToolCatalogResponse> listTools(String toolType, String riskLevel, Boolean enabled)
public List<ProjectToolConfigResponse> listProjectTools(Long projectId)
public ProjectToolConfigResponse enableProjectTool(Long projectId, Long toolId, UpdateProjectToolConfigRequest request)
public ProjectToolConfigResponse disableProjectTool(Long projectId, Long toolId)
```

权限：

| 操作 | 权限 |
|---|---|
| listTools | 登录用户 |
| listProjectTools | VIEWER+ |
| enableProjectTool | OWNER |
| disableProjectTool | OWNER |

### 8.2 ToolPolicyService

方法：

```java
public ToolPolicyDecision checkToolAllowed(
    Long projectId,
    String toolKey,
    String stepType,
    Long agentId
)
```

`ToolPolicyDecision` 可作为内部 static class：

字段：

- allowed boolean
- blockedReason String
- toolCatalog ToolCatalogEntity
- projectConfig ProjectToolConfigEntity

策略规则：

1. toolKey 不存在 → blocked。
2. tool 全局 disabled → blocked。
3. riskLevel HIGH / DANGEROUS → blocked。
4. project_tool_config 不存在 → blocked。
5. project_tool_config.enabled != 1 → blocked。
6. policy_json.allowedStepTypes 不包含 stepType → blocked。
7. policy_json.allowShell=true → blocked。
8. policy_json.allowGitWrite=true → blocked。
9. policy_json.allowFileWrite=true → blocked。
10. 其余情况 allowed。

blockedReason 示例：

- `工具未在项目中启用`
- `工具风险等级过高`
- `工具不允许用于当前步骤`
- `工具策略包含禁止的 shell 权限`

## 9. ToolSandboxExecutionService 改造

修改 36A 服务：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java
```

执行前新增策略校验：

```text
resolve toolKey from stepType
  -> ToolPolicyService.checkToolAllowed(...)
  -> allowed: create COMPLETED mock execution
  -> blocked: create BLOCKED execution with errorMessage / summary
```

BLOCKED 记录要求：

- status = BLOCKED
- executionMode = DRY_RUN 或 MOCK_EXECUTE
- outputPayload 包含：

```json
{
  "mock": true,
  "blocked": true,
  "reason": "工具未在项目中启用",
  "filesTouched": [],
  "gitOperations": []
}
```

任务日志：

```text
stage: TOOL_SANDBOX_BLOCKED
message: 工具 PROJECT_CONTEXT_SCAN 被策略阻止：工具未在项目中启用
```

允许时仍写：

```text
stage: TOOL_SANDBOX_EXECUTED
```

## 10. 默认项目工具配置策略

重要：36B 引入 project_tool_config 后，如果默认所有项目都没有工具配置，那么已有 E2E 会看到所有工具 BLOCKED。

本阶段推荐策略：

### 10.1 新项目默认启用 LOW 风险工具

在项目创建时不强制改 ProjectApplicationService，以避免影响已验证项目逻辑。

推荐在 `ToolPolicyService` 中采用温和默认策略：

- 如果 project_tool_config 不存在：
  - LOW 风险工具默认 allowed
  - MEDIUM 风险工具默认 blocked
  - HIGH / DANGEROUS blocked

同时 `listProjectTools(projectId)` 返回：

- LOW 工具 projectEnabled=true, config row may be null
- MEDIUM 工具 projectEnabled=false

### 10.2 Owner 显式配置后优先使用配置

如果存在 project_tool_config：

- enabled=1 → allowed（仍需通过风险和 policy_json）
- enabled=0 → blocked

这样可以兼容旧项目和旧测试，同时让项目 Owner 可显式关闭工具。

## 11. 后端 API

新增 Controller：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolCatalogController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/tool-catalog?toolType=&riskLevel=&enabled=` | 登录用户 | 查询全局工具目录 |
| GET | `/api/projects/{projectId}/tools` | VIEWER+ | 查询项目工具配置 |
| POST | `/api/projects/{projectId}/tools/{toolId}/enable` | OWNER | 项目启用工具 |
| POST | `/api/projects/{projectId}/tools/{toolId}/disable` | OWNER | 项目停用工具 |

返回示例：

```json
{
  "code": "OK",
  "data": [
    {
      "toolId": "910001",
      "toolKey": "PROJECT_CONTEXT_SCAN",
      "name": "项目上下文扫描",
      "toolType": "READ_ONLY",
      "riskLevel": "LOW",
      "globalEnabled": true,
      "projectEnabled": true
    }
  ]
}
```

## 12. 前端 API

修改：

```text
frontend/src/modules/task/api.ts
```

或新增：

```text
frontend/src/modules/tool/api.ts
```

推荐新增模块：

```text
frontend/src/modules/tool/api.ts
```

类型：

```ts
export interface ToolCatalog {
  id: string
  toolKey: string
  name: string
  description: string | null
  toolType: string
  riskLevel: string
  executionMode: string
  enabled: boolean
  builtIn: boolean
  policyJson: string | null
}

export interface ProjectToolConfig {
  id: string | null
  projectId: string
  toolId: string
  toolKey: string
  name: string
  description: string | null
  toolType: string
  riskLevel: string
  executionMode: string
  globalEnabled: boolean
  projectEnabled: boolean
  configJson: string | null
}
```

函数：

```ts
export function listToolCatalog(params?: { toolType?: string; riskLevel?: string; enabled?: boolean })
export function listProjectTools(projectId: string)
export function enableProjectTool(projectId: string, toolId: string)
export function disableProjectTool(projectId: string, toolId: string)
```

## 13. 前端页面

新增：

```text
frontend/src/modules/tool/pages/ProjectToolConfigPage.vue
```

并修改：

```text
frontend/src/app/router/index.ts
frontend/src/modules/project/pages/ProjectDetailPage.vue
```

在项目详情 SectionRail 中新增：

```text
工具
```

路由：

```text
/projects/:projectId/tools
```

页面内容：

1. 顶部说明：

```text
工具策略控制 Agent 可调用的安全工具。本阶段仅支持 Mock / Read-only 工具，不执行真实 Shell 或 Git 写操作。
```

2. 工具表格：

列：

- 工具名称
- toolKey
- 类型
- 风险等级
- 执行模式
- 全局状态
- 项目启用
- 说明
- 操作

3. 风险等级展示：

| risk | UI |
|---|---|
| LOW | 绿色 |
| MEDIUM | 黄色 |
| HIGH | 红色 |
| DANGEROUS | 红色 + 禁用操作 |

4. 操作：

- 启用
- 停用

5. 权限：

- VIEWER 可查看。
- 非 OWNER 点击启用/停用时后端返回 PROJECT_ACCESS_DENIED，前端显示 el-alert。

建议 data-testid：

- `project-tool-page`
- `project-tool-table`
- `project-tool-row`
- `btn-tool-enable`
- `btn-tool-disable`
- `tool-policy-error`

## 14. MultiAgentRunPanel 展示增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

如果 tool execution status = BLOCKED：

- 工具卡片使用 warning / danger 样式。
- 显示 blocked reason。
- Summary 统计增加：

```text
工具执行 3 / 阻止 1
```

要求：

- BLOCKED 不应被显示为系统错误。
- BLOCKED 是安全策略命中的正常状态。

## 15. 后端测试

新增或修改：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolCatalogPolicyIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/MultiAgentOrchestrationIntegrationTest.java
```

测试不少于 16 个：

### Tool Catalog / Project Config

1. tool_catalog seed 后返回 5 个工具。
2. GET /api/tool-catalog 登录用户可访问。
3. 未登录访问 /api/tool-catalog 返回 UNAUTHORIZED。
4. VIEWER+ 可查询项目工具配置。
5. OWNER 可启用项目工具。
6. OWNER 可停用项目工具。
7. 非 OWNER 启用工具返回 PROJECT_ACCESS_DENIED。
8. 无效 toolId 返回 NOT_FOUND。
9. listProjectTools 对 LOW 工具默认 projectEnabled=true。
10. listProjectTools 对 MEDIUM 工具默认 projectEnabled=false。

### Policy / Sandbox

11. LOW 默认工具 allowed，生成 COMPLETED execution。
12. MEDIUM 默认工具 blocked，生成 BLOCKED execution。
13. OWNER 启用 MEDIUM 工具后生成 COMPLETED execution。
14. OWNER 停用 LOW 工具后生成 BLOCKED execution。
15. BLOCKED execution outputPayload 包含 blocked=true / filesTouched=[] / gitOperations=[]。
16. task logs 包含 TOOL_SANDBOX_BLOCKED。
17. policy_json 禁止 stepType 时 blocked。
18. HIGH / DANGEROUS 工具始终 blocked。

全量后端质量门：

```bash
cd backend
mvn test
```

## 16. 前端 E2E

新增：

```text
frontend/e2e/project-tool-policy.spec.ts
```

测试：

1. 项目详情可打开「工具」Tab。
2. 工具表格显示 5 个内置工具。
3. LOW 工具默认启用。
4. MEDIUM 工具默认未启用。
5. Owner 可启用 / 停用工具。
6. Multi-Agent Run 中 BLOCKED 工具显示为策略阻止，而不是 JS error。
7. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 17. 文档与报告

完成后新增：

```text
docs/milestone-36b-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Catalog 设计说明
4. Project Tool Config 设计说明
5. Tool Policy 规则说明
6. ToolSandboxExecutionService 集成说明
7. 后端 API 清单
8. 前端工具 Tab 说明
9. Multi-Agent BLOCKED 展示说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 36C

## 18. 验收标准

必须满足：

- tool_catalog 表存在。
- project_tool_config 表存在。
- Seed 5 个内置工具。
- `/api/tool-catalog` 可查询。
- `/api/projects/{projectId}/tools` 可查询项目工具策略。
- Owner 可启用 / 停用项目工具。
- 非 Owner 不可修改项目工具策略。
- ToolSandboxExecutionService 执行前检查 ToolPolicy。
- 被阻止的工具生成 BLOCKED execution。
- BLOCKED outputPayload 包含：
  - `mock=true`
  - `blocked=true`
  - `filesTouched=[]`
  - `gitOperations=[]`
- 前端项目工具 Tab 可查看和配置工具。
- Multi-Agent 工具卡片可展示 BLOCKED 状态。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 19. 已知非目标

本阶段不做：

- 用户自定义工具注册
- 工具参数 schema 编辑器
- 工具 marketplace
- 真实 shell executor
- 真实 Git executor
- 文件写入工具
- Patch Proposal Artifact
- 工具调用审批流
- 异步 Worker
- 多租户工具权限

这些可进入后续：

- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue

## 20. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36B。

文档路径：
docs/milestone-36b-read-only-tool-catalog-policy.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A 的 Safe Tool Execution Sandbox 基础上，新增 Read-only Tool Catalog + Tool Policy。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要写真实代码文件。
6. 所有工具仍必须是 Mock / Dry-run / Read-only / Analysis。
7. 不要破坏 36A tool_sandbox_execution API。
8. 不要破坏 35A-35F Multi-Agent Run / Phase / Step / Message / Approval Gate / Workflow Template API。
9. 不要绕过 Human Approval Gate。
10. 不要绕过 ProjectPermissionService。
11. 不要改 Auth、Project、Member、Repository、Chat、RAG、Model Gateway 已验证逻辑，除非本模块必须依赖。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V21 tool_catalog / project_tool_config migration，并 seed 5 个内置工具。
2. 新增 ToolRiskLevel 枚举。
3. 新增 ToolCatalogEntity / ProjectToolConfigEntity / Mapper / DTO。
4. 新增 ToolCatalogApplicationService。
5. 新增 ToolPolicyService。
6. ToolSandboxExecutionService 执行前接入 ToolPolicyService。
7. 被策略阻止的工具生成 BLOCKED execution，不抛业务异常。
8. 新增 API：
   - GET /api/tool-catalog
   - GET /api/projects/{projectId}/tools
   - POST /api/projects/{projectId}/tools/{toolId}/enable
   - POST /api/projects/{projectId}/tools/{toolId}/disable
9. 前端新增 tool/api.ts。
10. 前端新增 ProjectToolConfigPage.vue。
11. 项目详情 SectionRail 新增「工具」Tab，路由 /projects/:projectId/tools。
12. MultiAgentRunPanel 增强 BLOCKED 工具展示。
13. 后端测试不少于 16 个。
14. 前端 E2E 覆盖工具 Tab 和 BLOCKED 展示。
15. 新增 docs/milestone-36b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 数据库表说明
3. Tool Catalog 设计说明
4. Project Tool Config 设计说明
5. Tool Policy 规则说明
6. ToolSandboxExecutionService 集成说明
7. 后端 API 清单
8. 前端工具 Tab 说明
9. Multi-Agent BLOCKED 展示说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 36C

现在开始实现，不要只给计划。
```
