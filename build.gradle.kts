plugins {
    kotlin("multiplatform") version "1.5.21" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.5.21"))
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

group = "dev.jomu.krpc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

