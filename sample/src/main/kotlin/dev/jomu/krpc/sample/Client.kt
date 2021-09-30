package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.KtorClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

suspend fun main() {
    val client = TestServiceClient(KtorClient(HttpClient(OkHttp)), "http://127.0.0.1:8080", PrintInterceptor)

    val r = client.third("Johannes", 123, 42.42f)
    println(r)
}