plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "dev.jomu.krpc"
version = "0.1.0"

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("io.ktor:ktor-server-core:1.6.1")
                api("io.ktor:ktor-server-netty:1.6.1")
                api("io.ktor:ktor-client-core:1.6.1")
                api("io.ktor:ktor-utils:1.6.1")
                api("io.ktor:ktor-client-okhttp:1.6.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
            }
        }
    }
}