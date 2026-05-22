# Milestone 36E — Tool Parameter Schema — 完成报告

## 概述
本里程碑在 36A-36D 基础上，为工具沙箱系统增加了可配置的参数 Schema 支持，使每个工具可以定义自己的参数结构，并在启用/执行时进行校验、注入和展示。

## 新增文件

| 文件 | 职责 |
|------|------|
| `ToolParameterSchemaService.java` | 参数校验与规范化：支持 text/textarea/boolean/number/select 5种类型 |
| `ToolParameterForm.vue` | 动态参数表单组件，根据 Schema JSON 渲染对应控件 |
| `ToolParameterSchemaIntegrationTest.java` | 20 个集成测试，覆盖 schema/config/execution/artifact |

## 修改文件

### 后端
| 文件 | 改动 |
|------|------|
| `ProjectToolConfigResponse.java` | 新增 parameterSchemaJson、parametersJson 字段 |
| `UpdateProjectToolConfigRequest.java` | 新增 parameters(Map) 字段 |
| `ToolCatalogApplicationService.java` | 参数校验/保存/默认值注入 |
| `ToolSandboxExecutionService.java` | inputPayload 注入参数，outputPayload 增加 parameterSummary |
| `PatchProposalArtifactService.java` | 根据参数(proposalScope/includeTests/maxChangedFiles/targetArea)定制产物内容 |
| `schema.sql` | 新增 parameter_schema_json、parameters_json 列，6 个工具的种子 schema |

### 前端
| 文件 | 改动 |
|------|------|
| `api.ts` | 新增 ToolParameterField/ToolParameterSchema 类型，参数相关字段 |
| `ProjectToolConfigPage.vue` | 参数配置对话框、参数摘要展示、"配置"按钮 |
| `MultiAgentRunPanel.vue` | 工具参数展示、inputPayload 参数显示 |
| `project-tool-policy.spec.ts` | 新增 4 个参数配置 E2E 测试 |

## 工具参数 Schema

| 工具 ID | 工具 Key | 参数 |
|---------|----------|------|
| 910001 | MOCK_CODE_ANALYSIS | scope(select), includeMetadata(boolean) |
| 910002 | MOCK_CODE_REVIEW | depth(select), maxFindings(number, 1-20) |
| 910003 | MOCK_FILE_INSPECTION | targetArea(text, max128), includeStyleHints(boolean) |
| 910004 | MOCK_TASK_DECOMPOSITION | testLevel(select, default=INTEGRATION), includeEdgeCases(boolean, default=true) |
| 910005 | MOCK_CODING_STANDARD | riskFocus(text, max64), maxFindings(number, 1-20, default=5) |
| 910006 | MOCK_PATCH_PROPOSAL | proposalScope(select), includeTests(boolean), maxChangedFiles(number, 1-10), targetArea(text, max256) |

## 测试结果
- **Backend**: 378/378 通过 (包含 20 个参数 schema 测试)
- **Frontend**: typecheck 通过，build 成功
- **E2E**: 新增 4 个参数配置 E2E 测试

## 边界约束
- 不执行真实 Shell 操作
- 不执行真实 Git 写操作
- 不执行真实文件写入
- 不执行补丁应用
- 参数校验拒绝：超长文本、越界数值、不在选项中的 select 值
- 无关参数自动丢弃 (extraParam filtering)
- DEFAULT_TOOL(危险) 在当前阶段不允许启用
- 默认参数在未提供时自动注入
