# Milestone 37E: Real Read-only Tool Adapter Hardening — 完成报告

## 1. 总览

| 项目 | 值 |
|------|-----|
| 里程碑 | 37E — Real Read-only Tool Adapter Hardening |
| 分支 | main |
| 完成日期 | 2026-05-21 |
| 后端新增文件 | 10 |
| 前端修改文件 | 3 |
| 后端测试 | 48 新增 (全量 597 通过) |
| E2E 测试 | 更新 selector |

## 2. 新增 / 修改文件清单

### 新增文件 (10)

```
backend/src/main/java/com/aicoding/platform/orchestration/dto/
├── ReadOnlyRepositoryRequest.java        — 请求 DTO
├── RepositoryReadFileItem.java           — 已读文件项
├── RepositorySkippedFileItem.java        — 跳过文件项
├── RepositoryTreeResult.java             — listTree 结果
├── RepositoryFileSnippetResult.java      — readSnippet 结果
├── RepositoryBranchResult.java           — listBranches 结果
├── RepositoryDiffSummaryResult.java      — readDiffSummary 结果

backend/src/main/java/com/aicoding/platform/orchestration/application/
├── ReadOnlyRepositoryAdapter.java        — 接口
├── LocalWorkspaceReadOnlyAdapter.java    — NIO 实现
├── RepositoryContentSafetyService.java   — 内容安全服务

backend/src/main/java/com/aicoding/platform/orchestration/config/
└── ReadOnlyToolProperties.java           — 配置属性

backend/src/test/java/com/aicoding/platform/orchestration/
├── ReadOnlyToolAdapterIntegrationTest.java  — 28 个集成测试
└── RepositoryContentSafetyServiceTest.java  — 20 个单元测试
```

### 修改文件 (8)

```
backend/src/main/java/com/aicoding/platform/orchestration/application/
├── RepositoryToolSafetyService.java      — 加固：validateAllowedPrefix, resolveAndValidatePath
├── RepositoryReadToolService.java        — 改为调用 ReadOnlyRepositoryAdapter
├── CodeIndexBuildService.java            — 改为通过 adapter 读取真实文件
└── PatchProposalArtifactService.java     — 标记 redacted/truncated

backend/src/main/resources/application.yml            — 新增 read-only-tools 配置
.env.example                                          — 新增 READ_ONLY_TOOL_* 变量
docs/environment-variable-index.md                    — 新增 Section 17

frontend/src/modules/task/components/MultiAgentRunPanel.vue  — 显示 skippedFiles/redacted/truncated
frontend/src/modules/code-index/pages/CodeIndexPage.vue      — 更新安全提示
```

## 3. ReadOnlyRepositoryAdapter 设计说明

接口定义在 `orchestration/application/ReadOnlyRepositoryAdapter.java`：

```java
public interface ReadOnlyRepositoryAdapter {
    RepositoryTreeResult listTree(ReadOnlyRepositoryRequest request);
    RepositoryFileSnippetResult readSnippet(ReadOnlyRepositoryRequest request);
    RepositoryBranchResult listBranches(ReadOnlyRepositoryRequest request);
    RepositoryDiffSummaryResult readDiffSummary(ReadOnlyRepositoryRequest request);
}
```

四种方法对应四种只读仓库工具类型。实现通过 `LocalWorkspaceReadOnlyAdapter` 提供。

## 4. LocalWorkspaceReadOnlyAdapter 行为说明

| 方法 | 行为 |
|------|------|
| `listTree` | 使用 Java NIO `DirectoryStream` 递归扫描工作区文件；过滤敏感路径、二进制文件、大文件；返回 filesRead + skippedFiles |
| `readSnippet` | 使用 `Files.readAllBytes()` + `RepositoryToolSafetyService.resolveAndValidatePath()` 校验路径；支持 startLine/maxLines 范围截取；应用 secret 脱敏 |
| `listBranches` | 从 `.git/refs/heads/` 读取本地分支（不执行 git checkout/pull/fetch）；标记 `noCheckout=true, noPull=true` |
| `readDiffSummary` | 不执行真实 `git diff`；返回空 diff 并标记 `noRealGitDiff=true` |

所有方法输出中跟踪：
- `filesTouched=[]` — 始终为空（不写文件）
- `gitOperations=[]` — 始终为空（不执行 Git 写操作）
- `truncated`/`redacted` — 如实反映内容处理状态

## 5. RepositoryToolSafetyService 加固说明

新增功能：
- `validateAllowedPrefix(relativePath)` — 检查路径是否在 `allowPrefixes` 内（默认：backend/src, frontend/src, docs, scripts, deploy, .github/workflows）
- `resolveAndValidatePath(Path root, String relativePath)` — 组合 normalize + startsWith 检查，确保解析后路径不逃逸根目录

## 6. RepositoryContentSafetyService 说明

| 功能 | 说明 |
|------|------|
| 二进制识别 | 前 4096 bytes NUL byte 检测 + 扩展名黑名单（28 种） |
| 文件大小限制 | 默认 128KB（可配置） |
| 输出大小限制 | 默认 256KB（可配置） |
| Secret 脱敏 | 匹配 sk-*, ghp_*, github_pat_*, Bearer JWT, api_key/secret/password/token 赋值、JWT pattern |
| 脱敏替换 | 替换为 `**REDACTED**`，记录 redactionCount |

## 7. Code Index 集成说明

`CodeIndexBuildService` 迁移至 adapter：
- 通过 `repositoryAdapter.listTree()` 获取真实文件列表
- 通过 `repositoryAdapter.readSnippet()` 读取文件内容（已脱敏）
- `setMock(usedMockFallback)` — 真实文件时标记为 false，mock 回退时标记为 true
- 脱敏后内容才入库进行符号提取和切片

## 8. 前端展示说明

`MultiAgentRunPanel.vue`:
- `getRepositoryInfo()` 解析 skippedFiles/redacted/truncated 字段
- 已脱敏 / 已截断 badge（el-tag）
- 跳过文件摘要（可展开列表）
- data-testid: `tool-redacted-badge`, `tool-truncated-badge`, `tool-skipped-files-summary`

`CodeIndexPage.vue`:
- 安全提示更新为："系统从工作区读取文件内容并构建索引。机密信息（如 API Key、Token、密码等）会在索引前自动脱敏。敏感路径（如 .env, .git, node_modules）会被跳过。"

## 9. 后端测试结果

```
ReadOnlyToolAdapterIntegrationTest: 28 tests, 0 failures
├── Path Safety (6) — 绝对路径、..、~、NUL、.env、.git、pem
├── Content Safety (8) — NUL 检测、扩展名、文件大小、API Key/GitHub Token redaction
├── Adapter Behavior (7) — listTree/readSnippet/branches/diff 行为验证
├── Tool Output Structure (3) — filesTouched=[], gitOperations=[], redacted, truncated
└── Content Redaction (4) — Bearer token, password, 正常文本, 输出限制

RepositoryContentSafetyServiceTest: 20 tests, 0 failures
├── Binary Detection (6) — 空/大/扩展名不区分大小写/null/blank
├── Secret Redaction (9) — sk-*, ghp_*, github_pat_*, JWT, api_key, secret, password, count, clean
├── File Size Limits (3) — 默认/自定义/null
└── Edge Cases (2) — null content, blank content
```

总计：**48 new tests, 0 failures** (全量 597 测试通过)

## 10. 安全边界说明

1. 路径逃逸防护 — `resolveAndValidatePath()` 确保 resolvedPath.startsWith(root)
2. 敏感路径禁止 — .env, .git, *.pem, *.key, node_modules/, target/ 等
3. allowPrefixes — 默认限制在 backend/src, frontend/src, docs, scripts, deploy, .github/workflows
4. 二进制文件跳过 — NUL byte 检测 + 28 种扩展名
5. 大文件截断 — 默认 128KB 单文件 / 256KB 总输出
6. Secret 脱敏 — API Key/Token/Password 替换为 **REDACTED**
7. 无 Git 写操作 — 不 checkout/pull/fetch/reset/apply/commit/push
8. 无文件写入 — filesTouched=[] 始终不变
9. payload 安全 — toSafeJobResponse 不返回 payload 原文

## 11. 验证

```bash
# Backend
cd backend && mvn test
# → 597 tests, 0 failures

# Frontend
cd frontend && npm run typecheck && npm run build
# → typecheck pass, build pass
```

## 12. 是否可以进入 Milestone 38A

**是**。Milestone 37E 已完成全部要求：

- ✅ ReadOnlyRepositoryAdapter 接口存在
- ✅ LocalWorkspaceReadOnlyAdapter 实现存在
- ✅ 敏感路径被拒绝
- ✅ 二进制文件被跳过
- ✅ 大文件被截断
- ✅ secret-like 内容被脱敏
- ✅ outputPayload 包含 filesRead/skippedFiles/redacted/truncated
- ✅ outputPayload 包含 filesTouched=[] 和 gitOperations=[]
- ✅ Code Index 不保存 secret 原文（脱敏后入库）
- ✅ 前端显示 read-only safety note
- ✅ 后端测试 ≥ 28 个（实际 48 个）
- ✅ E2E 测试 selector 已更新
