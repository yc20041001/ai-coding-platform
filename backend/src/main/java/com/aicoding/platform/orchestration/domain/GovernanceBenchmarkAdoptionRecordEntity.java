package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_benchmark_adoption_record")
public class GovernanceBenchmarkAdoptionRecordEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long projectId; private String projectName; private String metricKey;
    private String adoptionStatus; private BigDecimal currentScore; private BigDecimal targetScore;
    private String blockerType; private String blockerNote; private Long ownerId; private String ownerName;
    private LocalDateTime adoptedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public String getAdoptionStatus() { return adoptionStatus; } public void setAdoptionStatus(String v) { this.adoptionStatus = v; }
    public BigDecimal getCurrentScore() { return currentScore; } public void setCurrentScore(BigDecimal v) { this.currentScore = v; }
    public BigDecimal getTargetScore() { return targetScore; } public void setTargetScore(BigDecimal v) { this.targetScore = v; }
    public String getBlockerType() { return blockerType; } public void setBlockerType(String v) { this.blockerType = v; }
    public String getBlockerNote() { return blockerNote; } public void setBlockerNote(String v) { this.blockerNote = v; }
    public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getAdoptedAt() { return adoptedAt; } public void setAdoptedAt(LocalDateTime v) { this.adoptedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
