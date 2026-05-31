package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceRecommendationPackageResponse {
    private String id; private String recommendationId; private String draftPlanId; private String packageStatus;
    private String packageTitle; private String packageSummary; private String recommendationContextJson;
    private String attachmentsJson; private String reviewNotesText;
    private Boolean submitReadyFlag; private Boolean submittedFlag;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(String v) { this.draftPlanId = v; }
    public String getPackageStatus() { return packageStatus; } public void setPackageStatus(String v) { this.packageStatus = v; }
    public String getPackageTitle() { return packageTitle; } public void setPackageTitle(String v) { this.packageTitle = v; }
    public String getPackageSummary() { return packageSummary; } public void setPackageSummary(String v) { this.packageSummary = v; }
    public String getRecommendationContextJson() { return recommendationContextJson; } public void setRecommendationContextJson(String v) { this.recommendationContextJson = v; }
    public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String v) { this.attachmentsJson = v; }
    public String getReviewNotesText() { return reviewNotesText; } public void setReviewNotesText(String v) { this.reviewNotesText = v; }
    public Boolean getSubmitReadyFlag() { return submitReadyFlag; } public void setSubmitReadyFlag(Boolean v) { this.submitReadyFlag = v; }
    public Boolean getSubmittedFlag() { return submittedFlag; } public void setSubmittedFlag(Boolean v) { this.submittedFlag = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
