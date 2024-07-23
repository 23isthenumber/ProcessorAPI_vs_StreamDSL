package com.example.streamDSL

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import java.util.*

class SerdesUtil {

    companion object {

        fun <T : SpecificRecord> getSerde(kStreamsConfig: Properties): SpecificAvroSerde<T> =
            SpecificAvroSerde<T>().also {
                it.configure(
                    Collections.singletonMap(
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        kStreamsConfig.getProperty(
                            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
                        )
                    ), false
                )
            }
    }
}