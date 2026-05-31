package com.aicoding.platform.orchestration.domain;

public enum GovernanceAdaptiveSignalLevel {
    BOOST("BOOST"), KEEP("KEEP"), WATCH("WATCH"), DOWNRANK("DOWNRANK");
    private final String value;
    GovernanceAdaptiveSignalLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
