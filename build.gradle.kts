plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.securenotes"
version = "1.0-SNAPSHOT"

val springVersion = "3.4.4"
val springSecurityVersion = "6.4.4"
val jjwtVersion = "0.12.6"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    implementation("org.springframework.boot:spring-boot-starter-security:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
    testImplementation("org.springframework.security:spring-security-test:$springSecurityVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
//    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
