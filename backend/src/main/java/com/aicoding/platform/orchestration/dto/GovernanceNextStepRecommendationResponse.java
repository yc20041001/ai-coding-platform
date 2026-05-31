package com.aicoding.platform.orchestration.dto;

public class GovernanceNextStepRecommendationResponse {
    private String id; private String sessionId; private String guidedTaskId; private String recommendationId;
    private Integer suggestionRank; private String suggestionType; private String title;
    private String summaryText; private String rationaleText; private String expectedOutcomeText;
    private String actionPayloadJson;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(String v) { this.guidedTaskId = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public Integer getSuggestionRank() { return suggestionRank; } public void setSuggestionRank(Integer v) { this.suggestionRank = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
    public String getExpectedOutcomeText() { return expectedOutcomeText; } public void setExpectedOutcomeText(String v) { this.expectedOutcomeText = v; }
    public String getActionPayloadJson() { return actionPayloadJson; } public void setActionPayloadJson(String v) { this.actionPayloadJson = v; }
}
