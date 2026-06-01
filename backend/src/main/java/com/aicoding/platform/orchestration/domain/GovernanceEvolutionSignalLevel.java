package com.aicoding.platform.orchestration.domain;

public enum GovernanceEvolutionSignalLevel {
    IMPROVING("IMPROVING"), STABLE("STABLE"), DECLINING("DECLINING"), INSUFFICIENT("INSUFFICIENT");
    private final String value;
    GovernanceEvolutionSignalLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
