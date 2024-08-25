package com.stream_dsl.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;

@Configuration
@EnableKafka
public class StreamsDslConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;
    @Value("${spring.kafka.schema-registry}")
    private String schemaRegistryUrl;

    @Bean
    public Properties streamConfig() {
        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        APPLICATION_ID_CONFIG, "streams-app",
                        BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
                        DEFAULT_KEY_SERDE_CLASS_CONFIG, String.class,
                        DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl
                )
        );
        return properties;
    }

    @Bean
    public StreamsBuilder streamsBuilder(){
        return new StreamsBuilder();
    }

    @Bean
    public Pipeline pipeline(
            KafkaTopics kafkaTopics, StreamsBuilder streamsBuilder, Properties streamConfig
    ){
        return  new Pipeline(kafkaTopics, streamsBuilder, streamConfig);
    }

    @Bean
    public KafkaStreams kafkaStreams(Pipeline pipeline) {
        KafkaStreams kafkaStream = new KafkaStreams(pipeline.buildStream(), streamConfig());
        kafkaStream.start();
        return kafkaStream;
    }
}
