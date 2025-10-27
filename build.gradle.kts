plugins {
    kotlin("jvm") version "2.0.0"
    id("java")
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

group = "org.promptunit"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
}

dependencies {
    // Spring Framework BOM for dependency management (Spring AI transitively depends on this)
    implementation(platform("org.springframework:spring-framework-bom:6.1.0"))
    
    // Spring AI - align with APIs used in source (ResponseFormat, etc.)
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.0-M2"))
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-ollama")
    // api(project(":modules:template-parser"))

    // Jackson - let Spring AI BOM manage versions through Spring Framework BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.networknt:json-schema-validator:1.5.1")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("org.slf4j:slf4j-api:2.0.13")

    // Lombok for all modules
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

// Removed Spring Dependency Management extension usage; using BOMs via platform() above

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Enable annotation processing for Lombok and other annotation processors
    options.compilerArgs.addAll(listOf("-proc:full", "-parameters"))
}

tasks.test {
    useJUnitPlatform()
}