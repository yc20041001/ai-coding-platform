package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_workspace_session_insight")
public class GovernanceWorkspaceSessionInsightEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long sessionId; private Long operatorId; private String operatorName;
    private String insightWindow; private Integer totalActions;
    private Integer acceptedRecommendationCount; private Integer dismissedRecommendationCount;
    private Integer completedGuidedTaskCount; private Integer blockedGuidedTaskCount;
    private Integer avgActionDurationSeconds; private BigDecimal productivityScore;
    private String dominantActionPattern; private String summaryMarkdown;
    private LocalDateTime capturedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getInsightWindow() { return insightWindow; } public void setInsightWindow(String v) { this.insightWindow = v; }
    public Integer getTotalActions() { return totalActions; } public void setTotalActions(Integer v) { this.totalActions = v; }
    public Integer getAcceptedRecommendationCount() { return acceptedRecommendationCount; } public void setAcceptedRecommendationCount(Integer v) { this.acceptedRecommendationCount = v; }
    public Integer getDismissedRecommendationCount() { return dismissedRecommendationCount; } public void setDismissedRecommendationCount(Integer v) { this.dismissedRecommendationCount = v; }
    public Integer getCompletedGuidedTaskCount() { return completedGuidedTaskCount; } public void setCompletedGuidedTaskCount(Integer v) { this.completedGuidedTaskCount = v; }
    public Integer getBlockedGuidedTaskCount() { return blockedGuidedTaskCount; } public void setBlockedGuidedTaskCount(Integer v) { this.blockedGuidedTaskCount = v; }
    public Integer getAvgActionDurationSeconds() { return avgActionDurationSeconds; } public void setAvgActionDurationSeconds(Integer v) { this.avgActionDurationSeconds = v; }
    public BigDecimal getProductivityScore() { return productivityScore; } public void setProductivityScore(BigDecimal v) { this.productivityScore = v; }
    public String getDominantActionPattern() { return dominantActionPattern; } public void setDominantActionPattern(String v) { this.dominantActionPattern = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
    public LocalDateTime getCapturedAt() { return capturedAt; } public void setCapturedAt(LocalDateTime v) { this.capturedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
