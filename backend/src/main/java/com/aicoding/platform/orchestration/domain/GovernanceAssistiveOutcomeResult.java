package com.aicoding.platform.orchestration.domain;

public enum GovernanceAssistiveOutcomeResult {
    USEFUL("USEFUL"), PARTIALLY_USEFUL("PARTIALLY_USEFUL"), NOT_USEFUL("NOT_USEFUL"), NOT_APPLICABLE("NOT_APPLICABLE");
    private final String value;
    GovernanceAssistiveOutcomeResult(String v) { this.value = v; }
    public String getValue() { return value; }
}
