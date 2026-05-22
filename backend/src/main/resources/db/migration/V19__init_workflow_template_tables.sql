CREATE TABLE IF NOT EXISTS workflow_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'MULTI_AGENT',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    built_in TINYINT NOT NULL DEFAULT 0,
    template_json JSON NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workflow_template_key (template_key),
    INDEX idx_workflow_template_status (status),
    INDEX idx_workflow_template_category (category),
    INDEX idx_workflow_template_builtin (built_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板表';

-- Seed 4 built-in templates
INSERT INTO workflow_template (id, template_key, name, description, category, status, built_in, template_json) VALUES
(900001, 'STANDARD_DELIVERY', '标准交付流程', '架构 → 后端/前端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"STANDARD_DELIVERY","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"IMPLEMENTATION","title":"实现方案并行分析","steps":[{"stepOrder":1,"stepType":"BACKEND_IMPLEMENTATION_PLAN","agentCode":"backend-agent","laneKey":"backend","title":"后端实现计划"},{"stepOrder":2,"stepType":"FRONTEND_IMPLEMENTATION_PLAN","agentCode":"frontend-agent","laneKey":"frontend","title":"前端实现计划"},{"stepOrder":3,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900002, 'BACKEND_FOCUSED', '后端优先流程', '架构 → 后端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"BACKEND_FOCUSED","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"BACKEND_IMPLEMENTATION","title":"后端实现分析","steps":[{"stepOrder":1,"stepType":"BACKEND_IMPLEMENTATION_PLAN","agentCode":"backend-agent","laneKey":"backend","title":"后端实现计划"},{"stepOrder":2,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的后端实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900003, 'FRONTEND_FOCUSED', '前端优先流程', '架构 → 前端/测试并行 → 审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"FRONTEND_FOCUSED","phases":[{"phaseOrder":1,"phaseKey":"PLANNING","title":"架构规划","steps":[{"stepOrder":1,"stepType":"ARCHITECTURE_ANALYSIS","agentCode":"architect-agent","laneKey":"architect","title":"架构分析"}]},{"phaseOrder":2,"phaseKey":"FRONTEND_IMPLEMENTATION","title":"前端实现分析","steps":[{"stepOrder":1,"stepType":"FRONTEND_IMPLEMENTATION_PLAN","agentCode":"frontend-agent","laneKey":"frontend","title":"前端实现计划"},{"stepOrder":2,"stepType":"TEST_PLAN","agentCode":"test-agent","laneKey":"test","title":"测试计划"}]},{"phaseOrder":3,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":4,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[{"gateKey":"IMPLEMENTATION_PLAN_APPROVAL","title":"实施方案审批","description":"请确认多智能体生成的前端实施方案是否可以进入审查与总结阶段。","afterPhaseOrder":2}]}'),

(900004, 'REVIEW_ONLY', '审查流程', '审查 → 总结', 'MULTI_AGENT', 'ENABLED', 1,
 '{"strategyKey":"REVIEW_ONLY","phases":[{"phaseOrder":1,"phaseKey":"REVIEW","title":"综合审查","steps":[{"stepOrder":1,"stepType":"CODE_REVIEW","agentCode":"review-agent","laneKey":"review","title":"代码审查"}]},{"phaseOrder":2,"phaseKey":"SUMMARY","title":"最终总结","steps":[{"stepOrder":1,"stepType":"FINAL_SUMMARY","agentCode":"architect-agent","laneKey":"summary","title":"最终总结"}]}],"approvalGates":[]}')
ON DUPLICATE KEY UPDATE name = VALUES(name);
