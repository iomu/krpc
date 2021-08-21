package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.JsonSerializer
import dev.jomu.krpc.runtime.KtorClient
import dev.jomu.krpc.runtime.Metadata
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

suspend fun main() {
    val client = TestServiceClient(KtorClient(HttpClient(OkHttp)), "http://localhost:8080", JsonSerializer, PrintInterceptor)

    val r = client.hello("Johannes", Metadata(mapOf("a" to "b")))
    println(r)
    val r2 = client.second("Johannes", 123)
    println(r2)
    val r3 = client.third("Johannes", 123, 123.0f)
    println(r3)
}