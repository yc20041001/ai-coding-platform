package com.aicoding.platform.orchestration.domain;

public enum GovernanceAdoptionBlockerType {
    RESOURCE_CONSTRAINT, TECHNICAL_DEBT, PROCESS_GAP, SKILL_GAP, DEPENDENCY, OTHER;
    private final String value;
    GovernanceAdoptionBlockerType() { this.value = name(); }
    public String getValue() { return value; }
}
