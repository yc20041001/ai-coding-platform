package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSeverity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSlaStatus;
import com.aicoding.platform.orchestration.domain.ToolIncidentSourceType;
import com.aicoding.platform.orchestration.domain.ToolIncidentStatus;
import com.aicoding.platform.orchestration.dto.CreateToolIncidentRequest;
import com.aicoding.platform.orchestration.dto.ToolIncidentResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentSummaryResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolIncidentRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToolIncidentService {

    private static final Logger log = LoggerFactory.getLogger(ToolIncidentService.class);

    private static final Set<String> VALID_SOURCE_TYPES = Arrays.stream(ToolIncidentSourceType.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> VALID_STATUSES = Arrays.stream(ToolIncidentStatus.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> VALID_SEVERITIES = Arrays.stream(ToolIncidentSeverity.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> TERMINAL_STATUSES = Set.of(
            ToolIncidentStatus.RESOLVED.name(),
            ToolIncidentStatus.WONT_FIX.name(),
            ToolIncidentStatus.FALSE_POSITIVE.name());

    private final ToolIncidentMapper incidentMapper;
    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ToolAlertDeliveryService alertDeliveryService;
    private final ToolIncidentSlaService slaService;

    public ToolIncidentService(ToolIncidentMapper incidentMapper,
                               ToolExecutionJobMapper toolExecutionJobMapper,
                               ProjectPermissionService projectPermissionService,
                               ToolAlertDeliveryService alertDeliveryService,
                               ToolIncidentSlaService slaService) {
        this.incidentMapper = incidentMapper;
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.projectPermissionService = projectPermissionService;
        this.alertDeliveryService = alertDeliveryService;
        this.slaService = slaService;
    }

    @Transactional
    public ToolIncidentResponse createIncident(CreateToolIncidentRequest request) {
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        String sourceType = request.getSourceType();
        if (sourceType == null || !VALID_SOURCE_TYPES.contains(sourceType)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 sourceType: " + sourceType);
        }
        String severity = request.getSeverity();
        if (severity == null || !VALID_SEVERITIES.contains(severity)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 severity: " + severity);
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "标题不能为空");
        }

        // Validate linked entities
        if (request.getToolJobId() != null && !request.getToolJobId().isBlank()) {
            validateToolJobBelongsToProject(parseLong(request.getToolJobId(), "toolJobId"), projectId);
        }

        Long currentUserId = LoginUserContext.currentUserId();
        LocalDateTime now = LocalDateTime.now();

        ToolIncidentEntity entity = new ToolIncidentEntity();
        entity.setProjectId(projectId);
        entity.setSourceType(sourceType);
        entity.setSourceId(parseLongOrNull(request.getSourceId()));
        entity.setSeverity(severity);
        entity.setStatus(ToolIncidentStatus.OPEN.name());
        entity.setTitle(request.getTitle().trim());
        entity.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
        entity.setAssigneeId(parseLongOrNull(request.getAssigneeId()));
        entity.setToolExecutionId(parseLongOrNull(request.getToolExecutionId()));
        entity.setToolJobId(parseLongOrNull(request.getToolJobId()));
        entity.setOperatorReviewId(parseLongOrNull(request.getOperatorReviewId()));
        entity.setCreatedBy(currentUserId);
        entity.setFirstSeenAt(now);
        entity.setLastSeenAt(now);

        incidentMapper.insert(entity);
        log.info("Created incident: id={}, sourceType={}, projectId={}", entity.getId(), sourceType, projectId);

        // Initialize SLA
        slaService.initializeSla(entity);
        if (entity.getSlaStatus() != null) {
            incidentMapper.updateById(entity);
        }

        // Trigger mock alert routing
        triggerAlertRouting(entity);

        return toResponse(entity);
    }

    @Transactional
    public ToolIncidentResponse updateIncident(Long incidentId, UpdateToolIncidentRequest request) {
        ToolIncidentEntity entity = incidentMapper.selectById(incidentId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Incident 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.MAINTAINER, ProjectRole.OWNER);

        Long currentUserId = LoginUserContext.currentUserId();
        boolean changed = false;

        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            if (!VALID_STATUSES.contains(newStatus)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的状态: " + newStatus);
            }
            validateStatusTransition(entity.getStatus(), newStatus);
            entity.setStatus(newStatus);
            changed = true;

            if (ToolIncidentStatus.ACKNOWLEDGED.name().equals(newStatus)) {
                entity.setAcknowledgedBy(currentUserId);
                entity.setAcknowledgedAt(LocalDateTime.now());
            } else if (TERMINAL_STATUSES.contains(newStatus)) {
                entity.setResolvedBy(currentUserId);
                entity.setResolvedAt(LocalDateTime.now());
                entity.setSlaStatus(ToolIncidentSlaStatus.RESOLVED.name());
            }

            // Re-open clears resolved info and re-initializes SLA
            if (ToolIncidentStatus.OPEN.name().equals(newStatus)) {
                entity.setAcknowledgedBy(null);
                entity.setAcknowledgedAt(null);
                entity.setResolvedBy(null);
                entity.setResolvedAt(null);
                slaService.initializeSla(entity);
            }
        }

        if (request.getSeverity() != null) {
            if (!VALID_SEVERITIES.contains(request.getSeverity())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 severity: " + request.getSeverity());
            }
            entity.setSeverity(request.getSeverity());
            changed = true;
            // Re-initialize SLA for new severity
            slaService.initializeSla(entity);
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle().trim());
            changed = true;
        }
        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary().trim());
            changed = true;
        }
        if (request.getResolution() != null) {
            entity.setResolution(request.getResolution().trim());
            changed = true;
        }
        if (request.getAssigneeId() != null) {
            entity.setAssigneeId(parseLong(request.getAssigneeId(), "assigneeId"));
            changed = true;
        }

        if (changed) {
            incidentMapper.updateById(entity);
        }

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ToolIncidentResponse getIncident(Long incidentId) {
        ToolIncidentEntity entity = incidentMapper.selectById(incidentId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Incident 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<ToolIncidentResponse> listProjectIncidents(Long projectId, String status, String severity, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        LambdaQueryWrapper<ToolIncidentEntity> wrapper = new LambdaQueryWrapper<ToolIncidentEntity>()
                .eq(ToolIncidentEntity::getProjectId, projectId);

        if (status != null && !status.isBlank()) {
            if (!VALID_STATUSES.contains(status)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的状态: " + status);
            }
            wrapper.eq(ToolIncidentEntity::getStatus, status);
        }
        if (severity != null && !severity.isBlank()) {
            if (!VALID_SEVERITIES.contains(severity)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 severity: " + severity);
            }
            wrapper.eq(ToolIncidentEntity::getSeverity, severity);
        }

        wrapper.orderByDesc(ToolIncidentEntity::getLastSeenAt);

        Page<ToolIncidentEntity> mpPage = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ToolIncidentEntity> result = incidentMapper.selectPage(mpPage, wrapper);

        List<ToolIncidentResponse> records = result.getRecords().stream()
                .map(this::toResponse).collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public ToolIncidentSummaryResponse getProjectIncidentSummary(Long projectId) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        ToolIncidentSummaryResponse summary = new ToolIncidentSummaryResponse();

        // Count by status
        for (ToolIncidentStatus status : ToolIncidentStatus.values()) {
            long count = incidentMapper.selectCount(
                    new LambdaQueryWrapper<ToolIncidentEntity>()
                            .eq(ToolIncidentEntity::getProjectId, projectId)
                            .eq(ToolIncidentEntity::getStatus, status.name()));
            switch (status) {
                case OPEN -> summary.setOpenCount(count);
                case ACKNOWLEDGED -> summary.setAcknowledgedCount(count);
                case RESOLVED, WONT_FIX, FALSE_POSITIVE ->
                    summary.setResolvedCount(summary.getResolvedCount() + count);
            }
        }

        // Count by severity
        summary.setCriticalCount(incidentMapper.selectCount(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .eq(ToolIncidentEntity::getSeverity, ToolIncidentSeverity.CRITICAL.name())));
        summary.setHighCount(incidentMapper.selectCount(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .eq(ToolIncidentEntity::getSeverity, ToolIncidentSeverity.HIGH.name())));

        // Count by source type
        summary.setDeadLetteredCount(incidentMapper.selectCount(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .eq(ToolIncidentEntity::getSourceType, ToolIncidentSourceType.TOOL_JOB_DEAD_LETTERED.name())));
        summary.setRetryPendingCount(incidentMapper.selectCount(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .eq(ToolIncidentEntity::getSourceType, ToolIncidentSourceType.TOOL_JOB_RETRY_PENDING.name())));

        return summary;
    }

    @Transactional
    public Map<String, Integer> syncProblemJobs(Long projectId) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.MAINTAINER, ProjectRole.OWNER);

        String[] problemStatuses = {"FAILED", "RETRY_PENDING", "DEAD_LETTERED"};
        Map<String, String> statusToSourceType = Map.of(
                "FAILED", ToolIncidentSourceType.TOOL_JOB_FAILED.name(),
                "RETRY_PENDING", ToolIncidentSourceType.TOOL_JOB_RETRY_PENDING.name(),
                "DEAD_LETTERED", ToolIncidentSourceType.TOOL_JOB_DEAD_LETTERED.name());
        Map<String, String> statusToSeverity = Map.of(
                "FAILED", ToolIncidentSeverity.MEDIUM.name(),
                "RETRY_PENDING", ToolIncidentSeverity.LOW.name(),
                "DEAD_LETTERED", ToolIncidentSeverity.HIGH.name());

        List<ToolExecutionJobEntity> jobs = toolExecutionJobMapper.selectList(
                new LambdaQueryWrapper<ToolExecutionJobEntity>()
                        .eq(ToolExecutionJobEntity::getProjectId, projectId)
                        .in(ToolExecutionJobEntity::getStatus, (Object[]) problemStatuses));

        int created = 0, updated = 0, skipped = 0;
        LocalDateTime now = LocalDateTime.now();

        for (ToolExecutionJobEntity job : jobs) {
            String sourceType = statusToSourceType.get(job.getStatus());
            String severity = statusToSeverity.get(job.getStatus());

            // Check for existing non-terminal incident with same job_id + source_type
            ToolIncidentEntity existing = incidentMapper.selectOne(
                    new LambdaQueryWrapper<ToolIncidentEntity>()
                            .eq(ToolIncidentEntity::getToolJobId, job.getId())
                            .eq(ToolIncidentEntity::getSourceType, sourceType)
                            .last("LIMIT 1"));

            if (existing != null) {
                if (TERMINAL_STATUSES.contains(existing.getStatus())) {
                    skipped++;
                    continue;
                }
                // Update lastSeenAt
                existing.setLastSeenAt(now);
                incidentMapper.updateById(existing);
                updated++;
                continue;
            }

            ToolIncidentEntity entity = new ToolIncidentEntity();
            entity.setProjectId(projectId);
            entity.setToolJobId(job.getId());
            entity.setSourceType(sourceType);
            entity.setSourceId(job.getId());
            entity.setSeverity(severity);
            entity.setStatus(ToolIncidentStatus.OPEN.name());
            entity.setTitle(autoTitle(job));
            entity.setFirstSeenAt(now);
            entity.setLastSeenAt(now);
            entity.setCreatedBy(LoginUserContext.currentUserId());

            incidentMapper.insert(entity);
            slaService.initializeSla(entity);
            if (entity.getSlaStatus() != null) {
                incidentMapper.updateById(entity);
            }
            created++;
        }

        log.info("syncProblemJobs: projectId={}, created={}, updated={}, skipped={}", projectId, created, updated, skipped);

        Map<String, Integer> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("skipped", skipped);
        return result;
    }

    private void triggerAlertRouting(ToolIncidentEntity incident) {
        try {
            alertDeliveryService.routeIncident(incident);
        } catch (Exception e) {
            log.warn("Alert routing failed for incident {}: {}", incident.getId(), e.getMessage());
        }
    }

    private void validateToolJobBelongsToProject(Long jobId, Long projectId) {
        ToolExecutionJobEntity job = toolExecutionJobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Tool job 不存在");
        }
        if (!projectId.equals(job.getProjectId())) {
            throw new BizException(ErrorCode.PROJECT_ACCESS_DENIED, "Tool job 不属于该项目");
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) return;

        // Allow re-open from any terminal status
        if (ToolIncidentStatus.OPEN.name().equals(newStatus)) return;

        // From OPEN: can go to any status
        if (ToolIncidentStatus.OPEN.name().equals(currentStatus)) return;

        // From ACKNOWLEDGED: can go to terminal
        if (ToolIncidentStatus.ACKNOWLEDGED.name().equals(currentStatus) && TERMINAL_STATUSES.contains(newStatus)) return;

        throw new BizException(ErrorCode.VALIDATION_ERROR,
                "不能从 " + currentStatus + " 转换到 " + newStatus);
    }

    private String autoTitle(ToolExecutionJobEntity job) {
        String key = job.getToolKey() != null ? job.getToolKey() : "unknown";
        return switch (job.getStatus()) {
            case "FAILED" -> "工具 " + key + " 执行失败";
            case "RETRY_PENDING" -> "工具 " + key + " 待重试";
            case "DEAD_LETTERED" -> "工具 " + key + " 已进入死信队列";
            default -> "工具 " + key + " 异常";
        };
    }

    private ToolIncidentResponse toResponse(ToolIncidentEntity entity) {
        ToolIncidentResponse resp = new ToolIncidentResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId() != null ? entity.getToolExecutionId().toString() : null);
        resp.setToolJobId(entity.getToolJobId() != null ? entity.getToolJobId().toString() : null);
        resp.setOperatorReviewId(entity.getOperatorReviewId() != null ? entity.getOperatorReviewId().toString() : null);
        resp.setSourceType(entity.getSourceType());
        resp.setSourceId(entity.getSourceId() != null ? entity.getSourceId().toString() : null);
        resp.setSeverity(entity.getSeverity());
        resp.setStatus(entity.getStatus());
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setResolution(entity.getResolution());
        resp.setAssigneeId(entity.getAssigneeId() != null ? entity.getAssigneeId().toString() : null);
        resp.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null);
        resp.setAcknowledgedBy(entity.getAcknowledgedBy() != null ? entity.getAcknowledgedBy().toString() : null);
        resp.setResolvedBy(entity.getResolvedBy() != null ? entity.getResolvedBy().toString() : null);
        resp.setFirstSeenAt(entity.getFirstSeenAt());
        resp.setLastSeenAt(entity.getLastSeenAt());
        resp.setAcknowledgedAt(entity.getAcknowledgedAt());
        resp.setResolvedAt(entity.getResolvedAt());
        resp.setSlaMinutes(entity.getSlaMinutes());
        resp.setDueAt(entity.getDueAt());
        resp.setBreachedAt(entity.getBreachedAt());
        resp.setSlaStatus(entity.getSlaStatus());
        resp.setEscalationLevel(entity.getEscalationLevel());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String field) {
        try { return Long.valueOf(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.VALIDATION_ERROR, field + " 格式无效"); }
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.valueOf(value); }
        catch (NumberFormatException e) { return null; }
    }
}
