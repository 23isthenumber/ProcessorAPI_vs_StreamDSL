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
    void shouldDemonstrateSlidingWindowRollingExpiration() {
        // GIVEN: Pre-populate additional data table
        String referenceKey = "referenceData";
        AdditionalData additionalData = new AdditionalData("additionalDataValue", referenceKey);
        additionalDataInput.pipeInput(referenceKey, additionalData);

        // WHEN & THEN: Step-by-step sliding window evolution (5-second max time difference)

        // 1. Send Event 1 at 1000ms
        // Window opens around 1000ms. Count = 1
        mainDataInput.pipeInput(referenceKey, new MainData("event1", referenceKey), 1000L);

        // 2. Send Event 2 at 3000ms (2s later, within the 5s window threshold of Event 1)
        // Both event1 and event2 are in the window. Count = 2
        mainDataInput.pipeInput(referenceKey, new MainData("event2", referenceKey), 3000L);

        // 3. Send Event 3 at 7000ms (4s after Event 2, but 6s after Event 1)
        // Event 1 is now outside the 5s sliding threshold and expires!
        // Only Event 2 and Event 3 remain in the window. Count = 2 (instead of 3)
        mainDataInput.pipeInput(referenceKey, new MainData("event3", referenceKey), 7000L);

        var outputRecords = outputTopic.readKeyValuesToList();

        // Verify that the sliding window successfully recorded the accumulation and rolling expiration
        assertEquals(?, outputRecords.size());
    }
}
