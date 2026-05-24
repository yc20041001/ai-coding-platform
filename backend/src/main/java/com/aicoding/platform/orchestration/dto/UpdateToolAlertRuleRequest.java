package com.aicoding.platform.orchestration.dto;

public class UpdateToolAlertRuleRequest {

    private String name;
    private Boolean enabled;
    private String sourceType;
    private String minSeverity;
    private String channel;
    private String routeTarget;
    private String configJson;

    public UpdateToolAlertRuleRequest() {}

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
}
