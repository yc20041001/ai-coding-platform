package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceCapacityForecastEntity;
import com.aicoding.platform.orchestration.dto.GovernanceSimulationComparisonResponse;
import com.aicoding.platform.orchestration.dto.GovernanceSimulationResultResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCapacityForecastMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class GovernanceWhatIfPlannerService {

    private final GovernanceCapacityForecastMapper capacityForecastMapper;
    private final GovernanceSimulationService governanceSimulationService;

    public GovernanceWhatIfPlannerService(GovernanceCapacityForecastMapper capacityForecastMapper,
                                           GovernanceSimulationService governanceSimulationService) {
        this.capacityForecastMapper = capacityForecastMapper;
        this.governanceSimulationService = governanceSimulationService;
    }

    @Transactional(readOnly = true)
    public GovernanceSimulationComparisonResponse compareWithBaseline(String scenarioIdStr) {
        return governanceSimulationService.getComparison(scenarioIdStr);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageProjectedBacklog() {
        List<GovernanceCapacityForecastEntity> forecasts = getForecasts();
        return forecasts.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(forecasts.stream().mapToInt(f -> f.getProjectedBacklogCount() != null ? f.getProjectedBacklogCount() : 0).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageProjectedOverdue() {
        List<GovernanceCapacityForecastEntity> forecasts = getForecasts();
        return forecasts.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(forecasts.stream().mapToInt(f -> f.getProjectedOverdueCount() != null ? f.getProjectedOverdueCount() : 0).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<GovernanceCapacityForecastEntity> getForecasts() {
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceCapacityForecastEntity::getSnapshotDate, LocalDate.now());
        w.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, 7);
        List<GovernanceCapacityForecastEntity> list = capacityForecastMapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            w.last("LIMIT 50");
            list = capacityForecastMapper.selectList(w);
        }
        return list;
    }
}
