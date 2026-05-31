CREATE TABLE IF NOT EXISTS governance_recommendation_playbook_template (
    id BIGINT PRIMARY KEY, template_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL, guardrail_key VARCHAR(64) NULL, priority VARCHAR(32) NULL,
    enabled TINYINT NOT NULL DEFAULT 1, template_steps_json JSON NOT NULL,
    success_criteria_json JSON NULL, handoff_notes TEXT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_playbook_template(template_key),
    KEY idx_governance_playbook_template_match(recommendation_category, guardrail_key, priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation playbook template';

CREATE TABLE IF NOT EXISTS governance_recommendation_execution_plan (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
    plan_status VARCHAR(32) NOT NULL, template_key VARCHAR(64) NULL,
    owner_id BIGINT NULL, owner_name VARCHAR(128) NULL, due_at DATETIME NULL,
    steps_json JSON NOT NULL, completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_execution_plan_recommendation(recommendation_id),
    KEY idx_gov_execution_plan_project(project_id, plan_status),
    KEY idx_gov_execution_plan_due(due_at, plan_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation execution plan';

CREATE TABLE IF NOT EXISTS governance_handoff_checklist (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NOT NULL, execution_plan_id BIGINT NULL,
    from_owner_id BIGINT NULL, from_owner_name VARCHAR(128) NULL,
    to_owner_id BIGINT NULL, to_owner_name VARCHAR(128) NULL,
    checklist_status VARCHAR(32) NOT NULL, checklist_items_json JSON NOT NULL,
    handoff_note TEXT NULL, handed_off_at DATETIME NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_handoff_recommendation(recommendation_id),
    KEY idx_gov_handoff_plan(execution_plan_id),
    KEY idx_gov_handoff_status(checklist_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance handoff checklist';
