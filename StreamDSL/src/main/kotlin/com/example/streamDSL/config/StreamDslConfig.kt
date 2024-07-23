package com.example.streamDSL.config

import com.example.streamDSL.Pipeline
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import java.util.*

@Configuration
@EnableKafka
@EnableKafkaStreams
class StreamDslConfig {

    @Value(value = "\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapAddress: String

    @Bean
    fun streamConfig() = Properties().apply {
        put(APPLICATION_ID_CONFIG, "streams-app")
        put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress)
        put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
        put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde::class.java.name)
        put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "localhost:9091")
    }

    @Bean
    fun kafkaTopics(
        @Value("\${kafka.config.mainData}") mainData: String,
        @Value("\${kafka.config.additionalData}") additionalData: String,
        @Value("\${kafka.config.output}") output: String
    ) = KafkaTopics(mainData, additionalData, output)

    @Bean
    fun streamsBuilder() = StreamsBuilder()

    @Bean
    fun pipeline(kafkaTopics: KafkaTopics, streamsBuilder: StreamsBuilder, streamConfig: Properties) =
        Pipeline(kafkaTopics, streamsBuilder,streamConfig).buildStream()
}