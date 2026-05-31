package com.aicoding.platform.orchestration.domain;

public enum GovernanceAnalyticsWindow {
    LAST_7_DAYS("LAST_7_DAYS"), LAST_30_DAYS("LAST_30_DAYS"), LAST_90_DAYS("LAST_90_DAYS");
    private final String value;
    GovernanceAnalyticsWindow(String v) { this.value = v; }
    public String getValue() { return value; }
}
