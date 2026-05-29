package com.aicoding.platform.orchestration.domain;

public enum GovernanceSimulationResultStatus {
    SUCCESS("SUCCESS"), WARNING("WARNING"), NO_IMPROVEMENT("NO_IMPROVEMENT"), INVALID("INVALID");
    private final String value;
    GovernanceSimulationResultStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
