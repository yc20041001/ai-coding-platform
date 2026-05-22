# Milestone 36I Completion Report: Tool Parameter Advanced Schema

## Summary

Implemented product-grade tool parameter configuration capabilities on top of 36E's flat schema: schemaVersion, groups, dependsOn, array parameters, pathRules (allowlist/denylist), and parameter change audit logging.

## Changes

### Backend

| File | Change |
|------|--------|
| `ToolParameterSchemaService.java` | Complete rewrite (~150 new lines). Added `validateSchema()`, `resolveDependsOn()`, `normalizeArrayValue()`, `validatePathRules()`, `matchGlob()`. Enhanced `normalizeAndValidate()` with array/path/dependsOn handling. |
| `ToolCatalogApplicationService.java` | Added `toolParameterSchemaService.validateSchema()` + parameter change audit log via `TOOL_PARAMETER_UPDATE` action. |
| `PatchProposalArtifactService.java` | Enhanced `buildPatchProposalContent()` with `targetFiles` array display, `testLevel` in Test Suggestions section, controlled by `includeTests`/`testLevel` params. |
| `AuditActionType.java` | Added `TOOL_PARAMETER_UPDATE` enum. |
| `ErrorCode.java` | Added 9 new error codes: `PARAM_SCHEMA_VERSION_UNSUPPORTED`, `PARAM_GROUP_FIELD_NOT_FOUND`, `PARAM_DEPENDS_ON_FIELD_NOT_FOUND`, `PARAM_ARRAY_ITEM_TYPE_INVALID`, `PARAM_ARRAY_MAX_ITEMS_EXCEEDED`, `PARAM_ARRAY_ITEM_TOO_LONG`, `PARAM_PATH_DENIED`, `PARAM_PATH_NOT_ALLOWED`, `PARAM_PATH_INVALID`. |
| `V28__upgrade_tool_parameter_schema_v2.sql` | v2 schema for `MOCK_PATCH_PROPOSAL` (910006) — adds `targetFiles` array, `testLevel` with dependsOn, schemaVersion=2. `READ_FILE_SNIPPET` (910102) — adds file/path groups, pathRules with deny and allowPrefixes, schemaVersion=2. |
| `ToolParameterSchemaIntegrationTest.java` | 44 tests covering schemaVersion, array, dependsOn, pathRules, Patch Proposal content, audit logging, and READ_FILE_SNIPPET groups. |

### Frontend

| File | Change |
|------|--------|
| `tool/api.ts` | Added `ToolParameterGroup`, `ToolParameterDependsOn`, `ToolParameterPathRules` interfaces. Extended `ToolParameterField` with `'array'` type, `itemType`, `maxItems`, `itemMaxLength`, `dependsOn`, `pathRules`. Added `schemaVersion`, `groups` to `ToolParameterSchema`. |
| `ToolParameterForm.vue` | Groups rendering with title/description. Schema version badge. Array UI (add/remove items). dependsOn conditional visibility. pathRules hint block. Ungrouped fields section. |
| `ProjectToolConfigPage.vue` | Parameter summary shows "N 项" for array values. |
| `MultiAgentRunPanel.vue` | Added `getTargetFiles()` to parse targetFiles from tool execution payload. Displays target files in tool card. |
| `project-tool-policy.spec.ts` | Added 6 E2E tests: schema version badge, groups display, array save with count summary, pathRules hint, JS error checks for advanced features. |

## Verification

- **Backend compile**: `mvn compile -q` — clean
- **Frontend typecheck**: `vue-tsc --noEmit` — clean (fixed v-if/v-for conflicts)
- **Frontend build**: `npm run build` — clean
- **Integration tests**: 44/44 pass (6.98s runtime)
- **Frontend E2E**: All existing + 6 new advanced parameter tests added

## Design Decisions

- **path validation order**: absolute path check → `..` / `~` / null-char → deny list (glob) → allowPrefixes. Each stage returns a distinct error code for precise debugging.
- **dependsOn**: simple field equality check only. Hidden fields are dropped from the active parameter set. Required is not enforced when the field is hidden.
- **array**: only `itemType=text` supported. Empty/whitespace-only strings auto-discarded. `maxItems` and `itemMaxLength` enforced.
- **Audit**: `TOOL_PARAMETER_UPDATE` logged only when parameters actually change (old vs new JSON comparison).
