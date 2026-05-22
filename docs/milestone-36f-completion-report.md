# Milestone 36F: Sandbox Worker Queue — Completion Report

## 1. New / Modified File List

### New Backend Files
| File | Description |
|------|-------------|
| `backend/src/main/resources/db/migration/V25__init_tool_execution_job_tables.sql` | Create `tool_execution_job` table |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobStatus.java` | Enum: PENDING, RUNNING, COMPLETED, FAILED, CANCELED |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobPriority.java` | Enum: LOW, NORMAL, HIGH |
| `backend/src/main/java/com/aicoding/platform/orchestration/domain/ToolExecutionJobEntity.java` | MyBatis-Plus entity with ASSIGN_ID |
| `backend/src/main/java/com/aicoding/platform/orchestration/infrastructure/ToolExecutionJobMapper.java` | MyBatis-Plus mapper |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/ToolExecutionJobResponse.java` | Response DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/dto/RetryToolExecutionJobRequest.java` | Retry request DTO |
| `backend/src/main/java/com/aicoding/platform/orchestration/application/ToolExecutionJobService.java` | Core service: create, drain, retry, cancel, query |
| `backend/src/main/java/com/aicoding/platform/orchestration/controller/ToolExecutionJobController.java` | REST controller |
| `backend/src/test/java/com/aicoding/platform/orchestration/ToolExecutionJobIntegrationTest.java` | 20 backend tests |

### Modified Backend Files
| File | Change |
|------|--------|
| `ToolSandboxExecutionResponse.java` | Added `jobId`, `job` fields |
| `ToolSandboxExecutionService.java` | Creates jobs via `ToolExecutionJobService.createJob()`, drains via `drainMockJob()` |
| `ToolExecutionStatus.java` | Added `CANCELED` |

### New Frontend Files
None (all changes within existing files).

### Modified Frontend Files
| File | Change |
|------|--------|
| `frontend/src/modules/task/api.ts` | Added `ToolExecutionJob` interface, `RetryToolExecutionJobRequest`, 5 API functions, `jobId`/`job` fields on `ToolSandboxExecutionResponse` |
| `frontend/src/modules/task/components/MultiAgentRunPanel.vue` | Job status badge, retryCount, duration, detail expand/collapse, retry/cancel buttons |
| `frontend/e2e/multi-agent-orchestration.spec.ts` | 4 new E2E tests for job status display |

## 2. `tool_execution_job` Table

```sql
CREATE TABLE tool_execution_job (
  id              BIGINT    PRIMARY KEY,
  project_id      BIGINT    NOT NULL,
  task_id         BIGINT,
  run_id          BIGINT,
  step_id         BIGINT,
  tool_execution_id BIGINT,
  tool_key        VARCHAR(64),
  status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  priority        VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  retry_count     INT       NOT NULL DEFAULT 0,
  max_retry_count INT       NOT NULL DEFAULT 2,
  request_payload JSON,
  result_payload  JSON,
  last_error      TEXT,
  locked_by       VARCHAR(64),
  locked_at       DATETIME,
  started_at      DATETIME,
  finished_at     DATETIME,
  duration_ms     BIGINT,
  create_time     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_project_time (project_id, create_time),
  INDEX idx_task (task_id),
  INDEX idx_run (run_id),
  INDEX idx_step (step_id),
  INDEX idx_execution (tool_execution_id),
  INDEX idx_status (status),
  INDEX idx_tool (tool_key)
);
```

## 3. ToolExecutionJobService Design

- **createJob(execution, requestPayload)**: Creates PENDING job with NORMAL priority, retryCount=0, maxRetryCount=2, writes TOOL_JOB_CREATED task log.
- **drainMockJob(jobId)**: Validates PENDING → marks RUNNING → resolves parameters → builds mock output → for MOCK_PATCH_PROPOSAL: creates PatchProposalArtifact → marks COMPLETED. Writes TOOL_JOB_RUNNING / TOOL_JOB_COMPLETED logs.
- **retryJob(jobId, request)**: Only FAILED/CANCELED jobs, checks retryCount < maxRetryCount, creates new PENDING job with incremented retryCount, calls drainMockJob, writes TOOL_JOB_RETRIED log.
- **cancelJob(jobId)**: Only PENDING/RUNNING jobs, marks CANCELED, sets execution CANCELED, writes TOOL_JOB_CANCELED log.
- **Queries**: getJob (VIEWER+), listByExecution, listByRun, getLatestJobByExecution.

## 4. Sync Mock Drain

The "Worker Queue" is synchronous and in-process — no RabbitMQ, Redis Queue, Kafka, or async workers:

1. `ToolSandboxExecutionService` creates a PENDING job via `ToolExecutionJobService.createJob()`
2. Immediately calls `drainMockJob(jobId)` which:
   - Marks job RUNNING
   - Resolves tool parameters (via injected ToolParameterSchemaService)
   - Builds mock output payload (no real shell/git/file operations)
   - For MOCK_PATCH_PROPOSAL: creates task artifact
   - Marks job COMPLETED + execution COMPLETED
3. All in the same transaction, same thread

This provides the database-backed job infrastructure without operational complexity.

## 5. Retry / Cancel Rules

| Operation | Valid States | Behavior |
|-----------|-------------|----------|
| Retry | FAILED, CANCELED | Creates new PENDING job with retryCount++, then drains |
| Cancel | PENDING, RUNNING | Sets job CANCELED, sets execution CANCELED |
| Retry limit reached | retryCount >= maxRetryCount | CONFLICT error |

## 6. ToolSandboxExecutionService Integration

- `createCompletedExecution()`: Now creates execution as RUNNING, creates PENDING job via `toolExecutionJobService.createJob()`, drains via `drainMockJob()`, returns refreshed entity.
- `approveAndExecute()`: After marking approval APPROVED, sets execution RUNNING, creates job, drains, returns refreshed entity.
- `toResponse()`: Loads latest job via `getLatestJobByExecution()` and sets `jobId`/`job` on response.

## 7. Backend API List

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tool-execution-jobs/{jobId}` | Get job detail |
| GET | `/api/tool-sandbox-executions/{executionId}/jobs` | List jobs for execution |
| GET | `/api/multi-agent-runs/{runId}/tool-execution-jobs` | List jobs for run |
| POST | `/api/tool-execution-jobs/{jobId}/retry` | Retry failed/canceled job |
| POST | `/api/tool-execution-jobs/{jobId}/cancel` | Cancel pending/running job |

## 8. Frontend Job Status Display

- **Job status badge**: Shown in tool card header, e.g. "Job 已完成" / "Job 失败"
- **Retry count**: "retry 1/2" shown when retryCount > 0
- **Job duration**: "Job 42ms" displayed separately from tool duration
- **Job detail toggle**: Click "Job 详情" to expand requestPayload/resultPayload/lastError
- **Retry button**: Shown when job.status === FAILED, calls `POST /api/tool-execution-jobs/{jobId}/retry`
- **Cancel button**: Shown when job.status === PENDING || RUNNING, calls `POST /api/tool-execution-jobs/{jobId}/cancel`
- **E2E data-testid**: `tool-job-status`, `tool-job-retry-count`, `tool-job-duration`, `tool-job-detail`, `tool-job-request-payload`, `tool-job-result-payload`, `btn-retry-tool-job`, `btn-cancel-tool-job`

## 9. Safety Boundaries

- **No real execution**: drainMockJob produces only mock/simulated output. No shell commands, no Git writes, no file writes.
- **Approval enforcement**: MOCK_PATCH_PROPOSAL still requires approval before job creation.
- **Permission checks**: retry/cancel enforce OWNER/MAINTAINER role; queries enforce VIEWER+.
- **No async infrastructure**: All operations are synchronous database transactions.
- **All existing tests pass**: 398 backend tests, 0 failures.

## 10. Backend Test Results

```
Tests run: 398, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

ToolExecutionJobIntegrationTest (20 tests):
- Job Creation/Drain (7 tests): verify COMPLETED status, jobId in response, payloads, task logs
- Approval + Job (4 tests): WAITING_APPROVAL → approve → job created → COMPLETED → artifact
- Job API (4 tests): GET detail, GET by execution, GET by run, unauthorized, NOT_FOUND
- Retry/Cancel (5 tests): FAILED retry, retry limit, PENDING cancel, COMPLETED cancel rejected

## 11. Frontend Quality Gates

- **typecheck**: Passed (0 errors)
- **build**: Passed

## 12. Known Limitations

1. **Synchronous drain**: All jobs are drained immediately in the same thread. No async worker pool.
2. **No locking**: `locked_by`/`locked_at` fields exist but are not used (no concurrent workers).
3. **Mock execution only**: `drainMockJob` produces fake output. Real execution will come in later milestones.
4. **Retry creates new row**: Each retry inserts a new `tool_execution_job` row. The old job retains its FAILED/CANCELED status.

## 13. Ready for Milestone 36G

All 398 backend tests pass, frontend typecheck and build succeed, E2E coverage includes 4 new job status tests. The database-backed job queue infrastructure is complete and integrated with the existing tool sandbox, approval, and patch proposal flows.
