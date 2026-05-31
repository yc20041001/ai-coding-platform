package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernancePackageReviewEvaluationResponse {
    private String id; private String packageId; private String draftPlanId; private String operatorId;
    private String operatorName; private String evaluationResult; private Integer completenessScore;
    private Integer accuracyScore; private Integer overallScore; private String reasonCode;
    private String reviewNotesText; private LocalDateTime reviewedAt;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getPackageId() { return packageId; } public void setPackageId(String v) { this.packageId = v; }
    public String getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(String v) { this.draftPlanId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getEvaluationResult() { return evaluationResult; } public void setEvaluationResult(String v) { this.evaluationResult = v; }
    public Integer getCompletenessScore() { return completenessScore; } public void setCompletenessScore(Integer v) { this.completenessScore = v; }
    public Integer getAccuracyScore() { return accuracyScore; } public void setAccuracyScore(Integer v) { this.accuracyScore = v; }
    public Integer getOverallScore() { return overallScore; } public void setOverallScore(Integer v) { this.overallScore = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getReviewNotesText() { return reviewNotesText; } public void setReviewNotesText(String v) { this.reviewNotesText = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
