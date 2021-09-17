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
        server.handleRequest(path, KtorCall(call))
    }
}

private class KtorCall(val call: ApplicationCall) : Call {
    override suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromString(deserializer, call.receiveText())
    }

    override suspend fun <T> respond(
        json: Json,
        serializer: SerializationStrategy<T>,
        response: T,
        metadata: Metadata
    ) {
        metadata.forEach { (key, value) -> call.response.header("krpc-$key", value) }
        call.respondText(json.encodeToString(serializer, response))
    }

    override val metadata: Metadata
        get() = call.request.headers.toMetadata()

}

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .mapValues { (_, value) -> value.first() }
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })
