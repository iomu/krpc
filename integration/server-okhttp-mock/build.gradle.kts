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
    implementation(libs.kotlin.coroutines.core)

    api(libs.okhttp.mockwebserver)

    api(libs.kotlin.serialization.json)
}