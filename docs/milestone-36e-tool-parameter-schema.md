# Milestone 36E: Tool Parameter Schema

## 1. 背景

Milestone 36A-36D 已完成安全工具执行闭环：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact

当前工具已经具备：

```text
Tool Catalog -> Project Tool Config -> Policy Check -> Approval -> Mock Execute -> Artifact
```

但工具输入仍不够结构化，主要依赖系统自动生成的 inputPayload。项目 Owner 还不能为工具配置参数，后端也没有基于 schema 对工具参数做统一校验。

Milestone 36E 的目标是新增 **Tool Parameter Schema**：

```text
Tool Catalog parameter_schema_json
  -> Project Tool Config parameters
  -> Backend validation
  -> Tool Execution inputPayload
  -> Frontend dynamic form
```

本阶段仍不执行真实 shell，不做 Git 写操作，不写真实代码文件，不应用补丁。参数只影响 Mock 输出和审计记录。

## 2. 总目标

实现工具参数 schema 和项目级参数配置能力：

1. Tool Catalog 支持 `parameter_schema_json`。
2. Project Tool Config 支持保存 `parameters_json`。
3. 后端基于 schema 校验项目工具参数。
4. ToolSandboxExecutionService 将参数写入 inputPayload。
5. Mock 输出体现关键参数。
6. 前端 Project Tool 页面提供参数配置表单。
7. `MOCK_PATCH_PROPOSAL` 支持参数影响 Patch Proposal Artifact 内容。
8. 补齐后端集成测试和前端 E2E。

完成后，工具从：

```text
固定 Mock 输入
```

升级为：

```text
Schema-driven 参数配置 + 校验 + 执行记录
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不写真实代码文件。
4. 不执行真实文件扫描。
5. 不应用 patch。
6. 不做 Monaco / JSON Schema 高级编辑器。
7. 不引入第三方 JSON Schema validator 依赖，除非项目已有。
8. 不做复杂嵌套 schema。
9. 不做用户自定义工具创建。
10. 不破坏 36A-36D 已有 API。
11. 不破坏 Project Tool 启用 / 停用能力。
12. 不绕过审批流程。
13. 前端保持中文暗色科技风 UI。

允许做：

- 简化版 schema 结构。
- 后端手写参数校验。
- 前端根据 schema 渲染基础表单。
- 参数保存到 project_tool_config。
- inputPayload / artifact content 引用参数。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V24__add_tool_parameter_schema.sql
```

如果 V24 已存在，请顺延。

### 4.1 tool_catalog 新增字段

```sql
ALTER TABLE tool_catalog
    ADD COLUMN parameter_schema_json JSON NULL AFTER policy_json;
```

### 4.2 project_tool_config 新增字段

如果当前只有 `config_json`，可以复用 `config_json` 保存参数，不强制新增字段。

推荐新增更明确字段：

```sql
ALTER TABLE project_tool_config
    ADD COLUMN parameters_json JSON NULL AFTER config_json;
```

如果担心兼容性，可只使用现有 `config_json`，但 DTO 仍对外命名为 `parameters`。

## 5. Schema 设计

不使用完整 JSON Schema，采用项目内简化 schema：

```json
{
  "fields": [
    {
      "key": "scope",
      "label": "分析范围",
      "type": "select",
      "required": true,
      "defaultValue": "TASK",
      "options": ["TASK", "PROJECT", "CURRENT_PHASE"]
    },
    {
      "key": "includeTests",
      "label": "包含测试建议",
      "type": "boolean",
      "required": false,
      "defaultValue": true
    },
    {
      "key": "maxFindings",
      "label": "最大建议数",
      "type": "number",
      "required": true,
      "defaultValue": 5,
      "min": 1,
      "max": 20
    },
    {
      "key": "targetArea",
      "label": "目标区域",
      "type": "text",
      "required": false,
      "defaultValue": ""
    }
  ]
}
```

支持字段类型：

| type | 前端控件 | 后端校验 |
|---|---|---|
| text | el-input | string, maxLength |
| textarea | el-input textarea | string, maxLength |
| boolean | el-switch | boolean |
| number | el-input-number | min / max |
| select | el-select | value in options |

不支持：

- object
- array
- nested fields
- conditional schema
- remote options

## 6. Seed 参数 schema

为 6 个内置工具补齐 schema：

### 6.1 PROJECT_CONTEXT_SCAN

```json
{
  "fields": [
    {"key":"scope","label":"扫描范围","type":"select","required":true,"defaultValue":"TASK","options":["TASK","PROJECT","CURRENT_PHASE"]},
    {"key":"includeMetadata","label":"包含元数据","type":"boolean","required":false,"defaultValue":true}
  ]
}
```

### 6.2 TASK_REQUIREMENT_ANALYSIS

```json
{
  "fields": [
    {"key":"depth","label":"分析深度","type":"select","required":true,"defaultValue":"STANDARD","options":["BASIC","STANDARD","DETAILED"]},
    {"key":"maxFindings","label":"最大建议数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}
  ]
}
```

### 6.3 MOCK_FILE_INSPECTION

```json
{
  "fields": [
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"includeStyleHints","label":"包含样式建议","type":"boolean","required":false,"defaultValue":true}
  ]
}
```

### 6.4 MOCK_TEST_PLAN_SCAN

```json
{
  "fields": [
    {"key":"includeEdgeCases","label":"包含边界用例","type":"boolean","required":false,"defaultValue":true},
    {"key":"testLevel","label":"测试级别","type":"select","required":true,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"]}
  ]
}
```

### 6.5 MOCK_SECURITY_REVIEW

```json
{
  "fields": [
    {"key":"riskFocus","label":"风险重点","type":"select","required":true,"defaultValue":"STANDARD","options":["STANDARD","AUTH","DATA","DEPENDENCY"]},
    {"key":"maxFindings","label":"最大风险数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}
  ]
}
```

### 6.6 MOCK_PATCH_PROPOSAL

```json
{
  "fields": [
    {"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},
    {"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},
    {"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128}
  ]
}
```

## 7. Entity / DTO 变更

修改：

```text
ToolCatalogEntity.java
ProjectToolConfigEntity.java
ToolCatalogResponse.java
ProjectToolConfigResponse.java
UpdateProjectToolConfigRequest.java
ToolSandboxExecutionResponse.java
```

新增字段：

### ToolCatalogEntity / Response

- parameterSchemaJson String

### ProjectToolConfigEntity / Response

- parametersJson String
- parameters Map<String, Object> 或 String

建议 Response 对外：

```java
private String parameterSchemaJson;
private String parametersJson;
```

前端可 JSON.parse。

### UpdateProjectToolConfigRequest

新增：

```java
private Map<String, Object> parameters;
```

保留原有 enabled / config。

## 8. 后端参数校验服务

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ToolParameterSchemaService.java
```

职责：

```java
public Map<String, Object> normalizeAndValidate(String schemaJson, Map<String, Object> parameters)
```

行为：

1. schemaJson 为空 → 返回空 Map。
2. 对每个 field：
   - 如果 parameters 没传，使用 defaultValue。
   - required 且无值 / 空字符串 → BAD_REQUEST。
   - type 校验。
   - number 校验 min / max。
   - select 校验 options。
   - text / textarea 校验 maxLength。
3. 丢弃 schema 未定义的额外参数。
4. 返回 normalized parameters。

错误示例：

- `工具参数 scope 不能为空`
- `工具参数 maxFindings 超出范围`
- `工具参数 testLevel 不在允许选项中`

## 9. ToolCatalogApplicationService 改造

在启用项目工具时：

```text
enableProjectTool(projectId, toolId, request)
  -> load tool
  -> normalizeAndValidate(tool.parameterSchemaJson, request.parameters)
  -> save parametersJson
```

停用工具时：

- 不删除 parametersJson。
- 保留配置，方便再次启用。

listProjectTools：

- 返回 parameterSchemaJson。
- 返回 parametersJson。
- 如果 project config 不存在，返回默认参数（可选），建议返回 normalized default parameters。

## 10. ToolSandboxExecutionService 改造

执行工具时：

1. 获取 ToolCatalog。
2. 获取 ProjectToolConfig。
3. 使用 schema normalize parameters。
4. 将参数写入 inputPayload：

```json
{
  "stepType": "CODE_REVIEW",
  "toolKey": "MOCK_SECURITY_REVIEW",
  "parameters": {
    "riskFocus": "AUTH",
    "maxFindings": 5
  }
}
```

5. outputPayload 中回显关键参数：

```json
{
  "mock": true,
  "readOnly": true,
  "parametersApplied": true,
  "parameterSummary": "riskFocus=AUTH, maxFindings=5",
  "filesTouched": [],
  "gitOperations": []
}
```

## 11. Patch Proposal Artifact 集成

`MOCK_PATCH_PROPOSAL` 生成 artifact 时读取 parameters：

| parameter | artifact 影响 |
|---|---|
| proposalScope | Summary 显示提案范围 |
| includeTests | true 时 Review Checklist 包含测试项 |
| maxChangedFiles | Mock diff 中最多展示 N 个文件段 |
| targetArea | Summary 显示目标区域 |

仍然不生成真实 patch，不写文件。

Artifact safety block 必须保留：

```text
applied: false
filesTouched: []
gitOperations: []
```

## 12. 后端 API

不新增 endpoint。

复用：

| Method | Endpoint | 变更 |
|---|---|---|
| GET | `/api/tool-catalog` | 返回 parameterSchemaJson |
| GET | `/api/projects/{projectId}/tools` | 返回 parameterSchemaJson / parametersJson |
| POST | `/api/projects/{projectId}/tools/{toolId}/enable` | body 支持 parameters |
| POST | `/api/projects/{projectId}/tools/{toolId}/disable` | 保留 parameters |

请求示例：

```json
{
  "enabled": true,
  "parameters": {
    "proposalScope": "STANDARD",
    "includeTests": true,
    "maxChangedFiles": 3,
    "targetArea": "backend service"
  }
}
```

## 13. 前端 API

修改：

```text
frontend/src/modules/tool/api.ts
```

类型：

```ts
export interface ToolParameterField {
  key: string
  label: string
  type: 'text' | 'textarea' | 'boolean' | 'number' | 'select'
  required?: boolean
  defaultValue?: string | number | boolean
  options?: string[]
  min?: number
  max?: number
  maxLength?: number
}

export interface ToolParameterSchema {
  fields: ToolParameterField[]
}
```

更新：

```ts
parameterSchemaJson?: string | null
parametersJson?: string | null
```

`enableProjectTool` 支持：

```ts
parameters?: Record<string, unknown>
```

## 14. 前端参数表单组件

新增：

```text
frontend/src/modules/tool/components/ToolParameterForm.vue
```

Props：

```ts
schemaJson: string | null
modelValue: Record<string, unknown>
```

Emits：

```ts
update:modelValue
```

支持控件：

- text → el-input
- textarea → el-input type="textarea"
- boolean → el-switch
- number → el-input-number
- select → el-select

要求：

- 根据 defaultValue 初始化。
- 前端做基础 required / min / max 校验。
- 复杂校验以后端为准。
- 无 schema 时显示：

```text
该工具暂无可配置参数
```

## 15. ProjectToolConfigPage 改造

修改：

```text
frontend/src/modules/tool/pages/ProjectToolConfigPage.vue
```

新增能力：

1. 点击「配置」打开参数 Drawer / Dialog。
2. 显示工具基础信息。
3. 渲染 ToolParameterForm。
4. 保存时调用 enableProjectTool，并传 parameters。
5. 表格中展示参数摘要，例如：

```text
scope=TASK, maxFindings=5
```

6. 对 disabled 工具也可编辑参数，但保存时如选择启用则 enabled=true。

建议 data-testid：

- `tool-parameter-dialog`
- `tool-parameter-form`
- `tool-param-scope`
- `tool-param-maxFindings`
- `btn-save-tool-parameters`
- `tool-parameter-summary`

## 16. MultiAgentRunPanel 展示增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

在工具卡片中显示：

- 参数摘要
- inputPayload.parameters
- outputPayload.parameterSummary

data-testid：

- `tool-parameter-summary`
- `tool-input-parameters`

## 17. 后端测试

新增或修改：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolParameterSchemaIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/ToolCatalogPolicyIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/PatchProposalArtifactIntegrationTest.java
```

测试不少于 20 个：

### Schema / Config

1. tool_catalog 返回 parameterSchemaJson。
2. listProjectTools 返回默认 parametersJson。
3. enable tool 保存合法 parameters。
4. 缺少 required 参数使用 defaultValue。
5. required 无 default 且缺失返回 BAD_REQUEST。
6. number 小于 min 返回 BAD_REQUEST。
7. number 大于 max 返回 BAD_REQUEST。
8. select 不在 options 返回 BAD_REQUEST。
9. text 超过 maxLength 返回 BAD_REQUEST。
10. 额外参数被丢弃。

### Execution

11. tool execution inputPayload 包含 parameters。
12. outputPayload 包含 parameterSummary。
13. disabled 后再次 enable 保留 / 更新 parameters。
14. LOW 工具默认参数参与执行。
15. MEDIUM 工具启用参数参与执行。

### Patch Proposal

16. MOCK_PATCH_PROPOSAL artifact 包含 proposalScope。
17. includeTests=true 时 artifact checklist 包含测试项。
18. includeTests=false 时 artifact 不包含测试项。
19. maxChangedFiles 影响 mock diff 文件段数量。
20. targetArea 出现在 artifact summary。

全量后端质量门：

```bash
cd backend
mvn test
```

## 18. 前端 E2E

新增或修改：

```text
frontend/e2e/project-tool-policy.spec.ts
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. Project Tools 页面打开参数配置弹窗。
2. 参数表单根据 schema 渲染 select / switch / number / text。
3. 保存 MOCK_PATCH_PROPOSAL 参数。
4. 表格显示参数摘要。
5. 启动 Multi-Agent Run 并审批。
6. Tool card 显示参数摘要。
7. PATCH_PROPOSAL artifact 包含参数影响内容。
8. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 19. 文档与报告

完成后新增：

```text
docs/milestone-36e-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Tool Parameter Schema 设计说明
3. 数据库字段说明
4. ToolParameterSchemaService 校验规则
5. Project Tool 参数配置说明
6. ToolSandboxExecutionService 参数注入说明
7. Patch Proposal 参数影响说明
8. 前端 ToolParameterForm 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36F

## 20. 验收标准

必须满足：

- tool_catalog 支持 parameterSchemaJson。
- project_tool_config 支持 parametersJson 或等价字段。
- Project Tools API 返回 schema 和 parameters。
- 启用工具时可保存 parameters。
- 后端校验 required / number range / select options / maxLength。
- Tool execution inputPayload 包含 parameters。
- Tool execution outputPayload 包含 parameterSummary。
- Patch Proposal Artifact 可受 parameters 影响。
- 前端可动态渲染参数表单。
- 前端可保存并显示参数摘要。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 21. 已知非目标

本阶段不做：

- 完整 JSON Schema
- 嵌套参数
- array 参数
- 远程 options
- 参数条件联动
- 自定义工具创建
- 工具 marketplace
- 真实执行
- 真实 patch 生成
- Monaco 参数编辑器

这些可进入后续：

- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling
- 36H: Patch Review UI
- 36I: Tool Parameter Advanced Schema

## 22. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36E。

文档路径：
docs/milestone-36e-tool-parameter-schema.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A-36D 的 Tool Sandbox / Tool Policy / Approval / Patch Proposal 基础上，新增 Tool Parameter Schema。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要写真实代码文件。
6. 不要应用 patch。
7. 参数只影响 Mock output、inputPayload 和 PATCH_PROPOSAL artifact 内容。
8. 不要破坏 36A tool_sandbox_execution API。
9. 不要破坏 36B Tool Catalog / Project Tool Config API。
10. 不要破坏 36C Tool Approval API。
11. 不要破坏 36D PATCH_PROPOSAL Artifact 逻辑。
12. 不要破坏 35A-35F Multi-Agent API。
13. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
14. IDs 对外保持 String。
15. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V24 migration：tool_catalog.parameter_schema_json，project_tool_config.parameters_json（或复用 config_json 但对外保持 parameters）。
2. 为 6 个内置工具补齐 parameter_schema_json。
3. ToolCatalogEntity / ProjectToolConfigEntity / Response / Request 增加 schema 和 parameters 字段。
4. 新增 ToolParameterSchemaService，支持 text / textarea / boolean / number / select。
5. ToolCatalogApplicationService 启用工具时校验并保存 parameters。
6. listProjectTools 返回默认参数或已保存参数。
7. ToolSandboxExecutionService 将 parameters 写入 inputPayload，并在 outputPayload 写 parameterSummary。
8. PatchProposalArtifactService 根据 MOCK_PATCH_PROPOSAL parameters 调整 artifact 内容。
9. 前端新增 ToolParameterForm.vue。
10. ProjectToolConfigPage 支持参数配置弹窗和参数摘要展示。
11. MultiAgentRunPanel 显示工具参数摘要。
12. 后端测试不少于 20 个。
13. 前端 E2E 覆盖参数配置和执行展示。
14. 新增 docs/milestone-36e-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Tool Parameter Schema 设计说明
3. 数据库字段说明
4. ToolParameterSchemaService 校验规则
5. Project Tool 参数配置说明
6. ToolSandboxExecutionService 参数注入说明
7. Patch Proposal 参数影响说明
8. 前端 ToolParameterForm 说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 36F

现在开始实现，不要只给计划。
```
