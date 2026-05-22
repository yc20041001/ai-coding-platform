package com.aicoding.platform.orchestration.worker;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ToolExecutionQueueConfig {

    private final ToolWorkerProperties properties;

    public ToolExecutionQueueConfig(ToolWorkerProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public DirectExchange toolExecutionExchange() {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public DirectExchange toolExecutionDeadLetterExchange() {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public Queue toolExecutionQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", properties.getDeadLetterExchange());
        args.put("x-dead-letter-routing-key", properties.getDeadLetterRoutingKey());
        return new Queue(properties.getQueue(), true, false, false, args);
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public Queue toolExecutionDeadLetterQueue() {
        return new Queue(properties.getDeadLetterQueue(), true);
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public Binding toolExecutionBinding(DirectExchange toolExecutionExchange, Queue toolExecutionQueue) {
        return BindingBuilder.bind(toolExecutionQueue)
                .to(toolExecutionExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    @ConditionalOnProperty(name = "app.tool-worker.queue-enabled", havingValue = "true")
    public Binding toolExecutionDeadLetterBinding(DirectExchange toolExecutionDeadLetterExchange,
                                                   Queue toolExecutionDeadLetterQueue) {
        return BindingBuilder.bind(toolExecutionDeadLetterQueue)
                .to(toolExecutionDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @SuppressWarnings("null")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
