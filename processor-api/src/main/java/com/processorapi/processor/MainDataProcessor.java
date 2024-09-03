package com.processorapi.processor;

import com.processorapi.util.StoreType;
import com.processorapi.avro.AdditionalData;
import com.processorapi.avro.JoinedData;
import com.processorapi.avro.MainData;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class MainDataProcessor implements Processor<String, MainData, String, JoinedData> {

    private KeyValueStore<String, AdditionalData> store;
    private ProcessorContext<String, JoinedData> context;

    @Override
    public void init(ProcessorContext<String, JoinedData> processorContext) {
        context = processorContext;
        store = context.getStateStore(StoreType.AdditionalDataStore.name());
    }

    @Override
    public void process(Record<String, MainData> record) {
        MainData mainData = record.value();
        AdditionalData additionalData = store.get(mainData.getReferenceData());
        String additionalFields = "";
        if (additionalData != null) additionalFields = additionalData.getAdditionalField();
        JoinedData joinedData = new JoinedData(
                mainData.getMainField(),
                additionalFields,
                mainData.getReferenceData()
        );
        sendForwardInterestingData(joinedData);
    }

    private void sendForwardInterestingData(JoinedData joinedData) {
        if (!"boring_data".equals(joinedData.getMainField())) {
            context.forward(
                    new Record<>(
                            joinedData.getReferenceData(),
                            joinedData,
                            context.currentStreamTimeMs()
                    )
            );
        }
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}