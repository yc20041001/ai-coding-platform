package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReleaseRolloutPlanService {

    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseRolloutStepService releaseRolloutStepService;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final BetaReleaseDecisionMapper betaReleaseDecisionMapper;
    private final ProjectPermissionService projectPermissionService;

    public ReleaseRolloutPlanService(ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                     ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                     ReleaseRolloutStepService releaseRolloutStepService,
                                     ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                     BetaReleaseDecisionMapper betaReleaseDecisionMapper,
                                     ProjectPermissionService projectPermissionService) {
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseRolloutStepService = releaseRolloutStepService;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.betaReleaseDecisionMapper = betaReleaseDecisionMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ReleaseRolloutPlanResponse createPlan(CreateReleaseRolloutPlanRequest request) {
        if (request.getReleaseLabel() == null || request.getReleaseLabel().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "发布标签不能为空");
        }
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        LambdaQueryWrapper<ReleaseRolloutPlanEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(ReleaseRolloutPlanEntity::getProjectId, projectId)
                .eq(ReleaseRolloutPlanEntity::getReleaseLabel, request.getReleaseLabel());
        if (releaseRolloutPlanMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该发布标签已存在 rollout plan");
        }

        ReleaseRolloutPlanEntity entity = new ReleaseRolloutPlanEntity();
        entity.setProjectId(projectId);
        entity.setReleaseLabel(request.getReleaseLabel());
        entity.setSourceDecisionId(request.getSourceDecisionId() != null ? parseLong(request.getSourceDecisionId(), "sourceDecisionId") : null);
        entity.setRolloutStatus(ReleaseRolloutStatus.DRAFT.name());
        entity.setRolloutStrategy(request.getRolloutStrategy() != null ? request.getRolloutStrategy() : ReleaseRolloutStrategy.MANUAL_FULL.name());
        entity.setTargetEnvironment(request.getTargetEnvironment() != null ? request.getTargetEnvironment() : "production");
        entity.setOwnerId(request.getOwnerId() != null ? parseLong(request.getOwnerId(), "ownerId") : null);
        entity.setApproverId(request.getApproverId() != null ? parseLong(request.getApproverId(), "approverId") : null);
        entity.setPlannedStartAt(request.getPlannedStartAt());
        entity.setPlannedEndAt(request.getPlannedEndAt());
        entity.setObservationWindowMinutes(Objects.requireNonNullElse(request.getObservationWindowMinutes(), 60));
        entity.setRollbackTriggerSummary(request.getRollbackTriggerSummary());
        entity.setSuccessCriteriaSummary(request.getSuccessCriteriaSummary());
        entity.setReadinessSummary(request.getReadinessSummary());

        releaseRolloutPlanMapper.insert(entity);

        releaseRolloutStepService.initDefaultSteps(entity.getId(), projectId, request.getReleaseLabel());

        return toPlanResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReleaseRolloutPlanResponse> listPlans(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<ReleaseRolloutPlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRolloutPlanEntity::getProjectId, projectId);
        wrapper.orderByDesc(ReleaseRolloutPlanEntity::getCreateTime);

        return releaseRolloutPlanMapper.selectList(wrapper).stream()
                .map(this::toPlanResponseWithCounts)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseRolloutPlanResponse getPlan(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity entity = releaseRolloutPlanMapper.selectById(planId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toPlanResponseWithCounts(entity);
    }

    @Transactional
    public ReleaseRolloutPlanResponse updatePlan(String planIdStr, UpdateReleaseRolloutPlanRequest request) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity entity = releaseRolloutPlanMapper.selectById(planId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getRolloutStrategy() != null) entity.setRolloutStrategy(request.getRolloutStrategy());
        if (request.getTargetEnvironment() != null) entity.setTargetEnvironment(request.getTargetEnvironment());
        if (request.getPlannedStartAt() != null) entity.setPlannedStartAt(request.getPlannedStartAt());
        if (request.getPlannedEndAt() != null) entity.setPlannedEndAt(request.getPlannedEndAt());
        if (request.getObservationWindowMinutes() != null) entity.setObservationWindowMinutes(request.getObservationWindowMinutes());
        if (request.getRollbackTriggerSummary() != null) entity.setRollbackTriggerSummary(request.getRollbackTriggerSummary());
        if (request.getSuccessCriteriaSummary() != null) entity.setSuccessCriteriaSummary(request.getSuccessCriteriaSummary());
        if (request.getReadinessSummary() != null) entity.setReadinessSummary(request.getReadinessSummary());

        releaseRolloutPlanMapper.updateById(entity);
        return toPlanResponseWithCounts(entity);
    }

    @Transactional
    public ReleaseRolloutPlanResponse updatePlanStatus(String planIdStr, String newStatus) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity entity = releaseRolloutPlanMapper.selectById(planId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        String currentStatus = entity.getRolloutStatus();
        validateStatusTransition(currentStatus, newStatus);

        if ("IN_PROGRESS".equals(newStatus)) {
            validatePrerequisites(entity.getProjectId());
        }

        entity.setRolloutStatus(newStatus);
        releaseRolloutPlanMapper.updateById(entity);
        return toPlanResponseWithCounts(entity);
    }

    private void validateStatusTransition(String current, String target) {
        switch (current) {
            case "DRAFT" -> {
                if (!"READY".equals(target) && !"CANCELLED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "DRAFT 状态只能转为 READY 或 CANCELLED");
            }
            case "READY" -> {
                if (!"IN_PROGRESS".equals(target) && !"CANCELLED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "READY 状态只能转为 IN_PROGRESS 或 CANCELLED");
            }
            case "IN_PROGRESS" -> {
                if (!"OBSERVING".equals(target) && !"ROLLED_BACK".equals(target) && !"CANCELLED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "IN_PROGRESS 状态只能转为 OBSERVING、ROLLED_BACK 或 CANCELLED");
            }
            case "OBSERVING" -> {
                if (!"COMPLETED".equals(target) && !"ROLLED_BACK".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "OBSERVING 状态只能转为 COMPLETED 或 ROLLED_BACK");
            }
            case "COMPLETED" -> throw new BizException(ErrorCode.BAD_REQUEST, "COMPLETED 状态不可变更");
            case "ROLLED_BACK" -> throw new BizException(ErrorCode.BAD_REQUEST, "ROLLED_BACK 状态不可变更");
            case "CANCELLED" -> throw new BizException(ErrorCode.BAD_REQUEST, "CANCELLED 状态不可变更");
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知状态: " + current);
        }
    }

    private void validatePrerequisites(Long projectId) {
        LambdaQueryWrapper<BetaReleaseDecisionEntity> decWrapper = new LambdaQueryWrapper<>();
        decWrapper.eq(BetaReleaseDecisionEntity::getProjectId, projectId);
        decWrapper.orderByDesc(BetaReleaseDecisionEntity::getCreateTime);
        decWrapper.last("LIMIT 1");
        BetaReleaseDecisionEntity latestDecision = betaReleaseDecisionMapper.selectOne(decWrapper);

        if (latestDecision != null && "NO_GO".equals(latestDecision.getDecisionStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最新决策为 NO_GO，无法开始 rollout");
        }
    }

    private ReleaseRolloutPlanResponse toPlanResponse(ReleaseRolloutPlanEntity entity) {
        ReleaseRolloutPlanResponse resp = new ReleaseRolloutPlanResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setSourceDecisionId(entity.getSourceDecisionId() != null ? entity.getSourceDecisionId().toString() : null);
        resp.setRolloutStatus(entity.getRolloutStatus());
        resp.setRolloutStrategy(entity.getRolloutStrategy());
        resp.setTargetEnvironment(entity.getTargetEnvironment());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setApproverId(entity.getApproverId() != null ? entity.getApproverId().toString() : null);
        resp.setPlannedStartAt(entity.getPlannedStartAt());
        resp.setPlannedEndAt(entity.getPlannedEndAt());
        resp.setObservationWindowMinutes(entity.getObservationWindowMinutes());
        resp.setRollbackTriggerSummary(entity.getRollbackTriggerSummary());
        resp.setSuccessCriteriaSummary(entity.getSuccessCriteriaSummary());
        resp.setReadinessSummary(entity.getReadinessSummary());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private ReleaseRolloutPlanResponse toPlanResponseWithCounts(ReleaseRolloutPlanEntity entity) {
        ReleaseRolloutPlanResponse resp = toPlanResponse(entity);
        Long planId = entity.getId();

        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);
        resp.setStepCount(steps.size());
        resp.setPassedStepCount((int) steps.stream().filter(s -> "PASSED".equals(s.getStepStatus())).count());
        resp.setFailedStepCount((int) steps.stream().filter(s -> "FAILED".equals(s.getStepStatus())).count());

        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);
        resp.setVerificationCount(verifications.size());
        resp.setBlockingVerificationCount((int) verifications.stream()
                .filter(v -> "BLOCKING".equals(v.getSeverity()) || "CRITICAL".equals(v.getSeverity()))
                .count());

        return resp;
    }

    static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
