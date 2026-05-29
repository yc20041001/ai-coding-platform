package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;

public class GovernanceRecommendationResponse {

    private String projectId;
    private String projectName;
    private String priority;
    private String category;
    private String title;
    private String summary;
    private String sourceType;
    private String policyKey;
    private String guardrailKey;
    private LocalDate snapshotDate;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getGuardrailKey() { return guardrailKey; }
    public void setGuardrailKey(String guardrailKey) { this.guardrailKey = guardrailKey; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
}
