package com.aicoding.platform.auth.application;

import com.aicoding.platform.auth.config.LoginProtectionProperties;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.redis.RedisKeys;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final LoginProtectionProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, AtomicInteger> memoryFailCount = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginProtectionProperties properties,
                               Optional<StringRedisTemplate> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate.orElse(null);
    }

    public void checkLocked(String email, String ip) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);

        boolean emailLocked = checkRedisOrMemory(
                RedisKeys.loginLockEmail(normalizedEmail),
                () -> checkMemoryLock("lock:email:" + normalizedEmail)
        );

        boolean ipLocked = checkRedisOrMemory(
                RedisKeys.loginLockIp(ip),
                () -> checkMemoryLock("lock:ip:" + ip)
        );

        if (emailLocked || ipLocked) {
            throw new BizException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS, "登录尝试次数过多，请稍后再试");
        }
    }

    private boolean checkMemoryLock(String key) {
        return memoryFailCount.containsKey(key);
    }

    private boolean checkRedisOrMemory(String redisKey, java.util.function.Supplier<Boolean> memoryFallback) {
        try {
            StringRedisTemplate template = redisTemplate;
            if (isRedisAvailable(template)) {
                return Boolean.TRUE.equals(template.hasKey(required(redisKey)));
            }
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, falling back to memory: {}", e.getMessage());
        }
        return memoryFallback.get();
    }

    public void recordFailure(String email, String ip) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);

        StringRedisTemplate template = redisTemplate;
        if (isRedisAvailable(template)) {
            recordFailureRedis(template, normalizedEmail, ip);
        } else {
            recordFailureMemory(normalizedEmail, ip);
        }
    }

    private void recordFailureRedis(StringRedisTemplate template, String email, String ip) {
        try {
            String emailFailKey = RedisKeys.loginFailEmail(email);
            String ipFailKey = RedisKeys.loginFailIp(ip);
            String emailLockKey = RedisKeys.loginLockEmail(email);
            String ipLockKey = RedisKeys.loginLockIp(ip);

            template.opsForValue().increment(required(emailFailKey));
            template.expire(required(emailFailKey), seconds(properties.getFailureWindowSeconds()));

            template.opsForValue().increment(required(ipFailKey));
            template.expire(required(ipFailKey), seconds(properties.getFailureWindowSeconds()));

            Long emailFails = getRedisValue(template, emailFailKey);
            Long ipFails = getRedisValue(template, ipFailKey);

            if (emailFails != null && emailFails >= properties.getMaxEmailFailures()) {
                template.opsForValue().set(required(emailLockKey), required("1"),
                        seconds(properties.getLockSeconds()));
            }

            if (ipFails != null && ipFails >= properties.getMaxIpFailures()) {
                template.opsForValue().set(required(ipLockKey), required("1"),
                        seconds(properties.getLockSeconds()));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to record failure to Redis, falling back to memory: {}", e.getMessage());
            recordFailureMemory(email, ip);
        }
    }

    private void recordFailureMemory(String email, String ip) {
        String emailFailKey = "fail:email:" + email;
        String ipFailKey = "fail:ip:" + ip;

        memoryFailCount.computeIfAbsent(emailFailKey, k -> new AtomicInteger(0)).incrementAndGet();
        memoryFailCount.computeIfAbsent(ipFailKey, k -> new AtomicInteger(0)).incrementAndGet();

        AtomicInteger emailCount = memoryFailCount.get(emailFailKey);
        AtomicInteger ipCount = memoryFailCount.get(ipFailKey);

        if (emailCount.get() >= properties.getMaxEmailFailures()) {
            memoryFailCount.put("lock:email:" + email, new AtomicInteger(1));
        }

        if (ipCount.get() >= properties.getMaxIpFailures()) {
            memoryFailCount.put("lock:ip:" + ip, new AtomicInteger(1));
        }
    }

    public void recordSuccess(String email, String ip) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);

        StringRedisTemplate template = redisTemplate;
        if (isRedisAvailable(template)) {
            try {
                template.delete(required(RedisKeys.loginFailEmail(normalizedEmail)));
                template.delete(required(RedisKeys.loginFailIp(ip)));
            } catch (RuntimeException e) {
                log.warn("Failed to clear failure count from Redis: {}", e.getMessage());
            }
        } else {
            memoryFailCount.remove("fail:email:" + normalizedEmail);
            memoryFailCount.remove("fail:ip:" + ip);
        }
    }

    public String currentClientIp() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (RuntimeException e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return "unknown";
    }

    private boolean isRedisAvailable(StringRedisTemplate template) {
        if (template == null) {
            return false;
        }
        try {
            RedisConnectionFactory connectionFactory = template.getConnectionFactory();
            if (connectionFactory != null) {
                connectionFactory.getConnection().ping();
                return true;
            }
        } catch (RuntimeException e) {
            log.debug("Redis not available: {}", e.getMessage());
        }
        return false;
    }

    private Long getRedisValue(StringRedisTemplate template, String key) {
        String value = template.opsForValue().get(required(key));
        if (value != null) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return email != null ? email.toLowerCase().trim() : "";
    }

    private HttpServletRequest getCurrentRequest() {
        org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
        try {
            return ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                    .getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private @NonNull String required(String value) {
        return Objects.requireNonNull(value);
    }

    private @NonNull Duration seconds(long value) {
        return Objects.requireNonNull(Duration.ofSeconds(value));
    }
}
