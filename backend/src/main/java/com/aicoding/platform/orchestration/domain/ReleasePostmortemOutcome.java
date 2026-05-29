package com.aicoding.platform.orchestration.domain;

public enum ReleasePostmortemOutcome {

    SUCCESS("SUCCESS"),
    SUCCESS_WITH_ISSUES("SUCCESS_WITH_ISSUES"),
    ROLLBACK_NEEDED("ROLLBACK_NEEDED"),
    FAILED_RELEASE("FAILED_RELEASE");

    private final String value;

    ReleasePostmortemOutcome(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
