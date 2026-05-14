-- V5__init_agent_and_task_tables.sql
-- Agent & Task Module Core Tables

-- ============================================================
-- 1. ai_agent
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_agent (
    id          BIGINT        NOT NULL,
    name        VARCHAR(128)  NOT NULL,
    code        VARCHAR(64)   NOT NULL,
    type        VARCHAR(64)   NOT NULL COMMENT 'ARCHITECT,BACKEND,FRONTEND,TEST,REVIEW,DEVOPS',
    description VARCHAR(255)  NULL,
    avatar      VARCHAR(512)  NULL,
    status      VARCHAR(32)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED,DISABLED',
    create_time DATETIME(3)   NOT NULL,
    update_time DATETIME(3)   NOT NULL,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_agent_code (code),
    INDEX      idx_ai_agent_type (type),
    INDEX      idx_ai_agent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. ai_agent_version
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_agent_version (
    id               BIGINT        NOT NULL,
    agent_id         BIGINT        NOT NULL,
    version_no       VARCHAR(32)   NOT NULL,
    model_config_id  BIGINT        NULL,
    system_prompt    MEDIUMTEXT    NOT NULL,
    tool_policy      JSON          NULL,
    execution_policy JSON          NULL,
    status           VARCHAR(32)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,PUBLISHED,DISABLED',
    publish_time     DATETIME(3)   NULL,
    create_time      DATETIME(3)   NOT NULL,
    update_time      DATETIME(3)   NOT NULL,
    create_by        BIGINT        NULL,
    update_by        BIGINT        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_version (agent_id, version_no),
    INDEX      idx_agent_version_status (agent_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. model_config
-- ============================================================
CREATE TABLE IF NOT EXISTS model_config (
    id             BIGINT        NOT NULL,
    provider       VARCHAR(64)   NOT NULL COMMENT 'OPENAI,CLAUDE,DEEPSEEK,GEMINI,QWEN',
    model_name     VARCHAR(128)  NOT NULL,
    model_type     VARCHAR(32)   NOT NULL COMMENT 'CHAT,EMBEDDING,RERANK',
    api_base       VARCHAR(512)  NULL,
    api_key_enc    TEXT          NULL,
    default_params JSON          NULL,
    status         VARCHAR(32)   NOT NULL DEFAULT 'DISABLED' COMMENT 'ENABLED,DISABLED',
    create_time    DATETIME(3)   NOT NULL,
    update_time    DATETIME(3)   NOT NULL,
    create_by      BIGINT        NULL,
    update_by      BIGINT        NULL,
    PRIMARY KEY (id),
    INDEX idx_model_provider_type (provider, model_type),
    INDEX idx_model_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. project_agent_config
-- ============================================================
CREATE TABLE IF NOT EXISTS project_agent_config (
    id                BIGINT        NOT NULL,
    project_id        BIGINT        NOT NULL,
    agent_id          BIGINT        NOT NULL,
    agent_version_id  BIGINT        NULL,
    model_config_id   BIGINT        NULL,
    enabled           TINYINT       NOT NULL DEFAULT 1,
    config_json       JSON          NULL,
    create_time       DATETIME(3)   NOT NULL,
    update_time       DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_agent (project_id, agent_id),
    INDEX      idx_project_agent_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5. ai_task
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_task (
    id               BIGINT        NOT NULL,
    project_id       BIGINT        NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    description      MEDIUMTEXT    NULL,
    task_type        VARCHAR(64)   NOT NULL COMMENT 'CHAT,CODING,REVIEW,RAG_INDEX,DEVOPS',
    agent_id         BIGINT        NULL,
    agent_version_id BIGINT        NULL,
    creator_id       BIGINT        NOT NULL,
    assignee_id      BIGINT        NULL,
    status           VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,RUNNING,REVIEWING,COMPLETED,FAILED,CANCELED',
    priority         VARCHAR(32)   NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW,MEDIUM,HIGH,URGENT',
    source_type      VARCHAR(32)   NULL COMMENT 'MANUAL,CHAT,PR,SCHEDULE',
    source_id        BIGINT        NULL,
    branch           VARCHAR(128)  NULL,
    retry_count      INT           NOT NULL DEFAULT 0,
    max_retry_count  INT           NOT NULL DEFAULT 3,
    start_time       DATETIME(3)   NULL,
    end_time         DATETIME(3)   NULL,
    due_time         DATETIME(3)   NULL,
    error_message    TEXT          NULL,
    create_time      DATETIME(3)   NOT NULL,
    update_time      DATETIME(3)   NOT NULL,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    version          INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_ai_task_project_status (project_id, status),
    INDEX idx_ai_task_project_time (project_id, create_time),
    INDEX idx_ai_task_agent_status (agent_id, status),
    INDEX idx_ai_task_creator (creator_id),
    INDEX idx_ai_task_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6. ai_task_log
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_task_log (
    id          BIGINT        NOT NULL,
    task_id     BIGINT        NOT NULL,
    project_id  BIGINT        NOT NULL,
    level       VARCHAR(16)   NOT NULL COMMENT 'DEBUG,INFO,WARN,ERROR',
    stage       VARCHAR(64)   NULL,
    message     MEDIUMTEXT    NOT NULL,
    metadata    JSON          NULL,
    create_time DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_task_log_task_time (task_id, create_time),
    INDEX idx_task_log_project_time (project_id, create_time),
    INDEX idx_task_log_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 7. ai_task_artifact
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_task_artifact (
    id            BIGINT        NOT NULL,
    task_id       BIGINT        NOT NULL,
    project_id    BIGINT        NOT NULL,
    artifact_type VARCHAR(32)   NOT NULL COMMENT 'TEXT,PATCH,FILE,REPORT,TEST_RESULT,PR',
    name          VARCHAR(255)  NOT NULL,
    content       MEDIUMTEXT    NULL,
    file_url      VARCHAR(512)  NULL,
    metadata      JSON          NULL,
    create_time   DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_task_artifact_task (task_id),
    INDEX idx_task_artifact_project (project_id, create_time),
    INDEX idx_task_artifact_type (artifact_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 8. ai_task_event
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_task_event (
    id          BIGINT        NOT NULL,
    task_id     BIGINT        NOT NULL,
    project_id  BIGINT        NOT NULL,
    from_status VARCHAR(32)   NULL,
    to_status   VARCHAR(32)   NOT NULL,
    event_type  VARCHAR(64)   NOT NULL COMMENT 'CREATED,STARTED,FAILED,RETRIED,CANCELED,COMPLETED',
    operator_id BIGINT        NULL,
    reason      VARCHAR(512)  NULL,
    create_time DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_task_event_task_time (task_id, create_time),
    INDEX idx_task_event_project_time (project_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- Seed: Built-in Agents + Versions
-- ============================================================

-- Snowflake-style IDs generated for seed agents
-- Agent IDs: 300001 - 300006
-- Version IDs: 310001 - 310006

INSERT INTO ai_agent (id, name, code, type, description, status, create_time, update_time, deleted) VALUES
(300001, 'Architect Agent',  'architect-agent',  'ARCHITECT', 'Analyze requirements and generate architecture designs.',                   'ENABLED', NOW(3), NOW(3), 0),
(300002, 'Backend Agent',    'backend-agent',    'BACKEND',    'Generate backend APIs, services, data models and business logic.',         'ENABLED', NOW(3), NOW(3), 0),
(300003, 'Frontend Agent',   'frontend-agent',   'FRONTEND',   'Generate Vue 3 components, composables, routes and state management.',    'ENABLED', NOW(3), NOW(3), 0),
(300004, 'Test Agent',       'test-agent',       'TEST',       'Generate unit tests, integration tests and test fixtures.',               'ENABLED', NOW(3), NOW(3), 0),
(300005, 'Review Agent',     'review-agent',     'REVIEW',     'Review code changes for bugs, style, security and performance issues.',    'ENABLED', NOW(3), NOW(3), 0),
(300006, 'DevOps Agent',     'devops-agent',     'DEVOPS',     'Generate CI/CD pipelines, Dockerfiles, k8s manifests and deploy scripts.', 'ENABLED', NOW(3), NOW(3), 0);

INSERT INTO ai_agent_version (id, agent_id, version_no, system_prompt, tool_policy, execution_policy, status, publish_time, create_time, update_time) VALUES
(310001, 300001, '1.0.0',
 'You are an Architect Agent. Analyze project requirements and produce architecture documents, component diagrams, and technology recommendations.',
 '{"allowedTools":["file.read","file.search","rag.search"]}',
 '{"maxIterations":10,"approvalRequired":true}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3)),
(310002, 300002, '1.0.0',
 'You are a Backend Agent. Generate Java 17 + Spring Boot 3 APIs, MyBatis-Plus mappers, service logic, and database migrations.',
 '{"allowedTools":["file.read","file.create","file.patch","test.run","rag.search"]}',
 '{"maxIterations":15,"approvalRequired":true}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3)),
(310003, 300003, '1.0.0',
 'You are a Frontend Agent. Generate Vue 3 components with TypeScript, composables, Pinia stores, and Vue Router config.',
 '{"allowedTools":["file.read","file.create","file.patch","test.run","rag.search"]}',
 '{"maxIterations":15,"approvalRequired":true}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3)),
(310004, 300004, '1.0.0',
 'You are a Test Agent. Generate JUnit 5 tests, Mockito mocks, integration tests, and test data fixtures.',
 '{"allowedTools":["file.read","file.create","file.patch","test.run","rag.search"]}',
 '{"maxIterations":10,"approvalRequired":false}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3)),
(310005, 300005, '1.0.0',
 'You are a Review Agent. Review code for correctness, style, security vulnerabilities, and performance regressions.',
 '{"allowedTools":["file.read","git.diff","rag.search"]}',
 '{"maxIterations":5,"approvalRequired":false}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3)),
(310006, 300006, '1.0.0',
 'You are a DevOps Agent. Generate Dockerfiles, CI/CD pipeline configs, Kubernetes manifests, and deployment scripts.',
 '{"allowedTools":["file.read","file.create","file.patch","rag.search"]}',
 '{"maxIterations":10,"approvalRequired":true}',
 'PUBLISHED', NOW(3), NOW(3), NOW(3));
