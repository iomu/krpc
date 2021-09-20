buildscript {
    dependencies {
        classpath(libs.kotlin.gradle)
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

group = "dev.jomu.krpc"
version = "0.1.3"

repositories {
    mavenCentral()
}

