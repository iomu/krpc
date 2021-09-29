plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

repositories {
    mavenCentral()
}

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
                api(project(":core"))
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