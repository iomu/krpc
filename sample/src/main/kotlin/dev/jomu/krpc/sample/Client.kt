package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.JsonSerializer
import dev.jomu.krpc.runtime.KtorClient
import dev.jomu.krpc.runtime.Metadata
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

suspend fun main() {
    val client = TestServiceClient(KtorClient(HttpClient(OkHttp)), "http://127.0.0.1:8787", JsonSerializer, PrintInterceptor)

    val r = client.hello("Johannes", Metadata(mapOf("a" to "b")))
    println(r)

    runBlocking {
        for (i in 0..100) {
            async {
                val r = client.hello("Johannes", Metadata(mapOf("a" to "b")))
                println(r)

            }
    //        val r2 = client.second("Johannes", 123)
    //        println(r2)
    //        val r3 = client.third("Johannes", 123, 123.0f)
    //        println(r3)
        }

    }
}