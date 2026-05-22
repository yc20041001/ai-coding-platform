package com.aicoding.platform.orchestration.worker;

import com.aicoding.platform.orchestration.domain.ToolExecutionErrorCode;
import com.aicoding.platform.orchestration.domain.ToolExecutionFailureStage;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.domain.ToolExecutionStatus;
import com.aicoding.platform.orchestration.domain.ToolName;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.orchestration.application.CodeIndexApplicationService;
import com.aicoding.platform.orchestration.application.PatchProposalArtifactService;
import com.aicoding.platform.orchestration.application.RepositoryReadToolService;
import com.aicoding.platform.orchestration.application.ToolParameterSchemaService;
import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.aicoding.platform.orchestration.dto.CodeSearchRequest;
import com.aicoding.platform.orchestration.dto.CodeSearchResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSummaryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.List;

@Service
public class ToolExecutionWorkerService {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionWorkerService.class);

    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final PatchProposalArtifactService patchProposalArtifactService;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final ToolParameterSchemaService toolParameterSchemaService;
    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final RepositoryReadToolService repositoryReadToolService;
    private final CodeIndexApplicationService codeIndexApplicationService;
    private final ToolExecutionRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> REPOSITORY_TOOL_KEYS = Set.of(
            ToolName.READ_REPOSITORY_TREE.name(),
            ToolName.READ_FILE_SNIPPET.name(),
            ToolName.READ_DIFF_SUMMARY.name(),
            ToolName.READ_BRANCH_INFO.name()
    );

    private static final Set<String> CODE_SEARCH_TOOL_KEYS = Set.of(
            ToolName.READ_CODE_INDEX.name(),
            ToolName.SEARCH_CODE_SYMBOL.name(),
            ToolName.SEARCH_CODE_CHUNK.name()
    );

    public ToolExecutionWorkerService(ToolExecutionJobMapper toolExecutionJobMapper,
                                       ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                       PatchProposalArtifactService patchProposalArtifactService,
                                       AiTaskLogMapper aiTaskLogMapper,
                                       ToolParameterSchemaService toolParameterSchemaService,
                                       ToolCatalogMapper toolCatalogMapper,
                                       ProjectToolConfigMapper projectToolConfigMapper,
                                       RepositoryReadToolService repositoryReadToolService,
                                       CodeIndexApplicationService codeIndexApplicationService,
                                       ToolExecutionRetryPolicy retryPolicy) {
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.patchProposalArtifactService = patchProposalArtifactService;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.toolParameterSchemaService = toolParameterSchemaService;
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
        this.repositoryReadToolService = repositoryReadToolService;
        this.codeIndexApplicationService = codeIndexApplicationService;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    public void process(Long jobId) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            log.warn("Job not found: jobId={}, ignoring", jobId);
            return;
        }

        String status = job.getStatus();
        if (!ToolExecutionJobStatus.PENDING.name().equals(status)) {
            log.info("Job {} is not PENDING (status={}), skipping", jobId, status);
            return;
        }

        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(job.getToolExecutionId());
        if (execution == null) {
            log.warn("Execution not found for jobId={}, executionId={}", jobId, job.getToolExecutionId());
            markJobFailed(job, null, ToolExecutionFailureStage.CONSUME,
                    ToolExecutionErrorCode.JOB_NOT_FOUND, "关联的工具执行记录不存在");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Mark job RUNNING
        job.setStatus(ToolExecutionJobStatus.RUNNING.name());
        job.setStartedAt(now);
        toolExecutionJobMapper.updateById(job);

        writeTaskLog(execution, "TOOL_JOB_RUNNING",
                "工具执行 Job 运行中: " + execution.getToolName());

        try {
            // Resolve parameters
            Map<String, Object> parameters = resolveParameters(execution.getProjectId(), execution.getToolName());
            String paramSummary = buildParameterSummaryJson(parameters);

            // Simulate execution (mock, no real shell/git/file write)
            long durationMs = 5 + (long) (Math.random() * 20);
            LocalDateTime finishedAt = now.plusNanos(durationMs * 1_000_000);

            String resultPayload = buildJobResultPayload();
            job.setResultPayload(resultPayload);

            // Handle different tool types
            if (ToolName.MOCK_PATCH_PROPOSAL.name().equals(execution.getToolName())) {
                handlePatchProposalExecution(execution, paramSummary, finishedAt, durationMs);
            } else if (REPOSITORY_TOOL_KEYS.contains(execution.getToolName())) {
                handleRepositoryToolExecution(execution, finishedAt, durationMs);
            } else if (CODE_SEARCH_TOOL_KEYS.contains(execution.getToolName())) {
                handleCodeSearchExecution(execution, parameters, finishedAt, durationMs);
            } else {
                handleStandardExecution(execution, paramSummary, finishedAt, durationMs);
            }

            // Mark job COMPLETED
            job.setStatus(ToolExecutionJobStatus.COMPLETED.name());
            job.setFinishedAt(finishedAt);
            job.setDurationMs(durationMs);
            toolExecutionJobMapper.updateById(job);

            writeTaskLog(execution, "TOOL_JOB_COMPLETED",
                    "工具执行 Job 完成: " + execution.getToolName() + ", durationMs=" + durationMs);
        } catch (Exception e) {
            log.error("工具执行失败: jobId={}, toolKey={}", jobId, job.getToolKey(), e);
            markJobFailed(job, execution, ToolExecutionFailureStage.MOCK_EXECUTE,
                    ToolExecutionErrorCode.MOCK_EXECUTION_FAILED, e.getMessage());
        }
    }

    private void markJobFailed(ToolExecutionJobEntity job, ToolSandboxExecutionEntity execution,
                                ToolExecutionFailureStage stage, ToolExecutionErrorCode errorCode, String message) {
        LocalDateTime now = LocalDateTime.now();

        job.setErrorCode(errorCode.name());
        job.setFailureStage(stage.name());
        job.setLastError(message);
        job.setFinishedAt(now);

        if (retryPolicy.canRetry(job)) {
            job.setStatus(ToolExecutionJobStatus.RETRY_PENDING.name());
            job.setNextRetryAt(retryPolicy.nextRetryAt(job));
            log.info("Job {} marked as RETRY_PENDING, next retry at {}", job.getId(), job.getNextRetryAt());
        } else {
            job.setStatus(ToolExecutionJobStatus.DEAD_LETTERED.name());
            job.setDeadLetteredAt(now);
            job.setDeadLetterReason(message);
            log.info("Job {} marked as DEAD_LETTERED, reason: {}", job.getId(), message);
        }

        toolExecutionJobMapper.updateById(job);

        if (execution != null) {
            execution.setStatus(ToolExecutionStatus.FAILED.name());
            execution.setFinishedAt(now);
            execution.setDurationMs(0L);
            execution.setErrorMessage(message);
            toolSandboxExecutionMapper.updateById(execution);

            writeTaskLog(execution, "TOOL_JOB_FAILED",
                    "工具执行 Job 失败: " + job.getToolKey() + ", errorCode=" + errorCode);
        }
    }

    private void handlePatchProposalExecution(ToolSandboxExecutionEntity execution,
                                               String paramSummary,
                                               LocalDateTime finishedAt, long durationMs) {
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
            if (execution.getArtifactId() != null) {
                execution.setOutputPayload(
                        buildPatchProposalOutputPayload(execution.getArtifactId().toString(), paramSummary));
                toolSandboxExecutionMapper.updateById(execution);
            }
        }
    }

    private void handleRepositoryToolExecution(ToolSandboxExecutionEntity execution,
                                                LocalDateTime finishedAt, long durationMs) {
        Map<String, Object> repoParams = resolveParameters(execution.getProjectId(), execution.getToolName());
        RepositoryReadToolService.RepositoryToolResult repoResult =
                repositoryReadToolService.executeReadOnlyTool(
                        execution.getProjectId(), execution.getToolName(), repoParams);

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
    }

    private void handleStandardExecution(ToolSandboxExecutionEntity execution,
                                          String paramSummary,
                                          LocalDateTime finishedAt, long durationMs) {
        execution.setOutputPayload(buildMockOutputPayload(paramSummary));
        execution.setSummary("Mock 工具执行完成：" + execution.getToolName()
                + "，只读模拟，无文件写入，无 Git 操作。");
        execution.setStatus(ToolExecutionStatus.COMPLETED.name());
        execution.setFinishedAt(finishedAt);
        execution.setDurationMs(durationMs);
        execution.setErrorMessage(null);
        toolSandboxExecutionMapper.updateById(execution);
    }

    private void handleCodeSearchExecution(ToolSandboxExecutionEntity execution,
                                            Map<String, Object> parameters,
                                            LocalDateTime finishedAt, long durationMs) {
        String toolName = execution.getToolName();
        Long projectId = execution.getProjectId();
        String outputPayload;
        String summary;

        try {
            if (ToolName.READ_CODE_INDEX.name().equals(toolName)) {
                CodeIndexSummaryResponse indexSummary = codeIndexApplicationService.getSummary(projectId);
                outputPayload = objectMapper.writeValueAsString(Map.of(
                        "mock", true,
                        "readOnly", true,
                        "filesTouched", List.of(),
                        "gitOperations", List.of(),
                        "matchedFiles", indexSummary.getFileCount(),
                        "matchedSymbols", indexSummary.getSymbolCount(),
                        "matchedChunks", indexSummary.getChunkCount(),
                        "indexedAt", indexSummary.getIndexedAt() != null ? indexSummary.getIndexedAt() : "",
                        "filesRead", List.of()
                ));
                summary = "代码索引摘要读取完成: " + indexSummary.getFileCount()
                        + " 文件, " + indexSummary.getSymbolCount() + " 符号, "
                        + indexSummary.getChunkCount() + " 切片";
            } else {
                String keyword = parameters != null && parameters.get("keyword") != null
                        ? parameters.get("keyword").toString() : "";
                String branch = parameters != null && parameters.get("branch") != null
                        ? parameters.get("branch").toString() : null;
                String pathPrefix = parameters != null && parameters.get("pathPrefix") != null
                        ? parameters.get("pathPrefix").toString() : null;
                String language = parameters != null && parameters.get("language") != null
                        ? parameters.get("language").toString() : null;
                int limit = parameters != null && parameters.get("limit") instanceof Number
                        ? ((Number) parameters.get("limit")).intValue() : 10;

                CodeSearchRequest request = new CodeSearchRequest();
                request.setKeyword(keyword);
                request.setBranch(branch);
                request.setPathPrefix(pathPrefix);
                request.setLanguage(language);
                request.setLimit(limit);

                if (ToolName.SEARCH_CODE_SYMBOL.name().equals(toolName)) {
                    request.setSearchType("SYMBOL");
                } else {
                    request.setSearchType("CHUNK");
                }

                CodeSearchResponse searchResponse = codeIndexApplicationService.search(projectId, request);

                outputPayload = objectMapper.writeValueAsString(Map.of(
                        "mock", true,
                        "readOnly", true,
                        "filesTouched", List.of(),
                        "gitOperations", List.of(),
                        "keyword", keyword,
                        "searchType", request.getSearchType(),
                        "totalCount", searchResponse.getTotalCount(),
                        "matchedFiles", searchResponse.getTotalCount(),
                        "results", searchResponse.getResults()
                                .stream()
                                .map(r -> Map.of(
                                        "resultType", r.getResultType(),
                                        "filePath", r.getFilePath() != null ? r.getFilePath() : "",
                                        "symbolName", r.getSymbolName() != null ? r.getSymbolName() : "",
                                        "symbolType", r.getSymbolType() != null ? r.getSymbolType() : "",
                                        "startLine", r.getStartLine(),
                                        "endLine", r.getEndLine(),
                                        "snippet", r.getSnippet() != null ? r.getSnippet() : ""
                                ))
                                .toList(),
                        "filesRead", List.of()
                ));
                summary = "代码" + ("SYMBOL".equals(request.getSearchType()) ? "符号" : "片段")
                        + "搜索完成: keyword=" + keyword + ", 结果数=" + searchResponse.getTotalCount();
            }

            execution.setOutputPayload(outputPayload);
            execution.setSummary(summary);
            execution.setStatus(ToolExecutionStatus.COMPLETED.name());
            execution.setFinishedAt(finishedAt);
            execution.setDurationMs(durationMs);
            execution.setErrorMessage(null);
            toolSandboxExecutionMapper.updateById(execution);

            writeTaskLog(execution, "CODE_SEARCH_TOOL_COMPLETED",
                    "代码搜索工具执行完成: " + toolName);
        } catch (JsonProcessingException e) {
            log.error("代码搜索工具执行失败: toolName={}, projectId={}", toolName, projectId, e);
            throw new RuntimeException("代码搜索执行异常: " + e.getMessage(), e);
        }
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
}
