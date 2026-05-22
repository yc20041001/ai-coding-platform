-- V22: Tool Execution Approval tables + seed HIGH risk tool

CREATE TABLE IF NOT EXISTS tool_execution_approval (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    step_id BIGINT NULL,
    tool_execution_id BIGINT NOT NULL,
    tool_id BIGINT NULL,
    tool_key VARCHAR(64) NOT NULL,
    approval_key VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by BIGINT NULL,
    decided_by BIGINT NULL,
    decision_comment TEXT NULL,
    requested_at DATETIME NULL,
    decided_at DATETIME NULL,
    expires_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_approval_project_status(project_id, status),
    INDEX idx_tool_approval_task(task_id),
    INDEX idx_tool_approval_run(run_id),
    INDEX idx_tool_approval_step(step_id),
    INDEX idx_tool_approval_execution(tool_execution_id),
    INDEX idx_tool_approval_tool(tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行审批表';

-- Seed HIGH risk tool MOCK_PATCH_PROPOSAL (910006)
INSERT INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, policy_json, create_time, update_time) VALUES
(910006, 'MOCK_PATCH_PROPOSAL', 'Mock 补丁方案生成', '生成 Mock 补丁方案，不产生真实 patch 文件。需人工审批后执行。', 'ANALYSIS', 'HIGH', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false,"requiresApproval":true}', NOW(), NOW());
