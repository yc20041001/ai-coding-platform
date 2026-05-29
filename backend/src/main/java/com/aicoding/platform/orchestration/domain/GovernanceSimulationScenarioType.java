package com.aicoding.platform.orchestration.domain;

public enum GovernanceSimulationScenarioType {
    SLA_TUNING("SLA_TUNING"), OWNER_REBALANCING("OWNER_REBALANCING"),
    WAIVER_REDUCTION("WAIVER_REDUCTION"), POLICY_THRESHOLD_TUNING("POLICY_THRESHOLD_TUNING");
    private final String value;
    GovernanceSimulationScenarioType(String v) { this.value = v; }
    public String getValue() { return value; }
}
