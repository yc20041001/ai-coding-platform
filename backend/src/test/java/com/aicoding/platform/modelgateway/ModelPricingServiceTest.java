package com.aicoding.platform.modelgateway;

import com.aicoding.platform.modelgateway.application.ModelPricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPricingServiceTest {

    private final ModelPricingService service = new ModelPricingService();

    @Test
    void shouldReturnZeroForMockModel() {
        BigDecimal cost = service.estimateCost("mock-agent-model", 1000L, 1000L);
        assertEquals(0, cost.compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldEstimateCostForGpt4Mini() {
        // gpt-4.1-mini: input $0.15/M, output $0.60/M => per 1K tokens: $0.00015 / $0.00060
        BigDecimal cost = service.estimateCost("gpt-4.1-mini", 1000L, 1000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldEstimateCostForDeepSeek() {
        BigDecimal cost = service.estimateCost("deepseek-chat", 1000L, 1000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldEstimateCostForUnknownModel() {
        BigDecimal cost = service.estimateCost("unknown-model-xyz", 1000L, 1000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldHandleNullTokens() {
        BigDecimal cost = service.estimateCost("gpt-4.1-mini", null, null);
        assertEquals(0, cost.compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldEstimateCostForClaudeSonnet() {
        BigDecimal cost = service.estimateCost("claude-3-5-sonnet-latest", 1000L, 1000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldEstimateCostForClaudeOpus() {
        BigDecimal cost = service.estimateCost("claude-3-opus-latest", 100L, 0L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldEstimateCostForGemini() {
        BigDecimal cost = service.estimateCost("gemini-2.5-flash", 5000L, 2000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldUseCustomOverridePrice() {
        BigDecimal customInput = new BigDecimal("0.05000");
        BigDecimal customOutput = new BigDecimal("0.20000");
        BigDecimal cost = service.estimateCost("deepseek-chat", 1000L, 1000L, customInput, customOutput);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
        // 1K * 0.05 + 1K * 0.20 = 0.25
        assertEquals(0, cost.compareTo(new BigDecimal("0.25000000")));
    }

    @Test
    void shouldHandleOverrideWithNullDefaults() {
        BigDecimal cost = service.estimateCost("unknown-model", 2000L, 500L, null, null);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldReturnNonNegativeForAllKnownModels() {
        String[] models = {"gpt-4.1-mini", "gpt-4.1", "gpt-4o", "gpt-4o-mini",
                "claude-3-5-sonnet-latest", "claude-3-opus-latest", "claude-3-haiku-latest",
                "deepseek-chat", "deepseek-reasoner", "qwen-plus", "qwen-max",
                "gemini-2.5-flash", "gemini-2.5-pro", "mock-agent-model"};
        for (String model : models) {
            BigDecimal cost = service.estimateCost(model, 100L, 100L);
            assertTrue(cost.compareTo(BigDecimal.ZERO) >= 0,
                    "Cost should be non-negative for model: " + model + " but was: " + cost);
        }
    }

    @Test
    void shouldEstimateCostForQwen() {
        BigDecimal cost = service.estimateCost("qwen-max", 1000L, 1000L);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }
}
