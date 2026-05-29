CREATE TABLE IF NOT EXISTS organization_trial_policy (
    id BIGINT PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    policy_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    threshold_json JSON NULL,
    signoff_policy_json JSON NULL,
    rollback_policy_json JSON NULL,
    verification_policy_json JSON NULL,
    recommendation_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_organization_trial_policy(policy_key),
    KEY idx_organization_trial_policy_scope(policy_scope, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Organization trial policy';

CREATE TABLE IF NOT EXISTS release_guardrail_evaluation (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    policy_key VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NOT NULL,
    guardrail_category VARCHAR(64) NOT NULL,
    evaluation_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actual_value DECIMAL(18,6) NULL,
    threshold_value DECIMAL(18,6) NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    recommendation_text TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_guardrail_eval_date(snapshot_date, project_id),
    KEY idx_release_guardrail_eval_policy(policy_key, evaluation_status),
    KEY idx_release_guardrail_eval_severity(snapshot_date, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release guardrail evaluation';

CREATE TABLE IF NOT EXISTS portfolio_drift_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    drift_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    drift_level VARCHAR(32) NOT NULL,
    baseline_template_key VARCHAR(64) NULL,
    confidence_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    signoff_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    verification_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    rollback_readiness_changed TINYINT NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_portfolio_drift_snapshot_date(snapshot_date, drift_score),
    KEY idx_portfolio_drift_snapshot_project(project_id, snapshot_date),
    KEY idx_portfolio_drift_snapshot_level(snapshot_date, drift_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Portfolio drift snapshot';
