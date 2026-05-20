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
