package com.example.streamDSL.config

data class KafkaTopics(
    val mainData: String,
    val additionalData: String,
    val output: String
)