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
}
