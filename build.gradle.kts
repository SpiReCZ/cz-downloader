import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.graalvm.buildtools.native") version "0.9.18"
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.spring") version "1.7.21"
}

group = "cz.dwn"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    maven { url = uri("https://repo.spring.io/release") }
    mavenCentral()
    // TOR Onion Proxy
    maven { url = uri("https://jitpack.io") }
}

extra["springBootAdminVersion"] = "2.7.7"
extra["springDocVersion"] = "1.6.12"

dependencies {
    // Default
    //implementation("org.springframework.boot:spring-boot-starter-actuator")
    //implementation("org.springframework.boot:spring-boot-starter-cache")
    //implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    //implementation("org.springframework.boot:spring-boot-starter-security")
    //implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    //implementation("de.codecentric:spring-boot-admin-starter-server")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    //implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // Tensorflow support
    implementation("ai.djl.spring:djl-spring-boot-starter-tensorflow-auto:0.19")
    // Tor support
    implementation("io.github.theborakompanioni:spring-tor-starter:0.7.0")
    // Custom added
    implementation("org.springdoc:springdoc-openapi-webflux-ui:${property("springDocVersion")}")
    implementation("org.springdoc:springdoc-openapi-security:${property("springDocVersion")}")
    implementation("org.springdoc:springdoc-openapi-native:${property("springDocVersion")}")
    // Other
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    //testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    testImplementation("io.projectreactor:reactor-test")
    //testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
