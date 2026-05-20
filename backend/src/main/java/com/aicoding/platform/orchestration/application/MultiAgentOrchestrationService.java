package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.domain.ProjectAgentConfigEntity;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.agent.infrastructure.ProjectAgentConfigMapper;
import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.MultiAgentApprovalGateEntity;
import com.aicoding.platform.orchestration.domain.MultiAgentApprovalStatus;
import com.aicoding.platform.orchestration.domain.MultiAgentMessageEntity;
import com.aicoding.platform.orchestration.domain.MultiAgentMessageType;
import com.aicoding.platform.orchestration.domain.MultiAgentPhaseEntity;
import com.aicoding.platform.orchestration.domain.MultiAgentPhaseStatus;
import com.aicoding.platform.orchestration.domain.MultiAgentRunEntity;
import com.aicoding.platform.orchestration.domain.MultiAgentRunStatus;
import com.aicoding.platform.orchestration.domain.MultiAgentStepEntity;
import com.aicoding.platform.orchestration.domain.MultiAgentStepStatus;
import com.aicoding.platform.orchestration.domain.MultiAgentStepType;
import com.aicoding.platform.orchestration.dto.MultiAgentApprovalDecisionRequest;
import com.aicoding.platform.orchestration.dto.MultiAgentApprovalGateResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentMessageResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentPhaseResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentRunResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentStepResponse;
import com.aicoding.platform.orchestration.dto.StartMultiAgentRunRequest;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentApprovalGateMapper;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentMessageMapper;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentPhaseMapper;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentRunMapper;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentStepMapper;
import com.aicoding.platform.orchestrator.domain.AgentExecutionEntity;
import com.aicoding.platform.orchestrator.domain.AgentExecutionStatus;
import com.aicoding.platform.orchestrator.domain.AgentExecutionType;
import com.aicoding.platform.orchestrator.infrastructure.AgentExecutionMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.domain.AiTaskEntity;
import com.aicoding.platform.task.domain.AiTaskEventEntity;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskArtifactType;
import com.aicoding.platform.task.domain.TaskEventType;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.domain.TaskStatus;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.aicoding.platform.task.infrastructure.AiTaskEventMapper;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MultiAgentOrchestrationService {

    private final MultiAgentRunMapper multiAgentRunMapper;
    private final MultiAgentStepMapper multiAgentStepMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AiAgentMapper aiAgentMapper;
    private final ProjectAgentConfigMapper projectAgentConfigMapper;
    private final AgentExecutionMapper agentExecutionMapper;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final AiTaskEventMapper aiTaskEventMapper;
    private final ProjectPermissionService projectPermissionService;
    private final MultiAgentMessageMapper multiAgentMessageMapper;
    private final MultiAgentPhaseMapper multiAgentPhaseMapper;
    private final WorkflowStrategyCatalogService workflowStrategyCatalogService;
    private final MultiAgentApprovalGateMapper multiAgentApprovalGateMapper;
    private final AuditLogApplicationService auditLogApplicationService;

    public MultiAgentOrchestrationService(MultiAgentRunMapper multiAgentRunMapper,
                                           MultiAgentStepMapper multiAgentStepMapper,
                                           AiTaskMapper aiTaskMapper,
                                           AiAgentMapper aiAgentMapper,
                                           ProjectAgentConfigMapper projectAgentConfigMapper,
                                           AgentExecutionMapper agentExecutionMapper,
                                           AiTaskLogMapper aiTaskLogMapper,
                                           AiTaskArtifactMapper aiTaskArtifactMapper,
                                           AiTaskEventMapper aiTaskEventMapper,
                                           ProjectPermissionService projectPermissionService,
                                           MultiAgentMessageMapper multiAgentMessageMapper,
                                           MultiAgentPhaseMapper multiAgentPhaseMapper,
                                           WorkflowStrategyCatalogService workflowStrategyCatalogService,
                                           MultiAgentApprovalGateMapper multiAgentApprovalGateMapper,
                                           AuditLogApplicationService auditLogApplicationService) {
        this.multiAgentRunMapper = multiAgentRunMapper;
        this.multiAgentStepMapper = multiAgentStepMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.projectAgentConfigMapper = projectAgentConfigMapper;
        this.agentExecutionMapper = agentExecutionMapper;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.aiTaskEventMapper = aiTaskEventMapper;
        this.projectPermissionService = projectPermissionService;
        this.multiAgentMessageMapper = multiAgentMessageMapper;
        this.multiAgentPhaseMapper = multiAgentPhaseMapper;
        this.workflowStrategyCatalogService = workflowStrategyCatalogService;
        this.multiAgentApprovalGateMapper = multiAgentApprovalGateMapper;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public MultiAgentRunResponse startRun(Long taskId, StartMultiAgentRunRequest request) {
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        Long projectId = task.getProjectId();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        String taskStatus = task.getStatus();
        if (TaskStatus.COMPLETED.name().equals(taskStatus)) {
            throw new BizException(ErrorCode.CONFLICT, "任务已完成，无法启动多智能体编排");
        }

        String rawStrategy = request.getStrategy() != null && !request.getStrategy().isBlank()
                ? request.getStrategy() : null;
        if (rawStrategy != null && !workflowStrategyCatalogService.isValidStrategy(rawStrategy)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的策略: " + rawStrategy);
        }
        String normalizedKey = workflowStrategyCatalogService.normalizeStrategyKey(rawStrategy);
        WorkflowStrategyCatalogService.StrategyTemplate strategyTemplate =
                workflowStrategyCatalogService.resolveTemplate(normalizedKey);

        String instruction = request.getInstruction() != null ? request.getInstruction() : "";
        LocalDateTime now = LocalDateTime.now();

        // Create run
        MultiAgentRunEntity run = new MultiAgentRunEntity();
        run.setProjectId(projectId);
        run.setTaskId(taskId);
        run.setStatus(MultiAgentRunStatus.RUNNING.name());
        run.setStrategy(normalizedKey);
        run.setTitle("Multi-Agent Phased Run");
        run.setInputSummary(instruction.isBlank() ? task.getTitle() : task.getTitle() + " | " + instruction);
        run.setStartedAt(now);
        multiAgentRunMapper.insert(run);

        if (TaskStatus.PENDING.name().equals(taskStatus)) {
            task.setStatus(TaskStatus.RUNNING.name());
            task.setStartTime(now);
            aiTaskMapper.updateById(task);
            writeEvent(taskId, projectId, TaskStatus.PENDING.name(), TaskStatus.RUNNING.name(),
                    TaskEventType.STARTED.name(), "Multi-Agent phased orchestration started");
        }

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_PHASED_START",
                "多智能体分阶段编排启动，策略: " + strategyTemplate.getStrategyKey());

        // Create all phases upfront from strategy template
        List<MultiAgentPhaseEntity> phases = new ArrayList<>();
        for (WorkflowStrategyCatalogService.PhaseTemplate spt : strategyTemplate.getPhases()) {
            MultiAgentPhaseEntity phase = new MultiAgentPhaseEntity();
            phase.setRunId(run.getId());
            phase.setProjectId(projectId);
            phase.setTaskId(taskId);
            phase.setPhaseOrder(spt.getPhaseOrder());
            phase.setPhaseKey(spt.getPhaseKey());
            phase.setTitle(spt.getTitle());
            phase.setStatus(MultiAgentPhaseStatus.PENDING.name());
            multiAgentPhaseMapper.insert(phase);
            phases.add(phase);
        }

        // Determine if there are approval gates and where to stop
        List<WorkflowStrategyCatalogService.ApprovalGateTemplate> gates = strategyTemplate.getApprovalGates();
        int stopAfterPhase = gates.isEmpty() ? -1 : gates.get(0).getAfterPhaseOrder();

        // Execute phases in order, stopping at approval gate if present
        List<MultiAgentStepEntity> allSteps = new ArrayList<>();
        StringBuilder finalSummaryBuilder = new StringBuilder();
        List<String> phaseOutputSummaries = new ArrayList<>();
        int globalStepOrder = 1;
        boolean runFailed = false;

        for (int pi = 0; pi < phases.size() && !runFailed; pi++) {
            MultiAgentPhaseEntity phase = phases.get(pi);
            WorkflowStrategyCatalogService.PhaseTemplate spt = strategyTemplate.getPhases().get(pi);

            // Execute this phase
            boolean phaseFailed = executePhase(run, task, instruction, phases, phaseOutputSummaries,
                    allSteps, finalSummaryBuilder, globalStepOrder, pi, phase, spt, strategyTemplate, projectId, taskId);

            if (phaseFailed) {
                runFailed = true;
                continue;
            }

            int completedGlobalOrder = allSteps.stream()
                    .mapToInt(MultiAgentStepEntity::getStepOrder).max().orElse(0);
            globalStepOrder = completedGlobalOrder + 1;

            // Check if we should stop here for an approval gate
            if (stopAfterPhase > 0 && phase.getPhaseOrder() == stopAfterPhase
                    && !gates.isEmpty()) {
                WorkflowStrategyCatalogService.ApprovalGateTemplate gate = gates.get(0);

                // Create approval gate
                MultiAgentApprovalGateEntity gateEntity = new MultiAgentApprovalGateEntity();
                gateEntity.setRunId(run.getId());
                gateEntity.setProjectId(projectId);
                gateEntity.setTaskId(taskId);
                gateEntity.setPhaseId(phase.getId());
                gateEntity.setGateKey(gate.getGateKey());
                gateEntity.setTitle(gate.getTitle());
                gateEntity.setDescription(gate.getDescription());
                gateEntity.setStatus(MultiAgentApprovalStatus.PENDING.name());
                gateEntity.setRequestedBy(LoginUserContext.currentUserId());
                gateEntity.setRequestedAt(LocalDateTime.now());
                multiAgentApprovalGateMapper.insert(gateEntity);

                // Create APPROVAL_REQUEST message
                createMessage(run.getId(), projectId, taskId, null, null,
                        null, null, MultiAgentMessageType.APPROVAL_REQUEST,
                        buildApprovalRequestContent(gate, phase, task),
                        "审批闸门已创建: " + gate.getTitle());

                writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_APPROVAL_REQUESTED",
                        "审批请求已创建: " + gate.getTitle() + " (gateId=" + gateEntity.getId() + ")");

                // Set run to WAITING_APPROVAL
                run.setStatus(MultiAgentRunStatus.WAITING_APPROVAL.name());
                multiAgentRunMapper.updateById(run);

                // Tag the gate phase with WAITING_APPROVAL
                phase.setStatus(MultiAgentPhaseStatus.WAITING_APPROVAL.name());
                multiAgentPhaseMapper.updateById(phase);

                // Load and return response with pending approval gate
                List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                        new LambdaQueryWrapper<MultiAgentMessageEntity>()
                                .eq(MultiAgentMessageEntity::getRunId, run.getId())
                                .orderByAsc(MultiAgentMessageEntity::getCreateTime));

                MultiAgentRunResponse response = toRunResponse(run, allSteps, messages, phases);
                List<MultiAgentApprovalGateEntity> gateEntities = multiAgentApprovalGateMapper.selectList(
                        new LambdaQueryWrapper<MultiAgentApprovalGateEntity>()
                                .eq(MultiAgentApprovalGateEntity::getRunId, run.getId()));
                response.setApprovalGates(gateEntities.stream().map(this::toApprovalGateResponse).collect(Collectors.toList()));
                if (!gateEntities.isEmpty()) {
                    response.setPendingApprovalGate(toApprovalGateResponse(gateEntities.get(0)));
                }
                return response;
            }
        }

        // Mark remaining phases as SKIPPED if run failed
        if (runFailed) {
            for (MultiAgentPhaseEntity phase : phases) {
                if (phase.getStatus().equals(MultiAgentPhaseStatus.PENDING.name())) {
                    phase.setStatus(MultiAgentPhaseStatus.SKIPPED.name());
                    multiAgentPhaseMapper.updateById(phase);
                }
            }
        }

        // Build final summary
        String finalSummary = finalSummaryBuilder.length() > 0
                ? finalSummaryBuilder.toString()
                : "## 多智能体协作总结\n\n本次 Mock 多智能体分阶段编排已完成。\n\n### 阶段摘要\n\n"
                        + String.join("\n", phaseOutputSummaries);

        // Write task artifact
        AiTaskArtifactEntity artifact = new AiTaskArtifactEntity();
        artifact.setTaskId(taskId);
        artifact.setProjectId(projectId);
        artifact.setArtifactType(TaskArtifactType.REPORT.name());
        artifact.setName("Multi-Agent Mock Orchestration Summary");
        artifact.setContent(finalSummary);
        aiTaskArtifactMapper.insert(artifact);

        writeEvent(taskId, projectId, TaskStatus.RUNNING.name(),
                runFailed ? TaskStatus.FAILED.name() : TaskStatus.COMPLETED.name(),
                runFailed ? TaskEventType.FAILED.name() : TaskEventType.COMPLETED.name(),
                "Multi-Agent phased orchestration " + (runFailed ? "failed" : "completed"));

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_PHASED_DONE",
                "多智能体分阶段编排完成，" + phases.size() + " 个 Phase, "
                        + allSteps.size() + " 个步骤, "
                        + allSteps.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count() + " 完成, "
                        + allSteps.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count() + " 跳过");

        // Complete/fail task
        task.setStatus(runFailed ? TaskStatus.FAILED.name() : TaskStatus.COMPLETED.name());
        task.setEndTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // Complete/fail run
        run.setStatus(runFailed ? MultiAgentRunStatus.FAILED.name() : MultiAgentRunStatus.COMPLETED.name());
        run.setFinalSummary(finalSummary);
        run.setFinishedAt(LocalDateTime.now());
        multiAgentRunMapper.updateById(run);

        // Load for response
        List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<MultiAgentMessageEntity>()
                        .eq(MultiAgentMessageEntity::getRunId, run.getId())
                        .orderByAsc(MultiAgentMessageEntity::getCreateTime));

        return toRunResponse(run, allSteps, messages, phases);
    }

    /**
     * Execute a single phase — returns true if phase failed.
     */
    private boolean executePhase(MultiAgentRunEntity run, AiTaskEntity task, String instruction,
                                  List<MultiAgentPhaseEntity> allPhases, List<String> phaseOutputSummaries,
                                  List<MultiAgentStepEntity> allSteps, StringBuilder finalSummaryBuilder,
                                  int globalStepOrder, int pi, MultiAgentPhaseEntity phase,
                                  WorkflowStrategyCatalogService.PhaseTemplate spt,
                                  WorkflowStrategyCatalogService.StrategyTemplate strategyTemplate,
                                  Long projectId, Long taskId) {
        // Start phase
        phase.setStatus(MultiAgentPhaseStatus.RUNNING.name());
        phase.setStartedAt(LocalDateTime.now());

        // Build phase input summary from prior phase outputs
        String phaseInputSummary = buildPhaseInputSummary(pi, phaseOutputSummaries, task, instruction);
        phase.setInputSummary(phaseInputSummary);
        multiAgentPhaseMapper.updateById(phase);

        // TASK_CONTEXT for Phase 1
        if (pi == 0) {
            createMessage(run.getId(), projectId, taskId, null, null,
                    null, null, MultiAgentMessageType.TASK_CONTEXT,
                    buildTaskContextContent(task, instruction),
                    "任务上下文已传入 Phase 1 (" + spt.getTitle() + ")");
        }

        // HANDOFF from previous phase to this one
        if (pi > 0) {
            MultiAgentPhaseEntity prevPhase = allPhases.get(pi - 1);
            createMessage(run.getId(), projectId, taskId, null, null,
                    null, null, MultiAgentMessageType.HANDOFF,
                    buildPhaseHandoffContent(prevPhase, phase),
                    "Phase " + prevPhase.getPhaseOrder() + " → Phase " + phase.getPhaseOrder());
        }

        // FINAL_CONTEXT + REVIEW_FEEDBACK before last phase
        boolean isLastPhase = (pi == allPhases.size() - 1);
        if (isLastPhase) {
            if (pi > 0) {
                MultiAgentPhaseEntity prevPhase = allPhases.get(pi - 1);
                if ("REVIEW".equals(prevPhase.getPhaseKey())) {
                    createMessage(run.getId(), projectId, taskId, null, null,
                            null, null, MultiAgentMessageType.REVIEW_FEEDBACK,
                            buildPhaseReviewFeedback(prevPhase),
                            "审查阶段反馈已传递给 Summary");
                }
            }
            createMessage(run.getId(), projectId, taskId, null, null,
                    null, null, MultiAgentMessageType.FINAL_CONTEXT,
                    buildFinalContextFromPhases(allPhases.subList(0, pi)),
                    "所有已完成 Phase 上下文已聚合");
        }

        // Execute all steps in this phase
        List<MultiAgentStepEntity> phaseSteps = new ArrayList<>();
        boolean phaseHasFailed = false;
        boolean allSkipped = true;
        int stepOrder = globalStepOrder;

        for (WorkflowStrategyCatalogService.StepTemplate sst : spt.getSteps()) {
            MultiAgentStepEntity step = new MultiAgentStepEntity();
            step.setRunId(run.getId());
            step.setPhaseId(phase.getId());
            step.setPhaseOrder(phase.getPhaseOrder());
            step.setLaneKey(sst.getLaneKey());
            step.setProjectId(projectId);
            step.setTaskId(taskId);
            step.setStepOrder(stepOrder);
            step.setStepType(sst.getStepType());
            step.setStatus(MultiAgentStepStatus.RUNNING.name());
            step.setStartedAt(LocalDateTime.now());
            multiAgentStepMapper.insert(step);

            AiAgentEntity agent = aiAgentMapper.selectOne(
                    new LambdaQueryWrapper<AiAgentEntity>()
                            .eq(AiAgentEntity::getCode, sst.getAgentCode()));

            if (agent == null || !"ENABLED".equals(agent.getStatus())) {
                step.setStatus(MultiAgentStepStatus.SKIPPED.name());
                step.setFinishedAt(LocalDateTime.now());
                multiAgentStepMapper.updateById(step);
                writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "STEP_SKIPPED",
                        "步骤 [" + sst.getStepType() + "] Agent " + sst.getAgentCode() + " 不存在或已停用，跳过");
                phaseSteps.add(step);
                allSteps.add(step);
                stepOrder++;
                continue;
            }

            step.setAgentId(agent.getId());

            ProjectAgentConfigEntity config = projectAgentConfigMapper.selectOne(
                    new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                            .eq(ProjectAgentConfigEntity::getProjectId, projectId)
                            .eq(ProjectAgentConfigEntity::getAgentId, agent.getId()));

            if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
                step.setStatus(MultiAgentStepStatus.SKIPPED.name());
                step.setFinishedAt(LocalDateTime.now());
                multiAgentStepMapper.updateById(step);
                writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "STEP_SKIPPED",
                        "步骤 [" + sst.getStepType() + "] Agent " + agent.getName() + " 未在项目中启用，跳过");
                phaseSteps.add(step);
                allSteps.add(step);
                stepOrder++;
                continue;
            }

            // Build input context with phase awareness
            MultiAgentStepType stepType = MultiAgentStepType.valueOf(sst.getStepType());
            String inputContext = buildInputContext(stepType, task, instruction, phase, phaseInputSummary,
                    pi > 0 ? allPhases.subList(0, pi) : List.of());
            step.setInputContext(inputContext);

            // Create AgentExecution
            AgentExecutionEntity execution = new AgentExecutionEntity();
            execution.setProjectId(projectId);
            execution.setTaskId(taskId);
            execution.setAgentId(agent.getId());
            execution.setAgentVersionId(config.getAgentVersionId());
            execution.setExecutionType(AgentExecutionType.TASK.name());
            execution.setStatus(AgentExecutionStatus.RUNNING.name());
            execution.setInputPrompt(inputContext);
            execution.setStartedAt(LocalDateTime.now());
            agentExecutionMapper.insert(execution);

            // Generate mock output
            String outputContent = generateMockOutput(stepType, agent.getName(), task.getTitle(), instruction,
                    phase, allPhases.subList(0, pi), sst.getLaneKey(), spt, strategyTemplate);
            execution.setOutputContent(outputContent);
            execution.setStatus(AgentExecutionStatus.COMPLETED.name());
            execution.setFinishedAt(LocalDateTime.now());
            execution.setTokenUsage((long) (outputContent.length() / 2));
            agentExecutionMapper.updateById(execution);

            step.setAgentExecutionId(execution.getId());
            step.setOutputContent(outputContent);
            step.setStatus(MultiAgentStepStatus.COMPLETED.name());
            step.setFinishedAt(LocalDateTime.now());
            multiAgentStepMapper.updateById(step);

            // STEP_OUTPUT message
            String stepSummary = generateStepSummary(stepType);
            createMessage(run.getId(), projectId, taskId, step.getId(), null,
                    agent.getId(), null, MultiAgentMessageType.STEP_OUTPUT,
                    outputContent, stepSummary);

            writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "STEP_DONE",
                    "Phase " + phase.getPhaseOrder() + " [" + sst.getStepType() + "] " + agent.getName() + " 完成 (lane: " + sst.getLaneKey() + ")");

            if (sst.getStepType().equals("FINAL_SUMMARY")) {
                finalSummaryBuilder.append(outputContent);
            }

            phaseSteps.add(step);
            allSteps.add(step);
            stepOrder++;
            allSkipped = false;
        }

        // Determine phase status
        if (phaseHasFailed) {
            phase.setStatus(MultiAgentPhaseStatus.FAILED.name());
        } else if (allSkipped) {
            phase.setStatus(MultiAgentPhaseStatus.SKIPPED.name());
        } else {
            phase.setStatus(MultiAgentPhaseStatus.COMPLETED.name());
        }

        phase.setFinishedAt(LocalDateTime.now());

        // Build phase output summary
        String phaseOutputSummary = buildPhaseOutputSummary(phase, phaseSteps);
        phase.setOutputSummary(phaseOutputSummary);
        multiAgentPhaseMapper.updateById(phase);

        if (!allSkipped && !phaseHasFailed) {
            phaseOutputSummaries.add(phaseOutputSummary);
        }

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "PHASE_DONE",
                "Phase " + phase.getPhaseOrder() + " [" + spt.getPhaseKey() + "] " + phase.getStatus()
                        + ", " + phaseSteps.size() + " steps");

        return phaseHasFailed;
    }

    // ========================
    // Phase input/output summaries
    // ========================

    private String buildPhaseInputSummary(int phaseIdx, List<String> priorOutputs, AiTaskEntity task, String instruction) {
        StringBuilder sb = new StringBuilder();
        if (phaseIdx == 0) {
            sb.append("## Phase 1 输入\n\n任务: ").append(task.getTitle()).append("\n");
            if (!instruction.isBlank()) sb.append("附加指令: ").append(instruction).append("\n");
        } else {
            sb.append("## Phase ").append(phaseIdx + 1).append(" 输入\n\n");
            sb.append("已聚合前序 Phase 输出：\n\n");
            for (int i = 0; i < priorOutputs.size(); i++) {
                sb.append("- **Phase ").append(i + 1).append("**: ").append(priorOutputs.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildPhaseOutputSummary(MultiAgentPhaseEntity phase, List<MultiAgentStepEntity> steps) {
        StringBuilder sb = new StringBuilder();
        List<MultiAgentStepEntity> completed = steps.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus())).toList();
        sb.append(phase.getPhaseKey()).append(": ").append(phase.getTitle());
        if (completed.isEmpty()) {
            sb.append(" (无已完成步骤)");
        } else {
            sb.append(" - ");
            List<String> names = new ArrayList<>();
            for (MultiAgentStepEntity s : completed) {
                AiAgentEntity a = aiAgentMapper.selectById(s.getAgentId());
                names.add(a != null ? a.getName() : s.getStepType());
            }
            sb.append(String.join(", ", names));
        }
        return sb.toString();
    }

    // ========================
    // Message content builders (phase-aware)
    // ========================

    private String buildPhaseHandoffContent(MultiAgentPhaseEntity from, MultiAgentPhaseEntity to) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Phase 交接\n\n");
        sb.append("**来源**: Phase ").append(from.getPhaseOrder()).append(" (").append(from.getTitle()).append(")\n");
        sb.append("**目标**: Phase ").append(to.getPhaseOrder()).append(" (").append(to.getTitle()).append(")\n\n");
        if (from.getOutputSummary() != null) {
            sb.append("### 上阶段输出摘要\n\n").append(from.getOutputSummary()).append("\n\n");
        }
        sb.append("### 对下一阶段的建议\n\n请基于上一阶段的输出继续推进。\n");
        return sb.toString();
    }

    private String buildPhaseReviewFeedback(MultiAgentPhaseEntity reviewPhase) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 审查阶段反馈\n\n");
        sb.append("Phase ").append(reviewPhase.getPhaseOrder()).append(" (").append(reviewPhase.getTitle()).append(") 已完成。\n\n");
        if (reviewPhase.getOutputSummary() != null) {
            sb.append(reviewPhase.getOutputSummary()).append("\n\n");
        }
        sb.append("### 需要最终总结关注\n\n- 汇总审查发现的风险和建议\n- 形成可操作的改进方案\n");
        return sb.toString();
    }

    private String buildFinalContextFromPhases(List<MultiAgentPhaseEntity> priorPhases) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 所有已完成 Phase 摘要\n\n");
        sb.append("| Phase | Key | 标题 | 状态 |\n");
        sb.append("|---|---|---|---|\n");
        for (MultiAgentPhaseEntity p : priorPhases) {
            sb.append("| ").append(p.getPhaseOrder())
                    .append(" | ").append(p.getPhaseKey())
                    .append(" | ").append(p.getTitle())
                    .append(" | ").append(p.getStatus())
                    .append(" |\n");
        }
        sb.append("\n请基于以上所有 Phase 输出生成最终总结报告。\n");
        return sb.toString();
    }

    private String buildTaskContextContent(AiTaskEntity task, String instruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 任务上下文\n\n");
        sb.append("- **任务标题**: ").append(task.getTitle()).append("\n");
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append("- **任务描述**: ").append(task.getDescription()).append("\n");
        }
        sb.append("- **任务类型**: ").append(task.getTaskType() != null ? task.getTaskType() : "N/A").append("\n");
        sb.append("- **优先级**: ").append(task.getPriority() != null ? task.getPriority() : "N/A").append("\n");
        if (!instruction.isBlank()) {
            sb.append("- **附加指令**: ").append(instruction).append("\n");
        }
        sb.append("\n请基于以上任务上下文开始分阶段多智能体协作。\n");
        return sb.toString();
    }

    // ========================
    // Step summary generator
    // ========================

    private String generateStepSummary(MultiAgentStepType stepType) {
        return switch (stepType) {
            case ARCHITECTURE_ANALYSIS -> "架构分析完成：识别了后端接口、权限、测试风险。";
            case BACKEND_IMPLEMENTATION_PLAN -> "后端实现计划完成：Service、API、数据一致性方案已制定。";
            case FRONTEND_IMPLEMENTATION_PLAN -> "前端实现计划完成：页面入口、API 类型、状态交互方案已制定。";
            case TEST_PLAN -> "测试计划完成：单元测试、集成测试、E2E 覆盖方案已制定。";
            case CODE_REVIEW -> "代码审查完成：权限、状态机、安全性检查已通过。";
            case FINAL_SUMMARY -> "最终总结完成：聚合了所有 Phase 和 Agent 的输出。";
        };
    }

    // ========================
    // Mock output generators (phase-aware)
    // ========================

    private String generateMockOutput(MultiAgentStepType stepType, String agentName, String taskTitle,
                                       String instruction, MultiAgentPhaseEntity phase,
                                       List<MultiAgentPhaseEntity> priorPhases,
                                       String laneKey, WorkflowStrategyCatalogService.PhaseTemplate spt,
                                       WorkflowStrategyCatalogService.StrategyTemplate strategyTemplate) {
        String header = "> 智能体: " + agentName + "\n> 任务: " + taskTitle + "\n"
                + "> Phase: " + phase.getPhaseOrder() + " (" + phase.getTitle() + ")\n"
                + "> Lane: " + laneKey + "\n"
                + "> 时间: " + LocalDateTime.now() + "\n\n---\n\n";

        // Consumed context section
        StringBuilder ctx = new StringBuilder();
        if (!priorPhases.isEmpty()) {
            ctx.append("## 已消费上游 Phase 上下文\n\n");
            for (MultiAgentPhaseEntity p : priorPhases) {
                ctx.append("- **Phase ").append(p.getPhaseOrder()).append("** (").append(p.getTitle()).append(")\n");
            }
            ctx.append("\n");
        }

        // Parallel role awareness for phases with multiple steps
        String parallelNote = "";
        if (spt.getSteps().size() > 1) {
            StringBuilder pn = new StringBuilder();
            pn.append("## 同阶段并行角色\n\n");
            pn.append("本步骤属于 ").append(spt.getTitle()).append("，同阶段并行执行：\n\n");
            for (WorkflowStrategyCatalogService.StepTemplate sst : spt.getSteps()) {
                pn.append("- **").append(sst.getTitle()).append("** (").append(sst.getAgentCode()).append("): ").append(sst.getTitle()).append("\n");
            }
            pn.append("\n");
            parallelNote = pn.toString();
        }

        return switch (stepType) {
            case ARCHITECTURE_ANALYSIS -> header + ctx
                    + "## 架构分析\n\n"
                    + "- **识别任务目标**: " + taskTitle + "\n"
                    + "- **影响模块**: Task Orchestrator, Agent Service, Model Gateway\n"
                    + "- **推荐分层方案**: Controller → Application Service → Domain → Infrastructure\n"
                    + "- **主要风险**: 状态机边界、权限校验完整性\n";

            case BACKEND_IMPLEMENTATION_PLAN -> header + ctx + parallelNote
                    + "## 后端实现计划\n\n"
                    + "基于 Architect Agent 的架构规划，本步骤建议：\n\n"
                    + "- **需新增 Service**: MultiAgentOrchestrationService\n"
                    + "- **API 行为**: POST /api/tasks/{taskId}/multi-agent-runs\n"
                    + "- **数据一致性**: 事务内完成 Phase + Step 创建\n"
                    + "- **测试建议**: 覆盖权限校验、状态流转、Mock 输出验证\n";

            case FRONTEND_IMPLEMENTATION_PLAN -> header + ctx + parallelNote
                    + "## 前端实现计划\n\n"
                    + "基于 Architect Agent 的架构规划，本步骤建议：\n\n"
                    + "- **页面入口与路由**: 多智能体 Tab 集成 Phase / Lane 视图\n"
                    + "- **API client 类型定义**: MultiAgentPhaseResponse, MultiAgentStepResponse\n"
                    + "- **状态展示与交互反馈**: Phase 卡片、Lane 泳道、StatusPulse\n"
                    + "- **E2E 覆盖点**: Phase 视图、Lane 展开、消息链路\n";

            case TEST_PLAN -> header + ctx + parallelNote
                    + "## 测试计划\n\n"
                    + "基于 Architect、Backend 和 Frontend 的输出，建议覆盖：\n\n"
                    + "- **单元测试**: MultiAgentOrchestrationService Phase 流转\n"
                    + "- **集成测试**: API 鉴权 + Phase/Lane 生命周期\n"
                    + "- **E2E 验证**: 前端 Phase/Lane 交互\n"
                    + "- **回归风险**: 不破坏已有单 Agent 执行链路\n";

            case CODE_REVIEW -> {
                StringBuilder reviewCtx = new StringBuilder(ctx.toString());
                reviewCtx.append("## 已聚合 Phase 2 输出\n\n");
                reviewCtx.append("本审查基于实现阶段三个并行 Lane 的输出：\n\n");
                reviewCtx.append("- **Backend**: 后端 Service、API、数据方案\n");
                reviewCtx.append("- **Frontend**: 前端路由、类型、交互方案\n");
                reviewCtx.append("- **Test**: 测试覆盖方案\n\n");
                yield header + reviewCtx
                        + "## 代码审查清单\n\n"
                        + "基于前序 Phase 方案，发现以下关注点：\n\n"
                        + "- **权限检查**: checkProjectRole(OWNER, MAINTAINER, DEVELOPER) ✓\n"
                        + "- **状态机完整性**: PENDING → RUNNING → COMPLETED ✓\n"
                        + "- **Phase 状态计算**: 基于 Step 状态聚合 ✓\n"
                        + "- **敏感信息泄露**: 无泄露风险 ✓\n"
                        + "- **已有接口兼容性**: POST /execute 不受影响 ✓\n";
            }

            case FINAL_SUMMARY -> {
                int totalSteps = strategyTemplate.getPhases().stream()
                        .mapToInt(p -> p.getSteps().size()).sum();
                StringBuilder phaseTable = new StringBuilder();
                phaseTable.append("| Phase | 标题 | 步骤 |\n");
                phaseTable.append("|---|---|---|\n");
                for (WorkflowStrategyCatalogService.PhaseTemplate p : strategyTemplate.getPhases()) {
                    List<String> stepNames = p.getSteps().stream()
                            .map(WorkflowStrategyCatalogService.StepTemplate::getTitle)
                            .toList();
                    phaseTable.append("| ").append(p.getPhaseOrder())
                            .append(" | ").append(p.getTitle())
                            .append(" | ").append(String.join(" + ", stepNames))
                            .append(" |\n");
                }
                StringBuilder phaseSummary = new StringBuilder();
                for (WorkflowStrategyCatalogService.PhaseTemplate p : strategyTemplate.getPhases()) {
                    phaseSummary.append("- **").append(p.getPhaseKey()).append("**: ").append(p.getTitle()).append("\n");
                }
                yield """
                        ## 多智能体分阶段协作总结

                        本次任务 **%s** 已完成 Mock 多智能体分阶段协作分析。

                        ### 参与 Phase

                        %s
                        ### 已聚合所有 Phase

                        %s
                        ### 附加指令

                        %s

                        ### 结论

                        所有 Phase 已完成，共 %d 个步骤，输出已归档为 Task Artifact。
                        """.formatted(taskTitle, phaseTable.toString(), phaseSummary.toString(),
                        instruction.isBlank() ? "（无）" : instruction, totalSteps);
            }

            default -> header + ctx + parallelNote + "Mock output for " + stepType.name() + "\n";
        };
    }

    // ========================
    // Input context builder (phase-aware)
    // ========================

    private String buildInputContext(MultiAgentStepType stepType, AiTaskEntity task, String instruction,
                                      MultiAgentPhaseEntity phase, String phaseInputSummary,
                                      List<MultiAgentPhaseEntity> priorPhases) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 当前步骤上下文\n\n");

        sb.append("## Phase 信息\n\n");
        sb.append("- **Phase**: ").append(phase.getPhaseOrder()).append(" (").append(phase.getTitle()).append(")\n");
        sb.append("- **Phase Key**: ").append(phase.getPhaseKey()).append("\n");
        sb.append("- **步骤类型**: ").append(stepType.name()).append("\n");

        sb.append("\n## 任务上下文\n\n");
        sb.append("- **任务标题**: ").append(task.getTitle()).append("\n");
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append("- **任务描述**: ").append(task.getDescription()).append("\n");
        }
        if (!instruction.isBlank()) {
            sb.append("- **附加指令**: ").append(instruction).append("\n");
        }
        sb.append("\n");

        if (!priorPhases.isEmpty()) {
            sb.append("## 前序 Phase 输出摘要\n\n");
            for (MultiAgentPhaseEntity p : priorPhases) {
                sb.append("- **Phase ").append(p.getPhaseOrder()).append("** (").append(p.getTitle()).append("): ");
                sb.append(p.getOutputSummary() != null ? p.getOutputSummary() : "无输出").append("\n");
            }
            sb.append("\n");
        }

        sb.append("## Phase 输入上下文\n\n").append(phaseInputSummary).append("\n");

        return sb.toString();
    }

    // ========================
    // Read-only queries
    // ========================

    @Transactional(readOnly = true)
    public List<MultiAgentRunResponse> listRuns(Long taskId) {
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        projectPermissionService.checkProjectMember(task.getProjectId());

        List<MultiAgentRunEntity> runs = multiAgentRunMapper.selectList(
                new LambdaQueryWrapper<MultiAgentRunEntity>()
                        .eq(MultiAgentRunEntity::getTaskId, taskId)
                        .orderByDesc(MultiAgentRunEntity::getCreateTime));

        return runs.stream().map(run -> buildRunResponse(run)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MultiAgentRunResponse getRun(Long runId) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectMember(run.getProjectId());
        return buildRunResponse(run);
    }

    @Transactional(readOnly = true)
    public List<MultiAgentMessageResponse> getMessages(Long runId) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectMember(run.getProjectId());
        List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<MultiAgentMessageEntity>()
                        .eq(MultiAgentMessageEntity::getRunId, runId)
                        .orderByAsc(MultiAgentMessageEntity::getCreateTime));
        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MultiAgentPhaseResponse> getPhases(Long runId) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectMember(run.getProjectId());

        List<MultiAgentPhaseEntity> phases = multiAgentPhaseMapper.selectList(
                new LambdaQueryWrapper<MultiAgentPhaseEntity>()
                        .eq(MultiAgentPhaseEntity::getRunId, runId)
                        .orderByAsc(MultiAgentPhaseEntity::getPhaseOrder));

        return phases.stream().map(phase -> {
            List<MultiAgentStepEntity> steps = multiAgentStepMapper.selectList(
                    new LambdaQueryWrapper<MultiAgentStepEntity>()
                            .eq(MultiAgentStepEntity::getPhaseId, phase.getId())
                            .orderByAsc(MultiAgentStepEntity::getStepOrder));
            return toPhaseResponse(phase, steps);
        }).collect(Collectors.toList());
    }

    // ========================
    // Approval gate operations
    // ========================

    @Transactional(readOnly = true)
    public List<MultiAgentApprovalGateResponse> getApprovalGates(Long runId) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectMember(run.getProjectId());

        List<MultiAgentApprovalGateEntity> gates = multiAgentApprovalGateMapper.selectList(
                new LambdaQueryWrapper<MultiAgentApprovalGateEntity>()
                        .eq(MultiAgentApprovalGateEntity::getRunId, runId)
                        .orderByAsc(MultiAgentApprovalGateEntity::getCreateTime));
        return gates.stream().map(this::toApprovalGateResponse).collect(Collectors.toList());
    }

    @Transactional
    public MultiAgentRunResponse approveGate(Long runId, Long gateId, MultiAgentApprovalDecisionRequest request) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectRole(run.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (!MultiAgentRunStatus.WAITING_APPROVAL.name().equals(run.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "当前编排状态不允许审批，当前状态: " + run.getStatus());
        }

        MultiAgentApprovalGateEntity gate = multiAgentApprovalGateMapper.selectById(gateId);
        if (gate == null || !gate.getRunId().equals(runId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批闸门不存在");
        }
        if (!MultiAgentApprovalStatus.PENDING.name().equals(gate.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该审批闸门已被处理，当前状态: " + gate.getStatus());
        }

        Long projectId = run.getProjectId();
        Long taskId = run.getTaskId();

        // Mark gate APPROVED
        gate.setStatus(MultiAgentApprovalStatus.APPROVED.name());
        gate.setDecidedBy(LoginUserContext.currentUserId());
        gate.setDecisionComment(request.getComment());
        gate.setDecidedAt(LocalDateTime.now());
        multiAgentApprovalGateMapper.updateById(gate);

        // APPROVAL_DECISION message
        LoginUser currentUser = LoginUserContext.currentUser().orElse(null);
        String decisionContent = "## 审批决定: 批准\n\n**决定人**: "
                + (currentUser != null ? currentUser.getUsername() : "Unknown")
                + "\n**意见**: " + (request.getComment() != null ? request.getComment() : "无") + "\n";
        createMessage(runId, projectId, taskId, null, null, null, null,
                MultiAgentMessageType.APPROVAL_DECISION, decisionContent,
                "审批通过: " + gate.getTitle());

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_APPROVAL_APPROVED",
                "审批通过: " + gate.getTitle() + " (gateId=" + gateId + ")");

        // Audit
        auditLogApplicationService.recordSuccess(projectId, gateId,
                AuditActionType.MULTI_AGENT_APPROVE.name(),
                "multi_agent_approval_gate",
                "Approve multi-agent gate: " + gate.getTitle()
                        + " (runId=" + runId + ", taskId=" + taskId + ")");

        // Resume run
        run.setStatus(MultiAgentRunStatus.RUNNING.name());
        multiAgentRunMapper.updateById(run);

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_RESUMED_AFTER_APPROVAL",
                "审批通过，继续执行后续 Phase");

        // Continue executing remaining phases
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        String normalizedKey = run.getStrategy();
        WorkflowStrategyCatalogService.StrategyTemplate strategyTemplate =
                workflowStrategyCatalogService.resolveTemplate(normalizedKey);

        List<MultiAgentPhaseEntity> phases = multiAgentPhaseMapper.selectList(
                new LambdaQueryWrapper<MultiAgentPhaseEntity>()
                        .eq(MultiAgentPhaseEntity::getRunId, runId)
                        .orderByAsc(MultiAgentPhaseEntity::getPhaseOrder));

        List<MultiAgentStepEntity> existingSteps = multiAgentStepMapper.selectList(
                new LambdaQueryWrapper<MultiAgentStepEntity>()
                        .eq(MultiAgentStepEntity::getRunId, runId)
                        .orderByAsc(MultiAgentStepEntity::getStepOrder));

        // Restore accumulated state from completed phases
        List<String> phaseOutputSummaries = new ArrayList<>();
        for (MultiAgentPhaseEntity phase : phases) {
            if (MultiAgentPhaseStatus.COMPLETED.name().equals(phase.getStatus())
                    || MultiAgentPhaseStatus.WAITING_APPROVAL.name().equals(phase.getStatus())) {
                if (phase.getOutputSummary() != null && !phase.getOutputSummary().isBlank()) {
                    phaseOutputSummaries.add(phase.getOutputSummary());
                }
            }
        }

        StringBuilder finalSummaryBuilder = new StringBuilder();
        for (MultiAgentStepEntity step : existingSteps) {
            if ("FINAL_SUMMARY".equals(step.getStepType()) && step.getOutputContent() != null) {
                finalSummaryBuilder.append(step.getOutputContent());
            }
        }

        List<MultiAgentStepEntity> allSteps = new ArrayList<>(existingSteps);
        int globalStepOrder = existingSteps.stream()
                .mapToInt(MultiAgentStepEntity::getStepOrder).max().orElse(0) + 1;

        // Find gate phase index and reset its status
        int gatePhaseIdx = -1;
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).getId().equals(gate.getPhaseId())) {
                gatePhaseIdx = i;
                break;
            }
        }

        MultiAgentPhaseEntity gatePhase = phases.get(gatePhaseIdx);
        gatePhase.setStatus(MultiAgentPhaseStatus.COMPLETED.name());
        multiAgentPhaseMapper.updateById(gatePhase);

        boolean runFailed = false;
        String instruction = "";

        for (int pi = gatePhaseIdx + 1; pi < phases.size() && !runFailed; pi++) {
            MultiAgentPhaseEntity phase = phases.get(pi);
            WorkflowStrategyCatalogService.PhaseTemplate spt = strategyTemplate.getPhases().get(pi);

            boolean phaseFailed = executePhase(run, task, instruction, phases, phaseOutputSummaries,
                    allSteps, finalSummaryBuilder, globalStepOrder, pi, phase, spt, strategyTemplate, projectId, taskId);

            if (phaseFailed) {
                runFailed = true;
                continue;
            }

            int completedGlobalOrder = allSteps.stream()
                    .mapToInt(MultiAgentStepEntity::getStepOrder).max().orElse(0);
            globalStepOrder = completedGlobalOrder + 1;
        }

        // Mark skipped phases if failed
        if (runFailed) {
            for (MultiAgentPhaseEntity phase : phases) {
                if (MultiAgentPhaseStatus.PENDING.name().equals(phase.getStatus())) {
                    phase.setStatus(MultiAgentPhaseStatus.SKIPPED.name());
                    multiAgentPhaseMapper.updateById(phase);
                }
            }
        }

        completeRun(run, task, phases, allSteps, phaseOutputSummaries, finalSummaryBuilder, runFailed,
                " (after approval)");

        List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<MultiAgentMessageEntity>()
                        .eq(MultiAgentMessageEntity::getRunId, runId)
                        .orderByAsc(MultiAgentMessageEntity::getCreateTime));

        return toRunResponse(run, allSteps, messages, phases);
    }

    @Transactional
    public MultiAgentRunResponse rejectGate(Long runId, Long gateId, MultiAgentApprovalDecisionRequest request) {
        MultiAgentRunEntity run = multiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "多智能体编排记录不存在");
        }
        projectPermissionService.checkProjectRole(run.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (!MultiAgentRunStatus.WAITING_APPROVAL.name().equals(run.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "当前编排状态不允许审批，当前状态: " + run.getStatus());
        }

        MultiAgentApprovalGateEntity gate = multiAgentApprovalGateMapper.selectById(gateId);
        if (gate == null || !gate.getRunId().equals(runId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批闸门不存在");
        }
        if (!MultiAgentApprovalStatus.PENDING.name().equals(gate.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该审批闸门已被处理，当前状态: " + gate.getStatus());
        }

        Long projectId = run.getProjectId();
        Long taskId = run.getTaskId();

        // Mark gate REJECTED
        gate.setStatus(MultiAgentApprovalStatus.REJECTED.name());
        gate.setDecidedBy(LoginUserContext.currentUserId());
        gate.setDecisionComment(request.getComment());
        gate.setDecidedAt(LocalDateTime.now());
        multiAgentApprovalGateMapper.updateById(gate);

        // APPROVAL_DECISION message
        LoginUser currentUser2 = LoginUserContext.currentUser().orElse(null);
        String decisionContent2 = "## 审批决定: 驳回\n\n**决定人**: "
                + (currentUser2 != null ? currentUser2.getUsername() : "Unknown")
                + "\n**意见**: " + (request.getComment() != null ? request.getComment() : "无") + "\n";
        createMessage(runId, projectId, taskId, null, null, null, null,
                MultiAgentMessageType.APPROVAL_DECISION, decisionContent2,
                "审批驳回: " + gate.getTitle());

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_APPROVAL_REJECTED",
                "审批驳回: " + gate.getTitle() + " (gateId=" + gateId + ")");

        // Audit
        auditLogApplicationService.recordSuccess(projectId, gateId,
                AuditActionType.MULTI_AGENT_REJECT.name(),
                "multi_agent_approval_gate",
                "Reject multi-agent gate: " + gate.getTitle()
                        + " (runId=" + runId + ", taskId=" + taskId + ")");

        // Cancel run
        run.setStatus(MultiAgentRunStatus.CANCELED.name());
        run.setFinishedAt(LocalDateTime.now());
        multiAgentRunMapper.updateById(run);

        // Skip remaining phases
        List<MultiAgentPhaseEntity> phases = multiAgentPhaseMapper.selectList(
                new LambdaQueryWrapper<MultiAgentPhaseEntity>()
                        .eq(MultiAgentPhaseEntity::getRunId, runId)
                        .orderByAsc(MultiAgentPhaseEntity::getPhaseOrder));

        int gatePhaseIdx = -1;
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).getId().equals(gate.getPhaseId())) {
                gatePhaseIdx = i;
                break;
            }
        }

        // Reset WAITING_APPROVAL phase
        MultiAgentPhaseEntity gatePhase = phases.get(gatePhaseIdx);
        gatePhase.setStatus(MultiAgentPhaseStatus.SKIPPED.name());
        multiAgentPhaseMapper.updateById(gatePhase);

        // Skip later phases
        for (int pi = gatePhaseIdx + 1; pi < phases.size(); pi++) {
            MultiAgentPhaseEntity phase = phases.get(pi);
            phase.setStatus(MultiAgentPhaseStatus.SKIPPED.name());
            multiAgentPhaseMapper.updateById(phase);
        }

        // Cancel task
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        writeEvent(taskId, projectId, task.getStatus(), TaskStatus.CANCELED.name(),
                TaskEventType.CANCELED.name(), "Multi-Agent approval rejected");

        task.setStatus(TaskStatus.CANCELED.name());
        task.setEndTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_CANCELED",
                "多智能体编排已取消（审批驳回），" + phases.size() + " 个 Phase");

        // Load steps and messages for response
        List<MultiAgentStepEntity> steps = multiAgentStepMapper.selectList(
                new LambdaQueryWrapper<MultiAgentStepEntity>()
                        .eq(MultiAgentStepEntity::getRunId, runId)
                        .orderByAsc(MultiAgentStepEntity::getStepOrder));

        List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<MultiAgentMessageEntity>()
                        .eq(MultiAgentMessageEntity::getRunId, runId)
                        .orderByAsc(MultiAgentMessageEntity::getCreateTime));

        return toRunResponse(run, steps, messages, phases);
    }

    // ========================
    // Approval helpers
    // ========================

    private String buildApprovalRequestContent(WorkflowStrategyCatalogService.ApprovalGateTemplate gate,
                                               MultiAgentPhaseEntity phase, AiTaskEntity task) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 审批请求\n\n");
        sb.append("**闸门**: ").append(gate.getTitle()).append("\n\n");
        sb.append("**说明**: ").append(gate.getDescription()).append("\n\n");
        sb.append("**任务**: ").append(task.getTitle()).append("\n\n");
        sb.append("**已完成阶段**: Phase ").append(phase.getPhaseOrder())
                .append(" (").append(phase.getTitle()).append(")\n\n");
        sb.append("请确认是否继续执行后续阶段。\n");
        return sb.toString();
    }

    private MultiAgentApprovalGateResponse toApprovalGateResponse(MultiAgentApprovalGateEntity entity) {
        MultiAgentApprovalGateResponse resp = new MultiAgentApprovalGateResponse();
        resp.setId(entity.getId().toString());
        resp.setRunId(entity.getRunId().toString());
        resp.setPhaseId(entity.getPhaseId() != null ? entity.getPhaseId().toString() : null);
        resp.setGateKey(entity.getGateKey());
        resp.setTitle(entity.getTitle());
        resp.setDescription(entity.getDescription());
        resp.setStatus(entity.getStatus());
        resp.setRequestedBy(entity.getRequestedBy() != null ? entity.getRequestedBy().toString() : null);
        resp.setDecidedBy(entity.getDecidedBy() != null ? entity.getDecidedBy().toString() : null);
        resp.setDecisionComment(entity.getDecisionComment());
        resp.setRequestedAt(entity.getRequestedAt());
        resp.setDecidedAt(entity.getDecidedAt());
        return resp;
    }

    private void completeRun(MultiAgentRunEntity run, AiTaskEntity task,
                             List<MultiAgentPhaseEntity> phases,
                             List<MultiAgentStepEntity> allSteps,
                             List<String> phaseOutputSummaries,
                             StringBuilder finalSummaryBuilder,
                             boolean runFailed, String suffix) {
        Long taskId = task.getId();
        Long projectId = task.getProjectId();

        String finalSummary = finalSummaryBuilder.length() > 0
                ? finalSummaryBuilder.toString()
                : "## 多智能体协作总结\n\n本次 Mock 多智能体分阶段编排已完成。\n\n### 阶段摘要\n\n"
                        + String.join("\n", phaseOutputSummaries);

        AiTaskArtifactEntity artifact = new AiTaskArtifactEntity();
        artifact.setTaskId(taskId);
        artifact.setProjectId(projectId);
        artifact.setArtifactType(TaskArtifactType.REPORT.name());
        artifact.setName("Multi-Agent Mock Orchestration Summary");
        artifact.setContent(finalSummary);
        aiTaskArtifactMapper.insert(artifact);

        writeEvent(taskId, projectId, TaskStatus.RUNNING.name(),
                runFailed ? TaskStatus.FAILED.name() : TaskStatus.COMPLETED.name(),
                runFailed ? TaskEventType.FAILED.name() : TaskEventType.COMPLETED.name(),
                "Multi-Agent phased orchestration " + (runFailed ? "failed" : "completed") + suffix);

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MULTI_AGENT_PHASED_DONE",
                "多智能体分阶段编排完成" + suffix + "，" + phases.size() + " 个 Phase, "
                        + allSteps.size() + " 个步骤, "
                        + allSteps.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count() + " 完成, "
                        + allSteps.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count() + " 跳过");

        task.setStatus(runFailed ? TaskStatus.FAILED.name() : TaskStatus.COMPLETED.name());
        task.setEndTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        run.setStatus(runFailed ? MultiAgentRunStatus.FAILED.name() : MultiAgentRunStatus.COMPLETED.name());
        run.setFinalSummary(finalSummary);
        run.setFinishedAt(LocalDateTime.now());
        multiAgentRunMapper.updateById(run);
    }

    private MultiAgentRunResponse buildRunResponse(MultiAgentRunEntity run) {
        List<MultiAgentStepEntity> steps = multiAgentStepMapper.selectList(
                new LambdaQueryWrapper<MultiAgentStepEntity>()
                        .eq(MultiAgentStepEntity::getRunId, run.getId())
                        .orderByAsc(MultiAgentStepEntity::getStepOrder));
        List<MultiAgentMessageEntity> messages = multiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<MultiAgentMessageEntity>()
                        .eq(MultiAgentMessageEntity::getRunId, run.getId())
                        .orderByAsc(MultiAgentMessageEntity::getCreateTime));
        List<MultiAgentPhaseEntity> phases = multiAgentPhaseMapper.selectList(
                new LambdaQueryWrapper<MultiAgentPhaseEntity>()
                        .eq(MultiAgentPhaseEntity::getRunId, run.getId())
                        .orderByAsc(MultiAgentPhaseEntity::getPhaseOrder));
        return toRunResponse(run, steps, messages, phases);
    }

    // ========================
    // Message creation helper
    // ========================

    private void createMessage(Long runId, Long projectId, Long taskId,
                                Long fromStepId, Long toStepId,
                                Long fromAgentId, Long toAgentId,
                                MultiAgentMessageType messageType, String content, String summary) {
        MultiAgentMessageEntity msg = new MultiAgentMessageEntity();
        msg.setRunId(runId);
        msg.setProjectId(projectId);
        msg.setTaskId(taskId);
        msg.setFromStepId(fromStepId);
        msg.setToStepId(toStepId);
        msg.setFromAgentId(fromAgentId);
        msg.setToAgentId(toAgentId);
        msg.setMessageType(messageType.name());
        msg.setContent(content);
        msg.setSummary(summary);
        multiAgentMessageMapper.insert(msg);

        writeLog(taskId, projectId, TaskLogLevel.INFO.name(), "MESSAGE_CREATED",
                "消息 [" + messageType.name() + "] 已创建: " + summary);
    }

    // ========================
    // Helper methods
    // ========================

    private void writeLog(Long taskId, Long projectId, String level, String stage, String message) {
        AiTaskLogEntity log = new AiTaskLogEntity();
        log.setTaskId(taskId);
        log.setProjectId(projectId);
        log.setLevel(level);
        log.setStage(stage);
        log.setMessage(message);
        aiTaskLogMapper.insert(log);
    }

    private void writeEvent(Long taskId, Long projectId, String fromStatus, String toStatus,
                             String eventType, String reason) {
        AiTaskEventEntity event = new AiTaskEventEntity();
        event.setTaskId(taskId);
        event.setProjectId(projectId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType);
        event.setReason(reason);
        aiTaskEventMapper.insert(event);
    }

    // ========================
    // Response mapping
    // ========================

    private MultiAgentRunResponse toRunResponse(MultiAgentRunEntity run, List<MultiAgentStepEntity> steps,
                                                  List<MultiAgentMessageEntity> messages,
                                                  List<MultiAgentPhaseEntity> phases) {
        MultiAgentRunResponse resp = new MultiAgentRunResponse();
        resp.setId(run.getId().toString());
        resp.setProjectId(run.getProjectId().toString());
        resp.setTaskId(run.getTaskId().toString());
        resp.setStatus(run.getStatus());
        resp.setStrategy(run.getStrategy());
        resp.setStrategyKey(run.getStrategy());

        // Resolve strategy name/description from catalog
        WorkflowStrategyCatalogService.StrategyTemplate st =
                workflowStrategyCatalogService.resolveTemplate(run.getStrategy());
        if (st != null) {
            resp.setStrategyName(st.getName());
            resp.setStrategyDescription(st.getDescription());
        }
        resp.setTitle(run.getTitle());
        resp.setInputSummary(run.getInputSummary());
        resp.setFinalSummary(run.getFinalSummary());
        resp.setErrorMessage(run.getErrorMessage());
        resp.setStartedAt(run.getStartedAt() != null ? run.getStartedAt().toString() : null);
        resp.setFinishedAt(run.getFinishedAt() != null ? run.getFinishedAt().toString() : null);
        resp.setCreateTime(run.getCreateTime() != null ? run.getCreateTime().toString() : null);
        resp.setUpdateTime(run.getUpdateTime() != null ? run.getUpdateTime().toString() : null);

        // Phases with their steps
        Map<Long, List<MultiAgentStepEntity>> stepsByPhase = steps.stream()
                .filter(s -> s.getPhaseId() != null)
                .collect(Collectors.groupingBy(MultiAgentStepEntity::getPhaseId, LinkedHashMap::new, Collectors.toList()));

        List<MultiAgentPhaseResponse> phaseResponses = phases.stream().map(phase -> {
            List<MultiAgentStepEntity> phaseSteps = stepsByPhase.getOrDefault(phase.getId(), List.of());
            return toPhaseResponse(phase, phaseSteps);
        }).collect(Collectors.toList());
        resp.setPhases(phaseResponses);

        // All steps flat
        List<MultiAgentStepResponse> stepResponses = steps.stream().map(this::toStepResponse).collect(Collectors.toList());
        resp.setSteps(stepResponses);

        // Messages
        List<MultiAgentMessageResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse).collect(Collectors.toList());
        resp.setMessages(messageResponses);

        // Approval gates
        List<MultiAgentApprovalGateEntity> gateEntities = multiAgentApprovalGateMapper.selectList(
                new LambdaQueryWrapper<MultiAgentApprovalGateEntity>()
                        .eq(MultiAgentApprovalGateEntity::getRunId, run.getId()));
        resp.setApprovalGates(gateEntities.stream().map(this::toApprovalGateResponse).collect(Collectors.toList()));
        resp.setPendingApprovalGate(gateEntities.stream()
                .filter(g -> MultiAgentApprovalStatus.PENDING.name().equals(g.getStatus()))
                .findFirst()
                .map(this::toApprovalGateResponse)
                .orElse(null));

        return resp;
    }

    private MultiAgentPhaseResponse toPhaseResponse(MultiAgentPhaseEntity phase, List<MultiAgentStepEntity> steps) {
        MultiAgentPhaseResponse pr = new MultiAgentPhaseResponse();
        pr.setId(phase.getId().toString());
        pr.setRunId(phase.getRunId().toString());
        pr.setPhaseOrder(phase.getPhaseOrder());
        pr.setPhaseKey(phase.getPhaseKey());
        pr.setTitle(phase.getTitle());
        pr.setStatus(phase.getStatus());
        pr.setInputSummary(phase.getInputSummary());
        pr.setOutputSummary(phase.getOutputSummary());
        pr.setStartedAt(phase.getStartedAt() != null ? phase.getStartedAt().toString() : null);
        pr.setFinishedAt(phase.getFinishedAt() != null ? phase.getFinishedAt().toString() : null);
        pr.setSteps(steps.stream().map(this::toStepResponse).collect(Collectors.toList()));
        return pr;
    }

    private MultiAgentStepResponse toStepResponse(MultiAgentStepEntity step) {
        MultiAgentStepResponse sr = new MultiAgentStepResponse();
        sr.setId(step.getId().toString());
        sr.setRunId(step.getRunId().toString());
        sr.setPhaseId(step.getPhaseId() != null ? step.getPhaseId().toString() : null);
        sr.setPhaseOrder(step.getPhaseOrder());
        sr.setLaneKey(step.getLaneKey());
        sr.setStepOrder(step.getStepOrder());
        sr.setStepType(step.getStepType());
        sr.setStatus(step.getStatus());
        sr.setAgentId(step.getAgentId() != null ? step.getAgentId().toString() : null);
        sr.setAgentExecutionId(step.getAgentExecutionId() != null ? step.getAgentExecutionId().toString() : null);
        sr.setInputContext(step.getInputContext());
        sr.setOutputContent(step.getOutputContent());
        sr.setErrorMessage(step.getErrorMessage());
        sr.setStartedAt(step.getStartedAt() != null ? step.getStartedAt().toString() : null);
        sr.setFinishedAt(step.getFinishedAt() != null ? step.getFinishedAt().toString() : null);
        sr.setCreateTime(step.getCreateTime() != null ? step.getCreateTime().toString() : null);

        if (step.getAgentId() != null) {
            AiAgentEntity agent = aiAgentMapper.selectById(step.getAgentId());
            if (agent != null) {
                sr.setAgentName(agent.getName());
            }
        }
        return sr;
    }

    private MultiAgentMessageResponse toMessageResponse(MultiAgentMessageEntity msg) {
        MultiAgentMessageResponse mr = new MultiAgentMessageResponse();
        mr.setId(msg.getId().toString());
        mr.setRunId(msg.getRunId().toString());
        mr.setFromStepId(msg.getFromStepId() != null ? msg.getFromStepId().toString() : null);
        mr.setToStepId(msg.getToStepId() != null ? msg.getToStepId().toString() : null);
        mr.setFromAgentId(msg.getFromAgentId() != null ? msg.getFromAgentId().toString() : null);
        mr.setToAgentId(msg.getToAgentId() != null ? msg.getToAgentId().toString() : null);
        mr.setMessageType(msg.getMessageType());
        mr.setContent(msg.getContent());
        mr.setSummary(msg.getSummary());
        mr.setCreateTime(msg.getCreateTime() != null ? msg.getCreateTime().toString() : null);
        return mr;
    }
}
