package com.processorapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topic")
public record KafkaTopics(
        String mainData,
        String additionalData,
        String output
){}
