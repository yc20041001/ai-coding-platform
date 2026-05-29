-- V41: Beta-to-Production Readiness & Controlled Rollout
-- Creates 3 tables for release rollout management
-- Depends on: V38 (beta_trial_session, beta_trial_feedback, beta_environment_readiness)
-- Depends on: V39 (model_cost_summary, model_cost_alert, pr_review_quality_record)
-- Depends on: V40 (beta_release_gate_rule, beta_release_gate_evaluation, beta_release_decision)

CREATE TABLE release_rollout_plan (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    source_decision_id BIGINT NULL,
    rollout_status VARCHAR(32) NOT NULL,
    rollout_strategy VARCHAR(32) NOT NULL,
    target_environment VARCHAR(64) NOT NULL,
    owner_id BIGINT NULL,
    approver_id BIGINT NULL,
    planned_start_at DATETIME NULL,
    planned_end_at DATETIME NULL,
    observation_window_minutes INT NOT NULL DEFAULT 60,
    rollback_trigger_summary TEXT NULL,
    success_criteria_summary TEXT NULL,
    readiness_summary TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_rollout_plan_project (project_id, create_time),
    KEY idx_release_rollout_plan_status (rollout_status),
    UNIQUE KEY uk_release_rollout_label (project_id, release_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_rollout_step (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    step_order INT NOT NULL,
    step_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    step_status VARCHAR(32) NOT NULL,
    verification_scope VARCHAR(32) NOT NULL,
    required TINYINT NOT NULL DEFAULT 1,
    blocking TINYINT NOT NULL DEFAULT 1,
    instructions TEXT NULL,
    expected_result TEXT NULL,
    actual_result TEXT NULL,
    evidence_json JSON NULL,
    operator_id BIGINT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_rollout_step_plan (plan_id, step_order),
    KEY idx_release_rollout_step_status (step_status),
    UNIQUE KEY uk_release_rollout_step (plan_id, step_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_verification_record (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    verification_phase VARCHAR(32) NOT NULL,
    verification_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    verification_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    evidence_json JSON NULL,
    related_incident_id BIGINT NULL,
    related_alert_id BIGINT NULL,
    recorded_by BIGINT NULL,
    recorded_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_verification_plan (plan_id, verification_phase, recorded_at),
    KEY idx_release_verification_status (verification_status, severity),
    KEY idx_release_verification_project (project_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
