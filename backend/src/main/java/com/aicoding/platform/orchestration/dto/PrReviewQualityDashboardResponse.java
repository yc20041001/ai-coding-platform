package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class PrReviewQualityDashboardResponse {

    private Long totalReviews;
    private Long highValueReviews;
    private Long actionableReviews;
    private Long lowSignalReviews;
    private Long failedReviews;
    private Long pendingFeedbackReviews;
    private Long adoptedReviews;
    private Double averageUsefulnessScore;
    private List<PrReviewQualityRecordResponse> recentReviews;

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Long getHighValueReviews() {
        return highValueReviews;
    }

    public void setHighValueReviews(Long highValueReviews) {
        this.highValueReviews = highValueReviews;
    }

    public Long getActionableReviews() {
        return actionableReviews;
    }

    public void setActionableReviews(Long actionableReviews) {
        this.actionableReviews = actionableReviews;
    }

    public Long getLowSignalReviews() {
        return lowSignalReviews;
    }

    public void setLowSignalReviews(Long lowSignalReviews) {
        this.lowSignalReviews = lowSignalReviews;
    }

    public Long getFailedReviews() {
        return failedReviews;
    }

    public void setFailedReviews(Long failedReviews) {
        this.failedReviews = failedReviews;
    }

    public Long getPendingFeedbackReviews() {
        return pendingFeedbackReviews;
    }

    public void setPendingFeedbackReviews(Long pendingFeedbackReviews) {
        this.pendingFeedbackReviews = pendingFeedbackReviews;
    }

    public Long getAdoptedReviews() {
        return adoptedReviews;
    }

    public void setAdoptedReviews(Long adoptedReviews) {
        this.adoptedReviews = adoptedReviews;
    }

    public Double getAverageUsefulnessScore() {
        return averageUsefulnessScore;
    }

    public void setAverageUsefulnessScore(Double averageUsefulnessScore) {
        this.averageUsefulnessScore = averageUsefulnessScore;
    }

    public List<PrReviewQualityRecordResponse> getRecentReviews() {
        return recentReviews;
    }

    public void setRecentReviews(List<PrReviewQualityRecordResponse> recentReviews) {
        this.recentReviews = recentReviews;
    }
}
