package com.stream_dsl;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StreamDslApplication {

	public static void main(String[] args) {
        
        Properties consumerProperties = new Properties();
        setConsumerProps(consumerProperties);
        
        Properties producerProperties = new Properties();
        setProducerProps(producerProperties);
        
        KafkaConsumer<String, String> consumer1 = new KafkaConsumer<>(consumerProperties);
        KafkaConsumer<String, String> consumer2 = new KafkaConsumer<>(consumerProperties);

        KafkaProducer<String, String> producer1 = new KafkaProducer<>(producerProperties);

        subscribeToTopics(consumer1, consumer2);

        producer1.send(new ProducerRecord<>(
                "share-test",
                "1",
                "abc1"
        ));

        producer1.send(new ProducerRecord<>(
                "share-test",
                "user129",
                "abc2"
        ));

        while (true) {
            var records1 = consumer1.poll(Duration.ofMillis(100));
            var records2 = consumer2.poll(Duration.ofMillis(100));
            for (var record : records1) {
                System.out.printf("Consumer1 value %s and partition %s%n", record.value(), record.partition());
            }
            for (var record : records2) {
                System.out.printf("Consumer2 value %s and partition %s%n", record.value(), record.partition());
            }
        }
    }

    private static void subscribeToTopics(KafkaConsumer<String, String> consumer1, KafkaConsumer<String, String> consumer2) {
        consumer1.subscribe(List.of("share-test"));
        consumer2.subscribe(List.of("share-test"));
    }

    private static void setProducerProps(Properties producerProperties) {
        producerProperties.putAll(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        "http://localhost:9091"
                )
        );
    }

    private static void setConsumerProps(Properties consumerProperties) {
        consumerProperties.put("bootstrap.servers", "localhost:9092");
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "group-a");
        consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    }
}
