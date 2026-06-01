CREATE TABLE IF NOT EXISTS governance_benchmark_evolution_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, benchmark_type VARCHAR(32) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, current_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    previous_value DECIMAL(10,2) NOT NULL DEFAULT 0, delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    delta_percentage DECIMAL(10,2) NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    sample_count INT NOT NULL DEFAULT 0, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_evolution_snapshot_date(snapshot_date, benchmark_type),
    KEY idx_gov_evolution_snapshot_metric(metric_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance benchmark evolution snapshot';

CREATE TABLE IF NOT EXISTS governance_campaign_effectiveness_ranking (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, campaign_key VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(255) NOT NULL, ranking_window VARCHAR(32) NOT NULL,
    avg_uplift DECIMAL(10,2) NOT NULL DEFAULT 0, project_count INT NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL, rank_position INT NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_campaign_ranking_date(snapshot_date, rank_position),
    KEY idx_gov_campaign_ranking_effectiveness(effectiveness_level, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance campaign effectiveness ranking';

CREATE TABLE IF NOT EXISTS governance_progress_map_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, metric_key VARCHAR(64) NOT NULL,
    baseline_score DECIMAL(10,2) NOT NULL DEFAULT 0, current_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    target_score DECIMAL(10,2) NOT NULL DEFAULT 0, progress_percentage DECIMAL(10,2) NOT NULL DEFAULT 0,
    signal_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_progress_map_snapshot_date(snapshot_date, project_id),
    KEY idx_gov_progress_map_snapshot_signal(signal_level, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance progress map snapshot';
