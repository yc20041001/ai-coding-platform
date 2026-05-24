package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ToolAlertDeliveryEntity;
import com.aicoding.platform.orchestration.domain.ToolAlertDeliveryStatus;
import com.aicoding.platform.orchestration.domain.ToolAlertRuleEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.dto.ToolAlertDeliveryResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolAlertDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolAlertDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ToolAlertDeliveryService.class);

    private final ToolAlertDeliveryMapper deliveryMapper;
    private final ToolAlertRuleService alertRuleService;
    private final ProjectPermissionService projectPermissionService;

    public ToolAlertDeliveryService(ToolAlertDeliveryMapper deliveryMapper,
                                    ToolAlertRuleService alertRuleService,
                                    ProjectPermissionService projectPermissionService) {
        this.deliveryMapper = deliveryMapper;
        this.alertRuleService = alertRuleService;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public void routeIncident(ToolIncidentEntity incident) {
        List<ToolAlertRuleEntity> rules = alertRuleService.findMatchingRules(incident);

        if (rules.isEmpty()) {
            log.info("No matching alert rules for incident {} (projectId={})", incident.getId(), incident.getProjectId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (ToolAlertRuleEntity rule : rules) {
            ToolAlertDeliveryEntity delivery = new ToolAlertDeliveryEntity();
            delivery.setIncidentId(incident.getId());
            delivery.setProjectId(incident.getProjectId());
            delivery.setRuleId(rule.getId());
            delivery.setChannel(rule.getChannel());
            delivery.setRouteTarget(rule.getRouteTarget());

            // Mock payload
            String payload = String.format(
                    "{\"incidentId\":\"%s\",\"title\":\"%s\",\"severity\":\"%s\",\"sourceType\":\"%s\"}",
                    incident.getId(), incident.getTitle(), incident.getSeverity(), incident.getSourceType());
            delivery.setPayload(payload);

            // Mock delivery: mark as delivered for IN_APP, pending otherwise
            if ("IN_APP".equals(rule.getChannel())) {
                delivery.setStatus(ToolAlertDeliveryStatus.DELIVERED.name());
                delivery.setDeliveredAt(now);
            } else {
                delivery.setStatus(ToolAlertDeliveryStatus.PENDING.name());
            }

            delivery.setErrorMessage(null);
            deliveryMapper.insert(delivery);

            log.info("Alert delivery created: id={}, incidentId={}, ruleId={}, channel={}",
                    delivery.getId(), incident.getId(), rule.getId(), rule.getChannel());
        }
    }

    @Transactional(readOnly = true)
    public List<ToolAlertDeliveryResponse> listProjectDeliveries(Long projectId) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        List<ToolAlertDeliveryEntity> entities = deliveryMapper.selectList(
                new LambdaQueryWrapper<ToolAlertDeliveryEntity>()
                        .eq(ToolAlertDeliveryEntity::getProjectId, projectId)
                        .orderByDesc(ToolAlertDeliveryEntity::getCreateTime));

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolAlertDeliveryResponse> listIncidentDeliveries(Long incidentId) {
        List<ToolAlertDeliveryEntity> entities = deliveryMapper.selectList(
                new LambdaQueryWrapper<ToolAlertDeliveryEntity>()
                        .eq(ToolAlertDeliveryEntity::getIncidentId, incidentId)
                        .orderByDesc(ToolAlertDeliveryEntity::getCreateTime));

        if (!entities.isEmpty()) {
            projectPermissionService.checkProjectRole(entities.get(0).getProjectId(),
                    ProjectRole.VIEWER, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);
        }

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ToolAlertDeliveryResponse retryDelivery(Long deliveryId) {
        ToolAlertDeliveryEntity entity = deliveryMapper.selectById(deliveryId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Alert delivery 不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.MAINTAINER, ProjectRole.OWNER);

        // Mock retry: mark as delivered
        entity.setStatus(ToolAlertDeliveryStatus.DELIVERED.name());
        entity.setErrorMessage(null);
        entity.setDeliveredAt(LocalDateTime.now());
        deliveryMapper.updateById(entity);

        log.info("Retried alert delivery: id={}, incidentId={}", entity.getId(), entity.getIncidentId());

        return toResponse(entity);
    }

    private ToolAlertDeliveryResponse toResponse(ToolAlertDeliveryEntity entity) {
        ToolAlertDeliveryResponse resp = new ToolAlertDeliveryResponse();
        resp.setId(entity.getId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setRuleId(entity.getRuleId().toString());
        resp.setChannel(entity.getChannel());
        resp.setRouteTarget(entity.getRouteTarget());
        resp.setStatus(entity.getStatus());
        resp.setPayload(entity.getPayload());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setDeliveredAt(entity.getDeliveredAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
