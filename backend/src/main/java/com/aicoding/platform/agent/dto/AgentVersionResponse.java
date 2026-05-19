package com.aicoding.platform.agent.dto;

import java.time.LocalDateTime;

public class AgentVersionResponse {

    private String id;
    private String agentId;
    private String versionNo;
    private String status;
    private String systemPrompt;
    private String toolPolicy;
    private String executionPolicy;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getToolPolicy() { return toolPolicy; }
    public void setToolPolicy(String toolPolicy) { this.toolPolicy = toolPolicy; }

    public String getExecutionPolicy() { return executionPolicy; }
    public void setExecutionPolicy(String executionPolicy) { this.executionPolicy = executionPolicy; }

    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
