package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_assistive_action_quality_review")
public class GovernanceAssistiveActionQualityReviewEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long assistiveActionId; private Long draftPlanId; private Long operatorId; private String operatorName;
    private String outcomeResult; private Integer usefulnessRating; private String reasonCode;
    private String feedbackText; private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getAssistiveActionId() { return assistiveActionId; } public void setAssistiveActionId(Long v) { this.assistiveActionId = v; }
    public Long getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(Long v) { this.draftPlanId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getOutcomeResult() { return outcomeResult; } public void setOutcomeResult(String v) { this.outcomeResult = v; }
    public Integer getUsefulnessRating() { return usefulnessRating; } public void setUsefulnessRating(Integer v) { this.usefulnessRating = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getFeedbackText() { return feedbackText; } public void setFeedbackText(String v) { this.feedbackText = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
