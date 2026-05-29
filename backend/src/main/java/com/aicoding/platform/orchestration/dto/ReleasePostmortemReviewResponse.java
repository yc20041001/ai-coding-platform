package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleasePostmortemReviewResponse {

    private String id;
    private String planId;
    private String projectId;
    private String releaseLabel;
    private String reviewStatus;
    private String overallOutcome;
    private String summary;
    private String whatWentWell;
    private String whatWentWrong;
    private String customerImpact;
    private String followUpActions;
    private String reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getOverallOutcome() { return overallOutcome; }
    public void setOverallOutcome(String overallOutcome) { this.overallOutcome = overallOutcome; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getWhatWentWell() { return whatWentWell; }
    public void setWhatWentWell(String whatWentWell) { this.whatWentWell = whatWentWell; }
    public String getWhatWentWrong() { return whatWentWrong; }
    public void setWhatWentWrong(String whatWentWrong) { this.whatWentWrong = whatWentWrong; }
    public String getCustomerImpact() { return customerImpact; }
    public void setCustomerImpact(String customerImpact) { this.customerImpact = customerImpact; }
    public String getFollowUpActions() { return followUpActions; }
    public void setFollowUpActions(String followUpActions) { this.followUpActions = followUpActions; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
