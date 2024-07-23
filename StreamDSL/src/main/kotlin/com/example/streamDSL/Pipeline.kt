package com.example.streamDSL

import com.example.streamDSL.config.KafkaTopics
import com.streamdsl.avro.AdditionalData
import com.streamdsl.avro.JoinedData
import com.streamdsl.avro.MainData
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import java.util.*

class Pipeline(
    private val kafkaTopics: KafkaTopics,
    private val streamsBuilder: StreamsBuilder,
    private val streamConfig: Properties
) {

    fun buildStream(){
        streamsBuilder.apply {
            stream(
                kafkaTopics.mainData,
                Consumed.with(Serdes.String(), SerdesUtil.getSerde<MainData>(streamConfig))
            )
                .changeKeyAndMapToJoinedData()
                .leftJoin(
                    readAdditionalData(),
                    dataJoiner(),
                    Materialized.with(Serdes.String(), SerdesUtil.getSerde<JoinedData>(streamConfig))
                )
                .toStream()
                .sendInterestingData()
        }
    }

    private fun KStream<String, MainData>.changeKeyAndMapToJoinedData() =
        groupBy { _, value ->  value.referenceData }
        .aggregate(
            { JoinedData() },
            { _, value, _ -> JoinedData(value.mainField, "", value.referenceData) },
            Materialized.with(Serdes.String(), SerdesUtil.getSerde<JoinedData>(streamConfig))
        )

    private fun KStream<String, JoinedData>.sendInterestingData() =
        filter { _, value -> value.mainField != "boring_data" }
        .to(
            kafkaTopics.output,
            Produced.with(Serdes.String(), SerdesUtil.getSerde<JoinedData>(streamConfig))
        )

    private fun dataJoiner() = { joinedData: JoinedData, additionalData: AdditionalData? ->
        joinedData.additionalField = additionalData?.additionalField
        joinedData
    }

    private fun StreamsBuilder.readAdditionalData(): KTable<String, AdditionalData>? =
        stream(
            kafkaTopics.additionalData,
            Consumed.with(Serdes.String(), SerdesUtil.getSerde<AdditionalData>(streamConfig))
        )
            .selectKey { _, value -> value.referenceData }
            .toTable()
}