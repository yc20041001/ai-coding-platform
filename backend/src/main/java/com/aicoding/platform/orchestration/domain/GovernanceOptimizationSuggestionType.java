package com.aicoding.platform.orchestration.domain;

public enum GovernanceOptimizationSuggestionType {
    PROMOTE_RECIPE("PROMOTE_RECIPE"), PRUNE_RECIPE("PRUNE_RECIPE"),
    REFINE_PLAYBOOK("REFINE_PLAYBOOK"), SPLIT_PATTERN("SPLIT_PATTERN"),
    MERGE_DUPLICATE_RECIPES("MERGE_DUPLICATE_RECIPES");
    private final String value;
    GovernanceOptimizationSuggestionType(String v) { this.value = v; }
    public String getValue() { return value; }
}
