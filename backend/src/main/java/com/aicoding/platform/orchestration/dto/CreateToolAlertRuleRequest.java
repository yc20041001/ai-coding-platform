package com.aicoding.platform.orchestration.dto;

public class CreateToolAlertRuleRequest {

    private String projectId;
    private String name;
    private String sourceType;
    private String minSeverity;
    private String channel;
    private String routeTarget;
    private String configJson;

    public CreateToolAlertRuleRequest() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
}
