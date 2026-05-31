package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceSafeAssistiveActionResponse {
    private String id; private String draftPlanId; private String actionType; private String actionStatus;
    private String actionTitle; private String actionSummary; private String safetyLevel;
    private Boolean confirmationRequired; private String checklistJson; private String prefillPayloadJson;
    private Integer actionOrder; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(String v) { this.draftPlanId = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public String getActionStatus() { return actionStatus; } public void setActionStatus(String v) { this.actionStatus = v; }
    public String getActionTitle() { return actionTitle; } public void setActionTitle(String v) { this.actionTitle = v; }
    public String getActionSummary() { return actionSummary; } public void setActionSummary(String v) { this.actionSummary = v; }
    public String getSafetyLevel() { return safetyLevel; } public void setSafetyLevel(String v) { this.safetyLevel = v; }
    public Boolean getConfirmationRequired() { return confirmationRequired; } public void setConfirmationRequired(Boolean v) { this.confirmationRequired = v; }
    public String getChecklistJson() { return checklistJson; } public void setChecklistJson(String v) { this.checklistJson = v; }
    public String getPrefillPayloadJson() { return prefillPayloadJson; } public void setPrefillPayloadJson(String v) { this.prefillPayloadJson = v; }
    public Integer getActionOrder() { return actionOrder; } public void setActionOrder(Integer v) { this.actionOrder = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
