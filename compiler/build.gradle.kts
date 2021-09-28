plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.5"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
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

publishing {
    publications {
        create<MavenPublication>("compiler") {
            from(components["java"])
        }
    }
}
