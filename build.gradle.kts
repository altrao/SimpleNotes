plugins {
    kotlin("jvm") version "2.1.10"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.simplenotes"
version = "1.0-SNAPSHOT"

val springVersion = "3.4.4"
val jjwtVersion = "0.12.6"

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-tomcat")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.1")
    implementation("org.springframework.boot:spring-boot-starter-security:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springVersion")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:$springVersion")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    implementation("com.bucket4j:bucket4j-redis:8.10.1")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
    testImplementation("org.springframework.security:spring-security-test:6.4.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    manifest {
        attributes("Start-Class" to "com.simplenotes.Application")
    }

    archiveFileName.set("SimpleNotes.jar")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
