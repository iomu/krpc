package dev.jomu.krpc.runtime

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*

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

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .mapValues { (_, value) -> value.first() }
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })
