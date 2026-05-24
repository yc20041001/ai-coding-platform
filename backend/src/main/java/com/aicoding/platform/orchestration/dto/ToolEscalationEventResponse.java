package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolEscalationEventResponse {

    private String id;
    private String incidentId;
    private String projectId;
    private String policyId;
    private Integer escalationLevel;
    private String severity;
    private String channel;
    private String routeTarget;
    private String status;
    private String reason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ToolEscalationEventResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRouteTarget() { return routeTarget; }
    public void setRouteTarget(String routeTarget) { this.routeTarget = routeTarget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
