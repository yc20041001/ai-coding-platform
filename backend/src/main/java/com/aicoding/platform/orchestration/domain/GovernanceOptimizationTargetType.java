package com.aicoding.platform.orchestration.domain;

public enum GovernanceOptimizationTargetType {
    RECIPE("RECIPE"), PLAYBOOK("PLAYBOOK"), PATTERN("PATTERN"), KNOWLEDGE_ENTRY("KNOWLEDGE_ENTRY");
    private final String value;
    GovernanceOptimizationTargetType(String v) { this.value = v; }
    public String getValue() { return value; }
}
