package com.aicoding.platform.agent.dto;

public class EnableProjectAgentRequest {

    private String agentVersionId;
    private String modelConfigId;
    private ProjectAgentRuntimeConfig config;

    public String getAgentVersionId() { return agentVersionId; }
    public void setAgentVersionId(String agentVersionId) { this.agentVersionId = agentVersionId; }

    public String getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

    public ProjectAgentRuntimeConfig getConfig() { return config; }
    public void setConfig(ProjectAgentRuntimeConfig config) { this.config = config; }
}
