package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_package_review_evaluation")
public class GovernancePackageReviewEvaluationEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long packageId; private Long draftPlanId; private Long operatorId; private String operatorName;
    private String evaluationResult; private Integer completenessScore; private Integer accuracyScore;
    private Integer overallScore; private String reasonCode; private String reviewNotesText;
    private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getPackageId() { return packageId; } public void setPackageId(Long v) { this.packageId = v; }
    public Long getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(Long v) { this.draftPlanId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
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
