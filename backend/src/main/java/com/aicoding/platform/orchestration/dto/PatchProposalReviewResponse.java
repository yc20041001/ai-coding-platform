package com.aicoding.platform.orchestration.dto;

public class PatchProposalReviewResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String artifactId;
    private String toolExecutionId;
    private String status;
    private String decision;
    private String reviewerId;
    private String reviewComment;
    private String reviewedAt;
    private Boolean safetyConfirmed;
    private String checklistJson;
    private String createTime;
    private String updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(String toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }

    public Boolean getSafetyConfirmed() { return safetyConfirmed; }
    public void setSafetyConfirmed(Boolean safetyConfirmed) { this.safetyConfirmed = safetyConfirmed; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
