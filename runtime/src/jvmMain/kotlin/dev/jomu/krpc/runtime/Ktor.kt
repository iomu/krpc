package dev.jomu.krpc.runtime

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

fun Routing.asRegistrar(): MethodRegistrar = object : MethodRegistrar {
    override fun <T, Req, Resp> register(
        method: MethodDescriptor<T, Req, Resp>,
        service: T,
        path: String,
        interceptor: UnaryServerInterceptor?
    ) {
        post(path) {
            val request = method.readRequest(call.receiveText())
            val message = KrpcMessage(request, call.request.headers.toMetadata())
            val response = method.handler(service, message, interceptor)

            val encoded = method.encodeResponse(response.value)
            response.metadata.forEach { (key, value) -> call.response.header("krpc-$key", value) }
            call.respondText(encoded)
        }
    }
}

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

object JsonSerializer : Serializer {
    private val json = Json { ignoreUnknownKeys = true }
    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): String {
        return json.encodeToString(serializer, value)
    }

    override fun <T> decode(deserializer: DeserializationStrategy<T>, encoded: String): T {
        return json.decodeFromString(deserializer, encoded)
    }
}

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .mapValues { (_, value) -> value.first() }
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })
