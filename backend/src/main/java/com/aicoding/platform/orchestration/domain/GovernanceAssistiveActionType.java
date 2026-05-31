package com.aicoding.platform.orchestration.domain;

public enum GovernanceAssistiveActionType {
    OPEN_PLAYBOOK_DRAFT, OPEN_RECIPE_DRAFT, PREPARE_HANDOFF_NOTE,
    PREPARE_WAIVER_REVIEW, PREPARE_FORECAST_CHECK, PREPARE_RISK_SUMMARY;
    private final String value;
    GovernanceAssistiveActionType() { this.value = name(); }
    public String getValue() { return value; }
}
