package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class IncidentKnowledgeDocumentDraftResponse {

    private String documentId;
    private String title;
    private String status;
    private String knowledgeBaseId;
    private String knowledgeBaseName;
    private LocalDateTime createTime;

    public IncidentKnowledgeDocumentDraftResponse() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getKnowledgeBaseName() { return knowledgeBaseName; }
    public void setKnowledgeBaseName(String knowledgeBaseName) { this.knowledgeBaseName = knowledgeBaseName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
