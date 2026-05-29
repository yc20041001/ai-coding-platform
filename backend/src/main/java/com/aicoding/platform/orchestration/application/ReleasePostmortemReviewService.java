package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ReleaseAuditEventType;
import com.aicoding.platform.orchestration.domain.ReleasePostmortemReviewEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutStepEntity;
import com.aicoding.platform.orchestration.domain.ReleaseVerificationRecordEntity;
import com.aicoding.platform.orchestration.dto.CreateReleasePostmortemReviewRequest;
import com.aicoding.platform.orchestration.dto.ReleasePostmortemReviewResponse;
import com.aicoding.platform.orchestration.dto.UpdateReleasePostmortemReviewRequest;
import com.aicoding.platform.orchestration.infrastructure.ReleasePostmortemReviewMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutStepMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseVerificationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReleasePostmortemReviewService {

    private final ReleasePostmortemReviewMapper releasePostmortemReviewMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final ReleaseAuditTrailService releaseAuditTrailService;
    private final ProjectPermissionService projectPermissionService;

    public ReleasePostmortemReviewService(ReleasePostmortemReviewMapper releasePostmortemReviewMapper,
                                          ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                          ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                          ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                          ReleaseAuditTrailService releaseAuditTrailService,
                                          ProjectPermissionService projectPermissionService) {
        this.releasePostmortemReviewMapper = releasePostmortemReviewMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.releaseAuditTrailService = releaseAuditTrailService;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ReleasePostmortemReviewResponse createReview(CreateReleasePostmortemReviewRequest request) {
        Long planId = parseLong(request.getPlanId(), "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Check if review already exists
        LambdaQueryWrapper<ReleasePostmortemReviewEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(ReleasePostmortemReviewEntity::getPlanId, planId);
        if (releasePostmortemReviewMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该 plan 已存在 postmortem review");
        }

        ReleasePostmortemReviewEntity entity = new ReleasePostmortemReviewEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setReleaseLabel(request.getReleaseLabel() != null ? request.getReleaseLabel() : plan.getReleaseLabel());
        entity.setReviewStatus("DRAFT");
        entity.setOverallOutcome(request.getOverallOutcome() != null ? request.getOverallOutcome() : "SUCCESS_WITH_ISSUES");
        entity.setSummary(request.getSummary() != null ? request.getSummary() : "待补充。");
        entity.setWhatWentWell(request.getWhatWentWell() != null ? request.getWhatWentWell() : "待补充。");
        entity.setWhatWentWrong(request.getWhatWentWrong() != null ? request.getWhatWentWrong() : "待补充。");
        entity.setCustomerImpact(request.getCustomerImpact() != null ? request.getCustomerImpact() : "待补充。");
        entity.setFollowUpActions(request.getFollowUpActions() != null ? request.getFollowUpActions() : "待补充。");
        entity.setReviewerId(request.getReviewerId() != null ? parseLong(request.getReviewerId(), "reviewerId") : null);

        releasePostmortemReviewMapper.insert(entity);

        releaseAuditTrailService.recordEvent(projectId, planId, entity.getReleaseLabel(),
                ReleaseAuditEventType.POSTMORTEM_UPDATED.name(),
                null, "postmortem review", "创建发布复盘: " + entity.getReleaseLabel(), null);

        return toReviewResponse(entity);
    }

    @Transactional(readOnly = true)
    public ReleasePostmortemReviewResponse getReview(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleasePostmortemReviewEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleasePostmortemReviewEntity::getPlanId, planId);
        wrapper.orderByDesc(ReleasePostmortemReviewEntity::getCreateTime);
        wrapper.last("LIMIT 1");
        ReleasePostmortemReviewEntity entity = releasePostmortemReviewMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Postmortem review 不存在");
        }
        return toReviewResponse(entity);
    }

    @Transactional
    public ReleasePostmortemReviewResponse updateReview(String reviewIdStr, UpdateReleasePostmortemReviewRequest request) {
        Long reviewId = parseLong(reviewIdStr, "reviewId");
        ReleasePostmortemReviewEntity entity = releasePostmortemReviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Postmortem review 不存在");
        }
        if ("ARCHIVED".equals(entity.getReviewStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ARCHIVED 状态不可编辑");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        if (request.getOverallOutcome() != null) entity.setOverallOutcome(request.getOverallOutcome());
        if (request.getSummary() != null) entity.setSummary(request.getSummary());
        if (request.getWhatWentWell() != null) entity.setWhatWentWell(request.getWhatWentWell());
        if (request.getWhatWentWrong() != null) entity.setWhatWentWrong(request.getWhatWentWrong());
        if (request.getCustomerImpact() != null) entity.setCustomerImpact(request.getCustomerImpact());
        if (request.getFollowUpActions() != null) entity.setFollowUpActions(request.getFollowUpActions());
        if (request.getReviewerId() != null) entity.setReviewerId(parseLong(request.getReviewerId(), "reviewerId"));

        releasePostmortemReviewMapper.updateById(entity);

        releaseAuditTrailService.recordEvent(entity.getProjectId(), entity.getPlanId(), entity.getReleaseLabel(),
                ReleaseAuditEventType.POSTMORTEM_UPDATED.name(),
                null, "postmortem review", "更新发布复盘: " + entity.getReleaseLabel(), null);

        return toReviewResponse(entity);
    }

    @Transactional
    public ReleasePostmortemReviewResponse updateReviewStatus(String reviewIdStr, String reviewStatus) {
        Long reviewId = parseLong(reviewIdStr, "reviewId");
        ReleasePostmortemReviewEntity entity = releasePostmortemReviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Postmortem review 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        validateReviewStatusTransition(entity.getReviewStatus(), reviewStatus);
        entity.setReviewStatus(reviewStatus);

        if ("REVIEWED".equals(reviewStatus) || "PUBLISHED".equals(reviewStatus)) {
            entity.setReviewedAt(LocalDateTime.now());
        }

        releasePostmortemReviewMapper.updateById(entity);

        releaseAuditTrailService.recordEvent(entity.getProjectId(), entity.getPlanId(), entity.getReleaseLabel(),
                ReleaseAuditEventType.POSTMORTEM_UPDATED.name(),
                null, "postmortem review", "发布复盘状态变更: " + reviewStatus, null);

        return toReviewResponse(entity);
    }

    @Transactional(readOnly = true)
    public ReleasePostmortemReviewResponse getPrefilledReview(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Aggregate signals from steps and verifications
        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);

        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);

        long failedSteps = steps.stream().filter(s -> "FAILED".equals(s.getStepStatus())).count();
        long failedVerifications = verifications.stream().filter(v -> "FAILED".equals(v.getVerificationStatus())).count();

        StringBuilder summary = new StringBuilder();
        summary.append("Release ").append(plan.getReleaseLabel()).append(" ");
        switch (plan.getRolloutStatus()) {
            case "COMPLETED" -> summary.append("completed successfully. ");
            case "ROLLED_BACK" -> summary.append("was rolled back. ");
            default -> summary.append("is in ").append(plan.getRolloutStatus()).append(" status. ");
        }
        summary.append("Steps: ").append(steps.size()).append(" total, ").append(failedSteps).append(" failed. ");
        summary.append("Verifications: ").append(verifications.size()).append(" total, ").append(failedVerifications).append(" failed. ");

        String overallOutcome = "ROLLED_BACK".equals(plan.getRolloutStatus()) ? "ROLLBACK_NEEDED"
                : failedVerifications > 0 ? "SUCCESS_WITH_ISSUES" : "SUCCESS";

        ReleasePostmortemReviewResponse resp = new ReleasePostmortemReviewResponse();
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setReviewStatus("DRAFT");
        resp.setOverallOutcome(overallOutcome);
        resp.setSummary(summary.toString());
        resp.setWhatWentWell("待补充。");
        resp.setWhatWentWrong("待补充。");
        resp.setCustomerImpact("待补充。");
        resp.setFollowUpActions("待补充。");

        return resp;
    }

    private void validateReviewStatusTransition(String current, String target) {
        switch (current) {
            case "DRAFT" -> {
                if (!"REVIEWED".equals(target) && !"PUBLISHED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "DRAFT 状态只能转为 REVIEWED 或 PUBLISHED");
            }
            case "REVIEWED" -> {
                if (!"PUBLISHED".equals(target) && !"DRAFT".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "REVIEWED 状态只能转为 PUBLISHED 或 DRAFT");
            }
            case "PUBLISHED" -> {
                if (!"ARCHIVED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "PUBLISHED 状态只能转为 ARCHIVED");
            }
            case "ARCHIVED" -> throw new BizException(ErrorCode.BAD_REQUEST, "ARCHIVED 状态不可变更");
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知状态: " + current);
        }
    }

    private ReleasePostmortemReviewResponse toReviewResponse(ReleasePostmortemReviewEntity entity) {
        ReleasePostmortemReviewResponse resp = new ReleasePostmortemReviewResponse();
        resp.setId(entity.getId().toString());
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setReviewStatus(entity.getReviewStatus());
        resp.setOverallOutcome(entity.getOverallOutcome());
        resp.setSummary(entity.getSummary());
        resp.setWhatWentWell(entity.getWhatWentWell());
        resp.setWhatWentWrong(entity.getWhatWentWrong());
        resp.setCustomerImpact(entity.getCustomerImpact());
        resp.setFollowUpActions(entity.getFollowUpActions());
        resp.setReviewerId(entity.getReviewerId() != null ? entity.getReviewerId().toString() : null);
        resp.setReviewedAt(entity.getReviewedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
