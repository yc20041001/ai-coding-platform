package com.aicoding.platform.chat.dto;

import java.util.List;

public class ChatMessageResponse {

    private String id;
    private String sessionId;
    private String senderType;
    private String senderId;
    private String senderName;
    private String agentId;
    private String taskId;
    private String messageType;
    private String content;
    private String status;
    private Long tokenUsage;
    private List<ChatMessageReferenceResponse> references;
    private String createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(Long tokenUsage) { this.tokenUsage = tokenUsage; }

    public List<ChatMessageReferenceResponse> getReferences() { return references; }
    public void setReferences(List<ChatMessageReferenceResponse> references) { this.references = references; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
