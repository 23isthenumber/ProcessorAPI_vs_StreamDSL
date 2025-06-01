package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;

@TestConfiguration
public class TestConfig {

    @Bean
    public Properties config(Properties streamConfig) {
        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streamConfig.get(BOOTSTRAP_SERVERS_CONFIG),
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class,
                        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        streamConfig.get(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
                )
        );
        return properties;
    }

    @Bean
    public Properties producerConfig(Properties streamConfig) {
        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streamConfig.get(BOOTSTRAP_SERVERS_CONFIG),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        streamConfig.get(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG),
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true",
                        ProducerConfig.ACKS_CONFIG, "all"
                )
        );
        return properties;
    }

    @Bean
    public KafkaConsumer<String, JoinedData> outputConsumer(Properties config){
        config.put(
                ConsumerConfig.GROUP_ID_CONFIG, "outputConsumerGroup"
        );
        config.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
        );
        config.put(
                ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"
        );
        return new KafkaConsumer<>(config);
    }

    @Bean
    public KafkaProducer<String, MainData> mainDataProducer(Properties producerConfig){
        producerConfig.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transactional-id-1");
        KafkaProducer<String, MainData> producer = new KafkaProducer<>(producerConfig);
        producer.initTransactions();
        return producer;
    }

    @Bean
    public KafkaProducer<String, AdditionalData> additionalDataProducerProperties(Properties producerConfig){
        producerConfig.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transactional-id-2");
        KafkaProducer<String, AdditionalData> producer = new KafkaProducer<>(producerConfig);
        producer.initTransactions();
        return producer;
    }
}
