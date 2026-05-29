package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceSimulationComparisonResponse {
    private String scenarioId; private String scenarioName; private String scenarioType;
    private BigDecimal baselineProjectedBacklog; private BigDecimal simulatedProjectedBacklog;
    private BigDecimal baselineProjectedOverdue; private BigDecimal simulatedProjectedOverdue;
    private BigDecimal baselineRiskScore; private BigDecimal simulatedRiskScore;
    private String deltaSummary;
    public String getScenarioId() { return scenarioId; } public void setScenarioId(String v) { this.scenarioId = v; }
    public String getScenarioName() { return scenarioName; } public void setScenarioName(String v) { this.scenarioName = v; }
    public String getScenarioType() { return scenarioType; } public void setScenarioType(String v) { this.scenarioType = v; }
    public BigDecimal getBaselineProjectedBacklog() { return baselineProjectedBacklog; } public void setBaselineProjectedBacklog(BigDecimal v) { this.baselineProjectedBacklog = v; }
    public BigDecimal getSimulatedProjectedBacklog() { return simulatedProjectedBacklog; } public void setSimulatedProjectedBacklog(BigDecimal v) { this.simulatedProjectedBacklog = v; }
    public BigDecimal getBaselineProjectedOverdue() { return baselineProjectedOverdue; } public void setBaselineProjectedOverdue(BigDecimal v) { this.baselineProjectedOverdue = v; }
    public BigDecimal getSimulatedProjectedOverdue() { return simulatedProjectedOverdue; } public void setSimulatedProjectedOverdue(BigDecimal v) { this.simulatedProjectedOverdue = v; }
    public BigDecimal getBaselineRiskScore() { return baselineRiskScore; } public void setBaselineRiskScore(BigDecimal v) { this.baselineRiskScore = v; }
    public BigDecimal getSimulatedRiskScore() { return simulatedRiskScore; } public void setSimulatedRiskScore(BigDecimal v) { this.simulatedRiskScore = v; }
    public String getDeltaSummary() { return deltaSummary; } public void setDeltaSummary(String v) { this.deltaSummary = v; }
}
