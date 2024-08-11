package com.example.streamDSL;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;

import java.util.Collections;
import java.util.Properties;

public class SerdesUtil {

    public static  <T extends SpecificRecord> SpecificAvroSerde<T> getSerde(Properties config){
        SpecificAvroSerde<T> record = new SpecificAvroSerde<>();
        record.configure(
                Collections.singletonMap(
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        config.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
                ), false
        );
        return record;
    }
}
