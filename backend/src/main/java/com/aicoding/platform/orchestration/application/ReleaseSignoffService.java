package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.domain.ReleaseSignoffRecordEntity;
import com.aicoding.platform.orchestration.dto.CreateReleaseSignoffRecordRequest;
import com.aicoding.platform.orchestration.dto.ReleaseSignoffRecordResponse;
import com.aicoding.platform.orchestration.dto.UpdateReleaseSignoffRecordRequest;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseSignoffRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReleaseSignoffService {

    private final ReleaseSignoffRecordMapper releaseSignoffRecordMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ProjectPermissionService projectPermissionService;

    private static final List<String> DEFAULT_ROLES = Arrays.asList(
            "TECH_OWNER", "PRODUCT_OWNER", "OPS_OWNER",
            "SECURITY_REVIEWER", "QA_REVIEWER"
    );

    private static final List<String> TERMINAL_SIGNOFF_STATUSES = Arrays.asList(
            "APPROVED", "REJECTED", "SKIPPED"
    );

    public ReleaseSignoffService(ReleaseSignoffRecordMapper releaseSignoffRecordMapper,
                                  ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                  ProjectPermissionService projectPermissionService) {
        this.releaseSignoffRecordMapper = releaseSignoffRecordMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public List<ReleaseSignoffRecordResponse> listSignoffs(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        wrapper.orderByAsc(ReleaseSignoffRecordEntity::getSignoffRole);
        List<ReleaseSignoffRecordEntity> entities = releaseSignoffRecordMapper.selectList(wrapper);

        // If no signoffs exist, initialize with default roles
        if (entities.isEmpty()) {
            ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
            if (plan == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
            }
            for (String role : DEFAULT_ROLES) {
                ReleaseSignoffRecordEntity e = new ReleaseSignoffRecordEntity();
                e.setPlanId(planId);
                e.setProjectId(plan.getProjectId());
                e.setReleaseLabel(plan.getReleaseLabel());
                e.setSignoffRole(role);
                e.setSignoffStatus("PENDING");
                releaseSignoffRecordMapper.insert(e);
                entities.add(e);
            }
        }

        return entities.stream().map(this::toSignoffResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReleaseSignoffRecordResponse createSignoff(String planIdStr, CreateReleaseSignoffRecordRequest request) {
        Long planId = parseLong(planIdStr, "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Check for duplicate role
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        dupCheck.eq(ReleaseSignoffRecordEntity::getSignoffRole, request.getSignoffRole());
        if (releaseSignoffRecordMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该 plan 已存在 " + request.getSignoffRole() + " 角色签字");
        }

        ReleaseSignoffRecordEntity entity = new ReleaseSignoffRecordEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setReleaseLabel(plan.getReleaseLabel());
        entity.setSignoffRole(request.getSignoffRole());
        entity.setSignoffStatus(request.getSignoffStatus() != null ? request.getSignoffStatus() : "PENDING");
        entity.setSignerId(request.getSignerId() != null ? parseLong(request.getSignerId(), "signerId") : null);
        entity.setSignerName(request.getSignerName());
        entity.setCommentText(request.getCommentText());
        if (TERMINAL_SIGNOFF_STATUSES.contains(entity.getSignoffStatus())) {
            entity.setSignedAt(LocalDateTime.now());
        }

        releaseSignoffRecordMapper.insert(entity);
        return toSignoffResponse(entity);
    }

    @Transactional
    public ReleaseSignoffRecordResponse updateSignoff(String signoffIdStr, UpdateReleaseSignoffRecordRequest request) {
        Long signoffId = parseLong(signoffIdStr, "signoffId");
        ReleaseSignoffRecordEntity entity = releaseSignoffRecordMapper.selectById(signoffId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Signoff record 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        if (request.getSignoffStatus() != null) entity.setSignoffStatus(request.getSignoffStatus());
        if (request.getSignerId() != null) entity.setSignerId(parseLong(request.getSignerId(), "signerId"));
        if (request.getSignerName() != null) entity.setSignerName(request.getSignerName());
        if (request.getCommentText() != null) entity.setCommentText(request.getCommentText());
        if (request.getSignoffStatus() != null && TERMINAL_SIGNOFF_STATUSES.contains(request.getSignoffStatus())) {
            entity.setSignedAt(LocalDateTime.now());
        }

        releaseSignoffRecordMapper.updateById(entity);
        return toSignoffResponse(entity);
    }

    @Transactional
    public ReleaseSignoffRecordResponse updateSignoffStatus(String signoffIdStr, String signoffStatus) {
        Long signoffId = parseLong(signoffIdStr, "signoffId");
        ReleaseSignoffRecordEntity entity = releaseSignoffRecordMapper.selectById(signoffId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Signoff record 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        entity.setSignoffStatus(signoffStatus);
        if (TERMINAL_SIGNOFF_STATUSES.contains(signoffStatus)) {
            entity.setSignedAt(LocalDateTime.now());
        }

        releaseSignoffRecordMapper.updateById(entity);
        return toSignoffResponse(entity);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCompletionRate(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        List<ReleaseSignoffRecordEntity> entities = releaseSignoffRecordMapper.selectList(wrapper);

        if (entities.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long completed = entities.stream()
                .filter(e -> !"PENDING".equals(e.getSignoffStatus()))
                .count();
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(entities.size()), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<String> findMissingSignoffs(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        wrapper.eq(ReleaseSignoffRecordEntity::getSignoffStatus, "PENDING");
        return releaseSignoffRecordMapper.selectList(wrapper).stream()
                .map(ReleaseSignoffRecordEntity::getSignoffRole)
                .collect(Collectors.toList());
    }

    private ReleaseSignoffRecordResponse toSignoffResponse(ReleaseSignoffRecordEntity entity) {
        ReleaseSignoffRecordResponse resp = new ReleaseSignoffRecordResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setSignoffRole(entity.getSignoffRole());
        resp.setSignoffStatus(entity.getSignoffStatus());
        resp.setSignerId(entity.getSignerId() != null ? entity.getSignerId().toString() : null);
        resp.setSignerName(entity.getSignerName());
        resp.setCommentText(entity.getCommentText());
        resp.setSignedAt(entity.getSignedAt());
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
