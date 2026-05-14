package com.aicoding.platform.member.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.domain.ProjectMemberEntity;
import com.aicoding.platform.member.domain.ProjectMemberStatus;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.member.infrastructure.ProjectMemberMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectPermissionService {

    private final ProjectMemberMapper projectMemberMapper;

    public ProjectPermissionService(ProjectMemberMapper projectMemberMapper) {
        this.projectMemberMapper = projectMemberMapper;
    }

    public void checkProjectMember(Long projectId) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        ProjectMemberEntity member = getActiveMember(projectId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    public void checkProjectRole(Long projectId, ProjectRole... requiredRoles) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        ProjectMemberEntity member = getActiveMember(projectId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        Set<String> allowedRoles = Arrays.stream(requiredRoles)
                .map(Enum::name)
                .collect(Collectors.toSet());
        if (!allowedRoles.contains(member.getRole())) {
            throw new BizException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    public String getCurrentMemberRole(Long projectId) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) {
            return null;
        }
        ProjectMemberEntity member = getActiveMember(projectId, userId);
        return member != null ? member.getRole() : null;
    }

    public List<Long> getUserProjectIds(Long userId) {
        List<ProjectMemberEntity> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getUserId, userId)
                        .eq(ProjectMemberEntity::getStatus, ProjectMemberStatus.ACTIVE.name()));
        return members.stream()
                .map(ProjectMemberEntity::getProjectId)
                .collect(Collectors.toList());
    }

    public LoginUser requireCurrentUser() {
        return LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }

    private ProjectMemberEntity getActiveMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getProjectId, projectId)
                        .eq(ProjectMemberEntity::getUserId, userId)
                        .eq(ProjectMemberEntity::getStatus, ProjectMemberStatus.ACTIVE.name()));
    }
}
