package com.aicoding.platform.orchestrator.application;

import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.domain.AgentVersionStatus;
import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.aicoding.platform.agent.domain.ProjectAgentConfigEntity;
import com.aicoding.platform.agent.dto.ProjectAgentRuntimeConfig;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.agent.infrastructure.AiAgentVersionMapper;
import com.aicoding.platform.agent.infrastructure.ProjectAgentConfigMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.modelgateway.application.ModelGateway;
import com.aicoding.platform.rag.application.RagContextService;
import com.aicoding.platform.rag.dto.RagContext;
import com.aicoding.platform.rag.dto.RagReference;
import com.aicoding.platform.modelgateway.application.ModelRequestLogService;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.orchestrator.domain.AgentExecutionEntity;
import com.aicoding.platform.orchestrator.domain.AgentExecutionStatus;
import com.aicoding.platform.orchestrator.domain.AgentExecutionType;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.domain.ModelRequestType;
import com.aicoding.platform.orchestrator.dto.AgentExecutionResponse;
import com.aicoding.platform.orchestrator.dto.ExecuteTaskRequest;
import com.aicoding.platform.orchestrator.dto.ModelRequestLogResponse;
import com.aicoding.platform.orchestrator.infrastructure.AgentExecutionMapper;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.domain.AiTaskEntity;
import com.aicoding.platform.task.domain.AiTaskEventEntity;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskEventType;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.domain.TaskStatus;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.aicoding.platform.task.infrastructure.AiTaskEventMapper;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentOrchestratorService {

    private final AgentExecutionMapper agentExecutionMapper;
    private final ModelRequestLogMapper modelRequestLogMapper;
    private final ModelGateway modelGateway;
    private final ModelRequestLogService modelRequestLogService;
    private final AiTaskMapper aiTaskMapper;
    private final AiAgentMapper aiAgentMapper;
    private final AiAgentVersionMapper aiAgentVersionMapper;
    private final ProjectAgentConfigMapper projectAgentConfigMapper;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final AiTaskEventMapper aiTaskEventMapper;
    private final ProjectPermissionService projectPermissionService;
    private final RagContextService ragContextService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ObjectMapper objectMapper;

    public AgentOrchestratorService(AgentExecutionMapper agentExecutionMapper,
                                    ModelRequestLogMapper modelRequestLogMapper,
                                    ModelGateway modelGateway,
                                    ModelRequestLogService modelRequestLogService,
                                    AiTaskMapper aiTaskMapper,
                                    AiAgentMapper aiAgentMapper,
                                    AiAgentVersionMapper aiAgentVersionMapper,
                                    ProjectAgentConfigMapper projectAgentConfigMapper,
                                    AiTaskLogMapper aiTaskLogMapper,
                                    AiTaskArtifactMapper aiTaskArtifactMapper,
                                    AiTaskEventMapper aiTaskEventMapper,
                                    ProjectPermissionService projectPermissionService,
                                    RagContextService ragContextService,
                                    AuditLogApplicationService auditLogApplicationService,
                                    ObjectMapper objectMapper) {
        this.agentExecutionMapper = agentExecutionMapper;
        this.modelRequestLogMapper = modelRequestLogMapper;
        this.modelGateway = modelGateway;
        this.modelRequestLogService = modelRequestLogService;
        this.aiTaskMapper = aiTaskMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.aiAgentVersionMapper = aiAgentVersionMapper;
        this.projectAgentConfigMapper = projectAgentConfigMapper;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.aiTaskEventMapper = aiTaskEventMapper;
        this.projectPermissionService = projectPermissionService;
        this.ragContextService = ragContextService;
        this.auditLogApplicationService = auditLogApplicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentExecutionResponse executeTask(Long taskId, ExecuteTaskRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();

        // 1. Query task
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        // 2. Task not found
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        // 3. Check DEVELOPER+ permission
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER,
                ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        // 4. Validate task status must be PENDING
        if (!TaskStatus.PENDING.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT,
                    "任务状态为 " + task.getStatus() + "，只有 PENDING 状态的任务可以执行");
        }

        // 5. Resolve agent
        Long agentId = null;
        if (request.getAgentId() != null && !request.getAgentId().isBlank()) {
            agentId = Long.valueOf(request.getAgentId());
        } else if (task.getAgentId() != null) {
            agentId = task.getAgentId();
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "task 未绑定 Agent 且请求未指定 agentId");
        }

        // 6. Query agent
        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }
        if (!"ENABLED".equals(agent.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "Agent 当前不可用");
        }

        AiAgentVersionEntity agentVersion = resolveAgentVersion(task, request, agentId);

        // 6b. Read project agent runtime config
        ProjectAgentRuntimeConfig runtimeConfig = findProjectAgentRuntimeConfig(task.getProjectId(), agentId);
        String effectiveInstruction = request.getInstruction();
        if (runtimeConfig != null && runtimeConfig.getCustomInstruction() != null
                && !runtimeConfig.getCustomInstruction().isBlank()) {
            if (effectiveInstruction != null && !effectiveInstruction.isBlank()) {
                effectiveInstruction = effectiveInstruction + "\n\n" + runtimeConfig.getCustomInstruction();
            } else {
                effectiveInstruction = runtimeConfig.getCustomInstruction();
            }
        }

        // 6c. Execute RAG Search
        boolean ragUsed = false;
        List<RagReference> ragReferences = new ArrayList<>();

        StringBuilder ragQueryBuilder = new StringBuilder();
        if (task.getTitle() != null) ragQueryBuilder.append(task.getTitle());
        if (task.getDescription() != null) {
            if (ragQueryBuilder.length() > 0) ragQueryBuilder.append(" ");
            ragQueryBuilder.append(task.getDescription());
        }
        if (effectiveInstruction != null && !effectiveInstruction.isBlank()) {
            if (ragQueryBuilder.length() > 0) ragQueryBuilder.append(" ");
            ragQueryBuilder.append(effectiveInstruction);
        }
        String ragQuery = ragQueryBuilder.toString();

        // Determine RAG params: request takes precedence, then project agent config
        Boolean useRag = request.getUseRag();
        String knowledgeBaseId = request.getKnowledgeBaseId();
        if (useRag == null && runtimeConfig != null) {
            useRag = runtimeConfig.getUseRag();
        }
        if ((knowledgeBaseId == null || knowledgeBaseId.isBlank()) && runtimeConfig != null) {
            knowledgeBaseId = runtimeConfig.getKnowledgeBaseId();
        }

        RagContext ragContext = ragContextService.buildContextForTask(
                task.getProjectId(), ragQuery,
                knowledgeBaseId, request.getRagLimit(), useRag);

        if (ragContext.getTotal() > 0 && !ragContext.getReferences().isEmpty()) {
            ragUsed = true;
            ragReferences = ragContext.getReferences();
        }

        // 7. Create execution (RUNNING)
        AgentExecutionEntity execution = new AgentExecutionEntity();
        execution.setProjectId(task.getProjectId());
        execution.setTaskId(taskId);
        execution.setAgentId(agentId);
        execution.setAgentVersionId(agentVersion.getId());
        execution.setExecutionType(AgentExecutionType.TASK.name());
        execution.setStatus(AgentExecutionStatus.RUNNING.name());
        execution.setStartedAt(LocalDateTime.now());
        agentExecutionMapper.insert(execution);

        // 8. Write task event: PENDING -> RUNNING
        writeEvent(taskId, task.getProjectId(), TaskStatus.PENDING.name(), TaskStatus.RUNNING.name(),
                TaskEventType.STARTED, currentUser.getUserId(), "Orchestrator started execution #" + execution.getId());

        // 9. Write task log: ORCHESTRATOR_START
        writeLog(taskId, task.getProjectId(), TaskLogLevel.INFO, "ORCHESTRATOR_START",
                "Agent [" + agent.getName() + "] v" + agentVersion.getVersionNo()
                        + " 开始执行任务，executionId=" + execution.getId());

        // 9b. Write task log: RAG_SEARCH
        if (ragUsed) {
            writeLog(taskId, task.getProjectId(), TaskLogLevel.INFO, "RAG_SEARCH",
                    "RAG search completed, query=\"" + (ragQuery.length() > 80 ? ragQuery.substring(0, 80) + "..." : ragQuery)
                            + "\", total=" + ragContext.getTotal() + ", elapsedMs=" + ragContext.getElapsedMs());
        } else if (ragContext.getQuery() != null && !ragContext.getQuery().isBlank()) {
            writeLog(taskId, task.getProjectId(), TaskLogLevel.INFO, "RAG_SEARCH",
                    "RAG search completed, no references found");
        }

        // 10. Update task status to RUNNING
        task.setStatus(TaskStatus.RUNNING.name());
        task.setStartTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 11. Build Prompt (with RAG context and custom instruction)
        String inputPrompt = buildPrompt(agent, agentVersion, task, effectiveInstruction, ragContext.getContextText());

        // 12-13. Call ModelGateway and record log
        ModelRequest modelRequest = buildModelRequest(task.getProjectId(), execution.getId(), inputPrompt);
        modelRequest.setContext(ragContext.getContextText());
        ModelResponse modelResponse;
        try {
            modelResponse = modelGateway.generate(modelRequest);
            modelRequestLogService.record(task.getProjectId(), execution.getId(), modelRequest, modelResponse);
        } catch (Exception e) {
            modelResponse = new ModelResponse();
            modelResponse.setSuccess(false);
            modelResponse.setErrorMessage("Model gateway exception: " + e.getMessage());
            modelResponse.setPromptTokens(0L);
            modelResponse.setCompletionTokens(0L);
            modelResponse.setTotalTokens(0L);
            modelResponse.setLatencyMs(0L);
            modelRequestLogService.record(task.getProjectId(), execution.getId(), modelRequest, modelResponse);
        }

        // 14. Handle success
        if (Boolean.TRUE.equals(modelResponse.getSuccess())) {
            execution.setStatus(AgentExecutionStatus.COMPLETED.name());
            execution.setOutputContent(modelResponse.getContent());
            execution.setInputPrompt(inputPrompt);
            Long totalTokens = modelResponse.getTotalTokens();
            if (totalTokens == null) {
                execution.setTokenUsage(0L);
            } else {
                execution.setTokenUsage(totalTokens);
            }
            execution.setFinishedAt(LocalDateTime.now());
            agentExecutionMapper.updateById(execution);

            String resolvedProvider = modelResponse.getProvider() != null
                    ? modelResponse.getProvider() : modelRequest.getProvider();
            String resolvedModelName = modelResponse.getModelName() != null
                    ? modelResponse.getModelName() : modelRequest.getModelName();

            writeLog(taskId, task.getProjectId(), TaskLogLevel.INFO, "MODEL_GATEWAY_REQUEST",
                    "Model [" + resolvedProvider + "/" + resolvedModelName
                            + "] returned, totalTokens=" + modelResponse.getTotalTokens()
                            + ", latencyMs=" + modelResponse.getLatencyMs());
            writeLog(taskId, task.getProjectId(), TaskLogLevel.INFO, "ORCHESTRATOR_DONE",
                    "Agent 执行完成，executionId=" + execution.getId());

            AiTaskArtifactEntity artifact = new AiTaskArtifactEntity();
            artifact.setTaskId(taskId);
            artifact.setProjectId(task.getProjectId());
            artifact.setArtifactType("MARKDOWN");
            artifact.setName("Agent Execution Result");
            artifact.setContent(modelResponse.getContent());
            aiTaskArtifactMapper.insert(artifact);

            writeEvent(taskId, task.getProjectId(), TaskStatus.RUNNING.name(), TaskStatus.COMPLETED.name(),
                    TaskEventType.COMPLETED, currentUser.getUserId(), "Execution #" + execution.getId() + " completed");

            task.setStatus(TaskStatus.COMPLETED.name());
            task.setEndTime(LocalDateTime.now());
            aiTaskMapper.updateById(task);

            auditLogApplicationService.recordSuccess(task.getProjectId(), taskId,
                    AuditActionType.TASK_EXECUTE.name(), "TASK",
                    "Execute task with Agent [" + agent.getName() + "]");
        } else {
            // 15. Handle failure
            String errorMsg = modelResponse.getErrorMessage() != null
                    ? modelResponse.getErrorMessage() : "模型响应失败";
            execution.setStatus(AgentExecutionStatus.FAILED.name());
            execution.setInputPrompt(inputPrompt);
            execution.setErrorMessage(errorMsg);
            execution.setFinishedAt(LocalDateTime.now());
            agentExecutionMapper.updateById(execution);

            writeLog(taskId, task.getProjectId(), TaskLogLevel.ERROR, "ORCHESTRATOR_FAILED",
                    "执行失败: " + errorMsg + "，executionId=" + execution.getId());
            writeEvent(taskId, task.getProjectId(), TaskStatus.RUNNING.name(), TaskStatus.FAILED.name(),
                    TaskEventType.FAILED, currentUser.getUserId(), errorMsg);

            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage(errorMsg);
            task.setEndTime(LocalDateTime.now());
            aiTaskMapper.updateById(task);

            auditLogApplicationService.recordFailure(task.getProjectId(), taskId,
                    AuditActionType.TASK_EXECUTE.name(), "TASK",
                    "Execute task with Agent [" + agent.getName() + "]", errorMsg);
        }

        // 16. Return response
        AgentExecutionResponse resp = toAgentExecutionResponse(execution, agent.getName());
        resp.setRagUsed(ragUsed);
        resp.setReferences(ragReferences);
        return resp;
    }

    private AiAgentVersionEntity resolveAgentVersion(AiTaskEntity task, ExecuteTaskRequest request, Long agentId) {
        Long versionId = parseLongOrNull(request.getAgentVersionId());
        if (versionId == null) {
            versionId = task.getAgentVersionId();
        }
        if (versionId == null) {
            ProjectAgentConfigEntity projectConfig = projectAgentConfigMapper.selectOne(
                    new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                            .eq(ProjectAgentConfigEntity::getProjectId, task.getProjectId())
                            .eq(ProjectAgentConfigEntity::getAgentId, agentId)
                            .eq(ProjectAgentConfigEntity::getEnabled, 1)
                            .last("LIMIT 1"));
            if (projectConfig != null) {
                versionId = projectConfig.getAgentVersionId();
            }
        }

        AiAgentVersionEntity version;
        if (versionId != null) {
            version = aiAgentVersionMapper.selectById(versionId);
            if (version == null || !agentId.equals(version.getAgentId())) {
                throw new BizException(ErrorCode.NOT_FOUND, "Agent 版本不存在");
            }
            if (!AgentVersionStatus.PUBLISHED.name().equals(version.getStatus())) {
                throw new BizException(ErrorCode.CONFLICT, "Agent 版本不是 PUBLISHED 状态");
            }
            return version;
        }

        version = aiAgentVersionMapper.selectOne(
                new LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, agentId)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .orderByDesc(AiAgentVersionEntity::getPublishTime)
                        .last("LIMIT 1"));
        if (version == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 无 PUBLISHED 版本");
        }
        return version;
    }

    private ProjectAgentRuntimeConfig findProjectAgentRuntimeConfig(Long projectId, Long agentId) {
        ProjectAgentConfigEntity config = projectAgentConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                        .eq(ProjectAgentConfigEntity::getProjectId, projectId)
                        .eq(ProjectAgentConfigEntity::getAgentId, agentId)
                        .eq(ProjectAgentConfigEntity::getEnabled, 1)
                        .last("LIMIT 1"));
        if (config == null || config.getConfigJson() == null || config.getConfigJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(config.getConfigJson(), ProjectAgentRuntimeConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Transactional(readOnly = true)
    public PageResult<AgentExecutionResponse> listExecutions(Long taskId, PageQuery pageQuery) {
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<AgentExecutionEntity> wrapper = new LambdaQueryWrapper<AgentExecutionEntity>()
                .eq(AgentExecutionEntity::getTaskId, taskId)
                .orderByDesc(AgentExecutionEntity::getCreateTime);

        Page<AgentExecutionEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<AgentExecutionEntity> result = agentExecutionMapper.selectPage(page, wrapper);

        List<AgentExecutionResponse> records = result.getRecords().stream()
                .map(e -> {
                    String agentName = resolveAgentName(e.getAgentId());
                    return toAgentExecutionResponse(e, agentName);
                })
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public AgentExecutionResponse getExecutionDetail(Long executionId) {
        AgentExecutionEntity execution = agentExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "执行记录不存在");
        }
        projectPermissionService.checkProjectRole(execution.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        String agentName = resolveAgentName(execution.getAgentId());
        return toAgentExecutionResponse(execution, agentName);
    }

    @Transactional(readOnly = true)
    public List<ModelRequestLogResponse> getModelLogs(Long executionId) {
        AgentExecutionEntity execution = agentExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "执行记录不存在");
        }
        projectPermissionService.checkProjectRole(execution.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<ModelRequestLogEntity> logs = modelRequestLogMapper.selectList(
                new LambdaQueryWrapper<ModelRequestLogEntity>()
                        .eq(ModelRequestLogEntity::getExecutionId, executionId)
                        .orderByAsc(ModelRequestLogEntity::getCreateTime));

        return logs.stream().map(this::toModelRequestLogResponse).collect(Collectors.toList());
    }

    private String buildPrompt(AiAgentEntity agent, AiAgentVersionEntity agentVersion,
                               AiTaskEntity task, String instruction, String ragContextText) {
        StringBuilder sb = new StringBuilder();
        sb.append("System Prompt:\n");
        if (agentVersion.getSystemPrompt() != null && !agentVersion.getSystemPrompt().isBlank()) {
            sb.append(agentVersion.getSystemPrompt()).append("\n\n");
        } else {
            sb.append("你是 AI Coding Platform 的 Agent。\n\n");
        }

        sb.append("Agent:\n");
        sb.append("- Name: ").append(agent.getName()).append("\n");
        sb.append("- Type: ").append(agent.getType()).append("\n");
        sb.append("- Version: ").append(agentVersion.getVersionNo()).append("\n");
        sb.append("- VersionId: ").append(agentVersion.getId()).append("\n\n");
        if (agentVersion.getToolPolicy() != null && !agentVersion.getToolPolicy().isBlank()) {
            sb.append("Tool Policy:\n");
            sb.append(agentVersion.getToolPolicy()).append("\n\n");
        }
        if (agentVersion.getExecutionPolicy() != null && !agentVersion.getExecutionPolicy().isBlank()) {
            sb.append("Execution Policy:\n");
            sb.append(agentVersion.getExecutionPolicy()).append("\n\n");
        }
        sb.append("Task:\n");
        sb.append("- Title: ").append(task.getTitle()).append("\n");
        if (task.getDescription() != null) {
            sb.append("- Description: ").append(task.getDescription()).append("\n");
        }
        sb.append("- Type: ").append(task.getTaskType()).append("\n");
        sb.append("- Priority: ").append(task.getPriority()).append("\n\n");
        sb.append("Instruction:\n");
        if (instruction != null && !instruction.isBlank()) {
            sb.append(instruction).append("\n\n");
        } else {
            sb.append("请执行上述任务。\n\n");
        }
        if (ragContextText != null && !ragContextText.isBlank()) {
            sb.append("Project Knowledge Context:\n");
            sb.append(ragContextText).append("\n\n");
        }
        sb.append("Constraints:\n");
        sb.append("- 当前阶段使用 Mock Model Gateway\n");
        sb.append("- 不调用真实大模型\n");
        sb.append("- 不修改真实代码\n");
        sb.append("- 不执行 Git 写操作\n");
        sb.append("- 如果使用了上下文，请在输出中简要说明参考了哪些项目文档\n");
        sb.append("- 输出应该适合作为任务执行结果保存\n");
        return sb.toString();
    }

    private ModelRequest buildModelRequest(Long projectId, Long executionId, String inputPrompt) {
        ModelRequest request = new ModelRequest();
        request.setProjectId(projectId);
        request.setExecutionId(executionId);
        request.setRequestType(ModelRequestType.TASK_EXECUTION.name());
        request.setUserPrompt(inputPrompt);
        return request;
    }

    private void writeEvent(Long taskId, Long projectId, String fromStatus, String toStatus,
                            TaskEventType eventType, Long operatorId, String reason) {
        AiTaskEventEntity event = new AiTaskEventEntity();
        event.setTaskId(taskId);
        event.setProjectId(projectId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType.name());
        event.setOperatorId(operatorId);
        event.setReason(reason);
        aiTaskEventMapper.insert(event);
    }

    private void writeLog(Long taskId, Long projectId, TaskLogLevel level, String stage, String message) {
        AiTaskLogEntity log = new AiTaskLogEntity();
        log.setTaskId(taskId);
        log.setProjectId(projectId);
        log.setLevel(level.name());
        log.setStage(stage);
        log.setMessage(message);
        aiTaskLogMapper.insert(log);
    }

    private String resolveAgentName(Long agentId) {
        if (agentId == null) {
            return null;
        }
        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        return agent != null ? agent.getName() : null;
    }

    private AgentExecutionResponse toAgentExecutionResponse(AgentExecutionEntity e, String agentName) {
        AgentExecutionResponse resp = new AgentExecutionResponse();
        resp.setId(e.getId().toString());
        resp.setProjectId(e.getProjectId().toString());
        resp.setTaskId(e.getTaskId() != null ? e.getTaskId().toString() : null);
        resp.setChatSessionId(e.getChatSessionId() != null ? e.getChatSessionId().toString() : null);
        resp.setChatMessageId(e.getChatMessageId() != null ? e.getChatMessageId().toString() : null);
        resp.setAgentId(e.getAgentId().toString());
        resp.setAgentVersionId(e.getAgentVersionId() != null ? e.getAgentVersionId().toString() : null);
        resp.setAgentName(agentName);
        resp.setExecutionType(e.getExecutionType());
        resp.setStatus(e.getStatus());
        resp.setInputPrompt(e.getInputPrompt());
        resp.setOutputContent(e.getOutputContent());
        resp.setErrorMessage(e.getErrorMessage());
        resp.setTokenUsage(e.getTokenUsage());
        resp.setStartedAt(e.getStartedAt());
        resp.setFinishedAt(e.getFinishedAt());
        resp.setCreateTime(e.getCreateTime());
        return resp;
    }

    private ModelRequestLogResponse toModelRequestLogResponse(ModelRequestLogEntity entity) {
        ModelRequestLogResponse resp = new ModelRequestLogResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setExecutionId(entity.getExecutionId() != null ? entity.getExecutionId().toString() : null);
        resp.setProvider(entity.getProvider());
        resp.setModelName(entity.getModelName());
        resp.setRequestType(entity.getRequestType());
        resp.setPromptTokens(entity.getPromptTokens());
        resp.setCompletionTokens(entity.getCompletionTokens());
        resp.setTotalTokens(entity.getTotalTokens());
        resp.setLatencyMs(entity.getLatencyMs());
        resp.setSuccess(entity.getSuccess());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }
}
