package com.aicoding.platform.orchestration.domain;

public enum GovernanceActionTargetType {
    RECOMMENDATION, GUIDED_TASK, PLAYBOOK, RECIPE, KNOWLEDGE, HANDOFF, WAIVER, FORECAST, SESSION, REPORT;
    private final String value;
    GovernanceActionTargetType() { this.value = name(); }
    public String getValue() { return value; }
}
