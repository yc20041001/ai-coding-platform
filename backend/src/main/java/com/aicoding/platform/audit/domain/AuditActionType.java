package com.aicoding.platform.audit.domain;

public enum AuditActionType {
    AUTH_LOGIN,
    PROJECT_CREATE,
    PROJECT_UPDATE,
    PROJECT_DELETE,
    MEMBER_INVITE,
    REPOSITORY_BIND,
    TASK_CREATE,
    TASK_EXECUTE,
    TASK_CANCEL,
    AGENT_CREATE,
    CHAT_SEND,
    RAG_DOCUMENT_UPLOAD,
    RAG_SEARCH,
    MODEL_CALL,
    SYSTEM_OPERATION
}
