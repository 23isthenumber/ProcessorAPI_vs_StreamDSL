package com.stream_dsl.exception;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;

import java.util.Map;

public class CustomProduceHandler implements ProductionExceptionHandler {
    @Override
    public ProductionExceptionHandlerResponse handle(ProducerRecord<byte[], byte[]> record, Exception exception) {
        return ProductionExceptionHandlerResponse.CONTINUE;
    }

    @Override
    public ProductionExceptionHandlerResponse handleSerializationException(ProducerRecord record, Exception exception) {
        System.out.println("I am handling things "+ exception.getMessage());
        return ProductionExceptionHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
