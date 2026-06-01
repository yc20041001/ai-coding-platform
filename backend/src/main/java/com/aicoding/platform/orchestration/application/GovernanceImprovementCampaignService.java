package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceCrossTeamImprovementCampaignEntity;
import com.aicoding.platform.orchestration.dto.GovernanceCrossTeamImprovementCampaignResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCrossTeamImprovementCampaignMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceImprovementCampaignService {

    private final GovernanceCrossTeamImprovementCampaignMapper mapper;

    public GovernanceImprovementCampaignService(GovernanceCrossTeamImprovementCampaignMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceCrossTeamImprovementCampaignResponse createCampaign(String campaignKey, String campaignName,
                                                                          String improvementWindow) {
        LambdaQueryWrapper<GovernanceCrossTeamImprovementCampaignEntity> dup = new LambdaQueryWrapper<>();
        dup.eq(GovernanceCrossTeamImprovementCampaignEntity::getCampaignKey, campaignKey);
        if (mapper.selectCount(dup) > 0) throw new BizException(ErrorCode.CONFLICT, "Campaign key " + campaignKey + " 已存在");
        GovernanceCrossTeamImprovementCampaignEntity entity = new GovernanceCrossTeamImprovementCampaignEntity();
        entity.setCampaignKey(campaignKey); entity.setCampaignName(campaignName);
        entity.setCampaignStatus("DRAFT"); entity.setImprovementWindow(improvementWindow != null ? improvementWindow : "MONTH_3");
        entity.setTargetProjectIdsJson("[]");
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceCrossTeamImprovementCampaignResponse> listCampaigns() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceCrossTeamImprovementCampaignEntity>()
                .orderByDesc(GovernanceCrossTeamImprovementCampaignEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public GovernanceCrossTeamImprovementCampaignResponse updateCampaignStatus(String idStr, String newStatus) {
        GovernanceCrossTeamImprovementCampaignEntity entity = findEntity(idStr);
        entity.setCampaignStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private GovernanceCrossTeamImprovementCampaignEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceCrossTeamImprovementCampaignEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Campaign 不存在");
        return entity;
    }

    private GovernanceCrossTeamImprovementCampaignResponse toResponse(GovernanceCrossTeamImprovementCampaignEntity e) {
        GovernanceCrossTeamImprovementCampaignResponse r = new GovernanceCrossTeamImprovementCampaignResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setCampaignKey(e.getCampaignKey()); r.setCampaignName(e.getCampaignName());
        r.setCampaignStatus(e.getCampaignStatus());
        r.setTargetProjectIdsJson(e.getTargetProjectIdsJson());
        r.setSourceProjectId(e.getSourceProjectId() != null ? e.getSourceProjectId().toString() : null);
        r.setSourcePracticeType(e.getSourcePracticeType()); r.setImprovementWindow(e.getImprovementWindow());
        r.setGoalText(e.getGoalText()); r.setNotesText(e.getNotesText());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
