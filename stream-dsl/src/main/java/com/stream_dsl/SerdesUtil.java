package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import java.util.Collections;
import java.util.Properties;

public class SerdesUtil {

    public static Serde<MainData> MainDataSerde(Properties config){
        Serde<MainData> serde = new SpecificAvroSerde<>();
        serde.configure(
                Collections.singletonMap(
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        config.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
                ), false
        );
        return serde;
    }

    public static Serde<AdditionalData> AdditionalDataSerde(Properties config){
        Serde<AdditionalData> serde = new SpecificAvroSerde<>();
        serde.configure(
                Collections.singletonMap(
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        config.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
                ), false
        );
        return serde;
    }

    public static Serde<JoinedData> JoinedDataSerde(Properties config){
        Serde<JoinedData> serde = new SpecificAvroSerde<>();
        serde.configure(
                Collections.singletonMap(
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        config.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
                ), false
        );
        return serde;
    }
}