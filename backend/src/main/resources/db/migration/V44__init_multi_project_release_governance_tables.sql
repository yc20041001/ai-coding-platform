CREATE TABLE IF NOT EXISTS release_portfolio_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    latest_release_label VARCHAR(128) NULL,
    confidence_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    confidence_level VARCHAR(32) NOT NULL,
    rollout_status VARCHAR(32) NULL,
    decision_status VARCHAR(32) NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    open_incident_count INT NOT NULL DEFAULT 0,
    active_alert_count INT NOT NULL DEFAULT 0,
    failed_verification_count INT NOT NULL DEFAULT 0,
    rollback_ready TINYINT NOT NULL DEFAULT 0,
    signoff_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    portfolio_rank INT NULL,
    expansion_recommendation VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_portfolio_snapshot_date(snapshot_date, confidence_score),
    KEY idx_release_portfolio_snapshot_project(project_id, snapshot_date),
    KEY idx_release_portfolio_snapshot_rank(snapshot_date, portfolio_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release portfolio snapshot';

CREATE TABLE IF NOT EXISTS governance_baseline_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    template_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    default_signoff_roles_json JSON NULL,
    default_verification_rules_json JSON NULL,
    default_rollback_requirements_json JSON NULL,
    default_confidence_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_baseline_template(template_key),
    KEY idx_governance_baseline_template_scope(template_scope, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance baseline template';

CREATE TABLE IF NOT EXISTS release_risk_heatmap_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    risk_category VARCHAR(64) NOT NULL,
    risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    risk_level VARCHAR(32) NOT NULL,
    source_count INT NOT NULL DEFAULT 0,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_risk_heatmap_date(snapshot_date, risk_category),
    KEY idx_release_risk_heatmap_project(project_id, snapshot_date),
    KEY idx_release_risk_heatmap_level(snapshot_date, risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release risk heatmap snapshot';
