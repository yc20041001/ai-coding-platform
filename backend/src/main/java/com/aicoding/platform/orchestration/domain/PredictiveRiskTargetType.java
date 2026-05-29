package com.aicoding.platform.orchestration.domain;

public enum PredictiveRiskTargetType {
    OWNER("OWNER"), PROJECT("PROJECT"), WAIVER_GROUP("WAIVER_GROUP"), PORTFOLIO("PORTFOLIO");
    private final String value;
    PredictiveRiskTargetType(String v) { this.value = v; }
    public String getValue() { return value; }
}
