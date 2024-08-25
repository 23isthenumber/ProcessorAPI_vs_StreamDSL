package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import com.stream_dsl.config.Pipeline;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineTest {

    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, MainData> mainDataTopic;
    private TestInputTopic<String, AdditionalData> additionalDataTopic;
    private TestOutputTopic<String, JoinedData> outputTopic;
    private static final String MAIN_DATA_TOPIC = "mainData";
    private static final String ADDITIONAL_DATA_TOPIC = "additionalData";
    private static final String OUTPUT_TOPIC = "Output";

    @BeforeEach
    public void setUp() {
        final KafkaTopics kafkaTopics = Mockito.mock(KafkaTopics.class);
        mockKafkaTopics(kafkaTopics);
        final StreamsBuilder streamBuilder = new StreamsBuilder();
        final Properties props = generateProperties();
        setUpPipeline(kafkaTopics, streamBuilder, props);
        setUpTopologyTestDriver(streamBuilder, props);
    }

    @AfterEach
    public void cleanUp(){
        topologyTestDriver.close();
    }

    @Test
    public void shouldCombineDataFromTwoSources(){
        //GIVEN
        final String mainDataValue = "mainData";
        final String additionalDataValue = "additionalData";
        final String referenceData = "referenceData";
        final MainData mainData = new MainData(
                mainDataValue,
                referenceData
        );
        final AdditionalData additionalData = new AdditionalData(
                additionalDataValue,
                referenceData
        );
        final JoinedData expected = new JoinedData(
                mainDataValue,
                additionalDataValue,
                referenceData
        );
        //WHEN
        additionalDataTopic.pipeInput(additionalData);
        mainDataTopic.pipeInput(mainData);
        //THEN
        final JoinedData actual = outputTopic.readValue();
        assertEquals(expected, actual);
    }

    @Test
    public void shouldProcessDataOnlyFromMainSourceWhenSecondaryDataHasDifferentKey(){
        //GIVEN
        final String mainDataValue = "mainData";
        final String additionalDataValue = "additionalData";
        final MainData mainData = new MainData(
                mainDataValue,
                "123"
        );
        final AdditionalData additionalData = new AdditionalData(
                additionalDataValue,
                "124"
        );
        final JoinedData expected = new JoinedData(
                mainDataValue,
                "",
                "123"
        );
        //WHEN
        additionalDataTopic.pipeInput(additionalData);
        mainDataTopic.pipeInput(mainData);
        //THEN
        final JoinedData actual = outputTopic.readValue();
        assertEquals(expected, actual);
    }

    @Test
    public void  shouldFilterOutBoringData(){
        //GIVEN
        final String mainDataValue = "boring_data";
        final String secondMainDataValue = "interesting_data";
        final String additionalDataValue = "additionalData";
        final String referenceData = "referenceData";
        final MainData mainData = new MainData(
                mainDataValue,
                referenceData
        );
        final AdditionalData additionalData = new AdditionalData(
                additionalDataValue,
                referenceData
        );
        final MainData secondMainData = new MainData(
                secondMainDataValue,
                referenceData
        );
        final JoinedData expected = new JoinedData(
                secondMainDataValue,
                additionalDataValue,
                referenceData
        );
        //WHEN
        additionalDataTopic.pipeInput(additionalData);
        mainDataTopic.pipeInput(secondMainData);
        mainDataTopic.pipeInput(mainData);
        //THEN
        final List<JoinedData> actual = outputTopic.readValuesToList();
        assertEquals(expected, actual.getLast());
        assertEquals(1, actual.size());
    }

    private  Properties generateProperties() {
        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        StreamsConfig.APPLICATION_ID_CONFIG, "test",
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9090",
                        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, String.class,
                        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test"
                )
        );
        return properties;
    }

    private void setUpTopologyTestDriver(StreamsBuilder streamBuilder, Properties props) {
        topologyTestDriver = new TopologyTestDriver(streamBuilder.build(), props);
        mainDataTopic = topologyTestDriver.createInputTopic(
                PipelineTest.MAIN_DATA_TOPIC, new StringSerializer(), SerdesUtil.MainDataSerde(props).serializer()
        );
        additionalDataTopic = topologyTestDriver.createInputTopic(
                PipelineTest.ADDITIONAL_DATA_TOPIC, new StringSerializer(), SerdesUtil.AdditionalDataSerde(props).serializer()
        );
        outputTopic = topologyTestDriver.createOutputTopic(
                PipelineTest.OUTPUT_TOPIC, new StringDeserializer(), SerdesUtil.JoinedDataSerde(props).deserializer()
        );
    }

    private static void setUpPipeline(KafkaTopics kafkaTopics, StreamsBuilder streamBuilder, Properties props) {
        final Pipeline pipeline = new Pipeline(kafkaTopics, streamBuilder, props);
        pipeline.buildStream();
    }

    private static void mockKafkaTopics(KafkaTopics kafkaTopics) {
        Mockito.when(kafkaTopics.mainData()).thenReturn(PipelineTest.MAIN_DATA_TOPIC);
        Mockito.when(kafkaTopics.additionalData()).thenReturn(PipelineTest.ADDITIONAL_DATA_TOPIC);
        Mockito.when(kafkaTopics.output()).thenReturn(PipelineTest.OUTPUT_TOPIC);
    }
}
