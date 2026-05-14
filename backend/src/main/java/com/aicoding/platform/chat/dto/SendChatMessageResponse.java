package com.aicoding.platform.chat.dto;

import java.util.List;

public class SendChatMessageResponse {

    private String userMessageId;
    private String assistantMessageId;
    private String streamUrl;
    private Boolean ragUsed;
    private List<ChatMessageReferenceResponse> references;

    public String getUserMessageId() { return userMessageId; }
    public void setUserMessageId(String userMessageId) { this.userMessageId = userMessageId; }

    public String getAssistantMessageId() { return assistantMessageId; }
    public void setAssistantMessageId(String assistantMessageId) { this.assistantMessageId = assistantMessageId; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public Boolean getRagUsed() { return ragUsed; }
    public void setRagUsed(Boolean ragUsed) { this.ragUsed = ragUsed; }

    public List<ChatMessageReferenceResponse> getReferences() { return references; }
    public void setReferences(List<ChatMessageReferenceResponse> references) { this.references = references; }
}
