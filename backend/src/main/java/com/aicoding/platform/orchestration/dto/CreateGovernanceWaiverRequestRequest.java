package com.aicoding.platform.orchestration.dto;

public class CreateGovernanceWaiverRequestRequest {

    private String recommendationId;
    private String waiverScope;
    private String reasonText;
    private String expiresAt;

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getWaiverScope() { return waiverScope; }
    public void setWaiverScope(String waiverScope) { this.waiverScope = waiverScope; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
