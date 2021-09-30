plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

group = "dev.jomu.krpc"
version = "0.1.5"

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
            }
        }
    }
}