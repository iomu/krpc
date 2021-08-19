package dev.jomu.krpc.runtime

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

fun Routing.asRegistrar(): MethodRegistrar = object : MethodRegistrar {
    override fun <T> register(service: T, descriptor: ServiceDescriptor<T>, interceptor: UnaryServerInterceptor?) {
        for (method in descriptor.methods) {
            post(descriptor.path(method)) {
                val response = method.handler(service, JsonMessageStream(call), interceptor)

                val encoded = response.encodeJson()
                call.respondText(encoded)
            }
        }
    }
}

private class JsonMessageStream(val call: ApplicationCall) : MessageStream {
    override suspend fun <T> read(deserializer: DeserializationStrategy<T>): T {
        val text = call.receiveText()
        println(text)

        return Json.decodeFromString(deserializer, text)
    }
}

private fun <T> KrpcMessage<T>.encodeJson() = Json.encodeToString(serializer, value)

class KtorClient(private val client: HttpClient) : KrpcClient {
    override suspend fun executeUnaryCall(url: String, message: KrpcMessage<*>): MessageStream {
        val response = client.post<HttpResponse>(url) {
            body = message.encodeJson()
        }
        val contentAsString = response.content.readRemaining().readText()
        println(contentAsString)
        return object : MessageStream {
            override suspend fun <T> read(deserializer: DeserializationStrategy<T>): T {
                return Json.decodeFromString(deserializer, contentAsString)
            }
        }
    }
}
