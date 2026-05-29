package com.aicoding.platform.orchestration.domain;

public enum GovernanceCapacityRiskLevel {
    LOW("LOW"), WATCH("WATCH"), HIGH("HIGH"), CRITICAL("CRITICAL");
    private final String value;
    GovernanceCapacityRiskLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
