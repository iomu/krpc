package dev.jomu.krpc.sample

import dev.jomu.krpc.*
import dev.jomu.krpc.runtime.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.*

fun main() {
    val server = buildKrpcServer {
        registerTestService(Implementation)
        addInterceptor(PrintInterceptor)
    }
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            registerServer(server)
        }
    }.start(wait = true)
}

object Implementation : TestService {
    override suspend fun hello(name: String, metadata: Metadata): Response<String, Unit> {
        return Response.Success("Hello, $name!", metadata = metadata)
    }

    override suspend fun second(name: String, number: Int): Response<String, CustomError> {
        return Response.Error(ErrorCode.INTERNAL, "we got an error chief", CustomError(123))
    }

    override suspend fun third(name: String, number: Int, more: Float): Response<List<List<String>>, CustomError> {
        return Response.Success(listOf(listOf(name, "$number")))
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun generic(value: String, count: Int): Response<List<String>, Unit> {
        return Response.Success(buildList {
            for (i in 1..count) {
                add(value)
            }
        })
    }
}

object PrintInterceptor : UnaryServerInterceptor {
    override suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err> {
        println("Request: $request with $metadata")
        val response = next(request, metadata).also {
            println("Response: $it")
        }
        return response.withMetadata(Metadata(response.metadata.values.mapValues { (_, value) -> value.uppercase(Locale.getDefault()) }))
    }
}