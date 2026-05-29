package com.aicoding.platform.orchestration.domain;

public enum GovernanceEscalationStatus {
    OPEN("OPEN"), ACKNOWLEDGED("ACKNOWLEDGED"), RESOLVED("RESOLVED"), IGNORED("IGNORED");
    private final String value;
    GovernanceEscalationStatus(String value) { this.value = value; }
    public String getValue() { return value; }
}
