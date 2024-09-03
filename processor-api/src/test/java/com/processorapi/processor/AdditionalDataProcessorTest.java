package com.processorapi.processor;

import com.processorapi.avro.AdditionalData;
import com.processorapi.util.StoreType;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdditionalDataProcessorTest {

    private final KeyValueStore<String, AdditionalData> store = Mockito.mock(KeyValueStore.class);
    private final ProcessorContext<String, AdditionalData> processorContext = Mockito.mock(
            ProcessorContext.class
    );
    private final AdditionalDataProcessor additionalDataProcessor = new AdditionalDataProcessor();

    @BeforeEach
    void setup() {
        Mockito.when(processorContext.getStateStore(StoreType.AdditionalDataStore.name())).thenReturn(store);
        additionalDataProcessor.init(processorContext);
    }

    @Test
    void shouldPutToStore() {
        //given
        AdditionalData additionalData = new AdditionalData("a","b");
        Record<String, AdditionalData> record = new Record<>(
                "", additionalData, processorContext.currentStreamTimeMs()
        );
        //when
        additionalDataProcessor.process(record);
        //then
        Mockito.verify(store, Mockito.times(1)).put("b", additionalData);
    }
}