package com.aicoding.platform.orchestration.domain;

public enum GovernanceUpliftLevel {
    SIGNIFICANT("SIGNIFICANT"), MODERATE("MODERATE"), MINIMAL("MINIMAL"), NONE("NONE");
    private final String value;
    GovernanceUpliftLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
