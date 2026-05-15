package com.aicoding.platform.modelgateway.application;

import org.springframework.stereotype.Service;

/**
 * Masks API keys and sensitive values for logging and API responses.
 * Never returns plaintext keys to the frontend.
 */
@Service
public class ModelSecretMaskingService {

    /**
     * Mask an API key for display.
     * Length <= 8: all asterisks
     * Length > 8: first 3 + **** + last 4
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "<empty>";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    /**
     * Sanitize a string for logging - removes known key patterns.
     */
    public String sanitizeForLog(String text) {
        if (text == null) return null;
        return text
                .replaceAll("(?i)(Bearer\\s+)([a-zA-Z0-9_\\-]{20,})", "$1****")
                .replaceAll("(?i)(api[_-]?key[=:]\"?)([^\"&\\s]{8,})(\"?)", "$1****$3")
                .replaceAll("(?i)(sk-[a-zA-Z0-9]{20,})", "sk-****");
    }

    /**
     * Returns a masked representation that includes the provider hint.
     */
    public String maskWithLabel(String provider, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return provider + ":<not-set>";
        }
        return provider + ":" + mask(apiKey);
    }
}
