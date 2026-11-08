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
import static org.junit.jupiter.api.Assertions.*;

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

        // Configure input/output topics with appropriate SerDes
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
    void shouldAggregateEventsInTumblingWindow() {
        // GIVEN: Pre-populate additional data table
        String referenceKey = "referenceData";
        AdditionalData additionalData = new AdditionalData("additionalDataValue", referenceKey);
        additionalDataInput.pipeInput(referenceKey, additionalData);

        // WHEN: Send main data events into the stream
        mainDataInput.pipeInput(referenceKey, new MainData("event1", referenceKey), 1000L);
        mainDataInput.pipeInput(referenceKey, new MainData("event2", referenceKey), 2000L);

        // Advance stream time past the 5-second tumbling window boundary (e.g., to 6000ms)
        // to close the window and trigger emission.
        mainDataInput.pipeInput(referenceKey, new MainData("event3", referenceKey), 6000L);

        // THEN: Read the windowed aggregation result
        var outputRecords = outputTopic.readKeyValuesToList();

        // Verify that we received an output record with count = 2 for the first window
        boolean foundExpectedCount = false;
        for (var record : outputRecords) {
            if (record.value.equals(2L)) {
                foundExpectedCount = true;
                break;
            }
        }

        assertTrue(foundExpectedCount, "Expected to find a tumbling window count of 2");
    }
}