package dev.jomu.krpc.runtime

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

fun Routing.registerServer(server: KrpcServer, prefix: String = "") {
    post("$prefix/{path...}") {
        val path = call.parameters.getAll("path")?.joinToString("/") ?: return@post call.respondText("Not found", status = HttpStatusCode.NotFound)
        val response = server.handleRequest(path, KtorCall(call))
        response.metadata.forEach { (key, value) -> call.response.header("krpc-$key", value) }
        call.respondText(response.encode(JsonStringEncoder))
    }
}

private object JsonStringEncoder : JsonEncoder<String> {
    override fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): String {
        return json.encodeToString(serializer, value)
    }
}

private class KtorCall(val call: ApplicationCall) : Call {
    override suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromString(deserializer, call.receiveText())
    }

    override val metadata: Metadata
        get() = call.request.headers.toMetadata()

}

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .mapValues { (_, value) -> value.first() }
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })
