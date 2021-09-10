plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":runtime"))
    api(libs.ktor.server.core)
    api(libs.ktor.utils)
    api(libs.kotlin.serialization.json)
    implementation(libs.logback)
}

publishing {
    publications {
        create<MavenPublication>("server-ktor") {
            from(components["java"])
        }
    }
}