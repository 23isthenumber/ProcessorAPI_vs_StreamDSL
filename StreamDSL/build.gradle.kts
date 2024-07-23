plugins {
	id("org.springframework.boot") version "3.3.1"
	id("io.spring.dependency-management") version "1.1.5"
	kotlin("jvm") version "1.9.24"
	kotlin("plugin.spring") version "1.9.24"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()

	maven {
		url = uri("https://packages.confluent.io/maven")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.apache.kafka:kafka-streams:3.7.1")
	implementation("org.springframework.kafka:spring-kafka:3.2.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("io.confluent:kafka-streams-avro-serde:7.6.1")
	testImplementation("org.apache.kafka:kafka-streams-test-utils:3.7.1")
	testImplementation("io.mockk:mockk:1.13.12")
	testImplementation("org.springframework.kafka:spring-kafka-test:3.2.2")
	testImplementation("org.springframework.cloud:spring-cloud-stream-schema:2.2.1.RELEASE")
	implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
