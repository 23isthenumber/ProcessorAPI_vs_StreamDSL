package com.example.StreamDSL

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StreamDslApplication

fun main(args: Array<String>) {
	runApplication<StreamDslApplication>(*args)
}
