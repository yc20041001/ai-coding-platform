package com.aicoding.platform.task.dto;

public class TaskArtifactResponse {

    private String id;
    private String artifactType;
    private String name;
    private String content;
    private String fileUrl;
    private String createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
