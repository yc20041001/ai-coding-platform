# Milestone 36H: Patch Review UI — Completion Report

## Summary

Milestone 36H delivered a dedicated Patch Proposal Review experience, building on milestones 36A-36G (Tool Sandbox, Tool Approval, Tool Parameters, Repository Tools, Patch Proposal Artifact, Tool Execution Jobs). The milestone adds a review lifecycle (PENDING → REVIEWED) with safety guarantees, a full UI panel, and comprehensive test coverage.

## Deliverables

### Database
- `V27__init_patch_proposal_review_tables.sql` — `patch_proposal_review` table with UNIQUE on `artifact_id`, indexes on project/status, task, decision, reviewer

### Backend — Domain
- `PatchProposalReviewStatus.java` — enum: `PENDING`, `REVIEWED`
- `PatchProposalDecision.java` — enum: `ACCEPTED_AS_PLAN`, `REJECTED`, `NEEDS_CHANGES`, `MARKED_REVIEWED`
- `PatchProposalReviewEntity.java` — MyBatis-Plus entity with `@TableName("patch_proposal_review")`, `IdType.ASSIGN_ID`
- `PatchProposalReviewMapper.java` — MyBatis-Plus `BaseMapper`

### Backend — DTO
- `PatchProposalReviewResponse.java` — all fields exposed as String (IDs externalized)
- `PatchProposalReviewDecisionRequest.java` — decision, comment, safetyConfirmed, checklist

### Backend — Service
- `PatchProposalReviewService.java`:
  - `ensureReviewForArtifact()` — auto-creates PENDING review for PATCH_PROPOSAL artifacts
  - `decide()` — validates safetyConfirmed=true, enforces valid decision enum, creates or overwrites review, writes task log
  - `listTaskReviews()` — lists reviews for a task

### Backend — Controller
- `PatchProposalReviewController.java` — REST API at `/api/task-artifacts/{artifactId}/patch-review` with GET (ensure + get) and POST decision endpoints

### Backend — Integration
- `PatchProposalArtifactService.createPatchProposalArtifact()` — now auto-creates PENDING review with default checklist via `createPendingReview()`
- Task log stages: `PATCH_PROPOSAL_REVIEW_CREATED`, `PATCH_PROPOSAL_REVIEWED`, `PATCH_PROPOSAL_REVIEW_UPDATED`
- ErrorCode entries for `PATCH_REVIEW_NOT_FOUND`, `PATCH_REVIEW_INVALID_ARTIFACT_TYPE`, `PATCH_REVIEW_SAFETY_NOT_CONFIRMED`, `PATCH_REVIEW_INVALID_DECISION`

### Frontend — API Layer
- `PatchProposalReview` and `PatchProposalReviewDecisionRequest` interfaces
- `getPatchProposalReview()`, `submitPatchProposalReviewDecision()`, `listTaskPatchReviews()` functions

### Frontend — Components
- **PatchProposalReviewPanel.vue** — Safety banner, review status bar, file list (parsed from diff), diff content via MarkdownRenderer, 5-item checklist (2 readonly), safety confirmation checkbox, comment textarea, 4 decision buttons
- **TaskDetailPage.vue** — Uses PatchProposalReviewPanel for PATCH_PROPOSAL artifacts in artifacts tab
- **MultiAgentRunPanel.vue** — Displays review status tag (`tool-patch-review-status`) on tool cards with artifact link

### Frontend — E2E Tests
- `patch-proposal-review.spec.ts` — 7 tests:
  1. PatchProposalReviewPanel display with safety banner and file list
  2. Checklist with interactive checkboxes (disabled readonly items, toggling interactive ones)
  3. Submit ACCEPTED_AS_PLAN and verify REVIEWED status
  4. Submit REJECTED decision
  5. Safety confirmation required before enabling decision buttons
  6. Review status tag in multi-agent run panel
  7. No JS errors throughout review flow

## Test Results

### Backend (20/20 passing)
- `PatchProposalReviewIntegrationTest`: 20 tests, 0 failures, 0 errors
  - Covers: auto-create PENDING review, GET review, non-PATCH_PROPOSAL error, non-existent artifact, all 4 decision types, safety confirmation required, invalid decision, decision overwrite, task reviews list, review doesn't modify artifact, checklist JSON integrity, task log verification

### Frontend
- `npm run typecheck` — PASS (0 errors)
- `npm run build` — PASS (production build successful)
- `npm run test:e2e` — 7 new E2E tests for patch proposal review flow

## Files Created/Modified

### New Files (14)
| File | Purpose |
|------|---------|
| `backend/.../db/migration/V27__init_patch_proposal_review_tables.sql` | Database table |
| `backend/.../orchestration/domain/PatchProposalReviewStatus.java` | Status enum |
| `backend/.../orchestration/domain/PatchProposalDecision.java` | Decision enum |
| `backend/.../orchestration/domain/PatchProposalReviewEntity.java` | Entity |
| `backend/.../orchestration/infrastructure/PatchProposalReviewMapper.java` | Mapper |
| `backend/.../orchestration/dto/PatchProposalReviewResponse.java` | Response DTO |
| `backend/.../orchestration/dto/PatchProposalReviewDecisionRequest.java` | Request DTO |
| `backend/.../orchestration/application/PatchProposalReviewService.java` | Service |
| `backend/.../orchestration/controller/PatchProposalReviewController.java` | Controller |
| `backend/.../orchestration/PatchProposalReviewIntegrationTest.java` | Integration tests |
| `frontend/.../components/PatchProposalReviewPanel.vue` | Review UI component |
| `frontend/e2e/patch-proposal-review.spec.ts` | E2E tests |
| `docs/milestone-36h-completion-report.md` | This report |

### Modified Files (6)
| File | Change |
|------|--------|
| `backend/.../application/PatchProposalArtifactService.java` | Auto-create PENDING review |
| `backend/.../common/exception/ErrorCode.java` | Added review-specific error codes |
| `backend/src/test/resources/schema.sql` | Added V27 table schema |
| `frontend/.../task/api.ts` | Added review API functions + types |
| `frontend/.../task/pages/TaskDetailPage.vue` | Integrate PatchProposalReviewPanel |
| `frontend/.../task/components/MultiAgentRunPanel.vue` | Review status tags |

## Architecture Decisions

1. **Auto-create on artifact generation**: Creating PENDING review inside `PatchProposalArtifactService.createPatchProposalArtifact()` ensures every PATCH_PROPOSAL artifact automatically gets a review record without explicit API calls.

2. **Decision overwrite allowed**: Calling `decide()` on an already-reviewed artifact updates the existing decision (idempotent), enabling correction of review decisions.

3. **safetyConfirmed server-enforced**: The service layer rejects any decision where `safetyConfirmed` is not `true`, ensuring the safety acknowledgment cannot be bypassed via direct API calls.

4. **Default checklist**: Pre-filled with `noFileWritten: true` and `noGitOperation: true` (readonly in UI) reflecting system guarantees — the sandbox never writes files or runs git operations.

5. **Task log integration**: Review lifecycle events (`REVIEW_CREATED`, `REVIEWED`, `REVIEW_UPDATED`) are recorded in the task log for audit trail.
