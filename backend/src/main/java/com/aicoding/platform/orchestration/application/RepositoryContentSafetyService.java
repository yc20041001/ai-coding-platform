package com.aicoding.platform.orchestration.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class RepositoryContentSafetyService {

    private static final List<String> BINARY_EXTENSIONS = List.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg",
            ".woff", ".woff2", ".ttf", ".eot",
            ".zip", ".jar", ".war", ".tar", ".gz", ".7z", ".rar",
            ".class", ".o", ".so", ".dll", ".dylib",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".mp4", ".avi", ".mov", ".wmv", ".flv",
            ".pyc", ".pyo",
            ".exe", ".bin", ".dat", ".db", ".sqlite",
            ".ttf", ".otf"
    );

    private static final long DEFAULT_MAX_FILE_BYTES = 128 * 1024;

    private static final long DEFAULT_MAX_OUTPUT_BYTES = 256 * 1024;

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("sk-[A-Za-z0-9]{20,}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ghp_[A-Za-z0-9]{36,}"),
            Pattern.compile("github_pat_[A-Za-z0-9_]{30,}"),
            Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/-]{20,}"),
            Pattern.compile("(api[_-]?key\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(secret\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(password\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(token\\s*[=:]\\s*['\"])[^'\"]+(['\"])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}")
    );

    private static final String REDACTION_REPLACEMENT = "**REDACTED**";

    /**
     * Detect binary content by scanning for NUL bytes in the first 4096 bytes.
     */
    public boolean isBinaryContent(byte[] content) {
        if (content == null || content.length == 0) return false;
        int checkLen = Math.min(content.length, 4096);
        for (int i = 0; i < checkLen; i++) {
            if (content[i] == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect binary files by extension.
     */
    public boolean isBinaryExtension(String filePath) {
        if (filePath == null || filePath.isBlank()) return false;
        String lower = filePath.toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    public boolean isFileSizeWithinLimit(long fileSize) {
        return fileSize <= DEFAULT_MAX_FILE_BYTES;
    }

    public boolean isFileSizeWithinLimit(long fileSize, long maxFileBytes) {
        return fileSize <= maxFileBytes;
    }

    /**
     * Redact secrets from content. Returns redacted content.
     */
    public String redactSecrets(String content) {
        if (content == null || content.isBlank()) return content;
        String result = content;
        for (Pattern pattern : SECRET_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    matcher.appendReplacement(sb, matcher.group(1) + REDACTION_REPLACEMENT + matcher.group(2));
                } else {
                    matcher.appendReplacement(sb, REDACTION_REPLACEMENT);
                }
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    /**
     * Redact secrets from content and return count of redactions.
     */
    public RedactionResult redactSecretsWithCount(String content) {
        if (content == null || content.isBlank()) {
            return new RedactionResult(content, 0);
        }
        String result = content;
        int totalCount = 0;
        for (Pattern pattern : SECRET_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    matcher.appendReplacement(sb, matcher.group(1) + REDACTION_REPLACEMENT + matcher.group(2));
                } else {
                    matcher.appendReplacement(sb, REDACTION_REPLACEMENT);
                }
                totalCount++;
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return new RedactionResult(result, totalCount);
    }

    public boolean isOutputSizeWithinLimit(String content) {
        if (content == null) return true;
        return content.length() <= DEFAULT_MAX_OUTPUT_BYTES;
    }

    public boolean isOutputSizeWithinLimit(String content, long maxOutputBytes) {
        if (content == null) return true;
        return content.length() <= maxOutputBytes;
    }

    public long getDefaultMaxFileBytes() {
        return DEFAULT_MAX_FILE_BYTES;
    }

    public long getDefaultMaxOutputBytes() {
        return DEFAULT_MAX_OUTPUT_BYTES;
    }

    public static class RedactionResult {
        private final String content;
        private final int redactionCount;

        public RedactionResult(String content, int redactionCount) {
            this.content = content;
            this.redactionCount = redactionCount;
        }

        public String getContent() { return content; }
        public int getRedactionCount() { return redactionCount; }
    }
}
