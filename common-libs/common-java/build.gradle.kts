plugins {
    `java-library`
}

group = "com.polyshop"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web:3.5.7")
    api("org.springframework.boot:spring-boot-starter-validation:3.5.7")
    api("org.springframework.boot:spring-boot-starter-security:3.5.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.projectlombok:lombok:1.18.38")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
