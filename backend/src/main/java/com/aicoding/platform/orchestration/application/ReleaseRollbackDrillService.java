package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ReleaseAuditEventType;
import com.aicoding.platform.orchestration.domain.ReleaseRollbackDrillEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.dto.CreateReleaseRollbackDrillRequest;
import com.aicoding.platform.orchestration.dto.ReleaseRollbackDrillResponse;
import com.aicoding.platform.orchestration.dto.UpdateReleaseRollbackDrillRequest;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRollbackDrillMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReleaseRollbackDrillService {

    private final ReleaseRollbackDrillMapper releaseRollbackDrillMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseAuditTrailService releaseAuditTrailService;
    private final ProjectPermissionService projectPermissionService;

    public ReleaseRollbackDrillService(ReleaseRollbackDrillMapper releaseRollbackDrillMapper,
                                       ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                       ReleaseAuditTrailService releaseAuditTrailService,
                                       ProjectPermissionService projectPermissionService) {
        this.releaseRollbackDrillMapper = releaseRollbackDrillMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseAuditTrailService = releaseAuditTrailService;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ReleaseRollbackDrillResponse createDrill(CreateReleaseRollbackDrillRequest request) {
        Long planId = parseLong(request.getPlanId(), "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        ReleaseRollbackDrillEntity entity = new ReleaseRollbackDrillEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setReleaseLabel(request.getReleaseLabel() != null ? request.getReleaseLabel() : plan.getReleaseLabel());
        entity.setDrillStatus("PLANNED");
        entity.setDrillScope(request.getDrillScope() != null ? request.getDrillScope() : "CONFIG_ONLY");
        entity.setEnvironmentName(request.getEnvironmentName() != null ? request.getEnvironmentName() : "production");
        entity.setOwnerId(request.getOwnerId() != null ? parseLong(request.getOwnerId(), "ownerId") : null);
        entity.setExecutorId(request.getExecutorId() != null ? parseLong(request.getExecutorId(), "executorId") : null);
        entity.setPlannedAt(request.getPlannedAt());
        entity.setSuccessCriteria(request.getSuccessCriteria());
        entity.setRollbackStepsSummary(request.getRollbackStepsSummary());
        entity.setBlockersSummary(request.getBlockersSummary());
        entity.setResultSummary(request.getResultSummary());
        entity.setEvidenceJson(request.getEvidenceJson());

        releaseRollbackDrillMapper.insert(entity);

        releaseAuditTrailService.recordEvent(projectId, planId, entity.getReleaseLabel(),
                ReleaseAuditEventType.ROLLBACK_DRILL_UPDATED.name(),
                null, "rollback drill", "创建回滚演练: " + entity.getReleaseLabel(), null);

        return toDrillResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReleaseRollbackDrillResponse> listDrills(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseRollbackDrillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRollbackDrillEntity::getPlanId, planId);
        wrapper.orderByDesc(ReleaseRollbackDrillEntity::getCreateTime);
        return releaseRollbackDrillMapper.selectList(wrapper).stream()
                .map(this::toDrillResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseRollbackDrillResponse getDrill(String drillIdStr) {
        Long drillId = parseLong(drillIdStr, "drillId");
        ReleaseRollbackDrillEntity entity = releaseRollbackDrillMapper.selectById(drillId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollback drill 不存在");
        }
        return toDrillResponse(entity);
    }

    @Transactional
    public ReleaseRollbackDrillResponse updateDrill(String drillIdStr, UpdateReleaseRollbackDrillRequest request) {
        Long drillId = parseLong(drillIdStr, "drillId");
        ReleaseRollbackDrillEntity entity = releaseRollbackDrillMapper.selectById(drillId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollback drill 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        if (request.getDrillScope() != null) entity.setDrillScope(request.getDrillScope());
        if (request.getEnvironmentName() != null) entity.setEnvironmentName(request.getEnvironmentName());
        if (request.getOwnerId() != null) entity.setOwnerId(parseLong(request.getOwnerId(), "ownerId"));
        if (request.getExecutorId() != null) entity.setExecutorId(parseLong(request.getExecutorId(), "executorId"));
        if (request.getPlannedAt() != null) entity.setPlannedAt(request.getPlannedAt());
        if (request.getStartedAt() != null) entity.setStartedAt(request.getStartedAt());
        if (request.getFinishedAt() != null) entity.setFinishedAt(request.getFinishedAt());
        if (request.getDurationSeconds() != null) entity.setDurationSeconds(request.getDurationSeconds());
        if (request.getSuccessCriteria() != null) entity.setSuccessCriteria(request.getSuccessCriteria());
        if (request.getRollbackStepsSummary() != null) entity.setRollbackStepsSummary(request.getRollbackStepsSummary());
        if (request.getBlockersSummary() != null) entity.setBlockersSummary(request.getBlockersSummary());
        if (request.getResultSummary() != null) entity.setResultSummary(request.getResultSummary());
        if (request.getEvidenceJson() != null) entity.setEvidenceJson(request.getEvidenceJson());

        releaseRollbackDrillMapper.updateById(entity);

        releaseAuditTrailService.recordEvent(entity.getProjectId(), entity.getPlanId(), entity.getReleaseLabel(),
                ReleaseAuditEventType.ROLLBACK_DRILL_UPDATED.name(),
                null, "rollback drill", "更新回滚演练: " + entity.getReleaseLabel(), null);

        return toDrillResponse(entity);
    }

    @Transactional
    public ReleaseRollbackDrillResponse updateDrillStatus(String drillIdStr, String drillStatus) {
        Long drillId = parseLong(drillIdStr, "drillId");
        ReleaseRollbackDrillEntity entity = releaseRollbackDrillMapper.selectById(drillId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollback drill 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        validateDrillStatusTransition(entity.getDrillStatus(), drillStatus);
        entity.setDrillStatus(drillStatus);

        if ("RUNNING".equals(drillStatus) && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }
        if (isDrillTerminalStatus(drillStatus) && entity.getFinishedAt() == null) {
            entity.setFinishedAt(LocalDateTime.now());
        }
        if (entity.getStartedAt() != null && entity.getFinishedAt() != null && entity.getDurationSeconds() == null) {
            entity.setDurationSeconds(ChronoUnit.SECONDS.between(entity.getStartedAt(), entity.getFinishedAt()));
        }

        releaseRollbackDrillMapper.updateById(entity);

        releaseAuditTrailService.recordEvent(entity.getProjectId(), entity.getPlanId(), entity.getReleaseLabel(),
                ReleaseAuditEventType.ROLLBACK_DRILL_UPDATED.name(),
                null, "rollback drill", "回滚演练状态变更: " + entity.getDrillStatus(), null);

        return toDrillResponse(entity);
    }

    @Transactional(readOnly = true)
    public boolean isRollbackReady(Long planId) {
        LambdaQueryWrapper<ReleaseRollbackDrillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRollbackDrillEntity::getPlanId, planId);
        wrapper.orderByDesc(ReleaseRollbackDrillEntity::getCreateTime);
        wrapper.last("LIMIT 1");
        ReleaseRollbackDrillEntity latest = releaseRollbackDrillMapper.selectOne(wrapper);

        if (latest == null) return false;
        if (!"PASSED".equals(latest.getDrillStatus())) return false;
        if (latest.getRollbackStepsSummary() == null || latest.getRollbackStepsSummary().isBlank()) return false;
        return latest.getBlockersSummary() == null || latest.getBlockersSummary().isBlank();
    }

    private void validateDrillStatusTransition(String current, String target) {
        switch (current) {
            case "PLANNED" -> {
                if (!"RUNNING".equals(target) && !"CANCELLED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "PLANNED 状态只能转为 RUNNING 或 CANCELLED");
            }
            case "RUNNING" -> {
                if (!"PASSED".equals(target) && !"FAILED".equals(target) && !"BLOCKED".equals(target))
                    throw new BizException(ErrorCode.BAD_REQUEST, "RUNNING 状态只能转为 PASSED、FAILED 或 BLOCKED");
            }
            case "PASSED" -> throw new BizException(ErrorCode.BAD_REQUEST, "PASSED 状态不可变更");
            case "FAILED" -> throw new BizException(ErrorCode.BAD_REQUEST, "FAILED 状态不可变更");
            case "BLOCKED" -> throw new BizException(ErrorCode.BAD_REQUEST, "BLOCKED 状态不可变更");
            case "CANCELLED" -> throw new BizException(ErrorCode.BAD_REQUEST, "CANCELLED 状态不可变更");
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知状态: " + current);
        }
    }

    private boolean isDrillTerminalStatus(String status) {
        return "PASSED".equals(status) || "FAILED".equals(status) || "BLOCKED".equals(status) || "CANCELLED".equals(status);
    }

    private ReleaseRollbackDrillResponse toDrillResponse(ReleaseRollbackDrillEntity entity) {
        ReleaseRollbackDrillResponse resp = new ReleaseRollbackDrillResponse();
        resp.setId(entity.getId().toString());
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setDrillStatus(entity.getDrillStatus());
        resp.setDrillScope(entity.getDrillScope());
        resp.setEnvironmentName(entity.getEnvironmentName());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setExecutorId(entity.getExecutorId() != null ? entity.getExecutorId().toString() : null);
        resp.setPlannedAt(entity.getPlannedAt());
        resp.setStartedAt(entity.getStartedAt());
        resp.setFinishedAt(entity.getFinishedAt());
        resp.setDurationSeconds(entity.getDurationSeconds());
        resp.setSuccessCriteria(entity.getSuccessCriteria());
        resp.setRollbackStepsSummary(entity.getRollbackStepsSummary());
        resp.setBlockersSummary(entity.getBlockersSummary());
        resp.setResultSummary(entity.getResultSummary());
        resp.setEvidenceJson(entity.getEvidenceJson());
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
