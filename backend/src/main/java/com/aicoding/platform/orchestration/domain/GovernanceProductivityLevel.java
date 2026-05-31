package com.aicoding.platform.orchestration.domain;

public enum GovernanceProductivityLevel {
    HIGH("HIGH"), MEDIUM("MEDIUM"), LOW("LOW"), AT_RISK("AT_RISK");
    private final String value;
    GovernanceProductivityLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
