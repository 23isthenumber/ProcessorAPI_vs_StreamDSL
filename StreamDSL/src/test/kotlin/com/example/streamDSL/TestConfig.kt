package com.example.streamDSL

import com.streamdsl.avro.AdditionalData
import com.streamdsl.avro.JoinedData
import com.streamdsl.avro.MainData
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.*

@TestConfiguration
class TestConfig {

    @Bean
    fun consumerConfig(streamConfig: Properties) =
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streamConfig[BOOTSTRAP_SERVERS_CONFIG])
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                streamConfig[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG]
            )
        }

    @Bean
    fun producerConfig(streamConfig: Properties) =
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streamConfig[BOOTSTRAP_SERVERS_CONFIG])
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                streamConfig[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG]
            )
        }

    @Bean
    fun outputConsumer(consumerConfig: Properties) =
        KafkaConsumer<String, JoinedData>(consumerConfig.apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "outputConsumerGroup")
        })

    @Bean
    fun mainDataProducer(producerConfig: Properties) =
        KafkaProducer<String, MainData>(
            producerConfig
        )

    @Bean
    fun additionalDataProducer(producerConfig: Properties) =
        KafkaProducer<String, AdditionalData>(
            producerConfig
        )
}