package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class GovernanceSimulationDashboardResponse {
    private LocalDate snapshotDate; private Integer scenarioCount; private Integer successfulScenarioCount;
    private Integer warningScenarioCount; private Integer noImprovementCount;
    private List<GovernanceSimulationScenarioResponse> topScenarios;
    private List<PolicyTuningSuggestionResponse> topSuggestions;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getScenarioCount() { return scenarioCount; } public void setScenarioCount(Integer v) { this.scenarioCount = v; }
    public Integer getSuccessfulScenarioCount() { return successfulScenarioCount; } public void setSuccessfulScenarioCount(Integer v) { this.successfulScenarioCount = v; }
    public Integer getWarningScenarioCount() { return warningScenarioCount; } public void setWarningScenarioCount(Integer v) { this.warningScenarioCount = v; }
    public Integer getNoImprovementCount() { return noImprovementCount; } public void setNoImprovementCount(Integer v) { this.noImprovementCount = v; }
    public List<GovernanceSimulationScenarioResponse> getTopScenarios() { return topScenarios; }
    public void setTopScenarios(List<GovernanceSimulationScenarioResponse> v) { this.topScenarios = v; }
    public List<PolicyTuningSuggestionResponse> getTopSuggestions() { return topSuggestions; }
    public void setTopSuggestions(List<PolicyTuningSuggestionResponse> v) { this.topSuggestions = v; }
}
