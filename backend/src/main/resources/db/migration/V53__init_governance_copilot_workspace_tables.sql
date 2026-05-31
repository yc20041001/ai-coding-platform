CREATE TABLE IF NOT EXISTS governance_workspace_session (
    id BIGINT PRIMARY KEY, operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    session_status VARCHAR(32) NOT NULL, focus_mode VARCHAR(32) NOT NULL,
    selected_project_id BIGINT NULL, selected_recommendation_id BIGINT NULL,
    selected_owner_id BIGINT NULL, context_summary TEXT NULL,
    started_at DATETIME NOT NULL, ended_at DATETIME NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_workspace_session_operator(operator_id, session_status),
    KEY idx_gov_workspace_session_project(selected_project_id, session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance workspace session';

CREATE TABLE IF NOT EXISTS governance_guided_task (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, recommendation_id BIGINT NULL,
    task_type VARCHAR(64) NOT NULL, priority VARCHAR(32) NOT NULL, task_status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL, summary TEXT NULL, source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL, linked_playbook_key VARCHAR(64) NULL,
    linked_recipe_key VARCHAR(64) NULL, linked_knowledge_entry_id BIGINT NULL,
    due_at DATETIME NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_guided_task_session(session_id, task_status),
    KEY idx_gov_guided_task_priority(priority, task_status),
    KEY idx_gov_guided_task_recommendation(recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance guided task';

CREATE TABLE IF NOT EXISTS governance_next_step_recommendation (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL, suggestion_rank INT NOT NULL DEFAULT 0,
    suggestion_type VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL,
    summary_text TEXT NULL, rationale_text TEXT NULL, expected_outcome_text TEXT NULL,
    action_payload_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_next_step_session(session_id, suggestion_rank),
    KEY idx_gov_next_step_task(guided_task_id),
    KEY idx_gov_next_step_recommendation(recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance next step recommendation';
