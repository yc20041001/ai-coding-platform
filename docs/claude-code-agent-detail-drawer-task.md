# Claude Code Task: Agent Detail Drawer

## Goal

Implement a read-only Agent detail drawer in the frontend so users can click an Agent in the Agent list and inspect the runtime configuration that is actually used by task execution.

This is the next step after adding runtime Agent configuration support:

- Built-in Agent runtime prompts and policies are stored in `ai_agent_version`.
- Task execution now resolves an Agent version and records `agent_execution.agent_version_id`.
- The Agent list page still only shows basic metadata, so users cannot inspect `system_prompt`, `tool_policy`, or `execution_policy`.

## Audience

Claude Code should execute this as a focused implementation task. Do not redesign the Agent module or add editing/version publishing yet.

## Current Source Truth

Read these files first:

- `frontend/src/modules/agent/pages/AgentListPage.vue`
- `frontend/src/modules/agent/api.ts`
- `backend/src/main/java/com/aicoding/platform/agent/controller/AgentController.java`
- `backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java`
- `backend/src/main/java/com/aicoding/platform/agent/dto/AgentDetailResponse.java`
- `frontend/src/modules/task/components/AgentExecutionDrawer.vue`
- `backend/src/main/resources/db/migration/V12__upgrade_builtin_agent_runtime_configs.sql`
- `backend/src/main/resources/db/migration/V13__add_agent_version_to_execution.sql`

Important repository note:

- This project currently ignores `*.sql` via `.gitignore`.
- If staging migration files later, use `git add -f` for `V12` and `V13`.
- Do not remove or rewrite unrelated existing uncommitted changes.

## Functional Requirements

### 1. Agent List Interaction

Update `frontend/src/modules/agent/pages/AgentListPage.vue`:

- Make each Agent row clickable.
- On row click, open a right-side drawer.
- Drawer title should show the Agent name and code.
- Keep the existing list layout and visual style.
- Preserve existing loading and empty states.

### 2. Agent Detail Drawer

Add a new component:

```text
frontend/src/modules/agent/components/AgentDetailDrawer.vue
```

The drawer must fetch detail data using:

```text
GET /api/agents/{agentId}
```

Display these sections:

- Overview
  - id
  - name
  - code
  - type
  - status
  - description
- Latest Version
  - latestVersion.id
  - latestVersion.versionNo
  - latestVersion.status
  - latestVersion.modelConfigId
- System Prompt
- Tool Policy
- Execution Policy

Use read-only display only. No editing controls.

For JSON policy fields:

- Pretty-print valid JSON with indentation.
- If parsing fails, show the raw string.
- Keep long content scrollable.

Use existing shared components where appropriate:

- `StatusPulse`
- `EmptyState`
- `ErrorState`
- Existing drawer styling patterns from `AgentExecutionDrawer.vue`

### 3. Backend Detail API Must Include `systemPrompt`

Currently `AgentDetailResponse` exposes:

```text
toolPolicy
executionPolicy
```

but it does not expose:

```text
systemPrompt
```

Add `systemPrompt` to:

- `backend/src/main/java/com/aicoding/platform/agent/dto/AgentDetailResponse.java`
- `backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java`

`getAgentDetail` should return the latest published version prompt through `systemPrompt`.

Do not expose secrets. The Agent prompt and policies are configuration, not credentials.

### 4. Frontend API Types

Update `frontend/src/modules/agent/api.ts`.

Current `AgentDetail` type is too small. Replace or extend it to match backend response:

```ts
export interface AgentVersionInfo {
  id: string
  versionNo: string
  modelConfigId: string | null
  status: string
}

export interface AgentDetail {
  id: string
  name: string
  code: string
  type: string
  status: string
  description: string | null
  avatar: string | null
  latestVersion: AgentVersionInfo | null
  modelConfigId: string | null
  systemPrompt: string | null
  toolPolicy: string | null
  executionPolicy: string | null
}
```

Keep `listAgents` unchanged unless type fixes are necessary.

## Non-Goals

Do not implement:

- Agent creation UI
- Agent editing UI
- Agent version publishing
- Model config editing
- Project-level enable/disable UI
- Tool execution runtime
- Real code-writing tools

This task is only for read-only inspection.

## Suggested Implementation Steps

1. Backend response
   - Add `systemPrompt` field to `AgentDetailResponse`.
   - Populate it in `AgentApplicationService.toAgentDetailResponse(...)`.

2. Frontend API type
   - Update `AgentDetail` in `frontend/src/modules/agent/api.ts`.

3. Drawer component
   - Create `AgentDetailDrawer.vue`.
   - Props:
     - `agentId: string | null`
     - `visible: boolean`
   - Emits:
     - `close`
   - Watch `visible` and `agentId`.
   - Fetch detail when opened.
   - Reset stale detail when closed or load fails.

4. Agent list page integration
   - Add `selectedAgentId` and `detailVisible`.
   - Add table row click handler.
   - Render `AgentDetailDrawer`.
   - Use cursor styling to make row click discoverable.

5. Verification
   - Run backend targeted tests or at least compile:
     ```bash
     cd backend
     mvn -Dtest=TaskOrchestratorIntegrationTest test
     ```
   - Run frontend typecheck:
     ```bash
     cd frontend
     npm run typecheck
     ```
   - If frontend is running at `http://localhost:5173`, manually verify:
     - Agent list loads.
     - Clicking `Backend Agent` opens drawer.
     - Drawer shows `systemPrompt`.
     - Drawer shows `toolPolicy` and `executionPolicy`.
     - Closing and opening another Agent does not show stale data.

## Acceptance Criteria

- `GET /api/agents/{agentId}` returns `systemPrompt` for the latest published version.
- Frontend typecheck passes.
- Clicking an Agent row opens a detail drawer.
- Drawer shows basic Agent metadata.
- Drawer shows latest version metadata.
- Drawer shows `systemPrompt`, `toolPolicy`, and `executionPolicy`.
- JSON policy fields are readable and scrollable.
- Loading and error states are handled.
- No editing or mutation behavior is introduced.

## Expected Files To Change

Likely files:

```text
backend/src/main/java/com/aicoding/platform/agent/dto/AgentDetailResponse.java
backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java
frontend/src/modules/agent/api.ts
frontend/src/modules/agent/pages/AgentListPage.vue
frontend/src/modules/agent/components/AgentDetailDrawer.vue
```

Optional test files if adding coverage:

```text
backend/src/test/java/com/aicoding/platform/...
frontend/e2e/...
```

## Output Required From Claude Code

When finished, Claude Code should report:

- Changed files
- What behavior was added
- Verification commands run
- Any skipped verification and why
- Remaining risks or follow-up work
