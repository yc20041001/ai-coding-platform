package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentRootCauseNoteEntity;
import com.aicoding.platform.orchestration.dto.SimilarIncidentResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentRootCauseNoteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimilarIncidentSearchService {

    private final ToolIncidentMapper incidentMapper;
    private final ToolIncidentRootCauseNoteMapper noteMapper;

    public SimilarIncidentSearchService(ToolIncidentMapper incidentMapper,
                                        ToolIncidentRootCauseNoteMapper noteMapper) {
        this.incidentMapper = incidentMapper;
        this.noteMapper = noteMapper;
    }

    @Transactional(readOnly = true)
    public List<SimilarIncidentResponse> searchSimilar(Long incidentId, String query, Integer limit) {
        ToolIncidentEntity current = getIncidentOrThrow(incidentId);
        if (limit == null || limit < 1) limit = 10;
        if (limit > 50) limit = 50;

        List<ToolIncidentEntity> candidates = incidentMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, current.getProjectId())
                        .ne(ToolIncidentEntity::getId, incidentId)
                        .orderByDesc(ToolIncidentEntity::getCreateTime));

        String searchQuery = query != null ? query.toLowerCase() : "";

        List<SimilarIncidentResponse> results = new ArrayList<>();
        for (ToolIncidentEntity candidate : candidates) {
            double score;
            String matchedField = "default";
            String snippet = null;

            if (!searchQuery.isBlank()) {
                ScoreResult result = computeScore(candidate, searchQuery);
                score = result.score;
                matchedField = result.matchedField;
                snippet = result.snippet;
            } else {
                score = 0.5;
            }

            if (score > 0) {
                SimilarIncidentResponse resp = toResponse(candidate, score, matchedField, snippet);
                results.add(resp);
            }
        }

        results.sort(Comparator.comparingDouble(SimilarIncidentResponse::getScore).reversed());

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SimilarIncidentResponse> searchByIncident(Long incidentId, Integer limit) {
        ToolIncidentEntity current = getIncidentOrThrow(incidentId);

        List<ToolIncidentEntity> candidates = incidentMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentEntity>()
                        .eq(ToolIncidentEntity::getProjectId, current.getProjectId())
                        .ne(ToolIncidentEntity::getId, incidentId)
                        .orderByDesc(ToolIncidentEntity::getCreateTime));

        if (limit == null || limit < 1) limit = 10;
        if (limit > 50) limit = 50;

        List<SimilarIncidentResponse> results = new ArrayList<>();
        for (ToolIncidentEntity candidate : candidates) {
            double score = computeSimilarityScore(current, candidate);
            if (score > 0) {
                results.add(toResponse(candidate, score, "similarity", null));
            }
        }

        results.sort(Comparator.comparingDouble(SimilarIncidentResponse::getScore).reversed());
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private ScoreResult computeScore(ToolIncidentEntity candidate, String query) {
        double score = 0;
        String matchedField = null;
        String snippet = null;

        if (candidate.getTitle() != null && candidate.getTitle().toLowerCase().contains(query)) {
            score = 0.95;
            matchedField = "title";
            snippet = candidate.getTitle();
        } else if (candidate.getSummary() != null && candidate.getSummary().toLowerCase().contains(query)) {
            score = 0.75;
            matchedField = "summary";
            snippet = candidate.getSummary();
        }

        if (score < 1.0) {
            ToolIncidentRootCauseNoteEntity note = noteMapper.selectOne(
                    new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                            .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, candidate.getId())
                            .last("LIMIT 1"));

            if (note != null) {
                if (score < 0.90 && note.getRootCause() != null && note.getRootCause().toLowerCase().contains(query)) {
                    score = 0.90;
                    matchedField = "rootCause";
                    snippet = note.getRootCause();
                }
                if (score < 0.85 && note.getResolution() != null && note.getResolution().toLowerCase().contains(query)) {
                    score = 0.85;
                    matchedField = "resolution";
                    snippet = note.getResolution();
                }
                if (score < 0.70 && note.getTags() != null && note.getTags().toLowerCase().contains(query)) {
                    score = 0.70;
                    matchedField = "tags";
                    snippet = note.getTags();
                }
            }
        }

        return new ScoreResult(score, matchedField, snippet);
    }

    private double computeSimilarityScore(ToolIncidentEntity current, ToolIncidentEntity candidate) {
        double score = 0.5;
        if (current.getSeverity() != null && current.getSeverity().equals(candidate.getSeverity())) {
            score += 0.15;
        }
        if (current.getSourceType() != null && current.getSourceType().equals(candidate.getSourceType())) {
            score += 0.10;
        }
        return Math.min(score, 1.0);
    }

    private SimilarIncidentResponse toResponse(ToolIncidentEntity entity, double score,
                                                String matchedField, String snippet) {
        SimilarIncidentResponse resp = new SimilarIncidentResponse();
        resp.setIncidentId(entity.getId().toString());
        resp.setTitle(entity.getTitle());
        resp.setStatus(entity.getStatus());
        resp.setSeverity(entity.getSeverity());
        resp.setScore(score);
        resp.setMatchedField(matchedField);
        resp.setSnippet(snippet);
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return resp;
    }

    private ToolIncidentEntity getIncidentOrThrow(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return incident;
    }

    private static class ScoreResult {
        final double score;
        final String matchedField;
        final String snippet;

        ScoreResult(double score, String matchedField, String snippet) {
            this.score = score;
            this.matchedField = matchedField;
            this.snippet = snippet;
        }
    }
}
