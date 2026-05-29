package com.aicoding.platform.modelgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatCompletionResponse {

    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Message message;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }

        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
        private String reasoningContent;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getReasoningContent() { return reasoningContent; }
        @JsonProperty("reasoning_content")
        public void setReasoningContent(String reasoningContent) { this.reasoningContent = reasoningContent; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;

        public Long getPromptTokens() { return promptTokens; }
        @JsonProperty("prompt_tokens")
        public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }

        public Long getCompletionTokens() { return completionTokens; }
        @JsonProperty("completion_tokens")
        public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }

        public Long getTotalTokens() { return totalTokens; }
        @JsonProperty("total_tokens")
        public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    }
}
