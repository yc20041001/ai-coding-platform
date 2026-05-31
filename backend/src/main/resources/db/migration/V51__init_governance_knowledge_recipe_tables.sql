CREATE TABLE IF NOT EXISTS governance_knowledge_entry (
    id BIGINT PRIMARY KEY, project_id BIGINT NULL, source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL, title VARCHAR(255) NOT NULL, category VARCHAR(64) NOT NULL,
    tags_json JSON NULL, summary_text TEXT NULL, detail_markdown MEDIUMTEXT NULL,
    effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0, reuse_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_knowledge_entry_category(category),
    KEY idx_gov_knowledge_entry_project(project_id, create_time),
    KEY idx_gov_knowledge_entry_score(effectiveness_score, reuse_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance knowledge entry';

CREATE TABLE IF NOT EXISTS governance_pattern_library_item (
    id BIGINT PRIMARY KEY, pattern_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL, guardrail_key VARCHAR(64) NULL,
    priority VARCHAR(32) NULL, pattern_json JSON NOT NULL, notes TEXT NULL,
    enabled TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_pattern_library(pattern_key),
    KEY idx_governance_pattern_library_match(recommendation_category, guardrail_key, priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance pattern library item';

CREATE TABLE IF NOT EXISTS governance_remediation_recipe (
    id BIGINT PRIMARY KEY, recipe_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recipe_type VARCHAR(64) NOT NULL, recommendation_category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL, steps_json JSON NOT NULL, prerequisites_json JSON NULL,
    success_criteria_json JSON NULL, effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0, enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_remediation_recipe(recipe_key),
    KEY idx_governance_remediation_recipe_match(recipe_type, recommendation_category, guardrail_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance remediation recipe';
