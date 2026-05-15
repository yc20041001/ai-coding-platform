package com.aicoding.platform.modelgateway;

import com.aicoding.platform.modelgateway.application.MockModelProvider;
import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ModelGatewayIntegrationTest {

    @Autowired
    private MockModelProvider mockModelProvider;

    @Autowired
    private ModelGatewayProperties properties;

    @Test
    void shouldUseMockProviderByDefault() {
        assertEquals("MOCK", properties.getDefaultProvider());
        assertTrue(properties.isFallbackEnabled());
    }

    @Test
    void mockProviderShouldGenerateNonStreamResponse() {
        ModelRequest request = new ModelRequest();
        request.setRequestType("CHAT");
        request.setUserPrompt("Hello");

        ModelResponse response = mockModelProvider.generate(request);

        assertTrue(response.getSuccess());
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isEmpty());
        assertEquals("MOCK", response.getProvider());
        assertEquals("mock-agent-model", response.getModelName());
        assertTrue(response.getTotalTokens() > 0);
        assertTrue(response.getLatencyMs() > 0);
    }

    @Test
    void mockProviderShouldSupportStream() {
        assertTrue(mockModelProvider.supportsStream());
    }

    @Test
    void mockProviderShouldSupportAllProviders() {
        assertTrue(mockModelProvider.supports("MOCK"));
        assertTrue(mockModelProvider.supports(null));
        assertTrue(mockModelProvider.supports(""));
    }

    @Test
    void propertiesShouldHaveFallbackEnabled() {
        assertTrue(properties.isFallbackEnabled());
    }

    @Test
    void propertiesShouldHavePromptSafetyEnabled() {
        assertTrue(properties.isPromptSafetyEnabled());
    }

    @Test
    void propertiesShouldHaveTimeoutConfigured() {
        assertTrue(properties.getTimeoutMs() > 0);
    }
}
