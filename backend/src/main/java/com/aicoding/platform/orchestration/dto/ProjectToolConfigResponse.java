package com.aicoding.platform.orchestration.dto;

public class ProjectToolConfigResponse {

    private String id;
    private String projectId;
    private String toolId;
    private String toolKey;
    private String name;
    private String description;
    private String toolType;
    private String riskLevel;
    private String executionMode;
    private Boolean globalEnabled;
    private Boolean projectEnabled;
    private String configJson;
    private String parameterSchemaJson;
    private String parametersJson;
    private String createTime;
    private String updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getToolId() { return toolId; }
    public void setToolId(String toolId) { this.toolId = toolId; }

    public String getToolKey() { return toolKey; }
    public void setToolKey(String toolKey) { this.toolKey = toolKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getToolType() { return toolType; }
    public void setToolType(String toolType) { this.toolType = toolType; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

    public Boolean getGlobalEnabled() { return globalEnabled; }
    public void setGlobalEnabled(Boolean globalEnabled) { this.globalEnabled = globalEnabled; }

    public Boolean getProjectEnabled() { return projectEnabled; }
    public void setProjectEnabled(Boolean projectEnabled) { this.projectEnabled = projectEnabled; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getParameterSchemaJson() { return parameterSchemaJson; }
    public void setParameterSchemaJson(String parameterSchemaJson) { this.parameterSchemaJson = parameterSchemaJson; }

    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
