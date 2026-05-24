package com.aicoding.platform.orchestration.dto;

public class CreateKnowledgeQualityReviewRequest {

    private Long knowledgeDocumentId;
    private Long retrospectiveId;
    private Integer completenessScore;
    private Integer accuracyScore;
    private Integer actionabilityScore;
    private Integer relevanceScore;
    private String checklistJson;
    private String reviewComment;

    public CreateKnowledgeQualityReviewRequest() {}

    public Long getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }

    public Long getRetrospectiveId() { return retrospectiveId; }
    public void setRetrospectiveId(Long retrospectiveId) { this.retrospectiveId = retrospectiveId; }

    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }

    public Integer getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Integer accuracyScore) { this.accuracyScore = accuracyScore; }

    public Integer getActionabilityScore() { return actionabilityScore; }
    public void setActionabilityScore(Integer actionabilityScore) { this.actionabilityScore = actionabilityScore; }

    public Integer getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Integer relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
