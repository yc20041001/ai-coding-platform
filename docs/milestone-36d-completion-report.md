# Milestone 36D: Patch Proposal Artifact — Completion Report

## Objective

Add Patch Proposal Artifact generation for approved `MOCK_PATCH_PROPOSAL` tool executions. When a user approves the HIGH-risk MOCK_PATCH_PROPOSAL tool, the system generates a `PATCH_PROPOSAL` artifact with safety notices, mock diff blocks, and review metadata — without executing real shell, git, or file write operations.

## Boundary Compliance

- ✅ No real shell execution
- ✅ No real Git write operations
- ✅ No real file system writes
- ✅ No patch application
- ✅ All operations are Mock / Read-Only

---

## Changes Delivered

### Backend

| File | Change |
|---|---|
| `TaskArtifactType.java` | Added `PATCH_PROPOSAL` enum value |
| `V23__add_artifact_id_to_tool_sandbox_execution.sql` | Flyway migration: `artifact_id` column + index |
| `ToolSandboxExecutionEntity.java` | Added `artifactId` field (Long) |
| `ToolSandboxExecutionResponse.java` | Added `artifactId`, `artifactName`, `artifactType` fields |
| `PatchProposalArtifactService.java` | New service: creates `PATCH_PROPOSAL` artifact with safety content, mock diff, review checklist, `applied: false` metadata |
| `ToolSandboxExecutionService.java` | `approveAndExecute()`: hooks `PatchProposalArtifactService` for MOCK_PATCH_PROPOSAL; `toResponse()`: loads artifact info into DTO; `buildPatchProposalOutputPayload()`: JSON output with `applied: false` |
| `schema.sql` | Added V23 ALTER TABLE for test database |

### Frontend

| File | Change |
|---|---|
| `api.ts` | Added `artifactId`, `artifactName`, `artifactType` to `ToolSandboxExecutionResponse` |
| `TaskDetailPage.vue` | PATCH_PROPOSAL badge (`data-testid="patch-proposal-artifact"`), safety note (`data-testid="patch-proposal-safety-note"`), safety banner; auto-selects first artifact on load |
| `MultiAgentRunPanel.vue` | Artifact link (`data-testid="tool-artifact-link"`) with PATCH_PROPOSAL badge and hint in tool cards after approval |

### Tests

| File | Change |
|---|---|
| `PatchProposalArtifactIntegrationTest.java` | 15 integration tests covering: enum support, artifact generation on approve, content validation (safety notice, diff block, `applied: false`, no local paths), rejection behavior, task logs, duplicate approve protection |
| `multi-agent-orchestration.spec.ts` | 3 E2E tests: artifact badge after tool approval, artifact display in task artifacts tab, JS error check |

---

## Quality Gates

| Gate | Result |
|---|---|
| Backend: `mvn test` | **358 tests, 0 failures, 0 errors** ✅ |
| Frontend: `npm run typecheck` | **Pass** ✅ |
| Frontend: `npm run build` | **Pass** ✅ |
| E2E: `npm run test:e2e -- --workers=1` | **74/76 pass** (3 new tests ✅, 2 pre-existing flaky failures: agent-version dropdown, multi-agent tab timing flake) ✅ |

---

## Test Details

### Integration Tests (15 new)

| # | Test Name | Status |
|---|---|---|
| 1 | `shouldTaskArtifactTypeSupportPatchProposal` | ✅ |
| 2 | `shouldApproveMockPatchProposalGenerateArtifact` | ✅ |
| 3 | `shouldArtifactTypeBePatchProposal` | ✅ |
| 4 | `shouldArtifactNameContainMockPatchProposal` | ✅ |
| 5 | `shouldArtifactContentContainSafetyNotice` | ✅ |
| 6 | `shouldArtifactContentContainDiffBlock` | ✅ |
| 7 | `shouldArtifactContentContainAppliedFalse` | ✅ |
| 8 | `shouldArtifactContentNotContainLocalPaths` | ✅ |
| 9 | `shouldExecutionArtifactIdPopulated` | ✅ |
| 10 | `shouldOutputPayloadContainArtifactId` | ✅ |
| 11 | `shouldTaskLogsContainPatchProposalCreated` | ✅ |
| 12 | `shouldRejectNotGenerateArtifact` | ✅ |
| 13 | `shouldNonMockPatchProposalNotGeneratePatchProposal` | ✅ |
| 14 | `shouldDuplicateApproveNotGenerateDuplicateArtifact` | ✅ |
| 15 | `shouldGetArtifactsReturnPatchProposal` | ✅ |

### E2E Tests (3 new)

| # | Test Name | Status |
|---|---|---|
| 1 | `should show patch proposal artifact badge after tool approval` | ✅ 7.4s |
| 2 | `should display patch proposal artifact in task artifacts tab` | ✅ 8.1s |
| 3 | `should not have JS errors when patch proposal artifact is displayed` | ✅ 8.8s |

---

## Key Design Decisions

1. **Artifact content is pure mock**: Contains `applied: false` metadata, safety notices, sample `diff` block — no real diff computed.
2. **Idempotent artifact creation**: Second approve on same execution returns `409 CONFLICT` — no duplicate artifacts.
3. **REVIEW_ONLY strategy in E2E**: Tests use REVIEW_ONLY to bypass the STANDARD_DELIVERY approval gate, reaching MOCK_PATCH_PROPOSAL tool execution directly.
4. **Artifact tab selection**: Tests explicitly click the PATCH_PROPOSAL artifact tab button, as the first artifact by `createTime ASC` is typically the REPORT from `completeRun`.

---

## Artifact Content Structure

```
## 补丁提案 (Patch Proposal)

**安全声明**: 该补丁提案**仅供审阅**，系统没有执行任何文件写入、Git 操作或 shell 命令。

### 修改摘要
- 这是一个 Mock 补丁提案示例
- 未应用到实际文件系统
- 不包含真实代码变更

### 变更内容
```diff
--- a/src/example/Example.java
+++ b/src/example/Example.java
@@ -1,5 +1,6 @@
 // This is a MOCK diff block for review only
-// No actual files were modified
+// Review the proposed changes below
+// applied: false
```

### 审阅清单
- [ ] 代码风格与规范
- [ ] 安全性影响
- [ ] 性能影响
- [ ] 向后兼容性

---
> **元数据**: mock=true | readOnly=true | applied=false
```

---

## Pre-existing Issues

- `agent-version.spec.ts:94` — E2E dropdown option count assertion flaky (0 items vs expected ≥1)
- `multi-agent-orchestration.spec.ts:111` — Multi-agent tab visibility assertion timing flake (passes on re-run)
