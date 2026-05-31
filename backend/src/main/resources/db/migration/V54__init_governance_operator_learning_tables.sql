CREATE TABLE IF NOT EXISTS governance_operator_action_memory (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL, operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    action_type VARCHAR(64) NOT NULL, action_target_type VARCHAR(64) NOT NULL,
    action_target_id BIGINT NULL, accepted_flag TINYINT NOT NULL DEFAULT 0,
    success_flag TINYINT NOT NULL DEFAULT 0, duration_seconds INT NULL,
    note_text TEXT NULL, action_payload_json JSON NULL, occurred_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_gov_operator_action_session(session_id, occurred_at),
    KEY idx_gov_operator_action_operator(operator_id, occurred_at),
    KEY idx_gov_operator_action_guided_task(guided_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance operator action memory';

CREATE TABLE IF NOT EXISTS governance_workspace_session_insight (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL, insight_window VARCHAR(32) NOT NULL,
    total_actions INT NOT NULL DEFAULT 0, accepted_recommendation_count INT NOT NULL DEFAULT 0,
    dismissed_recommendation_count INT NOT NULL DEFAULT 0,
    completed_guided_task_count INT NOT NULL DEFAULT 0,
    blocked_guided_task_count INT NOT NULL DEFAULT 0, avg_action_duration_seconds INT NULL,
    productivity_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    dominant_action_pattern VARCHAR(128) NULL, summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_workspace_session_insight_session(session_id, insight_window),
    KEY idx_gov_workspace_session_insight_operator(operator_id, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance workspace session insight';

CREATE TABLE IF NOT EXISTS governance_remediation_reuse_bundle (
    id BIGINT PRIMARY KEY, bundle_key VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL, guardrail_key VARCHAR(64) NULL, priority VARCHAR(32) NULL,
    effectiveness_level VARCHAR(32) NOT NULL, reuse_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(10,2) NOT NULL DEFAULT 0, action_sequence_json JSON NOT NULL,
    source_session_id BIGINT NULL, source_operator_id BIGINT NULL,
    source_operator_name VARCHAR(128) NULL, enabled TINYINT NOT NULL DEFAULT 1,
    summary_text TEXT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_remediation_reuse_bundle_key(bundle_key),
    KEY idx_gov_remediation_reuse_bundle_category(category, enabled),
    KEY idx_gov_remediation_reuse_bundle_guardrail(guardrail_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance remediation reuse bundle';
