package com.aicoding.platform.orchestration.domain;

public enum GovernanceReuseBundleEffectivenessLevel {
    TOP("TOP"), USEFUL("USEFUL"), LIMITED("LIMITED"), LOW_VALUE("LOW_VALUE");
    private final String value;
    GovernanceReuseBundleEffectivenessLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
