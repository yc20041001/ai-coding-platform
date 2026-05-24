package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_incident_knowledge_link")
public class ToolIncidentKnowledgeLinkEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long incidentId;
    private Long rootCauseNoteId;
    private Long knowledgeBaseId;
    private Long knowledgeDocumentId;
    private String linkType;
    private String title;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public ToolIncidentKnowledgeLinkEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public Long getRootCauseNoteId() { return rootCauseNoteId; }
    public void setRootCauseNoteId(Long rootCauseNoteId) { this.rootCauseNoteId = rootCauseNoteId; }

    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public Long getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }

    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
