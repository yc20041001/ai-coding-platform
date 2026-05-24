package com.aicoding.platform.orchestration.dto;

public class GenerateIncidentKnowledgeDocumentRequest {

    private String knowledgeBaseId;
    private String title;
    private Boolean includeTimeline;
    private Boolean includeTraceSummary;
    private Boolean includeOperatorReview;
    private Boolean includeEscalation;

    public GenerateIncidentKnowledgeDocumentRequest() {}

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getIncludeTimeline() { return includeTimeline; }
    public void setIncludeTimeline(Boolean includeTimeline) { this.includeTimeline = includeTimeline; }

    public Boolean getIncludeTraceSummary() { return includeTraceSummary; }
    public void setIncludeTraceSummary(Boolean includeTraceSummary) { this.includeTraceSummary = includeTraceSummary; }

    public Boolean getIncludeOperatorReview() { return includeOperatorReview; }
    public void setIncludeOperatorReview(Boolean includeOperatorReview) { this.includeOperatorReview = includeOperatorReview; }

    public Boolean getIncludeEscalation() { return includeEscalation; }
    public void setIncludeEscalation(Boolean includeEscalation) { this.includeEscalation = includeEscalation; }
}
