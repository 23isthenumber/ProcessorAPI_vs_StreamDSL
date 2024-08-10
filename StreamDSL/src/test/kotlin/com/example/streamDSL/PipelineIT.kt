package com.example.streamDSL

import com.example.streamDSL.config.KafkaTopics
import com.streamdsl.avro.AdditionalData
import com.streamdsl.avro.JoinedData
import com.streamdsl.avro.MainData
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.Duration
import kotlin.test.assertEquals

@SpringBootTest(
    classes = [TestConfig::class]
)
@EmbeddedKafka(
    partitions = 1,
    topics = [
        ("\${kafka.config.mainData}"),
        ("\${kafka.config.additionalData}"),
        ("\${kafka.config.output}")
    ],
    bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineIT {

    @Autowired
    private lateinit var outputConsumer: KafkaConsumer<String, JoinedData>

    @Autowired
    private lateinit var mainDataProducer: KafkaProducer<String, MainData>

    @Autowired
    private lateinit var additionalDataProducer: KafkaProducer<String, AdditionalData>

    @Autowired
    private lateinit var topics: KafkaTopics

    @BeforeAll
    fun setup(){
        outputConsumer.subscribe(listOf(topics.output))
    }

    @AfterAll
    fun cleanUp(){
        outputConsumer.unsubscribe()
    }

    @Test
    fun `Should process data from two sources`(){
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
        additionalDataProducer.send(
            ProducerRecord(
                topics.additionalData,
                additionalData.referenceData,
                additionalData
            )
        )
        mainDataProducer.send(
            ProducerRecord(
                topics.mainData,
                mainData.referenceData,
                mainData
            )
        )
        //THEN
        val actual: JoinedData = outputConsumer.poll(Duration.ofMillis(1000)).last().value().also {
            outputConsumer.commitSync()
        }
        assertEquals(expected, actual)
    }
}