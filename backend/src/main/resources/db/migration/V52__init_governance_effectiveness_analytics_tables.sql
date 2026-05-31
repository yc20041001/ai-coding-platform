CREATE TABLE IF NOT EXISTS governance_recipe_effectiveness_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, recipe_id BIGINT NOT NULL,
    recipe_key VARCHAR(64) NOT NULL, recipe_name VARCHAR(255) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0, completion_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(8,2) NOT NULL DEFAULT 0, avg_completion_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    failure_rate DECIMAL(8,2) NOT NULL DEFAULT 0, effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_governance_recipe_effectiveness_date(snapshot_date, effectiveness_score),
    KEY idx_governance_recipe_effectiveness_recipe(recipe_id, snapshot_date),
    KEY idx_governance_recipe_effectiveness_level(snapshot_date, effectiveness_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recipe effectiveness snapshot';

CREATE TABLE IF NOT EXISTS governance_playbook_analytics_record (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, template_key VARCHAR(64) NOT NULL,
    template_name VARCHAR(255) NOT NULL, plan_count INT NOT NULL DEFAULT 0,
    completed_plan_count INT NOT NULL DEFAULT 0, blocked_plan_count INT NOT NULL DEFAULT 0,
    avg_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_step_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_resolution_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    related_recipe_count INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL,
    KEY idx_governance_playbook_analytics_date(snapshot_date, avg_completion_rate),
    KEY idx_governance_playbook_analytics_template(template_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance playbook analytics record';

CREATE TABLE IF NOT EXISTS governance_optimization_suggestion (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL, target_type VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NOT NULL, current_metric_value VARCHAR(255) NULL,
    suggested_action TEXT NOT NULL, expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_governance_optimization_suggestion_date(snapshot_date, priority),
    KEY idx_governance_optimization_suggestion_target(target_type, target_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance optimization suggestion';
