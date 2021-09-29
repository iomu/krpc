plugins {
    kotlin("multiplatform")
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(project(":runtime"))
                implementation(libs.ktor.client.core)
                implementation(libs.kotlin.coroutines.core)
                api(libs.ktor.client.core)
                api(libs.ktor.utils)
                api(libs.kotlin.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}