package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class KnowledgeQualityReviewResponse {

    private String id;
    private String projectId;
    private String incidentId;
    private String knowledgeDocumentId;
    private String retrospectiveId;
    private Integer completenessScore;
    private Integer accuracyScore;
    private Integer actionabilityScore;
    private Integer relevanceScore;
    private Double averageScore;
    private String reviewStatus;
    private String overallStatus;
    private String checklistJson;
    private String reviewComment;
    private String reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public KnowledgeQualityReviewResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(String knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }

    public String getRetrospectiveId() { return retrospectiveId; }
    public void setRetrospectiveId(String retrospectiveId) { this.retrospectiveId = retrospectiveId; }

    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }

    public Integer getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Integer accuracyScore) { this.accuracyScore = accuracyScore; }

    public Integer getActionabilityScore() { return actionabilityScore; }
    public void setActionabilityScore(Integer actionabilityScore) { this.actionabilityScore = actionabilityScore; }

    public Integer getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Integer relevanceScore) { this.relevanceScore = relevanceScore; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
