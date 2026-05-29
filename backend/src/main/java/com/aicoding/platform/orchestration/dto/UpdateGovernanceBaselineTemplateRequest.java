package com.aicoding.platform.orchestration.dto;

public class UpdateGovernanceBaselineTemplateRequest {

    private String displayName;
    private String templateScope;
    private String defaultSignoffRolesJson;
    private String defaultVerificationRulesJson;
    private String defaultRollbackRequirementsJson;
    private String defaultConfidencePolicyJson;
    private String notes;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getTemplateScope() { return templateScope; }
    public void setTemplateScope(String templateScope) { this.templateScope = templateScope; }
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
}
