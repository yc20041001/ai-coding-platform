package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolAlertRuleResponse {

    private String id;
    private String projectId;
    private String name;
    private Boolean enabled;
    private String sourceType;
    private String minSeverity;
    private String channel;
    private String routeTarget;
    private String configJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ToolAlertRuleResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getMinSeverity() { return minSeverity; }
    public void setMinSeverity(String minSeverity) { this.minSeverity = minSeverity; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRouteTarget() { return routeTarget; }
    public void setRouteTarget(String routeTarget) { this.routeTarget = routeTarget; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
