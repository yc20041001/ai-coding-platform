-- V28: Upgrade tool parameter schema to schemaVersion=2 with advanced schema features
-- Adds: schemaVersion, groups, dependsOn, array, pathRules

-- Upgrade MOCK_PATCH_PROPOSAL (910006) with targetFiles array + dependsOn
UPDATE tool_catalog SET parameter_schema_json = '{
  "schemaVersion": 2,
  "groups": [
    {"key":"scope","title":"提案范围","description":"控制提案的分析范围和目标","fields":["proposalScope","targetArea","targetFiles","maxChangedFiles"]},
    {"key":"tests","title":"测试建议","description":"控制测试提案的行为","fields":["includeTests","testLevel"]}
  ],
  "fields": [
    {"key":"proposalScope","label":"提案范围","type":"select","required":true,"defaultValue":"MINIMAL","options":["MINIMAL","STANDARD","EXPANDED"]},
    {"key":"targetArea","label":"目标区域","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"targetFiles","label":"目标文件","type":"array","itemType":"text","required":false,"defaultValue":[],"maxItems":10,"itemMaxLength":256,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"maxChangedFiles","label":"最大变更文件数","type":"number","required":true,"defaultValue":3,"min":1,"max":10},
    {"key":"includeTests","label":"包含测试提案","type":"boolean","required":false,"defaultValue":true},
    {"key":"testLevel","label":"测试级别","type":"select","required":false,"defaultValue":"INTEGRATION","options":["UNIT","INTEGRATION","E2E"],
      "dependsOn":{"field":"includeTests","equals":true}}
  ]
}' WHERE id = 910006;

-- Upgrade READ_FILE_SNIPPET (910102) with groups + pathRules
UPDATE tool_catalog SET parameter_schema_json = '{
  "schemaVersion": 2,
  "groups": [
    {"key":"target","title":"读取目标","description":"指定文件读取的范围","fields":["branch","filePath","startLine","maxLines"]}
  ],
  "fields": [
    {"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},
    {"key":"filePath","label":"文件路径","type":"text","required":true,"defaultValue":"","maxLength":512,
      "pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},
    {"key":"startLine","label":"起始行","type":"number","required":false,"defaultValue":1,"min":1,"max":100000},
    {"key":"maxLines","label":"最大行数","type":"number","required":true,"defaultValue":80,"min":1,"max":300}
  ]
}' WHERE id = 910102;
