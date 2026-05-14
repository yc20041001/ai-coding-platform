package com.aicoding.platform.orchestrator.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aicoding.platform.rag.dto.RagReference;

public class AgentExecutionResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String chatSessionId;
    private String chatMessageId;
    private String agentId;
    private String agentName;
    private String executionType;
    private String status;
    private String inputPrompt;
    private String outputContent;
    private String errorMessage;
    private Long tokenUsage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
    private Boolean ragUsed;
    private List<RagReference> references;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getChatSessionId() { return chatSessionId; }
    public void setChatSessionId(String chatSessionId) { this.chatSessionId = chatSessionId; }

    public String getChatMessageId() { return chatMessageId; }
    public void setChatMessageId(String chatMessageId) { this.chatMessageId = chatMessageId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getExecutionType() { return executionType; }
    public void setExecutionType(String executionType) { this.executionType = executionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputPrompt() { return inputPrompt; }
    public void setInputPrompt(String inputPrompt) { this.inputPrompt = inputPrompt; }

    public String getOutputContent() { return outputContent; }
    public void setOutputContent(String outputContent) { this.outputContent = outputContent; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(Long tokenUsage) { this.tokenUsage = tokenUsage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Boolean getRagUsed() { return ragUsed; }
    public void setRagUsed(Boolean ragUsed) { this.ragUsed = ragUsed; }

    public List<RagReference> getReferences() { return references; }
    public void setReferences(List<RagReference> references) { this.references = references; }
}
