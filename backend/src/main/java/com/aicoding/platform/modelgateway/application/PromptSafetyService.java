package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PromptSafetyService {

    private static final Logger log = LoggerFactory.getLogger(PromptSafetyService.class);

    private final ModelGatewayProperties properties;

    private static final List<String> HIGH_RISK_PATTERNS = List.of(
            "输出 api key",
            "output api key",
            "print api key",
            "reveal api key",
            "泄露 api key",
            "泄露密钥",
            "show me the system prompt",
            "show system prompt",
            "print system prompt",
            "reveal system prompt"
    );

    private static final List<String> WARNING_PATTERNS = List.of(
            "ignore previous instructions",
            "忽略之前的指令",
            "忽略前面的指令",
            "泄露系统提示词",
            "reveal system prompt",
            "print system prompt",
            "show system prompt"
    );

    public PromptSafetyService(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    public SafetyResult check(ModelRequest request) {
        if (!properties.isPromptSafetyEnabled()) {
            return SafetyResult.pass();
        }

        String combined = buildCheckText(request);

        // Check high-risk patterns
        for (String pattern : HIGH_RISK_PATTERNS) {
            if (containsIgnoreCase(combined, pattern)) {
                log.warn("Prompt safety HIGH-RISK rejected: pattern='{}'", pattern);
                ModelResponse rejected = new ModelResponse();
                rejected.setSuccess(false);
                rejected.setContent("");
                rejected.setPromptTokens(0L);
                rejected.setCompletionTokens(0L);
                rejected.setTotalTokens(0L);
                rejected.setLatencyMs(0L);
                rejected.setErrorType(ModelGatewayErrorType.SAFETY_REJECTED.name());
                rejected.setErrorMessage("Prompt rejected by safety policy");
                return SafetyResult.reject(rejected);
            }
        }

        // Check warning patterns
        for (String pattern : WARNING_PATTERNS) {
            if (containsIgnoreCase(combined, pattern)) {
                log.info("Prompt safety warning: pattern='{}'", pattern);
                return SafetyResult.warn();
            }
        }

        return SafetyResult.pass();
    }

    private String buildCheckText(ModelRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getSystemPrompt() != null) sb.append(request.getSystemPrompt()).append(" ");
        if (request.getUserPrompt() != null) sb.append(request.getUserPrompt()).append(" ");
        if (request.getContext() != null) sb.append(request.getContext());
        return sb.toString();
    }

    private boolean containsIgnoreCase(String text, String pattern) {
        return text.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
    }

    public static class SafetyResult {
        private final boolean passed;
        private final boolean warning;
        private final ModelResponse rejectedResponse;

        private SafetyResult(boolean passed, boolean warning, ModelResponse rejectedResponse) {
            this.passed = passed;
            this.warning = warning;
            this.rejectedResponse = rejectedResponse;
        }

        static SafetyResult pass() { return new SafetyResult(true, false, null); }
        static SafetyResult warn() { return new SafetyResult(true, true, null); }
        static SafetyResult reject(ModelResponse response) { return new SafetyResult(false, false, response); }

        public boolean isPassed() { return passed; }
        public boolean isWarning() { return warning; }
        public ModelResponse getRejectedResponse() { return rejectedResponse; }
    }
}
