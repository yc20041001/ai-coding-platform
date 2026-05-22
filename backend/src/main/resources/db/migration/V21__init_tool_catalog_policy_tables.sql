-- V21: Tool Catalog + Project Tool Config tables

CREATE TABLE IF NOT EXISTS tool_catalog (
    id BIGINT PRIMARY KEY,
    tool_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT NULL,
    tool_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    built_in TINYINT NOT NULL DEFAULT 1,
    policy_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_tool_catalog_key(tool_key),
    INDEX idx_tool_catalog_type(tool_type),
    INDEX idx_tool_catalog_risk(risk_level),
    INDEX idx_tool_catalog_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具目录表';

CREATE TABLE IF NOT EXISTS project_tool_config (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    tool_id BIGINT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    config_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_project_tool(project_id, tool_id),
    INDEX idx_project_tool_project(project_id),
    INDEX idx_project_tool_tool(tool_id),
    INDEX idx_project_tool_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工具配置表';

-- Seed 5 built-in read-only tools
INSERT INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, policy_json, create_time, update_time) VALUES
(910001, 'PROJECT_CONTEXT_SCAN', '项目上下文扫描', '只读扫描项目代码结构和依赖，不修改任何文件。', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["ARCHITECTURE_ANALYSIS","FINAL_SUMMARY"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910002, 'TASK_REQUIREMENT_ANALYSIS', '任务需求分析', '分析任务需求描述，生成结构化分析结果。', 'ANALYSIS', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","TEST_PLAN"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910003, 'MOCK_FILE_INSPECTION', 'Mock 文件检查', '模拟文件检查工具，扫描项目文件结构和质量。', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["FRONTEND_IMPLEMENTATION_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910004, 'MOCK_TEST_PLAN_SCAN', 'Mock 测试计划扫描', '扫描和验证测试计划的完整性。', 'ANALYSIS', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["TEST_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910005, 'MOCK_SECURITY_REVIEW', 'Mock 安全审查', '模拟安全审查，检查代码安全性和合规性。', 'ANALYSIS', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW());
