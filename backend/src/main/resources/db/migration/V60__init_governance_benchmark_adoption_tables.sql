CREATE TABLE IF NOT EXISTS governance_benchmark_adoption_record (
    id BIGINT PRIMARY KEY, project_id BIGINT NOT NULL, project_name VARCHAR(255) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, adoption_status VARCHAR(32) NOT NULL,
    current_score DECIMAL(10,2) NOT NULL DEFAULT 0, target_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    blocker_type VARCHAR(64) NULL, blocker_note TEXT NULL, owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL, adopted_at DATETIME NULL, create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_gov_benchmark_adoption_project(project_id, adoption_status),
    KEY idx_gov_benchmark_adoption_metric(metric_key, adoption_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance benchmark adoption record';

CREATE TABLE IF NOT EXISTS governance_cross_team_improvement_campaign (
    id BIGINT PRIMARY KEY, campaign_key VARCHAR(64) NOT NULL, campaign_name VARCHAR(255) NOT NULL,
    campaign_status VARCHAR(32) NOT NULL, target_project_ids_json JSON NOT NULL,
    source_project_id BIGINT NULL, source_practice_type VARCHAR(64) NULL,
    improvement_window VARCHAR(32) NOT NULL, goal_text TEXT NULL, notes_text TEXT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_improvement_campaign_key(campaign_key),
    KEY idx_gov_improvement_campaign_status(campaign_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance cross-team improvement campaign';

CREATE TABLE IF NOT EXISTS governance_uplift_measurement_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, campaign_key VARCHAR(64) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, before_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    after_score DECIMAL(10,2) NOT NULL DEFAULT 0, uplift DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_gov_uplift_snapshot_project(project_id, snapshot_date),
    KEY idx_gov_uplift_snapshot_campaign(campaign_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance uplift measurement snapshot';
