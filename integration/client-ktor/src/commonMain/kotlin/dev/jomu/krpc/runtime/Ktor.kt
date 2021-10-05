package dev.jomu.krpc.runtime

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class KtorClient(private val client: HttpClient) : KrpcHttpClient {
    override suspend fun post(url: String, message: OutgoingMessage<*>): IncomingMessage {
        val response = client.post<HttpResponse>(url) {
            body = message.write(JsonStringEncoder)
            headers {
                message.headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }

        return ResponseIncomingMessage(response)
    }
}

private class ResponseIncomingMessage(val response: HttpResponse) : IncomingMessage {
    override suspend fun <T> read(json: Json, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromString(deserializer, response.content.readRemaining().readText())
    }

    override val headers: Map<String, String>
        get() = response.headers.toMap()
            .mapValues { (_, value) -> value.first() }
}

private object JsonStringEncoder : JsonEncoder<String> {
    override fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): String {
        return json.encodeToString(serializer, value)
    }
}
