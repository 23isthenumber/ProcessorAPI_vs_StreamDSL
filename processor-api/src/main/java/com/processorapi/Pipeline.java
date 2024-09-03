package com.processorapi;

import com.processorapi.avro.AdditionalData;
import com.processorapi.config.KafkaTopics;
import com.processorapi.processor.AdditionalDataProcessor;
import com.processorapi.processor.MainDataProcessor;
import com.processorapi.util.SerdesUtil;
import com.processorapi.util.StoreType;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import java.util.Properties;

public class Pipeline {

    public final String ADDITIONAL_DATA_SOURCE = "AdditionalDataSource";
    public final String MAIN_DATA_SOURCE = "MainDataSource";
    private final KafkaTopics kafkaTopics;
    private final StreamsBuilder streamsBuilder;
    private final Properties streamConfig;

    public Pipeline(KafkaTopics kafkaTopics, StreamsBuilder streamsBuilder, Properties streamConfig) {
        this.kafkaTopics = kafkaTopics;
        this.streamsBuilder = streamsBuilder;
        this.streamConfig = streamConfig;
    }

    public Topology buildTopology() {
        final Topology topology = streamsBuilder.build();
        addSources(topology);
        processData(topology);
        addSink(topology);
        return topology;
    }

    private void addSink(Topology topology) {
        topology.addSink(
                "Sink",
                kafkaTopics.output(),
                MainDataProcessor.class.getSimpleName()
        );
    }

    private void processData(Topology topology) {
        StoreBuilder<KeyValueStore<String, AdditionalData>> stateStoreBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(StoreType.AdditionalDataStore.name()),
                        Serdes.String(),
                        SerdesUtil.AdditionalDataSerde.apply(streamConfig)
                );
        addProcessors(topology);
        topology.addStateStore(
                stateStoreBuilder,
                AdditionalDataProcessor.class.getSimpleName(),
                MainDataProcessor.class.getSimpleName()
        );
    }

    private void addProcessors(Topology topology) {
        topology.addProcessor(
                AdditionalDataProcessor.class.getSimpleName(),
                AdditionalDataProcessor::new,
                ADDITIONAL_DATA_SOURCE
        );
        topology.addProcessor(
                MainDataProcessor.class.getSimpleName(),
                MainDataProcessor::new,
                MAIN_DATA_SOURCE
        );
    }

    private void addSources(Topology topology) {
        topology.addSource(
                MAIN_DATA_SOURCE,
                Serdes.String().deserializer(),
                SerdesUtil.MainDataSerde.apply(streamConfig).deserializer(),
                kafkaTopics.mainData()
        );
        topology.addSource(
                ADDITIONAL_DATA_SOURCE,
                Serdes.String().deserializer(),
                SerdesUtil.AdditionalDataSerde.apply(streamConfig).deserializer(),
                kafkaTopics.additionalData()
        );
    }
}
