package com.aicoding.platform.task.dto;

public class TaskLogResponse {

    private String id;
    private String level;
    private String stage;
    private String message;
    private String createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
