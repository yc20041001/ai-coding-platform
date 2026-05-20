package com.aicoding.platform.orchestration.dto;

public class MultiAgentMessageResponse {

    private String id;
    private String runId;
    private String fromStepId;
    private String toStepId;
    private String fromAgentId;
    private String toAgentId;
    private String messageType;
    private String content;
    private String summary;
    private String createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getFromStepId() { return fromStepId; }
    public void setFromStepId(String fromStepId) { this.fromStepId = fromStepId; }

    public String getToStepId() { return toStepId; }
    public void setToStepId(String toStepId) { this.toStepId = toStepId; }

    public String getFromAgentId() { return fromAgentId; }
    public void setFromAgentId(String fromAgentId) { this.fromAgentId = fromAgentId; }

    public String getToAgentId() { return toAgentId; }
    public void setToAgentId(String toAgentId) { this.toAgentId = toAgentId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
