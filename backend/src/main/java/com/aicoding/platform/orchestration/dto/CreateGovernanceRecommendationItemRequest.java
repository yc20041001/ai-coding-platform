package com.aicoding.platform.orchestration.dto;

public class CreateGovernanceRecommendationItemRequest {

    private String projectId;
    private String projectName;
    private String policyKey;
    private String guardrailKey;
    private String category;
    private String priority;
    private String title;
    private String summary;
    private String ownerId;
    private String ownerName;
    private String dueAt;
    private String sourceEvidenceJson;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getGuardrailKey() { return guardrailKey; }
    public void setGuardrailKey(String guardrailKey) { this.guardrailKey = guardrailKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getDueAt() { return dueAt; }
    public void setDueAt(String dueAt) { this.dueAt = dueAt; }
    public String getSourceEvidenceJson() { return sourceEvidenceJson; }
    public void setSourceEvidenceJson(String sourceEvidenceJson) { this.sourceEvidenceJson = sourceEvidenceJson; }
}
