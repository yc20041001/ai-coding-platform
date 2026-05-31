package com.aicoding.platform.orchestration.domain;

public enum GovernanceEffectivenessLevel {
    LOW("LOW"), MEDIUM("MEDIUM"), HIGH("HIGH"), TOP("TOP");
    private final String value;
    GovernanceEffectivenessLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
