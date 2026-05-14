package com.aicoding.platform.observability.dto;

public class SystemOverviewResponse {

    private Long projectCount;
    private Long userCount;
    private Long taskCount;
    private Long runningTaskCount;
    private Long completedTaskCount;
    private Long agentCount;
    private Long knowledgeBaseCount;
    private Long documentCount;
    private Long chatMessageCount;
    private Long modelRequestCount;
    private Long todayModelRequestCount;
    private Long todayTokenUsage;

    public Long getProjectCount() { return projectCount; }
    public void setProjectCount(Long projectCount) { this.projectCount = projectCount; }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }

    public Long getTaskCount() { return taskCount; }
    public void setTaskCount(Long taskCount) { this.taskCount = taskCount; }

    public Long getRunningTaskCount() { return runningTaskCount; }
    public void setRunningTaskCount(Long runningTaskCount) { this.runningTaskCount = runningTaskCount; }

    public Long getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(Long completedTaskCount) { this.completedTaskCount = completedTaskCount; }

    public Long getAgentCount() { return agentCount; }
    public void setAgentCount(Long agentCount) { this.agentCount = agentCount; }

    public Long getKnowledgeBaseCount() { return knowledgeBaseCount; }
    public void setKnowledgeBaseCount(Long knowledgeBaseCount) { this.knowledgeBaseCount = knowledgeBaseCount; }

    public Long getDocumentCount() { return documentCount; }
    public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }

    public Long getChatMessageCount() { return chatMessageCount; }
    public void setChatMessageCount(Long chatMessageCount) { this.chatMessageCount = chatMessageCount; }

    public Long getModelRequestCount() { return modelRequestCount; }
    public void setModelRequestCount(Long modelRequestCount) { this.modelRequestCount = modelRequestCount; }

    public Long getTodayModelRequestCount() { return todayModelRequestCount; }
    public void setTodayModelRequestCount(Long todayModelRequestCount) { this.todayModelRequestCount = todayModelRequestCount; }

    public Long getTodayTokenUsage() { return todayTokenUsage; }
    public void setTodayTokenUsage(Long todayTokenUsage) { this.todayTokenUsage = todayTokenUsage; }
}
