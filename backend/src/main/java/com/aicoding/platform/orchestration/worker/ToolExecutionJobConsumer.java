package com.aicoding.platform.orchestration.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tool-worker.worker-enabled", havingValue = "true")
public class ToolExecutionJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionJobConsumer.class);

    private final ToolExecutionWorkerService workerService;

    public ToolExecutionJobConsumer(ToolExecutionWorkerService workerService) {
        this.workerService = workerService;
    }

    @RabbitListener(queues = "${app.tool-worker.queue}",
                    autoStartup = "${app.tool-worker.worker-enabled:false}")
    public void consume(ToolExecutionJobMessage message) {
        if (message == null || message.getJobId() == null) {
            log.warn("Received null or invalid job message");
            return;
        }

        log.info("Consumer received job message: jobId={}, toolKey={}",
                message.getJobId(), message.getToolKey());

        try {
            Long jobId = Long.valueOf(message.getJobId());
            workerService.process(jobId);
        } catch (NumberFormatException e) {
            log.error("Invalid jobId format in message: {}", message.getJobId());
            // Don't requeue invalid messages
        } catch (Exception e) {
            log.error("Error processing job {}: {}", message.getJobId(), e.getMessage());
            // Don't requeue - retry is handled at DB level (RETRY_PENDING / DEAD_LETTERED)
            // The worker has already updated the job status in the database
        }
    }
}
