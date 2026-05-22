package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class MultiAgentStepResponse {

    private String id;
    private String runId;
    private String phaseId;
    private Integer phaseOrder;
    private String laneKey;
    private Integer stepOrder;
    private String stepType;
    private String status;
    private String agentId;
    private String agentName;
    private String agentExecutionId;
    private String inputContext;
    private String outputContent;
    private String errorMessage;
    private String startedAt;
    private String finishedAt;
    private String createTime;
    private List<ToolSandboxExecutionResponse> toolExecutions;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getPhaseId() { return phaseId; }
    public void setPhaseId(String phaseId) { this.phaseId = phaseId; }

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public String getLaneKey() { return laneKey; }
    public void setLaneKey(String laneKey) { this.laneKey = laneKey; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentExecutionId() { return agentExecutionId; }
    public void setAgentExecutionId(String agentExecutionId) { this.agentExecutionId = agentExecutionId; }

    public String getInputContext() { return inputContext; }
    public void setInputContext(String inputContext) { this.inputContext = inputContext; }

    public String getOutputContent() { return outputContent; }
    public void setOutputContent(String outputContent) { this.outputContent = outputContent; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public List<ToolSandboxExecutionResponse> getToolExecutions() { return toolExecutions; }
    public void setToolExecutions(List<ToolSandboxExecutionResponse> toolExecutions) { this.toolExecutions = toolExecutions; }
}
