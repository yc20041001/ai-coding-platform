package com.aicoding.platform.auth.application;

import com.aicoding.platform.auth.domain.RoleEntity;
import com.aicoding.platform.auth.domain.RolePermissionEntity;
import com.aicoding.platform.auth.domain.UserEntity;
import com.aicoding.platform.auth.domain.UserRoleEntity;
import com.aicoding.platform.auth.domain.UserStatus;
import com.aicoding.platform.auth.domain.PermissionEntity;
import com.aicoding.platform.auth.dto.CurrentUserResponse;
import com.aicoding.platform.auth.dto.LoginRequest;
import com.aicoding.platform.auth.dto.LoginResponse;
import com.aicoding.platform.auth.dto.RefreshTokenRequest;
import com.aicoding.platform.auth.infrastructure.PermissionMapper;
import com.aicoding.platform.auth.infrastructure.RoleMapper;
import com.aicoding.platform.auth.infrastructure.RolePermissionMapper;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.auth.infrastructure.UserRoleMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.security.JwtTokenProvider;
import com.aicoding.platform.security.config.JwtProperties;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthApplicationService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;

    public AuthApplicationService(UserMapper userMapper,
                                  RoleMapper roleMapper,
                                  PermissionMapper permissionMapper,
                                  UserRoleMapper userRoleMapper,
                                  RolePermissionMapper rolePermissionMapper,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider,
                                  JwtProperties jwtProperties,
                                  CaptchaService captchaService,
                                  LoginAttemptService loginAttemptService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.captchaService = captchaService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String ip = loginAttemptService.currentClientIp();

        loginAttemptService.checkLocked(email, ip);
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());

        try {
            UserEntity user = userMapper.selectOne(
                    new LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getEmail, email));
            if (user == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
            }

            if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用或锁定");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
            }

            List<String> roles = getUserRoles(user.getId());

            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            loginAttemptService.recordSuccess(email, ip);

            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setExpiresIn(jwtProperties.getAccessTokenExpireSeconds());

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setEmail(user.getEmail());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setRoles(roles);
            response.setUser(userInfo);

            return response;
        } catch (BizException ex) {
            if (ErrorCode.UNAUTHORIZED.equals(ex.getErrorCode()) ||
                ErrorCode.FORBIDDEN.equals(ex.getErrorCode())) {
                loginAttemptService.recordFailure(email, ip);
            }
            throw ex;
        }
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请使用 refresh token 刷新，access token 不能用于刷新接口");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用或锁定");
        }

        List<String> roles = getUserRoles(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(jwtProperties.getAccessTokenExpireSeconds());
        return response;
    }

    public void logout() {
        // 无状态 JWT: logout 由客户端清除 token 即可。
        // 后续引入 Redis 后可在此处添加 token 黑名单逻辑。
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        LoginUser loginUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        UserEntity user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        List<String> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(user.getId());

        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }

    private List<String> getUserRoles(Long userId) {
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());

        return roleMapper.selectBatchIds(roleIds).stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toList());
    }

    private List<String> getUserPermissions(Long userId) {
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());

        List<RolePermissionEntity> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>()
                        .in(RolePermissionEntity::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toList());
    }
}
