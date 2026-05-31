CREATE TABLE IF NOT EXISTS governance_draft_adoption_review (
    id BIGINT PRIMARY KEY, draft_plan_id BIGINT NOT NULL, recommendation_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    adoption_result VARCHAR(32) NOT NULL, modification_level VARCHAR(32) NOT NULL,
    usefulness_rating INT NOT NULL, reason_code VARCHAR(64) NULL,
    outcome_note_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_draft_adoption_review_plan(draft_plan_id),
    KEY idx_gov_draft_adoption_review_result(adoption_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft adoption review';

CREATE TABLE IF NOT EXISTS governance_assistive_action_quality_review (
    id BIGINT PRIMARY KEY, assistive_action_id BIGINT NOT NULL, draft_plan_id BIGINT NOT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    outcome_result VARCHAR(32) NOT NULL, usefulness_rating INT NOT NULL,
    reason_code VARCHAR(64) NULL, feedback_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_assistive_quality_review_action(assistive_action_id),
    KEY idx_gov_assistive_quality_review_plan(draft_plan_id),
    KEY idx_gov_assistive_quality_review_result(outcome_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance assistive action quality review';

CREATE TABLE IF NOT EXISTS governance_package_review_evaluation (
    id BIGINT PRIMARY KEY, package_id BIGINT NOT NULL, draft_plan_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    evaluation_result VARCHAR(32) NOT NULL, completeness_score INT NOT NULL,
    accuracy_score INT NOT NULL, overall_score INT NOT NULL,
    reason_code VARCHAR(64) NULL, review_notes_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_package_review_evaluation_package(package_id),
    KEY idx_gov_package_review_evaluation_result(evaluation_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance package review evaluation';
