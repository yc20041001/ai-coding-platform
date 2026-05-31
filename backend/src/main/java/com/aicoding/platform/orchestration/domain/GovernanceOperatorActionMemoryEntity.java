package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_operator_action_memory")
public class GovernanceOperatorActionMemoryEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long sessionId; private Long guidedTaskId; private Long recommendationId;
    private Long operatorId; private String operatorName; private String actionType;
    private String actionTargetType; private Long actionTargetId;
    private Integer acceptedFlag; private Integer successFlag; private Integer durationSeconds;
    private String noteText; private String actionPayloadJson; private LocalDateTime occurredAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(Long v) { this.guidedTaskId = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public String getActionTargetType() { return actionTargetType; } public void setActionTargetType(String v) { this.actionTargetType = v; }
    public Long getActionTargetId() { return actionTargetId; } public void setActionTargetId(Long v) { this.actionTargetId = v; }
    public Integer getAcceptedFlag() { return acceptedFlag; } public void setAcceptedFlag(Integer v) { this.acceptedFlag = v; }
    public Integer getSuccessFlag() { return successFlag; } public void setSuccessFlag(Integer v) { this.successFlag = v; }
    public Integer getDurationSeconds() { return durationSeconds; } public void setDurationSeconds(Integer v) { this.durationSeconds = v; }
    public String getNoteText() { return noteText; } public void setNoteText(String v) { this.noteText = v; }
    public String getActionPayloadJson() { return actionPayloadJson; } public void setActionPayloadJson(String v) { this.actionPayloadJson = v; }
    public LocalDateTime getOccurredAt() { return occurredAt; } public void setOccurredAt(LocalDateTime v) { this.occurredAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
