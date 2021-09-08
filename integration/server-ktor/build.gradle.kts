plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":runtime"))
    api("io.ktor:ktor-server-core:1.6.1")
    api("io.ktor:ktor-server-netty:1.6.1")
    api("io.ktor:ktor-utils:1.6.1")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

publishing {
    publications {
        create<MavenPublication>("server-ktor") {
            from(components["java"])
        }
    }
}