-- Test database schema sync: V10 production hardening fields for model_request_log
-- Applied via spring.sql.init since flyway is disabled in test profile
-- continue-on-error: true handles the case where columns already exist

ALTER TABLE model_request_log
  ADD COLUMN fallback_used TINYINT NOT NULL DEFAULT 0;

ALTER TABLE model_request_log
  ADD COLUMN error_code VARCHAR(64) NULL;

ALTER TABLE model_request_log
  ADD COLUMN estimated_cost DECIMAL(12, 8) NULL;

-- Test database schema sync: V13 execution Agent version tracking
ALTER TABLE agent_execution
  ADD COLUMN agent_version_id BIGINT NULL;

CREATE INDEX idx_agent_execution_agent_version
  ON agent_execution (agent_version_id);

-- Test database schema sync: V14 multi-agent orchestration tables
CREATE TABLE IF NOT EXISTS multi_agent_run (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  strategy VARCHAR(32) NOT NULL DEFAULT 'DEFAULT_MOCK',
  title VARCHAR(255) NULL,
  input_summary TEXT NULL,
  final_summary LONGTEXT NULL,
  error_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_multi_agent_run_task (task_id),
  INDEX idx_multi_agent_run_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS multi_agent_step (
  id BIGINT NOT NULL AUTO_INCREMENT,
  run_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  agent_id BIGINT NULL,
  agent_execution_id BIGINT NULL,
  step_order INT NOT NULL,
  step_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  input_context TEXT NULL,
  output_content LONGTEXT NULL,
  error_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_multi_agent_step_run (run_id),
  INDEX idx_multi_agent_step_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V16 multi-agent message passing
CREATE TABLE IF NOT EXISTS multi_agent_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_multi_agent_message_run_time (run_id, create_time),
  INDEX idx_multi_agent_message_task (task_id),
  INDEX idx_multi_agent_message_from_step (from_step_id),
  INDEX idx_multi_agent_message_to_step (to_step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V17 multi-agent phases + step phase columns
CREATE TABLE IF NOT EXISTS multi_agent_phase (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_multi_agent_phase_run_order (run_id, phase_order),
  INDEX idx_multi_agent_phase_task (task_id),
  INDEX idx_multi_agent_phase_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE multi_agent_step
  ADD COLUMN phase_id BIGINT NULL AFTER run_id,
  ADD COLUMN phase_order INT NULL AFTER phase_id,
  ADD COLUMN lane_key VARCHAR(64) NULL AFTER phase_order,
  ADD INDEX idx_multi_agent_step_phase_order (phase_id, step_order);

-- Test database schema sync: V18 multi-agent approval gates
CREATE TABLE IF NOT EXISTS multi_agent_approval_gate (
  id BIGINT NOT NULL AUTO_INCREMENT,
  run_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  phase_id BIGINT NULL,
  gate_key VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT NULL,
  status VARCHAR(32) NOT NULL,
  requested_by BIGINT NULL,
  decided_by BIGINT NULL,
  decision_comment TEXT NULL,
  requested_at DATETIME NULL,
  decided_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_multi_agent_approval_run (run_id),
  INDEX idx_multi_agent_approval_task (task_id),
  INDEX idx_multi_agent_approval_project_status (project_id, status),
  INDEX idx_multi_agent_approval_decider (decided_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V19 workflow templates
CREATE TABLE IF NOT EXISTS workflow_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT NULL,
  category VARCHAR(64) NOT NULL DEFAULT 'MULTI_AGENT',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  built_in TINYINT NOT NULL DEFAULT 0,
  template_json LONGTEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_workflow_template_key (template_key),
  INDEX idx_workflow_template_status (status),
  INDEX idx_workflow_template_category (category),
  INDEX idx_workflow_template_builtin (built_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed built-in workflow templates (IDs using fixed values for test determinism)
INSERT INTO workflow_template (id, template_key, name, description, category, status, built_in, template_json) VALUES
(900001, 'STANDARD_DELIVERY', '标准交付流程', '架构 → 后端/前端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"STANDARD_DELIVERY","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"IMPLEMENTATION","title":"实现方案并行分析","steps":[{"stepOrder":1,"stepType":"BACKEND_IMPLEMENTATION_PLAN","agentCode":"backend-agent","laneKey":"backend","title":"后端实现计划"},{"stepOrder":2,"stepType":"FRONTEND_IMPLEMENTATION_PLAN","agentCode":"frontend-agent","laneKey":"frontend","title":"前端实现计划"},{"stepOrder":3,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900002, 'BACKEND_FOCUSED', '后端优先流程', '架构 → 后端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"BACKEND_FOCUSED","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"BACKEND_IMPLEMENTATION","title":"后端实现分析","steps":[{"stepOrder":1,"stepType":"BACKEND_IMPLEMENTATION_PLAN","agentCode":"backend-agent","laneKey":"backend","title":"后端实现计划"},{"stepOrder":2,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的后端实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900003, 'FRONTEND_FOCUSED', '前端优先流程', '架构 → 前端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"FRONTEND_FOCUSED","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"FRONTEND_IMPLEMENTATION","title":"前端实现分析","steps":[{"stepOrder":1,"stepType":"FRONTEND_IMPLEMENTATION_PLAN","agentCode":"frontend-agent","laneKey":"frontend","title":"前端实现计划"},{"stepOrder":2,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的前端实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900004, 'REVIEW_ONLY', '审查流程', '审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"REVIEW_ONLY","phases":[{"phaseOrder":1,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":2,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[]}');

-- Test database schema sync: V20 tool sandbox execution
CREATE TABLE IF NOT EXISTS tool_sandbox_execution (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  task_id BIGINT NULL,
  run_id BIGINT NULL,
  phase_id BIGINT NULL,
  step_id BIGINT NULL,
  agent_id BIGINT NULL,
  tool_name VARCHAR(64) NOT NULL,
  tool_type VARCHAR(32) NOT NULL,
  execution_mode VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  input_payload TEXT NULL,
  output_payload TEXT NULL,
  summary TEXT NULL,
  error_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  duration_ms BIGINT DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_tool_sandbox_project_time(project_id, create_time),
  INDEX idx_tool_sandbox_task(task_id),
  INDEX idx_tool_sandbox_run(run_id),
  INDEX idx_tool_sandbox_step(step_id),
  INDEX idx_tool_sandbox_agent(agent_id),
  INDEX idx_tool_sandbox_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V21 tool catalog + project tool config
CREATE TABLE IF NOT EXISTS tool_catalog (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tool_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT NULL,
  tool_type VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  execution_mode VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  built_in TINYINT NOT NULL DEFAULT 1,
  policy_json JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tool_catalog_key(tool_key),
  INDEX idx_tool_catalog_type(tool_type),
  INDEX idx_tool_catalog_risk(risk_level),
  INDEX idx_tool_catalog_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS project_tool_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  tool_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  config_json JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_tool(project_id, tool_id),
  INDEX idx_project_tool_project(project_id),
  INDEX idx_project_tool_tool(tool_id),
  INDEX idx_project_tool_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed tool catalog data
INSERT INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, policy_json, create_time, update_time) VALUES
(910001, 'PROJECT_CONTEXT_SCAN', '项目上下文扫描', '只读扫描项目代码结构和依赖，不修改任何文件。', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["ARCHITECTURE_ANALYSIS","FINAL_SUMMARY"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910002, 'TASK_REQUIREMENT_ANALYSIS', '任务需求分析', '分析任务需求描述，生成结构化分析结果。', 'ANALYSIS', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","TEST_PLAN"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910003, 'MOCK_FILE_INSPECTION', 'Mock 文件检查', '模拟文件检查工具，扫描项目文件结构和质量。', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["FRONTEND_IMPLEMENTATION_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910004, 'MOCK_TEST_PLAN_SCAN', 'Mock 测试计划扫描', '扫描和验证测试计划的完整性。', 'ANALYSIS', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["TEST_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW()),
(910005, 'MOCK_SECURITY_REVIEW', 'Mock 安全审查', '模拟安全审查，检查代码安全性和合规性。', 'ANALYSIS', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', NOW(), NOW());

-- Seed 910006 separately to avoid duplicate-key failure with existing rows
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, policy_json, create_time, update_time) VALUES
(910006, 'MOCK_PATCH_PROPOSAL', 'Mock 补丁方案生成', '生成 Mock 补丁方案，不产生真实 patch 文件。需人工审批后执行。', 'ANALYSIS', 'HIGH', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false,"requiresApproval":true}', NOW(), NOW());

-- Test database schema sync: V22 tool execution approval
CREATE TABLE IF NOT EXISTS tool_execution_approval (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_tool_approval_project_status(project_id, status),
  INDEX idx_tool_approval_task(task_id),
  INDEX idx_tool_approval_run(run_id),
  INDEX idx_tool_approval_step(step_id),
  INDEX idx_tool_approval_execution(tool_execution_id),
  INDEX idx_tool_approval_tool(tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V23 add artifact_id to tool_sandbox_execution
ALTER TABLE tool_sandbox_execution
  ADD COLUMN artifact_id BIGINT NULL AFTER error_message;

CREATE INDEX idx_tool_sandbox_artifact
  ON tool_sandbox_execution (artifact_id);

-- Test database schema sync: V24 tool parameter schema
ALTER TABLE tool_catalog
  ADD COLUMN parameter_schema_json JSON NULL AFTER policy_json;

ALTER TABLE project_tool_config
  ADD COLUMN parameters_json JSON NULL AFTER config_json;

-- Update parameter_schema_json for 6 built-in tools
UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"scope","label":"扫描范围","type":"select","required":true,"defaultValue":"TASK","options":["TASK","PROJECT","CURRENT_PHASE"]},{"key":"includeMetadata","label":"包含元数据","type":"boolean","required":false,"defaultValue":true}]}' WHERE id = 910001;

UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"depth","label":"分析深度","type":"select","required":true,"defaultValue":"STANDARD","options":["BASIC","STANDARD","DETAILED"]},{"key":"maxFindings","label":"最大建议数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}]}' WHERE id = 910002;

UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"includeStyleHints","label":"包含样式建议","type":"boolean","required":false,"defaultValue":true}]}' WHERE id = 910003;

UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"includeEdgeCases","label":"包含边界用例","type":"boolean","required":false,"defaultValue":true},{"key":"testLevel","label":"测试级别","type":"select","required":true,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"]}]}' WHERE id = 910004;

UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"riskFocus","label":"风险重点","type":"select","required":true,"defaultValue":"STANDARD","options":["STANDARD","AUTH","DATA","DEPENDENCY"]},{"key":"maxFindings","label":"最大风险数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}]}' WHERE id = 910005;

UPDATE tool_catalog SET parameter_schema_json = '{"fields":[{"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},{"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},{"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},{"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128}]}' WHERE id = 910006;

-- Test database schema sync: V25 tool_execution_job
CREATE TABLE IF NOT EXISTS tool_execution_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    step_id BIGINT NULL,
    tool_execution_id BIGINT NOT NULL,
    tool_key VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 2,
    request_payload TEXT NULL,
    result_payload TEXT NULL,
    last_error TEXT NULL,
    locked_by VARCHAR(128) NULL,
    locked_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_tool_job_project_time(project_id, create_time),
    INDEX idx_tool_job_task(task_id),
    INDEX idx_tool_job_run(run_id),
    INDEX idx_tool_job_step(step_id),
    INDEX idx_tool_job_execution(tool_execution_id),
    INDEX idx_tool_job_status(status),
    INDEX idx_tool_job_tool(tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行队列作业表';

-- Test database schema sync: V31 tool_execution_job DLQ/retry fields
ALTER TABLE tool_execution_job
    ADD COLUMN error_code VARCHAR(64) NULL AFTER last_error,
    ADD COLUMN failure_stage VARCHAR(64) NULL AFTER error_code,
    ADD COLUMN next_retry_at DATETIME NULL AFTER failure_stage,
    ADD COLUMN dead_lettered_at DATETIME NULL AFTER next_retry_at,
    ADD COLUMN dead_letter_reason TEXT NULL AFTER dead_lettered_at,
    ADD COLUMN source_job_id BIGINT NULL AFTER dead_letter_reason;

-- Test database schema sync: V26 read-only repository tools
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, policy_json, parameter_schema_json, create_time, update_time) VALUES
(910101, 'READ_REPOSITORY_TREE', '读取仓库文件树', '只读扫描项目仓库文件结构', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["ARCHITECTURE_ANALYSIS","FINAL_SUMMARY","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', '{"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256},{"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":50,"min":1,"max":200}]}', NOW(), NOW()),
(910102, 'READ_FILE_SNIPPET', '读取文件片段', '只读读取指定文件的指定行范围', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["ARCHITECTURE_ANALYSIS","BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","CODE_REVIEW"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', '{"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"filePath","label":"文件路径","type":"text","required":true,"defaultValue":"","maxLength":512},{"key":"startLine","label":"起始行","type":"number","required":false,"defaultValue":1,"min":1,"max":100000},{"key":"maxLines","label":"最大行数","type":"number","required":true,"defaultValue":80,"min":1,"max":300}]}', NOW(), NOW()),
(910103, 'READ_DIFF_SUMMARY', '读取 Diff 摘要', '只读读取基准分支与当前分支差异摘要', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["CODE_REVIEW","BACKEND_IMPLEMENTATION_PLAN","FRONTEND_IMPLEMENTATION_PLAN","FINAL_SUMMARY"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', '{"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"baseBranch","label":"基准分支","type":"text","required":false,"defaultValue":"main","maxLength":128},{"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":30,"min":1,"max":100}]}', NOW(), NOW()),
(910104, 'READ_BRANCH_INFO', '读取分支信息', '只读查询项目仓库分支列表', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"allowedStepTypes":["CODE_REVIEW","ARCHITECTURE_ANALYSIS","FINAL_SUMMARY"],"readOnly":true,"allowShell":false,"allowGitWrite":false,"allowFileWrite":false}', '{"fields":[{"key":"includeRemote","label":"包含远程分支","type":"boolean","required":false,"defaultValue":true},{"key":"maxBranches","label":"最大分支数","type":"number","required":true,"defaultValue":30,"min":1,"max":100}]}', NOW(), NOW());

-- Test database schema sync: V27 patch_proposal_review
CREATE TABLE IF NOT EXISTS patch_proposal_review (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    artifact_id BIGINT NOT NULL,
    tool_execution_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    decision VARCHAR(32) NULL,
    reviewer_id BIGINT NULL,
    review_comment TEXT NULL,
    reviewed_at DATETIME NULL,
    safety_confirmed TINYINT NOT NULL DEFAULT 0,
    checklist_json TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_patch_review_artifact(artifact_id),
    INDEX idx_patch_review_project_status(project_id, status),
    INDEX idx_patch_review_task(task_id),
    INDEX idx_patch_review_decision(decision),
    INDEX idx_patch_review_reviewer(reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补丁提案审阅表';

-- Test database schema sync: V28 upgrade tool parameter schema v2
UPDATE tool_catalog SET parameter_schema_json = '{"schemaVersion":2,"groups":[{"key":"scope","title":"提案范围","description":"控制提案的分析范围和目标","fields":["proposalScope","targetArea","targetFiles","maxChangedFiles"]},{"key":"tests","title":"测试建议","description":"控制测试提案的行为","fields":["includeTests","testLevel"]}],"fields":[{"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},{"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"targetFiles","label":"目标文件","type":"array","itemType":"text","required":false,"defaultValue":[],"maxItems":10,"itemMaxLength":256,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},{"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},{"key":"testLevel","label":"测试级别","type":"select","required":false,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"],"dependsOn":{"field":"includeTests","equals":true}}]}' WHERE id = 910006;

UPDATE tool_catalog SET parameter_schema_json = '{"schemaVersion":2,"groups":[{"key":"target","title":"读取目标","description":"指定文件读取的范围","fields":["branch","filePath","startLine","maxLines"]}],"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"filePath","label":"文件路径","type":"text","required":true,"defaultValue":"","maxLength":512,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"startLine","label":"起始行","type":"number","required":false,"defaultValue":1,"min":1,"max":100000},{"key":"maxLines","label":"最大行数","type":"number","required":true,"defaultValue":80,"min":1,"max":300}]}' WHERE id = 910102;

-- Test database schema sync: V29 code search index tables
CREATE TABLE IF NOT EXISTS code_index_file (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    repository_id BIGINT NULL,
    branch VARCHAR(128) NULL,
    file_path VARCHAR(500) NOT NULL,
    language VARCHAR(64) NULL,
    file_size BIGINT DEFAULT 0,
    line_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    indexed_at DATETIME NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_index_file(project_id, branch, file_path),
    INDEX idx_code_index_project_branch(project_id, branch),
    INDEX idx_code_index_language(language),
    INDEX idx_code_index_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引文件表';

CREATE TABLE IF NOT EXISTS code_index_symbol (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    symbol_name VARCHAR(256) NOT NULL,
    symbol_type VARCHAR(64) NOT NULL,
    language VARCHAR(64) NULL,
    file_path VARCHAR(500) NOT NULL,
    start_line INT NULL,
    end_line INT NULL,
    snippet TEXT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_code_symbol_project_name(project_id, symbol_name),
    INDEX idx_code_symbol_file(file_id),
    INDEX idx_code_symbol_type(symbol_type),
    INDEX idx_code_symbol_path(file_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引符号表';

CREATE TABLE IF NOT EXISTS code_index_chunk (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    chunk_index INT NOT NULL,
    start_line INT NOT NULL,
    end_line INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    token_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_chunk_file_index(file_id, chunk_index),
    INDEX idx_code_chunk_project(project_id),
    INDEX idx_code_chunk_file(file_id),
    INDEX idx_code_chunk_path(file_path),
    INDEX idx_code_chunk_hash(content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引切片表';

-- Test database schema sync: V30 seed code search tools
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, enabled, built_in, parameter_schema_json, create_time, update_time) VALUES
(910201, 'READ_CODE_INDEX', '读取代码索引摘要', '读取项目代码索引摘要信息，包括文件数、符号数和切片数', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE', 1, 1, '{"schemaVersion":2,"groups":[{"key":"scope","title":"索引范围","fields":["branch","pathPrefix","maxFiles"]}],"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":100,"min":1,"max":500}]}', NOW(), NOW()),
(910202, 'SEARCH_CODE_SYMBOL', '搜索代码符号', '搜索代码中的类、方法、函数等符号', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 0, 1, '{"schemaVersion":2,"groups":[{"key":"query","title":"搜索条件","fields":["keyword","branch","language","limit"]}],"fields":[{"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"language","label":"语言","type":"select","required":false,"defaultValue":"ALL","options":["ALL","java","ts","js","vue","sql","md"]},{"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}]}', NOW(), NOW()),
(910203, 'SEARCH_CODE_CHUNK', '搜索代码片段', '搜索代码文件内容中的匹配片段', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE', 0, 1, '{"schemaVersion":2,"groups":[{"key":"query","title":"搜索条件","fields":["keyword","branch","pathPrefix","limit"]}],"fields":[{"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}]}', NOW(), NOW());

-- Test database schema sync: V33 tool_operator_review
CREATE TABLE IF NOT EXISTS tool_operator_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    tool_execution_id BIGINT NULL,
    tool_job_id BIGINT NULL,
    review_target_type VARCHAR(32) NOT NULL,
    review_target_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    resolution TEXT NULL,
    assignee_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    resolved_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_tool_operator_review_project_time (project_id, create_time),
    INDEX idx_tool_operator_review_target (review_target_type, review_target_id),
    INDEX idx_tool_operator_review_execution (tool_execution_id),
    INDEX idx_tool_operator_review_job (tool_job_id),
    INDEX idx_tool_operator_review_status (status),
    INDEX idx_tool_operator_review_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

