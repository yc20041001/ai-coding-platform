package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_draft_remediation_plan")
public class GovernanceDraftRemediationPlanEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long recommendationId; private Long sessionId; private Long operatorId; private String operatorName;
    private String planStatus; private String planTitle; private String scopeType;
    private String summaryText; private String goalText; private String proposedStepsJson;
    private Long linkedBundleId; private String linkedPlaybookKey; private String linkedRecipeKey;
    private String riskLevel; private Integer humanConfirmationRequired;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getPlanStatus() { return planStatus; } public void setPlanStatus(String v) { this.planStatus = v; }
    public String getPlanTitle() { return planTitle; } public void setPlanTitle(String v) { this.planTitle = v; }
    public String getScopeType() { return scopeType; } public void setScopeType(String v) { this.scopeType = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getGoalText() { return goalText; } public void setGoalText(String v) { this.goalText = v; }
    public String getProposedStepsJson() { return proposedStepsJson; } public void setProposedStepsJson(String v) { this.proposedStepsJson = v; }
    public Long getLinkedBundleId() { return linkedBundleId; } public void setLinkedBundleId(Long v) { this.linkedBundleId = v; }
    public String getLinkedPlaybookKey() { return linkedPlaybookKey; } public void setLinkedPlaybookKey(String v) { this.linkedPlaybookKey = v; }
    public String getLinkedRecipeKey() { return linkedRecipeKey; } public void setLinkedRecipeKey(String v) { this.linkedRecipeKey = v; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { this.riskLevel = v; }
    public Integer getHumanConfirmationRequired() { return humanConfirmationRequired; } public void setHumanConfirmationRequired(Integer v) { this.humanConfirmationRequired = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
