# Milestone 37J: Incident Knowledge Base & Root Cause Notes

## Summary

Implemented the Incident Knowledge Base & Root Cause Notes feature set, completing 13 API endpoints across 4 services, with 3 new database tables, 4 enums, 3 entities, 3 mappers, 12 DTOs, frontend components, 47 backend integration tests, and 8 E2E tests.

## Deliverables

### Database (V36 Migration)
- `tool_incident_root_cause_note` — Root cause analysis records per incident
- `tool_known_issue_template` — Pre-defined templates for common incident patterns  
- `tool_incident_knowledge_link` — Associations between incidents and knowledge documents

### Backend Components

**Enums (4):**
- `IncidentRootCauseNoteStatus` — DRAFT, REVIEWED, PUBLISHED, ARCHIVED
- `IncidentRootCauseConfidence` — LOW, MEDIUM, HIGH, CONFIRMED
- `KnownIssueCategory` — 12 categories (TOOL_POLICY, READ_ONLY_ADAPTER, CODE_INDEX, WORKER_QUEUE, RABBITMQ, REDIS, MODEL_GATEWAY, GITHUB, RAG, FRONTEND, CONFIGURATION, UNKNOWN)
- `IncidentKnowledgeLinkType` — GENERATED_FROM_INCIDENT, RELATED_DOCUMENT, MANUAL_LINK, KNOWN_ISSUE_TEMPLATE

**Entities & Mappers (3 each):**
- `ToolIncidentRootCauseNoteEntity` / `ToolIncidentRootCauseNoteMapper`
- `ToolKnownIssueTemplateEntity` / `ToolKnownIssueTemplateMapper`
- `ToolIncidentKnowledgeLinkEntity` / `ToolIncidentKnowledgeLinkMapper`

**DTOs (12):**
- `CreateIncidentRootCauseNoteRequest`, `UpdateIncidentRootCauseNoteRequest`, `IncidentRootCauseNoteResponse`
- `CreateKnownIssueTemplateRequest`, `UpdateKnownIssueTemplateRequest`, `KnownIssueTemplateResponse`
- `GenerateIncidentKnowledgeDocumentRequest`, `IncidentKnowledgeDocumentDraftResponse`
- `SimilarIncidentResponse`
- `IncidentKnowledgeLinkResponse`, `IncidentKnowledgeSummaryResponse`

**Services (4):**
- `IncidentRootCauseService` — Create/update/get/list root cause notes, status transitions (DRAFT→REVIEWED→PUBLISHED→ARCHIVED), apply template, markdown export
- `KnownIssueTemplateService` — CRUD for known issue templates, filter by category/enabled
- `IncidentKnowledgeService` — Generate knowledge documents from incidents, list/delete knowledge links
- `SimilarIncidentSearchService` — Mock similarity search with scoring (title=0.95, rootCause=0.90, resolution=0.85, summary=0.75, tags=0.70, default=0.50)

**Controller (1):**
- `IncidentKnowledgeController` — 13 REST endpoints:
  - `POST /api/orchestration/incidents/{id}/root-cause-note`
  - `PUT /api/orchestration/incident-root-cause-notes/{id}`
  - `GET /api/orchestration/incidents/{id}/root-cause-note`
  - `GET /api/projects/{id}/incident-root-cause-notes`
  - `GET /api/orchestration/incident-root-cause-notes/{id}/markdown`
  - `POST /api/projects/{id}/known-issue-templates`
  - `PUT /api/orchestration/known-issue-templates/{id}`
  - `GET /api/projects/{id}/known-issue-templates`
  - `POST /api/orchestration/incidents/{id}/apply-known-issue-template/{templateId}`
  - `POST /api/orchestration/incidents/{id}/knowledge-document`
  - `GET /api/orchestration/incidents/{id}/knowledge-links`
  - `DELETE /api/orchestration/incident-knowledge-links/{id}`
  - `GET /api/orchestration/incidents/{id}/similar`

### Quality Gates

**Backend Tests — 47 integration tests** (`IncidentKnowledgeIntegrationTest`)
- Root cause notes: create, create with defaults, duplicate conflict, nonexistent incident
- Root cause get: success, not found
- Root cause update: content update, status transitions (REVIEWED, PUBLISHED), archived reject, invalid transition, REVIEWED→DRAFT, nonexistent note
- Root cause list: by project, by status, empty
- Markdown export: full, missing fields, nonexistent
- Templates: create, missing title, update, get, list by category, filter by enabled, nonexistent update
- Apply template: success, conflict with existing note, nonexistent template
- Knowledge generation: with note, missing knowledgeBaseId, without note
- Knowledge links: list, empty, delete, nonexistent delete
- Similar incident search: by query, no match, by incident, score descending, limit, nonexistent incident
- Edge cases: create after archive, confidence update, template with all fields

**Frontend Components (3 new):**
- `IncidentRootCauseEditor.vue` — Create/edit/view root cause notes with status transitions
- `KnownIssueTemplatePanel.vue` — CRUD panel for known issue templates
- `SimilarIncidentList.vue` — Display similar incidents with search and scoring

**E2E Tests — 8 tests** (`incident-knowledge.spec.ts`)
- Observability page incident section display
- Incident detail drawer with root cause info
- Root cause note section in drawer
- Similar incidents section in drawer
- Knowledge links section in drawer
- Known issue template section
- Drawer navigation

**Test Schema**
- V36 tables added to test schema.sql

### Architecture Decisions
- Status flow: DRAFT → REVIEWED → PUBLISHED → ARCHIVED (with REVIEWED→DRAFT allowed, ARCHIVED terminal)
- One active root cause note per incident (CONFLICT on duplicate)
- Knowledge document generation reuses existing `KnowledgeDocumentApplicationService.uploadDocument()`
- Similar incident search uses MySQL LIKE-based mock scoring (no vector DB)
- IDs use MyBatis-Plus ASSIGN_ID strategy
- Permissions: MAINTAINER+ for writes, VIEWER+ for reads

### Build & Test Results
- Backend: 47 new integration tests pass (total including legacy exceeds 800)
- Frontend: TypeScript check clean, build successful
- E2E: 8 tests created
