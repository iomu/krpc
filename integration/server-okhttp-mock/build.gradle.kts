plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":runtime"))
    implementation(libs.kotlin.coroutines.core)
    
//    api(libs.okhttp.core)
//    implementation(libs.okhttp.core)
//    implementation(libs.okhttp.mockwebserver)
    implementation(libs.okhttp.mockwebserver)

    api(libs.kotlin.serialization.json)
}

publishing {
    publications {
        create<MavenPublication>("server-okhttp-mock") {
            from(components["java"])
        }
    }
}