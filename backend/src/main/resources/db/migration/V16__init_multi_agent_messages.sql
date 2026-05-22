CREATE TABLE IF NOT EXISTS multi_agent_message (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    from_step_id BIGINT NULL,
    to_step_id BIGINT NULL,
    from_agent_id BIGINT NULL,
    to_agent_id BIGINT NULL,
    message_type VARCHAR(64) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    summary TEXT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_multi_agent_message_run_time (run_id, create_time),
    INDEX idx_multi_agent_message_task (task_id),
    INDEX idx_multi_agent_message_from_step (from_step_id),
    INDEX idx_multi_agent_message_to_step (to_step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 消息传递记录';
