package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolEscalationPolicyResponse {

    private String id;
    private String projectId;
    private String name;
    private Boolean enabled;
    private String severity;
    private Integer slaMinutes;
    private Integer escalationAfterMinutes;
    private Integer maxEscalationLevel;
    private String channel;
    private String routeTarget;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ToolEscalationPolicyResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Integer getEscalationAfterMinutes() { return escalationAfterMinutes; }
    public void setEscalationAfterMinutes(Integer escalationAfterMinutes) { this.escalationAfterMinutes = escalationAfterMinutes; }

    public Integer getMaxEscalationLevel() { return maxEscalationLevel; }
    public void setMaxEscalationLevel(Integer maxEscalationLevel) { this.maxEscalationLevel = maxEscalationLevel; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRouteTarget() { return routeTarget; }
    public void setRouteTarget(String routeTarget) { this.routeTarget = routeTarget; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
