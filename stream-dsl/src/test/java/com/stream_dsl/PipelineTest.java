package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Properties;
import static org.apache.kafka.streams.StreamsConfig.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, MainData> mainDataInput;
    private TestInputTopic<String, AdditionalData> additionalDataInput;
    private TestOutputTopic<String, Long> outputTopic;

    private final KafkaTopics topics = new KafkaTopics("mainDataTopic", "additionalDataTopic", "outputTopic");

    @BeforeEach
    void setUp() {
        Properties config = new Properties();
        config.putAll(Map.of(
                APPLICATION_ID_CONFIG, "test-app",
                BOOTSTRAP_SERVERS_CONFIG, "dummy:1234",
                DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName(),
                DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName(),
                "schema.registry.url", "mock://no-url"
        ));

        StreamsBuilder builder = new StreamsBuilder();
        Pipeline pipeline = new Pipeline(topics, builder, config);
        Topology topology = pipeline.buildStream();

        testDriver = new TopologyTestDriver(topology, config);

        mainDataInput = testDriver.createInputTopic(
                topics.mainData(),
                Serdes.String().serializer(),
                SerdesUtil.MainDataSerde.apply(config).serializer()
        );

        additionalDataInput = testDriver.createInputTopic(
                topics.additionalData(),
                Serdes.String().serializer(),
                SerdesUtil.AdditionalDataSerde.apply(config).serializer()
        );

        outputTopic = testDriver.createOutputTopic(
                topics.output(),
                Serdes.String().deserializer(),
                Serdes.Long().deserializer()
        );
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldDemonstrateHoppingWindowOverlap() {
        // GIVEN: Pre-populate additional data table
        String referenceKey = "referenceData";
        AdditionalData additionalData = new AdditionalData("additionalDataValue", referenceKey);
        additionalDataInput.pipeInput(referenceKey, additionalData);

        // WHEN: Send a SINGLE event at timestamp 3000ms (3 seconds)
        // With size = 5s and hop = 2s, this event falls into TWO overlapping windows:
        // 1. Window [0ms, 5000ms)     -> Window start key: referenceData@0
        // 2. Window [2000ms, 7000ms)  -> Window start key: referenceData@2000
        mainDataInput.pipeInput(referenceKey, new MainData("event1", referenceKey), 3000L);

        // Advance stream-time past 7000ms by sending a flush event at 8000ms
        // This forces both overlapping windows to close and emit their results.
        mainDataInput.pipeInput(referenceKey, new MainData("eventFlush", referenceKey), 8000L);

        // THEN: Read all emitted records from the output topic
        var outputRecords = outputTopic.readKeyValuesToList();

        boolean foundWindowZero = false;
        boolean foundWindowTwoThousand = false;

        for (var record : outputRecords) {
            // Check that the single event was duplicated into the overlapping window buckets
            if (record.key.equals(referenceKey + "@0") && record.value.equals(1L)) {
                foundWindowZero = true;
            }
            if (record.key.equals(referenceKey + "@2000") && record.value.equals(1L)) {
                foundWindowTwoThousand = true;
            }
        }

        assertTrue(foundWindowZero, "Expected an emission for the window starting at 0ms");
        assertTrue(foundWindowTwoThousand, "Expected an emission for the overlapping hopping window starting at 2000ms");
    }
}