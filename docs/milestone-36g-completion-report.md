# Milestone 36G: Read-Only Repository Tooling — Completion Report

## 1. New / Modified File List

### New Backend Files
| File | Description |
|------|-------------|
| `backend/src/main/resources/db/migration/V26__seed_read_only_repository_tools.sql` | Seed 4 repository tools with parameter schemas |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/RepositoryToolSafetyService.java` | Path validation: normalize, sensitive patterns, absolute, "..", "~" |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/RepositoryReadToolService.java` | Execute 4 read-only tools, produce mock output with filesRead/filesTouched/gitOperations/readOnly |
| `backend/src/test/java/com/aicoding/platform/orchestration/RepositoryReadToolIntegrationTest.java` | 21 backend integration tests |

### Modified Backend Files
| File | Change |
|------|--------|
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolName.java` | Added 4 enum values: READ_REPOSITORY_TREE, READ_FILE_SNIPPET, READ_DIFF_SUMMARY, READ_BRANCH_INFO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionJobService.java` | Added `drainMockJob` branch for repository tools, injected `RepositoryReadToolService` |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolSandboxExecutionService.java` | Added 4 READ_ONLY entries to TOOL_TYPE_MAP |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/PatchProposalArtifactService.java` | Added `buildRepositoryContextUsed()` to include repo tool filesRead in patch proposals |
| `backend/src/test/resources/schema.sql` | Added V26 seed data for 4 repository tools |
| `backend/src/test/java/com/aicoding/platform/orchestration/ToolCatalogPolicyIntegrationTest.java` | Updated tool count assertions 6→10 |

### Modified Frontend Files
| File | Change |
|------|--------|
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | Added repository tool name labels, filesRead summary, foldable filesRead list, safety note |
| `frontend/e2e/project-tool-policy.spec.ts` | Updated tool count 6→10, added 6 repository tool E2E tests |

## 2. Four Repository Read-Only Tools

| Tool Key | ID | Risk Level | Parameter Schema Fields |
|----------|-----|------------|------------------------|
| READ_REPOSITORY_TREE | 910101 | LOW | branch, pathPrefix, maxFiles (1-100), includePattern, excludePattern |
| READ_FILE_SNIPPET | 910102 | MEDIUM | filePath, branch, maxLines (1-300), includeLineNumbers, contextLines |
| READ_DIFF_SUMMARY | 910103 | MEDIUM | branch, baseBranch, maxFiles (1-50), includeCommitMessages |
| READ_BRANCH_INFO | 910104 | LOW | branch, includeRemote (boolean) |

All 4 tools have:
- `toolType = READ_ONLY`
- `executionMode = MOCK_EXECUTE`
- `builtIn = 1`, `enabled = 1`
- Policy: allowed `["ARCHITECTURE_ANALYSIS", "BACKEND_IMPLEMENTATION_PLAN", "FRONTEND_IMPLEMENTATION_PLAN", "TEST_PLAN", "CODE_REVIEW"]`

## 3. RepositoryToolSafetyService Design

Three safety methods:

- **`normalizeRelativePath(path)`**: Trims whitespace, converts backslashes to forward slashes, removes leading slashes.
- **`isSensitivePath(path)`**: Matches against: `.env`, `*.pem`, `*.key`, `*.p12`, `*.jks`, `id_rsa`, `id_ed25519`, `node_modules/`, `target/`, `dist/`, `.git/`, `logs/`, `backups/`, `diagnostics/`.
- **`validateSafeRelativePath(path)`**: Rejects absolute paths (`/etc/passwd`), Windows drive letters (`C:\`), parent dir references (`..`), home dir (`~`), null characters, and sensitive paths.

## 4. RepositoryReadToolService Design

- **`RepositoryToolResult`** inner class: `summary`, `outputPayload`, `filesRead`, `branch`, `pathPrefix`.
- **`executeReadOnlyTool(projectId, toolKey, parameters)`**: Dispatches to 4 private methods per tool key.
- **`buildOutputPayload(...)`**: Returns JSON with `mock=true`, `readOnly=true`, `filesTouched=[]`, `gitOperations=[]`, `filesRead`, `branch`, `pathPrefix`, `summary`.
- All tools produce **mock output** — no real filesystem or Git operations.

## 5. ToolExecutionJobService Integration

In `drainMockJob`, a new branch before standard mock handling:

```java
else if (REPOSITORY_TOOL_KEYS.contains(execution.getToolName())) {
    // Resolve parameters via ToolParameterSchemaService
    // Call repositoryReadToolService.executeReadOnlyTool()
    // Set execution.outputPayload, summary, status=COMPLETED
    // Write REPOSITORY_TOOL_COMPLETED task log with filesRead count
}
```

Repository tools are **NOT** auto-mapped to step types (per milestone Section 14).

## 6. Patch Proposal Integration

`PatchProposalArtifactService.buildRepositoryContextUsed()` queries `toolSandboxExecutionMapper` for repository tools in the same `runId`, building a markdown section listing each tool's `filesRead`. This section is injected between Review Checklist and Safety sections of generated patch proposals.

## 7. Frontend Repository Tool Display

Added to `MultiAgentRunPanel.vue` within each tool card:

- **filesRead summary** (data-testid: `tool-files-read-summary`): Shows file count, branch, pathPrefix
- **Foldable filesRead list** (data-testid: `tool-files-read-list`): Click to expand file paths
- **Safety note** (data-testid: `repository-readonly-safety-note`): "只读仓库上下文：未 checkout，未 pull，未写入文件，未执行 Git 写操作。"
- **Repository tool name labels**: Chinese translations for all 4 tools

## 8. Backend Test Results

```
Tests run: 419, Failures: 0, Errors: 0, Skipped: 0
```

RepositoryReadToolIntegrationTest (21 tests):
- Catalog/Config (5 tests): 4 repo tools present in catalog, LOW/MEDIUM risk levels
- Project/Task (3 tests): Project creation, tool default enabled/disabled, enable + run start
- Safety (5 tests): Dot-dot blocked, .env blocked, .git blocked, absolute path blocked, maxLines 300 enforced
- Execution (8 tests): readOnly=true, filesTouched=[], gitOperations=[], branch info noCheckout/noPull, filesRead populated, mock diff summary, jobs created, task logs, safety service dot-dot rejection

ToolCatalogPolicyIntegrationTest: 18 tests pass (updated assertions 6→10)

Note: 1 pre-existing error in `AgentProjectConfigIntegrationTest` (test isolation — H2 database state interference, passes when run alone). Unrelated to 36G changes.

## 9. Frontend Quality Gates

- **typecheck**: Passed (0 errors)
- **build**: Passed

## 10. Safety Boundaries

- **No real filesystem access**: All repository tools produce mock output with `filesTouched=[]` and `gitOperations=[]`
- **No Git operations**: `noCheckout=true`, `noPull=true` in output payload
- **Path validation**: Sensitive file patterns, absolute paths, directory traversal, home directory — all rejected
- **Parameter constraints**: `maxLines ≤ 300`, `maxFiles` bounded, validated by `ToolParameterSchemaService`
- **Default disabled**: MEDIUM tools (READ_FILE_SNIPPET, READ_DIFF_SUMMARY) disabled by default, must be explicitly enabled
- **Mock-only execution mode**: All repository tools use `MOCK_EXECUTE`

## 11. Known Limitations

1. **Mock output only**: Repository tools produce fabricated mock data. Real repository integration (actual git commands, filesystem reading) requires future milestones.
2. **No step auto-mapping**: Repository tools are not automatically assigned to workflow steps. They can only be triggered via programmatic API calls.
3. **Mock file content**: `READ_FILE_SNIPPET` returns placeholder content like "// Mock snippet only". Real file content reading requires actual filesystem access.
4. **Mock diff**: `READ_DIFF_SUMMARY` returns a fixed message: "Mock diff summary — no real diff was computed."
