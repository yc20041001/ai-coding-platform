package com.aicoding.platform.orchestration.worker;

import java.io.Serializable;

public class ToolExecutionJobMessage implements Serializable {

    private String jobId;
    private String toolExecutionId;
    private String projectId;
    private String taskId;
    private String runId;
    private String stepId;
    private String toolKey;
    private String requestedAt;

    public ToolExecutionJobMessage() {}

    public ToolExecutionJobMessage(String jobId, String toolExecutionId, String projectId,
                                    String taskId, String runId, String stepId,
                                    String toolKey, String requestedAt) {
        this.jobId = jobId;
        this.toolExecutionId = toolExecutionId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.runId = runId;
        this.stepId = stepId;
        this.toolKey = toolKey;
        this.requestedAt = requestedAt;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(String toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getToolKey() { return toolKey; }
    public void setToolKey(String toolKey) { this.toolKey = toolKey; }

    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
}
