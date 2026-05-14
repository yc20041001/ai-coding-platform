package com.aicoding.platform.audit.application;

import com.aicoding.platform.audit.domain.AuditLogEntity;
import com.aicoding.platform.audit.dto.AuditLogQueryRequest;
import com.aicoding.platform.audit.dto.AuditLogResponse;
import com.aicoding.platform.audit.infrastructure.AuditLogMapper;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogApplicationService.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogApplicationService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(AuditLogEntity auditLog) {
        try {
            enrichAuditLog(auditLog);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log, actionType={}, resourceType={}",
                    auditLog.getActionType(), auditLog.getResourceType(), e);
        }
    }

    public void recordSuccess(Long projectId, Long resourceId, String actionType,
                               String resourceType, String description) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setProjectId(projectId);
        entity.setResourceId(resourceId);
        entity.setActionType(actionType);
        entity.setResourceType(resourceType);
        entity.setDescription(description);
        entity.setSuccess(1);
        record(entity);
    }

    public void recordFailure(Long projectId, Long resourceId, String actionType,
                               String resourceType, String description, String errorMessage) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setProjectId(projectId);
        entity.setResourceId(resourceId);
        entity.setActionType(actionType);
        entity.setResourceType(resourceType);
        entity.setDescription(description);
        entity.setSuccess(0);
        entity.setErrorMessage(errorMessage);
        record(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLogResponse> list(AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();

        if (request.getActionType() != null && !request.getActionType().isBlank()) {
            wrapper.eq(AuditLogEntity::getActionType, request.getActionType());
        }
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            wrapper.eq(AuditLogEntity::getUserId, Long.parseLong(request.getUserId()));
        }
        if (request.getResourceType() != null && !request.getResourceType().isBlank()) {
            wrapper.eq(AuditLogEntity::getResourceType, request.getResourceType());
        }
        if (request.getResourceId() != null && !request.getResourceId().isBlank()) {
            wrapper.eq(AuditLogEntity::getResourceId, Long.parseLong(request.getResourceId()));
        }
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);

        Page<AuditLogEntity> page = new Page<>(request.getPage(), request.getPageSize());
        Page<AuditLogEntity> result = auditLogMapper.selectPage(page, wrapper);

        List<AuditLogResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, request.getPage(), request.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLogResponse> listByProject(Long projectId, AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getProjectId, projectId);

        if (request.getActionType() != null && !request.getActionType().isBlank()) {
            wrapper.eq(AuditLogEntity::getActionType, request.getActionType());
        }
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            wrapper.eq(AuditLogEntity::getUserId, Long.parseLong(request.getUserId()));
        }
        if (request.getResourceType() != null && !request.getResourceType().isBlank()) {
            wrapper.eq(AuditLogEntity::getResourceType, request.getResourceType());
        }
        if (request.getResourceId() != null && !request.getResourceId().isBlank()) {
            wrapper.eq(AuditLogEntity::getResourceId, Long.parseLong(request.getResourceId()));
        }
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);

        Page<AuditLogEntity> page = new Page<>(request.getPage(), request.getPageSize());
        Page<AuditLogEntity> result = auditLogMapper.selectPage(page, wrapper);

        List<AuditLogResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, request.getPage(), request.getPageSize(), result.getTotal());
    }

    private void enrichAuditLog(AuditLogEntity entity) {
        LoginUser currentUser = LoginUserContext.currentUser().orElse(null);
        if (currentUser != null) {
            if (entity.getUserId() == null) {
                entity.setUserId(currentUser.getUserId());
            }
            if (entity.getUsername() == null) {
                entity.setUsername(currentUser.getUsername());
            }
        }

        entity.setTraceId(MDC.get("traceId"));

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest httpReq = attrs.getRequest();
                entity.setRequestMethod(httpReq.getMethod());
                entity.setRequestPath(httpReq.getRequestURI());
                entity.setIpAddress(getClientIp(httpReq));
                entity.setUserAgent(httpReq.getHeader("User-Agent"));
            }
        } catch (Exception ignored) {
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setUserId(entity.getUserId() != null ? entity.getUserId().toString() : null);
        resp.setUsername(entity.getUsername());
        resp.setActionType(entity.getActionType());
        resp.setResourceType(entity.getResourceType());
        resp.setResourceId(entity.getResourceId() != null ? entity.getResourceId().toString() : null);
        resp.setDescription(entity.getDescription());
        resp.setTraceId(entity.getTraceId());
        resp.setSuccess(entity.getSuccess() != null && entity.getSuccess() == 1);
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return resp;
    }
}
