package com.aicoding.platform.orchestration.domain;

public enum GovernanceEffectivenessLevelV2 {
    LOW("LOW"), MEDIUM("MEDIUM"), HIGH("HIGH"), TOP("TOP");
    private final String value;
    GovernanceEffectivenessLevelV2(String v) { this.value = v; }
    public String getValue() { return value; }
}
