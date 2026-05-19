package com.aicoding.platform.common.redis;

public final class RedisKeys {
    private static final String PREFIX = "ai-coding-platform:";

    private RedisKeys() {
    }

    public static String captcha(String captchaId) {
        return PREFIX + "auth:captcha:" + captchaId;
    }

    public static String loginFailEmail(String email) {
        return PREFIX + "auth:login:fail:email:" + email.toLowerCase().trim();
    }

    public static String loginFailIp(String ip) {
        return PREFIX + "auth:login:fail:ip:" + ip;
    }

    public static String loginLockEmail(String email) {
        return PREFIX + "auth:login:lock:email:" + email.toLowerCase().trim();
    }

    public static String loginLockIp(String ip) {
        return PREFIX + "auth:login:lock:ip:" + ip;
    }
}