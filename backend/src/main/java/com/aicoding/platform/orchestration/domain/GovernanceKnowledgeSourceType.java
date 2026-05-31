package com.aicoding.platform.orchestration.domain;

public enum GovernanceKnowledgeSourceType {
    RECOMMENDATION("RECOMMENDATION"), EXECUTION_PLAN("EXECUTION_PLAN"),
    HANDOFF("HANDOFF"), WAIVER("WAIVER"), PLAYBOOK("PLAYBOOK");
    private final String value;
    GovernanceKnowledgeSourceType(String v) { this.value = v; }
    public String getValue() { return value; }
}
