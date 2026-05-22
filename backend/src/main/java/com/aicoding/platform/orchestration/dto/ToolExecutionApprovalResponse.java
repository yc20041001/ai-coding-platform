package com.aicoding.platform.orchestration.dto;

public class ToolExecutionApprovalResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String runId;
    private String stepId;
    private String toolExecutionId;
    private String toolId;
    private String toolKey;
    private String approvalKey;
    private String title;
    private String description;
    private String riskLevel;
    private String status;
    private String requestedBy;
    private String decidedBy;
    private String decisionComment;
    private String requestedAt;
    private String decidedAt;
    private String expiresAt;
    private String createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(String toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getToolId() { return toolId; }
    public void setToolId(String toolId) { this.toolId = toolId; }

    public String getToolKey() { return toolKey; }
    public void setToolKey(String toolKey) { this.toolKey = toolKey; }

    public String getApprovalKey() { return approvalKey; }
    public void setApprovalKey(String approvalKey) { this.approvalKey = approvalKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }

    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }

    public String getDecidedAt() { return decidedAt; }
    public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
