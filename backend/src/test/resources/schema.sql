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

-- Test database schema sync: V34 tool incident, alert rule, alert delivery
CREATE TABLE IF NOT EXISTS tool_incident (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    run_id BIGINT NULL,
    tool_execution_id BIGINT NULL,
    tool_job_id BIGINT NULL,
    operator_review_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    resolution TEXT NULL,
    assignee_id BIGINT NULL,
    created_by BIGINT NULL,
    acknowledged_by BIGINT NULL,
    resolved_by BIGINT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_incident_project_time(project_id, create_time),
    INDEX idx_tool_incident_status(status),
    INDEX idx_tool_incident_severity(severity),
    INDEX idx_tool_incident_execution(tool_execution_id),
    INDEX idx_tool_incident_job(tool_job_id),
    INDEX idx_tool_incident_review(operator_review_id),
    INDEX idx_tool_incident_source(source_type, source_id),
    INDEX idx_tool_incident_assignee(assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行事件表';

CREATE TABLE IF NOT EXISTS tool_alert_rule (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    source_type VARCHAR(32) NOT NULL,
    min_severity VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    config_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_alert_rule_project(project_id),
    INDEX idx_tool_alert_rule_enabled(enabled),
    INDEX idx_tool_alert_rule_source(source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具告警规则表';

CREATE TABLE IF NOT EXISTS tool_alert_delivery (
    id BIGINT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    rule_id BIGINT NULL,
    channel VARCHAR(32) NOT NULL,
    route_target VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    payload TEXT NULL,
    error_message TEXT NULL,
    delivered_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_tool_alert_delivery_incident(incident_id),
    INDEX idx_tool_alert_delivery_project_time(project_id, create_time),
    INDEX idx_tool_alert_delivery_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具告警投递记录表';

-- Test database schema sync: V35 SLA, escalation policy, escalation event
ALTER TABLE tool_incident
  ADD COLUMN sla_minutes INT NULL,
  ADD COLUMN due_at DATETIME NULL,
  ADD COLUMN breached_at DATETIME NULL,
  ADD COLUMN sla_status VARCHAR(32) NULL,
  ADD COLUMN escalation_level INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_tool_incident_sla_status ON tool_incident(sla_status);
CREATE INDEX IF NOT EXISTS idx_tool_incident_due_at ON tool_incident(due_at);

CREATE TABLE IF NOT EXISTS tool_escalation_policy (
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
    INDEX idx_tool_escalation_policy_project(project_id),
    INDEX idx_tool_escalation_policy_enabled(enabled),
    INDEX idx_tool_escalation_policy_severity(severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具升级策略表';

CREATE TABLE IF NOT EXISTS tool_escalation_event (
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
    INDEX idx_tool_escalation_event_incident(incident_id),
    INDEX idx_tool_escalation_event_project_time(project_id, create_time),
    INDEX idx_tool_escalation_event_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具升级事件表';

-- Test database schema sync: V36 incident knowledge base tables
CREATE TABLE IF NOT EXISTS tool_incident_root_cause_note (
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
    INDEX idx_incident_rca_project_time(project_id, create_time),
    INDEX idx_incident_rca_incident(incident_id),
    INDEX idx_incident_rca_status(status),
    INDEX idx_incident_rca_author(author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件根因分析记录表';

CREATE TABLE IF NOT EXISTS tool_known_issue_template (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NULL,
    severity VARCHAR(32) NULL,
    root_cause_template TEXT NULL,
    impact_template TEXT NULL,
    resolution_template TEXT NULL,
    prevention_template TEXT NULL,
    tags VARCHAR(512) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_known_issue_template_project(project_id),
    INDEX idx_known_issue_template_category(category),
    INDEX idx_known_issue_template_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已知问题模板表';

CREATE TABLE IF NOT EXISTS tool_incident_knowledge_link (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause_note_id BIGINT NULL,
    knowledge_base_id BIGINT NULL,
    knowledge_document_id BIGINT NULL,
    link_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_knowledge_link_incident(incident_id),
    INDEX idx_knowledge_link_project_time(project_id, create_time),
    INDEX idx_knowledge_link_type(link_type),
    INDEX idx_knowledge_link_document(knowledge_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件知识关联表';

-- Test database schema sync: V37 incident retrospective + knowledge quality review
CREATE TABLE IF NOT EXISTS tool_incident_retrospective (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    root_cause_note_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    what_happened TEXT NULL,
    impact_summary TEXT NULL,
    response_summary TEXT NULL,
    lessons_learned TEXT NULL,
    prevention_plan TEXT NULL,
    action_items TEXT NULL,
    owner_id BIGINT NULL,
    due_at DATETIME NULL,
    regression_risk VARCHAR(32) NOT NULL DEFAULT 'LOW',
    repeated_incident TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_retrospective_project_time(project_id, create_time),
    INDEX idx_retrospective_incident(incident_id),
    INDEX idx_retrospective_status(status),
    INDEX idx_retrospective_owner(owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件事后回顾报告表';

CREATE TABLE IF NOT EXISTS tool_knowledge_quality_review (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    knowledge_document_id BIGINT NULL,
    retrospective_id BIGINT NULL,
    completeness_score TINYINT NOT NULL DEFAULT 0,
    accuracy_score TINYINT NOT NULL DEFAULT 0,
    actionability_score TINYINT NOT NULL DEFAULT 0,
    relevance_score TINYINT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    overall_status VARCHAR(32) NOT NULL DEFAULT 'NEEDS_WORK',
    checklist_json TEXT NULL,
    review_comment TEXT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_quality_review_project_time(project_id, create_time),
    INDEX idx_quality_review_incident(incident_id),
    INDEX idx_quality_review_status(review_status),
    INDEX idx_quality_review_overall(overall_status),
    INDEX idx_quality_review_reviewer(reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识质量审查表';

-- Test database schema sync: V38 beta trial feedback loop tables
CREATE TABLE IF NOT EXISTS beta_trial_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  participant_role VARCHAR(64) NULL,
  environment_type VARCHAR(64) NULL,
  provider_mode VARCHAR(64) NULL,
  github_oauth_status VARCHAR(64) NULL,
  session_status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  completed_path_json TEXT NULL,
  blocked_at_step VARCHAR(255) NULL,
  blocker_summary TEXT NULL,
  satisfaction_score INT NULL,
  continue_intent VARCHAR(32) NULL,
  summary TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_beta_session_project (project_id),
  INDEX idx_beta_session_status (session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beta_trial_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  category VARCHAR(64) NULL,
  subcategory VARCHAR(64) NULL,
  severity VARCHAR(16) NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  title VARCHAR(255) NOT NULL,
  detail TEXT NULL,
  expected_behavior TEXT NULL,
  actual_behavior TEXT NULL,
  suggested_action TEXT NULL,
  triage_status VARCHAR(32) NOT NULL DEFAULT 'NEW',
  mapped_incident_id BIGINT NULL,
  mapped_known_issue_id BIGINT NULL,
  release_blocking TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_beta_feedback_session (session_id),
  INDEX idx_beta_feedback_project (project_id),
  INDEX idx_beta_feedback_severity (severity),
  INDEX idx_beta_feedback_triage (triage_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beta_environment_readiness (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  target_name VARCHAR(128) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  check_status VARCHAR(32) NOT NULL,
  summary TEXT NULL,
  detail_json TEXT NULL,
  checked_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_beta_readiness_project (project_id),
  INDEX idx_beta_readiness_session (session_id),
  INDEX idx_beta_readiness_status (check_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V39 model cost & PR review quality tables
CREATE TABLE IF NOT EXISTS model_cost_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    request_type VARCHAR(64) NOT NULL,
    stat_date DATE NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    fallback_count BIGINT NOT NULL DEFAULT 0,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(18,6) NOT NULL DEFAULT 0,
    avg_latency_ms BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_model_cost_project_date (project_id, stat_date),
    INDEX idx_model_cost_provider_model (provider, model_name),
    INDEX idx_model_cost_request_type (request_type),
    UNIQUE KEY uk_model_cost_daily (project_id, provider, model_name, request_type, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS model_cost_alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    stat_date DATE NOT NULL,
    threshold_value DECIMAL(18,6) NULL,
    actual_value DECIMAL(18,6) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_model_cost_alert_project (project_id, stat_date),
    INDEX idx_model_cost_alert_status (status, severity),
    INDEX idx_model_cost_alert_provider (provider, model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pr_review_quality_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    review_job_id BIGINT NOT NULL,
    github_binding_id BIGINT NULL,
    repository_full_name VARCHAR(255) NOT NULL,
    pull_request_number BIGINT NOT NULL,
    strategy_key VARCHAR(64) NULL,
    model_provider VARCHAR(32) NULL,
    model_name VARCHAR(128) NULL,
    findings_total INT NOT NULL DEFAULT 0,
    high_risk_findings INT NOT NULL DEFAULT 0,
    medium_risk_findings INT NOT NULL DEFAULT 0,
    low_risk_findings INT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    human_feedback_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    adoption_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    usefulness_score INT NULL,
    false_positive_score INT NULL,
    review_comment TEXT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_pr_quality_project_time (project_id, create_time),
    INDEX idx_pr_quality_repo_pr (repository_full_name, pull_request_number),
    INDEX idx_pr_quality_status (review_status, human_feedback_status, adoption_status),
    UNIQUE KEY uk_pr_quality_job (review_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V40 beta release gate tables
CREATE TABLE IF NOT EXISTS beta_release_gate_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    INDEX idx_beta_gate_rule_project (project_id),
    INDEX idx_beta_gate_rule_category (category, enabled),
    UNIQUE KEY uk_beta_gate_rule (project_id, rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beta_release_gate_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    INDEX idx_beta_gate_eval_project (project_id, evaluated_at),
    INDEX idx_beta_gate_eval_target (evaluation_target, evaluation_type),
    INDEX idx_beta_gate_eval_status (gate_status, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beta_release_decision (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    INDEX idx_beta_release_decision_project (project_id, create_time),
    INDEX idx_beta_release_decision_status (decision_status),
    UNIQUE KEY uk_beta_release_label (project_id, release_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed built-in gate rules (test compatible IDs)
INSERT IGNORE INTO beta_release_gate_rule (id, project_id, rule_key, category, display_name, threshold_operator, threshold_value, enabled, blocking, sort_order, description, create_time, update_time) VALUES
(920001, NULL, 'P0_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', 'P0 反馈数', 'EQ', 0, 1, 1, 1, 'P0 反馈数量必须为 0', NOW(), NOW()),
(920002, NULL, 'P1_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', 'P1 反馈数', 'LTE', 3, 1, 1, 2, 'P1 反馈数量不超过 3 个', NOW(), NOW()),
(920003, NULL, 'RELEASE_BLOCKING_FEEDBACK_COUNT', 'TRIAL_FEEDBACK', '阻塞性反馈数', 'EQ', 0, 1, 1, 3, '阻塞性反馈数量必须为 0', NOW(), NOW()),
(920004, NULL, 'READINESS_FAIL_COUNT', 'ENVIRONMENT_READINESS', '环境检查失败数', 'EQ', 0, 1, 1, 4, '环境就绪检查失败数量必须为 0', NOW(), NOW()),
(920005, NULL, 'MODEL_COST_ALERT_HIGH_COUNT', 'MODEL_COST', '成本告警数 (HIGH+)', 'EQ', 0, 1, 0, 5, '高级别成本告警数量必须为 0', NOW(), NOW()),
(920006, NULL, 'PR_REVIEW_FAILURE_RATIO', 'PR_REVIEW_QUALITY', 'PR 评审失败率', 'LTE', 0.20, 1, 0, 6, 'PR 评审失败率不超过 20%', NOW(), NOW()),
(920007, NULL, 'PR_REVIEW_ADOPTION_RATIO', 'PR_REVIEW_QUALITY', 'PR 评审采纳率', 'GTE', 0.30, 1, 0, 7, 'PR 评审建议采纳率不低于 30%', NOW(), NOW()),
(920008, NULL, 'OPEN_CRITICAL_INCIDENT_COUNT', 'INCIDENT_RISK', '未关闭严重事故数', 'EQ', 0, 1, 1, 8, '未关闭的严重事故数量必须为 0', NOW(), NOW()),
(920009, NULL, 'KNOWLEDGE_QUALITY_REJECTED_COUNT', 'KNOWLEDGE_QUALITY', '知识质量被拒数', 'EQ', 0, 1, 0, 9, '知识质量审查被拒数量必须为 0', NOW(), NOW());

-- Test database schema sync: V41
CREATE TABLE IF NOT EXISTS release_rollout_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    KEY idx_release_rollout_plan_project (project_id, create_time),
    KEY idx_release_rollout_plan_status (rollout_status),
    UNIQUE KEY uk_release_rollout_label (project_id, release_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_rollout_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    KEY idx_release_rollout_step_plan (plan_id, step_order),
    KEY idx_release_rollout_step_status (step_status),
    UNIQUE KEY uk_release_rollout_step (plan_id, step_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_verification_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    PRIMARY KEY (id),
    KEY idx_release_verification_plan (plan_id, verification_phase, recorded_at),
    KEY idx_release_verification_status (verification_status, severity),
    KEY idx_release_verification_project (project_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_rollback_drill (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    drill_status VARCHAR(32) NOT NULL,
    drill_scope VARCHAR(64) NOT NULL,
    environment_name VARCHAR(64) NOT NULL,
    owner_id BIGINT NULL,
    executor_id BIGINT NULL,
    planned_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_seconds BIGINT NULL,
    success_criteria TEXT NULL,
    rollback_steps_summary TEXT NULL,
    blockers_summary TEXT NULL,
    result_summary TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_release_rollback_drill_plan (plan_id),
    KEY idx_release_rollback_drill_project (project_id, create_time),
    KEY idx_release_rollback_drill_status (drill_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NULL,
    plan_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(128) NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    related_step_id BIGINT NULL,
    related_verification_id BIGINT NULL,
    related_incident_id BIGINT NULL,
    related_alert_id BIGINT NULL,
    evidence_json JSON NULL,
    event_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_release_audit_event_plan (plan_id, event_time),
    KEY idx_release_audit_event_project (project_id, event_time),
    KEY idx_release_audit_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_postmortem_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    release_label VARCHAR(128) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    overall_outcome VARCHAR(32) NOT NULL,
    summary TEXT NULL,
    what_went_well TEXT NULL,
    what_went_wrong TEXT NULL,
    customer_impact TEXT NULL,
    follow_up_actions TEXT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_release_postmortem_plan (plan_id),
    KEY idx_release_postmortem_project (project_id, create_time),
    KEY idx_release_postmortem_status (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test database schema sync: V43
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

-- V44: Multi-project release governance tables
CREATE TABLE IF NOT EXISTS release_portfolio_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    latest_release_label VARCHAR(128) NULL,
    confidence_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    confidence_level VARCHAR(32) NOT NULL,
    rollout_status VARCHAR(32) NULL,
    decision_status VARCHAR(32) NULL,
    blocking_issue_count INT NOT NULL DEFAULT 0,
    warning_issue_count INT NOT NULL DEFAULT 0,
    open_incident_count INT NOT NULL DEFAULT 0,
    active_alert_count INT NOT NULL DEFAULT 0,
    failed_verification_count INT NOT NULL DEFAULT 0,
    rollback_ready TINYINT NOT NULL DEFAULT 0,
    signoff_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    portfolio_rank INT NULL,
    expansion_recommendation VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_portfolio_snapshot_date(snapshot_date, confidence_score),
    KEY idx_release_portfolio_snapshot_project(project_id, snapshot_date),
    KEY idx_release_portfolio_snapshot_rank(snapshot_date, portfolio_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release portfolio snapshot';

CREATE TABLE IF NOT EXISTS governance_baseline_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    template_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    default_signoff_roles_json JSON NULL,
    default_verification_rules_json JSON NULL,
    default_rollback_requirements_json JSON NULL,
    default_confidence_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_baseline_template(template_key),
    KEY idx_governance_baseline_template_scope(template_scope, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance baseline template';

CREATE TABLE IF NOT EXISTS release_risk_heatmap_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    risk_category VARCHAR(64) NOT NULL,
    risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    risk_level VARCHAR(32) NOT NULL,
    source_count INT NOT NULL DEFAULT 0,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_risk_heatmap_date(snapshot_date, risk_category),
    KEY idx_release_risk_heatmap_project(project_id, snapshot_date),
    KEY idx_release_risk_heatmap_level(snapshot_date, risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release risk heatmap snapshot';

-- V45: Organization governance tables (40B)
CREATE TABLE IF NOT EXISTS organization_trial_policy (
    id BIGINT PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    policy_scope VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    threshold_json JSON NULL,
    signoff_policy_json JSON NULL,
    rollback_policy_json JSON NULL,
    verification_policy_json JSON NULL,
    recommendation_policy_json JSON NULL,
    notes TEXT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_organization_trial_policy(policy_key),
    KEY idx_organization_trial_policy_scope(policy_scope, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Organization trial policy';

CREATE TABLE IF NOT EXISTS release_guardrail_evaluation (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    policy_key VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NOT NULL,
    guardrail_category VARCHAR(64) NOT NULL,
    evaluation_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actual_value DECIMAL(18,6) NULL,
    threshold_value DECIMAL(18,6) NULL,
    summary VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    recommendation_text TEXT NULL,
    evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_release_guardrail_eval_date(snapshot_date, project_id),
    KEY idx_release_guardrail_eval_policy(policy_key, evaluation_status),
    KEY idx_release_guardrail_eval_severity(snapshot_date, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Release guardrail evaluation';

CREATE TABLE IF NOT EXISTS portfolio_drift_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    drift_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    drift_level VARCHAR(32) NOT NULL,
    baseline_template_key VARCHAR(64) NULL,
    confidence_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    signoff_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    verification_delta DECIMAL(8,2) NOT NULL DEFAULT 0,
    rollback_readiness_changed TINYINT NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    detail_json JSON NULL,
    create_time DATETIME NOT NULL,
    KEY idx_portfolio_drift_snapshot_date(snapshot_date, drift_score),
    KEY idx_portfolio_drift_snapshot_project(project_id, snapshot_date),
    KEY idx_portfolio_drift_snapshot_level(snapshot_date, drift_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Portfolio drift snapshot';

-- V46: Governance workflow and waiver tables (40C)
CREATE TABLE IF NOT EXISTS governance_recommendation_item (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    source_snapshot_date DATE NOT NULL,
    policy_key VARCHAR(64) NOT NULL,
    guardrail_key VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    workflow_status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL,
    due_at DATETIME NULL,
    resolved_at DATETIME NULL,
    resolution_note TEXT NULL,
    source_evidence_json JSON NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_recommendation_project(project_id, workflow_status),
    KEY idx_governance_recommendation_priority(priority, workflow_status),
    KEY idx_governance_recommendation_due(due_at, workflow_status),
    UNIQUE KEY uk_governance_recommendation_source(project_id, source_snapshot_date, policy_key, guardrail_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation item';

CREATE TABLE IF NOT EXISTS governance_waiver_request (
    id BIGINT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    waiver_status VARCHAR(32) NOT NULL,
    waiver_scope VARCHAR(64) NOT NULL,
    requested_by BIGINT NULL,
    requested_by_name VARCHAR(128) NULL,
    approved_by BIGINT NULL,
    approved_by_name VARCHAR(128) NULL,
    reason_text TEXT NOT NULL,
    approval_note TEXT NULL,
    expires_at DATETIME NULL,
    revoked_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_governance_waiver_project(project_id, waiver_status),
    KEY idx_governance_waiver_recommendation(recommendation_id),
    KEY idx_governance_waiver_expiry(expires_at, waiver_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance waiver request';

CREATE TABLE IF NOT EXISTS governance_workflow_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    total_recommendation_count INT NOT NULL DEFAULT 0,
    open_recommendation_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    blocked_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0,
    active_waiver_count INT NOT NULL DEFAULT 0,
    expired_waiver_count INT NOT NULL DEFAULT 0,
    completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    overdue_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_governance_workflow_snapshot_date(snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance workflow snapshot';

-- V47: Governance SLA, escalation and ownership tables (41A)
CREATE TABLE IF NOT EXISTS governance_sla_policy (
    id BIGINT PRIMARY KEY, policy_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    priority VARCHAR(32) NOT NULL, category VARCHAR(64) NULL, sla_hours INT NOT NULL,
    warning_hours INT NOT NULL, enabled TINYINT NOT NULL DEFAULT 1, notes TEXT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_sla_policy(policy_key),
    KEY idx_governance_sla_policy_priority(priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance SLA policy';

CREATE TABLE IF NOT EXISTS governance_escalation_event (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
    escalation_type VARCHAR(64) NOT NULL, escalation_level VARCHAR(32) NOT NULL,
    event_status VARCHAR(32) NOT NULL, summary VARCHAR(255) NOT NULL, detail TEXT NULL,
    owner_id BIGINT NULL, owner_name VARCHAR(128) NULL, triggered_at DATETIME NOT NULL,
    acknowledged_at DATETIME NULL, resolved_at DATETIME NULL, create_time DATETIME NOT NULL,
    KEY idx_governance_escalation_recommendation(recommendation_id, triggered_at),
    KEY idx_governance_escalation_project(project_id, triggered_at),
    KEY idx_governance_escalation_status(event_status, escalation_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance escalation event';

CREATE TABLE IF NOT EXISTS governance_ownership_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, owner_id BIGINT NOT NULL,
    owner_name VARCHAR(128) NOT NULL, total_assigned_count INT NOT NULL DEFAULT 0,
    open_count INT NOT NULL DEFAULT 0, in_progress_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0, completed_7d_count INT NOT NULL DEFAULT 0,
    active_waiver_count INT NOT NULL DEFAULT 0,
    owner_health_score DECIMAL(8,2) NOT NULL DEFAULT 0, owner_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_governance_ownership_snapshot_date(snapshot_date, owner_health_score),
    KEY idx_governance_ownership_snapshot_owner(owner_id, snapshot_date),
    KEY idx_governance_ownership_snapshot_level(snapshot_date, owner_health_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance ownership snapshot';

-- V48: Governance forecast and risk tables (41B)
CREATE TABLE IF NOT EXISTS governance_capacity_forecast (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, forecast_horizon_days INT NOT NULL,
    owner_id BIGINT NOT NULL, owner_name VARCHAR(128) NOT NULL,
    current_open_count INT NOT NULL DEFAULT 0, current_overdue_count INT NOT NULL DEFAULT 0,
    avg_completed_per_day DECIMAL(8,2) NOT NULL DEFAULT 0, projected_new_items INT NOT NULL DEFAULT 0,
    projected_completed_items INT NOT NULL DEFAULT 0, projected_backlog_count INT NOT NULL DEFAULT 0,
    projected_overdue_count INT NOT NULL DEFAULT 0, capacity_risk_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_capacity_forecast_date(snapshot_date, forecast_horizon_days),
    KEY idx_gov_capacity_forecast_owner(owner_id, snapshot_date),
    KEY idx_gov_capacity_forecast_level(snapshot_date, capacity_risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance capacity forecast';

CREATE TABLE IF NOT EXISTS predictive_risk_signal (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NULL, target_name VARCHAR(255) NOT NULL, signal_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL, risk_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    probability_score DECIMAL(8,2) NOT NULL DEFAULT 0, time_horizon_days INT NOT NULL DEFAULT 7,
    summary VARCHAR(255) NOT NULL, detail TEXT NULL, evidence_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_predictive_risk_signal_date(snapshot_date, risk_level),
    KEY idx_predictive_risk_signal_target(target_type, target_id, snapshot_date),
    KEY idx_predictive_risk_signal_type(signal_type, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Predictive risk signal';

CREATE TABLE IF NOT EXISTS governance_backlog_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, open_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0, blocked_count INT NOT NULL DEFAULT 0,
    overdue_count INT NOT NULL DEFAULT 0, waiver_active_count INT NOT NULL DEFAULT 0,
    incoming_7d_count INT NOT NULL DEFAULT 0, completed_7d_count INT NOT NULL DEFAULT 0,
    backlog_growth_rate DECIMAL(8,2) NOT NULL DEFAULT 0, backlog_health_level VARCHAR(32) NOT NULL,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_backlog_snapshot_date(snapshot_date, backlog_growth_rate),
    KEY idx_gov_backlog_snapshot_project(project_id, snapshot_date),
    KEY idx_gov_backlog_snapshot_level(snapshot_date, backlog_health_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance backlog snapshot';

-- V49: Governance simulation and tuning tables (41C)
CREATE TABLE IF NOT EXISTS governance_simulation_scenario (
    id BIGINT PRIMARY KEY, scenario_name VARCHAR(255) NOT NULL, scenario_type VARCHAR(64) NOT NULL,
    baseline_snapshot_date DATE NULL, scenario_status VARCHAR(32) NOT NULL,
    input_json JSON NOT NULL, notes TEXT NULL, created_by BIGINT NULL,
    created_by_name VARCHAR(128) NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_sim_scenario_type(scenario_type, scenario_status),
    KEY idx_gov_sim_scenario_date(baseline_snapshot_date, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance simulation scenario';

CREATE TABLE IF NOT EXISTS governance_simulation_result (
    id BIGINT PRIMARY KEY, scenario_id BIGINT NOT NULL, result_status VARCHAR(32) NOT NULL,
    impacted_owner_count INT NOT NULL DEFAULT 0, impacted_project_count INT NOT NULL DEFAULT 0,
    projected_backlog_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_overdue_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_risk_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    projected_capacity_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, detail_json JSON NULL,
    report_markdown MEDIUMTEXT NULL, calculated_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_sim_result_scenario(scenario_id),
    KEY idx_gov_sim_result_status(result_status, calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance simulation result';

CREATE TABLE IF NOT EXISTS policy_tuning_suggestion (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL, target_scope VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NULL, current_value VARCHAR(255) NULL,
    suggested_value VARCHAR(255) NULL, expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL, evidence_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_policy_tuning_suggestion_date(snapshot_date, priority),
    KEY idx_policy_tuning_suggestion_type(suggestion_type, target_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Policy tuning suggestion';

-- V50: Governance execution and playbook tables (42A)
CREATE TABLE IF NOT EXISTS governance_recommendation_playbook_template (
    id BIGINT PRIMARY KEY, template_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL, guardrail_key VARCHAR(64) NULL, priority VARCHAR(32) NULL,
    enabled TINYINT NOT NULL DEFAULT 1, template_steps_json JSON NOT NULL,
    success_criteria_json JSON NULL, handoff_notes TEXT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_playbook_template(template_key),
    KEY idx_governance_playbook_template_match(recommendation_category, guardrail_key, priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation playbook template';

CREATE TABLE IF NOT EXISTS governance_recommendation_execution_plan (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
    plan_status VARCHAR(32) NOT NULL, template_key VARCHAR(64) NULL,
    owner_id BIGINT NULL, owner_name VARCHAR(128) NULL, due_at DATETIME NULL,
    steps_json JSON NOT NULL, completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_execution_plan_recommendation(recommendation_id),
    KEY idx_gov_execution_plan_project(project_id, plan_status),
    KEY idx_gov_execution_plan_due(due_at, plan_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation execution plan';

CREATE TABLE IF NOT EXISTS governance_handoff_checklist (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NOT NULL, execution_plan_id BIGINT NULL,
    from_owner_id BIGINT NULL, from_owner_name VARCHAR(128) NULL,
    to_owner_id BIGINT NULL, to_owner_name VARCHAR(128) NULL,
    checklist_status VARCHAR(32) NOT NULL, checklist_items_json JSON NOT NULL,
    handoff_note TEXT NULL, handed_off_at DATETIME NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_handoff_recommendation(recommendation_id),
    KEY idx_gov_handoff_plan(execution_plan_id),
    KEY idx_gov_handoff_status(checklist_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance handoff checklist';

-- V51: Governance knowledge and recipe tables (42B)
CREATE TABLE IF NOT EXISTS governance_knowledge_entry (
    id BIGINT PRIMARY KEY, project_id BIGINT NULL, source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL, title VARCHAR(255) NOT NULL, category VARCHAR(64) NOT NULL,
    tags_json JSON NULL, summary_text TEXT NULL, detail_markdown MEDIUMTEXT NULL,
    effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0, reuse_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_knowledge_entry_category(category),
    KEY idx_gov_knowledge_entry_project(project_id, create_time),
    KEY idx_gov_knowledge_entry_score(effectiveness_score, reuse_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance knowledge entry';

CREATE TABLE IF NOT EXISTS governance_pattern_library_item (
    id BIGINT PRIMARY KEY, pattern_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recommendation_category VARCHAR(64) NULL, guardrail_key VARCHAR(64) NULL,
    priority VARCHAR(32) NULL, pattern_json JSON NOT NULL, notes TEXT NULL,
    enabled TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_pattern_library(pattern_key),
    KEY idx_governance_pattern_library_match(recommendation_category, guardrail_key, priority, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance pattern library item';

CREATE TABLE IF NOT EXISTS governance_remediation_recipe (
    id BIGINT PRIMARY KEY, recipe_key VARCHAR(64) NOT NULL, display_name VARCHAR(255) NOT NULL,
    recipe_type VARCHAR(64) NOT NULL, recommendation_category VARCHAR(64) NULL,
    guardrail_key VARCHAR(64) NULL, steps_json JSON NOT NULL, prerequisites_json JSON NULL,
    success_criteria_json JSON NULL, effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0, enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_governance_remediation_recipe(recipe_key),
    KEY idx_governance_remediation_recipe_match(recipe_type, recommendation_category, guardrail_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance remediation recipe';

-- V52: Governance effectiveness analytics tables (42C)
CREATE TABLE IF NOT EXISTS governance_recipe_effectiveness_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, recipe_id BIGINT NOT NULL,
    recipe_key VARCHAR(64) NOT NULL, recipe_name VARCHAR(255) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0, completion_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(8,2) NOT NULL DEFAULT 0, avg_completion_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    failure_rate DECIMAL(8,2) NOT NULL DEFAULT 0, effectiveness_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_recipe_effectiveness_date(snapshot_date, effectiveness_score),
    KEY idx_gov_recipe_effectiveness_recipe(recipe_id, snapshot_date),
    KEY idx_gov_recipe_effectiveness_level(snapshot_date, effectiveness_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recipe effectiveness snapshot';

CREATE TABLE IF NOT EXISTS governance_playbook_analytics_record (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, template_key VARCHAR(64) NOT NULL,
    template_name VARCHAR(255) NOT NULL, plan_count INT NOT NULL DEFAULT 0,
    completed_plan_count INT NOT NULL DEFAULT 0, blocked_plan_count INT NOT NULL DEFAULT 0,
    avg_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_step_completion_rate DECIMAL(8,2) NOT NULL DEFAULT 0,
    avg_resolution_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    related_recipe_count INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL,
    KEY idx_gov_playbook_analytics_date(snapshot_date, avg_completion_rate),
    KEY idx_gov_playbook_analytics_template(template_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance playbook analytics record';

CREATE TABLE IF NOT EXISTS governance_optimization_suggestion (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, suggestion_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL, target_type VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NOT NULL, current_metric_value VARCHAR(255) NULL,
    suggested_action TEXT NOT NULL, expected_impact_text VARCHAR(255) NOT NULL,
    rationale_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_optimization_suggestion_date(snapshot_date, priority),
    KEY idx_gov_optimization_suggestion_target(target_type, target_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance optimization suggestion';

-- V53: Governance copilot workspace tables (43A)
CREATE TABLE IF NOT EXISTS governance_workspace_session (
    id BIGINT PRIMARY KEY, operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    session_status VARCHAR(32) NOT NULL, focus_mode VARCHAR(32) NOT NULL,
    selected_project_id BIGINT NULL, selected_recommendation_id BIGINT NULL,
    selected_owner_id BIGINT NULL, context_summary TEXT NULL,
    started_at DATETIME NOT NULL, ended_at DATETIME NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_workspace_session_operator(operator_id, session_status),
    KEY idx_gov_workspace_session_project(selected_project_id, session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance workspace session';

CREATE TABLE IF NOT EXISTS governance_guided_task (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, recommendation_id BIGINT NULL,
    task_type VARCHAR(64) NOT NULL, priority VARCHAR(32) NOT NULL, task_status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL, summary TEXT NULL, source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL, linked_playbook_key VARCHAR(64) NULL,
    linked_recipe_key VARCHAR(64) NULL, linked_knowledge_entry_id BIGINT NULL,
    due_at DATETIME NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_guided_task_session(session_id, task_status),
    KEY idx_gov_guided_task_priority(priority, task_status),
    KEY idx_gov_guided_task_recommendation(recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance guided task';

CREATE TABLE IF NOT EXISTS governance_next_step_recommendation (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL, suggestion_rank INT NOT NULL DEFAULT 0,
    suggestion_type VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL,
    summary_text TEXT NULL, rationale_text TEXT NULL, expected_outcome_text TEXT NULL,
    action_payload_json JSON NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_next_step_session(session_id, suggestion_rank),
    KEY idx_gov_next_step_task(guided_task_id),
    KEY idx_gov_next_step_recommendation(recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance next step recommendation';

-- V54: Governance operator learning tables (43B)
CREATE TABLE IF NOT EXISTS governance_operator_action_memory (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, guided_task_id BIGINT NULL,
    recommendation_id BIGINT NULL, operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    action_type VARCHAR(64) NOT NULL, action_target_type VARCHAR(64) NOT NULL,
    action_target_id BIGINT NULL, accepted_flag TINYINT NOT NULL DEFAULT 0,
    success_flag TINYINT NOT NULL DEFAULT 0, duration_seconds INT NULL,
    note_text TEXT NULL, action_payload_json JSON NULL, occurred_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_gov_operator_action_session(session_id, occurred_at),
    KEY idx_gov_operator_action_operator(operator_id, occurred_at),
    KEY idx_gov_operator_action_guided_task(guided_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance operator action memory';

CREATE TABLE IF NOT EXISTS governance_workspace_session_insight (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL, insight_window VARCHAR(32) NOT NULL,
    total_actions INT NOT NULL DEFAULT 0, accepted_recommendation_count INT NOT NULL DEFAULT 0,
    dismissed_recommendation_count INT NOT NULL DEFAULT 0,
    completed_guided_task_count INT NOT NULL DEFAULT 0,
    blocked_guided_task_count INT NOT NULL DEFAULT 0, avg_action_duration_seconds INT NULL,
    productivity_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    dominant_action_pattern VARCHAR(128) NULL, summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_workspace_session_insight_session(session_id, insight_window),
    KEY idx_gov_workspace_session_insight_operator(operator_id, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance workspace session insight';

CREATE TABLE IF NOT EXISTS governance_remediation_reuse_bundle (
    id BIGINT PRIMARY KEY, bundle_key VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL, guardrail_key VARCHAR(64) NULL, priority VARCHAR(32) NULL,
    effectiveness_level VARCHAR(32) NOT NULL, reuse_count INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(10,2) NOT NULL DEFAULT 0, action_sequence_json JSON NOT NULL,
    source_session_id BIGINT NULL, source_operator_id BIGINT NULL,
    source_operator_name VARCHAR(128) NULL, enabled TINYINT NOT NULL DEFAULT 1,
    summary_text TEXT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_remediation_reuse_bundle_key(bundle_key),
    KEY idx_gov_remediation_reuse_bundle_category(category, enabled),
    KEY idx_gov_remediation_reuse_bundle_guardrail(guardrail_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance remediation reuse bundle';

-- V55: Governance copilot tuning tables (43C)
CREATE TABLE IF NOT EXISTS governance_operator_feedback (
    id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, operator_id BIGINT NULL,
    operator_name VARCHAR(128) NULL, suggestion_type VARCHAR(64) NULL, suggestion_id BIGINT NULL,
    guided_task_id BIGINT NULL, reuse_bundle_id BIGINT NULL,
    feedback_target_type VARCHAR(64) NOT NULL, feedback_rating INT NOT NULL,
    helpful_flag TINYINT NOT NULL DEFAULT 0, accepted_flag TINYINT NOT NULL DEFAULT 0,
    reason_code VARCHAR(64) NULL, note_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_feedback_session(session_id, create_time),
    KEY idx_gov_feedback_operator(operator_id, create_time),
    KEY idx_gov_feedback_target(feedback_target_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance operator feedback';

CREATE TABLE IF NOT EXISTS governance_adaptive_guidance_signal (
    id BIGINT PRIMARY KEY, signal_type VARCHAR(64) NOT NULL, focus_mode VARCHAR(32) NULL,
    category VARCHAR(64) NULL, suggestion_type VARCHAR(64) NULL,
    recommendation_priority VARCHAR(32) NULL, acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    completion_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    weight_score DECIMAL(10,2) NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    rationale_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_adaptive_signal_type(signal_type, captured_at),
    KEY idx_gov_adaptive_signal_focus(focus_mode, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance adaptive guidance signal';

CREATE TABLE IF NOT EXISTS governance_copilot_tuning_snapshot (
    id BIGINT PRIMARY KEY, snapshot_window VARCHAR(32) NOT NULL,
    total_feedback_count INT NOT NULL DEFAULT 0, acceptance_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    dismissal_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_feedback_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    top_suggestion_type VARCHAR(64) NULL, weakest_suggestion_type VARCHAR(64) NULL,
    top_focus_mode VARCHAR(32) NULL, weakest_focus_mode VARCHAR(32) NULL,
    tuning_confidence_score DECIMAL(10,2) NOT NULL DEFAULT 0, summary_markdown TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_tuning_snapshot_window(snapshot_window, captured_at),
    KEY idx_gov_tuning_snapshot_captured(captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance copilot tuning snapshot';

-- V56: Governance draft planning tables (44A)
CREATE TABLE IF NOT EXISTS governance_draft_remediation_plan (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NULL, session_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    plan_status VARCHAR(32) NOT NULL, plan_title VARCHAR(255) NOT NULL,
    scope_type VARCHAR(64) NOT NULL, summary_text TEXT NULL, goal_text TEXT NULL,
    proposed_steps_json JSON NOT NULL, linked_bundle_id BIGINT NULL,
    linked_playbook_key VARCHAR(64) NULL, linked_recipe_key VARCHAR(64) NULL,
    risk_level VARCHAR(32) NOT NULL, human_confirmation_required TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_draft_plan_recommendation(recommendation_id, plan_status),
    KEY idx_gov_draft_plan_session(session_id, plan_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft remediation plan';

CREATE TABLE IF NOT EXISTS governance_safe_assistive_action (
    id BIGINT PRIMARY KEY, draft_plan_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL, action_status VARCHAR(32) NOT NULL,
    action_title VARCHAR(255) NOT NULL, action_summary TEXT NULL,
    safety_level VARCHAR(32) NOT NULL, confirmation_required TINYINT NOT NULL DEFAULT 1,
    checklist_json JSON NOT NULL, prefill_payload_json JSON NULL,
    action_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_safe_assistive_action_plan(draft_plan_id, action_order),
    KEY idx_gov_safe_assistive_action_status(action_status, safety_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance safe assistive action';

CREATE TABLE IF NOT EXISTS governance_recommendation_package (
    id BIGINT PRIMARY KEY, recommendation_id BIGINT NULL, draft_plan_id BIGINT NULL,
    package_status VARCHAR(32) NOT NULL, package_title VARCHAR(255) NOT NULL,
    package_summary TEXT NULL, recommendation_context_json JSON NOT NULL,
    attachments_json JSON NULL, review_notes_text TEXT NULL,
    submit_ready_flag TINYINT NOT NULL DEFAULT 0, submitted_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_recommendation_package_recommendation(recommendation_id, package_status),
    KEY idx_gov_recommendation_package_plan(draft_plan_id, package_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance recommendation package';

-- V57: Governance outcome review tables (44B)
CREATE TABLE IF NOT EXISTS governance_draft_adoption_review (
    id BIGINT PRIMARY KEY, draft_plan_id BIGINT NOT NULL, recommendation_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    adoption_result VARCHAR(32) NOT NULL, modification_level VARCHAR(32) NOT NULL,
    usefulness_rating INT NOT NULL, reason_code VARCHAR(64) NULL,
    outcome_note_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_draft_adoption_review_plan(draft_plan_id),
    KEY idx_gov_draft_adoption_review_result(adoption_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft adoption review';

CREATE TABLE IF NOT EXISTS governance_assistive_action_quality_review (
    id BIGINT PRIMARY KEY, assistive_action_id BIGINT NOT NULL, draft_plan_id BIGINT NOT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    outcome_result VARCHAR(32) NOT NULL, usefulness_rating INT NOT NULL,
    reason_code VARCHAR(64) NULL, feedback_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_assistive_quality_review_action(assistive_action_id),
    KEY idx_gov_assistive_quality_review_plan(draft_plan_id),
    KEY idx_gov_assistive_quality_review_result(outcome_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance assistive action quality review';

CREATE TABLE IF NOT EXISTS governance_package_review_evaluation (
    id BIGINT PRIMARY KEY, package_id BIGINT NOT NULL, draft_plan_id BIGINT NULL,
    operator_id BIGINT NULL, operator_name VARCHAR(128) NULL,
    evaluation_result VARCHAR(32) NOT NULL, completeness_score INT NOT NULL,
    accuracy_score INT NOT NULL, overall_score INT NOT NULL,
    reason_code VARCHAR(64) NULL, review_notes_text TEXT NULL, reviewed_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    KEY idx_gov_package_review_evaluation_package(package_id),
    KEY idx_gov_package_review_evaluation_result(evaluation_result, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance package review evaluation';

-- V58: Governance draft optimization tables (44C)
CREATE TABLE IF NOT EXISTS governance_draft_optimization_signal (
    id BIGINT PRIMARY KEY, signal_type VARCHAR(64) NOT NULL, scope_type VARCHAR(64) NOT NULL,
    scope_key VARCHAR(128) NULL, adoption_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    rejection_rate DECIMAL(10,2) NOT NULL DEFAULT 0, avg_usefulness_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    sample_count INT NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    suggestion_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_draft_optimization_signal_type(signal_type, captured_at),
    KEY idx_gov_draft_optimization_scope(scope_type, scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance draft optimization signal';

CREATE TABLE IF NOT EXISTS governance_assistive_ordering_optimization (
    id BIGINT PRIMARY KEY, action_type VARCHAR(64) NOT NULL, avg_usefulness_rating DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_action_order DECIMAL(10,2) NOT NULL DEFAULT 0, usefulness_count INT NOT NULL DEFAULT 0,
    not_useful_count INT NOT NULL DEFAULT 0, optimization_level VARCHAR(32) NOT NULL,
    suggested_new_order INT NOT NULL DEFAULT 0, rationale_text TEXT NULL,
    captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_assistive_ordering_optimization_type(action_type, captured_at),
    KEY idx_gov_assistive_ordering_optimization_level(optimization_level, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance assistive ordering optimization';

CREATE TABLE IF NOT EXISTS governance_package_composition_tuning (
    id BIGINT PRIMARY KEY, score_range VARCHAR(32) NOT NULL, avg_completeness DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_accuracy DECIMAL(10,2) NOT NULL DEFAULT 0, avg_overall DECIMAL(10,2) NOT NULL DEFAULT 0,
    sample_count INT NOT NULL DEFAULT 0, tuning_level VARCHAR(32) NOT NULL,
    suggestion_text TEXT NULL, captured_at DATETIME NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_package_composition_tuning_range(score_range, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance package composition tuning';

-- V59: Governance portfolio benchmark tables (45A)
CREATE TABLE IF NOT EXISTS governance_portfolio_benchmark_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, benchmark_window VARCHAR(32) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, metric_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    percentile_rank DECIMAL(8,2) NOT NULL DEFAULT 0, peer_avg DECIMAL(10,2) NOT NULL DEFAULT 0,
    peer_p90 DECIMAL(10,2) NOT NULL DEFAULT 0, sample_count INT NOT NULL DEFAULT 0,
    signal_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_benchmark_snapshot_date(snapshot_date, metric_key),
    KEY idx_gov_benchmark_snapshot_window(benchmark_window, metric_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance portfolio benchmark snapshot';

CREATE TABLE IF NOT EXISTS governance_best_practice_alignment_item (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, practice_type VARCHAR(64) NOT NULL,
    alignment_level VARCHAR(32) NOT NULL, current_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    target_score DECIMAL(10,2) NOT NULL DEFAULT 0, gap DECIMAL(10,2) NOT NULL DEFAULT 0,
    suggestion_text TEXT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_best_practice_alignment_project(project_id, snapshot_date),
    KEY idx_gov_best_practice_alignment_practice(practice_type, alignment_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance best practice alignment item';

CREATE TABLE IF NOT EXISTS governance_maturity_scorecard (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, maturity_level VARCHAR(32) NOT NULL,
    total_score DECIMAL(8,2) NOT NULL DEFAULT 0, draft_adoption_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    assistive_quality_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    package_quality_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    outcome_review_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    operator_productivity_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_maturity_scorecard_date(snapshot_date, maturity_level),
    KEY idx_gov_maturity_scorecard_project(project_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance maturity scorecard';

-- V60: Governance benchmark adoption tables (45B)
CREATE TABLE IF NOT EXISTS governance_benchmark_adoption_record (
    id BIGINT PRIMARY KEY, project_id BIGINT NOT NULL, project_name VARCHAR(255) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, adoption_status VARCHAR(32) NOT NULL,
    current_score DECIMAL(10,2) NOT NULL DEFAULT 0, target_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    blocker_type VARCHAR(64) NULL, blocker_note TEXT NULL, owner_id BIGINT NULL,
    owner_name VARCHAR(128) NULL, adopted_at DATETIME NULL, create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_gov_benchmark_adoption_project(project_id, adoption_status),
    KEY idx_gov_benchmark_adoption_metric(metric_key, adoption_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance benchmark adoption record';

CREATE TABLE IF NOT EXISTS governance_cross_team_improvement_campaign (
    id BIGINT PRIMARY KEY, campaign_key VARCHAR(64) NOT NULL, campaign_name VARCHAR(255) NOT NULL,
    campaign_status VARCHAR(32) NOT NULL, target_project_ids_json JSON NOT NULL,
    source_project_id BIGINT NULL, source_practice_type VARCHAR(64) NULL,
    improvement_window VARCHAR(32) NOT NULL, goal_text TEXT NULL, notes_text TEXT NULL,
    create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
    UNIQUE KEY uk_gov_improvement_campaign_key(campaign_key),
    KEY idx_gov_improvement_campaign_status(campaign_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance cross-team improvement campaign';

CREATE TABLE IF NOT EXISTS governance_uplift_measurement_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, campaign_key VARCHAR(64) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, before_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    after_score DECIMAL(10,2) NOT NULL DEFAULT 0, uplift DECIMAL(10,2) NOT NULL DEFAULT 0,
    uplift_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    KEY idx_gov_uplift_snapshot_project(project_id, snapshot_date),
    KEY idx_gov_uplift_snapshot_campaign(campaign_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance uplift measurement snapshot';

-- V61: Governance uplift optimization tables (45C)
CREATE TABLE IF NOT EXISTS governance_benchmark_evolution_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, benchmark_type VARCHAR(32) NOT NULL,
    metric_key VARCHAR(64) NOT NULL, current_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    previous_value DECIMAL(10,2) NOT NULL DEFAULT 0, delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    delta_percentage DECIMAL(10,2) NOT NULL DEFAULT 0, signal_level VARCHAR(32) NOT NULL,
    sample_count INT NOT NULL DEFAULT 0, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_evolution_snapshot_date(snapshot_date, benchmark_type),
    KEY idx_gov_evolution_snapshot_metric(metric_key, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance benchmark evolution snapshot';

CREATE TABLE IF NOT EXISTS governance_campaign_effectiveness_ranking (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, campaign_key VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(255) NOT NULL, ranking_window VARCHAR(32) NOT NULL,
    avg_uplift DECIMAL(10,2) NOT NULL DEFAULT 0, project_count INT NOT NULL DEFAULT 0,
    effectiveness_level VARCHAR(32) NOT NULL, rank_position INT NOT NULL DEFAULT 0,
    summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_campaign_ranking_date(snapshot_date, rank_position),
    KEY idx_gov_campaign_ranking_effectiveness(effectiveness_level, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance campaign effectiveness ranking';

CREATE TABLE IF NOT EXISTS governance_progress_map_snapshot (
    id BIGINT PRIMARY KEY, snapshot_date DATE NOT NULL, project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL, metric_key VARCHAR(64) NOT NULL,
    baseline_score DECIMAL(10,2) NOT NULL DEFAULT 0, current_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    target_score DECIMAL(10,2) NOT NULL DEFAULT 0, progress_percentage DECIMAL(10,2) NOT NULL DEFAULT 0,
    signal_level VARCHAR(32) NOT NULL, summary_text VARCHAR(255) NOT NULL, create_time DATETIME NOT NULL,
    KEY idx_gov_progress_map_snapshot_date(snapshot_date, project_id),
    KEY idx_gov_progress_map_snapshot_signal(signal_level, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance progress map snapshot';
