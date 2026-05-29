-- V40: Beta Release Gate & Go/No-Go Decision Center
-- Creates 3 tables for release gate evaluation and release decision
-- Depends on: V38 (beta_trial_session, beta_trial_feedback, beta_environment_readiness)
-- Depends on: V39 (model_cost_summary, model_cost_alert, pr_review_quality_record)

CREATE TABLE beta_release_gate_rule (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    rule_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    threshold_operator VARCHAR(16) NOT NULL,
    threshold_value DECIMAL(18,6) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    blocking TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_gate_rule_project (project_id),
    KEY idx_beta_gate_rule_category (category, enabled),
    UNIQUE KEY uk_beta_gate_rule (project_id, rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE beta_release_gate_evaluation (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    evaluation_target VARCHAR(128) NOT NULL,
    evaluation_type VARCHAR(32) NOT NULL,
    rule_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    gate_status VARCHAR(32) NOT NULL,
    actual_value DECIMAL(18,6) NULL,
    threshold_value DECIMAL(18,6) NULL,
    blocking TINYINT NOT NULL DEFAULT 1,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    evidence_json JSON NULL,
    evaluated_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_gate_eval_project (project_id, evaluated_at),
    KEY idx_beta_gate_eval_target (evaluation_target, evaluation_type),
    KEY idx_beta_gate_eval_status (gate_status, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE beta_release_decision (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    decision_status VARCHAR(32) NOT NULL,
    decision_reason TEXT NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    approver_id BIGINT NULL,
    approved_at DATETIME NULL,
    report_markdown MEDIUMTEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_beta_release_decision_project (project_id, create_time),
    KEY idx_beta_release_decision_status (decision_status),
    UNIQUE KEY uk_beta_release_label (project_id, release_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed built-in gate rules for all projects (project_id = NULL means global)
INSERT INTO beta_release_gate_rule (id, project_id, rule_key, category, display_name, threshold_operator, threshold_value, enabled, blocking, sort_order, description, create_time, update_time) VALUES
(920001, NULL, 'P0_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', 'P0 反馈数', 'EQ', 0, 1, 1, 1, 'P0 反馈数量必须为 0', NOW(), NOW()),
(920002, NULL, 'P1_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', 'P1 反馈数', 'LTE', 3, 1, 1, 2, 'P1 反馈数量不超过 3 个', NOW(), NOW()),
(920003, NULL, 'RELEASE_BLOCKING_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', '阻塞性反馈数', 'EQ', 0, 1, 1, 3, '阻塞性反馈数量必须为 0', NOW(), NOW()),
(920004, NULL, 'READINESS_FAIL_COUNT', 'ENVIRONMENT_READINESS', '环境检查失败数', 'EQ', 0, 1, 1, 4, '环境就绪检查失败数量必须为 0', NOW(), NOW()),
(920005, NULL, 'MODEL_COST_ALERT_HIGH_COUNT', 'MODEL_COST', '成本告警数 (HIGH+)', 'EQ', 0, 1, 0, 5, '高级别成本告警数量必须为 0', NOW(), NOW()),
(920006, NULL, 'PR_REVIEW_FAILURE_RATIO', 'PR_REVIEW_QUALITY', 'PR 评审失败率', 'LTE', 0.20, 1, 0, 6, 'PR 评审失败率不超过 20%', NOW(), NOW()),
(920007, NULL, 'PR_REVIEW_ADOPTION_RATIO', 'PR_REVIEW_QUALITY', 'PR 评审采纳率', 'GTE', 0.30, 1, 0, 7, 'PR 评审建议采纳率不低于 30%', NOW(), NOW()),
(920008, NULL, 'OPEN_CRITICAL_INCIDENT_COUNT', 'INCIDENT_RISK', '未关闭严重事故数', 'EQ', 0, 1, 1, 8, '未关闭的严重事故数量必须为 0', NOW(), NOW()),
(920009, NULL, 'KNOWLEDGE_QUALITY_REJECTED_COUNT', 'KNOWLEDGE_QUALITY', '知识质量被拒数', 'EQ', 0, 1, 0, 9, '知识质量审查被拒数量必须为 0', NOW(), NOW());
