package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSeverity;
import com.aicoding.platform.orchestration.domain.ToolIncidentSlaStatus;
import com.aicoding.platform.orchestration.dto.ToolIncidentSlaScanResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ToolIncidentSlaService {

    private static final Logger log = LoggerFactory.getLogger(ToolIncidentSlaService.class);

    private static final Map<String, Integer> DEFAULT_SLA_MINUTES = Map.of(
            ToolIncidentSeverity.CRITICAL.name(), 30,
            ToolIncidentSeverity.HIGH.name(), 120,
            ToolIncidentSeverity.MEDIUM.name(), 480,
            ToolIncidentSeverity.LOW.name(), 1440);

    private static final double AT_RISK_THRESHOLD = 0.8;

    private final ToolIncidentMapper incidentMapper;

    public ToolIncidentSlaService(ToolIncidentMapper incidentMapper) {
        this.incidentMapper = incidentMapper;
    }

    public void initializeSla(ToolIncidentEntity entity) {
        if (entity.getSeverity() == null) return;

        if (ToolIncidentSeverity.INFO.name().equals(entity.getSeverity())) {
            entity.setSlaMinutes(null);
            entity.setDueAt(null);
            entity.setSlaStatus(ToolIncidentSlaStatus.WAIVED.name());
            return;
        }

        Integer slaMinutes = DEFAULT_SLA_MINUTES.get(entity.getSeverity());
        if (slaMinutes == null) {
            entity.setSlaMinutes(null);
            entity.setDueAt(null);
            entity.setSlaStatus(ToolIncidentSlaStatus.WAIVED.name());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setSlaMinutes(slaMinutes);
        entity.setDueAt(now.plusMinutes(slaMinutes));
        entity.setSlaStatus(ToolIncidentSlaStatus.WITHIN_SLA.name());
        entity.setBreachedAt(null);
    }

    public void refreshSla(ToolIncidentEntity entity) {
        if (entity.getSlaStatus() == null) {
            entity.setSlaStatus(ToolIncidentSlaStatus.NOT_STARTED.name());
            return;
        }

        String slaStatus = entity.getSlaStatus();
        if (ToolIncidentSlaStatus.RESOLVED.name().equals(slaStatus)
                || ToolIncidentSlaStatus.WAIVED.name().equals(slaStatus)) {
            return;
        }

        if (entity.getDueAt() == null) {
            if (ToolIncidentSeverity.INFO.name().equals(entity.getSeverity())) {
                entity.setSlaStatus(ToolIncidentSlaStatus.WAIVED.name());
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(entity.getDueAt())) {
            if (!ToolIncidentSlaStatus.BREACHED.name().equals(slaStatus)) {
                entity.setSlaStatus(ToolIncidentSlaStatus.BREACHED.name());
                entity.setBreachedAt(now);
                log.info("SLA breached: incidentId={}, dueAt={}", entity.getId(), entity.getDueAt());
            }
            return;
        }

        if (ToolIncidentSlaStatus.BREACHED.name().equals(slaStatus)) {
            return;
        }

        long elapsed = java.time.Duration.between(entity.getCreateTime(), now).toMillis();
        long total = entity.getSlaMinutes() * 60L * 1000L;
        if (total > 0 && (double) elapsed / total >= AT_RISK_THRESHOLD) {
            if (!ToolIncidentSlaStatus.AT_RISK.name().equals(slaStatus)) {
                entity.setSlaStatus(ToolIncidentSlaStatus.AT_RISK.name());
                log.info("SLA at risk: incidentId={}, dueAt={}", entity.getId(), entity.getDueAt());
            }
        } else {
            entity.setSlaStatus(ToolIncidentSlaStatus.WITHIN_SLA.name());
        }
    }

    public ToolIncidentSlaScanResponse scanProjectSla(Long projectId) {
        List<ToolIncidentEntity> openIncidents = incidentMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, projectId)
                        .notIn(ToolIncidentEntity::getSlaStatus,
                                ToolIncidentSlaStatus.RESOLVED.name(),
                                ToolIncidentSlaStatus.WAIVED.name(),
                                ToolIncidentSlaStatus.NOT_STARTED.name()));

        int scanned = 0, withinSla = 0, atRisk = 0, breached = 0, resolved = 0;

        for (ToolIncidentEntity entity : openIncidents) {
            String prevSlaStatus = entity.getSlaStatus();
            refreshSla(entity);

            if (!prevSlaStatus.equals(entity.getSlaStatus())) {
                incidentMapper.updateById(entity);
            }

            scanned++;
            switch (entity.getSlaStatus()) {
                case "WITHIN_SLA" -> withinSla++;
                case "AT_RISK" -> atRisk++;
                case "BREACHED" -> breached++;
                case "RESOLVED" -> resolved++;
            }
        }

        log.info("SLA scan complete: projectId={}, scanned={}, withinSla={}, atRisk={}, breached={}, resolved={}",
                projectId, scanned, withinSla, atRisk, breached, resolved);

        ToolIncidentSlaScanResponse response = new ToolIncidentSlaScanResponse();
        response.setScanned(scanned);
        response.setWithinSla(withinSla);
        response.setAtRisk(atRisk);
        response.setBreached(breached);
        response.setResolved(resolved);
        return response;
    }
}
