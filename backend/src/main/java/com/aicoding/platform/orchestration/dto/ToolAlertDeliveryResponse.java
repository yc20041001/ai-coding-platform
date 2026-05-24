package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolAlertDeliveryResponse {

    private String id;
    private String incidentId;
    private String projectId;
    private String ruleId;
    private String channel;
    private String routeTarget;
    private String status;
    private String payload;
    private String errorMessage;
    private LocalDateTime deliveredAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ToolAlertDeliveryResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRouteTarget() { return routeTarget; }
    public void setRouteTarget(String routeTarget) { this.routeTarget = routeTarget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
