package com.aicoding.platform.agent.dto;

public class ProjectAgentConfigResponse {

    private String projectId;
    private String agentId;
    private String agentName;
    private String agentCode;
    private String agentType;
    private String agentStatus;
    private String agentDescription;
    private Boolean enabled;
    private String projectAgentConfigId;
    private String agentVersionId;
    private String agentVersionNo;
    private String modelConfigId;
    private String modelProvider;
    private String modelName;
    private String configJson;
    private ProjectAgentRuntimeConfig config;
    private String updateTime;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getAgentStatus() { return agentStatus; }
    public void setAgentStatus(String agentStatus) { this.agentStatus = agentStatus; }

    public String getAgentDescription() { return agentDescription; }
    public void setAgentDescription(String agentDescription) { this.agentDescription = agentDescription; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getProjectAgentConfigId() { return projectAgentConfigId; }
    public void setProjectAgentConfigId(String projectAgentConfigId) { this.projectAgentConfigId = projectAgentConfigId; }

    public String getAgentVersionId() { return agentVersionId; }
    public void setAgentVersionId(String agentVersionId) { this.agentVersionId = agentVersionId; }

    public String getAgentVersionNo() { return agentVersionNo; }
    public void setAgentVersionNo(String agentVersionNo) { this.agentVersionNo = agentVersionNo; }

    public String getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public ProjectAgentRuntimeConfig getConfig() { return config; }
    public void setConfig(ProjectAgentRuntimeConfig config) { this.config = config; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
