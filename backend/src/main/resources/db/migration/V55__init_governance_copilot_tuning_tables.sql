CREATE TABLE IF NOT EXISTS governance_operator_feedback (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL, suggestion_type VARCHAR(64) NULL, suggestion_id BIGINT NULL,
    guided_task_id BIGINT NULL, reuse_bundle_id BIGINT NULL,
    feedback_target_type VARCHAR(64) NOT NULL, feedback_rating INT NOT NULL,
    helpful_flag TINYINT NOT NULL DEFAULT 0, accepted_flag TINYINT NOT NULL DEFAULT 0,
    reason_code VARCHAR(64) NULL, note_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_feedback_session(session_id, create_time),
    KEY idx_gov_feedback_operator(operator_id, create_time),
    KEY idx_gov_feedback_target(feedback_target_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance operator feedback';

CREATE TABLE IF NOT EXISTS governance_adaptive_guidance_signal (
    id BIGINT PRIMARY KEY, signal_type VARCHAR(64) NOT NULL, focus_mode VARCHAR(32) NULL,
    category VARCHAR(64) NULL, suggestion_type VARCHAR(64) NULL,
    recommendation_priority VARCHAR(32) NULL, acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    completion_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    weight_score DECIMAL(10,2) NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    rationale_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_adaptive_signal_type(signal_type, captured_at),
    KEY idx_gov_adaptive_signal_focus(focus_mode, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance adaptive guidance signal';

CREATE TABLE IF NOT EXISTS governance_copilot_tuning_snapshot (
    id BIGINT PRIMARY KEY, snapshot_window VARCHAR(32) NOT NULL,
    total_feedback_count INT NOT NULL DEFAULT 0, acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    dismissal_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    top_suggestion_type VARCHAR(64) NULL, weakest_suggestion_type VARCHAR(64) NULL,
    top_focus_mode VARCHAR(32) NULL, weakest_focus_mode VARCHAR(32) NULL,
    tuning_confidence_score DECIMAL(10,2) NOT NULL DEFAULT 0, summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_tuning_snapshot_window(snapshot_window, captured_at),
    KEY idx_gov_tuning_snapshot_captured(captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance copilot tuning snapshot';
