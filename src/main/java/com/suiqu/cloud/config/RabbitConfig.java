package com.suiqu.cloud.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String SYNC_QUEUE = "file.sync.queue";
    public static final String SYNC_EXCHANGE = "file.sync.exchange";

    @Bean
    public Queue syncQueue() { return new Queue(SYNC_QUEUE); }

    @Bean
    public TopicExchange syncExchange() { return new TopicExchange(SYNC_EXCHANGE); }

    @Bean
    public Binding binding(Queue syncQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange).with("file.sync.key");
    }
}