package com.aicoding.platform.support;

import java.util.List;
import java.util.Map;

/**
 * Lightweight test data factory — generates unique test data.
 * Avoids repeated JSON map construction across integration tests.
 */
public class TestDataFactory {

    private TestDataFactory() {}

    public static String uniqueSuffix() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static Map<String, Object> createProject(String name) {
        return Map.of(
                "name", name + "-" + uniqueSuffix(),
                "description", "Test project created by TestDataFactory",
                "techStack", List.of("Java", "Spring")
        );
    }

    public static Map<String, Object> createTask(String title, String agentId) {
        return Map.of(
                "title", title + "-" + uniqueSuffix(),
                "description", "Test task created by TestDataFactory",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", agentId
        );
    }

    public static Map<String, Object> executeTask(String instruction, String agentId) {
        return Map.of(
                "instruction", instruction,
                "agentId", agentId,
                "useRag", false,
                "ragLimit", 5
        );
    }

    public static Map<String, Object> createChatSession(String title) {
        return Map.of(
                "title", title + "-" + uniqueSuffix(),
                "sessionType", "PROJECT"
        );
    }

    public static Map<String, Object> sendMessage(String content, String agentId) {
        return Map.of(
                "content", content,
                "agentIds", List.of(agentId),
                "stream", false,
                "useRag", false,
                "ragLimit", 5
        );
    }

    public static Map<String, Object> createKnowledgeBase(String name) {
        return Map.of(
                "name", name + "-" + uniqueSuffix(),
                "description", "Test KB created by TestDataFactory",
                "chunkSize", 200,
                "chunkOverlap", 20
        );
    }

    public static Map<String, Object> uploadDocument(String kbId, String title, String content) {
        return Map.of(
                "knowledgeBaseId", kbId,
                "title", title + "-" + uniqueSuffix(),
                "documentType", "MARKDOWN",
                "sourceType", "MANUAL",
                "fileName", "test-doc-" + uniqueSuffix() + ".md",
                "content", content
        );
    }
}
