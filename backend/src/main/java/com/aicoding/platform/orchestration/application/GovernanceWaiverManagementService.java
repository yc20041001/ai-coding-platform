package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.domain.GovernanceWaiverRequestEntity;
import com.aicoding.platform.orchestration.dto.CreateGovernanceWaiverRequestRequest;
import com.aicoding.platform.orchestration.dto.GovernanceWaiverRequestResponse;
import com.aicoding.platform.orchestration.dto.UpdateGovernanceWaiverRequestRequest;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationItemMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWaiverRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceWaiverManagementService {

    private final GovernanceWaiverRequestMapper governanceWaiverRequestMapper;
    private final GovernanceRecommendationItemMapper governanceRecommendationItemMapper;

    public GovernanceWaiverManagementService(GovernanceWaiverRequestMapper governanceWaiverRequestMapper,
                                              GovernanceRecommendationItemMapper governanceRecommendationItemMapper) {
        this.governanceWaiverRequestMapper = governanceWaiverRequestMapper;
        this.governanceRecommendationItemMapper = governanceRecommendationItemMapper;
    }

    @Transactional
    public GovernanceWaiverRequestResponse createWaiver(String itemIdStr, CreateGovernanceWaiverRequestRequest request) {
        Long recommendationId = parseLong(itemIdStr, "recommendationId");

        // Verify recommendation item exists
        GovernanceRecommendationItemEntity item = governanceRecommendationItemMapper.selectById(recommendationId);
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Recommendation item 不存在");
        }

        // Check no active waiver exists
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> activeCheck = new LambdaQueryWrapper<>();
        activeCheck.eq(GovernanceWaiverRequestEntity::getRecommendationId, recommendationId);
        activeCheck.in(GovernanceWaiverRequestEntity::getWaiverStatus, "REQUESTED", "APPROVED");
        if (governanceWaiverRequestMapper.selectCount(activeCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该 recommendation 已有活跃 waiver");
        }

        GovernanceWaiverRequestEntity entity = new GovernanceWaiverRequestEntity();
        entity.setRecommendationId(recommendationId);
        entity.setProjectId(item.getProjectId());
        entity.setWaiverStatus("REQUESTED");
        entity.setWaiverScope(request.getWaiverScope() != null ? request.getWaiverScope() : "POLICY_EXCEPTION");
        entity.setReasonText(request.getReasonText());
        if (request.getExpiresAt() != null) {
            entity.setExpiresAt(LocalDateTime.parse(request.getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        governanceWaiverRequestMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceWaiverRequestResponse> listWaivers(String itemIdStr) {
        Long recommendationId = parseLong(itemIdStr, "recommendationId");
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceWaiverRequestEntity::getRecommendationId, recommendationId);
        wrapper.orderByDesc(GovernanceWaiverRequestEntity::getCreateTime);
        return governanceWaiverRequestMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GovernanceWaiverRequestResponse updateWaiver(String waiverIdStr, UpdateGovernanceWaiverRequestRequest request) {
        Long waiverId = parseLong(waiverIdStr, "waiverId");
        GovernanceWaiverRequestEntity entity = governanceWaiverRequestMapper.selectById(waiverId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Waiver 不存在");
        }

        if (request.getReasonText() != null) entity.setReasonText(request.getReasonText());
        if (request.getApprovalNote() != null) entity.setApprovalNote(request.getApprovalNote());
        if (request.getExpiresAt() != null) entity.setExpiresAt(LocalDateTime.parse(request.getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        entity.setUpdateTime(LocalDateTime.now());

        governanceWaiverRequestMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceWaiverRequestResponse updateWaiverStatus(String waiverIdStr, String newStatus, String approvalNote) {
        Long waiverId = parseLong(waiverIdStr, "waiverId");
        GovernanceWaiverRequestEntity entity = governanceWaiverRequestMapper.selectById(waiverId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Waiver 不存在");
        }

        String currentStatus = entity.getWaiverStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "Invalid waiver status transition from " + currentStatus + " to " + newStatus);
        }

        entity.setWaiverStatus(newStatus);
        if ("REVOKED".equals(newStatus)) {
            entity.setRevokedAt(LocalDateTime.now());
        }
        if (approvalNote != null) {
            entity.setApprovalNote(approvalNote);
        }
        entity.setUpdateTime(LocalDateTime.now());

        governanceWaiverRequestMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public int scanExpiredWaivers() {
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED");
        wrapper.isNotNull(GovernanceWaiverRequestEntity::getExpiresAt);
        wrapper.lt(GovernanceWaiverRequestEntity::getExpiresAt, LocalDateTime.now());

        List<GovernanceWaiverRequestEntity> expired = governanceWaiverRequestMapper.selectList(wrapper);
        for (GovernanceWaiverRequestEntity w : expired) {
            w.setWaiverStatus("EXPIRED");
            w.setUpdateTime(LocalDateTime.now());
            governanceWaiverRequestMapper.updateById(w);
        }
        return expired.size();
    }

    @Transactional(readOnly = true)
    public List<GovernanceWaiverRequestEntity> getActiveWaivers() {
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED");
        return governanceWaiverRequestMapper.selectList(wrapper);
    }

    @Transactional(readOnly = true)
    public List<GovernanceWaiverRequestEntity> getExpiredWaivers() {
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "EXPIRED");
        return governanceWaiverRequestMapper.selectList(wrapper);
    }

    private boolean isValidTransition(String current, String next) {
        if ("REQUESTED".equals(current) && ("APPROVED".equals(next) || "REJECTED".equals(next))) return true;
        if ("APPROVED".equals(current) && ("EXPIRED".equals(next) || "REVOKED".equals(next))) return true;
        return false;
    }

    private GovernanceWaiverRequestResponse toResponse(GovernanceWaiverRequestEntity entity) {
        GovernanceWaiverRequestResponse resp = new GovernanceWaiverRequestResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setRecommendationId(entity.getRecommendationId() != null ? entity.getRecommendationId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setWaiverStatus(entity.getWaiverStatus());
        resp.setWaiverScope(entity.getWaiverScope());
        resp.setRequestedBy(entity.getRequestedBy() != null ? entity.getRequestedBy().toString() : null);
        resp.setRequestedByName(entity.getRequestedByName());
        resp.setApprovedBy(entity.getApprovedBy() != null ? entity.getApprovedBy().toString() : null);
        resp.setApprovedByName(entity.getApprovedByName());
        resp.setReasonText(entity.getReasonText());
        resp.setApprovalNote(entity.getApprovalNote());
        resp.setExpiresAt(entity.getExpiresAt());
        resp.setRevokedAt(entity.getRevokedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
