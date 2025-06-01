package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
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
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
		classes = TestConfig.class
)
@EmbeddedKafka(
		partitions = 1,
		topics = {
				("${topic.mainData}"),
				("${topic.additionalData}"),
				("${topic.output}")
		},
		bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineIT {

	@Autowired
	private KafkaConsumer<String, JoinedData> outputConsumer;

	@Autowired
	private KafkaProducer<String, MainData> mainDataProducer;

	@Autowired
	private KafkaProducer<String, AdditionalData> additionalDataProducer;

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
		sendAdditionalDataToTopic(
						topics.additionalData(),
						additionalData.getReferenceData(),
						additionalData
				);
		sendMainDataToTopic(topics.mainData(), mainData.getReferenceData(), mainData);
		//THEN
		JoinedData actual = outputConsumer.poll(Duration.ofMillis(1000)).iterator().next().value();
		assertEquals(expected, actual);
	}

	@Test
	public void shouldReadNextMessageAfterException() throws ExecutionException, InterruptedException {
		//GIVEN
		final String additionalDataValue = "additionalData";
		final String referenceData = "referenceData";
		final MainData mainData = new MainData(
				"CORRUPTED",
				referenceData
		);
		final AdditionalData additionalData = new AdditionalData(
				additionalDataValue,
				referenceData
		);
		//WHEN
		sendAdditionalDataToTopic(
						topics.additionalData(),
						additionalData.getReferenceData(),
						additionalData
				);
		sendMainDataToTopic(topics.mainData(), mainData.getReferenceData(), mainData);
		//THEN
//		assertThrows(NoSuchElementException.class, () -> {
		TimeUnit.SECONDS.sleep(15);
			outputConsumer.poll(Duration.ofMillis(1000)).iterator().next().value();
//		});
		//AND
		TimeUnit.SECONDS.sleep(5);
		final String mainDataValue = "mainData";
		final String nextAdditionalDataValue = "nextAdditionalData";
		final String nextReferenceData = "nextReferenceData";
		final MainData nextMainData = new MainData(
				mainDataValue,
				nextReferenceData
		);
		final AdditionalData nextAdditionalData = new AdditionalData(
				nextAdditionalDataValue,
				nextReferenceData
		);
		final JoinedData expected = new JoinedData(
				mainDataValue,
				nextAdditionalDataValue,
				nextReferenceData
		);
		//WHEN
		sendAdditionalDataToTopic(
						topics.additionalData(),
						nextAdditionalData.getReferenceData(),
						nextAdditionalData
				);
		sendMainDataToTopic(topics.mainData(), nextMainData.getReferenceData(), nextMainData);

		JoinedData actual = outputConsumer.poll(Duration.ofMillis(1000)).iterator().next().value();
		assertEquals(expected, actual);
	}

	private void sendMainDataToTopic(String topic, String key, MainData value) throws InterruptedException, ExecutionException {
		mainDataProducer.send(
				new ProducerRecord<>(
						topic,
						key,
						value
				)
		).get();
	}

	private void sendAdditionalDataToTopic(String topic, String key, AdditionalData value) throws InterruptedException, ExecutionException {
		additionalDataProducer.send(
				new ProducerRecord<>(
						topic,
						key,
						value
				)
		).get();
	}
}
