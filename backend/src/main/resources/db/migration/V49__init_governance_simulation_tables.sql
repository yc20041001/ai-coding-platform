CREATE TABLE IF NOT EXISTS governance_simulation_scenario (
    id BIGINT PRIMARY KEY, scenario_name VARCHAR(255) NOT NULL, scenario_type VARCHAR(64) NOT NULL,
    baseline_snapshot_date DATE NULL, scenario_status VARCHAR(32) NOT NULL,
    input_json JSON NOT NULL, notes TEXT NULL, created_by BIGINT NULL,
    created_by_name VARCHAR(128) NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_sim_scenario_type(scenario_type, scenario_status),
    KEY idx_gov_sim_scenario_date(baseline_snapshot_date, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance simulation scenario';

CREATE TABLE IF NOT EXISTS governance_simulation_result (
    id BIGINT PRIMARY KEY, scenario_id BIGINT NOT NULL, result_status VARCHAR(32) NOT NULL,
    impacted_owner_count INT NOT NULL DEFAULT 0, impacted_project_count INT NOT NULL DEFAULT 0,
    projected_backlog_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_overdue_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_risk_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_capacity_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, detail_json JSON NULL,
    report_markdown MEDIUMTEXT NULL, calculated_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_sim_result_scenario(scenario_id),
    KEY idx_gov_sim_result_status(result_status, calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance simulation result';

CREATE TABLE IF NOT EXISTS policy_tuning_suggestion (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL, target_scope VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NULL, current_value VARCHAR(255) NULL,
    suggested_value VARCHAR(255) NULL, expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL, evidence_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_policy_tuning_suggestion_date(snapshot_date, priority),
    KEY idx_policy_tuning_suggestion_type(suggestion_type, target_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Policy tuning suggestion';
