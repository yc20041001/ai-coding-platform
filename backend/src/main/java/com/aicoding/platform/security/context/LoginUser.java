package com.aicoding.platform.security.context;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final Set<String> roles;
    private final Set<String> permissions;

    public LoginUser(Long userId, String username, String email, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        this.permissions = permissions != null ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPermissions() { return permissions; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return null; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
