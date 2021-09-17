plugins {
    alias(libs.plugins.ksp)
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

group = "dev.jomu.krpc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":runtime"))
    implementation(project(":integration:server-ktor"))
    api(project(":integration:server-ktor"))
    implementation(project(":integration:client-ktor"))
    ksp(project(":compiler"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.okhttp)

}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}


