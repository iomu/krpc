rootProject.name = "krpc"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.7")
}

enableFeaturePreview("VERSION_CATALOGS")

include("compiler")
include("core")
include("sample")
include("runtime")
include(":integration:client-ktor")
include(":integration:client-okhttp")
include(":integration:server-ktor")
include(":integration:server-okhttp-mock")
include(":integration:tests")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
