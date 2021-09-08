plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

group = "dev.jomu.krpc"
version = "0.1.0"

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
                implementation(kotlin("stdlib-common"))
                implementation(project(":runtime"))
                implementation("io.ktor:ktor-client-core:1.6.1")
                api("io.ktor:ktor-client-core:1.6.1")
                api("io.ktor:ktor-utils:1.6.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
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