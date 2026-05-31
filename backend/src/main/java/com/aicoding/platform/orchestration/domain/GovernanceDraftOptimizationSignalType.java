package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftOptimizationSignalType {
    DRAFT_STRUCTURE, SCOPE_ADOPTION, MODIFICATION_PATTERN, STEP_RETENTION, GOAL_ALIGNMENT;
    private final String value;
    GovernanceDraftOptimizationSignalType() { this.value = name(); }
    public String getValue() { return value; }
}
