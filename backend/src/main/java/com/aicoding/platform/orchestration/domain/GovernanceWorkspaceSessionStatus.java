package com.aicoding.platform.orchestration.domain;

public enum GovernanceWorkspaceSessionStatus {
    ACTIVE("ACTIVE"), PAUSED("PAUSED"), COMPLETED("COMPLETED"), ARCHIVED("ARCHIVED");
    private final String value;
    GovernanceWorkspaceSessionStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
