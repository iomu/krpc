plugins {
    alias(libs.plugins.binaryCompatability)
}


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

apiValidation {
    ignoredProjects.addAll(listOf("compiler", "sample", "tests"))
}
