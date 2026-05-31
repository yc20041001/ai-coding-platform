package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_safe_assistive_action")
public class GovernanceSafeAssistiveActionEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long draftPlanId; private String actionType; private String actionStatus;
    private String actionTitle; private String actionSummary; private String safetyLevel;
    private Integer confirmationRequired; private String checklistJson; private String prefillPayloadJson;
    private Integer actionOrder;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(Long v) { this.draftPlanId = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public String getActionStatus() { return actionStatus; } public void setActionStatus(String v) { this.actionStatus = v; }
    public String getActionTitle() { return actionTitle; } public void setActionTitle(String v) { this.actionTitle = v; }
    public String getActionSummary() { return actionSummary; } public void setActionSummary(String v) { this.actionSummary = v; }
    public String getSafetyLevel() { return safetyLevel; } public void setSafetyLevel(String v) { this.safetyLevel = v; }
    public Integer getConfirmationRequired() { return confirmationRequired; } public void setConfirmationRequired(Integer v) { this.confirmationRequired = v; }
    public String getChecklistJson() { return checklistJson; } public void setChecklistJson(String v) { this.checklistJson = v; }
    public String getPrefillPayloadJson() { return prefillPayloadJson; } public void setPrefillPayloadJson(String v) { this.prefillPayloadJson = v; }
    public Integer getActionOrder() { return actionOrder; } public void setActionOrder(Integer v) { this.actionOrder = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
