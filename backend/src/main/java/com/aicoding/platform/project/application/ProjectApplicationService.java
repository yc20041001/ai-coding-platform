package com.aicoding.platform.project.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectMemberEntity;
import com.aicoding.platform.member.domain.ProjectMemberStatus;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.member.infrastructure.ProjectMemberMapper;
import com.aicoding.platform.project.domain.ProjectConfigEntity;
import com.aicoding.platform.project.domain.ProjectEntity;
import com.aicoding.platform.project.domain.ProjectStatus;
import com.aicoding.platform.project.dto.CreateProjectRequest;
import com.aicoding.platform.project.dto.ProjectDetailResponse;
import com.aicoding.platform.project.dto.ProjectOverviewResponse;
import com.aicoding.platform.project.dto.ProjectResponse;
import com.aicoding.platform.project.dto.UpdateProjectRequest;
import com.aicoding.platform.project.infrastructure.ProjectConfigMapper;
import com.aicoding.platform.project.infrastructure.ProjectMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectApplicationService {

    private final ProjectMapper projectMapper;
    private final ProjectConfigMapper projectConfigMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectPermissionService projectPermissionService;

    public ProjectApplicationService(ProjectMapper projectMapper,
                                      ProjectConfigMapper projectConfigMapper,
                                      ProjectMemberMapper projectMemberMapper,
                                      ProjectPermissionService projectPermissionService) {
        this.projectMapper = projectMapper;
        this.projectConfigMapper = projectConfigMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();

        ProjectEntity project = new ProjectEntity();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setIcon(request.getIcon());
        project.setOwnerId(currentUser.getUserId());
        project.setTechStack(request.getTechStack() != null
                ? String.join(",", request.getTechStack()) : null);
        project.setStatus(ProjectStatus.ACTIVE.name());
        projectMapper.insert(project);

        ProjectMemberEntity ownerMember = new ProjectMemberEntity();
        ownerMember.setProjectId(project.getId());
        ownerMember.setUserId(currentUser.getUserId());
        ownerMember.setRole(ProjectRole.OWNER.name());
        ownerMember.setStatus(ProjectMemberStatus.ACTIVE.name());
        ownerMember.setJoinedTime(LocalDateTime.now());
        projectMemberMapper.insert(ownerMember);

        return toProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public PageResult<ProjectResponse> listProjects(PageQuery pageQuery, String keyword, String status) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();

        List<Long> projectIds = projectPermissionService.getUserProjectIds(currentUser.getUserId());
        if (projectIds.isEmpty()) {
            return PageResult.empty(pageQuery);
        }

        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<ProjectEntity>()
                .in(ProjectEntity::getId, projectIds)
                .ne(ProjectEntity::getStatus, ProjectStatus.DELETED.name());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ProjectEntity::getName, keyword)
                    .or().like(ProjectEntity::getDescription, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ProjectEntity::getStatus, status);
        }
        wrapper.orderByDesc(ProjectEntity::getCreateTime);

        Page<ProjectEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ProjectEntity> result = projectMapper.selectPage(page, wrapper);

        List<ProjectResponse> records = result.getRecords().stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null || ProjectStatus.DELETED.name().equals(project.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        ProjectDetailResponse response = new ProjectDetailResponse();
        fillProjectResponse(response, project);
        response.setOwnerName(getOwnerName(project.getOwnerId()));
        response.setRepoUrl(project.getRepoUrl());
        response.setCurrentUserRole(projectPermissionService.getCurrentMemberRole(projectId));
        response.setUpdateTime(project.getUpdateTime() != null ? project.getUpdateTime().toString() : null);
        return response;
    }

    @Transactional
    public boolean updateProject(Long projectId, UpdateProjectRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null || ProjectStatus.DELETED.name().equals(project.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTechStack() != null) {
            project.setTechStack(String.join(",", request.getTechStack()));
        }
        if (request.getIcon() != null) {
            project.setIcon(request.getIcon());
        }

        projectMapper.updateById(project);
        return true;
    }

    @Transactional
    public boolean archiveProject(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        project.setStatus(ProjectStatus.ARCHIVED.name());
        projectMapper.updateById(project);
        return true;
    }

    @Transactional(readOnly = true)
    public ProjectOverviewResponse getProjectOverview(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        Long memberCount = projectMemberMapper.selectCount(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getProjectId, projectId)
                        .eq(ProjectMemberEntity::getStatus, ProjectMemberStatus.ACTIVE.name()));

        ProjectOverviewResponse overview = ProjectOverviewResponse.empty();
        overview.setMemberCount(memberCount.intValue());
        return overview;
    }

    @Transactional
    public boolean updateProjectConfig(Long projectId, Map<String, Object> configs) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        if (configs == null || configs.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            upsertConfig(projectId, entry.getKey(), entry.getValue());
        }
        return true;
    }

    private void upsertConfig(Long projectId, String configKey, Object value) {
        ProjectConfigEntity existing = projectConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectConfigEntity>()
                        .eq(ProjectConfigEntity::getProjectId, projectId)
                        .eq(ProjectConfigEntity::getConfigKey, configKey));

        if (existing != null) {
            existing.setConfigValue(Objects.toString(value, null));
            projectConfigMapper.updateById(existing);
        } else {
            ProjectConfigEntity config = new ProjectConfigEntity();
            config.setProjectId(projectId);
            config.setConfigKey(configKey);
            config.setConfigValue(Objects.toString(value, null));
            projectConfigMapper.insert(config);
        }
    }

    private ProjectResponse toProjectResponse(ProjectEntity project) {
        ProjectResponse response = new ProjectResponse();
        fillProjectResponse(response, project);
        return response;
    }

    private void fillProjectResponse(ProjectResponse response, ProjectEntity project) {
        response.setId(project.getId().toString());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setIcon(project.getIcon());
        response.setOwnerId(project.getOwnerId() != null ? project.getOwnerId().toString() : null);
        response.setTechStack(parseTechStack(project.getTechStack()));
        response.setStatus(project.getStatus());
        response.setCreateTime(project.getCreateTime() != null ? project.getCreateTime().toString() : null);
    }

    private List<String> parseTechStack(String techStack) {
        if (techStack == null || techStack.isBlank()) {
            return null;
        }
        return List.of(techStack.split(","));
    }

    private String getOwnerName(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        // Owner name lookup will be added through an auth user query service.
        return null;
    }
}
