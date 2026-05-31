package com.aicoding.platform.orchestration.domain;

public enum GovernanceAlignmentLevel {
    ALIGNED, PARTIAL, DEVIATED, UNKNOWN;
    private final String value;
    GovernanceAlignmentLevel() { this.value = name(); }
    public String getValue() { return value; }
}
