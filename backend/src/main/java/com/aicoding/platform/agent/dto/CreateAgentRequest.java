package com.aicoding.platform.agent.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAgentRequest {

    @NotBlank
    private String name;
    @NotBlank
    private String code;
    @NotBlank
    private String type;
    private String description;
    private String systemPrompt;
    private String modelConfigId;
    private String toolPolicy;
    private String executionPolicy;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

    public String getToolPolicy() { return toolPolicy; }
    public void setToolPolicy(String toolPolicy) { this.toolPolicy = toolPolicy; }

    public String getExecutionPolicy() { return executionPolicy; }
    public void setExecutionPolicy(String executionPolicy) { this.executionPolicy = executionPolicy; }
}
