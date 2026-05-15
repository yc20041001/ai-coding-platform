package com.aicoding.platform.modelgateway.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Estimates cost based on token usage and configured pricing.
 * Uses hardcoded defaults for known models, overridable via DB config.
 */
@Service
public class ModelPricingService {

    // Approximate pricing in USD per 1K tokens (as of mid-2026)
    private static final Map<String, BigDecimal[]> DEFAULT_PRICING = Map.ofEntries(
            // [inputPrice, outputPrice] per 1K tokens
            Map.entry("gpt-4.1-mini", new BigDecimal[]{new BigDecimal("0.00015"), new BigDecimal("0.00060")}),
            Map.entry("gpt-4.1", new BigDecimal[]{new BigDecimal("0.00250"), new BigDecimal("0.01000")}),
            Map.entry("gpt-4o", new BigDecimal[]{new BigDecimal("0.00250"), new BigDecimal("0.01000")}),
            Map.entry("gpt-4o-mini", new BigDecimal[]{new BigDecimal("0.00015"), new BigDecimal("0.00060")}),
            Map.entry("claude-3-5-sonnet-latest", new BigDecimal[]{new BigDecimal("0.00300"), new BigDecimal("0.01500")}),
            Map.entry("claude-3-opus-latest", new BigDecimal[]{new BigDecimal("0.01500"), new BigDecimal("0.07500")}),
            Map.entry("claude-3-haiku-latest", new BigDecimal[]{new BigDecimal("0.00025"), new BigDecimal("0.00125")}),
            Map.entry("deepseek-chat", new BigDecimal[]{new BigDecimal("0.00014"), new BigDecimal("0.00028")}),
            Map.entry("deepseek-reasoner", new BigDecimal[]{new BigDecimal("0.00055"), new BigDecimal("0.00219")}),
            Map.entry("qwen-plus", new BigDecimal[]{new BigDecimal("0.00028"), new BigDecimal("0.00085")}),
            Map.entry("qwen-max", new BigDecimal[]{new BigDecimal("0.00110"), new BigDecimal("0.00340")}),
            Map.entry("gemini-2.5-flash", new BigDecimal[]{new BigDecimal("0.00015"), new BigDecimal("0.00060")}),
            Map.entry("gemini-2.5-pro", new BigDecimal[]{new BigDecimal("0.00125"), new BigDecimal("0.00500")}),
            Map.entry("mock-agent-model", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})
    );

    /**
     * Estimate cost from token counts.
     */
    public BigDecimal estimateCost(String modelName, Long promptTokens, Long completionTokens,
                                   BigDecimal inputPricePer1k, BigDecimal outputPricePer1k) {
        long prompt = promptTokens != null ? promptTokens : 0L;
        long completion = completionTokens != null ? completionTokens : 0L;

        BigDecimal inputPrice = resolveInputPrice(modelName, inputPricePer1k);
        BigDecimal outputPrice = resolveOutputPrice(modelName, outputPricePer1k);

        BigDecimal inputCost = BigDecimal.valueOf(prompt)
                .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP)
                .multiply(inputPrice);

        BigDecimal outputCost = BigDecimal.valueOf(completion)
                .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP)
                .multiply(outputPrice);

        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
    }

    public BigDecimal estimateCost(String modelName, Long promptTokens, Long completionTokens) {
        return estimateCost(modelName, promptTokens, completionTokens, null, null);
    }

    private BigDecimal resolveInputPrice(String modelName, BigDecimal override) {
        if (override != null) return override;
        BigDecimal[] prices = DEFAULT_PRICING.get(modelName);
        if (prices != null) return prices[0];
        return new BigDecimal("0.00100"); // default fallback
    }

    private BigDecimal resolveOutputPrice(String modelName, BigDecimal override) {
        if (override != null) return override;
        BigDecimal[] prices = DEFAULT_PRICING.get(modelName);
        if (prices != null) return prices[1];
        return new BigDecimal("0.00400"); // default fallback
    }
}
