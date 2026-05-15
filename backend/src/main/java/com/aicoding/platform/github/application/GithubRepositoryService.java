package com.aicoding.platform.github.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.github.domain.GithubRepositoryCacheEntity;
import com.aicoding.platform.github.dto.GithubRepositoryResponse;
import com.aicoding.platform.github.infrastructure.GithubRepositoryCacheMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GithubRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryService.class);

    private final GithubClient githubClient;
    private final GithubOAuthService githubOAuthService;
    private final GithubRepositoryCacheMapper repositoryCacheMapper;
    private final AuditLogApplicationService auditLogApplicationService;

    public GithubRepositoryService(GithubClient githubClient,
                                   GithubOAuthService githubOAuthService,
                                   GithubRepositoryCacheMapper repositoryCacheMapper,
                                   AuditLogApplicationService auditLogApplicationService) {
        this.githubClient = githubClient;
        this.githubOAuthService = githubOAuthService;
        this.repositoryCacheMapper = repositoryCacheMapper;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public List<GithubRepositoryResponse> sync() {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        String token = githubOAuthService.getAccessToken(userId);
        List<JsonNode> repos = githubClient.listRepositories(token);

        List<GithubRepositoryResponse> result = new ArrayList<>();
        for (JsonNode repo : repos) {
            Long githubRepoId = repo.get("id").asLong();
            String fullName = repo.has("full_name") ? repo.get("full_name").asText() : "";
            String owner = repo.has("owner") && repo.get("owner").has("login")
                    ? repo.get("owner").get("login").asText() : "";
            String repoName = repo.has("name") ? repo.get("name").asText() : "";

            GithubRepositoryCacheEntity existing = repositoryCacheMapper.selectOne(
                    new LambdaQueryWrapper<GithubRepositoryCacheEntity>()
                            .eq(GithubRepositoryCacheEntity::getUserId, userId)
                            .eq(GithubRepositoryCacheEntity::getGithubRepoId, githubRepoId));

            GithubRepositoryCacheEntity entity = existing != null ? existing : new GithubRepositoryCacheEntity();
            entity.setUserId(userId);
            entity.setGithubRepoId(githubRepoId);
            entity.setOwner(owner);
            entity.setRepoName(repoName);
            entity.setFullName(fullName);
            entity.setPrivateRepo(repo.has("private") && repo.get("private").asBoolean() ? 1 : 0);
            entity.setDefaultBranch(repo.has("default_branch") ? repo.get("default_branch").asText() : null);
            entity.setHtmlUrl(repo.has("html_url") ? repo.get("html_url").asText() : null);
            entity.setDescription(repo.has("description") && !repo.get("description").isNull()
                    ? repo.get("description").asText() : null);
            entity.setLanguage(repo.has("language") && !repo.get("language").isNull()
                    ? repo.get("language").asText() : null);
            if (repo.has("updated_at") && !repo.get("updated_at").isNull()) {
                try { entity.setGithubUpdatedAt(LocalDateTime.parse(
                        repo.get("updated_at").asText().replace("Z", ""))); } catch (Exception e) { /* ignore */ }
            }
            entity.setUpdateTime(LocalDateTime.now());

            if (existing != null) {
                repositoryCacheMapper.updateById(entity);
            } else {
                entity.setCreateTime(LocalDateTime.now());
                repositoryCacheMapper.insert(entity);
            }

            result.add(toResponse(entity));
        }

        auditLogApplicationService.recordSuccess(null, null,
                AuditActionType.GITHUB_REPOSITORY_SYNC.name(), "GITHUB_REPOSITORY",
                "同步 " + result.size() + " 个仓库");

        log.info("GitHub repo sync: userId={} count={}", userId, result.size());
        return result;
    }

    public List<GithubRepositoryResponse> list() {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        List<GithubRepositoryCacheEntity> entities = repositoryCacheMapper.selectList(
                new LambdaQueryWrapper<GithubRepositoryCacheEntity>()
                        .eq(GithubRepositoryCacheEntity::getUserId, userId)
                        .orderByDesc(GithubRepositoryCacheEntity::getUpdateTime));

        List<GithubRepositoryResponse> result = new ArrayList<>();
        for (GithubRepositoryCacheEntity e : entities) {
            result.add(toResponse(e));
        }
        return result;
    }

    private GithubRepositoryResponse toResponse(GithubRepositoryCacheEntity entity) {
        GithubRepositoryResponse r = new GithubRepositoryResponse();
        r.setId(entity.getId() != null ? entity.getId().toString() : null);
        r.setGithubRepoId(entity.getGithubRepoId());
        r.setOwner(entity.getOwner());
        r.setRepoName(entity.getRepoName());
        r.setFullName(entity.getFullName());
        r.setPrivateRepo(entity.getPrivateRepo() != null && entity.getPrivateRepo() == 1);
        r.setDefaultBranch(entity.getDefaultBranch());
        r.setHtmlUrl(entity.getHtmlUrl());
        r.setDescription(entity.getDescription());
        r.setLanguage(entity.getLanguage());
        r.setGithubUpdatedAt(entity.getGithubUpdatedAt() != null ? entity.getGithubUpdatedAt().toString() : null);
        return r;
    }
}
