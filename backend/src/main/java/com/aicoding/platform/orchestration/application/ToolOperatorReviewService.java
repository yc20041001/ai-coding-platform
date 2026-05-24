package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewEntity;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewSeverity;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewStatus;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewTargetType;
import com.aicoding.platform.orchestration.dto.CreateToolOperatorReviewRequest;
import com.aicoding.platform.orchestration.dto.ToolOperatorReviewResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolOperatorReviewRequest;
import com.aicoding.platform.orchestration.infrastructure.MultiAgentRunMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolOperatorReviewMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToolOperatorReviewService {

    private static final Logger log = LoggerFactory.getLogger(ToolOperatorReviewService.class);

    private static final Set<String> VALID_TARGET_TYPES = Arrays.stream(ToolOperatorReviewTargetType.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> VALID_STATUSES = Arrays.stream(ToolOperatorReviewStatus.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> VALID_SEVERITIES = Arrays.stream(ToolOperatorReviewSeverity.values())
            .map(Enum::name).collect(Collectors.toSet());

    private final ToolOperatorReviewMapper reviewMapper;
    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final MultiAgentRunMapper multiAgentRunMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ProjectPermissionService projectPermissionService;

    public ToolOperatorReviewService(ToolOperatorReviewMapper reviewMapper,
                                     ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                     ToolExecutionJobMapper toolExecutionJobMapper,
                                     MultiAgentRunMapper multiAgentRunMapper,
                                     AiTaskMapper aiTaskMapper,
                                     ProjectPermissionService projectPermissionService) {
        this.reviewMapper = reviewMapper;
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.multiAgentRunMapper = multiAgentRunMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ToolOperatorReviewResponse createReview(CreateToolOperatorReviewRequest request) {
        String targetType = request.getReviewTargetType();
        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的审查目标类型: " + targetType);
        }
        Long targetId = parseLongId(request.getReviewTargetId(), "reviewTargetId");

        if (request.getSeverity() == null || !VALID_SEVERITIES.contains(request.getSeverity())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的严重级别: " + request.getSeverity());
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "审查标题不能为空");
        }

        Long projectId = resolveProjectId(targetType, targetId);
        // Check DEVELOPER+ permission (operator review needs write-like access)
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        Long currentUserId = LoginUserContext.currentUserId();
        if (currentUserId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        ToolOperatorReviewEntity entity = new ToolOperatorReviewEntity();
        entity.setProjectId(projectId);
        entity.setReviewTargetType(targetType);
        entity.setReviewTargetId(targetId);
        entity.setStatus(ToolOperatorReviewStatus.OPEN.name());
        entity.setSeverity(request.getSeverity());
        entity.setTitle(request.getTitle().trim());
        entity.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);

        // Resolve assignee
        if (request.getAssigneeId() != null && !request.getAssigneeId().isBlank()) {
            entity.setAssigneeId(parseLongId(request.getAssigneeId(), "assigneeId"));
        }

        entity.setCreatedBy(currentUserId);

        reviewMapper.insert(entity);
        log.info("Created operator review: entityId={}, targetType={}, targetId={}", entity.getId(), targetType, targetId);

        return toResponse(entity);
    }

    @Transactional
    public ToolOperatorReviewResponse updateReview(String id, UpdateToolOperatorReviewRequest request) {
        Long reviewId = parseLongId(id, "id");
        ToolOperatorReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Operator review 记录不存在");
        }

        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        boolean changed = false;

        if (request.getStatus() != null) {
            if (!VALID_STATUSES.contains(request.getStatus())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的状态: " + request.getStatus());
            }
            entity.setStatus(request.getStatus());
            changed = true;

            // Auto-set resolved info on RESOLVED / WONT_FIX / FALSE_POSITIVE
            if (isTerminalStatus(request.getStatus())) {
                Long currentUserId = LoginUserContext.currentUserId();
                entity.setResolvedBy(currentUserId);
                entity.setResolvedAt(LocalDateTime.now());
            }
        }

        if (request.getSeverity() != null) {
            if (!VALID_SEVERITIES.contains(request.getSeverity())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的严重级别: " + request.getSeverity());
            }
            entity.setSeverity(request.getSeverity());
            changed = true;
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
            entity.setAssigneeId(parseLongId(request.getAssigneeId(), "assigneeId"));
            changed = true;
        }

        if (changed) {
            reviewMapper.updateById(entity);
        }

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ToolOperatorReviewResponse getReview(String id) {
        Long reviewId = parseLongId(id, "id");
        ToolOperatorReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Operator review 记录不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<ToolOperatorReviewResponse> listProjectReviews(Long projectId, String status, String severity, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        LambdaQueryWrapper<ToolOperatorReviewEntity> wrapper = new LambdaQueryWrapper<ToolOperatorReviewEntity>()
                .eq(ToolOperatorReviewEntity.GET_PROJECT_ID, projectId);

        if (status != null && !status.isBlank()) {
            if (!VALID_STATUSES.contains(status)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的状态: " + status);
            }
            wrapper.eq(ToolOperatorReviewEntity.GET_STATUS, status);
        }

        if (severity != null && !severity.isBlank()) {
            if (!VALID_SEVERITIES.contains(severity)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的严重级别: " + severity);
            }
            wrapper.eq(ToolOperatorReviewEntity.GET_SEVERITY, severity);
        }

        // Default sort by createTime desc
        wrapper.orderByDesc(ToolOperatorReviewEntity.GET_CREATE_TIME);

        String sort = pageQuery.getSort();
        if (sort != null && !sort.isBlank()) {
            applySort(wrapper, sort);
        }

        Page<ToolOperatorReviewEntity> mpPage = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ToolOperatorReviewEntity> result = reviewMapper.selectPage(mpPage, wrapper);

        List<ToolOperatorReviewResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public List<ToolOperatorReviewResponse> listTargetReviews(String targetType, String targetId) {
        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的审查目标类型: " + targetType);
        }
        Long tId = parseLongId(targetId, "targetId");

        List<ToolOperatorReviewEntity> entities = reviewMapper.selectList(
                new LambdaQueryWrapper<ToolOperatorReviewEntity>()
                        .eq(ToolOperatorReviewEntity.GET_REVIEW_TARGET_TYPE, targetType)
                        .eq(ToolOperatorReviewEntity.GET_REVIEW_TARGET_ID, tId)
                        .orderByDesc(ToolOperatorReviewEntity.GET_CREATE_TIME));

        if (!entities.isEmpty()) {
            projectPermissionService.checkProjectRole(entities.get(0).getProjectId(),
                    ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);
        }

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Long resolveProjectId(String targetType, Long targetId) {
        return switch (targetType) {
            case "TOOL_EXECUTION" -> resolveNullableProjectId(
                    toolSandboxExecutionMapper.selectById(targetId), "Tool execution");
            case "TOOL_JOB" -> resolveNullableProjectId(
                    toolExecutionJobMapper.selectById(targetId), "Tool job");
            case "MULTI_AGENT_RUN" -> resolveNullableProjectId(
                    multiAgentRunMapper.selectById(targetId), "Multi-agent run");
            case "TASK" -> resolveNullableProjectId(
                    aiTaskMapper.selectById(targetId), "Task");
            default -> throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持的审查目标类型: " + targetType);
        };
    }

    private Long resolveNullableProjectId(Object entity, String label) {
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, label + " 不存在");
        }
        // All target entities have getProjectId()
        if (entity instanceof com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity e) {
            return e.getProjectId();
        } else if (entity instanceof com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity e) {
            return e.getProjectId();
        } else if (entity instanceof com.aicoding.platform.orchestration.domain.MultiAgentRunEntity e) {
            return e.getProjectId();
        } else if (entity instanceof com.aicoding.platform.task.domain.AiTaskEntity e) {
            return e.getProjectId();
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "无法解析项目 ID");
    }

    private static boolean isTerminalStatus(String status) {
        return ToolOperatorReviewStatus.RESOLVED.name().equals(status)
                || ToolOperatorReviewStatus.WONT_FIX.name().equals(status)
                || ToolOperatorReviewStatus.FALSE_POSITIVE.name().equals(status);
    }

    private void applySort(LambdaQueryWrapper<ToolOperatorReviewEntity> wrapper, String sort) {
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0].trim() : "createTime";
        String dir = parts.length > 1 ? parts[1].trim() : "desc";

        boolean asc = "asc".equalsIgnoreCase(dir);

        switch (field) {
            case "createTime" -> {
                if (asc) wrapper.orderByAsc(ToolOperatorReviewEntity.GET_CREATE_TIME);
                else wrapper.orderByDesc(ToolOperatorReviewEntity.GET_CREATE_TIME);
            }
            case "severity" -> {
                if (asc) wrapper.orderByAsc(ToolOperatorReviewEntity.GET_SEVERITY);
                else wrapper.orderByDesc(ToolOperatorReviewEntity.GET_SEVERITY);
            }
            case "status" -> {
                if (asc) wrapper.orderByAsc(ToolOperatorReviewEntity.GET_STATUS);
                else wrapper.orderByDesc(ToolOperatorReviewEntity.GET_STATUS);
            }
            default -> wrapper.orderByDesc(ToolOperatorReviewEntity.GET_CREATE_TIME);
        }
    }

    private static Long parseLongId(String id, String fieldName) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, fieldName + " 格式无效");
        }
    }

    private ToolOperatorReviewResponse toResponse(ToolOperatorReviewEntity entity) {
        ToolOperatorReviewResponse resp = new ToolOperatorReviewResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId() != null ? entity.getToolExecutionId().toString() : null);
        resp.setToolJobId(entity.getToolJobId() != null ? entity.getToolJobId().toString() : null);
        resp.setReviewTargetType(entity.getReviewTargetType());
        resp.setReviewTargetId(entity.getReviewTargetId() != null ? entity.getReviewTargetId().toString() : null);
        resp.setStatus(entity.getStatus());
        resp.setSeverity(entity.getSeverity());
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setResolution(entity.getResolution());
        resp.setAssigneeId(entity.getAssigneeId() != null ? entity.getAssigneeId().toString() : null);
        resp.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null);
        resp.setResolvedBy(entity.getResolvedBy() != null ? entity.getResolvedBy().toString() : null);
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        resp.setResolvedAt(entity.getResolvedAt());
        return resp;
    }
}
