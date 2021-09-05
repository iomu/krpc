plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.21-1.0.0-beta05")
    implementation("com.squareup:kotlinpoet:1.9.0")
    implementation(project(":runtime"))

    testImplementation(kotlin("test"))
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.2")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.2")
    testImplementation("org.jetbrains.kotlin:kotlin-serialization:1.5.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
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
