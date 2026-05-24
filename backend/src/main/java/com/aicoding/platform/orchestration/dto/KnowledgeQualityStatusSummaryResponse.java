package com.aicoding.platform.orchestration.dto;

public class KnowledgeQualityStatusSummaryResponse {

    private Long totalReviews;
    private Long approvedCount;
    private Long needsWorkCount;
    private Long rejectedCount;
    private Long pendingCount;
    private Long inReviewCount;
    private Double averageCompletenessScore;
    private Double averageAccuracyScore;
    private Double averageActionabilityScore;
    private Double averageRelevanceScore;
    private Double overallAverageScore;

    public KnowledgeQualityStatusSummaryResponse() {}

    public Long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }

    public Long getApprovedCount() { return approvedCount; }
    public void setApprovedCount(Long approvedCount) { this.approvedCount = approvedCount; }

    public Long getNeedsWorkCount() { return needsWorkCount; }
    public void setNeedsWorkCount(Long needsWorkCount) { this.needsWorkCount = needsWorkCount; }

    public Long getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(Long rejectedCount) { this.rejectedCount = rejectedCount; }

    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }

    public Long getInReviewCount() { return inReviewCount; }
    public void setInReviewCount(Long inReviewCount) { this.inReviewCount = inReviewCount; }

    public Double getAverageCompletenessScore() { return averageCompletenessScore; }
    public void setAverageCompletenessScore(Double averageCompletenessScore) { this.averageCompletenessScore = averageCompletenessScore; }

    public Double getAverageAccuracyScore() { return averageAccuracyScore; }
    public void setAverageAccuracyScore(Double averageAccuracyScore) { this.averageAccuracyScore = averageAccuracyScore; }

    public Double getAverageActionabilityScore() { return averageActionabilityScore; }
    public void setAverageActionabilityScore(Double averageActionabilityScore) { this.averageActionabilityScore = averageActionabilityScore; }

    public Double getAverageRelevanceScore() { return averageRelevanceScore; }
    public void setAverageRelevanceScore(Double averageRelevanceScore) { this.averageRelevanceScore = averageRelevanceScore; }

    public Double getOverallAverageScore() { return overallAverageScore; }
    public void setOverallAverageScore(Double overallAverageScore) { this.overallAverageScore = overallAverageScore; }
}
