package com.aicoding.platform.orchestration.worker;

import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ToolExecutionJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionJobPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ToolWorkerProperties properties;

    @Autowired
    public ToolExecutionJobPublisher(RabbitTemplate rabbitTemplate, ToolWorkerProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(ToolExecutionJobEntity job) {
        if (!properties.isQueueEnabled()) {
            log.debug("Queue disabled, skipping publish for jobId={}", job.getId());
            return;
        }

        ToolExecutionJobMessage message = new ToolExecutionJobMessage(
                job.getId().toString(),
                job.getToolExecutionId() != null ? job.getToolExecutionId().toString() : null,
                job.getProjectId() != null ? job.getProjectId().toString() : null,
                job.getTaskId() != null ? job.getTaskId().toString() : null,
                job.getRunId() != null ? job.getRunId().toString() : null,
                job.getStepId() != null ? job.getStepId().toString() : null,
                job.getToolKey(),
                LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getRoutingKey(),
                    message);
            log.info("Published job message: jobId={}, toolKey={}", job.getId(), job.getToolKey());
        } catch (org.springframework.amqp.AmqpException e) {
            log.error("Failed to publish job message for jobId={}: {}", job.getId(), e.getMessage());
            throw new RuntimeException("Failed to publish tool execution job message", e);
        }
    }
}
