package com.aicoding.platform.orchestration.domain;

public enum GovernanceGuidedTaskType {
    TRIAGE_RECOMMENDATION("TRIAGE_RECOMMENDATION"), RUN_PLAYBOOK("RUN_PLAYBOOK"),
    APPLY_RECIPE_GUIDANCE("APPLY_RECIPE_GUIDANCE"), PREPARE_HANDOFF("PREPARE_HANDOFF"),
    REVIEW_WAIVER("REVIEW_WAIVER"), REDUCE_BACKLOG("REDUCE_BACKLOG");
    private final String value;
    GovernanceGuidedTaskType(String v) { this.value = v; }
    public String getValue() { return value; }
}
