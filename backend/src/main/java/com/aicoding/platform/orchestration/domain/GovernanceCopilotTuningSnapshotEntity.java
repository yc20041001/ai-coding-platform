package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_copilot_tuning_snapshot")
public class GovernanceCopilotTuningSnapshotEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String snapshotWindow; private Integer totalFeedbackCount;
    private BigDecimal acceptanceRate; private BigDecimal dismissalRate;
    private BigDecimal avgFeedbackRating; private String topSuggestionType;
    private String weakestSuggestionType; private String topFocusMode; private String weakestFocusMode;
    private BigDecimal tuningConfidenceScore; private String summaryMarkdown;
    private LocalDateTime capturedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getSnapshotWindow() { return snapshotWindow; } public void setSnapshotWindow(String v) { this.snapshotWindow = v; }
    public Integer getTotalFeedbackCount() { return totalFeedbackCount; } public void setTotalFeedbackCount(Integer v) { this.totalFeedbackCount = v; }
    public BigDecimal getAcceptanceRate() { return acceptanceRate; } public void setAcceptanceRate(BigDecimal v) { this.acceptanceRate = v; }
    public BigDecimal getDismissalRate() { return dismissalRate; } public void setDismissalRate(BigDecimal v) { this.dismissalRate = v; }
    public BigDecimal getAvgFeedbackRating() { return avgFeedbackRating; } public void setAvgFeedbackRating(BigDecimal v) { this.avgFeedbackRating = v; }
    public String getTopSuggestionType() { return topSuggestionType; } public void setTopSuggestionType(String v) { this.topSuggestionType = v; }
    public String getWeakestSuggestionType() { return weakestSuggestionType; } public void setWeakestSuggestionType(String v) { this.weakestSuggestionType = v; }
    public String getTopFocusMode() { return topFocusMode; } public void setTopFocusMode(String v) { this.topFocusMode = v; }
    public String getWeakestFocusMode() { return weakestFocusMode; } public void setWeakestFocusMode(String v) { this.weakestFocusMode = v; }
    public BigDecimal getTuningConfidenceScore() { return tuningConfidenceScore; } public void setTuningConfidenceScore(BigDecimal v) { this.tuningConfidenceScore = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
    public LocalDateTime getCapturedAt() { return capturedAt; } public void setCapturedAt(LocalDateTime v) { this.capturedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
