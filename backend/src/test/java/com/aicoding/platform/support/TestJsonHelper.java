package com.aicoding.platform.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Objects;

public class TestJsonHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode parse(String json) {
        String content = Objects.requireNonNull(json, "json");
        try {
            return mapper.readTree(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON: " + content, e);
        }
    }

    public static String getString(JsonNode root, String jsonPath) {
        JsonNode node = navigate(root, jsonPath);
        return node != null && !node.isNull() ? node.asText() : "";
    }

    public static long getLong(JsonNode root, String jsonPath) {
        JsonNode node = navigate(root, jsonPath);
        return node != null && !node.isNull() ? node.asLong() : 0L;
    }

    public static boolean getBool(JsonNode root, String jsonPath) {
        JsonNode node = navigate(root, jsonPath);
        return node != null && !node.isNull() && node.asBoolean();
    }

    private static JsonNode navigate(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (segment.isEmpty()) continue;
            if (current == null) return null;
            current = current.get(segment);
        }
        return current;
    }
}
