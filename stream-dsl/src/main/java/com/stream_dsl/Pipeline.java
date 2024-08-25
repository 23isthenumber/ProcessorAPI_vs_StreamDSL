package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;

public class Pipeline {

    private final KafkaTopics kafkaTopics;
    private final StreamsBuilder streamsBuilder;
    private final Properties streamConfig;

    public Pipeline(KafkaTopics kafkaTopics, StreamsBuilder streamsBuilder, Properties streamConfig) {
        this.kafkaTopics = kafkaTopics;
        this.streamsBuilder = streamsBuilder;
        this.streamConfig = streamConfig;
    }

    public Topology buildStream() {
        final KStream<String, MainData> mainDataStream = getMainDataStream();

        final KTable<String, AdditionalData> additionalDataTable = getAdditionalDataTable();

        final KStream<String, JoinedData> mainDataMapToJoinedData = mapMainDoJoinedModel(mainDataStream);

        final KStream<String, JoinedData> joinedWithAdditionalData = joinMainWithAdditionalData(
                mainDataMapToJoinedData, additionalDataTable
        );

        sendInterestingDataToOutput(joinedWithAdditionalData);

        return streamsBuilder.build();
    }

    private void sendInterestingDataToOutput(KStream<String, JoinedData> joinedWithAdditionalData) {
        joinedWithAdditionalData
                .filter((_, v) -> !"boring_data".equals(v.getMainField()))
                .to(
                        kafkaTopics.output(),
                        Produced.with(Serdes.String(), SerdesUtil.JoinedDataSerde(streamConfig))
                );
    }

    private KStream<String, JoinedData> joinMainWithAdditionalData(KStream<String, JoinedData> mainDataMapToJoinedData, KTable<String, AdditionalData> additionalDataTable) {
        return mainDataMapToJoinedData
                .leftJoin(
                        additionalDataTable,
                        getJoinedDataAdditionalDataJoinedDataValueJoiner(),
                        configureSerde()
                );
    }

    private static KStream<String, JoinedData> mapMainDoJoinedModel(KStream<String, MainData> mainDataStream) {
        return mainDataStream
                .mapValues((_, v) ->
                        new JoinedData(
                                v.getMainField(),
                                "",
                                v.getReferenceData()
                        )
                );
    }

    private KTable<String, AdditionalData> getAdditionalDataTable() {
        return streamsBuilder
                .stream(
                        kafkaTopics.additionalData(),
                        Consumed.with(Serdes.String(), SerdesUtil.AdditionalDataSerde(streamConfig))
                )
                .selectKey((_, v) -> v.getReferenceData())
                .toTable(Materialized.with(Serdes.String(), SerdesUtil.AdditionalDataSerde(streamConfig)));
    }

    private KStream<String, MainData> getMainDataStream() {
        return streamsBuilder
                .stream(
                        kafkaTopics.mainData(),
                        Consumed.with(Serdes.String(), SerdesUtil.MainDataSerde(streamConfig))
                )
                .selectKey((_, v) -> v.getReferenceData());
    }

    private Joined<String, JoinedData, AdditionalData> configureSerde() {
        return Joined.with(
                Serdes.String(),
                SerdesUtil.JoinedDataSerde(streamConfig),
                SerdesUtil.AdditionalDataSerde(streamConfig)
        );
    }

    private ValueJoiner<JoinedData, AdditionalData, JoinedData> getJoinedDataAdditionalDataJoinedDataValueJoiner() {
        return (joinedData, additionalData) ->
                new JoinedData(
                joinedData.getMainField(),
                additionalData != null ? additionalData.getAdditionalField() : "",
                joinedData.getReferenceData()
        );
    }
}
