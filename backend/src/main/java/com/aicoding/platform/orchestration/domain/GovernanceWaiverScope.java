package com.aicoding.platform.orchestration.domain;

public enum GovernanceWaiverScope {
    PROJECT_RELEASE("PROJECT_RELEASE"),
    POLICY_EXCEPTION("POLICY_EXCEPTION"),
    TEMPORARY_SIGNOFF_GAP("TEMPORARY_SIGNOFF_GAP"),
    ROLLBACK_READINESS_EXCEPTION("ROLLBACK_READINESS_EXCEPTION");

    private final String value;

    GovernanceWaiverScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
