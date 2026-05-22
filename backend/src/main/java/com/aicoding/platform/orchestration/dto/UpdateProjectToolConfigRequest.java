package com.aicoding.platform.orchestration.dto;

import java.util.Map;

public class UpdateProjectToolConfigRequest {

    private Boolean enabled;
    private Map<String, Object> config;
    private Map<String, Object> parameters;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
