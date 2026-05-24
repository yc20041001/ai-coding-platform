package com.aicoding.platform.orchestration.domain;

public enum ToolIncidentSourceType {
    TOOL_EXECUTION_FAILED,
    TOOL_JOB_FAILED,
    TOOL_JOB_RETRY_PENDING,
    TOOL_JOB_DEAD_LETTERED,
    READ_ONLY_CONTRACT_WARNING,
    TRACE_OUTPUT_PARSE_WARNING,
    HIGH_RISK_REVIEW,
    OPERATOR_REVIEW,
    MANUAL
}
