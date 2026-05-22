# Milestone 37E: Real Read-only Tool Adapter Hardening

## 1. 背景

Milestone 36G / 37B 已经让工具具备只读仓库上下文和代码索引能力：

- 36G: Read-only Repository Tooling
- 37B: Read-only Code Search Index
- 37C: Worker DLQ / Retry Backoff
- 37D: Tool Execution Metrics Dashboard

当前只读工具已经能输出仓库树、分支、文件片段、diff summary、code index 搜索结果，但实现仍偏 Mock-safe / lightweight。要进入真实演示和更可信的 Agent 上下文，需要对只读适配器做生产化加固：

```text
Repository / Workspace
  -> Safe Read-only Adapter
  -> Path sandbox
  -> Sensitive denylist
  -> Binary / large file guard
  -> Redaction / truncation
  -> Tool output
```

Milestone 37E 的目标是新增 **Real Read-only Tool Adapter Hardening**：

```text
真实读取允许范围内的本地仓库文本
但绝不 checkout / pull / write / commit / apply patch
```

## 2. 总目标

实现可信、安全、可演示的真实只读工具适配层：

1. 定义 ReadOnlyRepositoryAdapter 接口。
2. 实现 LocalWorkspaceReadOnlyAdapter。
3. 将只读仓库工具和 Code Index 构建迁移到 adapter。
4. 强化路径 sandbox。
5. 禁止敏感文件读取。
6. 识别并跳过二进制文件。
7. 限制大文件和输出长度。
8. 对可能的 secret pattern 做脱敏。
9. outputPayload 明确记录 filesRead / skippedFiles / redacted / truncated。
10. 补齐后端测试与前端 E2E。

完成后，系统从：

```text
mock-safe repository context
```

升级为：

```text
production-hardened read-only repository context
```

## 3. 严格边界

必须遵守：

1. 不执行真实 Git 写操作。
2. 不执行 `git checkout`。
3. 不执行 `git pull`。
4. 不执行 `git fetch`。
5. 不执行 `git reset`。
6. 不执行 `git apply`。
7. 不执行 `git add` / `git commit` / `git push`。
8. 不写真实代码文件。
9. 不修改 workspace 文件。
10. 不创建真实 patch 文件。
11. 不读取 `.git/**`。
12. 不读取 `.env*`。
13. 不读取 private key / certificate / keystore。
14. 不返回疑似 secret 原文。
15. 不破坏 36A-37D 已有 API。
16. 前端保持中文暗色科技风 UI。

允许做：

- 使用 Java NIO 只读读取文件。
- 使用 JGit 或现有 GitWorkspaceService 的只读能力，如果项目已有且不会修改工作区。
- 扫描安全路径下的文本文件。
- 返回截断、脱敏后的内容。
- 记录跳过原因。

## 4. 核心设计

新增接口：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/ReadOnlyRepositoryAdapter.java
```

```java
public interface ReadOnlyRepositoryAdapter {
    RepositoryTreeResult listTree(Long projectId, ReadOnlyRepositoryRequest request);
    RepositoryFileSnippetResult readSnippet(Long projectId, ReadOnlyRepositoryRequest request);
    RepositoryBranchResult listBranches(Long projectId, ReadOnlyRepositoryRequest request);
    RepositoryDiffSummaryResult readDiffSummary(Long projectId, ReadOnlyRepositoryRequest request);
}
```

新增实现：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/LocalWorkspaceReadOnlyAdapter.java
```

职责：

- 根据 project/repository/workspace 解析安全根目录。
- 只读扫描文件。
- 调用 RepositoryToolSafetyService 做路径校验。
- 对内容做二进制、大文件、敏感内容保护。

## 5. DTO / Result 设计

新增：

```text
ReadOnlyRepositoryRequest.java
RepositoryTreeResult.java
RepositoryFileSnippetResult.java
RepositoryBranchResult.java
RepositoryDiffSummaryResult.java
RepositoryReadFileItem.java
RepositorySkippedFileItem.java
```

### 5.1 ReadOnlyRepositoryRequest

字段：

- branch
- baseBranch
- pathPrefix
- filePath
- startLine
- maxLines
- maxFiles
- maxBytes
- includeRemote

### 5.2 RepositoryReadFileItem

字段：

- filePath
- language
- fileSize
- lineCount
- contentHash
- truncated Boolean
- redacted Boolean

### 5.3 RepositorySkippedFileItem

字段：

- filePath
- reason

跳过原因：

- SENSITIVE_PATH
- BINARY_FILE
- FILE_TOO_LARGE
- OUTSIDE_ALLOWED_PREFIX
- INVALID_PATH
- READ_ERROR

## 6. 安全路径规则

继续使用并增强：

```text
RepositoryToolSafetyService
```

必须支持：

### 6.1 禁止路径

- 绝对路径
- `..`
- `~`
- 空字符
- Windows drive path，例如 `C:\`
- `.git/**`
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
- `logs/**`
- `backups/**`
- `diagnostics/**`

### 6.2 allowPrefixes

默认允许：

- `backend/src`
- `frontend/src`
- `docs`
- `scripts`
- `deploy`
- `.github/workflows`

如工具参数提供 allowPrefixes，以工具参数为准，但不能允许敏感路径。

### 6.3 根目录保护

所有路径解析必须：

```text
root.resolve(relative).normalize()
```

并检查：

```text
resolved.startsWith(root)
```

否则拒绝。

## 7. 文件内容保护

新增：

```text
RepositoryContentSafetyService.java
```

职责：

### 7.1 二进制识别

规则：

- 前 4096 bytes 中含 NUL byte → binary。
- 常见二进制扩展直接跳过：
  - `.png`
  - `.jpg`
  - `.jpeg`
  - `.gif`
  - `.webp`
  - `.pdf`
  - `.zip`
  - `.jar`
  - `.class`
  - `.ico`
  - `.woff`
  - `.woff2`

### 7.2 大文件限制

默认：

- 单文件最大读取：128KB
- 单次工具最大输出：256KB
- 单次工具最大文件数：200

配置：

```yaml
app:
  read-only-tools:
    max-file-bytes: ${READ_ONLY_TOOL_MAX_FILE_BYTES:131072}
    max-output-bytes: ${READ_ONLY_TOOL_MAX_OUTPUT_BYTES:262144}
    max-files: ${READ_ONLY_TOOL_MAX_FILES:200}
```

### 7.3 Secret 脱敏

对读取内容进行 mask：

匹配：

- `sk-[A-Za-z0-9_-]{20,}`
- `ghp_[A-Za-z0-9_]{20,}`
- `github_pat_[A-Za-z0-9_]{20,}`
- `Bearer [A-Za-z0-9._-]{20,}`
- `(?i)(api[_-]?key|secret|password|token)\s*[:=]\s*['"]?[^'"\s]+`

替换：

```text
<redacted>
```

outputPayload 记录：

```json
{
  "redacted": true,
  "redactionCount": 3
}
```

## 8. 工具集成

修改：

```text
RepositoryReadToolService.java
CodeIndexBuildService.java
CodeSearchService.java
PatchProposalArtifactService.java
```

### 8.1 RepositoryReadToolService

改为调用 `ReadOnlyRepositoryAdapter`：

- READ_REPOSITORY_TREE → listTree
- READ_FILE_SNIPPET → readSnippet
- READ_DIFF_SUMMARY → readDiffSummary
- READ_BRANCH_INFO → listBranches

outputPayload 必须包含：

```json
{
  "readOnly": true,
  "filesRead": [],
  "skippedFiles": [],
  "filesTouched": [],
  "gitOperations": [],
  "truncated": false,
  "redacted": false
}
```

### 8.2 CodeIndexBuildService

构建索引时通过 adapter 获取文件列表和 snippet。

要求：

- 跳过敏感文件。
- 跳过二进制文件。
- 对内容先脱敏再入库。
- chunk 不保存 secret 原文。

### 8.3 PatchProposalArtifactService

引用 repository/code index context 时：

- 只引用 filePath / symbol / snippet summary。
- 不输出敏感内容。
- 标记 redacted/truncated。

## 9. 配置

修改：

```text
backend/src/main/resources/application.yml
.env.example
docs/environment-variable-index.md
```

新增：

```yaml
app:
  read-only-tools:
    max-file-bytes: ${READ_ONLY_TOOL_MAX_FILE_BYTES:131072}
    max-output-bytes: ${READ_ONLY_TOOL_MAX_OUTPUT_BYTES:262144}
    max-files: ${READ_ONLY_TOOL_MAX_FILES:200}
    redaction-enabled: ${READ_ONLY_TOOL_REDACTION_ENABLED:true}
```

`.env.example`：

```dotenv
READ_ONLY_TOOL_MAX_FILE_BYTES=131072
READ_ONLY_TOOL_MAX_OUTPUT_BYTES=262144
READ_ONLY_TOOL_MAX_FILES=200
READ_ONLY_TOOL_REDACTION_ENABLED=true
```

## 10. 后端 API 影响

不新增 API。

现有 API 输出增强：

- `/api/tool-sandbox-executions/{executionId}`
- `/api/multi-agent-runs/{runId}/tool-executions`
- `/api/projects/{projectId}/code-index/*`

新增字段可能在 outputPayload 中出现：

- skippedFiles
- redacted
- redactionCount
- truncated
- outputBytes

## 11. 前端展示

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
frontend/src/modules/code-index/pages/CodeIndexPage.vue
```

展示：

- filesRead count
- skippedFiles count
- redacted badge
- truncated badge
- safety note

文案：

```text
只读适配器已启用：系统不会 checkout、pull、写文件或执行 Git 写操作。敏感内容会被脱敏，大文件会被截断。
```

data-testid：

- `readonly-adapter-safety-note`
- `tool-skipped-files-summary`
- `tool-redacted-badge`
- `tool-truncated-badge`

## 12. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/ReadOnlyRepositoryAdapterIntegrationTest.java
backend/src/test/java/com/aicoding/platform/orchestration/RepositoryContentSafetyServiceTest.java
```

测试不少于 28 个：

### Path safety

1. `../secret` 被拒绝。
2. 绝对路径被拒绝。
3. `.env` 被拒绝。
4. `.git/config` 被拒绝。
5. `backend/src/main/java/App.java` 通过。
6. 不在 allowPrefixes 的路径被拒绝。

### Content safety

7. NUL byte 文件识别为 binary。
8. `.png` 跳过。
9. `.jar` 跳过。
10. 大文件被截断。
11. 输出超过 maxOutputBytes 被截断。
12. OpenAI-like key 被 redacted。
13. GitHub token 被 redacted。
14. password=xxx 被 redacted。

### Adapter behavior

15. listTree 返回 filesRead。
16. listTree 返回 skippedFiles。
17. readSnippet 返回指定行范围。
18. readSnippet 不超过 maxLines。
19. readSnippet 对敏感路径返回 BLOCKED 或 skipped。
20. listBranches 不 checkout。
21. readDiffSummary 不执行 shell git diff。

### Code index integration

22. code index 不索引敏感文件。
23. code index 不保存 secret 原文。
24. code index 跳过 binary。
25. code index chunk 标记 redacted/truncated。

### Tool output

26. repository tool outputPayload.filesTouched=[]。
27. repository tool outputPayload.gitOperations=[]。
28. repository tool outputPayload.redacted/truncated 字段存在。

全量后端质量门：

```bash
cd backend
mvn test
```

## 13. 前端 E2E

新增或修改：

```text
frontend/e2e/code-index.spec.ts
frontend/e2e/multi-agent-orchestration.spec.ts
```

测试：

1. Code Index 页面显示 read-only adapter safety note。
2. Multi-Agent 工具卡片显示 skipped files summary。
3. 如果 outputPayload.redacted=true，显示 redacted badge。
4. 如果 outputPayload.truncated=true，显示 truncated badge。
5. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 14. 文档与报告

完成后新增：

```text
docs/milestone-37e-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. ReadOnlyRepositoryAdapter 设计说明
3. LocalWorkspaceReadOnlyAdapter 行为说明
4. RepositoryToolSafetyService 加固说明
5. RepositoryContentSafetyService 说明
6. Code Index 集成说明
7. 前端展示说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 38A

## 15. 验收标准

必须满足：

- ReadOnlyRepositoryAdapter 接口存在。
- LocalWorkspaceReadOnlyAdapter 实现存在。
- 敏感路径被拒绝。
- 二进制文件被跳过。
- 大文件被截断。
- secret-like 内容被脱敏。
- outputPayload 包含 filesRead/skippedFiles/redacted/truncated。
- outputPayload 始终包含 filesTouched=[]。
- outputPayload 始终包含 gitOperations=[]。
- Code Index 不保存 secret 原文。
- 前端显示 read-only safety note。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 16. 已知非目标

本阶段不做：

- Tree-sitter
- LSP
- Embedding
- Vector DB
- 语义搜索
- Git checkout / pull
- 文件写入
- Patch apply
- PR comment
- 大仓库性能优化

这些可进入后续：

- 38A: Code Search Semantic RAG
- 38B: Worker Autoscaling / DLQ Operations
- 38C: Prometheus / Grafana Optional Integration
- 38D: Read-only Tool Adapter Metrics

## 17. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37E。

文档路径：
docs/milestone-37e-real-read-only-tool-adapter-hardening.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36G / 37B 的只读仓库工具和代码索引基础上，新增 Real Read-only Tool Adapter Hardening。
3. 不要执行真实 Git 写操作。
4. 不要执行 git checkout / git pull / git fetch / git reset / git apply / git add / git commit / git push。
5. 不要写真实代码文件。
6. 不要修改 workspace 文件。
7. 不要读取敏感路径，如 .env、*.pem、*.key、.git/**。
8. 不要返回疑似 secret 原文，必须脱敏。
9. 不要引入 Tree-sitter、向量数据库或 embedding。
10. 不要破坏 36A-37D 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 ReadOnlyRepositoryAdapter 接口。
2. 新增 LocalWorkspaceReadOnlyAdapter。
3. 新增 RepositoryContentSafetyService。
4. 加固 RepositoryToolSafetyService：路径 normalize、allowPrefixes、denylist。
5. RepositoryReadToolService 改为调用 adapter。
6. CodeIndexBuildService 通过 adapter 读取安全文本。
7. PatchProposalArtifactService 引用 repository/code context 时标记 redacted/truncated。
8. application.yml / .env.example / docs/environment-variable-index.md 增加 read-only tool 配置。
9. 前端 MultiAgentRunPanel / CodeIndexPage 显示 read-only safety note、skippedFiles、redacted、truncated。
10. 后端测试不少于 28 个。
11. 前端 E2E 覆盖安全提示和 redacted/truncated 展示。
12. 新增 docs/milestone-37e-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. ReadOnlyRepositoryAdapter 设计说明
3. LocalWorkspaceReadOnlyAdapter 行为说明
4. RepositoryToolSafetyService 加固说明
5. RepositoryContentSafetyService 说明
6. Code Index 集成说明
7. 前端展示说明
8. 安全边界说明
9. 后端测试结果
10. 前端 typecheck / build / E2E 结果
11. 已知限制
12. 是否可以进入 Milestone 38A

现在开始实现，不要只给计划。
```
