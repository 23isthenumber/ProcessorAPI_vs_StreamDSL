package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
		classes = TestConfig.class
)
@EmbeddedKafka(
		partitions = 1,
		topics = {
				("${topic.mainData}"),
				("${topic.output}")
		},
		bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineIT {

	@Autowired
	private KafkaConsumer<String, MainData> outputConsumer;

	@Autowired
	private KafkaProducer<String, MainData> mainDataProducer;

	@Autowired
	private KafkaTopics topics;

	@BeforeAll
	public void setup(){
		outputConsumer.subscribe(Collections.singletonList(topics.output()));
	}

	@AfterAll
	public void cleanUp(){
		outputConsumer.unsubscribe();
	}

	@Test
	public void shouldProcessDataFromTwoSources() throws ExecutionException, InterruptedException {
		//GIVEN
		final String mainDataValue = "mainData";
		final String referenceData = "referenceData";
		final MainData mainData = new MainData(
				mainDataValue,
				referenceData
		);
		final String expectedKey = "User123";
		//WHEN
		mainDataProducer.send(
				new ProducerRecord<>(
						topics.mainData(),
						mainData.getReferenceData(),
						mainData
				)
		).get();
		//THEN
		ConsumerRecord<String, MainData> actual = outputConsumer.poll(Duration.ofMillis(1000)).iterator().next();
		assertEquals(expectedKey, actual.key());
	}
}
