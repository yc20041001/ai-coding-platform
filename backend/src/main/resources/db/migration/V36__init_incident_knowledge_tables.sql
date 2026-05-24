CREATE TABLE tool_incident_root_cause_note (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause TEXT NULL,
    impact TEXT NULL,
    resolution TEXT NULL,
    prevention TEXT NULL,
    follow_up_actions TEXT NULL,
    tags VARCHAR(512) NULL,
    confidence VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    author_id BIGINT NOT NULL,
    last_editor_id BIGINT NULL,
    published_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_incident_rca_project_time(project_id, create_time),
    KEY idx_incident_rca_incident(incident_id),
    KEY idx_incident_rca_status(status),
    KEY idx_incident_rca_author(author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件根因分析记录表';

CREATE TABLE tool_known_issue_template (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    root_cause_template TEXT NULL,
    impact_template TEXT NULL,
    resolution_template TEXT NULL,
    prevention_template TEXT NULL,
    tags VARCHAR(512) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_known_issue_project(project_id),
    KEY idx_known_issue_category(category),
    KEY idx_known_issue_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已知问题模板表';

CREATE TABLE tool_incident_knowledge_link (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause_note_id BIGINT NULL,
    knowledge_base_id BIGINT NULL,
    knowledge_document_id BIGINT NULL,
    link_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_incident_knowledge_project(project_id),
    KEY idx_incident_knowledge_incident(incident_id),
    KEY idx_incident_knowledge_document(knowledge_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件知识库关联表';
