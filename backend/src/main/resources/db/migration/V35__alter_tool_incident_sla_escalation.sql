ALTER TABLE tool_incident
  ADD COLUMN sla_minutes INT NULL,
  ADD COLUMN due_at DATETIME NULL,
  ADD COLUMN breached_at DATETIME NULL,
  ADD COLUMN sla_status VARCHAR(32) NULL,
  ADD COLUMN escalation_level INT NOT NULL DEFAULT 0;

CREATE INDEX idx_tool_incident_sla_status ON tool_incident(sla_status);
CREATE INDEX idx_tool_incident_due_at ON tool_incident(due_at);

CREATE TABLE tool_escalation_policy (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    severity VARCHAR(32) NOT NULL,
    sla_minutes INT NULL,
    escalation_after_minutes INT NULL,
    max_escalation_level INT NOT NULL DEFAULT 3,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_escalation_policy_project(project_id),
    KEY idx_tool_escalation_policy_enabled(enabled),
    KEY idx_tool_escalation_policy_severity(severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具升级策略表';

CREATE TABLE tool_escalation_event (
    id BIGINT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    policy_id BIGINT NULL,
    escalation_level INT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    reason TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_tool_escalation_event_incident(incident_id),
    KEY idx_tool_escalation_event_project_time(project_id, create_time),
    KEY idx_tool_escalation_event_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具升级事件表';
