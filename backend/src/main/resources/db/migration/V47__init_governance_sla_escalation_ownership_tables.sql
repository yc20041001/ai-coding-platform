CREATE TABLE IF NOT EXISTS governance_sla_policy (
    id BIGINT PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    category VARCHAR(64) NULL,
    sla_hours INT NOT NULL,
    warning_hours INT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_sla_policy(policy_key),
    KEY idx_governance_sla_policy_priority(priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance SLA policy';

CREATE TABLE IF NOT EXISTS governance_escalation_event (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    escalation_type VARCHAR(64) NOT NULL,
    escalation_level VARCHAR(32) NOT NULL,
    event_status VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL,
    triggered_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_escalation_recommendation(recommendation_id, triggered_at),
    KEY idx_governance_escalation_project(project_id, triggered_at),
    KEY idx_governance_escalation_status(event_status, escalation_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance escalation event';

CREATE TABLE IF NOT EXISTS governance_ownership_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    owner_id BIGINT NOT NULL,
    owner_name VARCHAR(128) NOT NULL,
    total_assigned_count INT NOT NULL DEFAULT 0,
    open_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0,
    completed_7d_count INT NOT NULL DEFAULT 0,
    active_waiver_count INT NOT NULL DEFAULT 0,
    owner_health_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    owner_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_ownership_snapshot_date(snapshot_date, owner_health_score),
    KEY idx_governance_ownership_snapshot_owner(owner_id, snapshot_date),
    KEY idx_governance_ownership_snapshot_level(snapshot_date, owner_health_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance ownership snapshot';
