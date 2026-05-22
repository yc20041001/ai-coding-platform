package com.aicoding.platform.orchestration.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.tool-worker")
public class ToolWorkerProperties {

    private String mode = "SYNC_MOCK";
    private boolean queueEnabled = false;
    private boolean workerEnabled = false;
    private String exchange = "tool.execution.exchange";
    private String queue = "tool.execution.queue";
    private String routingKey = "tool.execution.run";
    private String deadLetterExchange = "tool.execution.dlx";
    private String deadLetterQueue = "tool.execution.dlq";
    private String deadLetterRoutingKey = "tool.execution.dead";
    private int maxRetryCount = 2;
    private long pollIntervalMs = 1500;
    private List<Integer> retryDelaysSeconds = List.of(5, 30, 120);
    private long runningTimeoutSeconds = 300;

    public ToolWorkerProperties() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public boolean isQueueEnabled() { return queueEnabled; }
    public void setQueueEnabled(boolean queueEnabled) { this.queueEnabled = queueEnabled; }

    public boolean isWorkerEnabled() { return workerEnabled; }
    public void setWorkerEnabled(boolean workerEnabled) { this.workerEnabled = workerEnabled; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getQueue() { return queue; }
    public void setQueue(String queue) { this.queue = queue; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }

    public String getDeadLetterQueue() { return deadLetterQueue; }
    public void setDeadLetterQueue(String deadLetterQueue) { this.deadLetterQueue = deadLetterQueue; }

    public String getDeadLetterRoutingKey() { return deadLetterRoutingKey; }
    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) { this.deadLetterRoutingKey = deadLetterRoutingKey; }

    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public List<Integer> getRetryDelaysSeconds() { return retryDelaysSeconds; }
    public void setRetryDelaysSeconds(List<Integer> retryDelaysSeconds) { this.retryDelaysSeconds = retryDelaysSeconds; }

    public long getRunningTimeoutSeconds() { return runningTimeoutSeconds; }
    public void setRunningTimeoutSeconds(long runningTimeoutSeconds) { this.runningTimeoutSeconds = runningTimeoutSeconds; }

    public boolean isAsyncMode() {
        return "ASYNC_RABBITMQ".equalsIgnoreCase(mode);
    }
}
