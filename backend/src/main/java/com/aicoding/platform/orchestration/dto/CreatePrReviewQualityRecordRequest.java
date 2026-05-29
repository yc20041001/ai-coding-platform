package com.aicoding.platform.orchestration.dto;

public class CreatePrReviewQualityRecordRequest {

    private String reviewJobId;
    private Integer usefulnessScore;
    private Integer falsePositiveScore;
    private String reviewComment;

    public String getReviewJobId() {
        return reviewJobId;
    }

    public void setReviewJobId(String reviewJobId) {
        this.reviewJobId = reviewJobId;
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
