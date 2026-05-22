package com.aicoding.platform.orchestration.worker;

import com.aicoding.platform.orchestration.domain.ToolExecutionErrorCode;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ToolExecutionRetryPolicy {

    private final ToolWorkerProperties properties;

    public ToolExecutionRetryPolicy(ToolWorkerProperties properties) {
        this.properties = properties;
    }

    /**
     * Check if the job can be automatically retried.
     */
    public boolean canRetry(ToolExecutionJobEntity job) {
        if (job == null) return false;

        String status = job.getStatus();
        if (ToolExecutionJobStatus.CANCELED.name().equals(status)) return false;
        if (ToolExecutionJobStatus.DEAD_LETTERED.name().equals(status)) return false;

        if (job.getRetryCount() == null || job.getMaxRetryCount() == null) return false;
        if (job.getRetryCount() >= job.getMaxRetryCount()) return false;

        // Certain error codes should not auto-retry
        String errorCode = job.getErrorCode();
        if (errorCode != null) {
            if (ToolExecutionErrorCode.POLICY_BLOCKED.name().equals(errorCode)) return false;
            if (ToolExecutionErrorCode.APPROVAL_REQUIRED.name().equals(errorCode)) return false;
            if (ToolExecutionErrorCode.MESSAGE_INVALID.name().equals(errorCode)) return false;
            if (ToolExecutionErrorCode.JOB_CANCELED.name().equals(errorCode)) return false;
        }

        return true;
    }

    /**
     * Calculate the next retry time based on current retry count.
     */
    public LocalDateTime nextRetryAt(ToolExecutionJobEntity job) {
        long delaySeconds = nextDelaySeconds(job);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * Get the delay in seconds for the next retry based on current retry count.
     */
    public long nextDelaySeconds(ToolExecutionJobEntity job) {
        if (job == null || job.getRetryCount() == null) return 5;

        List<Integer> delays = properties.getRetryDelaysSeconds();
        int idx = job.getRetryCount();
        if (idx < 0) idx = 0;
        if (idx >= delays.size()) {
            return delays.get(delays.size() - 1);
        }
        return delays.get(idx);
    }
}
