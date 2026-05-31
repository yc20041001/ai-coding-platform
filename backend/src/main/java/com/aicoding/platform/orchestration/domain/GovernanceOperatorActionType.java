package com.aicoding.platform.orchestration.domain;

public enum GovernanceOperatorActionType {
    OPEN_RECOMMENDATION, OPEN_PLAYBOOK, OPEN_RECIPE, OPEN_KNOWLEDGE, START_HANDOFF,
    UPDATE_GUIDED_TASK, COMPLETE_GUIDED_TASK, DISMISS_NEXT_STEP, ACCEPT_NEXT_STEP,
    REVIEW_WAIVER, REVIEW_FORECAST, EXPORT_REPORT;
    private final String value;
    GovernanceOperatorActionType() { this.value = name(); }
    public String getValue() { return value; }
}
