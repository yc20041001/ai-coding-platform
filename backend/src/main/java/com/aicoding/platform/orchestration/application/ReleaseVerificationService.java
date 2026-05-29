package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ReleaseVerificationRecordEntity;
import com.aicoding.platform.orchestration.dto.CreateReleaseVerificationRecordRequest;
import com.aicoding.platform.orchestration.dto.ReleaseVerificationRecordResponse;
import com.aicoding.platform.orchestration.dto.UpdateReleaseVerificationRecordRequest;
import com.aicoding.platform.orchestration.infrastructure.ReleaseVerificationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReleaseVerificationService {

    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final ProjectPermissionService projectPermissionService;

    public ReleaseVerificationService(ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                      ProjectPermissionService projectPermissionService) {
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public List<ReleaseVerificationRecordResponse> listVerifications(String planIdStr, String phase) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseVerificationRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        if (phase != null && !phase.isEmpty()) {
            wrapper.eq(ReleaseVerificationRecordEntity::getVerificationPhase, phase);
        }
        wrapper.orderByAsc(ReleaseVerificationRecordEntity::getRecordedAt);
        return releaseVerificationRecordMapper.selectList(wrapper).stream()
                .map(this::toVerificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReleaseVerificationRecordResponse createVerification(CreateReleaseVerificationRecordRequest request) {
        Long planId = parseLong(request.getPlanId(), "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseVerificationRecordEntity entity = new ReleaseVerificationRecordEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setVerificationPhase(request.getVerificationPhase());
        entity.setVerificationKey(request.getVerificationKey() != null ? request.getVerificationKey() : "manual-" + System.currentTimeMillis());
        entity.setDisplayName(request.getDisplayName());
        entity.setVerificationStatus(request.getVerificationStatus() != null ? request.getVerificationStatus() : "PENDING");
        entity.setSeverity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM");
        entity.setSummary(request.getSummary() != null ? request.getSummary() : "");
        entity.setDetail(request.getDetail());
        entity.setEvidenceJson(request.getEvidenceJson());
        entity.setRelatedIncidentId(request.getRelatedIncidentId() != null ? parseLong(request.getRelatedIncidentId(), "relatedIncidentId") : null);
        entity.setRelatedAlertId(request.getRelatedAlertId() != null ? parseLong(request.getRelatedAlertId(), "relatedAlertId") : null);
        entity.setRecordedBy(request.getRecordedBy() != null ? parseLong(request.getRecordedBy(), "recordedBy") : null);
        entity.setRecordedAt(LocalDateTime.now());

        releaseVerificationRecordMapper.insert(entity);
        return toVerificationResponse(entity);
    }

    @Transactional
    public ReleaseVerificationRecordResponse updateVerification(String recordIdStr, UpdateReleaseVerificationRecordRequest request) {
        Long recordId = parseLong(recordIdStr, "recordId");
        ReleaseVerificationRecordEntity entity = releaseVerificationRecordMapper.selectById(recordId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Verification record 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        if (request.getVerificationStatus() != null) entity.setVerificationStatus(request.getVerificationStatus());
        if (request.getSeverity() != null) entity.setSeverity(request.getSeverity());
        if (request.getSummary() != null) entity.setSummary(request.getSummary());
        if (request.getDetail() != null) entity.setDetail(request.getDetail());
        if (request.getEvidenceJson() != null) entity.setEvidenceJson(request.getEvidenceJson());

        releaseVerificationRecordMapper.updateById(entity);
        return toVerificationResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReleaseVerificationRecordResponse> listVerificationsByPlanAndPhase(Long planId, String phase) {
        LambdaQueryWrapper<ReleaseVerificationRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        if (phase != null) {
            wrapper.eq(ReleaseVerificationRecordEntity::getVerificationPhase, phase);
        }
        return releaseVerificationRecordMapper.selectList(wrapper).stream()
                .map(this::toVerificationResponse)
                .collect(Collectors.toList());
    }

    private ReleaseVerificationRecordResponse toVerificationResponse(ReleaseVerificationRecordEntity entity) {
        ReleaseVerificationRecordResponse resp = new ReleaseVerificationRecordResponse();
        resp.setId(entity.getId().toString());
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setVerificationPhase(entity.getVerificationPhase());
        resp.setVerificationKey(entity.getVerificationKey());
        resp.setDisplayName(entity.getDisplayName());
        resp.setVerificationStatus(entity.getVerificationStatus());
        resp.setSeverity(entity.getSeverity());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setEvidenceJson(entity.getEvidenceJson());
        resp.setRelatedIncidentId(entity.getRelatedIncidentId() != null ? entity.getRelatedIncidentId().toString() : null);
        resp.setRelatedAlertId(entity.getRelatedAlertId() != null ? entity.getRelatedAlertId().toString() : null);
        resp.setRecordedBy(entity.getRecordedBy() != null ? entity.getRecordedBy().toString() : null);
        resp.setRecordedAt(entity.getRecordedAt());
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
