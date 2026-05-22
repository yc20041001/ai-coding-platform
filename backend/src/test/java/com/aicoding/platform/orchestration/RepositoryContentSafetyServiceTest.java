package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.application.RepositoryContentSafetyService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RepositoryContentSafetyService.
 * Covers binary detection, secret redaction, file size limits, and edge cases.
 */
class RepositoryContentSafetyServiceTest {

    private final RepositoryContentSafetyService service = new RepositoryContentSafetyService();

    // ========================
    // Binary Detection
    // ========================

    @Test
    void shouldReturnFalseForEmptyContent() {
        assertThat(service.isBinaryContent(new byte[0])).isFalse();
    }

    @Test
    void shouldReturnFalseForNullContent() {
        assertThat(service.isBinaryContent(null)).isFalse();
    }

    @Test
    void shouldDetectBinaryInLargeContent() {
        byte[] content = new byte[5000];
        content[4097] = 0; // NUL byte beyond first 4096
        assertThat(service.isBinaryContent(content)).isTrue();
    }

    @Test
    void shouldDetectBinaryByExtensionCaseInsensitive() {
        assertThat(service.isBinaryExtension("image.PNG")).isTrue();
        assertThat(service.isBinaryExtension("Archive.JAR")).isTrue();
    }

    @Test
    void shouldReturnFalseForNullBinaryExtension() {
        assertThat(service.isBinaryExtension(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForBlankBinaryExtension() {
        assertThat(service.isBinaryExtension("")).isFalse();
    }

    // ========================
    // Secret Redaction
    // ========================

    @Test
    void shouldRedactNullContentToNull() {
        assertThat(service.redactSecrets(null)).isNull();
    }

    @Test
    void shouldRedactBlankContentUnchanged() {
        assertThat(service.redactSecrets("")).isEqualTo("");
        assertThat(service.redactSecrets("   ")).isEqualTo("   ");
    }

    @Test
    void shouldRedactSkToken() {
        String input = "export OPENAI_API_KEY=sk-abcdefghijklmnopqrstuvwxyz123456";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
        assertThat(result).doesNotContain("sk-abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    void shouldRedactGithubToken() {
        String input = "ghp_abcdefghijklmnopqrstuvwxyz1234567890123456";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
    }

    @Test
    void shouldRedactGithubPat() {
        String input = "github_pat_abcdefghijklmnopqrstuvwxyz123456789012";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
    }

    @Test
    void shouldRedactJwtToken() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.dGVzdA";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
    }

    @Test
    void shouldRedactApiKeyAssignment() {
        String input = "api_key = 'my-secret-api-key-value'";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
    }

    @Test
    void shouldRedactSecretAssignment() {
        String input = "SECRET='my-super-secret-value'";
        String result = service.redactSecrets(input);
        assertThat(result).contains("**REDACTED**");
    }

    @Test
    void shouldCountRedactions() {
        String input = "key1=sk-abc123def456ghi789jkl012mno345\nkey2=ghp_testToken123456789012345678901234567890";
        RepositoryContentSafetyService.RedactionResult result = service.redactSecretsWithCount(input);
        assertThat(result.getRedactionCount()).isGreaterThanOrEqualTo(2);
        assertThat(result.getContent()).contains("**REDACTED**");
    }

    @Test
    void shouldCountZeroForCleanContent() {
        RepositoryContentSafetyService.RedactionResult result = service.redactSecretsWithCount("public class Hello {}");
        assertThat(result.getRedactionCount()).isZero();
        assertThat(result.getContent()).isEqualTo("public class Hello {}");
    }

    @Test
    void shouldNotRedactShortStrings() {
        // Strings shorter than 20 chars should not match secret patterns
        String input = "sk-test";
        String result = service.redactSecrets(input);
        assertThat(result).isEqualTo(input);
    }

    // ========================
    // File Size Limits
    // ========================

    @Test
    void shouldValidateFileSizeWithDefaultLimit() {
        assertThat(service.isFileSizeWithinLimit(1024)).isTrue();
        assertThat(service.isFileSizeWithinLimit(1024 * 1024)).isFalse();
    }

    @Test
    void shouldValidateOutputSizeWithDefaultLimit() {
        assertThat(service.isOutputSizeWithinLimit("hello")).isTrue();
        assertThat(service.isOutputSizeWithinLimit("x".repeat(300 * 1024))).isFalse();
    }

    @Test
    void shouldReturnTrueForNullOutputSizeCheck() {
        assertThat(service.isOutputSizeWithinLimit(null)).isTrue();
    }
}
