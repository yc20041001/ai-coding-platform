package com.aicoding.platform.orchestration.domain;

public enum MultiAgentMessageType {
    TASK_CONTEXT,
    STEP_OUTPUT,
    HANDOFF,
    REVIEW_FEEDBACK,
    FINAL_CONTEXT,
    APPROVAL_REQUEST,
    APPROVAL_DECISION
}
