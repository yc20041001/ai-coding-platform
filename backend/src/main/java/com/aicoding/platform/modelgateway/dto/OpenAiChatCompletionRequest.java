package com.aicoding.platform.modelgateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class OpenAiChatCompletionRequest {

    private String model;
    private List<OpenAiChatMessage> messages;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Boolean stream;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<OpenAiChatMessage> getMessages() { return messages; }
    public void setMessages(List<OpenAiChatMessage> messages) { this.messages = messages; }

    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }

    @JsonProperty("max_tokens")
    public Integer getMaxTokens() { return maxTokens; }

    @JsonProperty("max_tokens")
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
}
