package com.aicoding.platform.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.read-only-tools")
public class ReadOnlyToolProperties {

    private long maxFileBytes = 128 * 1024;

    private long maxOutputBytes = 256 * 1024;

    private int maxFiles = 200;

    private boolean redactionEnabled = true;

    private List<String> allowPrefixes = List.of(
            "backend/src", "frontend/src", "docs", "scripts", "deploy", ".github/workflows"
    );

    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long maxFileBytes) { this.maxFileBytes = maxFileBytes; }

    public long getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(long maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }

    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }

    public boolean isRedactionEnabled() { return redactionEnabled; }
    public void setRedactionEnabled(boolean redactionEnabled) { this.redactionEnabled = redactionEnabled; }

    public List<String> getAllowPrefixes() { return allowPrefixes; }
    public void setAllowPrefixes(List<String> allowPrefixes) { this.allowPrefixes = allowPrefixes; }
}
