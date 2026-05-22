package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.config.ReadOnlyToolProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class RepositoryToolSafetyService {

    private static final List<String> SENSITIVE_FILE_PATTERNS = List.of(
            ".env", "*.pem", "*.key", "*.p12", "*.jks",
            "id_rsa", "id_ed25519",
            ".git/",
            "node_modules/", "target/", "dist/", "logs/", "backups/", "diagnostics/"
    );

    private static final List<String> SENSITIVE_FILE_EXACT_SUFFIX = List.of(
            ".pem", ".key", ".p12", ".jks", ".cer", ".crt", ".der"
    );

    private final ReadOnlyToolProperties readOnlyToolProperties;

    public RepositoryToolSafetyService(ReadOnlyToolProperties readOnlyToolProperties) {
        this.readOnlyToolProperties = readOnlyToolProperties;
    }

    /**
     * Normalize a relative path: trim whitespace, convert backslashes,
     * remove leading slashes.
     */
    public String normalizeRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /**
     * Check if a path matches any sensitive file pattern.
     */
    public boolean isSensitivePath(String path) {
        if (path == null || path.isBlank()) return false;
        String normalized = normalizeRelativePath(path);
        // Check exact suffix patterns
        String lower = normalized.toLowerCase();
        for (String suffix : SENSITIVE_FILE_EXACT_SUFFIX) {
            if (lower.endsWith(suffix)) return true;
        }
        // Check contains patterns
        for (String pattern : SENSITIVE_FILE_PATTERNS) {
            if (pattern.contains("*")) {
                String prefix = pattern.replace("*", "").toLowerCase();
                if (lower.endsWith(prefix)) return true;
            } else if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that a path is a safe relative path.
     * Throws IllegalArgumentException if the path is invalid or sensitive.
     */
    public void validateSafeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }

        // Absolute path check
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("禁止使用绝对路径: " + path);
        }

        // Windows drive path (e.g., C:\)
        if (path.matches("^[A-Za-z]:\\\\.*")) {
            throw new IllegalArgumentException("禁止使用 Windows 驱动器路径: " + path);
        }

        // Contains ".."
        if (path.contains("..")) {
            throw new IllegalArgumentException("路径不能包含 '..': " + path);
        }

        // Contains "~"
        if (path.contains("~")) {
            throw new IllegalArgumentException("路径不能包含 '~': " + path);
        }

        // Null character
        if (path.contains("\0")) {
            throw new IllegalArgumentException("路径包含空字符");
        }

        // Sensitive path patterns
        if (isSensitivePath(path)) {
            throw new IllegalArgumentException("禁止读取敏感路径: " + path);
        }
    }

    /**
     * Validate that a relative path is within allowed prefixes.
     * Uses the configured allowPrefixes from ReadOnlyToolProperties.
     * If allowPrefixes is empty, all paths are allowed (subject to other checks).
     */
    public void validateAllowedPrefix(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        List<String> allowPrefixes = readOnlyToolProperties.getAllowPrefixes();
        if (allowPrefixes == null || allowPrefixes.isEmpty()) return;

        String normalized = normalizeRelativePath(relativePath);
        for (String prefix : allowPrefixes) {
            String normalizedPrefix = normalizeRelativePath(prefix);
            if (normalized.equals(normalizedPrefix) || normalized.startsWith(normalizedPrefix + "/")) {
                return;
            }
        }
        throw new IllegalArgumentException("路径不在允许的访问前缀范围内: " + relativePath);
    }

    /**
     * Resolve a relative path against a workspace root and verify it doesn't escape.
     * Returns the resolved absolute path if safe.
     * Throws IllegalArgumentException if the resolved path escapes the root.
     */
    public Path resolveAndValidatePath(Path root, String relativePath) {
        validateSafeRelativePath(relativePath);
        validateAllowedPrefix(relativePath);

        String normalized = normalizeRelativePath(relativePath);
        Path resolved = root.resolve(normalized).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IllegalArgumentException("路径越过了工作区根目录: " + relativePath);
        }
        return resolved;
    }
}
