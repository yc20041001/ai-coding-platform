package com.aicoding.platform.member.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.member.application.ProjectMemberApplicationService;
import com.aicoding.platform.member.dto.InviteProjectMemberRequest;
import com.aicoding.platform.member.dto.InviteProjectMemberResponse;
import com.aicoding.platform.member.dto.ProjectMemberResponse;
import com.aicoding.platform.member.dto.UpdateProjectMemberRoleRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberApplicationService projectMemberApplicationService;

    public ProjectMemberController(ProjectMemberApplicationService projectMemberApplicationService) {
        this.projectMemberApplicationService = projectMemberApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectMemberResponse>> list(@PathVariable Long projectId,
                                                                @Valid PageQuery pageQuery) {
        return ApiResponse.ok(projectMemberApplicationService.getMembers(projectId, pageQuery));
    }

    @PostMapping
    public ApiResponse<InviteProjectMemberResponse> invite(@PathVariable Long projectId,
                                                            @Valid @RequestBody InviteProjectMemberRequest request) {
        return ApiResponse.ok(projectMemberApplicationService.inviteMember(projectId, request));
    }

    @PutMapping("/{userId}/role")
    public ApiResponse<Boolean> updateRole(@PathVariable Long projectId,
                                            @PathVariable Long userId,
                                            @Valid @RequestBody UpdateProjectMemberRoleRequest request) {
        return ApiResponse.ok(projectMemberApplicationService.updateMemberRole(
                projectId, userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Boolean> remove(@PathVariable Long projectId,
                                        @PathVariable Long userId) {
        return ApiResponse.ok(projectMemberApplicationService.removeMember(
                projectId, userId));
    }
}
