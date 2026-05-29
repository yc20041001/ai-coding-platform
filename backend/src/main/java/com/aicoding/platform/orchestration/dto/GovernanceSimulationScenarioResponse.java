package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GovernanceSimulationScenarioResponse {
    private String id; private String scenarioName; private String scenarioType;
    private LocalDate baselineSnapshotDate; private String scenarioStatus;
    private String inputJson; private String notes; private String createdBy; private String createdByName;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getScenarioName() { return scenarioName; } public void setScenarioName(String v) { this.scenarioName = v; }
    public String getScenarioType() { return scenarioType; } public void setScenarioType(String v) { this.scenarioType = v; }
    public LocalDate getBaselineSnapshotDate() { return baselineSnapshotDate; } public void setBaselineSnapshotDate(LocalDate v) { this.baselineSnapshotDate = v; }
    public String getScenarioStatus() { return scenarioStatus; } public void setScenarioStatus(String v) { this.scenarioStatus = v; }
    public String getInputJson() { return inputJson; } public void setInputJson(String v) { this.inputJson = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public String getCreatedByName() { return createdByName; } public void setCreatedByName(String v) { this.createdByName = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
