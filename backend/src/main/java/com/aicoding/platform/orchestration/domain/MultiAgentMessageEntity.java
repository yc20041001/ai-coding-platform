package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("multi_agent_message")
public class MultiAgentMessageEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long runId;
    private Long projectId;
    private Long taskId;
    private Long fromStepId;
    private Long toStepId;
    private Long fromAgentId;
    private Long toAgentId;
    private String messageType;
    private String content;
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getFromStepId() { return fromStepId; }
    public void setFromStepId(Long fromStepId) { this.fromStepId = fromStepId; }

    public Long getToStepId() { return toStepId; }
    public void setToStepId(Long toStepId) { this.toStepId = toStepId; }

    public Long getFromAgentId() { return fromAgentId; }
    public void setFromAgentId(Long fromAgentId) { this.fromAgentId = fromAgentId; }

    public Long getToAgentId() { return toAgentId; }
    public void setToAgentId(Long toAgentId) { this.toAgentId = toAgentId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
