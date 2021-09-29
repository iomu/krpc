buildscript {
    dependencies {
        classpath(libs.kotlin.gradle)
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

group = "dev.jomu.krpc"
version = "0.1.5"

repositories {
    mavenCentral()
}

