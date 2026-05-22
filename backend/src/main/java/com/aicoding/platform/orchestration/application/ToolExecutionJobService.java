package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionErrorCode;
import com.aicoding.platform.orchestration.domain.ToolExecutionFailureStage;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobPriority;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.domain.ToolExecutionStatus;
import com.aicoding.platform.orchestration.domain.ToolName;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.dto.RetryToolExecutionJobRequest;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobResponse;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.orchestration.worker.ToolExecutionJobPublisher;
import com.aicoding.platform.orchestration.worker.ToolExecutionRetryPolicy;
import com.aicoding.platform.orchestration.worker.ToolWorkerProperties;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolExecutionJobService {

    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final ProjectPermissionService projectPermissionService;
    private final PatchProposalArtifactService patchProposalArtifactService;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final ToolParameterSchemaService toolParameterSchemaService;
    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final RepositoryReadToolService repositoryReadToolService;
    private final ToolWorkerProperties toolWorkerProperties;
    private final ToolExecutionJobPublisher jobPublisher;
    private final ToolExecutionRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final java.util.Set<String> REPOSITORY_TOOL_KEYS = java.util.Set.of(
            com.aicoding.platform.orchestration.domain.ToolName.READ_REPOSITORY_TREE.name(),
            com.aicoding.platform.orchestration.domain.ToolName.READ_FILE_SNIPPET.name(),
            com.aicoding.platform.orchestration.domain.ToolName.READ_DIFF_SUMMARY.name(),
            com.aicoding.platform.orchestration.domain.ToolName.READ_BRANCH_INFO.name()
    );

    public ToolExecutionJobService(ToolExecutionJobMapper toolExecutionJobMapper,
                                    ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                    ProjectPermissionService projectPermissionService,
                                    PatchProposalArtifactService patchProposalArtifactService,
                                    AiTaskLogMapper aiTaskLogMapper,
                                    ToolParameterSchemaService toolParameterSchemaService,
                                    ToolCatalogMapper toolCatalogMapper,
                                    ProjectToolConfigMapper projectToolConfigMapper,
                                    RepositoryReadToolService repositoryReadToolService,
                                    ToolWorkerProperties toolWorkerProperties,
                                    ToolExecutionJobPublisher jobPublisher,
                                    ToolExecutionRetryPolicy retryPolicy) {
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.projectPermissionService = projectPermissionService;
        this.patchProposalArtifactService = patchProposalArtifactService;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.toolParameterSchemaService = toolParameterSchemaService;
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
        this.repositoryReadToolService = repositoryReadToolService;
        this.toolWorkerProperties = toolWorkerProperties;
        this.jobPublisher = jobPublisher;
        this.retryPolicy = retryPolicy;
    }

    // ========================
    // Create
    // ========================

    @Transactional
    public ToolExecutionJobEntity createJob(ToolSandboxExecutionEntity execution, String requestPayload) {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(execution.getProjectId());
        job.setTaskId(execution.getTaskId());
        job.setRunId(execution.getRunId());
        job.setStepId(execution.getStepId());
        job.setToolExecutionId(execution.getId());
        job.setToolKey(execution.getToolName());
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority(ToolExecutionJobPriority.NORMAL.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload(requestPayload);
        toolExecutionJobMapper.insert(job);

        writeTaskLog(execution, "TOOL_JOB_CREATED",
                "工具执行 Job 已创建: " + execution.getToolName() + ", jobId=" + job.getId());

        return job;
    }

    // ========================
    // Execute Job (sync or async based on mode)
    // ========================

    @Transactional
    public ToolExecutionJobEntity executeJob(ToolSandboxExecutionEntity execution, String requestPayload) {
        ToolExecutionJobEntity job = createJob(execution, requestPayload);
        if (toolWorkerProperties.isAsyncMode()) {
            jobPublisher.publish(job);
            writeTaskLog(execution, "TOOL_JOB_PUBLISHED",
                    "工具执行 Job 已发布到异步队列: " + execution.getToolName() + ", jobId=" + job.getId());
            return job;
        } else {
            return drainMockJob(job.getId());
        }
    }

    // ========================
    // Sync Mock Drain
    // ========================

    @Transactional
    public ToolExecutionJobEntity drainMockJob(Long jobId) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }
        if (!ToolExecutionJobStatus.PENDING.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "Job 当前状态不允许执行，状态: " + job.getStatus());
        }

        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(job.getToolExecutionId());
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "关联的工具执行记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        // Mark job RUNNING
        job.setStatus(ToolExecutionJobStatus.RUNNING.name());
        job.setStartedAt(now);
        toolExecutionJobMapper.updateById(job);

        writeTaskLog(execution, "TOOL_JOB_RUNNING",
                "工具执行 Job 运行中: " + execution.getToolName());

        // Resolve parameters for output building
        Map<String, Object> parameters = resolveParameters(execution.getProjectId(), execution.getToolName());
        String paramSummary = buildParameterSummaryJson(parameters);

        // Simulate execution (mock, no real shell/git/file write)
        long durationMs = 5 + (long) (Math.random() * 20);
        LocalDateTime finishedAt = now.plusNanos(durationMs * 1_000_000);

        // Build result payload for the job
        String resultPayload = buildJobResultPayload();
        job.setResultPayload(resultPayload);

        // For MOCK_PATCH_PROPOSAL, create artifact before marking COMPLETED
        if (ToolName.MOCK_PATCH_PROPOSAL.name().equals(execution.getToolName())) {
            // Set execution COMPLETED before artifact creation (required by PatchProposalArtifactService)
            execution.setStatus(ToolExecutionStatus.COMPLETED.name());
            execution.setSummary("Mock 工具执行完成（审批通过后执行）：" + execution.getToolName()
                    + "，只读模拟，无文件写入，无 Git 操作。");
            execution.setOutputPayload(buildPatchProposalOutputPayload("pending", paramSummary));
            execution.setFinishedAt(finishedAt);
            execution.setDurationMs(durationMs);
            toolSandboxExecutionMapper.updateById(execution);

            try {
                com.aicoding.platform.task.domain.AiTaskArtifactEntity artifact =
                        patchProposalArtifactService.createPatchProposalArtifact(execution);
                execution.setArtifactId(artifact.getId());
                execution.setOutputPayload(buildPatchProposalOutputPayload(artifact.getId().toString(), paramSummary));
                execution.setSummary("Mock 补丁方案已生成并保存为任务产物（审批通过），仅供审阅，未应用到文件系统。");
                toolSandboxExecutionMapper.updateById(execution);

                writeTaskLog(execution, "PATCH_PROPOSAL_CREATED",
                        "Mock Patch Proposal 产物已生成，仅供审阅，未应用到文件系统。");
            } catch (Exception e) {
                // If artifact already exists, keep existing artifact info
                if (execution.getArtifactId() != null) {
                    execution.setOutputPayload(
                            buildPatchProposalOutputPayload(execution.getArtifactId().toString(), paramSummary));
                    toolSandboxExecutionMapper.updateById(execution);
                }
            }
        } else if (REPOSITORY_TOOL_KEYS.contains(execution.getToolName())) {
            // Repository read-only tool: use RepositoryReadToolService
            Map<String, Object> repoParams = resolveParameters(execution.getProjectId(), execution.getToolName());
            RepositoryReadToolService.RepositoryToolResult repoResult =
                    repositoryReadToolService.executeReadOnlyTool(execution.getProjectId(), execution.getToolName(), repoParams);

            execution.setOutputPayload(repoResult.getOutputPayload());
            execution.setSummary(repoResult.getSummary());
            execution.setStatus(ToolExecutionStatus.COMPLETED.name());
            execution.setFinishedAt(finishedAt);
            execution.setDurationMs(durationMs);
            execution.setErrorMessage(null);
            toolSandboxExecutionMapper.updateById(execution);

            writeTaskLog(execution, "REPOSITORY_TOOL_COMPLETED",
                    "仓库只读工具执行完成: " + execution.getToolName()
                            + ", filesRead=" + (repoResult.getFilesRead() != null ? repoResult.getFilesRead().size() : 0));
        } else {
            // Standard mock output
            execution.setOutputPayload(buildMockOutputPayload(paramSummary));
            execution.setSummary("Mock 工具执行完成：" + execution.getToolName()
                    + "，只读模拟，无文件写入，无 Git 操作。");
            execution.setStatus(ToolExecutionStatus.COMPLETED.name());
            execution.setFinishedAt(finishedAt);
            execution.setDurationMs(durationMs);
            execution.setErrorMessage(null);
            toolSandboxExecutionMapper.updateById(execution);
        }

        // Mark job COMPLETED
        job.setStatus(ToolExecutionJobStatus.COMPLETED.name());
        job.setFinishedAt(finishedAt);
        job.setDurationMs(durationMs);
        toolExecutionJobMapper.updateById(job);

        writeTaskLog(execution, "TOOL_JOB_COMPLETED",
                "工具执行 Job 完成: " + execution.getToolName() + ", durationMs=" + durationMs);

        return job;
    }

    // ========================
    // Retry
    // ========================

    @Transactional
    public ToolExecutionJobResponse retryJob(Long jobId, RetryToolExecutionJobRequest request) {
        ToolExecutionJobEntity oldJob = toolExecutionJobMapper.selectById(jobId);
        if (oldJob == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }

        projectPermissionService.checkProjectRole(oldJob.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        String status = oldJob.getStatus();
        if (!ToolExecutionJobStatus.FAILED.name().equals(status)
                && !ToolExecutionJobStatus.CANCELED.name().equals(status)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "只有 FAILED 或 CANCELED 的 Job 可以重试，当前状态: " + status);
        }

        if (oldJob.getRetryCount() >= oldJob.getMaxRetryCount()) {
            throw new BizException(ErrorCode.CONFLICT,
                    "Job 已达最大重试次数 " + oldJob.getMaxRetryCount());
        }

        // Create new job for retry
        ToolExecutionJobEntity newJob = new ToolExecutionJobEntity();
        newJob.setProjectId(oldJob.getProjectId());
        newJob.setTaskId(oldJob.getTaskId());
        newJob.setRunId(oldJob.getRunId());
        newJob.setStepId(oldJob.getStepId());
        newJob.setToolExecutionId(oldJob.getToolExecutionId());
        newJob.setToolKey(oldJob.getToolKey());
        newJob.setStatus(ToolExecutionJobStatus.PENDING.name());
        newJob.setPriority(oldJob.getPriority());
        newJob.setRetryCount(oldJob.getRetryCount() + 1);
        newJob.setMaxRetryCount(oldJob.getMaxRetryCount());
        newJob.setRequestPayload(oldJob.getRequestPayload());
        toolExecutionJobMapper.insert(newJob);

        // Write task log
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(oldJob.getToolExecutionId());
        if (execution != null) {
            String reason = request != null ? request.getReason() : null;
            writeTaskLog(execution, "TOOL_JOB_RETRIED",
                    "工具执行 Job 重试: " + oldJob.getToolKey()
                            + ", newJobId=" + newJob.getId()
                            + ", retryCount=" + newJob.getRetryCount()
                            + (reason != null ? ", reason=" + reason : ""));
        }

        // Execute or publish based on mode
        if (toolWorkerProperties.isAsyncMode()) {
            jobPublisher.publish(newJob);
            if (execution != null) {
                writeTaskLog(execution, "TOOL_JOB_PUBLISHED",
                        "工具执行 Job 重试已发布到异步队列: " + newJob.getToolKey() + ", newJobId=" + newJob.getId());
            }
            return toResponse(newJob);
        } else {
            return toResponse(drainMockJob(newJob.getId()));
        }
    }

    // ========================
    // Cancel
    // ========================

    @Transactional
    public ToolExecutionJobResponse cancelJob(Long jobId) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }

        projectPermissionService.checkProjectRole(job.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        String status = job.getStatus();
        if (!ToolExecutionJobStatus.PENDING.name().equals(status)
                && !ToolExecutionJobStatus.RUNNING.name().equals(status)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "只有 PENDING 或 RUNNING 的 Job 可以取消，当前状态: " + status);
        }

        LocalDateTime now = LocalDateTime.now();

        // Mark job CANCELED
        job.setStatus(ToolExecutionJobStatus.CANCELED.name());
        job.setFinishedAt(now);
        job.setDurationMs(0L);
        toolExecutionJobMapper.updateById(job);

        // Mark execution CANCELED if not yet completed
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(job.getToolExecutionId());
        if (execution != null) {
            execution.setStatus(ToolExecutionStatus.CANCELED.name());
            execution.setErrorMessage("工具执行已被取消");
            execution.setFinishedAt(now);
            execution.setDurationMs(0L);
            toolSandboxExecutionMapper.updateById(execution);

            writeTaskLog(execution, "TOOL_JOB_CANCELED",
                    "工具执行 Job 已取消: " + execution.getToolName());
        }

        return toResponse(job);
    }

    // ========================
    // markFailedWithRetry
    // ========================

    @Transactional
    public ToolExecutionJobResponse markFailedWithRetry(Long jobId, ToolExecutionErrorCode errorCode,
                                                         ToolExecutionFailureStage stage, String message) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }

        job.setErrorCode(errorCode.name());
        job.setFailureStage(stage != null ? stage.name() : null);
        job.setLastError(message);
        job.setFinishedAt(LocalDateTime.now());

        if (retryPolicy.canRetry(job)) {
            job.setStatus(ToolExecutionJobStatus.RETRY_PENDING.name());
            job.setNextRetryAt(retryPolicy.nextRetryAt(job));
        } else {
            job.setStatus(ToolExecutionJobStatus.DEAD_LETTERED.name());
            job.setDeadLetteredAt(LocalDateTime.now());
            job.setDeadLetterReason(message);
        }

        toolExecutionJobMapper.updateById(job);

        return toResponse(job);
    }

    // ========================
    // moveToDeadLetter
    // ========================

    @Transactional
    public ToolExecutionJobResponse moveToDeadLetter(Long jobId, String reason) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ToolExecutionJobStatus.DEAD_LETTERED.name());
        job.setDeadLetteredAt(now);
        job.setDeadLetterReason(reason);
        job.setFinishedAt(now);
        toolExecutionJobMapper.updateById(job);

        return toResponse(job);
    }

    // ========================
    // listFailedJobs
    // ========================

    @Transactional(readOnly = true)
    public List<ToolExecutionJobResponse> listFailedJobs(Long projectId, String status) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        LambdaQueryWrapper<ToolExecutionJobEntity> wrapper = new LambdaQueryWrapper<ToolExecutionJobEntity>()
                .eq(ToolExecutionJobEntity::getProjectId, projectId);

        if (status != null && !status.isBlank()) {
            wrapper.eq(ToolExecutionJobEntity::getStatus, status);
        } else {
            wrapper.in(ToolExecutionJobEntity::getStatus,
                    ToolExecutionJobStatus.FAILED.name(),
                    ToolExecutionJobStatus.RETRY_PENDING.name(),
                    ToolExecutionJobStatus.DEAD_LETTERED.name());
        }

        wrapper.orderByDesc(ToolExecutionJobEntity::getCreateTime)
                .last("LIMIT 100");

        return toolExecutionJobMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========================
    // manualRetry
    // ========================

    @Transactional
    public ToolExecutionJobResponse manualRetry(Long jobId, RetryToolExecutionJobRequest request) {
        ToolExecutionJobEntity oldJob = toolExecutionJobMapper.selectById(jobId);
        if (oldJob == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }

        projectPermissionService.checkProjectRole(oldJob.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        String status = oldJob.getStatus();
        if (!ToolExecutionJobStatus.FAILED.name().equals(status)
                && !ToolExecutionJobStatus.DEAD_LETTERED.name().equals(status)
                && !ToolExecutionJobStatus.CANCELED.name().equals(status)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "只有 FAILED、DEAD_LETTERED 或 CANCELED 的 Job 可以手动重试，当前状态: " + status);
        }

        // Create new job for retry with sourceJobId linking to original
        ToolExecutionJobEntity newJob = new ToolExecutionJobEntity();
        newJob.setProjectId(oldJob.getProjectId());
        newJob.setTaskId(oldJob.getTaskId());
        newJob.setRunId(oldJob.getRunId());
        newJob.setStepId(oldJob.getStepId());
        newJob.setToolExecutionId(oldJob.getToolExecutionId());
        newJob.setToolKey(oldJob.getToolKey());
        newJob.setStatus(ToolExecutionJobStatus.PENDING.name());
        newJob.setPriority(oldJob.getPriority());
        newJob.setRetryCount(oldJob.getRetryCount() + 1);
        newJob.setMaxRetryCount(oldJob.getMaxRetryCount());
        newJob.setRequestPayload(oldJob.getRequestPayload());
        newJob.setSourceJobId(oldJob.getId());
        toolExecutionJobMapper.insert(newJob);

        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(oldJob.getToolExecutionId());
        if (execution != null) {
            String reason = request != null ? request.getReason() : null;
            writeTaskLog(execution, "TOOL_JOB_MANUAL_RETRY",
                    "工具执行 Job 手动重试: " + oldJob.getToolKey()
                            + ", oldJobId=" + oldJob.getId()
                            + ", newJobId=" + newJob.getId()
                            + (reason != null ? ", reason=" + reason : ""));
        }

        if (toolWorkerProperties.isAsyncMode()) {
            jobPublisher.publish(newJob);
        } else {
            return toResponse(drainMockJob(newJob.getId()));
        }

        return toResponse(newJob);
    }

    // ========================
    // recoverTimedOutRunningJobs
    // ========================

    @Transactional
    public int recoverTimedOutRunningJobs(Duration timeout) {
        if (timeout == null) {
            timeout = Duration.ofSeconds(toolWorkerProperties.getRunningTimeoutSeconds());
        }

        LocalDateTime threshold = LocalDateTime.now().minus(timeout);

        List<ToolExecutionJobEntity> timedOutJobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getStatus, ToolExecutionJobStatus.RUNNING.name())
                        .and(w -> w.isNotNull(ToolExecutionJobEntity::getLockedAt)
                                .or(iw -> iw.isNotNull(ToolExecutionJobEntity::getStartedAt)))
                        .apply("(locked_at < {0} OR started_at < {0})", threshold)
                        .last("LIMIT 50"));

        int count = 0;
        for (ToolExecutionJobEntity job : timedOutJobs) {
            try {
                markFailedWithRetry(job.getId(), ToolExecutionErrorCode.TIMEOUT,
                        ToolExecutionFailureStage.MOCK_EXECUTE, "Job 执行超时");
                count++;
            } catch (Exception e) {
                // skip individual failure
            }
        }

        return count;
    }

    // ========================
    // dispatchRetries
    // ========================

    @Transactional
    public int dispatchRetries() {
        LocalDateTime now = LocalDateTime.now();

        List<ToolExecutionJobEntity> pendingRetries = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getStatus, ToolExecutionJobStatus.RETRY_PENDING.name())
                        .and(w -> w.isNotNull(ToolExecutionJobEntity::getNextRetryAt)
                                .and(iw -> iw.le(ToolExecutionJobEntity::getNextRetryAt, now)))
                        .last("LIMIT 50"));

        int count = 0;
        for (ToolExecutionJobEntity job : pendingRetries) {
            try {
                job.setStatus(ToolExecutionJobStatus.PENDING.name());
                job.setNextRetryAt(null);
                job.setErrorCode(null);
                job.setFailureStage(null);
                toolExecutionJobMapper.updateById(job);

                if (toolWorkerProperties.isAsyncMode()) {
                    jobPublisher.publish(job);
                } else {
                    drainMockJob(job.getId());
                }
                count++;
            } catch (Exception e) {
                // skip individual failure
            }
        }

        return count;
    }

    // ========================
    // Queries
    // ========================

    @Transactional(readOnly = true)
    public ToolExecutionJobResponse getJob(Long jobId) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Job 不存在");
        }
        projectPermissionService.checkProjectMember(job.getProjectId());
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionJobResponse> listByExecution(Long executionId) {
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        projectPermissionService.checkProjectMember(execution.getProjectId());

        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getToolExecutionId, executionId)
                        .orderByAsc(ToolExecutionJobEntity::getCreateTime));
        return jobs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionJobResponse> listByRun(Long runId) {
        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getRunId, runId)
                        .orderByAsc(ToolExecutionJobEntity::getCreateTime));

        if (!jobs.isEmpty()) {
            projectPermissionService.checkProjectMember(jobs.get(0).getProjectId());
        }

        return jobs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolExecutionJobResponse getLatestJobByExecution(Long executionId) {
        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getToolExecutionId, executionId)
                        .orderByDesc(ToolExecutionJobEntity::getCreateTime)
                        .last("LIMIT 1"));
        if (jobs.isEmpty()) {
            return null;
        }
        return toResponse(jobs.get(0));
    }

    // ========================
    // Parameter resolution
    // ========================

    private Map<String, Object> resolveParameters(Long projectId, String toolNameStr) {
        ToolCatalogEntity tool = toolCatalogMapper.selectOne(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getToolKey, toolNameStr));
        if (tool == null) {
            return new java.util.HashMap<>();
        }

        ProjectToolConfigEntity config = projectToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId)
                        .eq(ProjectToolConfigEntity::getToolId, tool.getId()));

        String parametersJson = config != null ? config.getParametersJson() : null;
        Map<String, Object> rawParameters = new java.util.HashMap<>();
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(parametersJson, java.util.Map.class);
                if (parsed != null) {
                    rawParameters = parsed;
                }
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        return toolParameterSchemaService.normalizeAndValidate(
                tool.getParameterSchemaJson(), rawParameters);
    }

    private String buildParameterSummaryJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) return "";
        return toolParameterSchemaService.buildParameterSummary(parameters);
    }

    // ========================
    // Output builders
    // ========================

    private String buildJobResultPayload() {
        return "{"
                + "\"mock\":true,"
                + "\"jobCompleted\":true,"
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[]"
                + "}";
    }

    private String buildMockOutputPayload(String paramSummary) {
        String summary = paramSummary != null && !paramSummary.isBlank()
                ? ",\"parameterSummary\":\"" + paramSummary.replace("\"", "\\\"") + "\""
                : "";
        return "{"
                + "\"mock\":true,"
                + "\"readOnly\":true,"
                + "\"parametersApplied\":true,"
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[],"
                + "\"findings\":["
                + "\"Mock sandbox scanned task context.\","
                + "\"No real filesystem or git operation was executed.\""
                + "]"
                + summary
                + "}";
    }

    private String buildPatchProposalOutputPayload(String artifactId, String paramSummary) {
        String summary = paramSummary != null && !paramSummary.isBlank()
                ? ",\"parameterSummary\":\"" + paramSummary.replace("\"", "\\\"") + "\""
                : "";
        return "{"
                + "\"mock\":true,"
                + "\"readOnly\":true,"
                + "\"applied\":false,"
                + "\"artifactId\":\"" + artifactId + "\","
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[],"
                + "\"safety\":\"Proposal only. No real file or git operation was executed.\""
                + summary
                + "}";
    }

    // ========================
    // Helpers
    // ========================

    private void writeTaskLog(ToolSandboxExecutionEntity execution, String stage, String message) {
        if (execution.getTaskId() == null) return;
        AiTaskLogEntity taskLog = new AiTaskLogEntity();
        taskLog.setTaskId(execution.getTaskId());
        taskLog.setProjectId(execution.getProjectId());
        taskLog.setLevel(TaskLogLevel.INFO.name());
        taskLog.setStage(stage);
        taskLog.setMessage(message);
        aiTaskLogMapper.insert(taskLog);
    }

    public ToolExecutionJobResponse toResponse(ToolExecutionJobEntity entity) {
        ToolExecutionJobResponse resp = new ToolExecutionJobResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setStepId(entity.getStepId() != null ? entity.getStepId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId() != null ? entity.getToolExecutionId().toString() : null);
        resp.setToolKey(entity.getToolKey());
        resp.setStatus(entity.getStatus());
        resp.setPriority(entity.getPriority());
        resp.setRetryCount(entity.getRetryCount());
        resp.setMaxRetryCount(entity.getMaxRetryCount());
        resp.setRequestPayload(entity.getRequestPayload());
        resp.setResultPayload(entity.getResultPayload());
        resp.setLastError(entity.getLastError());
        resp.setErrorCode(entity.getErrorCode());
        resp.setFailureStage(entity.getFailureStage());
        resp.setNextRetryAt(entity.getNextRetryAt() != null ? entity.getNextRetryAt().toString() : null);
        resp.setDeadLetteredAt(entity.getDeadLetteredAt() != null ? entity.getDeadLetteredAt().toString() : null);
        resp.setDeadLetterReason(entity.getDeadLetterReason());
        resp.setSourceJobId(entity.getSourceJobId() != null ? entity.getSourceJobId().toString() : null);
        resp.setStartedAt(entity.getStartedAt() != null ? entity.getStartedAt().toString() : null);
        resp.setFinishedAt(entity.getFinishedAt() != null ? entity.getFinishedAt().toString() : null);
        resp.setDurationMs(entity.getDurationMs());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return resp;
    }
}
