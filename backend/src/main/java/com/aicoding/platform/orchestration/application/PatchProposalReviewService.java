package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.PatchProposalDecision;
import com.aicoding.platform.orchestration.domain.PatchProposalReviewEntity;
import com.aicoding.platform.orchestration.domain.PatchProposalReviewStatus;
import com.aicoding.platform.orchestration.dto.PatchProposalReviewDecisionRequest;
import com.aicoding.platform.orchestration.dto.PatchProposalReviewResponse;
import com.aicoding.platform.orchestration.infrastructure.PatchProposalReviewMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskArtifactType;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
//import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PatchProposalReviewService {

    private final PatchProposalReviewMapper patchProposalReviewMapper;
    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> VALID_DECISIONS = Arrays.stream(PatchProposalDecision.values())
            .map(Enum::name).collect(Collectors.toSet());

    private static final String DEFAULT_CHECKLIST_JSON = "{\"matchesRequirement\":false,\"noSensitiveData\":false,\"noFileWritten\":true,\"noGitOperation\":true,\"readyForManualImplementation\":false}";

    public PatchProposalReviewService(PatchProposalReviewMapper patchProposalReviewMapper,
                                       AiTaskArtifactMapper aiTaskArtifactMapper,
                                       AiTaskLogMapper aiTaskLogMapper,
                                       ProjectPermissionService projectPermissionService) {
        this.patchProposalReviewMapper = patchProposalReviewMapper;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.projectPermissionService = projectPermissionService;
    }

    /**
     * Ensure a PENDING review exists for the artifact, creating one if needed.
     */
    @Transactional
    public PatchProposalReviewResponse ensureReviewForArtifact(Long artifactId) {
        AiTaskArtifactEntity artifact = aiTaskArtifactMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "产物不存在");
        }
        if (!TaskArtifactType.PATCH_PROPOSAL.name().equals(artifact.getArtifactType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅 PATCH_PROPOSAL 类型产物支持审阅");
        }

        // Check project access (VIEWER+)
        projectPermissionService.checkProjectMember(artifact.getProjectId());

        // Check if review already exists
        PatchProposalReviewEntity existing = patchProposalReviewMapper.selectOne(
                new LambdaQueryWrapper<PatchProposalReviewEntity>()
                        .eq(PatchProposalReviewEntity::getArtifactId, artifactId));
        if (existing != null) {
            return toResponse(existing);
        }

        // Create PENDING review
        PatchProposalReviewEntity review = new PatchProposalReviewEntity();
        review.setProjectId(artifact.getProjectId());
        review.setTaskId(artifact.getTaskId());
        review.setArtifactId(artifactId);
        review.setStatus(PatchProposalReviewStatus.PENDING.name());
        review.setSafetyConfirmed(false);
        review.setChecklistJson(DEFAULT_CHECKLIST_JSON);
        patchProposalReviewMapper.insert(review);

        return toResponse(review);
    }

    /**
     * Submit a decision for the review (idempotent — allows overwrite).
     */
    @Transactional
    public PatchProposalReviewResponse decide(Long artifactId, PatchProposalReviewDecisionRequest request) {
        AiTaskArtifactEntity artifact = aiTaskArtifactMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "产物不存在");
        }
        if (!TaskArtifactType.PATCH_PROPOSAL.name().equals(artifact.getArtifactType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅 PATCH_PROPOSAL 类型产物支持审阅决策");
        }

        // Check MAINTAINER+ role
        projectPermissionService.checkProjectRole(artifact.getProjectId(), ProjectRole.MAINTAINER, ProjectRole.OWNER);

        // Validate decision
        if (request.getDecision() == null || !VALID_DECISIONS.contains(request.getDecision())) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "无效的决策值，有效值: " + String.join(", ", VALID_DECISIONS));
        }

        // safetyConfirmed must be true
        if (request.getSafetyConfirmed() == null || !request.getSafetyConfirmed()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "必须确认安全提示 (safetyConfirmed=true)");
        }

        Long currentUserId = LoginUserContext.currentUserId();

        // Find or create review
        PatchProposalReviewEntity review = patchProposalReviewMapper.selectOne(
                new LambdaQueryWrapper<PatchProposalReviewEntity>()
                        .eq(PatchProposalReviewEntity::getArtifactId, artifactId));
        if (review == null) {
            review = new PatchProposalReviewEntity();
            review.setProjectId(artifact.getProjectId());
            review.setTaskId(artifact.getTaskId());
            review.setArtifactId(artifactId);
            review.setStatus(PatchProposalReviewStatus.PENDING.name());
            review.setSafetyConfirmed(false);
            review.setChecklistJson(DEFAULT_CHECKLIST_JSON);
        }

        boolean isNew = review.getId() == null;
        boolean wasReviewed = PatchProposalReviewStatus.REVIEWED.name().equals(review.getStatus());

        review.setStatus(PatchProposalReviewStatus.REVIEWED.name());
        review.setDecision(request.getDecision());
        review.setReviewerId(currentUserId);
        review.setReviewComment(request.getComment());
        review.setReviewedAt(LocalDateTime.now());
        review.setSafetyConfirmed(true);
        if (request.getChecklist() != null) {
            try {
                review.setChecklistJson(objectMapper.writeValueAsString(request.getChecklist()));
            } catch (JsonProcessingException e) {
                review.setChecklistJson(request.getChecklist().toString());
            }
        }

        if (isNew) {
            patchProposalReviewMapper.insert(review);
        } else {
            patchProposalReviewMapper.updateById(review);
        }

        // Write task log
        String stage = wasReviewed ? "PATCH_PROPOSAL_REVIEW_UPDATED" : "PATCH_PROPOSAL_REVIEWED";
        String message = "Patch Proposal 已审阅，决策: " + request.getDecision();
        writeTaskLog(artifact.getTaskId(), artifact.getProjectId(), stage, message);

        return toResponse(review);
    }

    /**
     * List all patch reviews for a task.
     */
    public List<PatchProposalReviewResponse> listTaskReviews(Long taskId) {
        // Check project access via the task's project
        // (simplified: caller controller checks permission before calling)
        List<PatchProposalReviewEntity> reviews = patchProposalReviewMapper.selectList(
                new LambdaQueryWrapper<PatchProposalReviewEntity>()
                        .eq(PatchProposalReviewEntity::getTaskId, taskId)
                        .orderByDesc(PatchProposalReviewEntity::getUpdateTime));
        return reviews.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void writeTaskLog(Long taskId, Long projectId, String stage, String message) {
        if (taskId == null) return;
        AiTaskLogEntity taskLog = new AiTaskLogEntity();
        taskLog.setTaskId(taskId);
        taskLog.setProjectId(projectId);
        taskLog.setLevel(TaskLogLevel.INFO.name());
        taskLog.setStage(stage);
        taskLog.setMessage(message);
        aiTaskLogMapper.insert(taskLog);
    }

    public PatchProposalReviewResponse toResponse(PatchProposalReviewEntity entity) {
        PatchProposalReviewResponse resp = new PatchProposalReviewResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setArtifactId(entity.getArtifactId() != null ? entity.getArtifactId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId() != null ? entity.getToolExecutionId().toString() : null);
        resp.setStatus(entity.getStatus());
        resp.setDecision(entity.getDecision());
        resp.setReviewerId(entity.getReviewerId() != null ? entity.getReviewerId().toString() : null);
        resp.setReviewComment(entity.getReviewComment());
        resp.setReviewedAt(entity.getReviewedAt() != null ? entity.getReviewedAt().toString() : null);
        resp.setSafetyConfirmed(entity.getSafetyConfirmed());
        resp.setChecklistJson(entity.getChecklistJson());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return resp;
    }
}
