package com.aicoding.platform.orchestration.domain;

public enum GovernancePackageEvaluationResult {
    HIGH("HIGH"), MEDIUM("MEDIUM"), LOW("LOW"), INCOMPLETE("INCOMPLETE");
    private final String value;
    GovernancePackageEvaluationResult(String v) { this.value = v; }
    public String getValue() { return value; }
}
