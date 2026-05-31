package com.aicoding.platform.orchestration.domain;

public enum GovernanceNextStepSuggestionType {
    OPEN_PLAYBOOK("OPEN_PLAYBOOK"), OPEN_RECIPE("OPEN_RECIPE"),
    OPEN_KNOWLEDGE("OPEN_KNOWLEDGE"), START_HANDOFF("START_HANDOFF"),
    REVIEW_WAIVER("REVIEW_WAIVER"), REVIEW_FORECAST("REVIEW_FORECAST");
    private final String value;
    GovernanceNextStepSuggestionType(String v) { this.value = v; }
    public String getValue() { return value; }
}
