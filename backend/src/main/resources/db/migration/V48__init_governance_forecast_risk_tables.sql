CREATE TABLE IF NOT EXISTS governance_capacity_forecast (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, forecast_horizon_days INT NOT NULL,
    owner_id BIGINT NOT NULL, owner_name VARCHAR(128) NOT NULL,
    current_open_count INT NOT NULL DEFAULT 0, current_overdue_count INT NOT NULL DEFAULT 0,
    avg_completed_per_day DECIMAL(8,2) NOT NULL DEFAULT 0, projected_new_items INT NOT NULL DEFAULT 0,
    projected_completed_items INT NOT NULL DEFAULT 0, projected_backlog_count INT NOT NULL DEFAULT 0,
    projected_overdue_count INT NOT NULL DEFAULT 0, capacity_risk_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_capacity_forecast_date(snapshot_date, forecast_horizon_days),
    KEY idx_gov_capacity_forecast_owner(owner_id, snapshot_date),
    KEY idx_gov_capacity_forecast_level(snapshot_date, capacity_risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance capacity forecast';

CREATE TABLE IF NOT EXISTS predictive_risk_signal (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NULL, target_name VARCHAR(255) NOT NULL, signal_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL, risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    probability_score DECIMAL(8,2) NOT NULL DEFAULT 0, time_horizon_days INT NOT NULL DEFAULT 7,
    summary VARCHAR(255) NOT NULL, detail TEXT NULL, evidence_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_predictive_risk_signal_date(snapshot_date, risk_level),
    KEY idx_predictive_risk_signal_target(target_type, target_id, snapshot_date),
    KEY idx_predictive_risk_signal_type(signal_type, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Predictive risk signal';

CREATE TABLE IF NOT EXISTS governance_backlog_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, open_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0, blocked_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0, waiver_active_count INT NOT NULL DEFAULT 0,
    incoming_7d_count INT NOT NULL DEFAULT 0, completed_7d_count INT NOT NULL DEFAULT 0,
    backlog_growth_rate DECIMAL(8,2) NOT NULL DEFAULT 0, backlog_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_backlog_snapshot_date(snapshot_date, backlog_growth_rate),
    KEY idx_gov_backlog_snapshot_project(project_id, snapshot_date),
    KEY idx_gov_backlog_snapshot_level(snapshot_date, backlog_health_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance backlog snapshot';
