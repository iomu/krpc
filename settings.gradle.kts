rootProject.name = "krpc"
include("compiler")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

include("sample")
include("runtime")
include(":integration:client-ktor")
include(":integration:client-okhttp")
include(":integration:server-ktor")
include(":integration:server-okhttp-mock")
include(":integration:tests")
