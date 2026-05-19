# Milestone 33B: Project Agent Model Config Selection

## 1. 背景

Milestone 33A 已完成项目级智能体配置验证与收口：

- `GET /api/projects/{projectId}/agents` 可返回全局 Agent + 项目级配置。
- `POST /api/projects/{projectId}/agents/{agentId}/enable` 已完成安全校验。
- `POST /api/projects/{projectId}/agents/{agentId}/disable` 可停用项目 Agent。
- 前端已有项目详情页「智能体」Tab 和 `ProjectAgentConfigPage`。
- 后端 189 个测试通过，项目级 Agent 配置集成测试覆盖完整。

当前限制：

- 前端启用 Agent 时使用空 payload `{}`。
- 后端自动选择 latest published version。
- Model Config 没有选择 UI。
- 项目级 Agent 与 Model Gateway 还没有形成明确的可配置绑定体验。

Milestone 33B 的目标是补齐「项目 Agent 启用时选择 Agent Version + Model Config」的配置闭环。

## 2. 总目标

实现项目级智能体配置选择能力：

1. 前端启用项目 Agent 时打开配置弹窗。
2. 用户可选择 Agent Version。
3. 用户可选择 Model Config。
4. 后端保存 `agentVersionId`、`modelConfigId`、`config` 到项目级配置。
5. 列表展示当前绑定的版本和模型配置。
6. 后端校验 Model Config 有效性。
7. 测试覆盖正向、错误、禁用配置等场景。

## 3. 严格边界

执行本阶段必须遵守：

1. 不重写 Agent / Project / Model Gateway 架构。
2. 不改 Task Orchestrator 执行逻辑，除非发现明确 bug。
3. 不接真实模型 API。
4. 不新增模型配置 CRUD 功能，复用已有 Model Gateway 配置页面和 API。
5. 不新增复杂 JSON 配置编辑器。
6. 不改变已有 enable / disable 权限规则。
7. 不破坏 33A 已新增测试。
8. 不删除已有 E2E。
9. 前端保持当前中文科技风 UI。
10. 所有错误继续使用 ApiResponse / BizException / ErrorCode。

允许做：

- 修改 `EnableProjectAgentRequest`。
- 修改 `ProjectAgentConfigResponse`。
- 修改 `AgentApplicationService` 项目级 Agent 配置逻辑。
- 修改 `ProjectAgentConfigPage.vue`。
- 复用 `frontend/src/modules/model/api.ts` 的模型配置查询能力。
- 新增后端集成测试。
- 新增 / 修改前端 E2E。

## 4. 当前接口预期

### 4.1 GET 项目 Agent 列表

```http
GET /api/projects/{projectId}/agents
```

返回每个 Agent：

```json
{
  "agentId": "300002",
  "code": "backend-agent",
  "name": "Backend Agent",
  "type": "BACKEND",
  "globalStatus": "ENABLED",
  "enabled": true,
  "agentVersionId": "310002",
  "versionNo": "v1.0.0",
  "modelConfigId": "900001",
  "modelProvider": "OPENAI",
  "modelName": "gpt-4.1-mini",
  "updateTime": "2026-05-18T10:00:00"
}
```

如果没有项目配置：

- `enabled=false`
- `agentVersionId` 可回退 latest published version。
- `versionNo` 可回退 latest published version。
- `modelConfigId/modelProvider/modelName` 可为空或显示默认。

### 4.2 Enable 项目 Agent

```http
POST /api/projects/{projectId}/agents/{agentId}/enable
```

请求：

```json
{
  "agentVersionId": "310002",
  "modelConfigId": "900001",
  "config": {
    "temperature": 0.2,
    "maxTokens": 4096
  }
}
```

本阶段 `config` 可以先传 `{}`，不要求前端做复杂配置编辑器。

后端行为：

- `agentVersionId` 为空时继续使用 latest published version。
- `modelConfigId` 为空时允许使用默认模型配置，或保持为空，由后端现有逻辑决定。
- `agentVersionId` 非空时必须校验属于该 Agent。
- `agentVersionId` 必须是 `PUBLISHED`。
- `modelConfigId` 非空时必须存在。
- `modelConfigId` 对应 `ModelConfig` 必须是启用状态。
- disabled global Agent 仍然不能 enable。

### 4.3 Disable 项目 Agent

```http
POST /api/projects/{projectId}/agents/{agentId}/disable
```

本阶段不改变该接口行为。

## 5. 后端建议修改文件

### 5.1 DTO

检查并按需修改：

```text
backend/src/main/java/com/aicoding/platform/agent/dto/EnableProjectAgentRequest.java
backend/src/main/java/com/aicoding/platform/agent/dto/ProjectAgentConfigResponse.java
```

`EnableProjectAgentRequest` 至少支持：

```java
private String agentVersionId;
private String modelConfigId;
private String config;
```

或如果当前项目使用 Map：

```java
private Map<String, Object> config;
```

注意：

- 对外 ID 保持 String。
- Service 内部再转换 Long。
- 不使用 Lombok。

`ProjectAgentConfigResponse` 建议增加：

```java
private String modelConfigId;
private String modelProvider;
private String modelName;
```

### 5.2 Service

检查并按需修改：

```text
backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java
```

需要补齐：

1. enable 时读取 `modelConfigId`。
2. `modelConfigId` 非空时查询 `ModelConfigMapper`。
3. 不存在则抛 `BAD_REQUEST` 或新增 `MODEL_CONFIG_NOT_FOUND`。
4. 状态非 ENABLED 则抛 `BAD_REQUEST` 或新增 `MODEL_CONFIG_DISABLED`。
5. 保存 `modelConfigId` 到 `project_agent_config`。
6. list project agents 时填充 modelProvider / modelName。

如果当前 `ProjectAgentConfigEntity` 已有 `modelConfigId` 字段，直接复用。

如果没有该字段：

- 检查数据库表是否已有。
- 如没有，需要新增 Flyway migration，例如：

```text
backend/src/main/resources/db/migration/V12__add_model_config_to_project_agent_config.sql
```

SQL 示例：

```sql
ALTER TABLE project_agent_config
  ADD COLUMN model_config_id BIGINT NULL COMMENT '模型配置ID' AFTER agent_version_id;

CREATE INDEX idx_project_agent_config_model
  ON project_agent_config(model_config_id);
```

如果表已经有字段，不要重复迁移。

### 5.3 Mapper

可能需要注入：

```text
backend/src/main/java/com/aicoding/platform/agent/infrastructure/ModelConfigMapper.java
```

或实际路径：

```text
backend/src/main/java/com/aicoding/platform/agent/infrastructure/ModelConfigMapper.java
backend/src/main/java/com/aicoding/platform/modelgateway/...
```

以当前项目实际结构为准。

## 6. 前端建议修改文件

### 6.1 API 类型

检查并按需修改：

```text
frontend/src/modules/agent/api.ts
frontend/src/modules/model/api.ts
```

`ProjectAgentConfig` 类型应包含：

```ts
modelConfigId: string | null
modelProvider: string | null
modelName: string | null
```

`enableProjectAgent()` payload 应支持：

```ts
{
  agentVersionId?: string
  modelConfigId?: string
  config?: Record<string, unknown>
}
```

### 6.2 ProjectAgentConfigPage

修改：

```text
frontend/src/modules/agent/pages/ProjectAgentConfigPage.vue
```

交互要求：

1. 点击「启用」不直接请求接口，而是打开弹窗。
2. 弹窗展示：
   - Agent 名称。
   - Version 下拉。
   - Model Config 下拉。
   - 当前 global status。
3. Version 下拉：
   - 至少默认使用当前行返回的 latest published version。
   - 如果后端未提供完整 version list，本阶段可以只显示当前 latest published version。
   - 不强制实现版本列表接口。
4. Model Config 下拉：
   - 调用 `getModelConfigs()` 拉取模型配置。
   - 只允许选择 `ENABLED` 状态的配置。
   - 显示格式：`Provider / Model Name`。
5. 保存时调用 enable 接口，带上：

```ts
{
  agentVersionId,
  modelConfigId,
  config: {}
}
```

6. 保存成功后刷新表格。
7. 保存失败显示 el-alert 或 ElMessage。
8. disabled global Agent 仍不能点击启用。

表格建议显示列：

| 列 | 内容 |
|---|---|
| 智能体 | name / code |
| 类型 | type |
| 全局状态 | globalStatus |
| 项目状态 | enabled |
| 版本 | versionNo |
| 模型配置 | provider + modelName |
| 更新时间 | updateTime |
| 操作 | 启用 / 停用 |

## 7. 后端测试要求

修改或新增：

```text
backend/src/test/java/com/aicoding/platform/agent/AgentProjectConfigIntegrationTest.java
```

至少补充：

1. `enable` with valid `modelConfigId` 成功。
2. GET 列表返回 `modelConfigId/modelProvider/modelName`。
3. invalid `modelConfigId` 返回 BAD_REQUEST。
4. disabled `modelConfig` 不能用于 enable。
5. `modelConfigId` 为空时保持现有默认行为。
6. disable 后 model config 信息不应误显示为 enabled 配置。

如果当前测试库没有模型配置数据，测试中插入临时 `model_config` 记录，并在 finally 中清理。

## 8. 前端 E2E 要求

修改或新增：

```text
frontend/e2e/project-agent-config.spec.ts
```

至少覆盖：

1. 登录。
2. 进入项目详情。
3. 打开「智能体」Tab。
4. 点击启用按钮。
5. 弹出「启用智能体」配置弹窗。
6. 模型配置下拉可见。
7. 如果有可用模型配置，选择一个配置并保存。
8. 表格刷新后展示模型配置。

注意：

- 如果测试环境没有可用 Model Config，可跳过选择并验证弹窗存在。
- 不要让测试依赖真实 OpenAI / Claude API。
- 选择器优先使用 `data-testid`。

建议新增 data-testid：

```text
btn-agent-enable
agent-enable-dialog
select-agent-version
select-model-config
btn-confirm-enable-agent
project-agent-table
```

## 9. 必须执行验证

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果本地后端无法启动导致 E2E 不能跑，需要说明原因，并至少保证：

```bash
npm run typecheck
npm run build
```

## 10. 手动验证清单

如果本地服务可运行，访问：

```text
http://localhost:5173
```

验证：

1. 登录成功。
2. 打开项目详情。
3. 点击「智能体」Tab。
4. 表格加载正常。
5. 点击「启用」打开配置弹窗。
6. 可看到 Agent Version。
7. 可看到 Model Config 下拉。
8. 选择模型配置后保存。
9. 表格显示选中的 provider / modelName。
10. 点击「停用」后状态变为 disabled。
11. disabled global Agent 无法启用。
12. 权限错误时有清晰提示。

## 11. 已知非目标

本阶段不做：

- Model Config CRUD。
- Agent Version 管理页面。
- 多模型 fallback 链选择。
- temperature / maxTokens 可视化配置。
- Prompt 模板编辑器。
- 真实模型调用验证。
- Redis / 缓存相关能力。

这些可以留到后续：

```text
Milestone 33C: Agent Version Management UI
Milestone 33D: Project Agent Runtime Config Editor
Milestone 34: Multi-model fallback strategy UI
```

## 12. 完成报告格式

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. 后端接口与校验变更说明
3. 前端启用弹窗与模型配置选择说明
4. 数据库 / Migration 说明
5. 后端测试覆盖说明
6. 前端 E2E 覆盖说明
7. 后端 mvn test 结果
8. 前端 typecheck / build / E2E 结果
9. 手动 UI 验证结果
10. 已知限制
11. 是否可以进入 Milestone 33C

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 33B。

文档路径：

```text
docs/milestone-33b-project-agent-model-config-selection.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段只做 Project Agent 启用时的 Agent Version + Model Config 选择，不要扩散到 Agent Version 管理页面。
3. 不要重写 Agent / Project / Model Gateway 架构。
4. 不要接真实模型 API。
5. 不要新增复杂 JSON 配置编辑器，本阶段 `config` 可以传 `{}`。
6. 不要破坏 Milestone 33A 已通过的测试。
7. 不要删除已有 E2E。
8. 前端保持当前中文科技风 UI。
9. 所有后端错误继续使用 ApiResponse / BizException / ErrorCode。
10. 如果发现 `project_agent_config` 缺少 `model_config_id` 字段，添加最小 Flyway migration；如果已经存在，不要重复迁移。

需要实现：

1. 检查并补齐 `EnableProjectAgentRequest` 的 `agentVersionId/modelConfigId/config`。
2. 检查并补齐 `ProjectAgentConfigResponse` 的 `modelConfigId/modelProvider/modelName`。
3. 后端 enable 时校验 `modelConfigId` 存在且状态 ENABLED。
4. 后端 list project agents 时返回模型配置展示字段。
5. 前端 `ProjectAgentConfigPage.vue` 点击启用时打开配置弹窗。
6. 弹窗里展示 Agent Version 和 Model Config 下拉。
7. 复用 `model/api.ts` 获取模型配置。
8. 保存时调用 enable 接口并带上 `agentVersionId/modelConfigId/config:{}`。
9. 保存成功后刷新表格。
10. 补充后端集成测试。
11. 补充前端 E2E 或更新已有 E2E。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 12 节格式输出报告。

现在开始实现，不要只给计划。
