package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_knowledge_quality_review")
public class ToolKnowledgeQualityReviewEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long incidentId;
    private Long knowledgeDocumentId;
    private Long retrospectiveId;
    private Integer completenessScore;
    private Integer accuracyScore;
    private Integer actionabilityScore;
    private Integer relevanceScore;
    private String reviewStatus;
    private String overallStatus;
    private String checklistJson;
    private String reviewComment;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ToolKnowledgeQualityReviewEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public Long getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }

    public Long getRetrospectiveId() { return retrospectiveId; }
    public void setRetrospectiveId(Long retrospectiveId) { this.retrospectiveId = retrospectiveId; }

    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }

    public Integer getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Integer accuracyScore) { this.accuracyScore = accuracyScore; }

    public Integer getActionabilityScore() { return actionabilityScore; }
    public void setActionabilityScore(Integer actionabilityScore) { this.actionabilityScore = actionabilityScore; }

    public Integer getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Integer relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
