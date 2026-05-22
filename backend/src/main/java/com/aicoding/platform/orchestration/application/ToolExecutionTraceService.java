package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.PatchProposalReviewEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionApprovalEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.dto.ToolExecutionApprovalEvidenceResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionArtifactEvidenceResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionEvidenceResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionFileEvidenceResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobEvidenceResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionTraceEventResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionTraceResponse;
import com.aicoding.platform.orchestration.infrastructure.PatchProposalReviewMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionApprovalMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolExecutionTraceService {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionTraceService.class);

    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final ToolExecutionApprovalMapper toolExecutionApprovalMapper;
    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final PatchProposalReviewMapper patchProposalReviewMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ToolTracePayloadSanitizer payloadSanitizer;
    private final ObjectMapper objectMapper;

    public ToolExecutionTraceService(ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                      ToolExecutionApprovalMapper toolExecutionApprovalMapper,
                                      ToolExecutionJobMapper toolExecutionJobMapper,
                                      AiTaskArtifactMapper aiTaskArtifactMapper,
                                      PatchProposalReviewMapper patchProposalReviewMapper,
                                      ProjectPermissionService projectPermissionService,
                                      ToolTracePayloadSanitizer payloadSanitizer) {
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.toolExecutionApprovalMapper = toolExecutionApprovalMapper;
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.patchProposalReviewMapper = patchProposalReviewMapper;
        this.projectPermissionService = projectPermissionService;
        this.payloadSanitizer = payloadSanitizer;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(readOnly = true)
    public ToolExecutionTraceResponse getTrace(Long executionId) {
        ToolSandboxExecutionEntity execution = toolSandboxExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具沙箱执行记录不存在");
        }
        projectPermissionService.checkProjectRole(execution.getProjectId(),
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        return buildTrace(execution);
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionTraceResponse> listRunTraces(Long runId) {
        List<ToolSandboxExecutionEntity> executions = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getRunId, runId));

        if (executions.isEmpty()) return Collections.emptyList();

        projectPermissionService.checkProjectRole(executions.get(0).getProjectId(),
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        return executions.stream().map(this::buildTrace).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionTraceResponse> listTaskTraces(Long taskId) {
        List<ToolSandboxExecutionEntity> executions = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getTaskId, taskId)
                        .orderByAsc(ToolSandboxExecutionEntity::getCreateTime));

        if (executions.isEmpty()) return Collections.emptyList();

        projectPermissionService.checkProjectRole(executions.get(0).getProjectId(),
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        return executions.stream().map(this::buildTrace).collect(Collectors.toList());
    }

    private ToolExecutionTraceResponse buildTrace(ToolSandboxExecutionEntity execution) {
        ToolExecutionTraceResponse trace = new ToolExecutionTraceResponse();
        Long execId = execution.getId();

        trace.setExecutionId(execId.toString());
        trace.setProjectId(execution.getProjectId().toString());
        trace.setTaskId(execution.getTaskId() != null ? execution.getTaskId().toString() : null);
        trace.setRunId(execution.getRunId() != null ? execution.getRunId().toString() : null);
        trace.setStepId(execution.getStepId() != null ? execution.getStepId().toString() : null);
        trace.setToolKey(execution.getToolName());
        trace.setToolName(execution.getToolName());
        trace.setMode(execution.getExecutionMode());
        trace.setStatus(execution.getStatus());
        trace.setCreateTime(execution.getCreateTime());
        trace.setUpdateTime(execution.getUpdateTime());

        // Policy inference
        String status = execution.getStatus();
        trace.setPolicyAllowed(!"BLOCKED".equals(status) && !"REJECTED".equals(status));
        if ("BLOCKED".equals(status)) {
            trace.setPolicyReason(execution.getErrorMessage());
        }

        // Read-only from outputPayload
        trace.setReadOnly(parseReadOnly(execution.getOutputPayload()));

        // Sanitize payloads
        trace.setInputPayload(payloadSanitizer.sanitize(execution.getInputPayload()));
        String sanitizedOutput = payloadSanitizer.sanitize(execution.getOutputPayload());
        trace.setOutputPayload(sanitizedOutput);

        // Risk level from approval entity or infer
        trace.setRiskLevel(resolveRiskLevel(execId, execution.getToolName()));

        // Approval evidence
        trace.setApproval(buildApprovalEvidence(execId));

        // Job evidence
        trace.setJob(buildJobEvidence(execId));

        // Evidence (from outputPayload)
        trace.setEvidence(buildEvidence(execution));

        // Timeline events
        trace.setEvents(buildEvents(execution));

        return trace;
    }

    private ToolExecutionApprovalEvidenceResponse buildApprovalEvidence(Long executionId) {
        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, executionId));

        if (approval == null) return null;

        ToolExecutionApprovalEvidenceResponse resp = new ToolExecutionApprovalEvidenceResponse();
        resp.setApprovalId(approval.getId().toString());
        resp.setStatus(approval.getStatus());
        resp.setApproverId(approval.getDecidedBy() != null ? approval.getDecidedBy().toString() : null);
        resp.setComment(approval.getDecisionComment());
        resp.setCreateTime(approval.getCreateTime());
        resp.setDecidedAt(approval.getDecidedAt());
        return resp;
    }

    private ToolExecutionJobEvidenceResponse buildJobEvidence(Long executionId) {
        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getToolExecutionId, executionId)
                        .orderByDesc(ToolExecutionJobEntity::getCreateTime));

        if (jobs.isEmpty()) return null;

        ToolExecutionJobEntity job = jobs.get(0);

        ToolExecutionJobEvidenceResponse resp = new ToolExecutionJobEvidenceResponse();
        resp.setJobId(job.getId().toString());
        resp.setStatus(job.getStatus());
        resp.setPriority(job.getPriority());
        resp.setAttemptCount(job.getRetryCount() != null ? job.getRetryCount() + 1 : 1);
        resp.setErrorCode(job.getErrorCode());
        resp.setFailureStage(job.getFailureStage());
        resp.setNextRetryAt(job.getNextRetryAt());
        resp.setDeadLetteredAt(job.getDeadLetteredAt());
        resp.setDeadLetterReason(job.getDeadLetterReason());
        resp.setSourceJobId(job.getSourceJobId() != null ? job.getSourceJobId().toString() : null);
        resp.setCreateTime(job.getCreateTime());
        resp.setStartedAt(job.getStartedAt());
        resp.setFinishedAt(job.getFinishedAt());
        return resp;
    }

    private ToolExecutionEvidenceResponse buildEvidence(ToolSandboxExecutionEntity execution) {
        ToolExecutionEvidenceResponse evidence = new ToolExecutionEvidenceResponse();
        String outputPayload = execution.getOutputPayload();

        if (outputPayload == null || outputPayload.isBlank()) {
            evidence.setFilesReadCount(0);
            evidence.setSkippedFilesCount(0);
            evidence.setRedacted(false);
            evidence.setTruncated(false);
            evidence.setFilesRead(Collections.emptyList());
            evidence.setSkippedFiles(Collections.emptyList());
            return evidence;
        }

        try {
            JsonNode root = objectMapper.readTree(outputPayload);

            // Read-only tools evidence
            List<ToolExecutionFileEvidenceResponse> filesRead = new ArrayList<>();
            List<ToolExecutionFileEvidenceResponse> skippedFiles = new ArrayList<>();

            if (root.has("filesRead") && root.get("filesRead").isArray()) {
                for (JsonNode item : root.get("filesRead")) {
                    ToolExecutionFileEvidenceResponse f = new ToolExecutionFileEvidenceResponse();
                    f.setPath(item.has("filePath") ? item.get("filePath").asText() : item.asText());
                    f.setLineStart(item.has("startLine") ? item.get("startLine").asInt() : null);
                    f.setLineEnd(item.has("endLine") ? item.get("endLine").asInt() : null);
                    filesRead.add(f);
                }
            }

            if (root.has("skippedFiles") && root.get("skippedFiles").isArray()) {
                for (JsonNode item : root.get("skippedFiles")) {
                    String filePath = item.has("filePath") ? item.get("filePath").asText() : item.asText();
                    String reason = item.has("reason") ? item.get("reason").asText() : null;
                    ToolExecutionFileEvidenceResponse f = new ToolExecutionFileEvidenceResponse(filePath, reason);
                    skippedFiles.add(f);
                }
            }

            evidence.setFilesRead(filesRead);
            evidence.setSkippedFiles(skippedFiles);
            evidence.setFilesReadCount(filesRead.size());
            evidence.setSkippedFilesCount(skippedFiles.size());
            evidence.setRedacted(root.has("redacted") && root.get("redacted").asBoolean());
            evidence.setTruncated(root.has("truncated") && root.get("truncated").asBoolean());

            // Binary/denylist/path sandbox flags
            evidence.setBinarySkipped(root.has("binarySkipped") && root.get("binarySkipped").asBoolean());
            evidence.setSensitiveDenylistApplied(root.has("sensitiveDenylistApplied") && root.get("sensitiveDenylistApplied").asBoolean());

            // Non-empty filesTouched/gitOperations → not pure readOnly
            if (root.has("filesTouched") && root.get("filesTouched").isArray() && root.get("filesTouched").size() > 0) {
                evidence.setPathSandboxApplied(true);
            }
            if (root.has("gitOperations") && root.get("gitOperations").isArray() && root.get("gitOperations").size() > 0) {
                evidence.setPathSandboxApplied(true);
            }

            // Artifact evidence for PATCH_PROPOSAL
            if (execution.getArtifactId() != null) {
                evidence.setArtifacts(buildArtifactEvidence(execution));
            }

        } catch (JsonProcessingException e) {
            log.debug("Failed to parse outputPayload for executionId={}: {}", execution.getId(), e.getMessage());
            evidence.setFilesReadCount(0);
            evidence.setSkippedFilesCount(0);
            evidence.setRedacted(false);
            evidence.setTruncated(false);
            evidence.setFilesRead(Collections.emptyList());
            evidence.setSkippedFiles(Collections.emptyList());
        }

        return evidence;
    }

    private List<ToolExecutionArtifactEvidenceResponse> buildArtifactEvidence(ToolSandboxExecutionEntity execution) {
        if (execution.getArtifactId() == null) return null;

        AiTaskArtifactEntity artifact = aiTaskArtifactMapper.selectById(execution.getArtifactId());
        if (artifact == null) return Collections.emptyList();

        ToolExecutionArtifactEvidenceResponse artResp = new ToolExecutionArtifactEvidenceResponse();
        artResp.setArtifactId(artifact.getId().toString());
        artResp.setArtifactType(artifact.getArtifactType());
        artResp.setTitle(artifact.getName());
        artResp.setCreateTime(artifact.getCreateTime());

        // Look up patch review by artifactId or toolExecutionId
        PatchProposalReviewEntity review = patchProposalReviewMapper.selectOne(
                new LambdaQueryWrapper<PatchProposalReviewEntity>()
                        .eq(PatchProposalReviewEntity::getArtifactId, artifact.getId())
                        .last("LIMIT 1"));
        if (review == null) {
            review = patchProposalReviewMapper.selectOne(
                    new LambdaQueryWrapper<PatchProposalReviewEntity>()
                            .eq(PatchProposalReviewEntity::getToolExecutionId, execution.getId())
                            .last("LIMIT 1"));
        }
        if (review != null) {
            artResp.setPatchReviewStatus(review.getStatus());
            artResp.setPatchReviewDecision(review.getDecision());
        }

        return List.of(artResp);
    }

    private List<ToolExecutionTraceEventResponse> buildEvents(ToolSandboxExecutionEntity execution) {
        List<ToolExecutionTraceEventResponse> events = new ArrayList<>();

        // EXECUTION_CREATED
        events.add(new ToolExecutionTraceEventResponse(
                "EXECUTION_CREATED", "工具执行已创建", null,
                execution.getStatus(), execution.getCreateTime()));

        // POLICY_CHECKED
        String status = execution.getStatus();
        if ("BLOCKED".equals(status)) {
            events.add(new ToolExecutionTraceEventResponse(
                    "POLICY_CHECKED", "策略检查未通过",
                    execution.getErrorMessage(), "DENIED", execution.getCreateTime()));
        } else {
            events.add(new ToolExecutionTraceEventResponse(
                    "POLICY_CHECKED", "策略检查通过", null,
                    "ALLOWED", execution.getCreateTime()));
        }

        // APPROVAL events
        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, execution.getId()));
        if (approval != null) {
            events.add(new ToolExecutionTraceEventResponse(
                    "APPROVAL_CREATED", "审批已创建",
                    "风险等级: " + approval.getRiskLevel(), approval.getStatus(),
                    approval.getCreateTime()));

            if ("APPROVED".equals(approval.getStatus()) && approval.getDecidedAt() != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "APPROVAL_ACCEPTED", "审批已通过",
                        approval.getDecisionComment(), "APPROVED", approval.getDecidedAt()));
            } else if ("REJECTED".equals(approval.getStatus()) && approval.getDecidedAt() != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "APPROVAL_REJECTED", "审批已驳回",
                        approval.getDecisionComment(), "REJECTED", approval.getDecidedAt()));
            }
        }

        // JOB events
        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getToolExecutionId, execution.getId())
                        .orderByAsc(ToolExecutionJobEntity::getCreateTime));

        for (ToolExecutionJobEntity job : jobs) {
            String jobStatus = job.getStatus();

            if ("PENDING".equals(jobStatus) || "RUNNING".equals(jobStatus)) {
                events.add(new ToolExecutionTraceEventResponse(
                        "JOB_CREATED", "Worker 任务已创建",
                        "优先级: " + job.getPriority(), jobStatus, job.getCreateTime()));
            }

            if ("RUNNING".equals(jobStatus) && job.getStartedAt() != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "JOB_RUNNING", "Worker 执行中", null, "RUNNING", job.getStartedAt()));
            }

            if ("RETRY_PENDING".equals(jobStatus)) {
                Map<String, Object> meta = new HashMap<>();
                if (job.getNextRetryAt() != null) {
                    meta.put("nextRetryAt", job.getNextRetryAt().toString());
                }
                if (job.getErrorCode() != null) {
                    meta.put("errorCode", job.getErrorCode());
                }
                ToolExecutionTraceEventResponse retryEvent = new ToolExecutionTraceEventResponse(
                        "JOB_RETRY_PENDING", "任务待重试",
                        "错误: " + (job.getLastError() != null ? job.getLastError() : job.getErrorCode()),
                        "RETRY_PENDING", job.getUpdateTime());
                retryEvent.setMetadata(meta);
                events.add(retryEvent);
            }

            if ("COMPLETED".equals(jobStatus) && job.getFinishedAt() != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "JOB_COMPLETED", "Worker 执行完成",
                        "耗时: " + (job.getDurationMs() != null ? job.getDurationMs() + "ms" : "N/A"),
                        "COMPLETED", job.getFinishedAt()));
            }

            if ("FAILED".equals(jobStatus) && job.getFinishedAt() != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "JOB_FAILED", "Worker 执行失败",
                        job.getLastError(), "FAILED", job.getFinishedAt()));
            }

            if ("DEAD_LETTERED".equals(jobStatus)) {
                events.add(new ToolExecutionTraceEventResponse(
                        "JOB_DEAD_LETTERED", "任务已进入死信队列",
                        "原因: " + job.getDeadLetterReason(),
                        "DEAD_LETTERED", job.getDeadLetteredAt() != null ? job.getDeadLetteredAt() : job.getUpdateTime()));
            }
        }

        // OUTPUT_CAPTURED
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            events.add(new ToolExecutionTraceEventResponse(
                    "OUTPUT_CAPTURED", "输出已记录", null,
                    status, execution.getFinishedAt() != null ? execution.getFinishedAt() : execution.getUpdateTime()));
        }

        // READ_ONLY_CONTRACT_WARNING
        String output = execution.getOutputPayload();
        if (output != null && !output.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(output);
                boolean hasFilesTouched = root.has("filesTouched") && root.get("filesTouched").isArray()
                        && root.get("filesTouched").size() > 0;
                boolean hasGitOps = root.has("gitOperations") && root.get("gitOperations").isArray()
                        && root.get("gitOperations").size() > 0;
                if (hasFilesTouched || hasGitOps) {
                    events.add(new ToolExecutionTraceEventResponse(
                            "READ_ONLY_CONTRACT_WARNING", "只读契约警告",
                            "工具标记为只读但 outputPayload 包含 filesTouched 或 gitOperations 非空",
                            "WARNING", execution.getUpdateTime()));
                }
            } catch (JsonProcessingException e) {
                events.add(new ToolExecutionTraceEventResponse(
                        "OUTPUT_PARSE_WARNING", "输出解析警告",
                        "outputPayload JSON 解析失败", "WARNING", execution.getCreateTime()));
            }
        }

        // Artifact events
        if (execution.getArtifactId() != null) {
            events.add(new ToolExecutionTraceEventResponse(
                    "ARTIFACT_CREATED", "产物已生成",
                    "artifactType=PATCH_PROPOSAL", null, execution.getUpdateTime()));

            PatchProposalReviewEntity review = patchProposalReviewMapper.selectOne(
                    new LambdaQueryWrapper<PatchProposalReviewEntity>()
                            .eq(PatchProposalReviewEntity::getToolExecutionId, execution.getId())
                            .last("LIMIT 1"));
            if (review != null) {
                events.add(new ToolExecutionTraceEventResponse(
                        "PATCH_REVIEW_CREATED", "补丁审查已创建",
                        null, review.getStatus(), review.getCreateTime()));
                if (review.getReviewedAt() != null) {
                    events.add(new ToolExecutionTraceEventResponse(
                            "PATCH_REVIEW_DECIDED", "补丁审查已决定",
                            "决策: " + review.getDecision() + ", 备注: " + (review.getReviewComment() != null ? review.getReviewComment() : ""),
                            review.getDecision() != null ? review.getDecision() : review.getStatus(),
                            review.getReviewedAt()));
                }
            }
        }

        return events;
    }

    private Boolean parseReadOnly(String outputPayload) {
        if (outputPayload == null || outputPayload.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(outputPayload);
            if (root.has("readOnly")) return root.get("readOnly").asBoolean();
        } catch (JsonProcessingException e) {
            // ignore
        }
        return null;
    }

    private String resolveRiskLevel(Long executionId, String toolName) {
        ToolExecutionApprovalEntity approval = toolExecutionApprovalMapper.selectOne(
                new LambdaQueryWrapper<ToolExecutionApprovalEntity>()
                        .eq(ToolExecutionApprovalEntity::getToolExecutionId, executionId));
        if (approval != null && approval.getRiskLevel() != null) {
            return approval.getRiskLevel();
        }
        // Infer from tool name
        if (toolName != null && (toolName.startsWith("READ_") || toolName.startsWith("SEARCH_"))) {
            return "LOW";
        }
        return "MEDIUM";
    }
}
