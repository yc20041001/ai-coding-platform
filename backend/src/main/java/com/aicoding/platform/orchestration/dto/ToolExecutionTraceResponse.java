package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ToolExecutionTraceResponse {

    private String executionId;
    private String projectId;
    private String taskId;
    private String runId;
    private String stepId;
    private String toolKey;
    private String toolName;
    private String riskLevel;
    private String status;
    private String mode;
    private Boolean readOnly;
    private Boolean policyAllowed;
    private String policyReason;
    private String inputPayload;
    private String outputPayload;
    private ToolExecutionApprovalEvidenceResponse approval;
    private ToolExecutionJobEvidenceResponse job;
    private ToolExecutionEvidenceResponse evidence;
    private List<ToolExecutionTraceEventResponse> events;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ToolExecutionTraceResponse() {}

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

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

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }

    public Boolean getPolicyAllowed() { return policyAllowed; }
    public void setPolicyAllowed(Boolean policyAllowed) { this.policyAllowed = policyAllowed; }

    public String getPolicyReason() { return policyReason; }
    public void setPolicyReason(String policyReason) { this.policyReason = policyReason; }

    public String getInputPayload() { return inputPayload; }
    public void setInputPayload(String inputPayload) { this.inputPayload = inputPayload; }

    public String getOutputPayload() { return outputPayload; }
    public void setOutputPayload(String outputPayload) { this.outputPayload = outputPayload; }

    public ToolExecutionApprovalEvidenceResponse getApproval() { return approval; }
    public void setApproval(ToolExecutionApprovalEvidenceResponse approval) { this.approval = approval; }

    public ToolExecutionJobEvidenceResponse getJob() { return job; }
    public void setJob(ToolExecutionJobEvidenceResponse job) { this.job = job; }

    public ToolExecutionEvidenceResponse getEvidence() { return evidence; }
    public void setEvidence(ToolExecutionEvidenceResponse evidence) { this.evidence = evidence; }

    public List<ToolExecutionTraceEventResponse> getEvents() { return events; }
    public void setEvents(List<ToolExecutionTraceEventResponse> events) { this.events = events; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
