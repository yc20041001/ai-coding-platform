package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_recommendation_package")
public class GovernanceRecommendationPackageEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long recommendationId; private Long draftPlanId; private String packageStatus;
    private String packageTitle; private String packageSummary; private String recommendationContextJson;
    private String attachmentsJson; private String reviewNotesText;
    private Integer submitReadyFlag; private Integer submittedFlag;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(Long v) { this.draftPlanId = v; }
    public String getPackageStatus() { return packageStatus; } public void setPackageStatus(String v) { this.packageStatus = v; }
    public String getPackageTitle() { return packageTitle; } public void setPackageTitle(String v) { this.packageTitle = v; }
    public String getPackageSummary() { return packageSummary; } public void setPackageSummary(String v) { this.packageSummary = v; }
    public String getRecommendationContextJson() { return recommendationContextJson; } public void setRecommendationContextJson(String v) { this.recommendationContextJson = v; }
    public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String v) { this.attachmentsJson = v; }
    public String getReviewNotesText() { return reviewNotesText; } public void setReviewNotesText(String v) { this.reviewNotesText = v; }
    public Integer getSubmitReadyFlag() { return submitReadyFlag; } public void setSubmitReadyFlag(Integer v) { this.submitReadyFlag = v; }
    public Integer getSubmittedFlag() { return submittedFlag; } public void setSubmittedFlag(Integer v) { this.submittedFlag = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
