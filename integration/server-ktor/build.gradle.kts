plugins {
    id("java-library")
    kotlin("jvm")
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":runtime"))
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.core)
    api(libs.ktor.utils)
    api(libs.kotlin.serialization.json)
    implementation(libs.logback)
}