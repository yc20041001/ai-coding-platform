package com.aicoding.platform.orchestration.dto;

public class CreateToolOperatorReviewRequest {

    private String reviewTargetType;
    private String reviewTargetId;
    private String severity;
    private String title;
    private String summary;
    private String assigneeId;

    public CreateToolOperatorReviewRequest() {}

    public String getReviewTargetType() { return reviewTargetType; }
    public void setReviewTargetType(String reviewTargetType) { this.reviewTargetType = reviewTargetType; }

    public String getReviewTargetId() { return reviewTargetId; }
    public void setReviewTargetId(String reviewTargetId) { this.reviewTargetId = reviewTargetId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
}
