package com.aicoding.platform.project.dto;

import java.util.Collections;
import java.util.List;

public class ProjectOverviewResponse {

    private int memberCount;
    private int taskCount;
    private int runningTaskCount;
    private int completedTaskCount;
    private int agentCount;
    private int documentCount;
    private long tokenUsage;
    private List<RecentActivity> recentActivities;

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }

    public int getRunningTaskCount() { return runningTaskCount; }
    public void setRunningTaskCount(int runningTaskCount) { this.runningTaskCount = runningTaskCount; }

    public int getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(int completedTaskCount) { this.completedTaskCount = completedTaskCount; }

    public int getAgentCount() { return agentCount; }
    public void setAgentCount(int agentCount) { this.agentCount = agentCount; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

    public long getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(long tokenUsage) { this.tokenUsage = tokenUsage; }

    public List<RecentActivity> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<RecentActivity> recentActivities) { this.recentActivities = recentActivities; }

    public static ProjectOverviewResponse empty() {
        ProjectOverviewResponse response = new ProjectOverviewResponse();
        response.setRecentActivities(Collections.emptyList());
        return response;
    }

    public static class RecentActivity {
        private String type;
        private String title;
        private String time;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }
}
