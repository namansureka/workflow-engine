package com.naman.workflow_engine.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String WORKFLOW_QUEUE = "workflow.queue";
    public static final String DLQ="dead-letter.queue";
    public static final String DELAY_QUEUE = "delay.queue";


    @Bean
    public Queue workflowQueue() {
        return new Queue(WORKFLOW_QUEUE, true);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ, true);
    }

    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");  // default exchange
        args.put("x-dead-letter-routing-key", WORKFLOW_QUEUE);  // route to main queue
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}