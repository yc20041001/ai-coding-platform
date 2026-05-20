package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class WorkflowPhaseTemplateResponse {

    private Integer phaseOrder;
    private String phaseKey;
    private String title;
    private List<WorkflowStepTemplateResponse> steps;

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public String getPhaseKey() { return phaseKey; }
    public void setPhaseKey(String phaseKey) { this.phaseKey = phaseKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<WorkflowStepTemplateResponse> getSteps() { return steps; }
    public void setSteps(List<WorkflowStepTemplateResponse> steps) { this.steps = steps; }
}
