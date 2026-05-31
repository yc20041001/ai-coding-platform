CREATE TABLE IF NOT EXISTS governance_draft_optimization_signal (
    id BIGINT PRIMARY KEY, signal_type VARCHAR(64) NOT NULL, scope_type VARCHAR(64) NOT NULL,
    scope_key VARCHAR(128) NULL, adoption_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    rejection_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_usefulness_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    sample_count INT NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    suggestion_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_draft_optimization_signal_type(signal_type, captured_at),
    KEY idx_gov_draft_optimization_scope(scope_type, scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft optimization signal';

CREATE TABLE IF NOT EXISTS governance_assistive_ordering_optimization (
    id BIGINT PRIMARY KEY, action_type VARCHAR(64) NOT NULL, avg_usefulness_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_action_order DECIMAL(10,2) NOT NULL DEFAULT 0, usefulness_count INT NOT NULL DEFAULT 0,
    not_useful_count INT NOT NULL DEFAULT 0, optimization_level VARCHAR(32) NOT NULL,
    suggested_new_order INT NOT NULL DEFAULT 0, rationale_text TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_assistive_ordering_optimization_type(action_type, captured_at),
    KEY idx_gov_assistive_ordering_optimization_level(optimization_level, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance assistive ordering optimization';

CREATE TABLE IF NOT EXISTS governance_package_composition_tuning (
    id BIGINT PRIMARY KEY, score_range VARCHAR(32) NOT NULL, avg_completeness DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_accuracy DECIMAL(10,2) NOT NULL DEFAULT 0, avg_overall DECIMAL(10,2) NOT NULL DEFAULT 0,
    sample_count INT NOT NULL DEFAULT 0, tuning_level VARCHAR(32) NOT NULL,
    suggestion_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_package_composition_tuning_range(score_range, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance package composition tuning';
