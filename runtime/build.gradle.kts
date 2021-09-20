plugins {
    kotlin("multiplatform")
    id("maven-publish")
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

group = "dev.jomu.krpc"
version = "0.1.3"

kotlin {
    jvm()
    js(BOTH) {
        browser()
        nodejs()
    }
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.serialization.json)
                api(libs.kotlin.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting
    }
}