# Milestone 37B: Read-only Code Search Index

## 1. 背景

Milestone 36A-37A 已完成安全工具执行与异步队列基础：

- 36A: Safe Tool Execution Sandbox
- 36B: Read-only Tool Catalog + Tool Policy
- 36C: Human-approved Tool Execution
- 36D: Patch Proposal Artifact
- 36E: Tool Parameter Schema
- 36F: Sandbox Worker Queue
- 36G: Read-only Repository Tooling
- 36H: Patch Review UI
- 36I: Tool Parameter Advanced Schema
- 37A: Async Worker Queue with Redis / RabbitMQ

当前 Agent 已经可以通过只读工具获取仓库树、分支、文件片段和 diff 摘要。但这些能力仍偏“点查”，缺少跨文件的代码搜索索引能力。

Milestone 37B 的目标是新增 **Read-only Code Search Index**：

```text
Repository Read-only Context
  -> Code Index Build
  -> File / Symbol / Chunk Index
  -> Search Tools
  -> Multi-Agent / Patch Proposal Context
```

本阶段只做轻量级、只读、Mock-safe 的代码索引。不引入 Tree-sitter，不做语义向量，不写文件，不做 Git 写操作。

## 2. 总目标

实现只读代码索引与搜索基础能力：

1. 新增代码索引表：file / symbol / chunk。
2. 新增 Code Index Service，支持安全扫描仓库可读文件。
3. 新增轻量 symbol 提取，不引入复杂 parser。
4. 新增代码搜索工具：
   - `READ_CODE_INDEX`
   - `SEARCH_CODE_SYMBOL`
   - `SEARCH_CODE_CHUNK`
5. 工具执行仍走 Tool Policy / Approval / Job Queue。
6. outputPayload 记录 matchedFiles / matchedChunks / filesRead。
7. Patch Proposal 可引用 code index context。
8. 前端展示索引状态与搜索结果摘要。
9. 补齐后端测试与前端 E2E。

完成后，系统从：

```text
Agent can read repository fragments
```

升级为：

```text
Agent can search indexed code context
```

## 3. 严格边界

必须遵守：

1. 不执行真实 Git 写操作。
2. 不执行 git checkout / git pull / git reset / git apply / git add / git commit / git push。
3. 不写真实代码文件。
4. 不修改 workspace。
5. 不读取敏感路径。
6. 不读取二进制文件。
7. 不引入 Tree-sitter。
8. 不引入向量数据库。
9. 不做 embedding。
10. 不做全量大型仓库高性能索引优化。
11. 不绕过 36G 的路径安全规则。
12. 不绕过 Tool Policy / Approval / Job Queue。
13. 不破坏 36A-37A 已有 API。
14. 前端保持中文暗色科技风 UI。

允许做：

- 只读扫描安全路径下的文本文件。
- 建立轻量 file / symbol / chunk 索引。
- 使用 MySQL LIKE 搜索。
- 使用简单正则提取类名 / 函数名 / 方法名。
- 将搜索结果作为工具 outputPayload。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V29__init_code_search_index_tables.sql
```

如果 V29 已存在，请顺延。

### 4.1 code_index_file

```sql
CREATE TABLE IF NOT EXISTS code_index_file (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    repository_id BIGINT NULL,
    branch VARCHAR(128) NULL,
    file_path VARCHAR(1024) NOT NULL,
    language VARCHAR(64) NULL,
    file_size BIGINT DEFAULT 0,
    line_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    indexed_at DATETIME NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_index_file(project_id, branch, file_path),
    INDEX idx_code_index_project_branch(project_id, branch),
    INDEX idx_code_index_language(language),
    INDEX idx_code_index_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引文件表';
```

### 4.2 code_index_symbol

```sql
CREATE TABLE IF NOT EXISTS code_index_symbol (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    symbol_name VARCHAR(256) NOT NULL,
    symbol_type VARCHAR(64) NOT NULL,
    language VARCHAR(64) NULL,
    file_path VARCHAR(1024) NOT NULL,
    start_line INT NULL,
    end_line INT NULL,
    snippet TEXT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_code_symbol_project_name(project_id, symbol_name),
    INDEX idx_code_symbol_file(file_id),
    INDEX idx_code_symbol_type(symbol_type),
    INDEX idx_code_symbol_path(file_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引符号表';
```

### 4.3 code_index_chunk

```sql
CREATE TABLE IF NOT EXISTS code_index_chunk (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    chunk_index INT NOT NULL,
    start_line INT NOT NULL,
    end_line INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    token_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_chunk_file_index(file_id, chunk_index),
    INDEX idx_code_chunk_project(project_id),
    INDEX idx_code_chunk_file(file_id),
    INDEX idx_code_chunk_path(file_path),
    INDEX idx_code_chunk_hash(content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引切片表';
```

无物理外键，保持项目规范。

## 5. 状态与枚举

新增：

```text
CodeIndexFileStatus.java
CodeSymbolType.java
```

### 5.1 CodeIndexFileStatus

```java
public enum CodeIndexFileStatus {
    INDEXED,
    SKIPPED,
    FAILED
}
```

### 5.2 CodeSymbolType

```java
public enum CodeSymbolType {
    CLASS,
    INTERFACE,
    ENUM,
    METHOD,
    FUNCTION,
    COMPONENT,
    CONSTANT,
    UNKNOWN
}
```

## 6. Entity / Mapper / DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/CodeIndexFileEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/CodeIndexSymbolEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/domain/CodeIndexChunkEntity.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/CodeIndexFileMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/CodeIndexSymbolMapper.java
backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/CodeIndexChunkMapper.java
```

新增 DTO：

```text
CodeIndexSummaryResponse.java
CodeIndexFileResponse.java
CodeIndexSymbolResponse.java
CodeIndexChunkResponse.java
CodeSearchRequest.java
CodeSearchResponse.java
CodeSearchResultResponse.java
```

所有 ID 对外保持 String。

## 7. Code Index Service

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/CodeIndexApplicationService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/CodeIndexBuildService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/CodeSymbolExtractorService.java
backend/src/main/java/com/aicoding/platform/orchestration/application/CodeSearchService.java
```

### 7.1 CodeIndexBuildService

核心方法：

```java
public CodeIndexSummaryResponse buildIndex(Long projectId, String branch, String pathPrefix, Integer maxFiles)
```

行为：

1. 校验项目权限 DEVELOPER+。
2. 复用 36G RepositoryToolSafetyService。
3. 只扫描安全路径。
4. 跳过敏感路径和二进制文件。
5. 限制 maxFiles，默认 100，最大 500。
6. 对文本文件：
   - 计算 hash。
   - upsert code_index_file。
   - 删除旧 symbols/chunks。
   - 提取 symbols。
   - 切分 chunks。
7. 不写仓库文件。
8. 不执行 Git 写操作。

如果当前仓库未 clone 或无法安全读取真实文件：

- 允许生成 mock index entries。
- response 中标记 mock=true。
- 不失败。

### 7.2 CodeSymbolExtractorService

轻量正则，不引入 parser：

| language | 提取 |
|---|---|
| java | class/interface/enum/method |
| ts/js/vue | function/const/component/export |
| sql | table/index/migration-like keyword |
| md | heading |

找不到语言时：

- symbolType = UNKNOWN
- 可不生成 symbol。

### 7.3 CodeSearchService

方法：

```java
public CodeSearchResponse search(Long projectId, CodeSearchRequest request)
```

支持：

- keyword
- searchType: FILE / SYMBOL / CHUNK / ALL
- branch
- language
- pathPrefix
- limit

搜索策略：

- file: file_path LIKE
- symbol: symbol_name LIKE
- chunk: content LIKE
- ALL: 合并结果

不做向量检索。

## 8. 工具目录新增

新增 migration 或更新 seed：

```text
V30__seed_code_search_tools.sql
```

如果 V30 已存在，请顺延。

Seed 3 个工具：

| id | toolKey | name | type | risk | mode | 默认 |
|---|---|---|---|---|---|---|
| 910201 | READ_CODE_INDEX | 读取代码索引摘要 | READ_ONLY | LOW | MOCK_EXECUTE | enabled |
| 910202 | SEARCH_CODE_SYMBOL | 搜索代码符号 | READ_ONLY | MEDIUM | MOCK_EXECUTE | disabled |
| 910203 | SEARCH_CODE_CHUNK | 搜索代码片段 | READ_ONLY | MEDIUM | MOCK_EXECUTE | disabled |

### 8.1 READ_CODE_INDEX schema

```json
{
  "schemaVersion": 2,
  "groups": [{"key":"scope","title":"索引范围","fields":["branch","pathPrefix","maxFiles"]}],
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":100,"min":1,"max":500}
  ]
}
```

### 8.2 SEARCH_CODE_SYMBOL schema

```json
{
  "schemaVersion": 2,
  "groups": [{"key":"query","title":"搜索条件","fields":["keyword","branch","language","limit"]}],
  "fields": [
    {"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"language","label":"语言","type":"select","required":false,"defaultValue":"ALL","options":["ALL","java","ts","js","vue","sql","md"]},
    {"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}
  ]
}
```

### 8.3 SEARCH_CODE_CHUNK schema

```json
{
  "schemaVersion": 2,
  "groups": [{"key":"query","title":"搜索条件","fields":["keyword","branch","pathPrefix","limit"]}],
  "fields": [
    {"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}
  ]
}
```

## 9. ToolSandboxExecutionService 集成

当 toolKey 属于 code search tools：

```text
READ_CODE_INDEX
  -> build or read index summary

SEARCH_CODE_SYMBOL
  -> search symbols

SEARCH_CODE_CHUNK
  -> search chunks
```

outputPayload 必须包含：

```json
{
  "mock": true,
  "readOnly": true,
  "toolKey": "SEARCH_CODE_SYMBOL",
  "matchedFiles": [],
  "matchedSymbols": [],
  "matchedChunks": [],
  "filesRead": [],
  "filesTouched": [],
  "gitOperations": []
}
```

注意：

- `filesRead` 表示索引或搜索读取过的安全路径。
- `filesTouched` 必须为空。
- `gitOperations` 必须为空。

## 10. Patch Proposal 集成

Patch Proposal Artifact 若同 run 中存在 code search tool 输出：

新增 section：

```markdown
## Code Search Context

- Matched symbols: ...
- Matched chunks: ...
- Referenced files: ...

> 以上上下文来自只读代码索引。系统未写入文件，未执行 Git 操作。
```

不得将大段代码全文写入 patch proposal。

## 11. 后端 API

新增 Controller：

```text
backend/src/main/java/com/aicoding/platform/orchestration/controller/CodeIndexController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/code-index/build` | DEVELOPER+ | 构建只读代码索引 |
| GET | `/api/projects/{projectId}/code-index/summary` | VIEWER+ | 查询索引摘要 |
| GET | `/api/projects/{projectId}/code-index/files` | VIEWER+ | 查询索引文件 |
| GET | `/api/projects/{projectId}/code-index/symbols` | VIEWER+ | 查询符号 |
| POST | `/api/projects/{projectId}/code-index/search` | VIEWER+ | 搜索 file/symbol/chunk |

`CodeSearchRequest`：

```json
{
  "keyword": "TaskApplicationService",
  "searchType": "ALL",
  "branch": "main",
  "language": "java",
  "pathPrefix": "backend/src",
  "limit": 10
}
```

## 12. 前端 API

新增：

```text
frontend/src/modules/code-index/api.ts
```

类型：

```ts
export interface CodeIndexSummary {
  projectId: string
  fileCount: number
  symbolCount: number
  chunkCount: number
  indexedAt: string | null
  mock: boolean
}

export interface CodeSearchRequest {
  keyword: string
  searchType: 'FILE' | 'SYMBOL' | 'CHUNK' | 'ALL'
  branch?: string
  language?: string
  pathPrefix?: string
  limit?: number
}

export interface CodeSearchResult {
  resultType: string
  filePath: string
  symbolName?: string
  symbolType?: string
  startLine?: number
  endLine?: number
  snippet?: string
}
```

函数：

```ts
buildCodeIndex(projectId, payload)
getCodeIndexSummary(projectId)
listCodeIndexFiles(projectId, params)
listCodeIndexSymbols(projectId, params)
searchCodeIndex(projectId, payload)
```

## 13. 前端页面

新增：

```text
frontend/src/modules/code-index/pages/CodeIndexPage.vue
```

路由：

```text
/projects/:projectId/code-index
```

项目详情 SectionRail 新增：

```text
代码索引
```

页面功能：

1. 索引摘要卡片：
   - files
   - symbols
   - chunks
   - indexedAt
2. 构建索引按钮。
3. 搜索表单：
   - keyword
   - searchType
   - language
   - pathPrefix
   - limit
4. 搜索结果列表。
5. 安全提示：

```text
代码索引为只读能力，不 checkout、不 pull、不写文件、不执行 Git 写操作。
```

data-testid：

- `code-index-page`
- `btn-build-code-index`
- `code-index-summary`
- `code-search-input`
- `btn-search-code-index`
- `code-search-results`
- `code-search-result-item`
- `code-index-safety-note`

## 14. MultiAgentRunPanel 展示增强

修改：

```text
frontend/src/modules/task/components/MultiAgentRunPanel.vue
```

工具卡片中展示：

- matchedFiles count
- matchedSymbols count
- matchedChunks count
- code index context note

data-testid：

- `tool-code-index-summary`
- `tool-matched-symbols`
- `tool-matched-chunks`

## 15. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/orchestration/CodeSearchIndexIntegrationTest.java
```

测试不少于 24 个：

### Index build

1. build index 成功返回 summary。
2. summary 包含 fileCount/symbolCount/chunkCount。
3. 未登录 build 返回 UNAUTHORIZED。
4. VIEWER build 返回 PROJECT_ACCESS_DENIED。
5. pathPrefix 非法返回 BAD_REQUEST。
6. pathPrefix 敏感路径返回 BAD_REQUEST。
7. maxFiles 超限返回 BAD_REQUEST。

### Search

8. search keyword 返回 results。
9. searchType FILE 只返回 file result。
10. searchType SYMBOL 只返回 symbol result。
11. searchType CHUNK 只返回 chunk result。
12. searchType ALL 合并结果。
13. limit 生效。
14. language filter 生效。
15. pathPrefix filter 生效。
16. 空 keyword 返回 BAD_REQUEST。

### Tools

17. tool catalog 包含 READ_CODE_INDEX。
18. tool catalog 包含 SEARCH_CODE_SYMBOL。
19. tool catalog 包含 SEARCH_CODE_CHUNK。
20. READ_CODE_INDEX execution outputPayload.readOnly=true。
21. SEARCH_CODE_SYMBOL outputPayload.matchedSymbols 存在。
22. SEARCH_CODE_CHUNK outputPayload.matchedChunks 存在。
23. outputPayload.filesTouched=[]。
24. outputPayload.gitOperations=[]。
25. Patch Proposal Artifact 引用 Code Search Context。

全量后端质量门：

```bash
cd backend
mvn test
```

## 16. 前端 E2E

新增：

```text
frontend/e2e/code-index.spec.ts
```

测试：

1. 项目详情可打开代码索引 Tab。
2. 安全提示可见。
3. 点击构建索引。
4. 索引摘要展示。
5. 输入关键词搜索。
6. 搜索结果展示。
7. Project Tools 页面显示 code search tools。
8. Multi-Agent 工具卡片显示 code index summary。
9. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 17. 文档与报告

完成后新增：

```text
docs/milestone-37b-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. code_index_file / symbol / chunk 表说明
3. CodeIndexBuildService 说明
4. CodeSymbolExtractorService 说明
5. CodeSearchService 说明
6. 新增 Code Search Tools 说明
7. ToolSandboxExecutionService 集成说明
8. Patch Proposal Code Search Context 集成说明
9. 前端 CodeIndexPage 说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37C

## 18. 验收标准

必须满足：

- code_index_file 表存在。
- code_index_symbol 表存在。
- code_index_chunk 表存在。
- 可以构建只读代码索引。
- 可以查询索引摘要。
- 可以搜索 file/symbol/chunk。
- 工具目录包含 3 个 code search tools。
- 工具执行 outputPayload 包含 readOnly=true。
- 工具执行 outputPayload 包含 filesTouched=[]。
- 工具执行 outputPayload 包含 gitOperations=[]。
- 非法/敏感路径被拒绝。
- 前端 Code Index 页面可用。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 19. 已知非目标

本阶段不做：

- Tree-sitter
- LSP
- Embedding
- Vector DB
- 语义搜索
- 大仓库性能优化
- 增量索引 watcher
- 真实 Git 操作
- 文件写入
- Patch apply

这些可进入后续：

- 37C: Worker DLQ / Retry Backoff
- 37D: Tool Execution Metrics Dashboard
- 37E: Real Read-only Tool Adapter Hardening
- 38A: Code Search Semantic RAG

## 20. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37B。

文档路径：
docs/milestone-37b-read-only-code-search-index.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 36G 的 Read-only Repository Tooling 和 37A Async Worker Queue 基础上，新增 Read-only Code Search Index。
3. 不要执行真实 Git 写操作。
4. 不要执行 git checkout / git pull / git reset / git apply / git add / git commit / git push。
5. 不要写真实代码文件。
6. 不要修改 workspace 文件。
7. 不要读取敏感路径，如 .env、*.pem、*.key、.git/**。
8. 不要引入 Tree-sitter、向量数据库或 embedding。
9. 代码索引必须是只读能力，outputPayload 必须保留 filesTouched=[] 和 gitOperations=[]。
10. 不要破坏 36A-37A 已有 API。
11. 不要破坏 35A-35F Multi-Agent API。
12. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
13. IDs 对外保持 String。
14. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 V29 code_index_file / code_index_symbol / code_index_chunk migration。
2. 新增 CodeIndexFileEntity / CodeIndexSymbolEntity / CodeIndexChunkEntity / Mapper / DTO。
3. 新增 CodeIndexBuildService / CodeSymbolExtractorService / CodeSearchService / CodeIndexApplicationService。
4. 新增 V30 seed code search tools：
   - READ_CODE_INDEX
   - SEARCH_CODE_SYMBOL
   - SEARCH_CODE_CHUNK
5. ToolSandboxExecutionService 支持 code search tools。
6. outputPayload 写入 matchedFiles/matchedSymbols/matchedChunks/filesRead/readOnly/filesTouched=[]/gitOperations=[]。
7. Patch Proposal Artifact 可引用 Code Search Context。
8. 新增 CodeIndexController API。
9. 前端新增 code-index/api.ts 和 CodeIndexPage.vue。
10. 项目详情 SectionRail 新增「代码索引」Tab。
11. MultiAgentRunPanel 显示 code index summary。
12. 后端测试不少于 24 个。
13. 前端 E2E 覆盖 Code Index 页面和搜索。
14. 新增 docs/milestone-37b-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. code_index_file / symbol / chunk 表说明
3. CodeIndexBuildService 说明
4. CodeSymbolExtractorService 说明
5. CodeSearchService 说明
6. 新增 Code Search Tools 说明
7. ToolSandboxExecutionService 集成说明
8. Patch Proposal Code Search Context 集成说明
9. 前端 CodeIndexPage 说明
10. 安全边界说明
11. 后端测试结果
12. 前端 typecheck / build / E2E 结果
13. 已知限制
14. 是否可以进入 Milestone 37C

现在开始实现，不要只给计划。
```
