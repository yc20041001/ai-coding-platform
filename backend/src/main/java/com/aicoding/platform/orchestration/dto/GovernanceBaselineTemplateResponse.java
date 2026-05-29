package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceBaselineTemplateResponse {

    private String id;
    private String templateKey;
    private String displayName;
    private String templateScope;
    private Boolean enabled;
    private String defaultSignoffRolesJson;
    private String defaultVerificationRulesJson;
    private String defaultRollbackRequirementsJson;
    private String defaultConfidencePolicyJson;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getTemplateScope() { return templateScope; }
    public void setTemplateScope(String templateScope) { this.templateScope = templateScope; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getDefaultSignoffRolesJson() { return defaultSignoffRolesJson; }
    public void setDefaultSignoffRolesJson(String defaultSignoffRolesJson) { this.defaultSignoffRolesJson = defaultSignoffRolesJson; }
    public String getDefaultVerificationRulesJson() { return defaultVerificationRulesJson; }
    public void setDefaultVerificationRulesJson(String defaultVerificationRulesJson) { this.defaultVerificationRulesJson = defaultVerificationRulesJson; }
    public String getDefaultRollbackRequirementsJson() { return defaultRollbackRequirementsJson; }
    public void setDefaultRollbackRequirementsJson(String defaultRollbackRequirementsJson) { this.defaultRollbackRequirementsJson = defaultRollbackRequirementsJson; }
    public String getDefaultConfidencePolicyJson() { return defaultConfidencePolicyJson; }
    public void setDefaultConfidencePolicyJson(String defaultConfidencePolicyJson) { this.defaultConfidencePolicyJson = defaultConfidencePolicyJson; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
