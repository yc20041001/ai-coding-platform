package com.aicoding.platform.orchestration.dto;

public class GenerateReleaseEvidenceBundleRequest {

    private String projectId;
    private String generatedBy;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
