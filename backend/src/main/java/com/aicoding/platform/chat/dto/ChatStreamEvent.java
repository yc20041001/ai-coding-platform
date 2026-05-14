package com.aicoding.platform.chat.dto;

import java.util.List;

public class ChatStreamEvent {

    private String messageId;
    private String content;
    private String status;
    private Long tokenUsage;
    private String code;
    private String message;
    private Boolean ragUsed;
    private List<ChatMessageReferenceResponse> references;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(Long tokenUsage) { this.tokenUsage = tokenUsage; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getRagUsed() { return ragUsed; }
    public void setRagUsed(Boolean ragUsed) { this.ragUsed = ragUsed; }

    public List<ChatMessageReferenceResponse> getReferences() { return references; }
    public void setReferences(List<ChatMessageReferenceResponse> references) { this.references = references; }
}
