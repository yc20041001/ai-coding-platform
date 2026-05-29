package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReleaseEvidenceCenterService {

    private final ReleaseEvidenceBundleMapper releaseEvidenceBundleMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final ReleaseRollbackDrillMapper releaseRollbackDrillMapper;
    private final ReleasePostmortemReviewMapper releasePostmortemReviewMapper;
    private final ReleaseSignoffRecordMapper releaseSignoffRecordMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ObjectMapper objectMapper;

    public ReleaseEvidenceCenterService(ReleaseEvidenceBundleMapper releaseEvidenceBundleMapper,
                                         ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                         ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                         ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                         ReleaseRollbackDrillMapper releaseRollbackDrillMapper,
                                         ReleasePostmortemReviewMapper releasePostmortemReviewMapper,
                                         ReleaseSignoffRecordMapper releaseSignoffRecordMapper,
                                         ProjectPermissionService projectPermissionService,
                                         ObjectMapper objectMapper) {
        this.releaseEvidenceBundleMapper = releaseEvidenceBundleMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.releaseRollbackDrillMapper = releaseRollbackDrillMapper;
        this.releasePostmortemReviewMapper = releasePostmortemReviewMapper;
        this.releaseSignoffRecordMapper = releaseSignoffRecordMapper;
        this.projectPermissionService = projectPermissionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReleaseEvidenceBundleResponse generateBundle(String planIdStr, GenerateReleaseEvidenceBundleRequest request) {
        Long planId = parseLong(planIdStr, "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Check if bundle already exists — if so, regenerate
        LambdaQueryWrapper<ReleaseEvidenceBundleEntity> existCheck = new LambdaQueryWrapper<>();
        existCheck.eq(ReleaseEvidenceBundleEntity::getPlanId, planId);
        ReleaseEvidenceBundleEntity existing = releaseEvidenceBundleMapper.selectOne(existCheck);

        ReleaseEvidenceBundleEntity entity = existing != null ? existing : new ReleaseEvidenceBundleEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setReleaseLabel(plan.getReleaseLabel());
        entity.setBundleStatus("GENERATED");
        entity.setGeneratedBy(request.getGeneratedBy() != null ? parseLong(request.getGeneratedBy(), "generatedBy") : null);
        entity.setGeneratedAt(LocalDateTime.now());

        // Aggregate evidence data
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put("releaseLabel", plan.getReleaseLabel());
        evidenceData.put("rolloutStatus", plan.getRolloutStatus());
        evidenceData.put("rolloutStrategy", plan.getRolloutStrategy());
        evidenceData.put("targetEnvironment", plan.getTargetEnvironment());

        // Load steps
        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);
        evidenceData.put("stepCount", steps.size());
        evidenceData.put("passedStepCount", steps.stream().filter(s -> "PASSED".equals(s.getStepStatus())).count());
        evidenceData.put("failedStepCount", steps.stream().filter(s -> "FAILED".equals(s.getStepStatus())).count());

        // Load verifications
        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);
        evidenceData.put("verificationCount", verifications.size());
        evidenceData.put("failedVerificationCount", verifications.stream().filter(v -> "FAILED".equals(v.getVerificationStatus())).count());

        // Load drills
        LambdaQueryWrapper<ReleaseRollbackDrillEntity> drillWrapper = new LambdaQueryWrapper<>();
        drillWrapper.eq(ReleaseRollbackDrillEntity::getPlanId, planId);
        List<ReleaseRollbackDrillEntity> drills = releaseRollbackDrillMapper.selectList(drillWrapper);
        evidenceData.put("drillCount", drills.size());
        evidenceData.put("drillPassed", drills.stream().anyMatch(d -> "PASSED".equals(d.getDrillStatus())));

        // Load postmortem
        LambdaQueryWrapper<ReleasePostmortemReviewEntity> pmWrapper = new LambdaQueryWrapper<>();
        pmWrapper.eq(ReleasePostmortemReviewEntity::getPlanId, planId);
        List<ReleasePostmortemReviewEntity> reviews = releasePostmortemReviewMapper.selectList(pmWrapper);
        evidenceData.put("postmortemCount", reviews.size());
        if (!reviews.isEmpty()) {
            evidenceData.put("postmortemOutcome", reviews.get(0).getOverallOutcome());
        }

        // Load signoffs
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> signoffWrapper = new LambdaQueryWrapper<>();
        signoffWrapper.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        List<ReleaseSignoffRecordEntity> signoffs = releaseSignoffRecordMapper.selectList(signoffWrapper);
        evidenceData.put("signoffCount", signoffs.size());
        long approvedCount = signoffs.stream().filter(s -> "APPROVED".equals(s.getSignoffStatus())).count();
        long rejectedCount = signoffs.stream().filter(s -> "REJECTED".equals(s.getSignoffStatus())).count();
        evidenceData.put("approvedSignoffCount", approvedCount);
        evidenceData.put("rejectedSignoffCount", rejectedCount);

        // Generate markdown
        StringBuilder md = new StringBuilder();
        md.append("# Release Evidence Bundle\n\n");
        md.append("**Release**: ").append(plan.getReleaseLabel()).append("\n\n");
        md.append("**Status**: ").append(plan.getRolloutStatus()).append("\n\n");
        md.append("**Strategy**: ").append(plan.getRolloutStrategy()).append("\n\n");
        md.append("**Target Environment**: ").append(plan.getTargetEnvironment()).append("\n\n");
        md.append("**Generated At**: ").append(LocalDateTime.now()).append("\n\n");
        md.append("---\n\n");

        md.append("## Rollout Summary\n\n");
        md.append("- Total Steps: ").append(steps.size()).append("\n");
        md.append("- Passed Steps: ").append(evidenceData.get("passedStepCount")).append("\n");
        md.append("- Failed Steps: ").append(evidenceData.get("failedStepCount")).append("\n\n");

        md.append("## Verification Summary\n\n");
        md.append("- Total Verifications: ").append(verifications.size()).append("\n");
        md.append("- Failed: ").append(evidenceData.get("failedVerificationCount")).append("\n\n");

        md.append("## Rollback Drill Result\n\n");
        md.append("- Drills: ").append(drills.size()).append("\n");
        md.append("- Passed: ").append(evidenceData.get("drillPassed")).append("\n\n");

        md.append("## Postmortem Outcome\n\n");
        if (!reviews.isEmpty()) {
            md.append("- Outcome: ").append(reviews.get(0).getOverallOutcome()).append("\n");
            md.append("- Status: ").append(reviews.get(0).getReviewStatus()).append("\n\n");
        } else {
            md.append("No postmortem review recorded.\n\n");
        }

        md.append("## Sign-off Status\n\n");
        md.append("- Total: ").append(signoffs.size()).append("\n");
        md.append("- Approved: ").append(approvedCount).append("\n");
        md.append("- Rejected: ").append(rejectedCount).append("\n\n");

        md.append("## Open Risks\n\n");
        md.append("- Blocking Issues: 0 (to be confirmed)\n");
        md.append("- Residual Risks: 0 (to be confirmed)\n");

        entity.setSummaryMarkdown(md.toString());
        try {
            entity.setEvidenceJson(objectMapper.writeValueAsString(evidenceData));
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "证据数据序列化失败");
        }

        if (existing != null) {
            releaseEvidenceBundleMapper.updateById(entity);
        } else {
            releaseEvidenceBundleMapper.insert(entity);
        }

        return toBundleResponse(entity);
    }

    @Transactional(readOnly = true)
    public ReleaseEvidenceBundleResponse getBundle(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseEvidenceBundleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseEvidenceBundleEntity::getPlanId, planId);
        ReleaseEvidenceBundleEntity entity = releaseEvidenceBundleMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Evidence bundle 不存在，请先生成");
        }
        return toBundleResponse(entity);
    }

    @Transactional
    public ReleaseEvidenceBundleResponse updateBundleStatus(String planIdStr, String bundleStatus) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseEvidenceBundleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseEvidenceBundleEntity::getPlanId, planId);
        ReleaseEvidenceBundleEntity entity = releaseEvidenceBundleMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Evidence bundle 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        validateBundleStatusTransition(entity.getBundleStatus(), bundleStatus);
        entity.setBundleStatus(bundleStatus);

        releaseEvidenceBundleMapper.updateById(entity);
        return toBundleResponse(entity);
    }

    private void validateBundleStatusTransition(String current, String target) {
        switch (current) {
            case "DRAFT" -> {
                if (!"GENERATED".equals(target) && !"ARCHIVED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "DRAFT 状态只能转为 GENERATED 或 ARCHIVED");
            }
            case "GENERATED" -> {
                if (!"PUBLISHED".equals(target) && !"ARCHIVED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "GENERATED 状态只能转为 PUBLISHED 或 ARCHIVED");
            }
            case "PUBLISHED" -> {
                if (!"ARCHIVED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "PUBLISHED 状态只能转为 ARCHIVED");
            }
            case "ARCHIVED" -> throw new BizException(ErrorCode.BAD_REQUEST, "ARCHIVED 状态不可变更");
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知状态: " + current);
        }
    }

    private ReleaseEvidenceBundleResponse toBundleResponse(ReleaseEvidenceBundleEntity entity) {
        ReleaseEvidenceBundleResponse resp = new ReleaseEvidenceBundleResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setBundleStatus(entity.getBundleStatus());
        resp.setSummaryMarkdown(entity.getSummaryMarkdown());
        resp.setEvidenceJson(entity.getEvidenceJson());
        resp.setGeneratedBy(entity.getGeneratedBy() != null ? entity.getGeneratedBy().toString() : null);
        resp.setGeneratedAt(entity.getGeneratedAt());
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
