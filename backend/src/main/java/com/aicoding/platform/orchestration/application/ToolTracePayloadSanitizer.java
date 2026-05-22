package com.aicoding.platform.orchestration.application;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ToolTracePayloadSanitizer {

    private static final Pattern[] SECRET_PATTERNS = {
        Pattern.compile("sk-[A-Za-z0-9]{20,}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ghp_[A-Za-z0-9]{36,}"),
        Pattern.compile("github_pat_[A-Za-z0-9_]{30,}"),
        Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/-]{20,}"),
        Pattern.compile("(api[_-]?key\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(secret\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(password\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
        Pattern.compile("-----BEGIN[\\s\\S]*?PRIVATE KEY-----"),
    };

    private static final String MASK = "**MASKED_BY_TRACE**";
    private static final int MAX_PAYLOAD_LENGTH = 64 * 1024;

    public String sanitize(String payload) {
        if (payload == null || payload.isBlank()) return payload;

        String result = payload;
        for (Pattern pattern : SECRET_PATTERNS) {
            result = pattern.matcher(result).replaceAll(MASK);
        }

        if (result.length() > MAX_PAYLOAD_LENGTH) {
            result = result.substring(0, MAX_PAYLOAD_LENGTH);
        }

        return result;
    }

    public boolean isTruncated(String payload) {
        return payload != null && payload.length() > MAX_PAYLOAD_LENGTH;
    }
}
