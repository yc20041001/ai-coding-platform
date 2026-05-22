package com.aicoding.platform.orchestration.domain;

public enum ToolExecutionStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    BLOCKED,
    REJECTED,
    CANCELED
}
