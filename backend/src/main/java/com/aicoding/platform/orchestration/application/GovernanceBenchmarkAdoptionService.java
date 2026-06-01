package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceBenchmarkAdoptionRecordEntity;
import com.aicoding.platform.orchestration.dto.GovernanceBenchmarkAdoptionRecordResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceBenchmarkAdoptionRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceBenchmarkAdoptionService {

    private final GovernanceBenchmarkAdoptionRecordMapper mapper;

    public GovernanceBenchmarkAdoptionService(GovernanceBenchmarkAdoptionRecordMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceBenchmarkAdoptionRecordResponse createRecord(String projectIdStr, String projectName, String metricKey,
                                                                    String adoptionStatus, String blockerType, String blockerNote) {
        GovernanceBenchmarkAdoptionRecordEntity entity = new GovernanceBenchmarkAdoptionRecordEntity();
        entity.setProjectId(parseLong(projectIdStr)); entity.setProjectName(projectName);
        entity.setMetricKey(metricKey); entity.setAdoptionStatus(adoptionStatus != null ? adoptionStatus : "IDENTIFIED");
        entity.setCurrentScore(BigDecimal.ZERO); entity.setTargetScore(BigDecimal.valueOf(80));
        entity.setBlockerType(blockerType); entity.setBlockerNote(blockerNote);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceBenchmarkAdoptionRecordResponse> listRecords() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceBenchmarkAdoptionRecordEntity>()
                .orderByDesc(GovernanceBenchmarkAdoptionRecordEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public GovernanceBenchmarkAdoptionRecordResponse updateRecordStatus(String idStr, String newStatus) {
        GovernanceBenchmarkAdoptionRecordEntity entity = findEntity(idStr);
        entity.setAdoptionStatus(newStatus);
        if ("ADOPTED".equals(newStatus)) entity.setAdoptedAt(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private GovernanceBenchmarkAdoptionRecordEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceBenchmarkAdoptionRecordEntity entity = mapper.selectById(id);
        if (entity == null) throw new com.aicoding.platform.common.exception.BizException(
                com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Adoption record 不存在");
        return entity;
    }

    private GovernanceBenchmarkAdoptionRecordResponse toResponse(GovernanceBenchmarkAdoptionRecordEntity e) {
        GovernanceBenchmarkAdoptionRecordResponse r = new GovernanceBenchmarkAdoptionRecordResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setProjectName(e.getProjectName()); r.setMetricKey(e.getMetricKey());
        r.setAdoptionStatus(e.getAdoptionStatus()); r.setCurrentScore(e.getCurrentScore());
        r.setTargetScore(e.getTargetScore()); r.setBlockerType(e.getBlockerType());
        r.setBlockerNote(e.getBlockerNote());
        r.setOwnerId(e.getOwnerId() != null ? e.getOwnerId().toString() : null);
        r.setOwnerName(e.getOwnerName()); r.setAdoptedAt(e.getAdoptedAt());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
