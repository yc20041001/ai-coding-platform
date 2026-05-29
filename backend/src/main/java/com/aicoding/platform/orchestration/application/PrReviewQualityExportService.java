package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.PrReviewQualityRecordEntity;
import com.aicoding.platform.orchestration.dto.ExportPrReviewQualityReportResponse;
import com.aicoding.platform.orchestration.infrastructure.PrReviewQualityRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrReviewQualityExportService {

    private final PrReviewQualityRecordMapper prReviewQualityRecordMapper;
    private final ProjectPermissionService projectPermissionService;

    public PrReviewQualityExportService(PrReviewQualityRecordMapper prReviewQualityRecordMapper,
                                        ProjectPermissionService projectPermissionService) {
        this.prReviewQualityRecordMapper = prReviewQualityRecordMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public ExportPrReviewQualityReportResponse exportReport(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        List<PrReviewQualityRecordEntity> records = prReviewQualityRecordMapper.selectList(
                new LambdaQueryWrapper<PrReviewQualityRecordEntity>()
                        .eq(PrReviewQualityRecordEntity::getProjectId, projectId)
                        .orderByDesc(PrReviewQualityRecordEntity::getCreateTime));

        long highValue = records.stream().filter(r -> "HIGH_VALUE".equals(r.getReviewStatus())).count();
        long actionable = records.stream().filter(r -> "ACTIONABLE".equals(r.getReviewStatus())).count();
        long lowSignal = records.stream().filter(r -> "LOW_SIGNAL".equals(r.getReviewStatus())).count();
        long adopted = records.stream().filter(r -> "ADOPTED".equals(r.getAdoptionStatus())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("# PR 评审质量报告\n\n");
        sb.append("**生成时间**: ").append(java.time.LocalDateTime.now()).append("\n\n");
        sb.append("## 总览\n\n");
        sb.append("- **总评审数**: ").append(records.size()).append("\n");
        sb.append("- **高价值评审**: ").append(highValue).append("\n");
        sb.append("- **可操作评审**: ").append(actionable).append("\n");
        sb.append("- **低信号评审**: ").append(lowSignal).append("\n");
        sb.append("- **已采纳**: ").append(adopted).append("\n\n");
        sb.append("## 详细记录\n\n");
        sb.append("| 仓库 | PR # | 状态 | 人工反馈 | 采纳状态 | 有用性 | 假阳性 |\n");
        sb.append("|------|------|------|----------|----------|--------|--------|\n");

        for (PrReviewQualityRecordEntity r : records) {
            sb.append("| ").append(r.getRepositoryFullName())
                    .append(" | ").append(r.getPullRequestNumber())
                    .append(" | ").append(r.getReviewStatus())
                    .append(" | ").append(r.getHumanFeedbackStatus())
                    .append(" | ").append(r.getAdoptionStatus())
                    .append(" | ").append(r.getUsefulnessScore() != null ? r.getUsefulnessScore() : "-")
                    .append(" | ").append(r.getFalsePositiveScore() != null ? r.getFalsePositiveScore() : "-")
                    .append(" |\n");
        }

        ExportPrReviewQualityReportResponse resp = new ExportPrReviewQualityReportResponse();
        resp.setContent(sb.toString());
        resp.setFileName("pr-review-quality-report.md");
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
