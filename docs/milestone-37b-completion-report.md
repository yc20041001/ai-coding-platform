# Milestone 37B — Read-only Code Search Index

## Summary

Implemented a read-only code search index system enabling AI agents to perform code search across indexed project files. Three new tools grant agents code navigation capabilities without file system access.

## Tools Added

| Tool ID | Tool Name | Risk Level | Type |
|---------|-----------|-----------|------|
| 910201 | READ_CODE_INDEX | LOW | READ_ONLY |
| 910202 | SEARCH_CODE_SYMBOL | MEDIUM | READ_ONLY |
| 910203 | SEARCH_CODE_CHUNK | MEDIUM | READ_ONLY |

## Backend Changes

### Database (Flyway)
- **V29**: `code_index_file`, `code_index_symbol`, `code_index_chunk` tables (MySQL with VARCHAR(500) for file_path to stay within utf8mb4 key length limits)
- **V30 (extension)**: Seed data for 3 code search tools with full JSON parameter schemas

### Domain Layer
- `CodeIndexFileEntity`, `CodeIndexSymbolEntity`, `CodeIndexChunkEntity` — MyBatis-Plus entities
- `CodeIndexFileStatus` — enum (INDEXED, FAILED, STALE)
- `CodeSymbolType` — enum (CLASS, METHOD, FUNCTION, INTERFACE, ENUM, COMPONENT, CONSTANT)

### Service Layer
- **CodeIndexBuildService**: Builds mock code index (regex-based symbol extraction, file chunking, content hashing). Generates realistic content per language (Java, TS/JS, Vue, SQL, MD)
- **CodeIndexApplicationService**: Orchestrates build/summary/list/search operations with input validation
- **CodeSymbolExtractorService**: Regex-based symbol extraction (no Tree-sitter). Supports Java (class/method), TypeScript (function/class/interface/component), Vue (SFC setup), SQL (table/index), Markdown (heading)
- **CodeIndexApplicationService.search()**: MySQL LIKE-based search across files, symbols, and chunks with path safety validation

### Controller
- `POST /api/projects/{projectId}/code-index/build` — Build index (requires DEVELOPER+)
- `GET /api/projects/{projectId}/code-index/summary` — Get summary stats (requires member)
- `GET /api/projects/{projectId}/code-index/files` — List indexed files (requires member)
- `GET /api/projects/{projectId}/code-index/symbols` — List extracted symbols (requires member)
- `POST /api/projects/{projectId}/code-index/search` — Search across index (requires member)
- Path traversal protection via `validatePathPrefix()` (.env, .git, node_modules, target, dist blocked)
- `GET /api/projects/{projectId}/code-index/summary` intentionally allows no-auth access

### Worker Integration (ToolSandboxExecutionService)
- `READ_CODE_INDEX` → `codeIndexApplicationService.getSummary()` — returns file/symbol/chunk counts
- `SEARCH_CODE_SYMBOL` → `codeIndexApplicationService.search()` with searchType=SYMBOL
- `SEARCH_CODE_CHUNK` → `codeIndexApplicationService.search()` with searchType=CHUNK
- All output payloads include `filesTouched: []`, `gitOperations: []`, `readOnly: true`, `mock: true`

### Patch Proposal Integration
- `PatchProposalArtifactService` includes code search context in artifact content alongside repository context

### Build Service Features
- Mock file generation: 25 files across Java, TS/TSX, Vue, SQL, MD languages
- Realistic content preamble per language with actual code patterns (not just comments)
- Chunk size: 50 lines per chunk, content hashed with SHA-256

## Frontend Changes

### New Module: `src/modules/code-index/`
- **api.ts**: TypeScript interfaces and API functions for all 5 endpoints
- **CodeIndexPage.vue**: Full page with:
  - Summary section (file/symbol/chunk count cards with mock badge)
  - Build button with loading state
  - Search form (keyword, searchType selector, language filter, pathPrefix, limit)
  - Results cards showing type badge, file path, line ranges, symbol info, snippet
  - Safety note
  - All elements have `data-testid` attributes for E2E testing

### Router & Navigation
- Route added: `ProjectDetail > children > code-index`
- Rail tab: "代码索引" with ◈ icon
- Route mapping → tab rendering

### MultiAgentRunPanel
- Tool name labels for READ_CODE_INDEX, SEARCH_CODE_SYMBOL, SEARCH_CODE_CHUNK
- Code search summary display: keyword, search type, matched files/symbols/chunks/total count
- Safety note display

## Test Coverage

### Backend Integration Tests (19 tests)
- Build: summary, maxFiles, pathPrefix
- Summary: file/symbol/chunk counts
- Files: list, pathPrefix filter
- Symbols: list, language filter
- Search: keyword (ALL/FILE/SYMBOL/CHUNK), required field validation, empty keyword validation, sensitive path prefix validation
- Permissions: unauthenticated reject, summary without auth
- Worker integration: READ_CODE_INDEX tool processing, SEARCH_CODE_SYMBOL tool processing

### E2E Tests (5 tests)
- Tab visibility, empty state, build index, search with results, search type selector

## Quality Gates

- **Backend**: 500 tests, all passing
- **Frontend**: `vue-tsc --noEmit` passes, `npm run build` succeeds

## Key Design Decisions

1. **No Tree-sitter**: Regex-based symbol extraction. Sufficient for mock mode and avoids native dependency complexity
2. **No Vector DB**: MySQL LIKE-based search. Acceptable for codebases under 100K files; avoids infrastructure dependency
3. **No Embeddings**: Keyword search only. Aligns with read-only constraint; no external API calls
4. **Mock Mode**: No real file system scanning. Build service generates realistic mock file structure; all output payloads marked `mock: true`
5. **MySQL utf8mb4 key limit**: file_path VARCHAR(500) instead of VARCHAR(1024) to stay within 3072-byte key length limit
6. **Map.ofEntries()**: Used instead of Map.of() to support 13+ tool type mappings (Java 9 limit of 10 entries)

## Files Changed

### New Files
- `backend/.../controller/CodeIndexController.java`
- `backend/.../application/CodeIndexApplicationService.java`
- `backend/.../application/CodeIndexBuildService.java`
- `backend/.../application/CodeSymbolExtractorService.java`
- `backend/.../domain/CodeIndexChunkEntity.java`
- `backend/.../domain/CodeIndexFileEntity.java`
- `backend/.../domain/CodeIndexFileStatus.java`
- `backend/.../domain/CodeIndexSymbolEntity.java`
- `backend/.../domain/CodeSymbolType.java`
- `backend/.../infrastructure/CodeIndexChunkMapper.java`
- `backend/.../infrastructure/CodeIndexFileMapper.java`
- `backend/.../infrastructure/CodeIndexSymbolMapper.java`
- `backend/.../dto/CodeIndexFileResponse.java`
- `backend/.../dto/CodeIndexSummaryResponse.java`
- `backend/.../dto/CodeIndexSymbolResponse.java`
- `backend/.../dto/CodeSearchRequest.java`
- `backend/.../dto/CodeSearchResponse.java`
- `backend/resources/db/migration/V29__init_code_search_index_tables.sql`
- `backend/src/test/.../CodeSearchIndexIntegrationTest.java`
- `frontend/src/modules/code-index/api.ts`
- `frontend/src/modules/code-index/pages/CodeIndexPage.vue`
- `frontend/e2e/code-index.spec.ts`
- `docs/milestone-37b-completion-report.md`

### Modified Files
- `backend/.../application/ToolSandboxExecutionService.java` — Added 3 code search tools to TOOL_TYPE_MAP
- `backend/.../worker/ToolExecutionWorkerService.java` — Added code search tool dispatch
- `backend/.../application/PatchProposalArtifactService.java` — Added code search context to artifacts
- `backend/.../controller/CodeIndexController.java` — Updated ApiResponse.success()→ok()
- `backend/src/test/resources/schema.sql` — Added V29+V30 tables and seeds
- `frontend/src/app/router/index.ts` — Added code-index route
- `frontend/.../ProjectDetailPage.vue` — Added 代码索引 rail tab
- `frontend/.../MultiAgentRunPanel.vue` — Added code search tool summaries
