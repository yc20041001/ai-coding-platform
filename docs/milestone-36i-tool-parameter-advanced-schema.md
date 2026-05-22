# Milestone 36I: Tool Parameter Advanced Schema

## 1. 背景

Milestone 36A-36H 已完成安全工具执行与补丁审阅闭环：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling
- 36H: Patch Review UI

当前工具参数已经支持基础 schema：

```text
text / textarea / boolean / number / select
```

但还缺少产品化配置能力，例如参数分组、条件显示、数组参数、路径 allowlist/denylist、schema version 和参数变更审计。

Milestone 36I 的目标是新增 **Tool Parameter Advanced Schema**：

```text
Advanced Schema
  -> groups
  -> dependsOn
  -> array
  -> pathRules
  -> schemaVersion
  -> parameter audit
```

本阶段仍不执行真实 shell，不做 Git 写操作，不写真实代码文件。高级参数只影响 Mock output、审计记录和 Patch Proposal 内容。

## 2. 总目标

实现产品级工具参数配置能力：

1. Tool parameter schema 支持 `schemaVersion`。
2. 支持参数分组 `groups`。
3. 支持条件显示 `dependsOn`。
4. 支持基础 array 类型。
5. 支持 path allowlist / denylist 校验。
6. 支持参数变更审计日志。
7. 前端 ToolParameterForm 支持高级 schema 渲染。
8. 只读仓库工具使用 pathRules 强化路径安全。
9. Patch Proposal 工具支持 targetFiles 数组。
10. 补齐后端测试和前端 E2E。

完成后，工具参数从：

```text
flat fields
```

升级为：

```text
versioned grouped conditional schema
```

## 3. 严格边界

必须遵守：

1. 不执行真实 shell。
2. 不执行真实 Git 写操作。
3. 不执行 git apply / git add / git commit / git push。
4. 不写真实代码文件。
5. 不修改 workspace 文件。
6. 不引入完整 JSON Schema validator。
7. 不做远程 options。
8. 不做复杂表达式引擎。
9. 不做嵌套 object 参数。
10. 不做自定义工具创建。
11. 不破坏 36E 基础 schema 兼容性。
12. 不破坏 36A-36H 已有 API。
13. 前端保持中文暗色科技风 UI。

允许做：

- 扩展现有简化 schema。
- 手写 schema validation。
- 前端动态表单增强。
- 参数变更审计。
- path allowlist/denylist 校验。

## 4. Advanced Schema 结构

36E 的 schema：

```json
{
  "fields": []
}
```

36I 扩展为：

```json
{
  "schemaVersion": 2,
  "groups": [
    {
      "key": "scope",
      "title": "范围设置",
      "description": "控制工具读取或分析的范围",
      "fields": ["branch", "pathPrefix", "targetFiles"]
    }
  ],
  "fields": [
    {
      "key": "targetFiles",
      "label": "目标文件",
      "type": "array",
      "itemType": "text",
      "required": false,
      "defaultValue": [],
      "maxItems": 10,
      "itemMaxLength": 256,
      "pathRules": {
        "deny": [".env", ".git/**", "*.pem", "*.key"],
        "allowPrefixes": ["backend/src", "frontend/src", "docs"]
      }
    },
    {
      "key": "includeTests",
      "label": "包含测试建议",
      "type": "boolean",
      "defaultValue": true
    },
    {
      "key": "testLevel",
      "label": "测试级别",
      "type": "select",
      "defaultValue": "INTEGRATION",
      "options": ["UNIT", "INTEGRATION", "E2E"],
      "dependsOn": {
        "field": "includeTests",
        "equals": true
      }
    }
  ]
}
```

## 5. 新增 Schema 能力

### 5.1 schemaVersion

规则：

- 缺失时视为 `1`。
- 当前支持 `1` 和 `2`。
- 大于 2 返回 BAD_REQUEST。

### 5.2 groups

仅用于前端展示和后端基础结构校验。

字段：

- key
- title
- description
- fields

规则：

- group.fields 中引用不存在 field 时 BAD_REQUEST。
- 未分组字段显示在“其他参数”分组。

### 5.3 dependsOn

支持简单条件：

```json
{
  "field": "includeTests",
  "equals": true
}
```

规则：

- 只支持 equals。
- dependsOn.field 不存在时 BAD_REQUEST。
- 如果条件不满足：
  - 前端隐藏字段。
  - 后端忽略该字段，使用 null 或不写入 normalized params。
  - required 不生效。

### 5.4 array

支持：

```json
{
  "type": "array",
  "itemType": "text",
  "maxItems": 10,
  "itemMaxLength": 256
}
```

限制：

- 仅支持 `itemType=text`。
- 不支持 nested array。
- 不支持 object item。

校验：

- value 必须是 array。
- maxItems 生效。
- 每项必须是 string。
- itemMaxLength 生效。
- 空字符串项自动丢弃。

### 5.5 pathRules

用于 text / array text 字段。

结构：

```json
{
  "deny": [".env", ".git/**", "*.pem", "*.key"],
  "allowPrefixes": ["backend/src", "frontend/src", "docs"]
}
```

规则：

- 先做通用路径安全校验：
  - 禁止绝对路径
  - 禁止 `..`
  - 禁止 `~`
  - 禁止空字符
  - 禁止 `.git/**`
  - 禁止 `.env*`
- deny 命中 → BAD_REQUEST。
- allowPrefixes 非空时，路径必须以其中一个 prefix 开头。
- 规则适用于 filePath / pathPrefix / targetFiles。

## 6. 数据库设计

本阶段优先不新增表。

复用：

- `tool_catalog.parameter_schema_json`
- `project_tool_config.parameters_json`

新增参数变更审计可复用 audit_log。

如果需要记录参数历史，可新增表，但本阶段不强制。

推荐不新增表，降低回归风险。

## 7. 后端服务改造

修改：

```text
ToolParameterSchemaService.java
```

新增能力：

```java
validateSchema(String schemaJson)
normalizeAndValidate(String schemaJson, Map<String, Object> parameters)
```

### 7.1 validateSchema

检查：

1. schemaVersion <= 2。
2. fields 是数组。
3. field.key 不为空且唯一。
4. type 合法。
5. array 必须 itemType=text。
6. dependsOn.field 必须存在。
7. groups.fields 必须引用存在字段。
8. pathRules 格式合法。

### 7.2 normalizeAndValidate

在 36E 基础上新增：

1. 处理 dependsOn。
2. 处理 array。
3. 处理 pathRules。
4. 丢弃条件不满足字段。
5. 丢弃额外参数。
6. 返回 normalized params。

## 8. 参数变更审计

修改：

```text
ToolCatalogApplicationService.java
```

当 project tool parameters 发生变化时写 audit log。

新增 audit action：

```text
TOOL_PARAMETER_UPDATE
```

如果 audit enum 修改风险低，则加入：

```text
backend/src/main/java/com/aicoding/platform/audit/domain/AuditActionType.java
```

审计 description：

```text
项目工具参数已更新：MOCK_PATCH_PROPOSAL
```

注意：

- 不记录完整敏感参数值。
- 可记录 changedKeys。
- 不记录 API Key / token / 密码。

如果 audit 接入较麻烦，至少写 task/project log 不适合；本阶段建议接 audit。

## 9. Seed Schema 升级

新增迁移：

```text
backend/src/main/resources/db/migration/V28__upgrade_tool_parameter_schema_v2.sql
```

如果 V28 已存在，请顺延。

重点升级：

### 9.1 READ_FILE_SNIPPET

新增 groups / pathRules：

```json
{
  "schemaVersion": 2,
  "groups": [
    {"key":"target","title":"读取目标","fields":["branch","filePath","startLine","maxLines"]}
  ],
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"filePath","label":"文件路径","type":"text","required":true,"defaultValue":"","maxLength":512,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"startLine","label":"起始行","type":"number","required":false,"defaultValue":1,"min":1,"max":100000},
    {"key":"maxLines","label":"最大行数","type":"number","required":true,"defaultValue":80,"min":1,"max":300}
  ]
}
```

### 9.2 MOCK_PATCH_PROPOSAL

新增 targetFiles array 和 dependsOn：

```json
{
  "schemaVersion": 2,
  "groups": [
    {"key":"scope","title":"提案范围","fields":["proposalScope","targetArea","targetFiles","maxChangedFiles"]},
    {"key":"tests","title":"测试建议","fields":["includeTests","testLevel"]}
  ],
  "fields": [
    {"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"targetFiles","label":"目标文件","type":"array","itemType":"text","required":false,"defaultValue":[],"maxItems":10,"itemMaxLength":256,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},
    {"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},
    {"key":"testLevel","label":"测试级别","type":"select","required":false,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"],
      "dependsOn":{"field":"includeTests","equals":true}}
  ]
}
```

## 10. Patch Proposal 集成

修改：

```text
PatchProposalArtifactService.java
```

增强：

- 如果 parameters.targetFiles 存在，Artifact Summary 显示目标文件列表。
- Mock diff 文件段优先使用 targetFiles。
- includeTests=false 时不生成测试建议 section。
- testLevel 显示在测试建议 section。
- targetFiles 仍只作为文本引用，不读取、不写入。

Safety block 保持：

```text
applied: false
filesTouched: []
gitOperations: []
```

## 11. 前端类型

修改：

```text
frontend/src/modules/tool/api.ts
```

新增：

```ts
export interface ToolParameterGroup {
  key: string
  title: string
  description?: string
  fields: string[]
}

export interface ToolParameterDependsOn {
  field: string
  equals: string | number | boolean
}

export interface ToolParameterPathRules {
  deny?: string[]
  allowPrefixes?: string[]
}

export interface ToolParameterField {
  key: string
  label: string
  type: 'text' | 'textarea' | 'boolean' | 'number' | 'select' | 'array'
  itemType?: 'text'
  required?: boolean
  defaultValue?: string | number | boolean | string[]
  options?: string[]
  min?: number
  max?: number
  maxLength?: number
  maxItems?: number
  itemMaxLength?: number
  dependsOn?: ToolParameterDependsOn
  pathRules?: ToolParameterPathRules
}

export interface ToolParameterSchema {
  schemaVersion?: number
  groups?: ToolParameterGroup[]
  fields: ToolParameterField[]
}
```

## 12. ToolParameterForm 改造

修改：

```text
frontend/src/modules/tool/components/ToolParameterForm.vue
```

新增能力：

1. 按 groups 分区展示。
2. 条件显示 dependsOn。
3. array text 输入。
4. pathRules 提示。
5. schemaVersion badge。

### 12.1 Array 控件

UI：

- 多行输入或 tag list。
- “新增一项”按钮。
- 删除单项按钮。

data-testid：

- `tool-param-array-targetFiles`
- `btn-add-array-item-targetFiles`
- `btn-remove-array-item-targetFiles`

### 12.2 dependsOn

如果 includeTests=false：

- 隐藏 testLevel。

data-testid：

- `tool-param-testLevel`

### 12.3 pathRules 提示

显示：

```text
允许前缀：backend/src, frontend/src, docs
禁止：.env, .git/**, *.pem, *.key
```

data-testid：

- `tool-param-path-rules`

## 13. ProjectToolConfigPage 改造

修改：

```text
frontend/src/modules/tool/pages/ProjectToolConfigPage.vue
```

要求：

1. 参数摘要支持 array：

```text
targetFiles=3 项, includeTests=true
```

2. 参数保存成功显示：

```text
工具参数已保存
```

3. 如果后端校验失败，展示具体错误。

## 14. MultiAgentRunPanel 展示增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

显示：

- schemaVersion
- targetFiles count
- parameterSummary

如果 outputPayload 包含 targetFiles：

```text
目标文件：backend/src/..., frontend/src/...
```

## 15. 后端测试

新增或修改：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ToolParameterSchemaIntegrationTest.java
```

测试不少于 24 个：

### Schema validation

1. schemaVersion 缺失默认为 1。
2. schemaVersion=2 通过。
3. schemaVersion>2 返回 BAD_REQUEST。
4. group 引用不存在 field 返回 BAD_REQUEST。
5. dependsOn 引用不存在 field 返回 BAD_REQUEST。
6. array itemType 非 text 返回 BAD_REQUEST。

### Parameters

7. array 参数合法保存。
8. array 超过 maxItems 返回 BAD_REQUEST。
9. array item 超过 itemMaxLength 返回 BAD_REQUEST。
10. array 空字符串项被丢弃。
11. dependsOn 条件满足时字段保留。
12. dependsOn 条件不满足时字段丢弃。
13. dependsOn 条件不满足时 required 不生效。

### Path rules

14. targetFiles 包含 `.env` 返回 BAD_REQUEST。
15. targetFiles 包含 `.git/config` 返回 BAD_REQUEST。
16. targetFiles 包含 `../secret` 返回 BAD_REQUEST。
17. targetFiles 不在 allowPrefixes 返回 BAD_REQUEST。
18. targetFiles 在 backend/src 下通过。

### Execution / Artifact

19. execution inputPayload 包含 targetFiles。
20. outputPayload parameterSummary 包含 targetFiles count。
21. Patch Proposal artifact 显示 targetFiles。
22. includeTests=false 时 artifact 不显示测试建议。
23. includeTests=true 时 artifact 显示 testLevel。
24. parameter update 写入 audit log 或可验证记录。

全量后端质量门：

```bash
cd backend
mvn test
```

## 16. 前端 E2E

修改：

```text
frontend/e2e/project-tool-policy.spec.ts
frontend/e2e/patch-proposal-review.spec.ts
```

测试：

1. MOCK_PATCH_PROPOSAL 参数表单显示分组。
2. targetFiles array 可新增 / 删除。
3. includeTests=false 隐藏 testLevel。
4. includeTests=true 显示 testLevel。
5. pathRules 提示可见。
6. 保存 targetFiles 成功。
7. 启动流程后 Patch Proposal 显示 targetFiles。
8. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 17. 文档与报告

完成后新增：

```text
docs/milestone-36i-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Advanced Schema 设计说明
3. groups / dependsOn / array / pathRules 说明
4. ToolParameterSchemaService 改造说明
5. 参数变更审计说明
6. Patch Proposal 参数增强说明
7. 前端 ToolParameterForm 增强说明
8. 后端测试结果
9. 前端 typecheck / build / E2E 结果
10. 已知限制
11. 是否可以进入 Milestone 37A

## 18. 验收标准

必须满足：

- schemaVersion 支持。
- groups 支持。
- dependsOn 支持。
- array text 支持。
- pathRules 支持。
- targetFiles 可保存。
- 非法路径被拒绝。
- dependsOn 隐藏字段不参与 required 校验。
- Tool execution inputPayload 包含高级参数。
- Patch Proposal Artifact 体现 targetFiles / includeTests / testLevel。
- 前端可动态渲染分组、条件字段、array 控件。
- 参数变更有审计或可追踪记录。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 19. 已知非目标

本阶段不做：

- 完整 JSON Schema
- nested object
- array object
- remote options
- 表达式引擎
- Monaco schema editor
- 自定义工具创建
- 真实工具执行
- 真实 patch apply

这些可进入后续：

- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index
- 37C: Tool Marketplace Draft
- 37D: Tool Schema Visual Editor

## 20. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36I。

文档路径：
docs/milestone-36i-tool-parameter-advanced-schema.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36E 的 Tool Parameter Schema 基础上，新增 Advanced Schema 能力。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git apply / git add / git commit / git push。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 高级参数只影响 Mock output、inputPayload 和 PATCH_PROPOSAL artifact 内容。
9. 不要引入完整 JSON Schema validator。
10. 不要破坏 36A-36H 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V28 migration 升级 tool parameter schema 到 schemaVersion=2。
2. ToolParameterSchemaService 支持 schemaVersion / groups / dependsOn / array / pathRules。
3. MOCK_PATCH_PROPOSAL 增加 targetFiles array、includeTests/testLevel dependsOn。
4. READ_FILE_SNIPPET 增加 pathRules。
5. ToolCatalogApplicationService 保存参数时执行高级校验。
6. 参数变更写 audit log 或等价可追踪记录。
7. PatchProposalArtifactService 根据 targetFiles/includeTests/testLevel 调整内容。
8. 前端 ToolParameterForm 支持分组、条件显示、array text、pathRules 提示。
9. ProjectToolConfigPage 参数摘要支持 array。
10. MultiAgentRunPanel 展示高级参数摘要。
11. 后端测试不少于 24 个。
12. 前端 E2E 覆盖高级参数表单和 Patch Proposal 参数影响。
13. 新增 docs/milestone-36i-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Advanced Schema 设计说明
3. groups / dependsOn / array / pathRules 说明
4. ToolParameterSchemaService 改造说明
5. 参数变更审计说明
6. Patch Proposal 参数增强说明
7. 前端 ToolParameterForm 增强说明
8. 后端测试结果
9. 前端 typecheck / build / E2E 结果
10. 已知限制
11. 是否可以进入 Milestone 37A

现在开始实现，不要只给计划。
```
