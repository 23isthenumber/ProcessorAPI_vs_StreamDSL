package com.stream_dsl;

import com.stream_dsl.avro.MainData;
import com.stream_dsl.config.KafkaTopics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import java.util.Properties;
import java.util.function.Consumer;
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
        sendDataToOutput.accept(getMainDataStream.get());
        return streamsBuilder.build();
    }

    private final Consumer<KStream<String, MainData>> sendDataToOutput =
            dataToSend ->
                    dataToSend
                            .to(
                                    kafkaTopics.output(),
                                    Produced.with(Serdes.String(), SerdesUtil.MainDataSerde.apply(streamConfig))
                            );

    private final Supplier<KStream<String, MainData>> getMainDataStream = () ->
         streamsBuilder
                .stream(
                        kafkaTopics.mainData(),
                        Consumed.with(Serdes.String(), SerdesUtil.MainDataSerde.apply(streamConfig))
                )
                .selectKey((_, _) -> "User123");
}
