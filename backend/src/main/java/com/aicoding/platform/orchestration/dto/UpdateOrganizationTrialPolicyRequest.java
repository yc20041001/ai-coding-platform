package com.aicoding.platform.orchestration.dto;

public class UpdateOrganizationTrialPolicyRequest {

    private String displayName;
    private String policyScope;
    private String thresholdJson;
    private String signoffPolicyJson;
    private String rollbackPolicyJson;
    private String verificationPolicyJson;
    private String recommendationPolicyJson;
    private String notes;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPolicyScope() { return policyScope; }
    public void setPolicyScope(String policyScope) { this.policyScope = policyScope; }
    public String getThresholdJson() { return thresholdJson; }
    public void setThresholdJson(String thresholdJson) { this.thresholdJson = thresholdJson; }
    public String getSignoffPolicyJson() { return signoffPolicyJson; }
    public void setSignoffPolicyJson(String signoffPolicyJson) { this.signoffPolicyJson = signoffPolicyJson; }
    public String getRollbackPolicyJson() { return rollbackPolicyJson; }
    public void setRollbackPolicyJson(String rollbackPolicyJson) { this.rollbackPolicyJson = rollbackPolicyJson; }
    public String getVerificationPolicyJson() { return verificationPolicyJson; }
    public void setVerificationPolicyJson(String verificationPolicyJson) { this.verificationPolicyJson = verificationPolicyJson; }
    public String getRecommendationPolicyJson() { return recommendationPolicyJson; }
    public void setRecommendationPolicyJson(String recommendationPolicyJson) { this.recommendationPolicyJson = recommendationPolicyJson; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
