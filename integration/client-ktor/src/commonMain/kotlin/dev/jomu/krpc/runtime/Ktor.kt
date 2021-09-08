package dev.jomu.krpc.runtime

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class KtorClient(private val client: HttpClient) : KrpcClient {
    override suspend fun executeUnaryCall(url: String, message: KrpcMessage<String>): KrpcMessage<String> {
        val response = client.post<HttpResponse>(url) {
            body = message.value
            headers {
                message.metadata.forEach { (key, value) ->
                    append("krpc-$key", value)
                }
            }
        }
        val contentAsString = response.content.readRemaining().readText()
        val metadata = response.headers.toMetadata()

        return KrpcMessage(contentAsString, metadata)
    }
}

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .mapValues { (_, value) -> value.first() }
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })
