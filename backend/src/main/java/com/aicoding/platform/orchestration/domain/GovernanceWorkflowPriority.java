package com.aicoding.platform.orchestration.domain;

public enum GovernanceWorkflowPriority {
    P0("P0"),
    P1("P1"),
    P2("P2"),
    P3("P3");

    private final String value;

    GovernanceWorkflowPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
