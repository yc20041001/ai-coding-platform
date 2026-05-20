package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class WorkflowStrategyResponse {

    private String strategyKey;
    private String name;
    private String description;
    private Integer phaseCount;
    private Integer stepCount;
    private List<WorkflowPhaseTemplateResponse> phases;

    public String getStrategyKey() { return strategyKey; }
    public void setStrategyKey(String strategyKey) { this.strategyKey = strategyKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPhaseCount() { return phaseCount; }
    public void setPhaseCount(Integer phaseCount) { this.phaseCount = phaseCount; }

    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }

    public List<WorkflowPhaseTemplateResponse> getPhases() { return phases; }
    public void setPhases(List<WorkflowPhaseTemplateResponse> phases) { this.phases = phases; }
}
