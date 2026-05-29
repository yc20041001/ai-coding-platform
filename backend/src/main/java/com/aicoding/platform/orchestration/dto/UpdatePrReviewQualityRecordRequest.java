package com.aicoding.platform.orchestration.dto;

public class UpdatePrReviewQualityRecordRequest {

    private String humanFeedbackStatus;
    private String adoptionStatus;
    private Integer usefulnessScore;
    private Integer falsePositiveScore;
    private String reviewComment;

    public String getHumanFeedbackStatus() {
        return humanFeedbackStatus;
    }

    public void setHumanFeedbackStatus(String humanFeedbackStatus) {
        this.humanFeedbackStatus = humanFeedbackStatus;
    }

    public String getAdoptionStatus() {
        return adoptionStatus;
    }

    public void setAdoptionStatus(String adoptionStatus) {
        this.adoptionStatus = adoptionStatus;
    }

    public Integer getUsefulnessScore() {
        return usefulnessScore;
    }

    public void setUsefulnessScore(Integer usefulnessScore) {
        this.usefulnessScore = usefulnessScore;
    }

    public Integer getFalsePositiveScore() {
        return falsePositiveScore;
    }

    public void setFalsePositiveScore(Integer falsePositiveScore) {
        this.falsePositiveScore = falsePositiveScore;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
