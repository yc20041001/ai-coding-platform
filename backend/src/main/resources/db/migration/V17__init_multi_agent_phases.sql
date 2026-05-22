CREATE TABLE IF NOT EXISTS multi_agent_phase (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    phase_order INT NOT NULL,
    phase_key VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_summary TEXT NULL,
    output_summary TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_multi_agent_phase_run_order (run_id, phase_order),
    INDEX idx_multi_agent_phase_task (task_id),
    INDEX idx_multi_agent_phase_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 编排阶段';

ALTER TABLE multi_agent_step
    ADD COLUMN phase_id BIGINT NULL AFTER run_id,
    ADD COLUMN phase_order INT NULL AFTER phase_id,
    ADD COLUMN lane_key VARCHAR(64) NULL AFTER phase_order,
    ADD INDEX idx_multi_agent_step_phase_order (phase_id, step_order);
