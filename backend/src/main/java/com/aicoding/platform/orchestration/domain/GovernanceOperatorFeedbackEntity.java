package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_operator_feedback")
public class GovernanceOperatorFeedbackEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long sessionId; private Long operatorId; private String operatorName;
    private String suggestionType; private Long suggestionId; private Long guidedTaskId;
    private Long reuseBundleId; private String feedbackTargetType; private Integer feedbackRating;
    private Integer helpfulFlag; private Integer acceptedFlag; private String reasonCode;
    private String noteText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public Long getSuggestionId() { return suggestionId; } public void setSuggestionId(Long v) { this.suggestionId = v; }
    public Long getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(Long v) { this.guidedTaskId = v; }
    public Long getReuseBundleId() { return reuseBundleId; } public void setReuseBundleId(Long v) { this.reuseBundleId = v; }
    public String getFeedbackTargetType() { return feedbackTargetType; } public void setFeedbackTargetType(String v) { this.feedbackTargetType = v; }
    public Integer getFeedbackRating() { return feedbackRating; } public void setFeedbackRating(Integer v) { this.feedbackRating = v; }
    public Integer getHelpfulFlag() { return helpfulFlag; } public void setHelpfulFlag(Integer v) { this.helpfulFlag = v; }
    public Integer getAcceptedFlag() { return acceptedFlag; } public void setAcceptedFlag(Integer v) { this.acceptedFlag = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getNoteText() { return noteText; } public void setNoteText(String v) { this.noteText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
