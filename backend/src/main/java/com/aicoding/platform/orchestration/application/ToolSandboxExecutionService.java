package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolApprovalStatus;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionApprovalEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionMode;
import com.aicoding.platform.orchestration.domain.ToolExecutionStatus;
import com.aicoding.platform.orchestration.domain.ToolName;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.domain.ToolType;
import com.aicoding.platform.orchestration.dto.ToolExecutionApprovalResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobResponse;
import com.aicoding.platform.orchestration.dto.ToolSandboxExecutionResponse;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionApprovalMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolSandboxExecutionService {

    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final ToolExecutionApprovalMapper toolExecutionApprovalMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ToolPolicyService toolPolicyService;
    private final com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper aiTaskArtifactMapper;
    private final ToolParameterSchemaService toolParameterSchemaService;
    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final ToolExecutionJobService toolExecutionJobService;

    private static final Map<String, ToolName> STEP_TYPE_TO_TOOL = Map.of(
            "ARCHITECTURE_ANALYSIS", ToolName.PROJECT_CONTEXT_SCAN,
            "BACKEND_IMPLEMENTATION_PLAN", ToolName.TASK_REQUIREMENT_ANALYSIS,
            "FRONTEND_IMPLEMENTATION_PLAN", ToolName.MOCK_FILE_INSPECTION,
            "TEST_PLAN", ToolName.MOCK_TEST_PLAN_SCAN,
            "CODE_REVIEW", ToolName.MOCK_SECURITY_REVIEW,
            "FINAL_SUMMARY", ToolName.PROJECT_CONTEXT_SCAN
    );

    private static final Map<ToolName, ToolType> TOOL_TYPE_MAP = Map.ofEntries(
            Map.entry(ToolName.PROJECT_CONTEXT_SCAN, ToolType.READ_ONLY),
            Map.entry(ToolName.TASK_REQUIREMENT_ANALYSIS, ToolType.ANALYSIS),
            Map.entry(ToolName.MOCK_FILE_INSPECTION, ToolType.READ_ONLY),
            Map.entry(ToolName.MOCK_TEST_PLAN_SCAN, ToolType.MOCK),
            Map.entry(ToolName.MOCK_SECURITY_REVIEW, ToolType.ANALYSIS),
            Map.entry(ToolName.MOCK_PATCH_PROPOSAL, ToolType.ANALYSIS),
            Map.entry(ToolName.READ_REPOSITORY_TREE, ToolType.READ_ONLY),
            Map.entry(ToolName.READ_FILE_SNIPPET, ToolType.READ_ONLY),
            Map.entry(ToolName.READ_DIFF_SUMMARY, ToolType.READ_ONLY),
            Map.entry(ToolName.READ_BRANCH_INFO, ToolType.READ_ONLY),
            Map.entry(ToolName.READ_CODE_INDEX, ToolType.READ_ONLY),
            Map.entry(ToolName.SEARCH_CODE_SYMBOL, ToolType.READ_ONLY),
            Map.entry(ToolName.SEARCH_CODE_CHUNK, ToolType.READ_ONLY)
    );

    private static final String APPROVAL_KEY = "TOOL_EXECUTION_APPROVAL";

    public ToolSandboxExecutionService(ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                        ToolExecutionApprovalMapper toolExecutionApprovalMapper,
                                        ProjectPermissionService projectPermissionService,
                                        ToolPolicyService toolPolicyService,
                                        com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper aiTaskArtifactMapper,
                                        ToolParameterSchemaService toolParameterSchemaService,
                                        ToolCatalogMapper toolCatalogMapper,
                                        ProjectToolConfigMapper projectToolConfigMapper,
                                        ToolExecutionJobService toolExecutionJobService) {
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.toolExecutionApprovalMapper = toolExecutionApprovalMapper;
        this.projectPermissionService = projectPermissionService;
        this.toolPolicyService = toolPolicyService;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.toolParameterSchemaService = toolParameterSchemaService;
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
        this.toolExecutionJobService = toolExecutionJobService;
    }

    @Transactional
    public ToolSandboxExecutionEntity mockExecuteForStep(Long projectId, Long taskId, Long runId,
                                                          Long phaseId, Long stepId, Long agentId,
                                                          String stepType, String inputContext) {
        // For CODE_REVIEW steps, check if MOCK_PATCH_PROPOSAL is enabled → use it instead
        ToolName toolName = resolveToolName(projectId, stepType);
        ToolType toolType = TOOL_TYPE_MAP.getOrDefault(toolName, ToolType.MOCK);
        LocalDateTime now = LocalDateTime.now();

        // Check tool policy
        ToolPolicyService.ToolPolicyDecision decision =
                toolPolicyService.checkToolAllowed(projectId, toolName.name(), stepType, agentId);

        if (decision.isRequiresApproval()) {
            return createWaitingApprovalExecution(projectId, taskId, runId, phaseId, stepId, agentId,
                    toolName, toolType, stepType, inputContext, decision, now);
        }

        if (!decision.isAllowed()) {
            return createBlockedExecution(projectId, taskId, runId, phaseId, stepId, agentId,
                    toolName, toolType, stepType, inputContext, decision.getBlockedReason(), now);
        }

        return createCompletedExecution(projectId, taskId, runId, phaseId, stepId, agentId,
                toolName, toolType, stepType, inputContext, now);
    }

    private ToolName resolveToolName(Long projectId, String stepType) {
        ToolName defaultTool = STEP_TYPE_TO_TOOL.getOrDefault(stepType, ToolName.PROJECT_CONTEXT_SCAN);

        if ("CODE_REVIEW".equals(stepType) || "BACKEND_IMPLEMENTATION_PLAN".equals(stepType)
                || "FRONTEND_IMPLEMENTATION_PLAN".equals(stepType)) {
            ToolPolicyService.ToolPolicyDecision highDecision =
                    toolPolicyService.checkToolAllowed(projectId, ToolName.MOCK_PATCH_PROPOSAL.name(), stepType, null);
            if (highDecision.isRequiresApproval()) {
                return ToolName.MOCK_PATCH_PROPOSAL;
            }
        }

        return defaultTool;
    }

    // ========================
    // Execution creation helpers
    // ========================

    private ToolSandboxExecutionEntity createBlockedExecution(
            Long projectId, Long taskId, Long runId, Long phaseId, Long stepId, Long agentId,
            ToolName toolName, ToolType toolType, String stepType, String inputContext,
            String blockedReason, LocalDateTime now) {

        Map<String, Object> parameters = resolveParameters(projectId, toolName.name());
        String paramSummary = buildParameterSummaryJson(parameters);

        ToolSandboxExecutionEntity entity = new ToolSandboxExecutionEntity();
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setRunId(runId);
        entity.setPhaseId(phaseId);
        entity.setStepId(stepId);
        entity.setAgentId(agentId);
        entity.setToolName(toolName.name());
        entity.setToolType(toolType.name());
        entity.setExecutionMode(ToolExecutionMode.MOCK_EXECUTE.name());
        entity.setStatus(ToolExecutionStatus.BLOCKED.name());
        entity.setInputPayload(buildInputPayload(stepType, inputContext, toolName.name(), parameters));
        entity.setOutputPayload(buildBlockedOutputPayload(blockedReason, paramSummary));
        entity.setSummary("工具 " + toolName.name() + " 被策略阻止：" + blockedReason);
        entity.setErrorMessage(blockedReason);
        entity.setStartedAt(now);
        entity.setFinishedAt(now);
        entity.setDurationMs(0L);
        toolSandboxExecutionMapper.insert(entity);

        return entity;
    }

    private ToolSandboxExecutionEntity createWaitingApprovalExecution(
            Long projectId, Long taskId, Long runId, Long phaseId, Long stepId, Long agentId,
            ToolName toolName, ToolType toolType, String stepType, String inputContext,
            ToolPolicyService.ToolPolicyDecision decision, LocalDateTime now) {

        Map<String, Object> parameters = resolveParameters(projectId, toolName.name());
        String paramSummary = buildParameterSummaryJson(parameters);

        ToolSandboxExecutionEntity entity = new ToolSandboxExecutionEntity();
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setRunId(runId);
        entity.setPhaseId(phaseId);
        entity.setStepId(stepId);
        entity.setAgentId(agentId);
        entity.setToolName(toolName.name());
        entity.setToolType(toolType.name());
        entity.setExecutionMode(ToolExecutionMode.MOCK_EXECUTE.name());
        entity.setStatus(ToolExecutionStatus.WAITING_APPROVAL.name());
        entity.setInputPayload(buildInputPayload(stepType, inputContext, toolName.name(), parameters));
        entity.setOutputPayload(buildWaitingApprovalOutputPayload(paramSummary));
        entity.setSummary("工具 " + toolName.name() + " 等待人工审批");
        entity.setStartedAt(now);
        entity.setDurationMs(0L);
        toolSandboxExecutionMapper.insert(entity);

        // Create approval record
        ToolExecutionApprovalEntity approval = new ToolExecutionApprovalEntity();
        approval.setProjectId(projectId);
        approval.setTaskId(taskId);
        approval.setRunId(runId);
        approval.setStepId(stepId);
        approval.setToolExecutionId(entity.getId());
        approval.setToolId(decision.getToolCatalog() != null ? decision.getToolCatalog().getId() : null);
        approval.setToolKey(toolName.name());
        approval.setApprovalKey(APPROVAL_KEY);
        approval.setTitle("审批: " + toolName.name());
        approval.setDescription("工具 " + toolName.name() + " 需要人工审批后执行 Mock。审批通过后仍只执行 Mock，不会执行真实 Shell、Git 或文件写入。");
        approval.setRiskLevel(decision.getToolCatalog() != null ? decision.getToolCatalog().getRiskLevel() : "HIGH");
        approval.setStatus(ToolApprovalStatus.PENDING.name());
        approval.setRequestedAt(now);
        toolExecutionApprovalMapper.insert(approval);

        return entity;
    }

    private ToolSandboxExecutionEntity createCompletedExecution(
            Long projectId, Long taskId, Long runId, Long phaseId, Long stepId, Long agentId,
            ToolName toolName, ToolType toolType, String stepType, String inputContext,
            LocalDateTime now) {

        Map<String, Object> parameters = resolveParameters(projectId, toolName.name());

        ToolSandboxExecutionEntity entity = new ToolSandboxExecutionEntity();
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setRunId(runId);
        entity.setPhaseId(phaseId);
        entity.setStepId(stepId);
        entity.setAgentId(agentId);
        entity.setToolName(toolName.name());
        entity.setToolType(toolType.name());
        entity.setExecutionMode(ToolExecutionMode.MOCK_EXECUTE.name());
        entity.setStatus(ToolExecutionStatus.RUNNING.name());
        entity.setInputPayload(buildInputPayload(stepType, inputContext, toolName.name(), parameters));
        entity.setStartedAt(now);
        toolSandboxExecutionMapper.insert(entity);

        // Create job and execute (sync drain or async publish)
        String requestPayload = entity.getInputPayload();
        toolExecutionJobService.executeJob(entity, requestPayload);

        return toolSandboxExecutionMapper.selectById(entity.getId());
    }

    // ========================
    // Approval operations
    // ========================

    @Transactional
    public ToolSandboxExecutionResponse approveAndExecute(Long executionId, String comment) {
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        if (!ToolExecutionStatus.WAITING_APPROVAL.name().equals(execution.getStatus())) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_CONFLICT,
                    "当前执行状态不允许审批，状态: " + execution.getStatus());
        }

        projectPermissionService.checkProjectRole(execution.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, executionId));
        if (approval == null) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_NOT_FOUND, "工具审批记录不存在");
        }
        if (!ToolApprovalStatus.PENDING.name().equals(approval.getStatus())) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_CONFLICT,
                    "该审批已被处理，当前状态: " + approval.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        // Mark approval APPROVED
        approval.setStatus(ToolApprovalStatus.APPROVED.name());
        approval.setDecidedBy(com.aicoding.platform.security.context.LoginUserContext.currentUserId());
        approval.setDecisionComment(comment);
        approval.setDecidedAt(now);
        toolExecutionApprovalMapper.updateById(approval);

        // Create job and execute (sync drain or async publish)
        execution.setStatus(ToolExecutionStatus.RUNNING.name());
        toolSandboxExecutionMapper.updateById(execution);

        String requestPayload = execution.getInputPayload();
        toolExecutionJobService.executeJob(execution, requestPayload);

        execution = toolSandboxExecutionMapper.selectById(executionId);
        return toResponse(execution);
    }

    @Transactional
    public ToolSandboxExecutionResponse rejectExecution(Long executionId, String comment) {
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        if (!ToolExecutionStatus.WAITING_APPROVAL.name().equals(execution.getStatus())) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_CONFLICT,
                    "当前执行状态不允许审批，状态: " + execution.getStatus());
        }

        projectPermissionService.checkProjectRole(execution.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, executionId));
        if (approval == null) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_NOT_FOUND, "工具审批记录不存在");
        }
        if (!ToolApprovalStatus.PENDING.name().equals(approval.getStatus())) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_CONFLICT,
                    "该审批已被处理，当前状态: " + approval.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        // Mark approval REJECTED
        approval.setStatus(ToolApprovalStatus.REJECTED.name());
        approval.setDecidedBy(com.aicoding.platform.security.context.LoginUserContext.currentUserId());
        approval.setDecisionComment(comment);
        approval.setDecidedAt(now);
        toolExecutionApprovalMapper.updateById(approval);

        // Resolve parameters for audit
        Map<String, Object> parameters = resolveParameters(execution.getProjectId(), execution.getToolName());
        String paramSummary = buildParameterSummaryJson(parameters);

        // Mark execution REJECTED
        execution.setOutputPayload(buildRejectedOutputPayload(comment, paramSummary));
        execution.setSummary("工具 " + execution.getToolName() + " 已被人工驳回"
                + (comment != null && !comment.isBlank() ? "：" + comment : ""));
        execution.setStatus(ToolExecutionStatus.REJECTED.name());
        execution.setFinishedAt(now);
        execution.setDurationMs(0L);
        toolSandboxExecutionMapper.updateById(execution);

        return toResponse(execution);
    }

    // ========================
    // Approval queries
    // ========================

    @Transactional(readOnly = true)
    public ToolExecutionApprovalResponse getApproval(Long executionId) {
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        projectPermissionService.checkProjectMember(execution.getProjectId());

        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, executionId));
        if (approval == null) {
            throw new BizException(ErrorCode.TOOL_APPROVAL_NOT_FOUND, "该执行无审批记录");
        }
        return toApprovalResponse(approval);
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionApprovalResponse> listProjectApprovals(Long projectId, String status) {
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<ToolExecutionApprovalEntity> wrapper =
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getProjectId, projectId)
                        .orderByDesc(ToolExecutionApprovalEntity::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ToolExecutionApprovalEntity::getStatus, status);
        }

        List<ToolExecutionApprovalEntity> approvals = toolExecutionApprovalMapper.selectList(wrapper);
        return approvals.stream().map(this::toApprovalResponse).collect(Collectors.toList());
    }

    // ========================
    // Existing queries
    // ========================

    @Transactional(readOnly = true)
    public List<ToolSandboxExecutionResponse> listByRun(Long runId) {
        List<ToolSandboxExecutionEntity> entities = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getRunId, runId)
                        .orderByAsc(ToolSandboxExecutionEntity::getCreateTime));

        if (!entities.isEmpty()) {
            projectPermissionService.checkProjectMember(entities.get(0).getProjectId());
        }

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolSandboxExecutionResponse> listByStep(Long stepId) {
        List<ToolSandboxExecutionEntity> entities = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getStepId, stepId)
                        .orderByAsc(ToolSandboxExecutionEntity::getCreateTime));

        if (!entities.isEmpty()) {
            projectPermissionService.checkProjectMember(entities.get(0).getProjectId());
        }

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolSandboxExecutionResponse getExecution(Long executionId) {
        ToolSandboxExecutionEntity entity = toolSandboxExecutionMapper.selectById(executionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ToolSandboxExecutionResponse> listByStepIds(List<Long> stepIds) {
        if (stepIds == null || stepIds.isEmpty()) {
            return List.of();
        }
        List<ToolSandboxExecutionEntity> entities = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .in(ToolSandboxExecutionEntity::getStepId, stepIds)
                        .orderByAsc(ToolSandboxExecutionEntity::getCreateTime));
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ========================
    // Parameter resolution
    // ========================

    private Map<String, Object> resolveParameters(Long projectId, String toolNameStr) {
        ToolCatalogEntity tool = toolCatalogMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getToolKey, toolNameStr));
        if (tool == null) {
            return new java.util.HashMap<>();
        }

        ProjectToolConfigEntity config = projectToolConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId)
                        .eq(ProjectToolConfigEntity::getToolId, tool.getId()));

        String parametersJson = config != null ? config.getParametersJson() : null;
        Map<String, Object> rawParameters = new java.util.HashMap<>();
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(parametersJson, java.util.Map.class);
                if (parsed != null) {
                    rawParameters = parsed;
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // ignore parse errors, use empty
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
    // Payload builders
    // ========================

    private String buildInputPayload(String stepType, String inputContext,
                                      String toolKey, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"stepType\":\"").append(stepType).append("\"");
        sb.append(",\"inputLength\":").append(inputContext != null ? inputContext.length() : 0);
        if (toolKey != null) {
            sb.append(",\"toolKey\":\"").append(toolKey).append("\"");
        }
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(",\"parameters\":{");
            boolean first = true;
            for (java.util.Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                Object val = entry.getValue();
                if (val instanceof String s) {
                    sb.append("\"").append(s.replace("\"", "\\\"")).append("\"");
                } else if (val instanceof Boolean || val instanceof Number) {
                    sb.append(val);
                } else {
                    sb.append("\"").append(val != null ? val.toString().replace("\"", "\\\"") : "null").append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildBlockedOutputPayload(String reason, String paramSummary) {
        String summary = paramSummary != null && !paramSummary.isBlank()
                ? ",\"parameterSummary\":\"" + paramSummary.replace("\"", "\\\"") + "\""
                : "";
        return "{"
                + "\"mock\":true,"
                + "\"readOnly\":true,"
                + "\"blocked\":true,"
                + "\"reason\":\"" + reason.replace("\"", "\\\"") + "\","
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[]"
                + summary
                + "}";
    }

    private String buildWaitingApprovalOutputPayload(String paramSummary) {
        String summary = paramSummary != null && !paramSummary.isBlank()
                ? ",\"parameterSummary\":\"" + paramSummary.replace("\"", "\\\"") + "\""
                : "";
        return "{"
                + "\"mock\":true,"
                + "\"waitingApproval\":true,"
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[]"
                + summary
                + "}";
    }

    private String buildRejectedOutputPayload(String comment, String paramSummary) {
        String summary = paramSummary != null && !paramSummary.isBlank()
                ? ",\"parameterSummary\":\"" + paramSummary.replace("\"", "\\\"") + "\""
                : "";
        return "{"
                + "\"mock\":true,"
                + "\"readOnly\":true,"
                + "\"rejected\":true,"
                + (comment != null && !comment.isBlank()
                    ? "\"reason\":\"" + comment.replace("\"", "\\\"") + "\","
                    : "")
                + "\"filesTouched\":[],"
                + "\"gitOperations\":[]"
                + summary
                + "}";
    }

    // ========================
    // Response mapping
    // ========================

    public ToolSandboxExecutionResponse toResponse(ToolSandboxExecutionEntity entity) {
        ToolSandboxExecutionResponse resp = new ToolSandboxExecutionResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setPhaseId(entity.getPhaseId() != null ? entity.getPhaseId().toString() : null);
        resp.setStepId(entity.getStepId() != null ? entity.getStepId().toString() : null);
        resp.setAgentId(entity.getAgentId() != null ? entity.getAgentId().toString() : null);
        resp.setToolName(entity.getToolName());
        resp.setToolType(entity.getToolType());
        resp.setExecutionMode(entity.getExecutionMode());
        resp.setStatus(entity.getStatus());
        resp.setInputPayload(entity.getInputPayload());
        resp.setOutputPayload(entity.getOutputPayload());
        resp.setSummary(entity.getSummary());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setStartedAt(entity.getStartedAt() != null ? entity.getStartedAt().toString() : null);
        resp.setFinishedAt(entity.getFinishedAt() != null ? entity.getFinishedAt().toString() : null);
        resp.setDurationMs(entity.getDurationMs());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);

        // Load approval info
        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, entity.getId()));
        if (approval != null) {
            resp.setApproval(toApprovalResponse(approval));
            resp.setRequiresApproval(true);
        } else {
            resp.setRequiresApproval(false);
        }

        // Load artifact info
        if (entity.getArtifactId() != null) {
            resp.setArtifactId(entity.getArtifactId().toString());
            try {
                com.aicoding.platform.task.domain.AiTaskArtifactEntity artEntity =
                        aiTaskArtifactMapper.selectById(entity.getArtifactId());
                if (artEntity != null) {
                    resp.setArtifactType(artEntity.getArtifactType());
                    resp.setArtifactName(artEntity.getName());
                }
            } catch (Exception e) {
                resp.setArtifactType(com.aicoding.platform.task.domain.TaskArtifactType.PATCH_PROPOSAL.name());
                resp.setArtifactName("Mock Patch Proposal");
            }
        }

        // Load latest job info
        ToolExecutionJobResponse latestJob = toolExecutionJobService.getLatestJobByExecution(entity.getId());
        if (latestJob != null) {
            resp.setJobId(latestJob.getId());
            resp.setJob(latestJob);
        }

        return resp;
    }

    private ToolExecutionApprovalResponse toApprovalResponse(ToolExecutionApprovalEntity entity) {
        ToolExecutionApprovalResponse resp = new ToolExecutionApprovalResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setStepId(entity.getStepId() != null ? entity.getStepId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId().toString());
        resp.setToolId(entity.getToolId() != null ? entity.getToolId().toString() : null);
        resp.setToolKey(entity.getToolKey());
        resp.setApprovalKey(entity.getApprovalKey());
        resp.setTitle(entity.getTitle());
        resp.setDescription(entity.getDescription());
        resp.setRiskLevel(entity.getRiskLevel());
        resp.setStatus(entity.getStatus());
        resp.setRequestedBy(entity.getRequestedBy() != null ? entity.getRequestedBy().toString() : null);
        resp.setDecidedBy(entity.getDecidedBy() != null ? entity.getDecidedBy().toString() : null);
        resp.setDecisionComment(entity.getDecisionComment());
        resp.setRequestedAt(entity.getRequestedAt() != null ? entity.getRequestedAt().toString() : null);
        resp.setDecidedAt(entity.getDecidedAt() != null ? entity.getDecidedAt().toString() : null);
        resp.setExpiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().toString() : null);
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return resp;
    }
}
