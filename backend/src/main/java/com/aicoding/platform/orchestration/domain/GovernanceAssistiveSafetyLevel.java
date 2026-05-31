package com.aicoding.platform.orchestration.domain;

public enum GovernanceAssistiveSafetyLevel {
    INFO("INFO"), SAFE("SAFE"), CAUTION("CAUTION"), REVIEW_REQUIRED("REVIEW_REQUIRED");
    private final String value;
    GovernanceAssistiveSafetyLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
