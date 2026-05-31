package com.aicoding.platform.orchestration.domain;

public enum GovernanceAssistiveOrderingOptimizationLevel {
    PROMOTE("PROMOTE"), KEEP("KEEP"), DEMOTE("DEMOTE"), REMOVE("REMOVE");
    private final String value;
    GovernanceAssistiveOrderingOptimizationLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
