package com.stream_dsl.exception;

import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

public class CatchMeIfYouCan implements StreamsUncaughtExceptionHandler {
    @Override
    public StreamThreadExceptionResponse handle(Throwable exception) {
        System.out.println("Catch me if you can");
        return StreamThreadExceptionResponse.REPLACE_THREAD;
    }
}
