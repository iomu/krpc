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
    implementation(kotlin("stdlib"))
    api(project(":runtime"))
    api("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
}

publishing {
    publications {
        create<MavenPublication>("client-okhttp") {
            from(components["java"])
        }
    }
}