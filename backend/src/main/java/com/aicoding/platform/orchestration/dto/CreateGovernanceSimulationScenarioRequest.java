package com.aicoding.platform.orchestration.dto;

public class CreateGovernanceSimulationScenarioRequest {
    private String scenarioName; private String scenarioType; private String baselineSnapshotDate;
    private String inputJson; private String notes;
    public String getScenarioName() { return scenarioName; } public void setScenarioName(String v) { this.scenarioName = v; }
    public String getScenarioType() { return scenarioType; } public void setScenarioType(String v) { this.scenarioType = v; }
    public String getBaselineSnapshotDate() { return baselineSnapshotDate; } public void setBaselineSnapshotDate(String v) { this.baselineSnapshotDate = v; }
    public String getInputJson() { return inputJson; } public void setInputJson(String v) { this.inputJson = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
}
