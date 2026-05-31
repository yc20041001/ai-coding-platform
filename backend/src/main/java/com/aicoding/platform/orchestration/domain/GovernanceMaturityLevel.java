package com.aicoding.platform.orchestration.domain;

public enum GovernanceMaturityLevel {
    INITIAL("INITIAL"), DEVELOPING("DEVELOPING"), DEFINED("DEFINED"), MANAGED("MANAGED"), OPTIMIZING("OPTIMIZING");
    private final String value;
    GovernanceMaturityLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
