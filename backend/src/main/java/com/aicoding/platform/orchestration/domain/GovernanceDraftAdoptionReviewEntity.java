package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_draft_adoption_review")
public class GovernanceDraftAdoptionReviewEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long draftPlanId; private Long recommendationId; private Long operatorId; private String operatorName;
    private String adoptionResult; private String modificationLevel; private Integer usefulnessRating;
    private String reasonCode; private String outcomeNoteText; private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(Long v) { this.draftPlanId = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getAdoptionResult() { return adoptionResult; } public void setAdoptionResult(String v) { this.adoptionResult = v; }
    public String getModificationLevel() { return modificationLevel; } public void setModificationLevel(String v) { this.modificationLevel = v; }
    public Integer getUsefulnessRating() { return usefulnessRating; } public void setUsefulnessRating(Integer v) { this.usefulnessRating = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getOutcomeNoteText() { return outcomeNoteText; } public void setOutcomeNoteText(String v) { this.outcomeNoteText = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
