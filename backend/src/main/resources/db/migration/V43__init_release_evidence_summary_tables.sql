CREATE TABLE IF NOT EXISTS release_evidence_bundle (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    bundle_status VARCHAR(32) NOT NULL,
    summary_markdown MEDIUMTEXT NULL,
    evidence_json JSON NULL,
    generated_by BIGINT NULL,
    generated_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_evidence_bundle_project(project_id, create_time),
    KEY idx_release_evidence_bundle_plan(plan_id),
    KEY idx_release_evidence_bundle_status(bundle_status),
    UNIQUE KEY uk_release_evidence_bundle(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布证据包';

CREATE TABLE IF NOT EXISTS release_signoff_record (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    signoff_role VARCHAR(64) NOT NULL,
    signoff_status VARCHAR(32) NOT NULL,
    signer_id BIGINT NULL,
    signer_name VARCHAR(128) NULL,
    comment_text TEXT NULL,
    signed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_release_signoff_project(project_id, create_time),
    KEY idx_release_signoff_plan(plan_id, signoff_role),
    KEY idx_release_signoff_status(signoff_status),
    UNIQUE KEY uk_release_signoff(plan_id, signoff_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布签字记录';

CREATE TABLE IF NOT EXISTS release_confidence_snapshot (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    release_label VARCHAR(128) NOT NULL,
    confidence_score DECIMAL(8,2) NOT NULL,
    confidence_level VARCHAR(32) NOT NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    open_incident_count INT NOT NULL DEFAULT 0,
    active_alert_count INT NOT NULL DEFAULT 0,
    failed_verification_count INT NOT NULL DEFAULT 0,
    rollback_ready TINYINT NOT NULL DEFAULT 0,
    signoff_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    snapshot_summary VARCHAR(255) NOT NULL,
    snapshot_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_confidence_project(project_id, snapshot_time),
    KEY idx_release_confidence_plan(plan_id),
    KEY idx_release_confidence_level(confidence_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布信心快照';
