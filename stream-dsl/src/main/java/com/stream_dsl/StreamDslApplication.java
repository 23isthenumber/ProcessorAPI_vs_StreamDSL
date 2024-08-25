package com.stream_dsl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StreamDslApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamDslApplication.class, args);
	}
}