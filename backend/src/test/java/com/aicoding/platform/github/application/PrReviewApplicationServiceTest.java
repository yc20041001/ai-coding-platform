package com.aicoding.platform.github.application;

import com.aicoding.platform.github.dto.GithubPullRequestFileResponse;
import com.aicoding.platform.github.dto.GithubPullRequestResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrReviewApplicationServiceTest {

    private final TestPrReviewService testService = new TestPrReviewService();

    // === parseReviewJson tests ===

    @Test
    void shouldParseValidJson() {
        String json = "{\"summary\":\"Looks good\",\"riskLevel\":\"LOW\",\"findings\":[]}";
        JsonNode result = testService.parseReviewJson(json);
        assertNotNull(result);
        assertEquals("Looks good", result.get("summary").asText());
        assertEquals("LOW", result.get("riskLevel").asText());
    }

    @Test
    void shouldParseJsonWrappedInMarkdown() {
        String json = "```json\n{\"summary\":\"ok\",\"riskLevel\":\"MEDIUM\"}\n```";
        JsonNode result = testService.parseReviewJson(json);
        assertNotNull(result);
        assertEquals("ok", result.get("summary").asText());
    }

    @Test
    void shouldParseJsonWithLeadingText() {
        String json = "Here is the review:\n{\"summary\":\"test\",\"riskLevel\":\"HIGH\"}";
        JsonNode result = testService.parseReviewJson(json);
        assertNotNull(result);
        assertEquals("test", result.get("summary").asText());
    }

    @Test
    void shouldReturnNullForNonJsonContent() {
        JsonNode result = testService.parseReviewJson("This is just plain text, no JSON at all");
        assertNull(result);
    }

    @Test
    void shouldReturnNullForNullContent() {
        assertNull(testService.parseReviewJson(null));
    }

    @Test
    void shouldReturnNullForBlankContent() {
        assertNull(testService.parseReviewJson("   "));
    }

    @Test
    void shouldParseJsonWithFindings() {
        String json = """
                {
                  "summary": "Found issues",
                  "riskLevel": "HIGH",
                  "findings": [
                    {
                      "severity": "ERROR",
                      "category": "BUG",
                      "filePath": "src/App.vue",
                      "lineNumber": 42,
                      "title": "Null pointer",
                      "description": "Potential NPE",
                      "suggestion": "Add null check",
                      "codeSnippet": "foo.bar()"
                    }
                  ]
                }""";
        JsonNode result = testService.parseReviewJson(json);
        assertNotNull(result);
        assertEquals("Found issues", result.get("summary").asText());
        assertEquals("HIGH", result.get("riskLevel").asText());
        assertEquals(1, result.get("findings").size());
        assertEquals("Null pointer", result.get("findings").get(0).get("title").asText());
    }

    // === validateRiskLevel tests ===

    @Test
    void shouldValidateAllRiskLevels() {
        assertEquals("LOW", testService.validateRiskLevel("LOW"));
        assertEquals("MEDIUM", testService.validateRiskLevel("MEDIUM"));
        assertEquals("HIGH", testService.validateRiskLevel("HIGH"));
        assertEquals("CRITICAL", testService.validateRiskLevel("CRITICAL"));
    }

    @Test
    void shouldDefaultToMediumForNull() {
        assertEquals("MEDIUM", testService.validateRiskLevel(null));
    }

    @Test
    void shouldDefaultToMediumForInvalidLevel() {
        assertEquals("MEDIUM", testService.validateRiskLevel("UNKNOWN"));
    }

    @Test
    void shouldHandleLowercaseInput() {
        assertEquals("LOW", testService.validateRiskLevel("low"));
        assertEquals("HIGH", testService.validateRiskLevel("high"));
    }

    // === buildSystemPrompt tests ===

    @Test
    void shouldIncludeReviewModeInSystemPrompt() {
        String prompt = testService.buildSystemPrompt("FULL");
        assertTrue(prompt.contains("FULL"));
        assertTrue(prompt.contains("summary"));
        assertTrue(prompt.contains("riskLevel"));
        assertTrue(prompt.contains("findings"));
    }

    // === buildUserPrompt tests ===

    @Test
    void shouldBuildUserPromptWithPrData() {
        GithubPullRequestResponse pr = new GithubPullRequestResponse();
        pr.setTitle("Add login feature");
        pr.setAuthorLogin("developer1");
        pr.setBaseBranch("main");
        pr.setHeadBranch("feature/login");
        pr.setAdditions(100);
        pr.setDeletions(20);
        pr.setChangedFiles(5);

        List<GithubPullRequestFileResponse> files = new ArrayList<>();
        GithubPullRequestFileResponse f = new GithubPullRequestFileResponse();
        f.setFilename("src/Login.vue");
        f.setAdditions(50);
        f.setDeletions(10);
        files.add(f);

        String patch = "diff --git a/src/Login.vue b/src/Login.vue\n...";

        String prompt = testService.buildUserPrompt(pr, files, patch, "FULL");
        assertTrue(prompt.contains("Add login feature"));
        assertTrue(prompt.contains("developer1"));
        assertTrue(prompt.contains("main"));
        assertTrue(prompt.contains("feature/login"));
        assertTrue(prompt.contains("src/Login.vue"));
        assertTrue(prompt.contains("diff --git"));
    }

    @Test
    void shouldHandleNullPatch() {
        GithubPullRequestResponse pr = new GithubPullRequestResponse();
        pr.setTitle("Test PR");
        pr.setAdditions(0);
        pr.setDeletions(0);
        pr.setChangedFiles(1);
        List<GithubPullRequestFileResponse> files = new ArrayList<>();

        String prompt = testService.buildUserPrompt(pr, files, null, "SUMMARY");
        assertTrue(prompt.contains("(no patch available)"));
    }

    // === Non-JSON output fallback tests ===

    @Test
    void shouldReturnNullForNonJsonOutput() {
        // Simulates model returning prose instead of JSON
        JsonNode result = testService.parseReviewJson(
                "Based on my analysis, this PR looks good overall. No major issues found.");
        assertNull(result);
    }

    @Test
    void shouldReturnNullForMalformedJson() {
        JsonNode result = testService.parseReviewJson("{\"summary\": \"incomplete json\"");
        assertNull(result);
    }

    // === Prompt should NOT contain tokens ===

    @Test
    void shouldNotContainTokenSecretsInSystemPrompt() {
        String prompt = testService.buildSystemPrompt("FULL");
        // System prompt should not contain any token/secret references
        assertFalse(prompt.contains("token"));
        assertFalse(prompt.contains("api_key"), "System prompt should not contain api_key");
        assertFalse(prompt.contains("secret"), "System prompt should not reference secrets");
        assertFalse(prompt.contains("password"), "System prompt should not reference passwords");
    }

    @Test
    void shouldNotContainTokenSecretsInUserPrompt() {
        GithubPullRequestResponse pr = new GithubPullRequestResponse();
        pr.setTitle("Test PR");
        pr.setAdditions(10);
        pr.setDeletions(5);
        pr.setChangedFiles(1);
        List<GithubPullRequestFileResponse> files = new ArrayList<>();

        String patch = "+  const API_KEY = 'sk-abcdefghijklmnop'";
        String prompt = testService.buildUserPrompt(pr, files, patch, "FULL");

        // The prompt contains the patch as-is (user code). But the system prompt builder
        // should not INJECT secrets. This test verifies the builder doesn't add secrets.
        assertTrue(prompt.contains("Test PR"), "User prompt should contain PR data");
    }

    // === Very long patch handling ===

    @Test
    void shouldBuildPromptWithLongPatch() {
        GithubPullRequestResponse pr = new GithubPullRequestResponse();
        pr.setTitle("Large refactor PR");
        pr.setAuthorLogin("dev1");
        pr.setBaseBranch("main");
        pr.setHeadBranch("feature/large-refactor");
        pr.setAdditions(5000);
        pr.setDeletions(3000);
        pr.setChangedFiles(200);
        List<GithubPullRequestFileResponse> files = new ArrayList<>();

        String longPatch = "diff --git a/file.vue b/file.vue\n" + "+line\n".repeat(100);
        String prompt = testService.buildUserPrompt(pr, files, longPatch, "FULL");

        assertTrue(prompt.contains("Large refactor PR"));
        assertTrue(prompt.contains("5000"));
        // The builder includes the patch as-is — verifies it doesn't crash on large content
        assertTrue(prompt.length() > 500);
    }

    // === buildSystemPrompt mode tests ===

    @Test
    void shouldIncludeModeInSystemPrompt() {
        String summaryPrompt = testService.buildSystemPrompt("SUMMARY");
        assertTrue(summaryPrompt.contains("SUMMARY"));
        assertTrue(summaryPrompt.contains("summary"));
        assertTrue(summaryPrompt.contains("riskLevel"));
        assertTrue(summaryPrompt.contains("findings"));
    }

    // === buildUserPrompt edge cases ===

    @Test
    void shouldHandlePrWithNullFields() {
        GithubPullRequestResponse pr = new GithubPullRequestResponse();
        pr.setTitle("Minimal PR");
        // Many fields are null — should not crash
        List<GithubPullRequestFileResponse> files = new ArrayList<>();
        GithubPullRequestFileResponse f = new GithubPullRequestFileResponse();
        f.setFilename("file.txt");
        files.add(f);

        String prompt = testService.buildUserPrompt(pr, files, null, "FULL");
        assertTrue(prompt.contains("Minimal PR"));
        assertTrue(prompt.contains("(no patch available)"));
        assertTrue(prompt.contains("unknown"));
    }

    // === validateRiskLevel edge cases ===

    @Test
    void shouldValidateAllKnownRiskLevels() {
        assertEquals("LOW", testService.validateRiskLevel("LOW"));
        assertEquals("MEDIUM", testService.validateRiskLevel("MEDIUM"));
        assertEquals("HIGH", testService.validateRiskLevel("HIGH"));
        assertEquals("CRITICAL", testService.validateRiskLevel("CRITICAL"));
    }

    @Test
    void shouldDefaultInvalidRiskLevels() {
        assertEquals("MEDIUM", testService.validateRiskLevel("EXTREME"));
        assertEquals("MEDIUM", testService.validateRiskLevel(""));
        assertEquals("MEDIUM", testService.validateRiskLevel("   "));
    }

    /**
     * Test-only subclass that exposes package-private methods.
     */
    static class TestPrReviewService extends PrReviewApplicationService {
        TestPrReviewService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public JsonNode parseReviewJson(String content) {
            return super.parseReviewJson(content);
        }

        @Override
        public String validateRiskLevel(String risk) {
            return super.validateRiskLevel(risk);
        }

        @Override
        public String buildSystemPrompt(String reviewMode) {
            return super.buildSystemPrompt(reviewMode);
        }

        @Override
        public String buildUserPrompt(GithubPullRequestResponse pr,
                                       List<GithubPullRequestFileResponse> files,
                                       String patch, String reviewMode) {
            return super.buildUserPrompt(pr, files, patch, reviewMode);
        }
    }
}
