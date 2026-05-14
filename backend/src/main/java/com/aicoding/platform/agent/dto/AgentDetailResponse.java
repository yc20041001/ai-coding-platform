package com.aicoding.platform.agent.dto;

public class AgentDetailResponse {

    private String id;
    private String name;
    private String code;
    private String type;
    private String description;
    private String status;
    private String avatar;
    private AgentVersionInfo latestVersion;
    private String modelConfigId;
    private String toolPolicy;
    private String executionPolicy;

    public static class AgentVersionInfo {
        private String id;
        private String versionNo;
        private String modelConfigId;
        private String status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getVersionNo() { return versionNo; }
        public void setVersionNo(String versionNo) { this.versionNo = versionNo; }

        public String getModelConfigId() { return modelConfigId; }
        public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public AgentVersionInfo getLatestVersion() { return latestVersion; }
    public void setLatestVersion(AgentVersionInfo latestVersion) { this.latestVersion = latestVersion; }

    public String getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

    public String getToolPolicy() { return toolPolicy; }
    public void setToolPolicy(String toolPolicy) { this.toolPolicy = toolPolicy; }

    public String getExecutionPolicy() { return executionPolicy; }
    public void setExecutionPolicy(String executionPolicy) { this.executionPolicy = executionPolicy; }
}
