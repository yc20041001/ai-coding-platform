package com.aicoding.platform.orchestration.domain;

public enum GovernanceSimulationScenarioStatus {
    DRAFT("DRAFT"), READY("READY"), SIMULATED("SIMULATED"), ARCHIVED("ARCHIVED");
    private final String value;
    GovernanceSimulationScenarioStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
