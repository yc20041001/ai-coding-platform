package com.aicoding.platform.member.application;

import com.aicoding.platform.auth.domain.UserEntity;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.domain.ProjectInvitationEntity;
import com.aicoding.platform.member.domain.ProjectInvitationStatus;
import com.aicoding.platform.member.domain.ProjectMemberEntity;
import com.aicoding.platform.member.domain.ProjectMemberStatus;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.member.dto.InviteProjectMemberRequest;
import com.aicoding.platform.member.dto.InviteProjectMemberResponse;
import com.aicoding.platform.member.dto.ProjectMemberResponse;
import com.aicoding.platform.member.dto.UpdateProjectMemberRoleRequest;
import com.aicoding.platform.member.infrastructure.ProjectInvitationMapper;
import com.aicoding.platform.member.infrastructure.ProjectMemberMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectMemberApplicationService {

    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectInvitationMapper projectInvitationMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;

    public ProjectMemberApplicationService(ProjectMemberMapper projectMemberMapper,
                                            ProjectInvitationMapper projectInvitationMapper,
                                            UserMapper userMapper,
                                            ProjectPermissionService projectPermissionService) {
        this.projectMemberMapper = projectMemberMapper;
        this.projectInvitationMapper = projectInvitationMapper;
        this.userMapper = userMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public PageResult<ProjectMemberResponse> getMembers(Long projectId, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        Page<ProjectMemberEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        LambdaQueryWrapper<ProjectMemberEntity> wrapper = new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getStatus, ProjectMemberStatus.ACTIVE.name())
                .orderByAsc(ProjectMemberEntity::getJoinedTime);

        Page<ProjectMemberEntity> result = projectMemberMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(ProjectMemberEntity::getUserId)
                .collect(Collectors.toList());
        Map<Long, UserEntity> userMap = buildUserMap(userIds);

        List<ProjectMemberResponse> records = result.getRecords().stream()
                .map(member -> toMemberResponse(member, userMap.get(member.getUserId())))
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional
    public InviteProjectMemberResponse inviteMember(Long projectId, InviteProjectMemberRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        String role = request.getRole().toUpperCase();
        validateRole(role);

        ProjectInvitationEntity invitation = new ProjectInvitationEntity();
        invitation.setProjectId(projectId);
        invitation.setEmail(request.getEmail());
        invitation.setInviterId(currentUser.getUserId());
        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(ProjectInvitationStatus.PENDING.name());
        invitation.setExpireTime(LocalDateTime.now().plusDays(7));
        projectInvitationMapper.insert(invitation);

        InviteProjectMemberResponse response = new InviteProjectMemberResponse();
        response.setInvitationId(invitation.getId().toString());
        response.setEmail(invitation.getEmail());
        response.setRole(invitation.getRole());
        response.setStatus(invitation.getStatus());
        response.setExpireTime(invitation.getExpireTime().toString());
        return response;
    }

    @Transactional
    public boolean updateMemberRole(Long projectId, Long targetUserId, UpdateProjectMemberRoleRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        if (currentUser.getUserId().equals(targetUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
        }

        String newRole = request.getRole().toUpperCase();
        validateRole(newRole);

        ProjectMemberEntity member = getActiveMember(projectId, targetUserId);
        if (member == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        member.setRole(newRole);
        projectMemberMapper.updateById(member);
        return true;
    }

    @Transactional
    public boolean removeMember(Long projectId, Long targetUserId) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        if (currentUser.getUserId().equals(targetUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能移除自己");
        }

        ProjectMemberEntity member = getActiveMember(projectId, targetUserId);
        if (member == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        member.setStatus(ProjectMemberStatus.REMOVED.name());
        projectMemberMapper.updateById(member);
        return true;
    }

    private ProjectMemberEntity getActiveMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getProjectId, projectId)
                        .eq(ProjectMemberEntity::getUserId, userId)
                        .eq(ProjectMemberEntity::getStatus, ProjectMemberStatus.ACTIVE.name()));
    }

    private void validateRole(String role) {
        try {
            ProjectRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR,
                    "无效的角色: " + role + "，可选值: OWNER, MAINTAINER, DEVELOPER, VIEWER");
        }
    }

    private Map<Long, UserEntity> buildUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    private ProjectMemberResponse toMemberResponse(ProjectMemberEntity member, UserEntity user) {
        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setUserId(member.getUserId().toString());
        response.setRole(member.getRole());
        response.setStatus(member.getStatus());
        response.setJoinedTime(member.getJoinedTime() != null ? member.getJoinedTime().toString() : null);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }
}
