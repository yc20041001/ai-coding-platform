package com.aicoding.platform.orchestration.domain;

public enum GovernanceWorkspaceFocusMode {
    PRIORITY_FIRST("PRIORITY_FIRST"), OWNER_CENTRIC("OWNER_CENTRIC"),
    PROJECT_CENTRIC("PROJECT_CENTRIC"), WAIVER_REDUCTION("WAIVER_REDUCTION"),
    BACKLOG_REDUCTION("BACKLOG_REDUCTION");
    private final String value;
    GovernanceWorkspaceFocusMode(String v) { this.value = v; }
    public String getValue() { return value; }
}
