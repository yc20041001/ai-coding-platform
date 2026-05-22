# Milestone 37G Completion Report: Tool Execution Audit Export & Operator Review

## Summary

Implements **Tool Execution Audit Export** (Markdown export for single execution, run, and task) and **Tool Operator Review** (CRUD for manual review records on tool executions/jobs/runs/tasks).

## Backend Changes

### New Files

| File | Purpose |
|------|---------|
| `backend/src/main/resources/db/migration/V33__init_tool_operator_review.sql` | Flyway migration for `tool_operator_review` table |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolOperatorReviewStatus.java` | Enum: OPEN, IN_PROGRESS, RESOLVED, WONT_FIX, FALSE_POSITIVE |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolOperatorReviewSeverity.java` | Enum: INFO, LOW, MEDIUM, HIGH, CRITICAL |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolOperatorReviewTargetType.java` | Enum: TOOL_EXECUTION, TOOL_JOB, MULTI_AGENT_RUN, TASK |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolOperatorReviewEntity.java` | MyBatis-Plus entity with ASSIGN_ID, auto-fill timestamps |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolOperatorReviewMapper.java` | MyBatis-Plus mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/CreateToolOperatorReviewRequest.java` | Create request DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/UpdateToolOperatorReviewRequest.java` | Update request DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolOperatorReviewResponse.java` | Response DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolAuditExportResponse.java` | Export response DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionAuditExportService.java` | Markdown export service (3 methods) |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolOperatorReviewService.java` | CRUD service with target validation |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionAuditController.java` | 3 export endpoints |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolOperatorReviewController.java` | 5 review endpoints |

### 8 API Endpoints

**Export:**
- `GET /api/orchestration/executions/{executionId}/audit-export` — single execution Markdown
- `GET /api/orchestration/runs/{runId}/evidence-export` — multi-agent run evidence
- `GET /api/orchestration/tasks/{taskId}/tool-audit-export` — task tool audit

**Review CRUD:**
- `POST /api/orchestration/operator-reviews` — create review (validates target existence)
- `PUT /api/orchestration/operator-reviews/{id}` — update review (auto-sets resolvedAt/resolvedBy on terminal status)
- `GET /api/orchestration/operator-reviews/{id}` — get single review
- `GET /api/projects/{projectId}/operator-reviews` — paginated project reviews (status/severity filters, sortable)
- `GET /api/orchestration/operator-reviews/by-target` — list reviews by targetType + targetId

### Key Design Decisions
- Export **only reads DB trace data** — no tool re-execution, no file re-read
- Operator Review **only records manual processing info** — doesn't change execution results
- Target validation checks existence of: ToolSandboxExecution, ToolExecutionJob, MultiAgentRun, AiTask
- Terminal statuses (RESOLVED, WONT_FIX, FALSE_POSITIVE) auto-set resolvedBy + resolvedAt
- Markdown export uses `ToolTracePayloadSanitizer.sanitize()` for secret masking
- Permission: VIEWER for read, DEVELOPER+ for write

## Frontend Changes

| File | Changes |
|------|---------|
| `frontend/src/modules/task/api.ts` | Added `ToolAuditExport`, `ToolOperatorReview` types + 8 API functions |
| `frontend/src/modules/task/components/ToolExecutionTraceDrawer.vue` | Added export dropdown (execution/run/task), copy summary, create review button |
| `frontend/src/modules/task/components/ToolOperatorReviewDialog.vue` | New dialog for creating operator reviews with severity/title/summary/assignee |

## Test Coverage

- **ToolAuditExportIntegrationTest**: 14 tests (trace export, run evidence, task audit, error cases, permissions)
- **ToolOperatorReviewIntegrationTest**: 25 tests (CRUD, validation, filtering, edge cases)
- **Total: 39 new tests** (≥34 requirement), **zero failures**
- Existing 35 trace tests also pass (74 total orchestration tests, 0 failures)

## Quality Gates

| Gate | Status |
|------|--------|
| `mvn compile` (backend) | ✅ Pass |
| `mvn test` (74 orchestration tests) | ✅ Pass (0 failures) |
| `npm run typecheck` (frontend) | ✅ Pass |
| `npm run build` (frontend) | ✅ Pass |

## Backward Compatibility
- No existing APIs modified
- V33 migration is additive (new table)
- Existing 36A-37F and 35A-35F APIs unchanged
