package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceDraftRemediationPlanResponse {
    private String id; private String recommendationId; private String sessionId; private String operatorId;
    private String operatorName; private String planStatus; private String planTitle; private String scopeType;
    private String summaryText; private String goalText; private String proposedStepsJson;
    private String linkedBundleId; private String linkedPlaybookKey; private String linkedRecipeKey;
    private String riskLevel; private Boolean humanConfirmationRequired;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getPlanStatus() { return planStatus; } public void setPlanStatus(String v) { this.planStatus = v; }
    public String getPlanTitle() { return planTitle; } public void setPlanTitle(String v) { this.planTitle = v; }
    public String getScopeType() { return scopeType; } public void setScopeType(String v) { this.scopeType = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getGoalText() { return goalText; } public void setGoalText(String v) { this.goalText = v; }
    public String getProposedStepsJson() { return proposedStepsJson; } public void setProposedStepsJson(String v) { this.proposedStepsJson = v; }
    public String getLinkedBundleId() { return linkedBundleId; } public void setLinkedBundleId(String v) { this.linkedBundleId = v; }
    public String getLinkedPlaybookKey() { return linkedPlaybookKey; } public void setLinkedPlaybookKey(String v) { this.linkedPlaybookKey = v; }
    public String getLinkedRecipeKey() { return linkedRecipeKey; } public void setLinkedRecipeKey(String v) { this.linkedRecipeKey = v; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { this.riskLevel = v; }
    public Boolean getHumanConfirmationRequired() { return humanConfirmationRequired; } public void setHumanConfirmationRequired(Boolean v) { this.humanConfirmationRequired = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
