CREATE TABLE IF NOT EXISTS governance_portfolio_benchmark_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, benchmark_window VARCHAR(32) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, metric_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    percentile_rank DECIMAL(8,2) NOT NULL DEFAULT 0, peer_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    peer_p90 DECIMAL(10,2) NOT NULL DEFAULT 0, sample_count INT NOT NULL DEFAULT 0,
    signal_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_benchmark_snapshot_date(snapshot_date, metric_key),
    KEY idx_gov_benchmark_snapshot_window(benchmark_window, metric_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance portfolio benchmark snapshot';

CREATE TABLE IF NOT EXISTS governance_best_practice_alignment_item (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, practice_type VARCHAR(64) NOT NULL,
    alignment_level VARCHAR(32) NOT NULL, current_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    target_score DECIMAL(10,2) NOT NULL DEFAULT 0, gap DECIMAL(10,2) NOT NULL DEFAULT 0,
    suggestion_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_best_practice_alignment_project(project_id, snapshot_date),
    KEY idx_gov_best_practice_alignment_practice(practice_type, alignment_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance best practice alignment item';

CREATE TABLE IF NOT EXISTS governance_maturity_scorecard (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, maturity_level VARCHAR(32) NOT NULL,
    total_score DECIMAL(8,2) NOT NULL DEFAULT 0, draft_adoption_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    assistive_quality_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    package_quality_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    outcome_review_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    operator_productivity_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_maturity_scorecard_date(snapshot_date, maturity_level),
    KEY idx_gov_maturity_scorecard_project(project_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance maturity scorecard';
