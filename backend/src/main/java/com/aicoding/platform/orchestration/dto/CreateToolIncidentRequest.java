package com.aicoding.platform.orchestration.dto;

public class CreateToolIncidentRequest {

    private String projectId;
    private String sourceType;
    private String sourceId;
    private String severity;
    private String title;
    private String summary;
    private String assigneeId;
    private String toolExecutionId;
    private String toolJobId;
    private String operatorReviewId;

    public CreateToolIncidentRequest() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(String toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getToolJobId() { return toolJobId; }
    public void setToolJobId(String toolJobId) { this.toolJobId = toolJobId; }

    public String getOperatorReviewId() { return operatorReviewId; }
    public void setOperatorReviewId(String operatorReviewId) { this.operatorReviewId = operatorReviewId; }
}
