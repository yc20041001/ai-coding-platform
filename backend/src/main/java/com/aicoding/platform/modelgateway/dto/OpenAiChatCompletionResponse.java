package com.aicoding.platform.modelgateway.dto;

import java.util.List;

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

    public static class Choice {
        private Integer index;
        private Message message;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }

        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
    }

    public static class Message {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Usage {
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;

        public Long getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }

        public Long getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }

        public Long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    }
}
