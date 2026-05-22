package com.aicoding.platform.orchestration.domain;

public enum ToolExecutionFailureStage {
    CREATE_JOB,
    PUBLISH,
    CONSUME,
    LOCK,
    POLICY_CHECK,
    MOCK_EXECUTE,
    ARTIFACT,
    COMPLETE
}
