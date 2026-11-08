package com.stream_dsl;

import com.stream_dsl.avro.AdditionalData;
import com.stream_dsl.avro.JoinedData;
import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
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
        // Pipeline connecting: Source -> Join -> Hopping Window Count -> Output
        sendWindowedCountToOutput.accept(
                joinMainWithAdditionalData.apply(
                        mapMainDoJoinedModel.apply(getMainDataStream.get()), getAdditionalDataTable.get()
                )
        );
        return streamsBuilder.build();
    }

    private final Consumer<KStream<String, JoinedData>> sendWindowedCountToOutput =
            joinedWithAdditionalData ->
                    joinedWithAdditionalData
                            .groupByKey(Grouped.with(Serdes.String(), SerdesUtil.JoinedDataSerde.apply(streamConfig)))
                            // Hopping Window: Size = 5 seconds, Advance (Hop) = 2 seconds
                            .windowedBy(
                                    TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5))
                                            .advanceBy(Duration.ofSeconds(2))
                            )
                            .count(Materialized.with(Serdes.String(), Serdes.Long()))
                            .toStream()
                            // Map Windowed<String> key to a readable string format showing window start time
                            .map((windowedKey, count) ->
                                    new KeyValue<>(windowedKey.key() + "@" + windowedKey.window().start(), count)
                            )
                            .to(
                                    kafkaTopics.output(),
                                    Produced.with(Serdes.String(), Serdes.Long())
                            );

    private final BiFunction<KStream<String, JoinedData>, KTable<String, AdditionalData>, KStream<String, JoinedData>>
            joinMainWithAdditionalData = (mainDataMapToJoinedData, additionalDataTable) ->
            mainDataMapToJoinedData
                    .leftJoin(
                            additionalDataTable,
                            getJoinedDataAdditionalDataJoinedDataValueJoiner(),
                            configureSerde()
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

    private final Supplier<KTable<String, AdditionalData>> getAdditionalDataTable = () ->
            streamsBuilder
                    .stream(
                            kafkaTopics.additionalData(),
                            Consumed.with(Serdes.String(), SerdesUtil.AdditionalDataSerde.apply(streamConfig))
                    )
                    .selectKey((_, v) -> v.getReferenceData())
                    .toTable(Materialized.with(Serdes.String(), SerdesUtil.AdditionalDataSerde.apply(streamConfig)));

    private final Supplier<KStream<String, MainData>> getMainDataStream = () ->
            streamsBuilder
                    .stream(
                            kafkaTopics.mainData(),
                            Consumed.with(Serdes.String(), SerdesUtil.MainDataSerde.apply(streamConfig))
                    )
                    .selectKey((_, v) -> v.getReferenceData());

    private Joined<String, JoinedData, AdditionalData> configureSerde() {
        return Joined.with(
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