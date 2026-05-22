-- Read Code Index
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, parameter_schema_json, enabled_by_default, sort_order)
VALUES (910201, 'READ_CODE_INDEX', '读取代码索引摘要', '读取项目代码索引摘要信息，包括文件数、符号数和切片数', 'READ_ONLY', 'LOW', 'MOCK_EXECUTE',
        '{"schemaVersion":2,"groups":[{"key":"scope","title":"索引范围","fields":["branch","pathPrefix","maxFiles"]}],"fields":[{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"maxFiles","label":"最大文件数","type":"number","required":true,"defaultValue":100,"min":1,"max":500}]}',
        1, 201);

-- Search Code Symbol
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, parameter_schema_json, enabled_by_default, sort_order)
VALUES (910202, 'SEARCH_CODE_SYMBOL', '搜索代码符号', '搜索代码中的类、方法、函数等符号', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE',
        '{"schemaVersion":2,"groups":[{"key":"query","title":"搜索条件","fields":["keyword","branch","language","limit"]}],"fields":[{"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"language","label":"语言","type":"select","required":false,"defaultValue":"ALL","options":["ALL","java","ts","js","vue","sql","md"]},{"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}]}',
        0, 202);

-- Search Code Chunk
INSERT IGNORE INTO tool_catalog (id, tool_key, name, description, tool_type, risk_level, execution_mode, parameter_schema_json, enabled_by_default, sort_order)
VALUES (910203, 'SEARCH_CODE_CHUNK', '搜索代码片段', '搜索代码文件内容中的匹配片段', 'READ_ONLY', 'MEDIUM', 'MOCK_EXECUTE',
        '{"schemaVersion":2,"groups":[{"key":"query","title":"搜索条件","fields":["keyword","branch","pathPrefix","limit"]}],"fields":[{"key":"keyword","label":"关键词","type":"text","required":true,"defaultValue":"","maxLength":128},{"key":"branch","label":"分支","type":"text","required":false,"defaultValue":"","maxLength":128},{"key":"pathPrefix","label":"路径前缀","type":"text","required":false,"defaultValue":"","maxLength":256,"pathRules":{"deny":[".env",".env.*",".git/**","*.pem","*.key"],"allowPrefixes":["backend/src","frontend/src","docs"]}},{"key":"limit","label":"结果数量","type":"number","required":true,"defaultValue":10,"min":1,"max":50}]}',
        0, 203);
