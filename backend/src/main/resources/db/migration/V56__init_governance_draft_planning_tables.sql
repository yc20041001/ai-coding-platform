CREATE TABLE IF NOT EXISTS governance_draft_remediation_plan (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NULL, session_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    plan_status VARCHAR(32) NOT NULL, plan_title VARCHAR(255) NOT NULL,
    scope_type VARCHAR(64) NOT NULL, summary_text TEXT NULL, goal_text TEXT NULL,
    proposed_steps_json JSON NOT NULL, linked_bundle_id BIGINT NULL,
    linked_playbook_key VARCHAR(64) NULL, linked_recipe_key VARCHAR(64) NULL,
    risk_level VARCHAR(32) NOT NULL, human_confirmation_required TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_draft_plan_recommendation(recommendation_id, plan_status),
    KEY idx_gov_draft_plan_session(session_id, plan_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft remediation plan';

CREATE TABLE IF NOT EXISTS governance_safe_assistive_action (
    id BIGINT PRIMARY KEY, draft_plan_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL, action_status VARCHAR(32) NOT NULL,
    action_title VARCHAR(255) NOT NULL, action_summary TEXT NULL,
    safety_level VARCHAR(32) NOT NULL, confirmation_required TINYINT NOT NULL DEFAULT 1,
    checklist_json JSON NOT NULL, prefill_payload_json JSON NULL,
    action_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_safe_assistive_action_plan(draft_plan_id, action_order),
    KEY idx_gov_safe_assistive_action_status(action_status, safety_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance safe assistive action';

CREATE TABLE IF NOT EXISTS governance_recommendation_package (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NULL, draft_plan_id BIGINT NULL,
    package_status VARCHAR(32) NOT NULL, package_title VARCHAR(255) NOT NULL,
    package_summary TEXT NULL, recommendation_context_json JSON NOT NULL,
    attachments_json JSON NULL, review_notes_text TEXT NULL,
    submit_ready_flag TINYINT NOT NULL DEFAULT 0, submitted_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_recommendation_package_recommendation(recommendation_id, package_status),
    KEY idx_gov_recommendation_package_plan(draft_plan_id, package_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation package';
