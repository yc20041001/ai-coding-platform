package com.aicoding.platform.orchestration.dto;

public class CreateGovernanceSlaPolicyRequest {
    private String policyKey; private String displayName; private String priority;
    private String category; private Integer slaHours; private Integer warningHours; private String notes;
    public String getPolicyKey() { return policyKey; } public void setPolicyKey(String v) { this.policyKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public Integer getSlaHours() { return slaHours; } public void setSlaHours(Integer v) { this.slaHours = v; }
    public Integer getWarningHours() { return warningHours; } public void setWarningHours(Integer v) { this.warningHours = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
}
