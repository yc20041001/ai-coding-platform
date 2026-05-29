package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class OrganizationTrialPolicyResponse {

    private String id;
    private String policyKey;
    private String displayName;
    private String policyScope;
    private Boolean enabled;
    private String thresholdJson;
    private String signoffPolicyJson;
    private String rollbackPolicyJson;
    private String verificationPolicyJson;
    private String recommendationPolicyJson;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPolicyScope() { return policyScope; }
    public void setPolicyScope(String policyScope) { this.policyScope = policyScope; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
