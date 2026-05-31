package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_package_composition_tuning")
public class GovernancePackageCompositionTuningEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String scoreRange; private BigDecimal avgCompleteness; private BigDecimal avgAccuracy;
    private BigDecimal avgOverall; private Integer sampleCount;
    private String tuningLevel; private String suggestionText; private LocalDateTime capturedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getScoreRange() { return scoreRange; } public void setScoreRange(String v) { this.scoreRange = v; }
    public BigDecimal getAvgCompleteness() { return avgCompleteness; } public void setAvgCompleteness(BigDecimal v) { this.avgCompleteness = v; }
    public BigDecimal getAvgAccuracy() { return avgAccuracy; } public void setAvgAccuracy(BigDecimal v) { this.avgAccuracy = v; }
    public BigDecimal getAvgOverall() { return avgOverall; } public void setAvgOverall(BigDecimal v) { this.avgOverall = v; }
    public Integer getSampleCount() { return sampleCount; } public void setSampleCount(Integer v) { this.sampleCount = v; }
    public String getTuningLevel() { return tuningLevel; } public void setTuningLevel(String v) { this.tuningLevel = v; }
    public String getSuggestionText() { return suggestionText; } public void setSuggestionText(String v) { this.suggestionText = v; }
    public LocalDateTime getCapturedAt() { return capturedAt; } public void setCapturedAt(LocalDateTime v) { this.capturedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
