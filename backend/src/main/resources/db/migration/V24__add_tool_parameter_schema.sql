-- V24: Add Tool Parameter Schema support

ALTER TABLE tool_catalog
    ADD COLUMN parameter_schema_json JSON NULL AFTER policy_json;

ALTER TABLE project_tool_config
    ADD COLUMN parameters_json JSON NULL AFTER config_json;

-- Update 6 built-in tools with parameter_schema_json

-- 910001: PROJECT_CONTEXT_SCAN
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"scope","label":"扫描范围","type":"select","required":true,"defaultValue":"TASK","options":["TASK","PROJECT","CURRENT_PHASE"]},
    {"key":"includeMetadata","label":"包含元数据","type":"boolean","required":false,"defaultValue":true}
  ]
}' WHERE id = 910001;

-- 910002: TASK_REQUIREMENT_ANALYSIS
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"depth","label":"分析深度","type":"select","required":true,"defaultValue":"STANDARD","options":["BASIC","STANDARD","DETAILED"]},
    {"key":"maxFindings","label":"最大建议数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}
  ]
}' WHERE id = 910002;

-- 910003: MOCK_FILE_INSPECTION
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"includeStyleHints","label":"包含样式建议","type":"boolean","required":false,"defaultValue":true}
  ]
}' WHERE id = 910003;

-- 910004: MOCK_TEST_PLAN_SCAN
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"includeEdgeCases","label":"包含边界用例","type":"boolean","required":false,"defaultValue":true},
    {"key":"testLevel","label":"测试级别","type":"select","required":true,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"]}
  ]
}' WHERE id = 910004;

-- 910005: MOCK_SECURITY_REVIEW
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"riskFocus","label":"风险重点","type":"select","required":true,"defaultValue":"STANDARD","options":["STANDARD","AUTH","DATA","DEPENDENCY"]},
    {"key":"maxFindings","label":"最大风险数","type":"number","required":true,"defaultValue":5,"min":1,"max":20}
  ]
}' WHERE id = 910005;

-- 910006: MOCK_PATCH_PROPOSAL
UPDATE tool_catalog SET parameter_schema_json = '{
  "fields": [
    {"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},
    {"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},
    {"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128}
  ]
}' WHERE id = 910006;
