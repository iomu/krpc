buildscript {
    dependencies {
        classpath(libs.kotlin.gradle)
        classpath(libs.mavenPublishGradle)
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

repositories {
    mavenCentral()
}

