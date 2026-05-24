package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class IncidentKnowledgeLinkResponse {

    private String id;
    private String projectId;
    private String incidentId;
    private String rootCauseNoteId;
    private String knowledgeBaseId;
    private String knowledgeDocumentId;
    private String linkType;
    private String title;
    private LocalDateTime createTime;

    public IncidentKnowledgeLinkResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getRootCauseNoteId() { return rootCauseNoteId; }
    public void setRootCauseNoteId(String rootCauseNoteId) { this.rootCauseNoteId = rootCauseNoteId; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(String knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }

    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
