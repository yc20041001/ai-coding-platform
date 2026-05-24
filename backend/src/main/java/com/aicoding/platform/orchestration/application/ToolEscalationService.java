package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.ToolEscalationEventEntity;
import com.aicoding.platform.orchestration.domain.ToolEscalationEventStatus;
import com.aicoding.platform.orchestration.domain.ToolEscalationPolicyEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSlaStatus;
import com.aicoding.platform.orchestration.dto.EscalateIncidentRequest;
import com.aicoding.platform.orchestration.dto.ToolEscalationEventResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentEscalationScanResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolEscalationEventMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolEscalationService {

    private static final Logger log = LoggerFactory.getLogger(ToolEscalationService.class);

    private final ToolIncidentMapper incidentMapper;
    private final ToolEscalationEventMapper escalationEventMapper;
    private final ToolEscalationPolicyService escalationPolicyService;

    public ToolEscalationService(ToolIncidentMapper incidentMapper,
                                 ToolEscalationEventMapper escalationEventMapper,
                                 ToolEscalationPolicyService escalationPolicyService) {
        this.incidentMapper = incidentMapper;
        this.escalationEventMapper = escalationEventMapper;
        this.escalationPolicyService = escalationPolicyService;
    }

    @Transactional
    public ToolIncidentEscalationScanResponse scanEscalation(Long projectId) {
        List<ToolIncidentEntity> breachedIncidents = incidentMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .eq(ToolIncidentEntity::getSlaStatus, ToolIncidentSlaStatus.BREACHED.name()));

        int scanned = 0, escalated = 0, skipped = 0, maxLevelReached = 0;

        for (ToolIncidentEntity incident : breachedIncidents) {
            scanned++;

            ToolEscalationPolicyEntity policy = escalationPolicyService.findMatchingPolicy(
                    projectId, incident.getSeverity());

            if (policy == null) {
                skipped++;
                continue;
            }

            int currentLevel = incident.getEscalationLevel() != null ? incident.getEscalationLevel() : 0;
            if (currentLevel >= policy.getMaxEscalationLevel()) {
                maxLevelReached++;
                continue;
            }

            int nextLevel = currentLevel + 1;

            if (isEventAlreadyCreated(incident.getId(), nextLevel)) {
                skipped++;
                continue;
            }

            if (policy.getEscalationAfterMinutes() != null && incident.getBreachedAt() != null) {
                LocalDateTime eligibleAt = incident.getBreachedAt().plusMinutes(policy.getEscalationAfterMinutes());
                if (LocalDateTime.now().isBefore(eligibleAt)) {
                    skipped++;
                    continue;
                }
            }

            createEscalationEvent(incident, policy, nextLevel, null);
            incident.setEscalationLevel(nextLevel);
            incidentMapper.updateById(incident);
            escalated++;
        }

        log.info("Escalation scan complete: projectId={}, scanned={}, escalated={}, skipped={}, maxLevelReached={}",
                projectId, scanned, escalated, skipped, maxLevelReached);

        ToolIncidentEscalationScanResponse response = new ToolIncidentEscalationScanResponse();
        response.setScanned(scanned);
        response.setEscalated(escalated);
        response.setSkipped(skipped);
        response.setMaxLevelReached(maxLevelReached);
        return response;
    }

    @Transactional
    public ToolEscalationEventResponse escalateIncident(Long incidentId, EscalateIncidentRequest request) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Incident 不存在");
        }

        ToolEscalationPolicyEntity policy = escalationPolicyService.findMatchingPolicy(
                incident.getProjectId(), incident.getSeverity());

        if (policy == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "未找到匹配的升级策略");
        }

        int currentLevel = incident.getEscalationLevel() != null ? incident.getEscalationLevel() : 0;
        int nextLevel = currentLevel + 1;

        if (nextLevel > policy.getMaxEscalationLevel()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "已达到最大升级级别: " + policy.getMaxEscalationLevel());
        }

        if (isEventAlreadyCreated(incident.getId(), nextLevel)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "该升级级别的事件已存在");
        }

        String reason = request != null ? request.getReason() : null;
        createEscalationEvent(incident, policy, nextLevel, reason);
        incident.setEscalationLevel(nextLevel);
        incidentMapper.updateById(incident);

        log.info("Manual escalation: incidentId={}, level={}, reason={}", incidentId, nextLevel, reason);
        return toEventResponse(getLatestEvent(incident.getId(), nextLevel));
    }

    @Transactional(readOnly = true)
    public List<ToolEscalationEventResponse> listIncidentEscalationEvents(Long incidentId) {
        List<ToolEscalationEventEntity> events = escalationEventMapper.selectList(
                new LambdaQueryWrapper<ToolEscalationEventEntity>()
                        .eq(ToolEscalationEventEntity::getIncidentId, incidentId)
                        .orderByAsc(ToolEscalationEventEntity::getCreateTime));
        return events.stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolEscalationEventResponse getEscalationEvent(Long eventId) {
        ToolEscalationEventEntity entity = escalationEventMapper.selectById(eventId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Escalation event 不存在");
        }
        return toEventResponse(entity);
    }

    private boolean isEventAlreadyCreated(Long incidentId, Integer escalationLevel) {
        Long count = escalationEventMapper.selectCount(
                new LambdaQueryWrapper<ToolEscalationEventEntity>()
                        .eq(ToolEscalationEventEntity::getIncidentId, incidentId)
                        .eq(ToolEscalationEventEntity::getEscalationLevel, escalationLevel));
        return count != null && count > 0;
    }

    private void createEscalationEvent(ToolIncidentEntity incident, ToolEscalationPolicyEntity policy,
                                        Integer level, String reason) {
        ToolEscalationEventEntity event = new ToolEscalationEventEntity();
        event.setIncidentId(incident.getId());
        event.setProjectId(incident.getProjectId());
        event.setPolicyId(policy.getId());
        event.setEscalationLevel(level);
        event.setSeverity(incident.getSeverity());
        event.setChannel(policy.getChannel());
        event.setRouteTarget(policy.getRouteTarget());
        event.setStatus(ToolEscalationEventStatus.CREATED.name());
        event.setReason(reason);
        escalationEventMapper.insert(event);
        log.info("Created escalation event: incidentId={}, level={}, channel={}, target={}",
                incident.getId(), level, policy.getChannel(), policy.getRouteTarget());
    }

    private ToolEscalationEventEntity getLatestEvent(Long incidentId, Integer escalationLevel) {
        return escalationEventMapper.selectOne(
                new LambdaQueryWrapper<ToolEscalationEventEntity>()
                        .eq(ToolEscalationEventEntity::getIncidentId, incidentId)
                        .eq(ToolEscalationEventEntity::getEscalationLevel, escalationLevel)
                        .orderByDesc(ToolEscalationEventEntity::getCreateTime)
                        .last("LIMIT 1"));
    }

    private ToolEscalationEventResponse toEventResponse(ToolEscalationEventEntity entity) {
        ToolEscalationEventResponse resp = new ToolEscalationEventResponse();
        resp.setId(entity.getId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setPolicyId(entity.getPolicyId().toString());
        resp.setEscalationLevel(entity.getEscalationLevel());
        resp.setSeverity(entity.getSeverity());
        resp.setChannel(entity.getChannel());
        resp.setRouteTarget(entity.getRouteTarget());
        resp.setStatus(entity.getStatus());
        resp.setReason(entity.getReason());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
