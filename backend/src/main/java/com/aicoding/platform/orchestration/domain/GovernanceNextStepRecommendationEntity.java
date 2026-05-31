package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_next_step_recommendation")
public class GovernanceNextStepRecommendationEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long sessionId; private Long guidedTaskId; private Long recommendationId;
    private Integer suggestionRank; private String suggestionType; private String title;
    private String summaryText; private String rationaleText; private String expectedOutcomeText;
    private String actionPayloadJson;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(Long v) { this.guidedTaskId = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Integer getSuggestionRank() { return suggestionRank; } public void setSuggestionRank(Integer v) { this.suggestionRank = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
    public String getExpectedOutcomeText() { return expectedOutcomeText; } public void setExpectedOutcomeText(String v) { this.expectedOutcomeText = v; }
    public String getActionPayloadJson() { return actionPayloadJson; } public void setActionPayloadJson(String v) { this.actionPayloadJson = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
