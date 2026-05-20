package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class WorkflowTemplateResponse {

    private String id;
    private String templateKey;
    private String name;
    private String description;
    private String category;
    private String status;
    private Boolean builtIn;
    private String templateJson;
    private WorkflowStrategyResponse strategy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Phase/step counts derived from the parsed strategy
    private Integer phaseCount;
    private Integer stepCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }

    public String getTemplateJson() { return templateJson; }
    public void setTemplateJson(String templateJson) { this.templateJson = templateJson; }

    public WorkflowStrategyResponse getStrategy() { return strategy; }
    public void setStrategy(WorkflowStrategyResponse strategy) { this.strategy = strategy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getPhaseCount() { return phaseCount; }
    public void setPhaseCount(Integer phaseCount) { this.phaseCount = phaseCount; }

    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
}
