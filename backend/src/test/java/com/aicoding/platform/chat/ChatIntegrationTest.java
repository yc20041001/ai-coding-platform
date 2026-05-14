package com.aicoding.platform.chat;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatIntegrationTest extends IntegrationTestBase {

    private String createProject() {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Chat-Project-" + System.currentTimeMillis(),
                "description", "Chat integration test",
                "techStack", List.of("Java")
        ));
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    @Test
    void shouldCreateChatSession() {
        String projectId = createProject();
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/chat/sessions", Map.of(
                "title", "IT-Chat-Session-" + System.currentTimeMillis(),
                "sessionType", "PROJECT"
        ));

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.status")).isIn("ACTIVE", "active");
    }

    @Test
    void shouldSendMessageAndCreateAssistantReply() {
        String projectId = createProject();

        // Create session
        ResponseEntity<String> sessionRes = post("/api/projects/" + projectId + "/chat/sessions", Map.of(
                "title", "IT-Chat-Msg-" + System.currentTimeMillis(),
                "sessionType", "PROJECT"
        ));
        String sessionId = TestJsonHelper.getString(
                TestJsonHelper.parse(sessionRes.getBody()), "data.id");

        // Send message
        ResponseEntity<String> sendRes = post("/api/chat/sessions/" + sessionId + "/messages", Map.of(
                "content", "Hello, this is an integration test message",
                "agentIds", List.of(AGENT_ID),
                "stream", false,
                "useRag", false,
                "ragLimit", 5
        ));
        assertOk(sendRes);
        JsonNode sendRoot = TestJsonHelper.parse(sendRes.getBody());
        String userMsgId = TestJsonHelper.getString(sendRoot, "data.userMessageId");
        assertThat(userMsgId).isNotEmpty();
        String assistantMsgId = TestJsonHelper.getString(sendRoot, "data.assistantMessageId");
        assertThat(assistantMsgId).isNotEmpty();

        // Get messages
        ResponseEntity<String> messagesRes = get("/api/chat/sessions/" + sessionId + "/messages?limit=20");
        assertOk(messagesRes);
        String messagesJson = TestJsonHelper.parse(messagesRes.getBody()).get("data").toString();
        assertThat(messagesJson).contains(userMsgId);
        assertThat(messagesJson).contains(assistantMsgId);
    }

    @Test
    void shouldListSessions() {
        String projectId = createProject();

        post("/api/projects/" + projectId + "/chat/sessions", Map.of(
                "title", "IT-List-Session", "sessionType", "PROJECT"));

        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/chat/sessions?page=1&pageSize=10");
        assertOk(listRes);
        JsonNode root = TestJsonHelper.parse(listRes.getBody());
        assertThat(TestJsonHelper.getLong(root, "data.total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectWithoutToken() {
        ResponseEntity<String> res = getNoAuth("/api/chat/sessions/99999/messages");
        assertCode(res, "UNAUTHORIZED");
    }
}
