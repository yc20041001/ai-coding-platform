package com.aicoding.platform.orchestration.domain;

public enum ToolExecutionJobStatus {
    PENDING,
    RUNNING,
    RETRY_PENDING,
    COMPLETED,
    FAILED,
    CANCELED,
    DEAD_LETTERED
}
