package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class MultiAgentRunResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String status;
    private String strategy;
    private String strategyKey;
    private String strategyName;
    private String strategyDescription;
    private String title;
    private String inputSummary;
    private String finalSummary;
    private String errorMessage;
    private String startedAt;
    private String finishedAt;
    private String createTime;
    private String updateTime;
    private List<MultiAgentPhaseResponse> phases;
    private List<MultiAgentStepResponse> steps;
    private List<MultiAgentMessageResponse> messages;
    private List<MultiAgentApprovalGateResponse> approvalGates;
    private MultiAgentApprovalGateResponse pendingApprovalGate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getStrategyKey() { return strategyKey; }
    public void setStrategyKey(String strategyKey) { this.strategyKey = strategyKey; }

    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }

    public String getStrategyDescription() { return strategyDescription; }
    public void setStrategyDescription(String strategyDescription) { this.strategyDescription = strategyDescription; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }

    public String getFinalSummary() { return finalSummary; }
    public void setFinalSummary(String finalSummary) { this.finalSummary = finalSummary; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

    public List<MultiAgentPhaseResponse> getPhases() { return phases; }
    public void setPhases(List<MultiAgentPhaseResponse> phases) { this.phases = phases; }

    public List<MultiAgentStepResponse> getSteps() { return steps; }
    public void setSteps(List<MultiAgentStepResponse> steps) { this.steps = steps; }

    public List<MultiAgentMessageResponse> getMessages() { return messages; }
    public void setMessages(List<MultiAgentMessageResponse> messages) { this.messages = messages; }

    public List<MultiAgentApprovalGateResponse> getApprovalGates() { return approvalGates; }
    public void setApprovalGates(List<MultiAgentApprovalGateResponse> approvalGates) { this.approvalGates = approvalGates; }

    public MultiAgentApprovalGateResponse getPendingApprovalGate() { return pendingApprovalGate; }
    public void setPendingApprovalGate(MultiAgentApprovalGateResponse pendingApprovalGate) { this.pendingApprovalGate = pendingApprovalGate; }
}
