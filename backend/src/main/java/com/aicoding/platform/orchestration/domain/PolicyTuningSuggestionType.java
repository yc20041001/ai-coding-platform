package com.aicoding.platform.orchestration.domain;

public enum PolicyTuningSuggestionType {
    ADJUST_SLA("ADJUST_SLA"), REBALANCE_OWNER_LOAD("REBALANCE_OWNER_LOAD"),
    REDUCE_WAIVER_CLUSTER("REDUCE_WAIVER_CLUSTER"), ADJUST_GUARDRAIL_THRESHOLD("ADJUST_GUARDRAIL_THRESHOLD");
    private final String value;
    PolicyTuningSuggestionType(String v) { this.value = v; }
    public String getValue() { return value; }
}
