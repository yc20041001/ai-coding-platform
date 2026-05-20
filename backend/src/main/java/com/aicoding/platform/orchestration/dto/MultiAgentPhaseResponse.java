package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class MultiAgentPhaseResponse {

    private String id;
    private String runId;
    private Integer phaseOrder;
    private String phaseKey;
    private String title;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String startedAt;
    private String finishedAt;
    private List<MultiAgentStepResponse> steps;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public String getPhaseKey() { return phaseKey; }
    public void setPhaseKey(String phaseKey) { this.phaseKey = phaseKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }

    public String getOutputSummary() { return outputSummary; }
    public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }

    public List<MultiAgentStepResponse> getSteps() { return steps; }
    public void setSteps(List<MultiAgentStepResponse> steps) { this.steps = steps; }
}
