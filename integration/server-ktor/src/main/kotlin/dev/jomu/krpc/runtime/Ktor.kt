package dev.jomu.krpc.runtime

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

fun Routing.registerServer(server: KrpcServer, prefix: String = "") {
    post("$prefix/{path...}") {
        val path = call.parameters.getAll("path")?.joinToString("/") ?: return@post call.respondText(
            "Not found",
            status = HttpStatusCode.NotFound
        )
        val response = server.handleRequest(path, KtorCall(call))
        response.headers.forEach { (key, value) -> call.response.header(key, value) }

        call.respondOutputStream {
            response.encode(StreamEncoder(this))
        }
    }
}

private class StreamEncoder(val stream: OutputStream) : JsonEncoder<Unit> {
    @OptIn(ExperimentalSerializationApi::class)
    override fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U) {
        json.encodeToStream(serializer, value, stream)

    }
}

private class KtorCall(val call: ApplicationCall) : Call {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T {
        return withContext(Dispatchers.IO) {
            call.receive<InputStream>()
                .use { json.decodeFromStream(deserializer, it) }
        }
    }

    override val headers: Map<String, String>
        get() = call.request.headers.toMap().mapValues { (_, value) -> value.first() }
}
