package com.aicoding.platform.orchestration.domain;

public enum PredictiveRiskSignalType {
    OWNER_OVERLOAD_FORECAST("OWNER_OVERLOAD_FORECAST"), OVERDUE_TREND_FORECAST("OVERDUE_TREND_FORECAST"),
    WAIVER_EXPIRY_CLUSTER("WAIVER_EXPIRY_CLUSTER"), PROJECT_BACKLOG_GROWTH("PROJECT_BACKLOG_GROWTH"),
    THROUGHPUT_DEFICIT("THROUGHPUT_DEFICIT");
    private final String value;
    PredictiveRiskSignalType(String v) { this.value = v; }
    public String getValue() { return value; }
}
