package com.aicoding.platform.modelgateway;

import com.aicoding.platform.modelgateway.application.ModelSecretMaskingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelSecretMaskingServiceTest {

    private final ModelSecretMaskingService service = new ModelSecretMaskingService();

    @Test
    void shouldReturnEmptyForNull() {
        assertEquals("<empty>", service.mask(null));
    }

    @Test
    void shouldReturnEmptyForBlank() {
        assertEquals("<empty>", service.mask(""));
    }

    @Test
    void shouldMaskShortKey() {
        assertEquals("****", service.mask("abc123"));
    }

    @Test
    void shouldMask8CharKey() {
        assertEquals("****", service.mask("12345678"));
    }

    @Test
    void shouldMaskLongKey() {
        String masked = service.mask("sk-1234567890abcdefghij");
        assertEquals("sk-****ghij", masked);
    }

    @Test
    void shouldSanitizeBearerToken() {
        String input = "Authorization: Bearer sk-1234567890abcdefghij";
        String sanitized = service.sanitizeForLog(input);
        assertEquals("Authorization: Bearer ****", sanitized);
    }

    @Test
    void shouldSanitizeApiKeyInUrl() {
        String input = "api-key=sk-1234567890abcdefghij&other=value";
        String sanitized = service.sanitizeForLog(input);
        assertEquals("api-key=****&other=value", sanitized);
    }

    @Test
    void shouldSanitizeApiKeyWithColon() {
        String input = "api_key: sk-1234567890abcdefghij";
        String sanitized = service.sanitizeForLog(input);
        assertEquals("api_key: sk-****", sanitized);
    }

    @Test
    void shouldSanitizeMultipleSecrets() {
        String input = "Authorization: Bearer abcdefghijklmnopqrstuvwxyz, api-key=sk-1234567890abcdefghij";
        String sanitized = service.sanitizeForLog(input);
        assertEquals("Authorization: Bearer ****, api-key=****", sanitized);
    }

    @Test
    void shouldPreserveNonSecretContent() {
        String input = "This is a normal log message without secrets";
        String sanitized = service.sanitizeForLog(input);
        assertEquals(input, sanitized);
    }

    @Test
    void shouldMaskWithLabel() {
        assertEquals("openai:sk-****ghij", service.maskWithLabel("openai", "sk-1234567890abcdefghij"));
    }

    @Test
    void shouldMaskWithLabelForEmptyKey() {
        assertEquals("deepseek:<not-set>", service.maskWithLabel("deepseek", null));
        assertEquals("mock:<not-set>", service.maskWithLabel("mock", ""));
    }

    @Test
    void shouldMaskShortKeyWithLabel() {
        assertEquals("custom:****", service.maskWithLabel("custom", "1234"));
    }

    @Test
    void shouldHandleNullSanitizeForLog() {
        assertEquals(null, service.sanitizeForLog(null));
    }
}
