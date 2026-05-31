package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_assistive_ordering_optimization")
public class GovernanceAssistiveOrderingOptimizationEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String actionType; private BigDecimal avgUsefulnessRating; private BigDecimal avgActionOrder;
    private Integer usefulnessCount; private Integer notUsefulCount;
    private String optimizationLevel; private Integer suggestedNewOrder; private String rationaleText;
    private LocalDateTime capturedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public BigDecimal getAvgUsefulnessRating() { return avgUsefulnessRating; } public void setAvgUsefulnessRating(BigDecimal v) { this.avgUsefulnessRating = v; }
    public BigDecimal getAvgActionOrder() { return avgActionOrder; } public void setAvgActionOrder(BigDecimal v) { this.avgActionOrder = v; }
    public Integer getUsefulnessCount() { return usefulnessCount; } public void setUsefulnessCount(Integer v) { this.usefulnessCount = v; }
    public Integer getNotUsefulCount() { return notUsefulCount; } public void setNotUsefulCount(Integer v) { this.notUsefulCount = v; }
    public String getOptimizationLevel() { return optimizationLevel; } public void setOptimizationLevel(String v) { this.optimizationLevel = v; }
    public Integer getSuggestedNewOrder() { return suggestedNewOrder; } public void setSuggestedNewOrder(Integer v) { this.suggestedNewOrder = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
    public LocalDateTime getCapturedAt() { return capturedAt; } public void setCapturedAt(LocalDateTime v) { this.capturedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
