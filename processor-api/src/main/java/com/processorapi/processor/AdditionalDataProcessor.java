package com.processorapi.processor;

import com.processorapi.util.StoreType;
import com.processorapi.avro.AdditionalData;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class AdditionalDataProcessor implements Processor<String, AdditionalData, String, AdditionalData> {

    private KeyValueStore<String, AdditionalData> store;

    @Override
    public void init(ProcessorContext<String, AdditionalData> processorContext) {
        store = processorContext.getStateStore(StoreType.AdditionalDataStore.name());
    }

    @Override
    public void process(Record<String, AdditionalData> record) {
        store.put(record.value().getReferenceData(), record.value());
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
