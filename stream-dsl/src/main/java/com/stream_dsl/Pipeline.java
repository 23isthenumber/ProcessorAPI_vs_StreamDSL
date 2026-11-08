package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import java.time.Duration;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Pipeline {

    private KafkaTopics kafkaTopics;
    private StreamsBuilder streamsBuilder;
    private Properties streamConfig;

    public Pipeline(KafkaTopics kafkaTopics, StreamsBuilder streamsBuilder, Properties streamConfig) {
        this.kafkaTopics = kafkaTopics;
        this.streamsBuilder = streamsBuilder;
        this.streamConfig = streamConfig;
    }

    public Topology buildStream() {
        sendInterestingDataToOutput.accept(
                joinMainWithAdditionalData.apply(
                        mapMainDoJoinedModel.apply(getMainDataStream.get()), getAdditionalDataStream.get()
                )
        );
        return streamsBuilder.build();
    }

    private final Consumer<KStream<String, JoinedData>> sendInterestingDataToOutput =
            joinedWithAdditionalData ->
                    joinedWithAdditionalData
                            .filter((_, v) -> !"boring_data".equals(v.getMainField()))
                            .to(
                                    kafkaTopics.output(),
                                    Produced.with(Serdes.String(), SerdesUtil.JoinedDataSerde.apply(streamConfig))
                            );

    private final BiFunction<KStream<String, JoinedData>, KStream<String, AdditionalData>, KStream<String, JoinedData>>
            joinMainWithAdditionalData = (mainDataMapToJoinedData, additionalDataStream) ->
            mainDataMapToJoinedData
                    .leftJoin(
                            additionalDataStream,
                            getJoinedDataAdditionalDataJoinedDataValueJoiner(),
                            // FIX: Use JoinWindows instead of SlidingWindows for stream-stream joins
                            JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(5)),
                            configureStreamJoined()
                    );

    private final Function<KStream<String, MainData>, KStream<String, JoinedData>> mapMainDoJoinedModel =
            mainDataStream ->
                    mainDataStream
                            .mapValues((_, v) ->
                                    new JoinedData(
                                            v.getMainField(),
                                            "",
                                            v.getReferenceData()
                                    )
                            );

    private final Supplier<KStream<String, AdditionalData>> getAdditionalDataStream = () ->
            streamsBuilder
                    .stream(
                            kafkaTopics.additionalData(),
                            Consumed.with(Serdes.String(), SerdesUtil.AdditionalDataSerde.apply(streamConfig))
                    )
                    .selectKey((_, v) -> v.getReferenceData());

    private final Supplier<KStream<String, MainData>> getMainDataStream = () ->
            streamsBuilder
                    .stream(
                            kafkaTopics.mainData(),
                            Consumed.with(Serdes.String(), SerdesUtil.MainDataSerde.apply(streamConfig))
                    )
                    .selectKey((_, v) -> v.getReferenceData());

    private StreamJoined<String, JoinedData, AdditionalData> configureStreamJoined() {
        return StreamJoined.with(
                Serdes.String(),
                SerdesUtil.JoinedDataSerde.apply(streamConfig),
                SerdesUtil.AdditionalDataSerde.apply(streamConfig)
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