package com.aicoding.platform.orchestration.domain;

public enum GovernanceSessionInsightWindow {
    SESSION("SESSION"), DAY_7("DAY_7"), DAY_14("DAY_14");
    private final String value;
    GovernanceSessionInsightWindow(String v) { this.value = v; }
    public String getValue() { return value; }
}
