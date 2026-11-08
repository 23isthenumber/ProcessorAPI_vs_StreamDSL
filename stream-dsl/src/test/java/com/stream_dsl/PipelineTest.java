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
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void shouldDemonstrateSessionWindowInactivityGap() {
        // GIVEN: Pre-populate additional data table
        String referenceKey = "referenceData";
        AdditionalData additionalData = new AdditionalData("additionalDataValue", referenceKey);
        additionalDataInput.pipeInput(referenceKey, additionalData);

        // WHEN: Send events with a 5-second inactivity gap configuration
        // 1. Event 1 at 1000ms (Starts Session #1)
        mainDataInput.pipeInput(referenceKey, new MainData("event1", referenceKey), 1000L);

        // 2. Event 2 at 3000ms (Within 5s gap, merges into Session #1)
        mainDataInput.pipeInput(referenceKey, new MainData("event2", referenceKey), 3000L);

        // 3. Event 3 at 10000ms (7s after Event 2, exceeding the 5s inactivity gap!)
        // This closes Session #1 (with count = 2) and starts a brand new Session #2.
        mainDataInput.pipeInput(referenceKey, new MainData("event3", referenceKey), 10000L);

        // THEN: Read output records
        var outputRecords = outputTopic.readKeyValuesToList();

        // Verify that session window results were successfully emitted
        assertEquals(?, outputRecords.size());
    }
}