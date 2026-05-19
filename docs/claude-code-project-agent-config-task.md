# Claude Code Task: Project Agent Configuration

## Goal

Implement project-level Agent configuration so a project owner can see which built-in Agents are enabled for a project, choose the Agent version/model config used by that project, and enable or disable Agents from the project workspace.

This builds on the current Agent runtime work:

- Global Agents are stored in `ai_agent`.
- Runtime prompts and policies are stored in `ai_agent_version`.
- Project overrides are stored in `project_agent_config`.
- Task execution now resolves Agent version in this order:
  1. request `agentVersionId`
  2. task `agentVersionId`
  3. `project_agent_config.agent_version_id`
  4. latest `PUBLISHED` Agent version

The missing piece is a project UI/API for managing `project_agent_config`.

## Scope

Implement a read/write project Agent configuration panel.

This task should add the smallest usable slice:

- Backend API to list project Agent configs.
- Frontend API bindings.
- New project tab/page to manage project Agents.
- Enable/disable actions.
- Show selected version/model config info.

Do not implement full Agent authoring, version publishing, or prompt editing in this task.

## Current Source Truth

Read these files first:

- `backend/src/main/java/com/aicoding/platform/agent/controller/AgentController.java`
- `backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java`
- `backend/src/main/java/com/aicoding/platform/agent/domain/AiAgentEntity.java`
- `backend/src/main/java/com/aicoding/platform/agent/domain/AiAgentVersionEntity.java`
- `backend/src/main/java/com/aicoding/platform/agent/domain/ProjectAgentConfigEntity.java`
- `backend/src/main/java/com/aicoding/platform/agent/dto/EnableProjectAgentRequest.java`
- `backend/src/main/java/com/aicoding/platform/orchestrator/application/AgentOrchestratorService.java`
- `frontend/src/modules/agent/api.ts`
- `frontend/src/modules/agent/pages/AgentListPage.vue`
- `frontend/src/modules/project/pages/ProjectDetailPage.vue`
- `frontend/src/app/router/index.ts`
- `frontend/src/modules/model/api.ts`

Important repository note:

- The workspace may already contain unrelated uncommitted changes. Do not revert or rewrite them.
- `*.sql` is ignored by `.gitignore`; if adding migration files, stage with `git add -f`.
- Prefer using existing Element Plus and shared components.

## Backend Requirements

### 1. Add Project Agent Config Response DTO

Add a DTO, for example:

```text
backend/src/main/java/com/aicoding/platform/agent/dto/ProjectAgentConfigResponse.java
```

Suggested fields:

```java
private String projectId;
private String agentId;
private String agentName;
private String agentCode;
private String agentType;
private String agentStatus;
private String agentDescription;
private Boolean enabled;
private String projectAgentConfigId;
private String agentVersionId;
private String agentVersionNo;
private String modelConfigId;
private String configJson;
private String updateTime;
```

Notes:

- Return one row per global Agent.
- If a project has no config row for an Agent, return `enabled=false` and still include latest published version info.
- If a project has a config row, return its enabled state, selected version, selected model config, and config JSON.

### 2. Add List API

Add:

```text
GET /api/projects/{projectId}/agents
```

Behavior:

- Requires project membership: `OWNER`, `MAINTAINER`, `DEVELOPER`, or `VIEWER`.
- Returns all global Agents with project-specific config state.
- Sort order should match existing Agent list order by type/code/name.

Example response shape:

```json
{
  "code": "OK",
  "data": [
    {
      "projectId": "205...",
      "agentId": "300002",
      "agentName": "Backend Agent",
      "agentCode": "backend-agent",
      "agentType": "BACKEND",
      "agentStatus": "ENABLED",
      "agentDescription": "Generate backend APIs...",
      "enabled": true,
      "projectAgentConfigId": "206...",
      "agentVersionId": "310002",
      "agentVersionNo": "1.0.0",
      "modelConfigId": null,
      "configJson": null,
      "updateTime": "2026-..."
    }
  ]
}
```

### 3. Harden Enable API

Existing endpoint:

```text
POST /api/projects/{projectId}/agents/{agentId}/enable
```

Keep it, but verify:

- Requires project `OWNER`.
- Rejects disabled or missing global Agent.
- Resolves `agentVersionId` to the latest published version if omitted.
- Rejects a version that does not belong to the Agent.
- Rejects a non-`PUBLISHED` version.
- Stores optional `modelConfigId` and `configJson`.

If any of those checks are missing, add them.

### 4. Disable API

Existing endpoint:

```text
POST /api/projects/{projectId}/agents/{agentId}/disable
```

Keep behavior:

- Requires project `OWNER`.
- If config exists, set `enabled=0`.
- If no config exists, return success without creating one.

### 5. Optional Model Config Validation

If low risk, validate `modelConfigId` exists when provided.

Do not implement model config assignment UI deeply in this task if it expands scope too much. Displaying and sending the selected ID is enough.

## Frontend Requirements

### 1. Add Project Agents API

Update:

```text
frontend/src/modules/agent/api.ts
```

Add types and functions:

```ts
export interface ProjectAgentConfig {
  projectId: string
  agentId: string
  agentName: string
  agentCode: string
  agentType: string
  agentStatus: string
  agentDescription: string | null
  enabled: boolean
  projectAgentConfigId: string | null
  agentVersionId: string | null
  agentVersionNo: string | null
  modelConfigId: string | null
  configJson: string | null
  updateTime: string | null
}

export interface EnableProjectAgentPayload {
  agentVersionId?: string
  modelConfigId?: string
  configJson?: string
}

export function listProjectAgents(projectId: string)
export function enableProjectAgent(projectId: string, agentId: string, payload: EnableProjectAgentPayload)
export function disableProjectAgent(projectId: string, agentId: string)
```

### 2. Add Project Agents Page

Add:

```text
frontend/src/modules/agent/pages/ProjectAgentConfigPage.vue
```

Page behavior:

- Load `GET /api/projects/{projectId}/agents`.
- Show a table or compact cards for all Agents.
- For each Agent, show:
  - name/code
  - type
  - global status
  - project enabled state
  - version number
  - model config id or `Default`
  - update time
- Owner can enable or disable.
- Non-owner users should still see read-only state if backend allows read. If frontend does not know role, show controls and rely on backend errors, but display error messages cleanly.

Controls:

- Enable button for disabled Agents.
- Disable button for enabled Agents.
- Optional version/model config fields in a small dialog before enabling.

Keep first slice simple:

- If no version picker exists, enable with empty payload and let backend choose latest published version.
- If model config list is easy to load from existing `model/api.ts`, allow selecting a model config. Otherwise show model config as read-only and leave selection for follow-up.

### 3. Add Project Tab

Update:

```text
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/app/router/index.ts
```

Add a new project tab:

```text
agents / 智能体
```

Route:

```text
/projects/:projectId/agents
```

Component:

```text
@/modules/agent/pages/ProjectAgentConfigPage.vue
```

Update route-to-tab mapping so this tab highlights correctly.

## Non-Goals

Do not implement:

- Global Agent create/edit UI.
- Agent version publishing UI.
- Prompt editing UI.
- Full model config management UI.
- Tool execution runtime.
- Real code modification tools.
- Multi-Agent workflow execution.
- Git commit/push/PR automation.

This task is only project-level Agent enable/disable/config visibility.

## Acceptance Criteria

- `GET /api/projects/{projectId}/agents` returns all global Agents with project config state.
- Viewers/developers can read project Agent config if they are project members.
- Only project owners can enable or disable Agents.
- Enabling an Agent creates or updates `project_agent_config`.
- Disabling an Agent sets `enabled=false`.
- If `agentVersionId` is omitted, backend resolves latest published version.
- Invalid Agent version is rejected if it belongs to a different Agent or is not published.
- Frontend has a project `智能体` tab.
- Project Agent page displays all Agents and their project enabled state.
- Enable/disable buttons update the UI after success.
- Loading, empty, error, and permission failure states are handled.
- Existing global `/agents` page still works.
- Existing task execution tests still pass.

## Suggested Verification

Backend:

```bash
cd backend
mvn -Dtest=TaskOrchestratorIntegrationTest test
```

Frontend:

```bash
cd frontend
npm run typecheck
```

Manual UI check if dev server is available:

```text
http://localhost:5173/projects/{projectId}/agents
```

Verify:

- Page loads all 6 built-in Agents.
- Enabling Backend Agent succeeds.
- Disabling Backend Agent succeeds.
- Refresh preserves state.
- Non-owner permission errors are displayed cleanly if tested.

## Expected Files To Change

Likely backend files:

```text
backend/src/main/java/com/aicoding/platform/agent/controller/AgentController.java
backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java
backend/src/main/java/com/aicoding/platform/agent/dto/ProjectAgentConfigResponse.java
```

Likely frontend files:

```text
frontend/src/modules/agent/api.ts
frontend/src/modules/agent/pages/ProjectAgentConfigPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/app/router/index.ts
```

Optional tests:

```text
backend/src/test/java/com/aicoding/platform/agent/...
frontend/e2e/...
```

## Output Required From Claude Code

When finished, Claude Code should report:

- Changed files
- Backend API behavior added
- Frontend behavior added
- Verification commands and results
- Manual UI verification status
- Remaining risks and follow-up work
