package com.aicoding.platform.modelgateway.domain;

import java.math.BigDecimal;

/**
 * Pricing rule for a model. Used for cost estimation.
 */
public class ModelPricingRule {

    private String provider;
    private String modelName;
    private BigDecimal inputPricePer1k;
    private BigDecimal outputPricePer1k;
    private String currency;

    public ModelPricingRule() {}

    public ModelPricingRule(String provider, String modelName,
                            BigDecimal inputPricePer1k, BigDecimal outputPricePer1k) {
        this.provider = provider;
        this.modelName = modelName;
        this.inputPricePer1k = inputPricePer1k;
        this.outputPricePer1k = outputPricePer1k;
        this.currency = "USD";
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public BigDecimal getInputPricePer1k() { return inputPricePer1k; }
    public void setInputPricePer1k(BigDecimal inputPricePer1k) { this.inputPricePer1k = inputPricePer1k; }

    public BigDecimal getOutputPricePer1k() { return outputPricePer1k; }
    public void setOutputPricePer1k(BigDecimal outputPricePer1k) { this.outputPricePer1k = outputPricePer1k; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
