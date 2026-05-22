package com.aicoding.platform.orchestration.domain;

public enum ToolExecutionErrorCode {
    PUBLISH_FAILED,
    MESSAGE_INVALID,
    JOB_NOT_FOUND,
    JOB_CANCELED,
    MOCK_EXECUTION_FAILED,
    POLICY_BLOCKED,
    APPROVAL_REQUIRED,
    ARTIFACT_CREATE_FAILED,
    TIMEOUT,
    UNKNOWN
}
