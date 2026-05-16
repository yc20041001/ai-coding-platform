package com.aicoding.platform.security;

import com.aicoding.platform.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = newProvider();

    private static JwtTokenProvider newProvider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-provider-unit-tests-32bytes");
        properties.setAccessTokenExpireSeconds(7200);
        properties.setRefreshTokenExpireSeconds(604800);
        return new JwtTokenProvider(properties);
    }

    // ---- Access token generation ----

    @Test
    void shouldGenerateAccessTokenWithTypeAccess() {
        String token = provider.generateAccessToken(1L, "testuser", List.of("ADMIN"));
        assertThat(token).isNotEmpty();

        Claims claims = provider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
    }

    @Test
    void shouldGenerateAccessTokenWithRoles() {
        String token = provider.generateAccessToken(42L, "dev", List.of("DEVELOPER", "VIEWER"));
        List<String> roles = provider.getRoles(token);
        assertThat(roles).containsExactly("DEVELOPER", "VIEWER");
    }

    @Test
    void shouldGenerateRefreshTokenWithTypeRefresh() {
        String token = provider.generateRefreshToken(1L);
        assertThat(token).isNotEmpty();

        Claims claims = provider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    void shouldNotIncludeRolesInRefreshToken() {
        String token = provider.generateRefreshToken(1L);
        List<String> roles = provider.getRoles(token);
        assertThat(roles).isNull();
    }

    @Test
    void shouldNotIncludeUsernameInRefreshToken() {
        String token = provider.generateRefreshToken(1L);
        String username = provider.getUsername(token);
        assertThat(username).isNull();
    }

    // ---- Token type discrimination ----

    @Test
    void shouldIdentifyAccessToken() {
        String token = provider.generateAccessToken(1L, "user", List.of("VIEWER"));
        assertTrue(provider.isAccessToken(token));
        assertFalse(provider.isRefreshToken(token));
        assertThat(provider.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void shouldIdentifyRefreshToken() {
        String token = provider.generateRefreshToken(1L);
        assertTrue(provider.isRefreshToken(token));
        assertFalse(provider.isAccessToken(token));
        assertThat(provider.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void shouldReturnNullTokenTypeForInvalidToken() {
        // Token with no type claim would throw before reaching getTokenType
        // Test that tampered tokens are rejected
        String validToken = provider.generateAccessToken(1L, "user", List.of("VIEWER"));
        String tamperedToken = validToken.substring(0, validToken.length() - 1);
        JwtException thrown = assertThrows(JwtException.class, () -> provider.getTokenType(tamperedToken));
        assertThat(thrown).isNotNull();
    }

    // ---- Token validation ----

    @Test
    void shouldValidateValidToken() {
        String token = provider.generateAccessToken(1L, "user", List.of("VIEWER"));
        assertTrue(provider.validateToken(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        String validToken = provider.generateAccessToken(1L, "user", List.of("VIEWER"));
        String tamperedToken = validToken.substring(0, validToken.length() - 3) + "xyz";
        assertFalse(provider.validateToken(tamperedToken));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("other-secret-key-for-testing-different-signature");
        otherProperties.setAccessTokenExpireSeconds(7200);
        otherProperties.setRefreshTokenExpireSeconds(604800);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);

        String token = otherProvider.generateAccessToken(1L, "user", List.of("VIEWER"));
        assertFalse(provider.validateToken(token));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertFalse(provider.validateToken("not-a-valid-jwt-token-at-all"));
        assertFalse(provider.validateToken("header.payload.signature"));
    }

    @Test
    void shouldThrowOnEmptyToken() {
        // jjwt throws IllegalArgumentException for null/empty input
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> provider.validateToken(""));
        assertThat(thrown).isNotNull();
    }

    // ---- User ID extraction ----

    @Test
    void shouldExtractUserId() {
        String token = provider.generateAccessToken(99L, "user99", List.of("VIEWER"));
        assertThat(provider.getUserId(token)).isEqualTo(99L);
    }

    // ---- Username extraction ----

    @Test
    void shouldExtractUsername() {
        String token = provider.generateAccessToken(1L, "alice", List.of("ADMIN"));
        assertThat(provider.getUsername(token)).isEqualTo("alice");
    }
}
