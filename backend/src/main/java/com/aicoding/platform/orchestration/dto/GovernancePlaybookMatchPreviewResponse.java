package com.aicoding.platform.orchestration.dto;

public class GovernancePlaybookMatchPreviewResponse {
    private String recommendationId; private String matchMode; private GovernancePlaybookTemplateResponse matchedTemplate;
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getMatchMode() { return matchMode; } public void setMatchMode(String v) { this.matchMode = v; }
    public GovernancePlaybookTemplateResponse getMatchedTemplate() { return matchedTemplate; }
    public void setMatchedTemplate(GovernancePlaybookTemplateResponse v) { this.matchedTemplate = v; }
}
