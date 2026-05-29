package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ModelCostSummaryEntity;
import com.aicoding.platform.orchestration.dto.ExportModelCostReportResponse;
import com.aicoding.platform.orchestration.infrastructure.ModelCostSummaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ModelCostReportExportService {

    private final ModelCostSummaryMapper modelCostSummaryMapper;
    private final ProjectPermissionService projectPermissionService;

    public ModelCostReportExportService(ModelCostSummaryMapper modelCostSummaryMapper,
                                        ProjectPermissionService projectPermissionService) {
        this.modelCostSummaryMapper = modelCostSummaryMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public ExportModelCostReportResponse exportReport(String projectIdStr, LocalDate startDate, LocalDate endDate) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<ModelCostSummaryEntity> records = modelCostSummaryMapper.selectList(
                new LambdaQueryWrapper<ModelCostSummaryEntity>()
                        .eq(ModelCostSummaryEntity::getProjectId, projectId)
                        .ge(ModelCostSummaryEntity::getStatDate, start)
                        .le(ModelCostSummaryEntity::getStatDate, end)
                        .orderByAsc(ModelCostSummaryEntity::getStatDate));

        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0;
        long totalRequests = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("# 模型成本报告\n\n");
        sb.append("**统计周期**: ").append(start).append(" ~ ").append(end).append("\n\n");
        sb.append("| 日期 | 供应商 | 模型 | 请求类型 | 请求数 | 成功 | 失败 | 回退 | 预估成本 |\n");
        sb.append("|------|--------|------|----------|--------|------|------|------|----------|\n");

        for (ModelCostSummaryEntity r : records) {
            sb.append("| ").append(r.getStatDate())
                    .append(" | ").append(r.getProvider())
                    .append(" | ").append(r.getModelName())
                    .append(" | ").append(r.getRequestType())
                    .append(" | ").append(r.getRequestCount())
                    .append(" | ").append(r.getSuccessCount())
                    .append(" | ").append(r.getFailureCount())
                    .append(" | ").append(r.getFallbackCount())
                    .append(" | $").append(r.getEstimatedCost())
                    .append(" |\n");
            totalCost = totalCost.add(r.getEstimatedCost() != null ? r.getEstimatedCost() : BigDecimal.ZERO);
            totalTokens += r.getTotalTokens() != null ? r.getTotalTokens() : 0;
            totalRequests += r.getRequestCount() != null ? r.getRequestCount() : 0;
        }

        sb.append("\n## 汇总\n\n");
        sb.append("- **总请求数**: ").append(totalRequests).append("\n");
        sb.append("- **总 Token 数**: ").append(totalTokens).append("\n");
        sb.append("- **总预估成本**: $").append(totalCost).append("\n");

        ExportModelCostReportResponse resp = new ExportModelCostReportResponse();
        resp.setContent(sb.toString());
        resp.setFileName("model-cost-report-" + start + "-to-" + end + ".md");
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
