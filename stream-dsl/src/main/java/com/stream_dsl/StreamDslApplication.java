package com.stream_dsl;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StreamDslApplication {

	public static void main(String[] args) {
//		SpringApplication.run(StreamDslApplication.class, args);
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "share-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

//        KafkaShareConsumer<String, String> consumer1 = new KafkaShareConsumer<>(props);
//        KafkaShareConsumer<String, String> consumer2 = new KafkaShareConsumer<>(props);
        KafkaConsumer<String, String> consumer1 = new KafkaConsumer<>(props);
        KafkaConsumer<String, String> consumer2 = new KafkaConsumer<>(props);

        consumer1.subscribe(List.of("share-test"));
        consumer2.subscribe(List.of("share-test"));

        // Poll for records
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
}
