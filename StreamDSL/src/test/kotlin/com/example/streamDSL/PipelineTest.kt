package com.example.streamDSL

import com.example.streamDSL.config.KafkaTopics
import com.streamdsl.avro.AdditionalData
import com.streamdsl.avro.JoinedData
import com.streamdsl.avro.MainData
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineTest {

    private lateinit var kafkaTopics: KafkaTopics
    private lateinit var streamBuilder: StreamsBuilder
    private lateinit var topologyTestDriver: TopologyTestDriver
    private lateinit var mainDataTopic: TestInputTopic<String, MainData>
    private lateinit var additionalDataTopic: TestInputTopic<String, AdditionalData>
    private lateinit var outputTopic: TestOutputTopic<String, JoinedData>
    private val props = Properties().also {
        it.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "test")
        it.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9090")
        it.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java.name)
        it.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde::class.java.name)
        it.setProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test")
    }

    @BeforeEach
    fun setUp() {
        kafkaTopics = mockk<KafkaTopics>().apply {
            every { mainData } returns MAIN_DATA_TOPIC
            every { additionalData } returns ADDITIONAL_DATA_TOPIC
            every { output } returns OUTPUT_TOPIC
        }
        streamBuilder = StreamsBuilder()
        Pipeline(kafkaTopics, streamBuilder, props).buildStream()
        topologyTestDriver =
            TopologyTestDriver(
                streamBuilder.build(), props
            ).also {
                mainDataTopic = it.createInputTopic(
                    MAIN_DATA_TOPIC, StringSerializer(), SerdesUtil.getSerde<MainData>(props).serializer()
                )
                additionalDataTopic = it.createInputTopic(
                    ADDITIONAL_DATA_TOPIC, StringSerializer(), SerdesUtil.getSerde<AdditionalData>(props).serializer()
                )
                outputTopic = it.createOutputTopic(
                    OUTPUT_TOPIC, StringDeserializer(), SerdesUtil.getSerde<JoinedData>(props).deserializer()
                )
            }
    }

        @AfterEach
        fun cleanUp(){
            topologyTestDriver.close()
        }

        @Test
        fun `Should combine data from two sources`(){
            //GIVEN
            val mainDataValue = "mainData"
            val additionalDataValue = "additionalData"
            val referenceData = "referenceData"
            val mainData = MainData(
                mainDataValue,
                referenceData
            )
            val additionalData = AdditionalData(
                additionalDataValue,
                referenceData
            )
            val expected = JoinedData(
                mainDataValue,
                additionalDataValue,
                referenceData
            )
            //WHEN
            additionalDataTopic.pipeInput(additionalData)
            mainDataTopic.pipeInput(mainData)
            //THEN
            val actual = outputTopic.readValue()
            assertEquals(expected, actual)
        }

        @Test
        fun `Should process data only from main source when secondary data has different key`(){
            //GIVEN
            val mainDataValue = "mainData"
            val additionalDataValue = "additionalData"
            val mainData = MainData(
                mainDataValue,
                "123"
            )
            val additionalData = AdditionalData(
                additionalDataValue,
                "124"
            )
            val expected = JoinedData(
                mainDataValue,
                null,
                "123"
            )
            //WHEN
            additionalDataTopic.pipeInput(additionalData)
            mainDataTopic.pipeInput(mainData)
            //THEN
            val actual = outputTopic.readValue()
            assertEquals(expected, actual)
        }

        @Test
        fun `Should filter out boring data`(){
            //GIVEN
            val mainDataValue = "boring_data"
            val secondMainDataValue = "interesting_data"
            val additionalDataValue = "additionalData"
            val referenceData = "referenceData"
            val mainData = MainData(
                mainDataValue,
                referenceData
            )
            val additionalData = AdditionalData(
                additionalDataValue,
                referenceData
            )
            val secondMainData = MainData(
                secondMainDataValue,
                referenceData
            )
            val expected = JoinedData(
                secondMainDataValue,
                additionalDataValue,
                referenceData
            )
            //WHEN
            additionalDataTopic.pipeInput(additionalData)
            mainDataTopic.pipeInput(secondMainData)
            mainDataTopic.pipeInput(mainData)
            //THEN
            val actual = outputTopic.readValuesToList()
            assertEquals(expected, actual.last())
            assertEquals(1, actual.size)
        }

    companion object {
        private const val MAIN_DATA_TOPIC = "mainData"
        private const val ADDITIONAL_DATA_TOPIC = "additionalData"
        private const val OUTPUT_TOPIC = "Output"
    }
}