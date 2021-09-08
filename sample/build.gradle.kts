plugins {
    id("com.google.devtools.ksp") version "1.5.21-1.0.0-beta05"
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.5.21"
}

group = "dev.jomu.krpc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":runtime"))
    implementation(project(":integration:server-ktor"))
    implementation(project(":integration:client-ktor"))
    ksp(project(":compiler"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("io.ktor:ktor-client-okhttp:1.6.1")

}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}


