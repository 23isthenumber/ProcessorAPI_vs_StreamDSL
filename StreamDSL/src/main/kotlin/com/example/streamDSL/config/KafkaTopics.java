package com.example.streamDSL.config;

public record KafkaTopics(
        String mainData,
        String additionalData,
        String output
){}
