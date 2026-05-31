package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationPackageEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRecommendationPackageResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationPackageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceRecommendationPackageService {

    private final GovernanceRecommendationPackageMapper packageMapper;

    public GovernanceRecommendationPackageService(GovernanceRecommendationPackageMapper packageMapper) {
        this.packageMapper = packageMapper;
    }

    @Transactional
    public GovernanceRecommendationPackageResponse createPackage(String packageTitle, String recommendationIdStr) {
        GovernanceRecommendationPackageEntity entity = new GovernanceRecommendationPackageEntity();
        entity.setPackageTitle(packageTitle);
        entity.setPackageStatus("DRAFT");
        entity.setRecommendationContextJson("{}");
        entity.setSubmitReadyFlag(0); entity.setSubmittedFlag(0);
        if (recommendationIdStr != null) entity.setRecommendationId(parseLong(recommendationIdStr));
        packageMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationPackageResponse> listPackages() {
        return packageMapper.selectList(new LambdaQueryWrapper<GovernanceRecommendationPackageEntity>()
                .orderByDesc(GovernanceRecommendationPackageEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceRecommendationPackageResponse getPackage(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceRecommendationPackageResponse updatePackageStatus(String idStr, String newStatus) {
        GovernanceRecommendationPackageEntity entity = findEntity(idStr);
        String current = entity.getPackageStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid package transition from " + current + " to " + newStatus);
        }
        entity.setPackageStatus(newStatus);
        if ("READY".equals(newStatus)) entity.setSubmitReadyFlag(1);
        entity.setUpdateTime(LocalDateTime.now());
        packageMapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("DRAFT".equals(current) && "READY".equals(next)) return true;
        if ("READY".equals(current) && "REVIEWED".equals(next)) return true;
        if ("REVIEWED".equals(current) && "ARCHIVED".equals(next)) return true;
        return false;
    }

    private GovernanceRecommendationPackageEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceRecommendationPackageEntity entity = packageMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Package 不存在");
        return entity;
    }

    private GovernanceRecommendationPackageResponse toResponse(GovernanceRecommendationPackageEntity e) {
        GovernanceRecommendationPackageResponse r = new GovernanceRecommendationPackageResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setDraftPlanId(e.getDraftPlanId() != null ? e.getDraftPlanId().toString() : null);
        r.setPackageStatus(e.getPackageStatus()); r.setPackageTitle(e.getPackageTitle());
        r.setPackageSummary(e.getPackageSummary()); r.setRecommendationContextJson(e.getRecommendationContextJson());
        r.setAttachmentsJson(e.getAttachmentsJson()); r.setReviewNotesText(e.getReviewNotesText());
        r.setSubmitReadyFlag(e.getSubmitReadyFlag() != null && e.getSubmitReadyFlag() == 1);
        r.setSubmittedFlag(e.getSubmittedFlag() != null && e.getSubmittedFlag() == 1);
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
