plugins {
	kotlin("jvm") version "2.1.10"
	kotlin("plugin.spring") version "2.1.10"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.1.10"
}

group = "com.ryuken"
version = "0.0.1-SNAPSHOT"
description = "This is a backend for a social media application"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

// Added versions for libraries not managed by Spring Boot's BOM
val jjwtVersion = "0.12.6"
val awsSdkVersion = "2.25.65"
val bucket4jVersion = "8.10.1"

dependencies {
	// H2 Console (available via spring-boot-starter-web + h2 runtime dependency)

	// Web & API
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Database
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// Auth
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	// Cache
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// WebSockets
	implementation("org.springframework.boot:spring-boot-starter-websocket")

	// Storage (AWS SDK S3)
	implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
	implementation("software.amazon.awssdk:s3")

	// Search
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

	// Email
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// Monitoring + OpenAPI + Rate limiting
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
	implementation("com.bucket4j:bucket4j-core:$bucket4jVersion")

	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Dev & Test
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

