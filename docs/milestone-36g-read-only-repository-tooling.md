# Milestone 36G: Read-only Repository Tooling

## 1. 背景

Milestone 36A-36F 已完成安全工具执行基础设施：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue

当前工具链路已经具备：

```text
Tool Catalog -> Project Config -> Parameters -> Policy -> Approval -> Job Queue -> Mock Drain -> Artifact
```

但工具仍主要基于任务上下文和 Mock 输入，无法读取项目仓库上下文。为了让 Code Review、Patch Proposal、Multi-Agent 分析更贴近真实项目，需要新增只读仓库工具。

Milestone 36G 的目标是新增 **Read-only Repository Tooling**：

```text
Repository Context
  -> read-only tool
  -> branch / tree / snippet / diff summary
  -> outputPayload.filesRead
  -> no file write
  -> no git write
```

本阶段只允许读取已有仓库信息，不执行任何会改变工作区或远程仓库的操作。

## 2. 总目标

实现只读仓库工具基础能力：

1. 新增 4 个只读仓库工具。
2. 工具参数支持 branch / pathPrefix / maxFiles / maxLines。
3. 后端工具执行可读取仓库上下文摘要。
4. outputPayload 记录 `filesRead`，`filesTouched` 仍为空。
5. Patch Proposal Artifact 可引用只读仓库上下文来源。
6. 前端工具卡片展示 filesRead / branch / pathPrefix。
7. 补齐后端测试与前端 E2E。

完成后，系统从：

```text
Agent only sees task context
```

升级为：

```text
Agent can consume read-only repository context
```

## 3. 严格边界

必须遵守：

1. 不执行真实 Git 写操作。
2. 不执行 `git checkout`。
3. 不执行 `git pull`。
4. 不执行 `git reset`。
5. 不执行 `git add` / `git commit` / `git push`。
6. 不执行 `git apply`。
7. 不写真实代码文件。
8. 不修改 workspace 文件。
9. 不创建真实 patch 文件。
10. 不访问项目目录以外路径。
11. 不读取敏感文件内容，例如 `.env`、密钥、证书。
12. 不绕过 Tool Policy / Approval / Job Queue。
13. 不破坏 Repository 模块已验证接口。
14. 不破坏 36A-36F API。
15. 前端保持中文暗色科技风 UI。

允许做：

- 读取已存在的仓库元数据。
- 读取安全范围内的文件路径列表。
- 读取受限文本片段。
- 读取已有 diff summary，如果 Repository 模块已有只读能力。
- 输出 mock-safe summary。
- 在 outputPayload 中记录 filesRead。

## 4. 工具目录新增

新增或迁移更新：

```text
backend/src/main/resources/db/migration/V26__seed_read_only_repository_tools.sql
```

如果 V26 已存在，请顺延。

Seed 4 个工具：

| id | toolKey | name | type | risk | mode | 默认 |
|---|---|---|---|---|---|---|
| 910101 | READ_REPOSITORY_TREE | 读取仓库文件树 | READ_ONLY | LOW | MOCK_EXECUTE | enabled |
| 910102 | READ_FILE_SNIPPET | 读取文件片段 | READ_ONLY | MEDIUM | MOCK_EXECUTE | disabled |
| 910103 | READ_DIFF_SUMMARY | 读取 Diff 摘要 | READ_ONLY | MEDIUM | MOCK_EXECUTE | disabled |
| 910104 | READ_BRANCH_INFO | 读取分支信息 | READ_ONLY | LOW | MOCK_EXECUTE | enabled |

注意：

- `execution_mode` 仍保持 `MOCK_EXECUTE` 或 `DRY_RUN`，但可以读取安全元数据。
- 工具结果不得改变仓库状态。
- 不新增 WRITE / GIT_WRITE 类型。

## 5. 参数 Schema

为 4 个工具补充 parameter_schema_json。

### 5.1 READ_REPOSITORY_TREE

```json
{
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"", "maxLength":128},
    {"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"", "maxLength":256},
    {"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":50,"min":1,"max":200}
  ]
}
```

### 5.2 READ_FILE_SNIPPET

```json
{
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"", "maxLength":128},
    {"key":"filePath","label":"文件路径","type":"text","required":true,"defaultValue":"", "maxLength":512},
    {"key":"startLine","label":"起始行","type":"number","required":false,"defaultValue":1,"min":1,"max":100000},
    {"key":"maxLines","label":"最大行数","type":"number","required":true,"defaultValue":80,"min":1,"max":300}
  ]
}
```

### 5.3 READ_DIFF_SUMMARY

```json
{
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"", "maxLength":128},
    {"key":"baseBranch","label":"基准分支","type":"text","required":false,"defaultValue":"main", "maxLength":128},
    {"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":30,"min":1,"max":100}
  ]
}
```

### 5.4 READ_BRANCH_INFO

```json
{
  "fields": [
    {"key":"includeRemote","label":"包含远程分支","type":"boolean","required":false,"defaultValue":true},
    {"key":"maxBranches","label":"最大分支数","type":"number","required":true,"defaultValue":30,"min":1,"max":100}
  ]
}
```

## 6. 安全路径规则

新增服务必须执行路径安全校验。

禁止读取：

- `.env`
- `.env.*`
- `*.pem`
- `*.key`
- `*.p12`
- `*.jks`
- `id_rsa`
- `id_ed25519`
- `node_modules/**`
- `target/**`
- `dist/**`
- `.git/**`
- `logs/**`
- `backups/**`
- `diagnostics/**`

禁止路径：

- 绝对路径
- 包含 `..`
- 包含 `~`
- 空字符
- Windows drive path，例如 `C:\`

新增 helper：

```text
RepositoryToolSafetyService
```

职责：

```java
public String normalizeRelativePath(String path)
public boolean isSensitivePath(String path)
public void validateSafeRelativePath(String path)
```

## 7. 后端服务设计

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/RepositoryReadToolService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/RepositoryToolSafetyService.java
```

### 7.1 RepositoryReadToolService

核心方法：

```java
public RepositoryToolResult executeReadOnlyTool(
    Long projectId,
    String toolKey,
    Map<String, Object> parameters
)
```

`RepositoryToolResult` 字段：

- summary
- outputPayload
- filesRead
- branch
- pathPrefix

### 7.2 工具行为

#### READ_REPOSITORY_TREE

行为：

- 查询项目绑定仓库或 workspace 根信息。
- 如果真实文件树能力已可安全使用，则返回受限文件列表。
- 如果仓库未 clone / 未绑定，返回 mock-safe empty tree summary。
- 不执行 git 命令。

outputPayload：

```json
{
  "mock": true,
  "readOnly": true,
  "toolKey": "READ_REPOSITORY_TREE",
  "branch": "main",
  "pathPrefix": "backend/src",
  "filesRead": ["backend/src/main/java/..."],
  "filesTouched": [],
  "gitOperations": []
}
```

#### READ_FILE_SNIPPET

行为：

- 校验 filePath 安全。
- 如果文件读取能力安全可用，则读取最多 maxLines 行。
- 不读取敏感路径。
- 不返回超长内容。
- 如果文件不存在，返回 summary 说明，不抛系统异常。

#### READ_DIFF_SUMMARY

行为：

- 优先复用 Repository 模块已有只读 diff API / service。
- 不执行 `git diff` shell。
- 如果没有可用 diff 数据，返回 mock summary。

#### READ_BRANCH_INFO

行为：

- 复用已有 GitWorkspaceService.listBranches() 或 Repository API。
- 只读查询分支。
- 不 checkout。
- 不 pull。

## 8. ToolSandboxExecutionService 集成

修改：

```text
ToolSandboxExecutionService
```

当 toolKey 属于 repository read tools：

```text
policy allowed
  -> create job
  -> drain mock job
  -> RepositoryReadToolService.executeReadOnlyTool(...)
  -> outputPayload.filesRead populated
  -> filesTouched=[]
  -> gitOperations=[]
```

要求：

1. 所有 repository tool 都走 36F Job Queue。
2. 所有 repository tool 都写 inputPayload parameters。
3. outputPayload 必须包含 readOnly=true。
4. outputPayload 必须包含 filesTouched=[]。
5. outputPayload 必须包含 gitOperations=[]。
6. 敏感路径被请求时，execution status = BLOCKED 或 FAILED，推荐 BLOCKED。

## 9. Patch Proposal 集成

`MOCK_PATCH_PROPOSAL` 生成 Artifact 时，如果同一个 run 中存在 repository read tool 输出：

1. Artifact Summary 增加：

```text
Repository Context Used
```

2. 列出 filesRead。
3. 标记：

```text
只读上下文来源，未修改文件
```

4. 不将真实文件片段完整塞入 patch proposal，避免内容过长。

## 10. 后端 API

不强制新增 API。

复用：

| API | 用途 |
|---|---|
| GET `/api/tool-catalog` | 查看新增 repository tools |
| GET `/api/projects/{projectId}/tools` | 项目启用 / 配置 repository tools |
| POST `/api/projects/{projectId}/tools/{toolId}/enable` | 启用只读工具 |
| GET `/api/multi-agent-runs/{runId}/tool-execution-jobs` | 查看只读工具 Job |
| GET `/api/tool-sandbox-executions/{executionId}` | 查看 outputPayload.filesRead |

可选新增：

```text
GET /api/projects/{projectId}/repository-context/branches
```

但如果已有 repository / branch API，不要重复。

## 11. 前端 API

通常无需新增 API。

更新类型：

```ts
export interface ToolSandboxExecutionResponse {
  // existing fields
  filesRead?: string[]
}
```

如果 filesRead 只存在 outputPayload 中，前端可解析 outputPayload。

## 12. 前端 Project Tool 页面

修改：

```text
frontend/src/modules/tool/pages/ProjectToolConfigPage.vue
```

要求：

1. 显示 4 个新增 repository tools。
2. 风险等级：
   - READ_REPOSITORY_TREE: LOW
   - READ_BRANCH_INFO: LOW
   - READ_FILE_SNIPPET: MEDIUM
   - READ_DIFF_SUMMARY: MEDIUM
3. 参数表单显示 branch / pathPrefix / filePath / maxFiles / maxLines。
4. MEDIUM 工具默认未启用。
5. 工具说明明确：

```text
只读仓库工具不会执行 checkout、pull、commit 或文件写入。
```

## 13. 前端 MultiAgentRunPanel 展示

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

工具卡片中展示：

- branch
- pathPrefix
- filesRead count
- filesRead list 折叠区域
- safety note

data-testid：

- `tool-files-read-summary`
- `tool-files-read-list`
- `repository-readonly-safety-note`

文案：

```text
只读仓库上下文：未 checkout，未 pull，未写入文件，未执行 Git 写操作。
```

## 14. Multi-Agent 集成建议

本阶段不要求所有 step 都自动调用 repository tools。

推荐最小集成：

1. 如果项目启用了 `READ_REPOSITORY_TREE`：
   - ARCHITECTURE_ANALYSIS step 额外执行该工具。
2. 如果项目启用了 `READ_BRANCH_INFO`：
   - CODE_REVIEW step 额外执行该工具。
3. 如果项目启用了 `READ_DIFF_SUMMARY`：
   - CODE_REVIEW step 额外执行该工具。
4. 如果项目启用了 `READ_FILE_SNIPPET`：
   - 仅当参数 filePath 非空时执行。

所有新增工具执行仍走：

```text
Policy -> Approval if needed -> Job -> Mock drain -> Tool execution response
```

## 15. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/RepositoryReadToolIntegrationTest.java
```

测试不少于 20 个：

### Tool Catalog / Config

1. tool catalog seed 后包含 4 个 repository tools。
2. READ_REPOSITORY_TREE risk=LOW。
3. READ_BRANCH_INFO risk=LOW。
4. READ_FILE_SNIPPET risk=MEDIUM。
5. READ_DIFF_SUMMARY risk=MEDIUM。
6. Project tools 列表包含 repository tools。
7. LOW repository tools 默认 enabled。
8. MEDIUM repository tools 默认 disabled。

### Safety

9. filePath 包含 `..` 被 BLOCKED。
10. filePath 是 `.env` 被 BLOCKED。
11. filePath 是 `.git/config` 被 BLOCKED。
12. pathPrefix 是绝对路径被 BLOCKED。
13. maxLines 超出 schema max 返回 BAD_REQUEST。

### Execution

14. READ_REPOSITORY_TREE 执行后 outputPayload.readOnly=true。
15. READ_REPOSITORY_TREE outputPayload.filesTouched=[]。
16. READ_BRANCH_INFO 不执行 checkout。
17. READ_FILE_SNIPPET 合法路径生成 filesRead。
18. READ_DIFF_SUMMARY 无 diff 时返回 mock summary，不失败。
19. Repository tools 都创建 job。
20. task logs 包含 repository read tool 执行记录。
21. Patch Proposal Artifact 引用 repository context used。

全量后端质量门：

```bash
cd backend
mvn test
```

## 16. 前端 E2E

新增或修改：

```text
frontend/e2e/project-tool-policy.spec.ts
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. Project Tools 页面显示 repository tools。
2. READ_REPOSITORY_TREE 默认启用。
3. READ_FILE_SNIPPET 默认未启用。
4. 参数表单显示 branch / pathPrefix / maxFiles。
5. 启动 Multi-Agent Run 后工具卡片显示 filesRead summary。
6. filesRead 折叠列表可展开。
7. safety note 显示“不执行 checkout/pull/git 写操作”。
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
docs/milestone-36g-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. 新增 Repository Read-only Tools 说明
3. 参数 Schema 说明
4. RepositoryToolSafetyService 安全规则
5. RepositoryReadToolService 行为说明
6. ToolSandboxExecutionService 集成说明
7. Patch Proposal Repository Context 集成说明
8. 前端展示说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36H

## 18. 验收标准

必须满足：

- tool catalog 包含 4 个 repository read-only tools。
- Project Tools 页面可查看并配置 repository tools。
- 参数 schema 可校验 branch/pathPrefix/filePath/maxFiles/maxLines。
- 敏感路径和非法路径被 BLOCKED。
- outputPayload 包含 `readOnly=true`。
- outputPayload 包含 `filesRead`。
- outputPayload 包含 `filesTouched=[]`。
- outputPayload 包含 `gitOperations=[]`。
- 不执行 checkout / pull / git write。
- MultiAgentRunPanel 可展示 filesRead。
- Patch Proposal Artifact 可引用 repository context。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 19. 已知非目标

本阶段不做：

- 真实 git checkout
- 真实 git pull
- 真实 git diff shell
- 真实文件写入
- Patch apply
- PR comment
- 大文件全文读取
- 二进制文件读取
- 代码搜索索引
- Tree-sitter 解析
- 语义代码检索

这些可进入后续：

- 36H: Patch Review UI
- 36I: Tool Parameter Advanced Schema
- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index

## 20. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 36G。

文档路径：
docs/milestone-36g-read-only-repository-tooling.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36A-36F 的 Tool Sandbox / Policy / Approval / Patch Proposal / Parameters / Job Queue 基础上，新增 Read-only Repository Tooling。
3. 不要执行真实 shell 命令作为业务能力。
4. 不要执行真实 Git 写操作。
5. 不要执行 git checkout / git pull / git reset / git add / git commit / git push / git apply。
6. 不要写真实代码文件。
7. 不要修改 workspace 文件。
8. 不要读取敏感文件内容，如 .env、*.pem、*.key、.git/**。
9. 所有 repository tools 必须是 READ_ONLY，outputPayload 必须保留 filesTouched=[] 和 gitOperations=[]。
10. 不要破坏 36A-36F 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V26 migration seed 4 个 repository read-only tools：
   - READ_REPOSITORY_TREE
   - READ_FILE_SNIPPET
   - READ_DIFF_SUMMARY
   - READ_BRANCH_INFO
2. 为 4 个工具添加 parameter_schema_json。
3. 新增 RepositoryToolSafetyService。
4. 新增 RepositoryReadToolService。
5. ToolSandboxExecutionService 支持 repository read tools。
6. outputPayload 写入 branch/pathPrefix/filesRead/readOnly/filesTouched=[]/gitOperations=[]。
7. 敏感路径和非法路径必须 BLOCKED。
8. Patch Proposal Artifact 可引用 repository context used。
9. ProjectToolConfigPage 显示 repository tools 和参数表单。
10. MultiAgentRunPanel 显示 filesRead summary/list 和安全提示。
11. 后端测试不少于 20 个。
12. 前端 E2E 覆盖 repository tools 展示和 filesRead。
13. 新增 docs/milestone-36g-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. 新增 Repository Read-only Tools 说明
3. 参数 Schema 说明
4. RepositoryToolSafetyService 安全规则
5. RepositoryReadToolService 行为说明
6. ToolSandboxExecutionService 集成说明
7. Patch Proposal Repository Context 集成说明
8. 前端展示说明
9. 安全边界说明
10. 后端测试结果
11. 前端 typecheck / build / E2E 结果
12. 已知限制
13. 是否可以进入 Milestone 36H

现在开始实现，不要只给计划。
```
