package com.aicoding.platform.observability.application;

import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.observability.dto.ToolExecutionDailyMetricResponse;
import com.aicoding.platform.observability.dto.ToolExecutionFailureMetricResponse;
import com.aicoding.platform.observability.dto.ToolExecutionMetricsResponse;
import com.aicoding.platform.observability.dto.ToolExecutionSummaryResponse;
import com.aicoding.platform.observability.dto.ToolExecutionToolMetricResponse;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolExecutionMetricsApplicationService {

    private final ToolExecutionJobMapper toolExecutionJobMapper;
    private final ProjectPermissionService projectPermissionService;

    public ToolExecutionMetricsApplicationService(ToolExecutionJobMapper toolExecutionJobMapper,
                                                   ProjectPermissionService projectPermissionService) {
        this.toolExecutionJobMapper = toolExecutionJobMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public ToolExecutionMetricsResponse getGlobalMetrics() {
        return buildMetrics(null);
    }

    @Transactional(readOnly = true)
    public ToolExecutionMetricsResponse getProjectMetrics(Long projectId) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.OWNER, ProjectRole.MAINTAINER);
        return buildMetrics(projectId);
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionJobResponse> listProblemJobs(Long projectId, String status, Integer limit) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (limit == null || limit < 1) limit = 50;
        if (limit > 200) limit = 200;

        LambdaQueryWrapper<ToolExecutionJobEntity> wrapper = new LambdaQueryWrapper<ToolExecutionJobEntity>()
                .eq(ToolExecutionJobEntity::getProjectId, projectId)
                .orderByDesc(ToolExecutionJobEntity::getCreateTime)
                .last("LIMIT " + limit);

        if (status != null && !status.isBlank()) {
            wrapper.eq(ToolExecutionJobEntity::getStatus, status);
        } else {
            wrapper.in(ToolExecutionJobEntity::getStatus,
                    ToolExecutionJobStatus.FAILED.name(),
                    ToolExecutionJobStatus.RETRY_PENDING.name(),
                    ToolExecutionJobStatus.DEAD_LETTERED.name());
        }

        return toolExecutionJobMapper.selectList(wrapper).stream()
                .map(this::toSafeJobResponse)
                .collect(Collectors.toList());
    }

    private ToolExecutionMetricsResponse buildMetrics(Long projectId) {
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();

        LambdaQueryWrapper<ToolExecutionJobEntity> baseQuery = new LambdaQueryWrapper<ToolExecutionJobEntity>()
                .ge(ToolExecutionJobEntity::getCreateTime, thirtyDaysAgo);
        if (projectId != null) {
            baseQuery.eq(ToolExecutionJobEntity::getProjectId, projectId);
        }

        List<ToolExecutionJobEntity> recentJobs = toolExecutionJobMapper.selectList(baseQuery);

        ToolExecutionMetricsResponse resp = new ToolExecutionMetricsResponse();
        resp.setSummary(buildSummary(recentJobs));
        resp.setTools(buildToolMetrics(recentJobs));
        resp.setDaily(buildDailyMetrics(recentJobs));
        resp.setErrorCodes(buildFailureMetrics(recentJobs, true));
        resp.setFailureStages(buildFailureMetrics(recentJobs, false));
        return resp;
    }

    private ToolExecutionSummaryResponse buildSummary(List<ToolExecutionJobEntity> jobs) {
        ToolExecutionSummaryResponse s = new ToolExecutionSummaryResponse();
        if (jobs.isEmpty()) {
            s.setTotalJobs(0L); s.setPendingJobs(0L); s.setRunningJobs(0L);
            s.setCompletedJobs(0L); s.setFailedJobs(0L); s.setRetryPendingJobs(0L);
            s.setCanceledJobs(0L); s.setDeadLetteredJobs(0L);
            s.setSuccessRate(0.0); s.setFailureRate(0.0); s.setRetryRate(0.0);
            s.setAvgDurationMs(0.0); s.setMaxDurationMs(0L); s.setTotalRetries(0L);
            return s;
        }

        long total = jobs.size();
        long pending = countByStatus(jobs, ToolExecutionJobStatus.PENDING);
        long running = countByStatus(jobs, ToolExecutionJobStatus.RUNNING);
        long completed = countByStatus(jobs, ToolExecutionJobStatus.COMPLETED);
        long failed = countByStatus(jobs, ToolExecutionJobStatus.FAILED);
        long retryPending = countByStatus(jobs, ToolExecutionJobStatus.RETRY_PENDING);
        long canceled = countByStatus(jobs, ToolExecutionJobStatus.CANCELED);
        long deadLettered = countByStatus(jobs, ToolExecutionJobStatus.DEAD_LETTERED);

        s.setTotalJobs(total);
        s.setPendingJobs(pending);
        s.setRunningJobs(running);
        s.setCompletedJobs(completed);
        s.setFailedJobs(failed);
        s.setRetryPendingJobs(retryPending);
        s.setCanceledJobs(canceled);
        s.setDeadLetteredJobs(deadLettered);

        s.setSuccessRate(total > 0 ? (double) completed / total : 0.0);
        s.setFailureRate(total > 0 ? (double) (failed + deadLettered) / total : 0.0);

        long retryCountSum = jobs.stream()
                .filter(j -> j.getRetryCount() != null && j.getRetryCount() > 0)
                .count();
        s.setRetryRate(total > 0 ? (double) retryCountSum / total : 0.0);

        double avgMs = jobs.stream()
                .filter(j -> j.getDurationMs() != null && j.getDurationMs() > 0
                        && ToolExecutionJobStatus.COMPLETED.name().equals(j.getStatus()))
                .mapToLong(ToolExecutionJobEntity::getDurationMs)
                .average().orElse(0.0);
        s.setAvgDurationMs(avgMs);

        long maxMs = jobs.stream()
                .filter(j -> j.getDurationMs() != null)
                .mapToLong(ToolExecutionJobEntity::getDurationMs)
                .max().orElse(0L);
        s.setMaxDurationMs(maxMs);

        long totalRetries = jobs.stream()
                .filter(j -> j.getRetryCount() != null)
                .mapToLong(ToolExecutionJobEntity::getRetryCount)
                .sum();
        s.setTotalRetries(totalRetries);

        return s;
    }

    private List<ToolExecutionToolMetricResponse> buildToolMetrics(List<ToolExecutionJobEntity> jobs) {
        Map<String, List<ToolExecutionJobEntity>> byTool = jobs.stream()
                .filter(j -> j.getToolKey() != null)
                .collect(Collectors.groupingBy(ToolExecutionJobEntity::getToolKey));

        List<ToolExecutionToolMetricResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<ToolExecutionJobEntity>> entry : byTool.entrySet()) {
            List<ToolExecutionJobEntity> toolJobs = entry.getValue();
            ToolExecutionToolMetricResponse m = new ToolExecutionToolMetricResponse();
            m.setToolKey(entry.getKey());
            m.setTotalJobs((long) toolJobs.size());
            m.setCompletedJobs(countByStatus(toolJobs, ToolExecutionJobStatus.COMPLETED));
            m.setFailedJobs(countByStatus(toolJobs, ToolExecutionJobStatus.FAILED));
            m.setDeadLetteredJobs(countByStatus(toolJobs, ToolExecutionJobStatus.DEAD_LETTERED));

            long completed = m.getCompletedJobs();
            long total = toolJobs.size();
            m.setSuccessRate(total > 0 ? (double) completed / total : 0.0);

            double avgMs = toolJobs.stream()
                    .filter(j -> j.getDurationMs() != null && j.getDurationMs() > 0)
                    .mapToLong(ToolExecutionJobEntity::getDurationMs)
                    .average().orElse(0.0);
            m.setAvgDurationMs(avgMs);

            m.setTotalRetries(toolJobs.stream()
                    .filter(j -> j.getRetryCount() != null)
                    .mapToLong(ToolExecutionJobEntity::getRetryCount)
                    .sum());

            // Find top error code
            Map<String, Long> errorCodeCounts = toolJobs.stream()
                    .filter(j -> j.getErrorCode() != null)
                    .collect(Collectors.groupingBy(ToolExecutionJobEntity::getErrorCode, Collectors.counting()));
            m.setTopErrorCode(errorCodeCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null));

            // Find top failure stage
            Map<String, Long> stageCounts = toolJobs.stream()
                    .filter(j -> j.getFailureStage() != null)
                    .collect(Collectors.groupingBy(ToolExecutionJobEntity::getFailureStage, Collectors.counting()));
            m.setTopFailureStage(stageCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null));

            result.add(m);
        }

        result.sort(Comparator.comparing(ToolExecutionToolMetricResponse::getTotalJobs).reversed());
        return result;
    }

    private List<ToolExecutionDailyMetricResponse> buildDailyMetrics(List<ToolExecutionJobEntity> jobs) {
        Map<LocalDate, List<ToolExecutionJobEntity>> byDay = jobs.stream()
                .filter(j -> j.getCreateTime() != null)
                .collect(Collectors.groupingBy(j -> j.getCreateTime().toLocalDate()));

        // Fill gaps for the last 30 days
        LocalDate start = LocalDate.now().minusDays(29);
        LocalDate end = LocalDate.now().plusDays(1);
        List<ToolExecutionDailyMetricResponse> result = new ArrayList<>();
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            List<ToolExecutionJobEntity> dayJobs = byDay.getOrDefault(date, List.of());
            ToolExecutionDailyMetricResponse d = new ToolExecutionDailyMetricResponse();
            d.setDate(date.toString());
            d.setTotalJobs((long) dayJobs.size());
            d.setCompletedJobs(countByStatus(dayJobs, ToolExecutionJobStatus.COMPLETED));
            d.setFailedJobs(countByStatus(dayJobs, ToolExecutionJobStatus.FAILED));
            d.setDeadLetteredJobs(countByStatus(dayJobs, ToolExecutionJobStatus.DEAD_LETTERED));
            d.setRetryPendingJobs(countByStatus(dayJobs, ToolExecutionJobStatus.RETRY_PENDING));

            double avgMs = dayJobs.stream()
                    .filter(j -> j.getDurationMs() != null && j.getDurationMs() > 0)
                    .mapToLong(ToolExecutionJobEntity::getDurationMs)
                    .average().orElse(0.0);
            d.setAvgDurationMs(avgMs);

            result.add(d);
        }
        return result;
    }

    private List<ToolExecutionFailureMetricResponse> buildFailureMetrics(List<ToolExecutionJobEntity> jobs, boolean byErrorCode) {
        Map<String, List<ToolExecutionJobEntity>> grouped;
        if (byErrorCode) {
            grouped = jobs.stream()
                    .filter(j -> j.getErrorCode() != null && !j.getErrorCode().isBlank())
                    .collect(Collectors.groupingBy(ToolExecutionJobEntity::getErrorCode));
        } else {
            grouped = jobs.stream()
                    .filter(j -> j.getFailureStage() != null && !j.getFailureStage().isBlank())
                    .collect(Collectors.groupingBy(ToolExecutionJobEntity::getFailureStage));
        }

        List<ToolExecutionFailureMetricResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<ToolExecutionJobEntity>> entry : grouped.entrySet()) {
            ToolExecutionFailureMetricResponse f = new ToolExecutionFailureMetricResponse();
            f.setErrorCode(entry.getKey());
            f.setCount((long) entry.getValue().size());

            String latest = entry.getValue().stream()
                    .filter(j -> j.getFinishedAt() != null)
                    .max(Comparator.comparing(ToolExecutionJobEntity::getFinishedAt))
                    .map(j -> j.getFinishedAt().toString())
                    .orElse(null);
            f.setLatestTime(latest);
            result.add(f);
        }

        result.sort(Comparator.comparing(ToolExecutionFailureMetricResponse::getCount).reversed());
        return result;
    }

    private long countByStatus(List<ToolExecutionJobEntity> jobs, ToolExecutionJobStatus status) {
        return jobs.stream()
                .filter(j -> status.name().equals(j.getStatus()))
                .count();
    }

    private ToolExecutionJobResponse toSafeJobResponse(ToolExecutionJobEntity entity) {
        ToolExecutionJobResponse resp = new ToolExecutionJobResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setTaskId(entity.getTaskId() != null ? entity.getTaskId().toString() : null);
        resp.setRunId(entity.getRunId() != null ? entity.getRunId().toString() : null);
        resp.setStepId(entity.getStepId() != null ? entity.getStepId().toString() : null);
        resp.setToolExecutionId(entity.getToolExecutionId() != null ? entity.getToolExecutionId().toString() : null);
        resp.setToolKey(entity.getToolKey());
        resp.setStatus(entity.getStatus());
        resp.setPriority(entity.getPriority());
        resp.setRetryCount(entity.getRetryCount());
        resp.setMaxRetryCount(entity.getMaxRetryCount());
        // Intentionally omit requestPayload and resultPayload for security
        resp.setRequestPayload(null);
        resp.setResultPayload(null);
        resp.setLastError(entity.getLastError());
        resp.setErrorCode(entity.getErrorCode());
        resp.setFailureStage(entity.getFailureStage());
        resp.setNextRetryAt(entity.getNextRetryAt() != null ? entity.getNextRetryAt().toString() : null);
        resp.setDeadLetteredAt(entity.getDeadLetteredAt() != null ? entity.getDeadLetteredAt().toString() : null);
        resp.setDeadLetterReason(entity.getDeadLetterReason());
        resp.setSourceJobId(entity.getSourceJobId() != null ? entity.getSourceJobId().toString() : null);
        resp.setStartedAt(entity.getStartedAt() != null ? entity.getStartedAt().toString() : null);
        resp.setFinishedAt(entity.getFinishedAt() != null ? entity.getFinishedAt().toString() : null);
        resp.setDurationMs(entity.getDurationMs());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return resp;
    }
}
