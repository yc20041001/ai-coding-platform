package com.aicoding.platform.orchestration.domain;

public enum GovernanceFeedbackTargetType {
    NEXT_STEP("NEXT_STEP"), GUIDED_TASK("GUIDED_TASK"), REUSE_BUNDLE("REUSE_BUNDLE"), WORKSPACE_SESSION("WORKSPACE_SESSION");
    private final String value;
    GovernanceFeedbackTargetType(String v) { this.value = v; }
    public String getValue() { return value; }
}
