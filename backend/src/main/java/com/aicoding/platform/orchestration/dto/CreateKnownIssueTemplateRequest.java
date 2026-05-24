package com.aicoding.platform.orchestration.dto;

public class CreateKnownIssueTemplateRequest {

    private String projectId;
    private String title;
    private String category;
    private String severity;
    private String rootCauseTemplate;
    private String impactTemplate;
    private String resolutionTemplate;
    private String preventionTemplate;
    private String tags;

    public CreateKnownIssueTemplateRequest() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getRootCauseTemplate() { return rootCauseTemplate; }
    public void setRootCauseTemplate(String rootCauseTemplate) { this.rootCauseTemplate = rootCauseTemplate; }

    public String getImpactTemplate() { return impactTemplate; }
    public void setImpactTemplate(String impactTemplate) { this.impactTemplate = impactTemplate; }

    public String getResolutionTemplate() { return resolutionTemplate; }
    public void setResolutionTemplate(String resolutionTemplate) { this.resolutionTemplate = resolutionTemplate; }

    public String getPreventionTemplate() { return preventionTemplate; }
    public void setPreventionTemplate(String preventionTemplate) { this.preventionTemplate = preventionTemplate; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
