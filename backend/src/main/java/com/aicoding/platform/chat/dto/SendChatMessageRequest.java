package com.aicoding.platform.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class SendChatMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private List<String> agentIds;
    private Context context;
    private Boolean stream;
    private Boolean useRag;
    private String knowledgeBaseId;
    private Integer ragLimit;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getAgentIds() { return agentIds; }
    public void setAgentIds(List<String> agentIds) { this.agentIds = agentIds; }

    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }

    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }

    public Boolean getUseRag() { return useRag; }
    public void setUseRag(Boolean useRag) { this.useRag = useRag; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public Integer getRagLimit() { return ragLimit; }
    public void setRagLimit(Integer ragLimit) { this.ragLimit = ragLimit; }

    public static class Context {
        private List<String> filePaths;
        private String taskId;

        public List<String> getFilePaths() { return filePaths; }
        public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }
}
