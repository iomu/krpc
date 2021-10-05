plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":runtime"))
    implementation(libs.kotlin.coroutines.core)

    ksp(project(":compiler"))

    implementation(project(":integration:client-okhttp"))
    implementation(project(":integration:client-ktor"))
    implementation(libs.ktor.client.okhttp)
    implementation(project(":integration:server-ktor"))
    implementation(project(":integration:server-okhttp-mock"))

    testImplementation(libs.ktor.server.test)

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }

    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}