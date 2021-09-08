rootProject.name = "krpc"
include("compiler")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}
include("sample")
include("runtime")
include(":integration:client-ktor")
include(":integration:client-okhttp")
include(":integration:server-ktor")
