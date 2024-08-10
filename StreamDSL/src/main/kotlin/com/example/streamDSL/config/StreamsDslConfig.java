package com.example.streamDSL.config;

import com.example.streamDSL.Pipeline;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
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

    @Value(value = "{spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value = "{spring.kafka.schema-registry}")
    private String schemaRegistryUrl;

    @Bean
    public Properties streamConfig() {
        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        APPLICATION_ID_CONFIG, "streams-app",
                        BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
                        DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String(),
                        DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl
                )
        );
        return properties;
    }

    @Bean
    public KafkaTopics kafkaTopics(
            @Value("{kafka.config.mainData}") String mainData,
            @Value("{kafka.config.additionalData}") String additionalData,
            @Value("{kafka.config.output}") String output
    ){
       return new KafkaTopics(mainData, additionalData, output);
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
