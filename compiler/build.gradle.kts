plugins {
    id("java-library")
    kotlin("jvm")
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":core"))
    implementation(project(":runtime"))

    testImplementation(kotlin("test"))
    testImplementation(libs.compileTesting.core)
    testImplementation(libs.compileTesting.ksp)
    testImplementation(libs.kotlin.serialization.core)
    testImplementation(libs.kotlin.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}
