package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ToolAlertChannel;
import com.aicoding.platform.orchestration.domain.ToolAlertRuleEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSeverity;
import com.aicoding.platform.orchestration.dto.CreateToolAlertRuleRequest;
import com.aicoding.platform.orchestration.dto.ToolAlertRuleResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolAlertRuleRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolAlertRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToolAlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(ToolAlertRuleService.class);

    private static final Set<String> VALID_CHANNELS = Arrays.stream(ToolAlertChannel.values())
            .map(Enum::name).collect(Collectors.toSet());

    private final ToolAlertRuleMapper ruleMapper;
    private final ProjectPermissionService projectPermissionService;

    public ToolAlertRuleService(ToolAlertRuleMapper ruleMapper,
                                ProjectPermissionService projectPermissionService) {
        this.ruleMapper = ruleMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ToolAlertRuleResponse createRule(CreateToolAlertRuleRequest request) {
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "规则名称不能为空");
        }
        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "sourceType 不能为空");
        }
        if (request.getChannel() == null || !VALID_CHANNELS.contains(request.getChannel())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 channel: " + request.getChannel());
        }

        ToolAlertRuleEntity entity = new ToolAlertRuleEntity();
        entity.setProjectId(projectId);
        entity.setName(request.getName().trim());
        entity.setEnabled(true);
        entity.setSourceType(request.getSourceType());
        entity.setMinSeverity(request.getMinSeverity());
        entity.setChannel(request.getChannel());
        entity.setRouteTarget(request.getRouteTarget());
        entity.setConfigJson(request.getConfigJson());

        ruleMapper.insert(entity);
        log.info("Created alert rule: id={}, name={}, projectId={}", entity.getId(), entity.getName(), projectId);

        return toResponse(entity);
    }

    @Transactional
    public ToolAlertRuleResponse updateRule(Long ruleId, UpdateToolAlertRuleRequest request) {
        ToolAlertRuleEntity entity = ruleMapper.selectById(ruleId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Alert rule 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.MAINTAINER, ProjectRole.OWNER);

        boolean changed = false;

        if (request.getName() != null) {
            entity.setName(request.getName().trim());
            changed = true;
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
            changed = true;
        }
        if (request.getSourceType() != null) {
            entity.setSourceType(request.getSourceType());
            changed = true;
        }
        if (request.getMinSeverity() != null) {
            entity.setMinSeverity(request.getMinSeverity());
            changed = true;
        }
        if (request.getChannel() != null) {
            if (!VALID_CHANNELS.contains(request.getChannel())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "无效的 channel: " + request.getChannel());
            }
            entity.setChannel(request.getChannel());
            changed = true;
        }
        if (request.getRouteTarget() != null) {
            entity.setRouteTarget(request.getRouteTarget());
            changed = true;
        }
        if (request.getConfigJson() != null) {
            entity.setConfigJson(request.getConfigJson());
            changed = true;
        }

        if (changed) {
            ruleMapper.updateById(entity);
        }

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ToolAlertRuleResponse> listProjectRules(Long projectId) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        List<ToolAlertRuleEntity> entities = ruleMapper.selectList(
                new LambdaQueryWrapper<ToolAlertRuleEntity>()
                        .eq(ToolAlertRuleEntity::getProjectId, projectId)
                        .orderByDesc(ToolAlertRuleEntity::getCreateTime));

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolAlertRuleEntity> findMatchingRules(ToolIncidentEntity incident) {
        List<ToolAlertRuleEntity> allRules = ruleMapper.selectList(
                new LambdaQueryWrapper<ToolAlertRuleEntity>()
                        .eq(ToolAlertRuleEntity::getProjectId, incident.getProjectId())
                        .eq(ToolAlertRuleEntity::getEnabled, true));

        return allRules.stream()
                .filter(rule -> matchesSourceType(rule, incident.getSourceType()))
                .filter(rule -> matchesSeverity(rule, incident.getSeverity()))
                .collect(Collectors.toList());
    }

    private boolean matchesSourceType(ToolAlertRuleEntity rule, String incidentSourceType) {
        if (rule.getSourceType() == null || rule.getSourceType().isBlank()) {
            return true;
        }
        return rule.getSourceType().equals(incidentSourceType);
    }

    private boolean matchesSeverity(ToolAlertRuleEntity rule, String incidentSeverity) {
        if (rule.getMinSeverity() == null || rule.getMinSeverity().isBlank()) {
            return true;
        }
        return severityValue(incidentSeverity) >= severityValue(rule.getMinSeverity());
    }

    private int severityValue(String severity) {
        if (severity == null) return 0;
        switch (severity) {
            case "CRITICAL": return 5;
            case "HIGH": return 4;
            case "MEDIUM": return 3;
            case "LOW": return 2;
            case "INFO": return 1;
            default: return 0;
        }
    }

    private ToolAlertRuleResponse toResponse(ToolAlertRuleEntity entity) {
        ToolAlertRuleResponse resp = new ToolAlertRuleResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setName(entity.getName());
        resp.setEnabled(entity.getEnabled());
        resp.setSourceType(entity.getSourceType());
        resp.setMinSeverity(entity.getMinSeverity());
        resp.setChannel(entity.getChannel());
        resp.setRouteTarget(entity.getRouteTarget());
        resp.setConfigJson(entity.getConfigJson());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String field) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.VALIDATION_ERROR, field + " 格式无效"); }
    }
}
