package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ToolAlertDeliveryEntity;
import com.aicoding.platform.orchestration.domain.ToolEscalationEventEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewEntity;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.dto.ToolIncidentTimelineEventResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentTimelineResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolAlertDeliveryMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolEscalationEventMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolOperatorReviewMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolIncidentTimelineService {

    private final ToolIncidentMapper incidentMapper;
    private final ToolAlertDeliveryMapper alertDeliveryMapper;
    private final ToolEscalationEventMapper escalationEventMapper;
    private final ToolOperatorReviewMapper operatorReviewMapper;
    private final ToolSandboxExecutionMapper sandboxExecutionMapper;

    public ToolIncidentTimelineService(ToolIncidentMapper incidentMapper,
                                        ToolAlertDeliveryMapper alertDeliveryMapper,
                                        ToolEscalationEventMapper escalationEventMapper,
                                        ToolOperatorReviewMapper operatorReviewMapper,
                                        ToolSandboxExecutionMapper sandboxExecutionMapper) {
        this.incidentMapper = incidentMapper;
        this.alertDeliveryMapper = alertDeliveryMapper;
        this.escalationEventMapper = escalationEventMapper;
        this.operatorReviewMapper = operatorReviewMapper;
        this.sandboxExecutionMapper = sandboxExecutionMapper;
    }

    @Transactional(readOnly = true)
    public ToolIncidentTimelineResponse getIncidentTimeline(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            return emptyTimeline(incidentId);
        }

        List<ToolIncidentTimelineEventResponse> events = new ArrayList<>();

        // 1. Incident lifecycle events
        addIncidentLifecycleEvents(incident, events);

        // 2. Alert delivery events
        addAlertDeliveryEvents(incidentId, events);

        // 3. Escalation events
        addEscalationEvents(incidentId, events);

        // 4. Operator review events
        addOperatorReviewEvents(incident, events);

        // 5. Trace execution events
        addTraceExecutionEvents(incident, events);

        // Sort by eventTime ascending
        events.sort(Comparator.comparing(ToolIncidentTimelineEventResponse::getEventTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        ToolIncidentTimelineResponse response = new ToolIncidentTimelineResponse();
        response.setIncidentId(incidentId.toString());
        response.setEvents(events);
        return response;
    }

    private void addIncidentLifecycleEvents(ToolIncidentEntity incident, List<ToolIncidentTimelineEventResponse> events) {
        // Created
        events.add(createEvent("INCIDENT_CREATED", "Incident 创建",
                "严重级别: " + incident.getSeverity(), incident.getStatus(), incident.getCreateTime()));

        // Acknowledged
        if (incident.getAcknowledgedAt() != null) {
            events.add(createEvent("INCIDENT_ACKNOWLEDGED", "Incident 已确认",
                    "确认人: " + (incident.getAcknowledgedBy() != null ? incident.getAcknowledgedBy().toString() : "system"),
                    "ACKNOWLEDGED", incident.getAcknowledgedAt()));
        }

        // SLA breached
        if (incident.getBreachedAt() != null) {
            events.add(createEvent("INCIDENT_SLA_BREACHED", "SLA 超期",
                    "SLA " + (incident.getSlaMinutes() != null ? incident.getSlaMinutes() + "分钟" : "") + " 内未处理",
                    "BREACHED", incident.getBreachedAt()));
        }

        // Resolved
        if (incident.getResolvedAt() != null) {
            events.add(createEvent("INCIDENT_RESOLVED", "Incident 已解决",
                    incident.getResolution() != null ? incident.getResolution() : "已关闭",
                    incident.getStatus(), incident.getResolvedAt()));
        }
    }

    private void addAlertDeliveryEvents(Long incidentId, List<ToolIncidentTimelineEventResponse> events) {
        List<ToolAlertDeliveryEntity> deliveries = alertDeliveryMapper.selectList(
                new LambdaQueryWrapper<ToolAlertDeliveryEntity>()
                        .eq(ToolAlertDeliveryEntity::getIncidentId, incidentId));

        for (ToolAlertDeliveryEntity delivery : deliveries) {
            LocalDateTime eventTime = delivery.getDeliveredAt() != null
                    ? delivery.getDeliveredAt() : delivery.getCreateTime();
            events.add(createEvent("ALERT_DELIVERED", "告警推送 " + delivery.getChannel(),
                    "目标: " + delivery.getRouteTarget()
                            + (delivery.getErrorMessage() != null ? ", 错误: " + delivery.getErrorMessage() : ""),
                    delivery.getStatus(), eventTime));
        }
    }

    private void addEscalationEvents(Long incidentId, List<ToolIncidentTimelineEventResponse> events) {
        List<ToolEscalationEventEntity> escalations = escalationEventMapper.selectList(
                new LambdaQueryWrapper<ToolEscalationEventEntity>()
                        .eq(ToolEscalationEventEntity::getIncidentId, incidentId));

        for (ToolEscalationEventEntity esc : escalations) {
            String title = "升级 L" + esc.getEscalationLevel() + " (" + esc.getChannel() + ")";
            String desc = "目标: " + (esc.getRouteTarget() != null ? esc.getRouteTarget() : "-")
                    + (esc.getReason() != null ? ", 原因: " + esc.getReason() : "");
            events.add(createEvent("ESCALATION", title, desc, esc.getStatus(), esc.getCreateTime()));
        }
    }

    private void addOperatorReviewEvents(ToolIncidentEntity incident, List<ToolIncidentTimelineEventResponse> events) {
        if (incident.getOperatorReviewId() == null) return;

        ToolOperatorReviewEntity review = operatorReviewMapper.selectById(incident.getOperatorReviewId());
        if (review == null) return;

        events.add(createEvent("REVIEW_CREATED", "操作员审查",
                "标题: " + (review.getTitle() != null ? review.getTitle() : "-"),
                review.getStatus(), review.getCreateTime()));

        if (review.getResolvedAt() != null) {
            events.add(createEvent("REVIEW_RESOLVED", "审查已结束",
                    (review.getResolution() != null ? "结论: " + review.getResolution() : ""),
                    review.getStatus(), review.getResolvedAt()));
        }
    }

    private void addTraceExecutionEvents(ToolIncidentEntity incident, List<ToolIncidentTimelineEventResponse> events) {
        // Find executions linked via toolExecutionId or toolJobId
        List<ToolSandboxExecutionEntity> executions = new ArrayList<>();

        if (incident.getToolExecutionId() != null) {
            ToolSandboxExecutionEntity exec = sandboxExecutionMapper.selectById(incident.getToolExecutionId());
            if (exec != null) executions.add(exec);
        }
        if (incident.getToolJobId() != null) {
            List<ToolSandboxExecutionEntity> jobExecs = sandboxExecutionMapper.selectList(
                    new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                            .eq(ToolSandboxExecutionEntity::getId, incident.getToolJobId())
                            .or(w -> w.eq(ToolSandboxExecutionEntity::getRunId, incident.getToolJobId())));
            executions.addAll(jobExecs);
        }

        for (ToolSandboxExecutionEntity exec : executions) {
            String toolLabel = exec.getToolName() != null ? exec.getToolName() : "未知工具";
            if (exec.getStartedAt() != null) {
                events.add(createEvent("EXECUTION_STARTED", "工具执行: " + toolLabel,
                        "模式: " + (exec.getExecutionMode() != null ? exec.getExecutionMode() : "-"),
                        "RUNNING", exec.getStartedAt()));
            }
            if (exec.getFinishedAt() != null) {
                events.add(createEvent("EXECUTION_FINISHED", "工具执行完成: " + toolLabel,
                        "耗时: " + (exec.getDurationMs() != null ? exec.getDurationMs() + "ms" : "-")
                                + (exec.getErrorMessage() != null ? ", 错误: " + exec.getErrorMessage() : ""),
                        exec.getStatus(), exec.getFinishedAt()));
            }
        }
    }

    private ToolIncidentTimelineEventResponse createEvent(String eventType, String title,
                                                           String description, String status,
                                                           LocalDateTime eventTime) {
        ToolIncidentTimelineEventResponse event = new ToolIncidentTimelineEventResponse();
        event.setEventType(eventType);
        event.setTitle(title);
        event.setDescription(description);
        event.setStatus(status);
        event.setEventTime(eventTime);
        return event;
    }

    private ToolIncidentTimelineResponse emptyTimeline(Long incidentId) {
        ToolIncidentTimelineResponse response = new ToolIncidentTimelineResponse();
        response.setIncidentId(incidentId != null ? incidentId.toString() : null);
        response.setEvents(List.of());
        return response;
    }
}
