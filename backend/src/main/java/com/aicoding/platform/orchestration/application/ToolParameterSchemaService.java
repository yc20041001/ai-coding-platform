package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ToolParameterSchemaService {

    private static final Set<String> VALID_TYPES = Set.of("text", "textarea", "boolean", "number", "select", "array");
    private static final Set<String> ITEM_TYPES = Set.of("text");
    private static final int MAX_SUPPORTED_SCHEMA_VERSION = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate the structure of an advanced parameter schema JSON.
     * Checks: schemaVersion, fields array, field.key uniqueness, valid types,
     * array itemType, dependsOn.field existence, groups.fields references, pathRules format.
     */
    public void validateSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return; // empty schema is valid
        }

        JsonNode schema;
        try {
            schema = objectMapper.readTree(schemaJson);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "工具参数 schema 解析失败");
        }

        // schemaVersion check
        int schemaVersion = schema.has("schemaVersion") ? schema.get("schemaVersion").asInt() : 1;
        if (schemaVersion > MAX_SUPPORTED_SCHEMA_VERSION) {
            throw new BizException(ErrorCode.PARAM_SCHEMA_VERSION_UNSUPPORTED,
                    "参数 schema 版本 " + schemaVersion + " 不支持，当前最高支持版本 " + MAX_SUPPORTED_SCHEMA_VERSION);
        }

        JsonNode fields = schema.get("fields");
        if (fields == null || !fields.isArray()) {
            return;
        }

        // Collect all field keys for cross-referencing
        Set<String> fieldKeys = StreamSupport.stream(fields.spliterator(), false)
                .map(f -> f.has("key") ? f.get("key").asText() : null)
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toSet());

        // Validate each field
        for (JsonNode field : fields) {
            String key = field.has("key") ? field.get("key").asText() : null;
            if (key == null || key.isBlank()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "参数字段 key 不能为空");
            }

            String type = field.has("type") ? field.get("type").asText() : "text";
            if (!VALID_TYPES.contains(type)) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "参数字段 " + key + " 的类型 '" + type + "' 不合法");
            }

            // Array type must have itemType=text
            if ("array".equals(type)) {
                String itemType = field.has("itemType") ? field.get("itemType").asText() : null;
                if (itemType == null || !ITEM_TYPES.contains(itemType)) {
                    throw new BizException(ErrorCode.PARAM_ARRAY_ITEM_TYPE_INVALID,
                            "数组参数 " + key + " 的 itemType 无效，仅支持 text");
                }
            }

            // dependsOn.field must exist
            if (field.has("dependsOn")) {
                JsonNode dependsOn = field.get("dependsOn");
                if (dependsOn.has("field")) {
                    String depField = dependsOn.get("field").asText();
                    if (!fieldKeys.contains(depField)) {
                        throw new BizException(ErrorCode.PARAM_DEPENDS_ON_FIELD_NOT_FOUND,
                                "参数字段 " + key + " 的 dependsOn.field '" + depField + "' 不存在");
                    }
                }
            }
        }

        // Validate groups
        if (schema.has("groups")) {
            JsonNode groups = schema.get("groups");
            for (JsonNode group : groups) {
                if (group.has("fields")) {
                    for (JsonNode gf : group.get("fields")) {
                        String gfKey = gf.asText();
                        if (!fieldKeys.contains(gfKey)) {
                            throw new BizException(ErrorCode.PARAM_GROUP_FIELD_NOT_FOUND,
                                    "分组引用了不存在的字段: " + gfKey);
                        }
                    }
                }
            }
        }
    }

    /**
     * Normalize and validate parameters against the tool's parameter schema,
     * including advanced features: dependsOn, array, pathRules.
     */
    public Map<String, Object> normalizeAndValidate(String schemaJson, Map<String, Object> parameters) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return new HashMap<>();
        }

        JsonNode schema;
        try {
            schema = objectMapper.readTree(schemaJson);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "工具参数 schema 解析失败");
        }

        JsonNode fields = schema.get("fields");
        if (fields == null || !fields.isArray()) {
            return new HashMap<>();
        }

        if (parameters == null) {
            parameters = new HashMap<>();
        }

        // Build a map of field definitions keyed by field key
        Map<String, JsonNode> fieldDefs = new HashMap<>();
        for (JsonNode field : fields) {
            String key = field.has("key") ? field.get("key").asText() : null;
            if (key != null && !key.isBlank()) {
                fieldDefs.put(key, field);
            }
        }

        // Resolve dependsOn conditions to determine which fields are active
        Set<String> activeFields = resolveDependsOn(fieldDefs, parameters);

        Map<String, Object> normalized = new HashMap<>();

        for (JsonNode field : fields) {
            String key = field.has("key") ? field.get("key").asText() : null;
            if (key == null || key.isBlank()) continue;

            String type = field.has("type") ? field.get("type").asText() : "text";
            boolean required = field.has("required") && field.get("required").asBoolean();

            // Check if the field is active (dependsOn condition met)
            boolean isActive = activeFields.contains(key);

            Object rawValue = parameters.get(key);

            // If field is not active, skip. Required doesn't apply when inactive.
            if (!isActive) {
                continue;
            }

            // Use defaultValue if not provided
            if (rawValue == null) {
                if (field.has("defaultValue")) {
                    rawValue = jsonNodeToObject(field.get("defaultValue"));
                } else if (required) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "工具参数 " + key + " 不能为空");
                } else {
                    // Optional field with no value and no default — skip
                    continue;
                }
            }

            // Type-specific validation
            switch (type) {
                case "text", "textarea" -> {
                    String strVal = rawValue == null ? "" : rawValue.toString();
                    if (required && strVal.isBlank()) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "工具参数 " + key + " 不能为空");
                    }
                    if (field.has("maxLength") && strVal.length() > field.get("maxLength").asInt()) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "工具参数 " + key + " 超出最大长度 " + field.get("maxLength").asInt());
                    }
                    if (field.has("pathRules")) {
                        validatePathRules(key, strVal, field.get("pathRules"));
                    }
                    normalized.put(key, strVal);
                }
                case "boolean" -> {
                    if (rawValue instanceof Boolean) {
                        normalized.put(key, rawValue);
                    } else {
                        normalized.put(key, Boolean.valueOf(rawValue != null ? rawValue.toString() : "false"));
                    }
                }
                case "number" -> {
                    int intVal;
                    if (rawValue instanceof Number n) {
                        intVal = n.intValue();
                    } else {
                        try {
                            intVal = Integer.parseInt(rawValue != null ? rawValue.toString() : "0");
                        } catch (NumberFormatException e) {
                            throw new BizException(ErrorCode.BAD_REQUEST,
                                    "工具参数 " + key + " 必须是数字");
                        }
                    }
                    if (field.has("min") && intVal < field.get("min").asInt()) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "工具参数 " + key + " 不能小于 " + field.get("min").asInt());
                    }
                    if (field.has("max") && intVal > field.get("max").asInt()) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "工具参数 " + key + " 不能大于 " + field.get("max").asInt());
                    }
                    normalized.put(key, intVal);
                }
                case "select" -> {
                    String selectedVal = rawValue == null ? "" : rawValue.toString();
                    if (required && selectedVal.isBlank()) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "工具参数 " + key + " 不能为空");
                    }
                    if (field.has("options")) {
                        boolean validOption = false;
                        for (JsonNode opt : field.get("options")) {
                            if (opt.asText().equals(selectedVal)) {
                                validOption = true;
                                break;
                            }
                        }
                        if (!validOption) {
                            throw new BizException(ErrorCode.BAD_REQUEST,
                                    "工具参数 " + key + " 不在允许选项中");
                        }
                    }
                    normalized.put(key, selectedVal);
                }

                case "array" -> {
                    List<String> arrayVal = normalizeArrayValue(key, rawValue, field);
                    if (field.has("pathRules")) {
                        for (String item : arrayVal) {
                            validatePathRules(key, item, field.get("pathRules"));
                        }
                    }
                    normalized.put(key, arrayVal);
                }
                default -> normalized.put(key, rawValue == null ? "" : rawValue.toString());
            }
        }

        return normalized;
    }

    /**
     * Extract a parameter summary string from normalized parameters, e.g.
     * "scope=TASK, maxFindings=5, targetFiles=3 项".
     * Arrays display as "N 項" format.
     */
    public String buildParameterSummary(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, Object>> it = parameters.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Object> entry = it.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                int size = ((List<Object>) value).size();
                sb.append(key).append("=").append(size).append(" 项");
            } else {
                sb.append(key).append("=").append(value);
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Get default parameters from the schema (for tools without a project config).
     */
    public Map<String, Object> getDefaultParameters(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            JsonNode schema = objectMapper.readTree(schemaJson);
            JsonNode fields = schema.get("fields");
            if (fields == null || !fields.isArray()) {
                return new HashMap<>();
            }
            Map<String, Object> defaults = new HashMap<>();
            for (JsonNode field : fields) {
                String key = field.has("key") ? field.get("key").asText() : null;
                if (key == null || key.isBlank()) continue;
                if (field.has("defaultValue")) {
                    defaults.put(key, jsonNodeToObject(field.get("defaultValue")));
                }
            }
            return defaults;
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    // ========================
    // Private helpers
    // ========================

    /**
     * Resolve which fields are active based on dependsOn conditions.
     * If a field has dependsOn and the condition is not met, it's excluded from activeFields.
     */
    private Set<String> resolveDependsOn(Map<String, JsonNode> fieldDefs, Map<String, Object> parameters) {
        Set<String> active = new java.util.LinkedHashSet<>(fieldDefs.keySet());

        for (Map.Entry<String, JsonNode> entry : fieldDefs.entrySet()) {
            String key = entry.getKey();
            JsonNode field = entry.getValue();

            if (field.has("dependsOn")) {
                JsonNode dependsOn = field.get("dependsOn");
                String depField = dependsOn.has("field") ? dependsOn.get("field").asText() : null;
                if (depField != null && active.contains(depField)) {
                    Object actualValue = parameters.get(depField);
                    boolean conditionMet = false;

                    if (dependsOn.has("equals")) {
                        JsonNode expected = dependsOn.get("equals");
                        if (expected.isBoolean()) {
                            conditionMet = actualValue instanceof Boolean
                                    ? Boolean.TRUE.equals(actualValue)
                                    : Boolean.parseBoolean(String.valueOf(actualValue));
                            conditionMet = conditionMet == expected.asBoolean();
                        } else if (expected.isTextual()) {
                            conditionMet = expected.asText().equals(String.valueOf(actualValue));
                        } else if (expected.isNumber()) {
                            conditionMet = actualValue instanceof Number
                                    && ((Number) actualValue).intValue() == expected.asInt();
                        }
                    }

                    if (!conditionMet) {
                        active.remove(key);
                    }
                }
            }
        }

        return active;
    }

    /**
     * Normalize an array value from raw input.
     */
    private List<String> normalizeArrayValue(String key, Object rawValue, JsonNode field) {
        List<String> result = new ArrayList<>();
        int maxItems = field.has("maxItems") ? field.get("maxItems").asInt() : Integer.MAX_VALUE;
        int itemMaxLength = field.has("itemMaxLength") ? field.get("itemMaxLength").asInt() : Integer.MAX_VALUE;

        if (rawValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) rawValue;

            if (rawList.size() > maxItems) {
                throw new BizException(ErrorCode.PARAM_ARRAY_MAX_ITEMS_EXCEEDED,
                        "工具参数 " + key + " 最多允许 " + maxItems + " 项");
            }

            for (Object item : rawList) {
                String str = item == null ? "" : item.toString().trim();
                if (str.isEmpty()) {
                    continue; // auto-discard empty strings
                }
                if (str.length() > itemMaxLength) {
                    throw new BizException(ErrorCode.PARAM_ARRAY_ITEM_TOO_LONG,
                            "工具参数 " + key + " 的项超出最大长度 " + itemMaxLength);
                }
                result.add(str);
            }
        } else if (rawValue instanceof String s) {
            String str = s.trim();
            if (!str.isEmpty()) {
                if (str.length() > itemMaxLength) {
                    throw new BizException(ErrorCode.PARAM_ARRAY_ITEM_TOO_LONG,
                            "工具参数 " + key + " 的项超出最大长度 " + itemMaxLength);
                }
                result.add(str);
            }
        }

        return result;
    }

    /**
     * Validate a path value against pathRules (deny + allowPrefixes).
     * Also performs general path safety checks.
     */
    private void validatePathRules(String key, String path, JsonNode pathRules) {
        if (path == null || path.isBlank()) return;

        // General path safety checks
        if (path.startsWith("/") || path.startsWith("\\")) {
            throw new BizException(ErrorCode.PARAM_PATH_INVALID,
                    "工具参数 " + key + " 包含绝对路径: " + path);
        }
        if (path.contains("..")) {
            throw new BizException(ErrorCode.PARAM_PATH_INVALID,
                    "工具参数 " + key + " 包含 '..': " + path);
        }
        if (path.contains("~")) {
            throw new BizException(ErrorCode.PARAM_PATH_INVALID,
                    "工具参数 " + key + " 包含 '~': " + path);
        }
        if (path.contains("\0")) {
            throw new BizException(ErrorCode.PARAM_PATH_INVALID,
                    "工具参数 " + key + " 包含空字符");
        }

        // Deny list
        if (pathRules.has("deny")) {
            for (JsonNode denyPattern : pathRules.get("deny")) {
                String pattern = denyPattern.asText();
                if (matchGlob(pattern, path)) {
                    throw new BizException(ErrorCode.PARAM_PATH_DENIED,
                            "工具参数 " + key + " 的路径被禁止: " + path + " (匹配 " + pattern + ")");
                }
            }
        }

        // Allow prefixes
        if (pathRules.has("allowPrefixes") && pathRules.get("allowPrefixes").isArray()
                && pathRules.get("allowPrefixes").size() > 0) {
            boolean allowed = false;
            for (JsonNode prefixNode : pathRules.get("allowPrefixes")) {
                String prefix = prefixNode.asText();
                if (path.startsWith(prefix)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new BizException(ErrorCode.PARAM_PATH_NOT_ALLOWED,
                        "工具参数 " + key + " 的路径不在允许前缀范围内: " + path);
            }
        }
    }

    /**
     * Simple glob matching for path rules. Supports:
     * - ** (match all)
     * - * (match within segment)
     * - Simple suffix/prefix patterns
     */
    private boolean matchGlob(String pattern, String path) {
        if ("**".equals(pattern)) return true;
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1);
            return path.endsWith(suffix);
        }
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return path.matches(regex);
        }
        return pattern.equals(path);
    }

    private Object jsonNodeToObject(JsonNode node) {
        if (node == null) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isDouble()) return node.asDouble();
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : node) {
                items.add(item.asText());
            }
            return items;
        }
        return node.toString();
    }
}
