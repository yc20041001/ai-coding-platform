package com.aicoding.platform.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class LoginUserContext {

    private LoginUserContext() {
    }

    public static Optional<LoginUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }
        return Optional.empty();
    }

    public static Long currentUserId() {
        return currentUser()
                .map(LoginUser::getUserId)
                .orElse(null);
    }
}
