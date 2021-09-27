plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":runtime"))
    api(libs.okhttp.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)
}

publishing {
    publications {
        create<MavenPublication>("client-okhttp") {
            from(components["java"])
        }
    }
}