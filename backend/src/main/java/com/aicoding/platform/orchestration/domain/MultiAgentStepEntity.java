package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("multi_agent_step")
public class MultiAgentStepEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long runId;
    private Long phaseId;
    private Integer phaseOrder;
    private String laneKey;
    private Long projectId;
    private Long taskId;
    private Long agentId;
    private Long agentExecutionId;
    private Integer stepOrder;
    private String stepType;
    private String status;
    private String inputContext;
    private String outputContent;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getPhaseId() { return phaseId; }
    public void setPhaseId(Long phaseId) { this.phaseId = phaseId; }

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public String getLaneKey() { return laneKey; }
    public void setLaneKey(String laneKey) { this.laneKey = laneKey; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }

    public Long getAgentExecutionId() { return agentExecutionId; }
    public void setAgentExecutionId(Long agentExecutionId) { this.agentExecutionId = agentExecutionId; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputContext() { return inputContext; }
    public void setInputContext(String inputContext) { this.inputContext = inputContext; }

    public String getOutputContent() { return outputContent; }
    public void setOutputContent(String outputContent) { this.outputContent = outputContent; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
