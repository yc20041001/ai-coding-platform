package com.aicoding.platform.repository.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.repository.domain.GitOperationLogEntity;
import com.aicoding.platform.repository.domain.GitOperationStatus;
import com.aicoding.platform.repository.domain.GitOperationType;
import com.aicoding.platform.repository.domain.ProjectRepositoryEntity;
import com.aicoding.platform.repository.domain.RepositoryStatus;
import com.aicoding.platform.repository.dto.BindRepositoryRequest;
import com.aicoding.platform.repository.dto.CloneRepositoryRequest;
import com.aicoding.platform.repository.dto.GitOperationResponse;
import com.aicoding.platform.repository.dto.GithubRepositoryResponse;
import com.aicoding.platform.repository.dto.PullRepositoryRequest;
import com.aicoding.platform.repository.dto.RepositoryBranchResponse;
import com.aicoding.platform.repository.dto.RepositoryDiffResponse;
import com.aicoding.platform.repository.dto.RepositoryResponse;
import com.aicoding.platform.repository.infrastructure.GitOperationLogMapper;
import com.aicoding.platform.repository.infrastructure.ProjectRepositoryMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class RepositoryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryApplicationService.class);

    private final ProjectRepositoryMapper projectRepositoryMapper;
    private final GitOperationLogMapper gitOperationLogMapper;
    private final GitWorkspaceService gitWorkspaceService;
    private final ProjectPermissionService projectPermissionService;

    public RepositoryApplicationService(ProjectRepositoryMapper projectRepositoryMapper,
                                         GitOperationLogMapper gitOperationLogMapper,
                                         GitWorkspaceService gitWorkspaceService,
                                         ProjectPermissionService projectPermissionService) {
        this.projectRepositoryMapper = projectRepositoryMapper;
        this.gitOperationLogMapper = gitOperationLogMapper;
        this.gitWorkspaceService = gitWorkspaceService;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public PageResult<GithubRepositoryResponse> listGithubRepositories(PageQuery pageQuery, String keyword) {
        // GitHub OAuth 未实现，返回空列表
        log.debug("GitHub repository list requested (keyword={}), returning empty (OAuth not configured)", keyword);
        return PageResult.empty(pageQuery);
    }

    @Transactional
    public RepositoryResponse bindRepository(Long projectId, BindRepositoryRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        ProjectRepositoryEntity existing = projectRepositoryMapper.selectOne(
                new LambdaQueryWrapper<ProjectRepositoryEntity>()
                        .eq(ProjectRepositoryEntity::getProjectId, projectId));
        if (existing != null) {
            throw new BizException(ErrorCode.CONFLICT, "项目已绑定仓库");
        }

        ProjectRepositoryEntity repo = new ProjectRepositoryEntity();
        repo.setProjectId(projectId);
        repo.setProvider(request.getProvider());
        repo.setRepoFullName(request.getRepoFullName());
        repo.setRepoUrl(request.getRepoUrl());
        repo.setCloneUrl(request.getCloneUrl());
        repo.setDefaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main");
        repo.setStatus(RepositoryStatus.BOUND.name());
        projectRepositoryMapper.insert(repo);

        RepositoryResponse response = new RepositoryResponse();
        response.setRepositoryId(repo.getId().toString());
        response.setStatus(repo.getStatus());
        return response;
    }

    @Transactional
    public GitOperationResponse cloneRepository(Long projectId, CloneRepositoryRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ProjectRepositoryEntity repo = getRepository(projectId);

        GitOperationLogEntity operation = createOperationLog(projectId, repo.getId(),
                currentUser.getUserId(), GitOperationType.CLONE, request.getBranch());

        try {
            repo.setStatus(RepositoryStatus.CLONING.name());
            projectRepositoryMapper.updateById(repo);

            Path repoPath = gitWorkspaceService.getRepoPath(projectId);
            gitWorkspaceService.cloneRepository(repo.getCloneUrl(), repoPath, request.getBranch(), request.isForce());

            repo.setLocalPath(repoPath.toString());
            repo.setStatus(RepositoryStatus.READY.name());
            repo.setLastSyncTime(LocalDateTime.now());
            projectRepositoryMapper.updateById(repo);

            updateOperationSuccess(operation, "Clone 完成");
        } catch (GitAPIException | IOException e) {
            log.error("Clone failed for project {}", projectId, e);
            repo.setStatus(RepositoryStatus.FAILED.name());
            projectRepositoryMapper.updateById(repo);
            updateOperationFailed(operation, e.getMessage());
        }

        GitOperationResponse response = new GitOperationResponse();
        response.setOperationId(operation.getId().toString());
        response.setStatus(operation.getStatus());
        return response;
    }

    @Transactional
    public GitOperationResponse pullRepository(Long projectId, PullRepositoryRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ProjectRepositoryEntity repo = getRepository(projectId);
        if (!RepositoryStatus.READY.name().equals(repo.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仓库状态不是 READY，无法 Pull");
        }

        GitOperationLogEntity operation = createOperationLog(projectId, repo.getId(),
                currentUser.getUserId(), GitOperationType.PULL, request.getBranch());

        try {
            Path repoPath = gitWorkspaceService.getRepoPath(projectId);
            gitWorkspaceService.pullRepository(repoPath, request.getBranch());

            repo.setLastSyncTime(LocalDateTime.now());
            projectRepositoryMapper.updateById(repo);

            updateOperationSuccess(operation, "Pull 完成");
        } catch (GitAPIException | IOException e) {
            log.error("Pull failed for project {}", projectId, e);
            updateOperationFailed(operation, e.getMessage());
        }

        GitOperationResponse response = new GitOperationResponse();
        response.setOperationId(operation.getId().toString());
        response.setStatus(operation.getStatus());
        return response;
    }

    @Transactional(readOnly = true)
    public List<RepositoryBranchResponse> getBranches(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ProjectRepositoryEntity repo = getRepository(projectId);
        if (!RepositoryStatus.READY.name().equals(repo.getStatus())) {
            // 仓库未 Clone，返回空列表
            return Collections.emptyList();
        }

        try {
            Path repoPath = gitWorkspaceService.getRepoPath(projectId);
            return gitWorkspaceService.listBranches(repoPath);
        } catch (GitAPIException | IOException e) {
            log.error("Failed to list branches for project {}", projectId, e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public RepositoryDiffResponse getDiff(Long projectId, String base, String head) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ProjectRepositoryEntity repo = getRepository(projectId);
        if (!RepositoryStatus.READY.name().equals(repo.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仓库未 Clone，无法查看 Diff");
        }

        try {
            Path repoPath = gitWorkspaceService.getRepoPath(projectId);
            return gitWorkspaceService.getDiff(repoPath, base, head);
        } catch (GitAPIException | IOException e) {
            log.error("Failed to get diff for project {}", projectId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "获取 Diff 失败: " + e.getMessage());
        }
    }

    private ProjectRepositoryEntity getRepository(Long projectId) {
        ProjectRepositoryEntity repo = projectRepositoryMapper.selectOne(
                new LambdaQueryWrapper<ProjectRepositoryEntity>()
                        .eq(ProjectRepositoryEntity::getProjectId, projectId));
        if (repo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目未绑定仓库");
        }
        return repo;
    }

    private GitOperationLogEntity createOperationLog(Long projectId, Long repositoryId,
                                                      Long userId, GitOperationType type, String branch) {
        GitOperationLogEntity operation = new GitOperationLogEntity();
        operation.setProjectId(projectId);
        operation.setRepositoryId(repositoryId);
        operation.setUserId(userId);
        operation.setOperationType(type.name());
        operation.setBranch(branch);
        operation.setStatus(GitOperationStatus.PENDING.name());
        operation.setStartTime(LocalDateTime.now());
        gitOperationLogMapper.insert(operation);
        return operation;
    }

    private void updateOperationSuccess(GitOperationLogEntity operation, String message) {
        operation.setStatus(GitOperationStatus.SUCCESS.name());
        operation.setMessage(message);
        operation.setEndTime(LocalDateTime.now());
        gitOperationLogMapper.updateById(operation);
    }

    private void updateOperationFailed(GitOperationLogEntity operation, String errorMessage) {
        operation.setStatus(GitOperationStatus.FAILED.name());
        operation.setErrorMessage(errorMessage);
        operation.setEndTime(LocalDateTime.now());
        gitOperationLogMapper.updateById(operation);
    }
}
