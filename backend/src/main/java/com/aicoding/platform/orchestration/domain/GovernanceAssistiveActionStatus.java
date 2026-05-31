package com.aicoding.platform.orchestration.domain;

public enum GovernanceAssistiveActionStatus {
    PENDING("PENDING"), REVIEWED("REVIEWED"), SKIPPED("SKIPPED"), READY("READY");
    private final String value;
    GovernanceAssistiveActionStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
