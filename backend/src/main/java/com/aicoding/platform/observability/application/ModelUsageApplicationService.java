package com.aicoding.platform.observability.application;

import com.aicoding.platform.observability.dto.ModelUsageDailyResponse;
import com.aicoding.platform.observability.dto.ModelUsageSummaryResponse;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelUsageApplicationService {

    private final ModelRequestLogMapper modelRequestLogMapper;

    public ModelUsageApplicationService(ModelRequestLogMapper modelRequestLogMapper) {
        this.modelRequestLogMapper = modelRequestLogMapper;
    }

    @Transactional(readOnly = true)
    public ModelUsageSummaryResponse getGlobalSummary() {
        return buildSummary(new LambdaQueryWrapper<>());
    }

    @Transactional(readOnly = true)
    public ModelUsageSummaryResponse getProjectSummary(Long projectId) {
        return buildSummary(new LambdaQueryWrapper<ModelRequestLogEntity>()
                .eq(ModelRequestLogEntity::getProjectId, projectId));
    }

    @Transactional(readOnly = true)
    public List<ModelUsageDailyResponse> getProjectDaily(Long projectId) {
        List<ModelRequestLogEntity> logs = modelRequestLogMapper.selectList(
                new LambdaQueryWrapper<ModelRequestLogEntity>()
                        .eq(ModelRequestLogEntity::getProjectId, projectId)
                        .ge(ModelRequestLogEntity::getCreateTime, LocalDate.now().minusDays(30).atStartOfDay())
                        .orderByAsc(ModelRequestLogEntity::getCreateTime));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<ModelRequestLogEntity>> byDate = logs.stream()
                .collect(Collectors.groupingBy(e -> e.getCreateTime().toLocalDate().format(dateFmt)));

        List<ModelUsageDailyResponse> result = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(dateFmt);
            ModelUsageDailyResponse resp = new ModelUsageDailyResponse();
            resp.setDate(date);

            List<ModelRequestLogEntity> dayLogs = byDate.getOrDefault(date, List.of());
            resp.setRequestCount((long) dayLogs.size());
            resp.setSuccessCount(dayLogs.stream().filter(e -> Boolean.TRUE.equals(e.getSuccess())).count());
            resp.setFailureCount(dayLogs.stream().filter(e -> !Boolean.TRUE.equals(e.getSuccess())).count());
            resp.setTotalTokens(sumTotalTokens(dayLogs));
            result.add(resp);
        }
        return result;
    }

    private ModelUsageSummaryResponse buildSummary(LambdaQueryWrapper<ModelRequestLogEntity> wrapper) {
        List<ModelRequestLogEntity> logs = modelRequestLogMapper.selectList(wrapper);

        long total = logs.size();
        long success = logs.stream().filter(e -> Boolean.TRUE.equals(e.getSuccess())).count();
        long failure = total - success;

        long promptTokens = sumPromptTokens(logs);
        long completionTokens = sumCompletionTokens(logs);
        long totalTokens = sumTotalTokens(logs);

        double avgLatency = averageLatency(logs);

        long mockCount = logs.stream().filter(e -> "MOCK".equalsIgnoreCase(e.getProvider())).count();
        long realCount = total - mockCount;

        ModelUsageSummaryResponse resp = new ModelUsageSummaryResponse();
        resp.setRequestCount(total);
        resp.setSuccessCount(success);
        resp.setFailureCount(failure);
        resp.setSuccessRate(total > 0
                ? BigDecimal.valueOf(success).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        resp.setPromptTokens(promptTokens);
        resp.setCompletionTokens(completionTokens);
        resp.setTotalTokens(totalTokens);
        resp.setAvgLatencyMs(BigDecimal.valueOf(avgLatency).setScale(2, RoundingMode.HALF_UP));
        resp.setMockCount(mockCount);
        resp.setRealProviderCount(realCount);
        return resp;
    }

    private long sumPromptTokens(List<ModelRequestLogEntity> logs) {
        long sum = 0L;
        for (ModelRequestLogEntity log : logs) {
            Long promptTokens = log.getPromptTokens();
            if (promptTokens != null) {
                sum += promptTokens;
            }
        }
        return sum;
    }

    private long sumCompletionTokens(List<ModelRequestLogEntity> logs) {
        long sum = 0L;
        for (ModelRequestLogEntity log : logs) {
            Long completionTokens = log.getCompletionTokens();
            if (completionTokens != null) {
                sum += completionTokens;
            }
        }
        return sum;
    }

    private long sumTotalTokens(List<ModelRequestLogEntity> logs) {
        long sum = 0L;
        for (ModelRequestLogEntity log : logs) {
            Long totalTokens = log.getTotalTokens();
            if (totalTokens != null) {
                sum += totalTokens;
            }
        }
        return sum;
    }

    private double averageLatency(List<ModelRequestLogEntity> logs) {
        if (logs.isEmpty()) {
            return 0.0;
        }

        long sum = 0L;
        for (ModelRequestLogEntity log : logs) {
            Long latencyMs = log.getLatencyMs();
            if (latencyMs != null) {
                sum += latencyMs;
            }
        }
        return (double) sum / logs.size();
    }
}
